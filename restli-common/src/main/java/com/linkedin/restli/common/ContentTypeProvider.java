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

import java.net.URI;
import java.util.Map;
import javax.activation.MimeType;


/**
 * Provides a custom {@link ContentType} instance based on the mime type.
 */
public interface ContentTypeProvider {

  /**
   * Get a content type instance based on the given {@link MimeType} to decode the request/response body.
   *
   * @param rawMimeType  The raw mime type string. This is passed in addition to the parsed mime type to avoid
   *                     re-serialization of the mime type if we only need the string.
   * @param mimeType     The parsed mime type.
   *
   * @return The {@link ContentType} for the given mime type.
   */
  ContentType getContentType(String rawMimeType, MimeType mimeType);

  /**
   * Get a content type instance to encode the request body.
   *
   * @param rawMimeType   The raw mime type string. This is passed in addition to the parsed mime type to avoid
   *                      re-serialization of the mime type if we only need the string.
   * @param mimeType      The mime type.
   * @param requestUri    The request URI.
   * @return The {@link ContentType} for the given mime type parameters mapping.
   */
  default ContentType getRequestContentType(String rawMimeType, MimeType mimeType, URI requestUri)
  {
    return getContentType(rawMimeType, mimeType);
  }

  /**
   * Get a content type instance to encode the response body.
   *
   * @param rawMimeType    The raw mime type string. This is passed in addition to the parsed mime type to avoid
   *                       re-serialization of the mime type if we only need the string.
   * @param mimeType       The mime type.
   * @param requestUri     The request URI.
   * @param requestHeaders The request headers.
   * @return The {@link ContentType} for the given mime type parameters mapping.
   */
  default ContentType getResponseContentType(String rawMimeType, MimeType mimeType, URI requestUri, Map<String, String> requestHeaders)
  {
    return getContentType(rawMimeType, mimeType);
  }
}