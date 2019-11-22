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

package com.linkedin.restli.common;

import com.linkedin.data.codec.symbol.SymbolTable;
import com.linkedin.data.codec.symbol.SymbolTableProviderHolder;
import java.net.URI;
import java.util.Map;
import java.util.function.BiFunction;
import javax.activation.MimeType;


/**
 * Provides a custom {@link ContentType} instance based on the {@link RestConstants#CONTENT_TYPE_PARAM_SYMBOL_TABLE}
 * mime type parameter.
 *
 * <p>This is useful for constructing custom codecs like LICOR and Protobuf.</p>
 */
public class SymbolTableBasedContentTypeProvider implements ContentTypeProvider
{
  private final ContentType _baseContentType;
  private final BiFunction<String, SymbolTable, ContentType> _symbolTableMapper;

  public SymbolTableBasedContentTypeProvider(ContentType baseContentType, BiFunction<String, SymbolTable, ContentType> symbolTableMapper)
  {
    _baseContentType = baseContentType;
    _symbolTableMapper = symbolTableMapper;
  }

  @Override
  public final ContentType getContentType(String rawMimeType, MimeType mimeType)
  {
    if (mimeType.getParameters().isEmpty())
    {
      return _baseContentType;
    }

    String symbolTableName = mimeType.getParameter(RestConstants.CONTENT_TYPE_PARAM_SYMBOL_TABLE);
    if (symbolTableName == null)
    {
      return _baseContentType;
    }

    final SymbolTable symbolTable =
        SymbolTableProviderHolder.INSTANCE.getSymbolTableProvider().getSymbolTable(symbolTableName);
    if (symbolTable == null)
    {
      return _baseContentType;
    }

    return _symbolTableMapper.apply(rawMimeType, symbolTable);
  }

  @Override
  public ContentType getRequestContentType(String rawMimeType, MimeType mimeType, URI requestUri)
  {
    final SymbolTable requestSymbolTable =
        SymbolTableProviderHolder.INSTANCE.getSymbolTableProvider().getRequestSymbolTable(requestUri);
    return getContentType(mimeType, requestSymbolTable);
  }

  @Override
  public ContentType getResponseContentType(String rawMimeType, MimeType mimeType, URI requestUri,
      Map<String, String> requestHeaders) {
    final SymbolTable responseSymbolTable =
        SymbolTableProviderHolder.INSTANCE.getSymbolTableProvider().getResponseSymbolTable(requestUri, requestHeaders);
    return getContentType(mimeType, responseSymbolTable);
  }

  public ContentType getBaseContentType()
  {
    return _baseContentType;
  }

  private ContentType getContentType(MimeType mimeType, SymbolTable symbolTable)
  {
    if (symbolTable == null)
    {
      return _baseContentType;
    }

    mimeType.setParameter(RestConstants.CONTENT_TYPE_PARAM_SYMBOL_TABLE, symbolTable.getName());
    return _symbolTableMapper.apply(mimeType.toString(), symbolTable);
  }
}
