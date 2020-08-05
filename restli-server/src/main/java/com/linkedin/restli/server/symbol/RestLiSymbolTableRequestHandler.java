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

package com.linkedin.restli.server.symbol;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.linkedin.common.callback.Callback;
import com.linkedin.data.ByteString;
import com.linkedin.data.codec.symbol.DefaultSymbolTableProvider;
import com.linkedin.data.codec.symbol.SymbolTable;
import com.linkedin.data.codec.symbol.SymbolTableProvider;
import com.linkedin.data.codec.symbol.SymbolTableProviderHolder;
import com.linkedin.data.codec.symbol.SymbolTableSerializer;
import com.linkedin.jersey.api.uri.UriComponent;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.restli.common.ContentType;
import com.linkedin.restli.common.HttpMethod;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.server.NonResourceRequestHandler;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.activation.MimeTypeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A {@link NonResourceRequestHandler} used to serve symbol tables.
 */
public class RestLiSymbolTableRequestHandler implements NonResourceRequestHandler
{
  public static final String SYMBOL_TABLE_URI_PATH = DefaultSymbolTableProvider.SYMBOL_TABLE_URI_PATH;
  private static final Logger LOGGER = LoggerFactory.getLogger(RestLiSymbolTableRequestHandler.class);
  private static final int DEFAULT_CACHE_SIZE = 100;

  private final Cache<String, ByteString> _symbolTableNameToSerializedBytesCache;

  /**
   * Construct an instance with a symbol table name to serialized bytes cache size of {@link #DEFAULT_CACHE_SIZE}.
   */
  public RestLiSymbolTableRequestHandler()
  {
    this(DEFAULT_CACHE_SIZE);
  }

  /**
   * Construct an instance with a specific symbol table name to serialized bytes cache size.
   *
   * @param cacheSize The size of the symbol table name to serialized bytes cache.
   */
  public RestLiSymbolTableRequestHandler(int cacheSize)
  {
    _symbolTableNameToSerializedBytesCache = Caffeine.newBuilder().maximumSize(cacheSize).build();
  }

  @Override
  public boolean shouldHandle(Request request)
  {
    // we don't check the method here because we want to return 405 if it is anything but GET
    final String path = request.getURI().getRawPath();
    if (path == null)
    {
      return false;
    }

    final List<UriComponent.PathSegment> pathSegments = UriComponent.decodePath(path, true);
    if (pathSegments.size() < 2)
    {
      return false;
    }

    //
    // When path is service scoped, URI is in the form of /<SERVICE>/symbolTable, else it
    // is in the form of /symbolTable or /symbolTable/<TABLENAME>
    //
    boolean isServiceScopedPath = request.getHeaders().containsKey(RestConstants.HEADER_SERVICE_SCOPED_PATH);
    if (isServiceScopedPath)
    {
      return (pathSegments.size() == 3 && pathSegments.get(2).getPath().equals(SYMBOL_TABLE_URI_PATH));
    }
    else
    {
      return ((pathSegments.size() == 2 || pathSegments.size() == 3) && pathSegments.get(1).getPath().equals(SYMBOL_TABLE_URI_PATH));
    }
  }

  @Override
  public void handleRequest(RestRequest request, RequestContext requestContext, Callback<RestResponse> callback)
  {
    if (HttpMethod.GET != HttpMethod.valueOf(request.getMethod()))
    {
      LOGGER.error("GET is expected, but " + request.getMethod() + " received");
      callback.onError(RestException.forError(HttpStatus.S_405_METHOD_NOT_ALLOWED.getCode(), "Invalid method"));
      return;
    }

    //
    // Determine response content type based on accept header.
    // Assume protobuf2 if no accept header is specified. Note that this is a deviation from the rest of rest.li
    // which assumes JSON as the default, for efficiency reasons.
    //
    ContentType type;
    String mimeType =
        Optional.ofNullable(request.getHeader(RestConstants.HEADER_ACCEPT))
            .orElse(RestConstants.HEADER_VALUE_APPLICATION_PROTOBUF2);
    try
    {
      type =  ContentType.getContentType(mimeType).orElseThrow(() ->
          new MimeTypeParseException("Invalid accept type: " + mimeType));
    }
    catch (MimeTypeParseException e)
    {
      LOGGER.error("Could not handle accept type", e);
      callback.onError(RestException.forError(HttpStatus.S_406_NOT_ACCEPTABLE.getCode(), "Invalid accept type: " + mimeType));
      return;
    }

    final String path = request.getURI().getRawPath();
    final List<UriComponent.PathSegment> pathSegments = UriComponent.decodePath(path, true);

    final SymbolTableProvider provider = SymbolTableProviderHolder.INSTANCE.getSymbolTableProvider();
    SymbolTable symbolTable = null;
    try
    {
      boolean isServiceScopedPath = request.getHeaders().containsKey(RestConstants.HEADER_SERVICE_SCOPED_PATH);
      if (isServiceScopedPath)
      {
        symbolTable = provider.getResponseSymbolTable(request.getURI(), request.getHeaders());
      }
      else
      {
        if (pathSegments.size() == 2)
        {
          symbolTable = provider.getResponseSymbolTable(request.getURI(), request.getHeaders());
        }
        else if (pathSegments.size() == 3)
        {
          symbolTable = provider.getSymbolTable(pathSegments.get(2).getPath());
        }
      }
    }
    catch (IllegalStateException e)
    {
      LOGGER.error("Exception retrieving symbol table for URI " + request.getURI());
      symbolTable = null;
    }

    if (symbolTable == null)
    {
      LOGGER.error("Did not find symbol table for path " + path);
      callback.onError(RestException.forError(HttpStatus.S_404_NOT_FOUND.getCode(), "Did not find symbol table"));
      return;
    }

    try
    {
      // Cache key is the name of the symbol table concatenated with the type used to serialize the payload.
      String cacheKey = symbolTable.getName() + ":" + type.getHeaderKey();
      ByteString serializedTable = _symbolTableNameToSerializedBytesCache.getIfPresent(cacheKey);
      if (serializedTable == null)
      {
        serializedTable = SymbolTableSerializer.toByteString(type.getCodec(), symbolTable);
        _symbolTableNameToSerializedBytesCache.put(cacheKey, serializedTable);
      }

      RestResponse restResponse =
          new RestResponseBuilder()
              .setStatus(HttpStatus.S_200_OK.getCode())
              .setHeader(RestConstants.HEADER_CONTENT_TYPE, type.getHeaderKey())
              .setEntity(serializedTable)
              .build();
      callback.onSuccess(restResponse);
    }
    catch (IOException e)
    {
      callback.onError(e);
    }
  }
}
