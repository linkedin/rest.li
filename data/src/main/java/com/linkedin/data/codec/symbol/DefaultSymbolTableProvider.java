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
import java.net.URLEncoder;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
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
   * IC header key
   */
  private static final String IC_HEADER = "X-LI-R2-W-IC-1";

  /**
   * Default IC header
   */
  private static final String DEFAULT_IC_HEADER = "serviceCallTraceData=%7B%22caller%22%3A%7B%22container%22%3A%22container%22%2C%22service%22%3A%22restli%22%2C%22env%22%3A%22env%22%2C%22instance%22%3A%22inst%22%2C%22machine%22%3A%22machine.linkedin.biz%22%2C%22version%22%3A%220.0.1%22%7D%2C%22treeId%22%3A%22{}%22%2C%22context%22%3A%7B%22source%22%3A%22restli%22%2C%22forceTraceEnabled%22%3A%22false%22%2C%22debugEnabled%22%3A%22false%22%2C%22traceGroupingKey%22%3A%22restli-default-symboltable%22%7D%7D,com.linkedin.container.rpc.trace.rpcTrace=(machine.linkedin.biz,restli,1970/01/01 01:00:00.000)";

  /**
   * Symbol table request header
   */
  private static final String SYMBOL_TABLE_HEADER = "x-restli-symbol-table-request";

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
   * Overridden SSL socket factory if any.
   */
  private static SSLSocketFactory SSL_SOCKET_FACTORY;

  /**
   * Default request headers to fetch remote symbol table if any.
   */
  private static Map<String, String> DEFAULT_HEADERS;

  /**
   * Cache storing mapping from symbol table name to symbol table.
   */
  private final Cache<String, SymbolTable> _cache;

  /**
   * Set the overridden SSL socket factory.
   */
  public static void setSSLSocketFactory(SSLSocketFactory socketFactory) {
    SSL_SOCKET_FACTORY = socketFactory;
  }

  /**
   * Set Default headers to fetch remote symbol table
   */
  public static void setDefaultHeaders(Map<String, String> defaultHeaders) {
    DEFAULT_HEADERS = defaultHeaders;
  }

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
        if (DEFAULT_HEADERS != null)
        {
          DEFAULT_HEADERS.entrySet().forEach(entry -> connection.setRequestProperty(entry.getKey(), entry.getValue()));
        }
        connection.setRequestProperty(IC_HEADER,
                DEFAULT_IC_HEADER.replaceAll("\\{}", Base64.getEncoder().encodeToString(getTreeId(Instant.now()))));
        connection.setRequestProperty(ACCEPT_HEADER, ProtobufDataCodec.DEFAULT_HEADER);
        connection.setRequestProperty(SYMBOL_TABLE_HEADER, Boolean.toString(true));
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
    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    if (SSL_SOCKET_FACTORY != null && connection instanceof HttpsURLConnection)
    {
      ((HttpsURLConnection) connection).setSSLSocketFactory(SSL_SOCKET_FACTORY);
    }

    return connection;
  }


  private final static int TREE_ID_RESERVED_LENGTH = 1;
  private final static int TREE_ID_LENGTH = 16;
  private final static int TREE_ID_RANDOM_LENGTH = 8;
  // TreeId only support version 0~3
  private final static byte TREE_ID_RESERVED_VERSION_MASK = 0x03;

  // Below are config specific for treeId_v0
  private final static byte TREE_ID_RESERVED_UNUSED = 0x00;
  private final static byte TREE_ID_RESERVED_EXCLUDE_VERSION_MASK = (byte) 0xfc;
  private final static int TREE_ID_VERSION = 0x00; // current version = 0
  // 6 bits unused + 2 bit version
  private final static byte TREE_ID_RESERVED = (TREE_ID_RESERVED_UNUSED & TREE_ID_RESERVED_EXCLUDE_VERSION_MASK)
          | (TREE_ID_VERSION & TREE_ID_RESERVED_VERSION_MASK);

  private byte[] getTreeId(Instant instant) {
    final byte[] treeId = new byte[TREE_ID_LENGTH];
    final byte[] random = new byte[TREE_ID_RANDOM_LENGTH];

    // assign reserved to treeId
    treeId[0] = TREE_ID_RESERVED;
    // assign micro-second epoch time to treeId
    long currentTimeUs = ChronoUnit.MICROS.between(Instant.EPOCH, instant);
    for (int i = TREE_ID_LENGTH - TREE_ID_RANDOM_LENGTH - 1; i >= TREE_ID_RESERVED_LENGTH; i--) {
      treeId[i] = (byte) (currentTimeUs & 0xFF);
      currentTimeUs >>= Byte.SIZE;
    }
    // assign random to treeId
    ThreadLocalRandom.current().nextBytes(random);
    System.arraycopy(random, 0, treeId, TREE_ID_LENGTH - TREE_ID_RANDOM_LENGTH, TREE_ID_RANDOM_LENGTH);

    return treeId;
  }
}
