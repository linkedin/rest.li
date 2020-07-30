/*
   Copyright (c) 2020 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.linkedin.data.codec.symbol;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.linkedin.data.codec.ProtobufCodecOptions;
import com.linkedin.data.codec.ProtobufDataCodec;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A default {@link SymbolTableProvider} implementation that doesn't use symbol tables for requests/responses of its
 * own, but is able to retrieve remote symbol tables to decode responses from other services
 */
public class DefaultSymbolTableProvider implements SymbolTableProvider
{
  /**
   * Metadata extractor
   */
  private static final SymbolTableMetadataExtractor METADATA_EXTRACTOR = new SymbolTableMetadataExtractor();

  /**
   * Accept header
   */
  private static final String ACCEPT_HEADER = "Accept";

  /**
   * Logger.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSymbolTableProvider.class.getSimpleName());

  /**
   * Codec
   */
  static final ProtobufDataCodec CODEC =
      new ProtobufDataCodec(new ProtobufCodecOptions.Builder().setEnableASCIIOnlyStrings(true).build());

  /**
   * Path from which symbol tables are served by remote Rest.li services.
   */
  public static final String SYMBOL_TABLE_URI_PATH = "symbolTable";

  /**
   * Cache storing mapping from symbol table name to symbol table.
   */
  private final Cache<String, SymbolTable> _cache;

  /**
   * Constructor
   */
  DefaultSymbolTableProvider()
  {
    _cache = Caffeine.newBuilder().maximumSize(1000).build();
  }

  /**
   * Inject a local symbol table into the symbol table cache.
   *
   * @param symbolTable The symbol table to inject.
   */
  public void injectLocalSymbolTable(SymbolTable symbolTable)
  {
    if (symbolTable != null)
    {
      _cache.put(symbolTable.getName(), symbolTable);
    }
    else
    {
      LOGGER.error("Cannot inject null local symbol table");
    }
  }

  @Override
  public SymbolTable getSymbolTable(String symbolTableName)
  {
    try
    {
      SymbolTableMetadata metadata = METADATA_EXTRACTOR.extractMetadata(symbolTableName);
      String serverNodeUri = metadata.getServerNodeUri();
      String tableName = metadata.getSymbolTableName();
      boolean isRemote = metadata.isRemote();

      // First check the cache.
      SymbolTable symbolTable = _cache.getIfPresent(tableName);
      if (symbolTable != null)
      {
        return symbolTable;
      }

      // If this is not a remote table, and we didn't find it in the cache, cry foul.
      if (!isRemote)
      {
        throw new IllegalStateException("Unable to fetch symbol table with name: " + symbolTableName);
      }

      // Ok, we didn't find it in the cache, let's go query the service the table was served from.
      String url = serverNodeUri + "/" + SYMBOL_TABLE_URI_PATH + "/" + tableName;
      HttpURLConnection connection = openConnection(url);
      try
      {
        connection.setRequestProperty(ACCEPT_HEADER, ProtobufDataCodec.DEFAULT_HEADER);
        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK)
        {
          InputStream inputStream = connection.getInputStream();
          // Deserialize
          symbolTable = SymbolTableSerializer.fromInputStream(inputStream, CODEC, null);
        }
        else
        {
          throw new IOException("Unexpected response status: " + responseCode);
        }
      }
      finally
      {
        connection.disconnect();
      }

      // Cache the retrieved table.
      _cache.put(tableName, symbolTable);
      return symbolTable;
    }
    catch (MalformedURLException ex)
    {
      LOGGER.error("Failed to construct symbol table URL from symbol table name: " + symbolTableName, ex);
    }
    catch (Exception e)
    {
      LOGGER.error("Failed to fetch remote symbol table with name: " + symbolTableName, e);
    }

    throw new IllegalStateException("Unable to fetch symbol table with name: " + symbolTableName);
  }

  HttpURLConnection openConnection(String url) throws IOException
  {
    return (HttpURLConnection) new URL(url).openConnection();
  }
}
