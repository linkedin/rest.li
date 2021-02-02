/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.restli.tools.symbol;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.linkedin.d2.balancer.util.LoadBalancerUtil;
import com.linkedin.data.ByteString;
import com.linkedin.data.codec.symbol.EmptySymbolTable;
import com.linkedin.data.codec.symbol.InMemorySymbolTable;
import com.linkedin.data.codec.symbol.SymbolTable;
import com.linkedin.data.codec.symbol.SymbolTableMetadata;
import com.linkedin.data.codec.symbol.SymbolTableProvider;
import com.linkedin.data.codec.symbol.SymbolTableSerializer;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.restli.common.ContentType;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.server.ResourceDefinition;
import com.linkedin.restli.server.ResourceDefinitionListener;
import com.linkedin.restli.server.symbol.RestLiSymbolTableRequestHandler;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A reference {@link SymbolTableProvider} implementation that implements a symmetric protocol to
 * communicate between Rest.li services using symbol tables.
 *
 * <br>
 * <br>
 * <p>
 * Using this implementation should be done as follows:
 * <li>Construct an instance using the public constructor.
 * <li>Use {@link com.linkedin.data.codec.symbol.SymbolTableProviderHolder#setSymbolTableProvider(SymbolTableProvider)} to
 * set the instance as the global provider instance.
 * <li>Use {@link com.linkedin.restli.server.RestLiConfig#addResourceDefinitionListener(ResourceDefinitionListener)} to add
 * the instance as a listener to build the server side symbol table from all resources on server startup, or generate
 * your own list of symbols and pass them in as a {@link List<String>} via the constructor.
 * </p>
 *
 * <br>
 * <br>
 * <p>This implementation retrieves symbol tables by calling the {@link RestLiSymbolTableRequestHandler#SYMBOL_TABLE_URI_PATH}
 * endpoint on the remote rest.li service. Results are cached by name to avoid unnecessary future invocations.</p>
 *
 * <br>
 * <p>The symbol table name used by this provider is prefixed with the root URI of the service on which
 * the symbol table is hosted. For remote symbol tables, this prefix is renamed to the prefix of the current service
 * before it is cached. The final symbol table name is in the form of ServiceURI|Prefix-SymbolListHashCode</p>
 */
public class RestLiSymbolTableProvider implements SymbolTableProvider, ResourceDefinitionListener
{
  private static final Logger LOGGER = LoggerFactory.getLogger(RestLiSymbolTableProvider.class.getSimpleName());

  /**
   * Default timeout in milliseconds to use when fetching symbols from other services.
   */
  private static final long DEFAULT_TIMEOUT_MILLIS = 1000;
  private final Client _client;
  private final String _uriPrefix;
  private final String _serverNodeUri;
  private final SymbolTableNameHandler _symbolTableNameHandler;
  private final Cache<String, SymbolTable> _serviceNameToSymbolTableCache;
  private final Cache<String, SymbolTable> _symbolTableNameToSymbolTableCache;
  private volatile SymbolTable _defaultResponseSymbolTable = null;
  private volatile String _defaultResponseSymbolTableName = null;

  /**
   * Constructor
   *
   * @param client             The {@link Client} to use to make requests to remote services to fetch their symbol tables.
   * @param uriPrefix          The URI prefix to use when invoking remote services by name (and not by hostname:port)
   * @param cacheSize          The size of the caches used to store symbol tables.
   * @param symbolTablePrefix  The prefix to use for symbol tables vended by this instance.
   * @param serverNodeUri      The URI on which the current service is running. This should also include the context
   *                           and servlet path (if applicable).
   */
  public RestLiSymbolTableProvider(Client client,
      String uriPrefix,
      int cacheSize,
      String symbolTablePrefix,
      String serverNodeUri)
  {
    this(client, uriPrefix, cacheSize, symbolTablePrefix, serverNodeUri, null);
  }

  /**
   * Constructor
   *
   * @param client             The {@link Client} to use to make requests to remote services to fetch their symbol tables.
   * @param uriPrefix          The URI prefix to use when invoking remote services by name (and not by hostname:port)
   * @param cacheSize          The size of the caches used to store symbol tables.
   * @param symbolTablePrefix  The prefix to use for symbol tables vended by this instance.
   * @param serverNodeUri      The URI on which the current service is running. This should also include the context
   *                           and servlet path (if applicable).
   * @param overriddenSymbols  The list of overridden symbols to use for the symbol table.
   */
  public RestLiSymbolTableProvider(Client client,
      String uriPrefix,
      int cacheSize,
      String symbolTablePrefix,
      String serverNodeUri,
      List<String> overriddenSymbols)
  {
    _client = client;
    _uriPrefix = uriPrefix;
    _serverNodeUri = serverNodeUri;
    _symbolTableNameHandler = new SymbolTableNameHandler(symbolTablePrefix, serverNodeUri);
    _serviceNameToSymbolTableCache = Caffeine.newBuilder().maximumSize(cacheSize).build();
    _symbolTableNameToSymbolTableCache = Caffeine.newBuilder().maximumSize(cacheSize).build();

    if (serverNodeUri != null && overriddenSymbols != null)
    {
      String symbolTableName = _symbolTableNameHandler.generateName(overriddenSymbols);
      _defaultResponseSymbolTable = new InMemorySymbolTable(symbolTableName, overriddenSymbols);
      _defaultResponseSymbolTableName = _symbolTableNameHandler.extractMetadata(symbolTableName).getSymbolTableName();
    }
  }

  /**
   * Constructor
   *
   * @param client               The {@link Client} to use to make requests to remote services to fetch their symbol tables.
   * @param uriPrefix            The URI prefix to use when invoking remote services by name (and not by hostname:port)
   * @param cacheSize            The size of the caches used to store symbol tables.
   * @param serverNodeUri        The URI on which the current service is running. This should also include the context
   *                             and servlet path (if applicable).
   * @param responseSymbolTable  The pre-generated response symbol table.
   */
  public RestLiSymbolTableProvider(Client client,
      String uriPrefix,
      int cacheSize,
      String serverNodeUri,
      SymbolTable responseSymbolTable)
  {
    _client = client;
    _uriPrefix = uriPrefix;
    _serverNodeUri = serverNodeUri;
    _symbolTableNameHandler = new SymbolTableNameHandler(responseSymbolTable.getName(), serverNodeUri);
    _serviceNameToSymbolTableCache = Caffeine.newBuilder().maximumSize(cacheSize).build();
    _symbolTableNameToSymbolTableCache = Caffeine.newBuilder().maximumSize(cacheSize).build();
    
    if (_serverNodeUri != null)
    {
      _defaultResponseSymbolTable = responseSymbolTable;
      _defaultResponseSymbolTableName = responseSymbolTable.getName();
    }
  }

  @Override
  public SymbolTable getSymbolTable(String symbolTableName)
  {
    try
    {
      SymbolTableMetadata metadata = _symbolTableNameHandler.extractMetadata(symbolTableName);
      String serverNodeUri = metadata.getServerNodeUri();
      String tableName = metadata.getSymbolTableName();
      boolean isRemote = metadata.isRemote();

      // Check if it's the default table name.
      if (tableName.equals(_defaultResponseSymbolTableName))
      {
        return _defaultResponseSymbolTable;
      }

      // First check the cache.
      SymbolTable symbolTable = _symbolTableNameToSymbolTableCache.getIfPresent(tableName);
      if (symbolTable != null)
      {
        return symbolTable;
      }

      // If this is a local table, and we didn't find it in the cache, cry foul.
      if (!isRemote)
      {
        throw new IllegalStateException("Unable to fetch symbol table with name: " + symbolTableName);
      }

      // Ok, we didn't find it in the cache, let's go query the service the table was served from.
      URI symbolTableUri = new URI(serverNodeUri + "/" + RestLiSymbolTableRequestHandler.SYMBOL_TABLE_URI_PATH + "/" + tableName);
      symbolTable = fetchRemoteSymbolTable(symbolTableUri, Collections.emptyMap(), false);

      if (symbolTable != null)
      {
        // Cache the retrieved table.
        _symbolTableNameToSymbolTableCache.put(tableName, symbolTable);
        return symbolTable;
      }
    }
    catch (URISyntaxException ex)
    {
      LOGGER.error("Failed to construct symbol table URI from symbol table name: " + symbolTableName, ex);
    }

    throw new IllegalStateException("Unable to fetch symbol table with name: " + symbolTableName);
  }

  @Override
  public SymbolTable getRequestSymbolTable(URI requestUri)
  {
    // If the URI prefix doesn't match, return null.
    if (!requestUri.toString().startsWith(_uriPrefix))
    {
      return null;
    }
    String serviceName = LoadBalancerUtil.getServiceNameFromUri(requestUri);

    // First check the cache.
    SymbolTable symbolTable = _serviceNameToSymbolTableCache.getIfPresent(serviceName);
    if (symbolTable != null)
    {
      // If we got a 404, we will cache an empty symbol table. For such cases, just return null, so that no
      // symbol table is used.
      return symbolTable == EmptySymbolTable.SHARED ? null : symbolTable;
    }

    try
    {
      URI symbolTableUri = new URI(_uriPrefix + serviceName + "/" + RestLiSymbolTableRequestHandler.SYMBOL_TABLE_URI_PATH);

      //
      // Fetch remote symbol table, configuring the fetch to return an empty table on 404. This will ensure that
      // for services that don't have symbol tables enabled yet, we will not use any symbol tables when encoding.
      //
      symbolTable = fetchRemoteSymbolTable(symbolTableUri, Collections.emptyMap(), true);

      if (symbolTable != null)
      {
        // Cache the retrieved table.
        _serviceNameToSymbolTableCache.put(serviceName, symbolTable);

        // If this symbol table is not the shared empty table, also cache it by symbol table name, else return null
        // to not use any symbol tables when encoding.
        if (symbolTable != EmptySymbolTable.SHARED)
        {
          _symbolTableNameToSymbolTableCache.put(
              _symbolTableNameHandler.extractMetadata(symbolTable.getName()).getSymbolTableName(), symbolTable);
        }
        else
        {
          return null;
        }
      }

      return symbolTable;
    }
    catch (URISyntaxException ex)
    {
      LOGGER.error("Failed to construct symbol table URI from request URI " + requestUri, ex);
    }

    return null;
  }

  @Override
  public SymbolTable getResponseSymbolTable(URI requestUri, Map<String, String> requestHeaders)
  {
    return _defaultResponseSymbolTable;
  }

  @Override
  public void onInitialized(Map<String, ResourceDefinition> resourceDefinitions)
  {
    // Do nothing if the server node URI was null or if an overridden list of symbols was passed and the default
    // response symbol table was already built.
    if (_serverNodeUri == null || _defaultResponseSymbolTable != null)
    {
      return;
    }

    Set<DataSchema> schemas = new HashSet<>();
    resourceDefinitions.values().forEach(resourceDefinition -> resourceDefinition.collectReferencedDataSchemas(schemas));
    _defaultResponseSymbolTable = RuntimeSymbolTableGenerator.generate(_symbolTableNameHandler, schemas);
    _defaultResponseSymbolTableName =
        _symbolTableNameHandler.extractMetadata(_defaultResponseSymbolTable.getName()).getSymbolTableName();
  }

  SymbolTable fetchRemoteSymbolTable(URI symbolTableUri, Map<String, String> requestHeaders, boolean returnEmptyOn404)
  {
    try
    {
      Map<String, String> headers = new HashMap<>(requestHeaders);
      headers.put(RestConstants.HEADER_FETCH_SYMBOL_TABLE, Boolean.TRUE.toString());
      Future<RestResponse> future = _client.restRequest(new RestRequestBuilder(symbolTableUri).setHeaders(headers).build());
      RestResponse restResponse = future.get(DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
      int status = restResponse.getStatus();

      if (returnEmptyOn404 && status == HttpStatus.S_404_NOT_FOUND.getCode())
      {
        return EmptySymbolTable.SHARED;
      }

      if (status == HttpStatus.S_200_OK.getCode())
      {
        ByteString byteString = restResponse.getEntity();
        if (byteString == null)
        {
          throw new IOException("Empty body");
        }

        ContentType contentType =
            ContentType.getContentType(restResponse.getHeader(RestConstants.HEADER_CONTENT_TYPE))
                .orElseThrow(() -> new IOException("Could not parse response content type"));

        // Deserialize, and rename to replace url prefix with current url prefix.
        return SymbolTableSerializer.fromByteString(byteString, contentType.getCodec(), _symbolTableNameHandler::replaceServerNodeUri);
      }

      throw new IOException("Unexpected response status: " + status);
    }
    catch (ExecutionException ex)
    {
      LOGGER.error("Failed to fetch symbol table from " + symbolTableUri, ex.getCause());
    }
    catch (Exception ex)
    {
      LOGGER.error("Failed to fetch symbol table from " + symbolTableUri, ex);
    }

    return null;
  }
}
