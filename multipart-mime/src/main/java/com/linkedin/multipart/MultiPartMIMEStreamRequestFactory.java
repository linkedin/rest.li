/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.multipart;


import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.Map;


/**
 * A wrapper to enforce creating a proper multipart mime{@link com.linkedin.r2.message.stream.StreamRequest}
 *
 * @author Karim Vidhani
 */
public final class MultiPartMIMEStreamRequestFactory
{
  /**
   * Create a {@link com.linkedin.r2.message.stream.StreamRequest} using the specified parameters. This API should be used
   * if the user does not have a need to define a {@link com.linkedin.r2.message.stream.StreamRequestBuilder} in advance.
   *
   * @param uri the target URI to be used.
   * @param mimeSubType the mime subtype of the multipart to be used. For example, 'mixed' would result in a Content-Type of
   *                    'multipart/mixed'. It is generally good practice to use subtypes described in RFC 1341, although
   *                    this API does not enforce this.
   * @param writer the {@link com.linkedin.multipart.MultiPartMIMEWriter} to use for the payload of the request.
   * @return the newly created {@link com.linkedin.r2.message.stream.StreamRequest}.
   */
  public static StreamRequest generateMultiPartMIMEStreamRequest(final URI uri, final String mimeSubType,
                                                                 final MultiPartMIMEWriter writer)
  {
    return generateMultiPartMIMEStreamRequest(mimeSubType, writer, Collections.<String, String>emptyMap(), new StreamRequestBuilder(uri));
  }

  /**
   * Create a {@link com.linkedin.r2.message.stream.StreamRequest} using the specified parameters. This API should be used
   * if the user does not have a need to define a {@link com.linkedin.r2.message.stream.StreamRequestBuilder} in advance.
   *
   * @param uri the target URI to be used.
   * @param mimeSubType the mime subtype of the multipart to be used. For example, 'mixed' would result in a Content-Type of
   *                    'multipart/mixed'. It is generally good practice to use subtypes described in RFC 1341, although
   *                    this API does not enforce this.
   * @param writer the {@link com.linkedin.multipart.MultiPartMIMEWriter} to use for the payload of the request.
   * @param contentTypeParameters any additional parameters needed when constructing the Content-Type header.
   * @return the newly created {@link com.linkedin.r2.message.stream.StreamRequest}.
   */
  public static StreamRequest generateMultiPartMIMEStreamRequest(final URI uri, final String mimeSubType,
                                                                 final MultiPartMIMEWriter writer,
                                                                 final Map<String, String> contentTypeParameters)
  {
    return generateMultiPartMIMEStreamRequest(mimeSubType, writer, contentTypeParameters, new StreamRequestBuilder(uri));
  }

  /**
   * Create a {@link com.linkedin.r2.message.stream.StreamRequest} using the specified parameters. This API should be used
   * if the user has a need to define a {@link com.linkedin.r2.message.stream.StreamRequestBuilder} in advance. For example
   * if the user wants to add specific headers or modify the StreamRequest before it is built, then this API should be used.
   *
   * @param mimeSubType the mime subtype of the multipart to be used. For example, 'mixed' would result in a Content-Type of
   *                    'multipart/mixed'. It is generally good practice to use subtypes described in RFC 1341, although
   *                    this API does not enforce this.
   * @param writer the {@link com.linkedin.multipart.MultiPartMIMEWriter} to use for the payload of the request.
   * @param contentTypeParameters any additional parameters needed when constructing the Content-Type header.
   * @param streamRequestBuilder the {@link com.linkedin.r2.message.stream.StreamRequestBuilder} to begin with in order to
   *                             construct the final {@link com.linkedin.r2.message.stream.StreamRequest}.
   * @return the newly created {@link com.linkedin.r2.message.stream.StreamRequest}.
   */
  public static StreamRequest generateMultiPartMIMEStreamRequest(final String mimeSubType,
                                                                 final MultiPartMIMEWriter writer,
                                                                 final Map<String, String> contentTypeParameters,
                                                                 final StreamRequestBuilder streamRequestBuilder)
  {
    final String contentTypeHeader =
        MultiPartMIMEUtils.buildMIMEContentTypeHeader(mimeSubType.trim(), writer.getBoundary(), contentTypeParameters);
    streamRequestBuilder.addHeaderValue(MultiPartMIMEUtils.CONTENT_TYPE_HEADER, contentTypeHeader);
    return streamRequestBuilder.build(writer.getEntityStream());
  }

  private MultiPartMIMEStreamRequestFactory()
  {
  }
}