/*
   Copyright (c) 2012 LinkedIn Corp.

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

/**
 * $Id: $
 */

package com.linkedin.restli.internal.server;


import com.linkedin.data.ByteString;
import com.linkedin.data.DataMap;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.entitystream.EntityStream;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.attachments.RestLiAttachmentReader;
import com.linkedin.restli.server.LocalRequestProjectionMask;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiServiceException;

import java.net.HttpCookie;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Richer resource context used inside server framework.
 *
 * @author Josh Walker
 * @version $Revision: $
 */
public interface ServerResourceContext extends ResourceContext
{
  /**
   * Local attribute key for request cookies. Value should be a {@link List} of {@link HttpCookie} objects.
   */
  String CONTEXT_COOKIES_KEY = ServerResourceContext.class.getName() + ".cookie";

  /**
   * Local attribute key for query params datamap. Value should be a {@link DataMap}.
   */
  String CONTEXT_QUERY_PARAMS_KEY = ServerResourceContext.class.getName() + ".queryParams";

  /**
   * Local attribute key for projection masks. Value should be a
   * {@link LocalRequestProjectionMask} instance.
   */
  String CONTEXT_PROJECTION_MASKS_KEY = ServerResourceContext.class.getName() + ".projectionMasks";

  /**
   * Local attribute key to indicate that this request will be served by an in-process Rest.li server. Value must
   * be a {@link Boolean}.
   */
  String CONTEXT_IN_PROCESS_RESOLUTION_KEY = ServerResourceContext.class.getName() + ".inProcessResolution";

  /**
   * Local attribute key for protocol version used by the request. Value must be a {@link ProtocolVersion}.
   */
  String CONTEXT_PROTOCOL_VERSION_KEY = ServerResourceContext.class.getName() + ".protocolVersion";

  /**
   * @return {@link DataMap} of request parameters.
   */
  DataMap getParameters();

  /**
   * @return request {@link URI}
   */
  URI getRequestURI();

  /**
   * @return rest.li request action name
   */
  String getRequestActionName();

  /**
   * @return rest.li request finder name
   */
  String getRequestFinderName();

  /**
   * @return rest.li request batch finder name
   */
  String getRequestBatchFinderName();

  /**
   * @return rest.li request method name from the Method type
   */
  String getMethodName(ResourceMethod type);

  /**
   * @return rest.li request method name
   */
  String getMethodName();

  @Override
  MutablePathKeys getPathKeys();

  /**
   * @return response headers
   */
  Map<String, String> getResponseHeaders();

  /**
   * function to retrive the response cookie
   *
   * @return response cookies
   */
  List<HttpCookie> getResponseCookies();

  /**
   * @return map of {@link RestLiServiceException}s keyed by batch key values
   */
  Map<Object, RestLiServiceException> getBatchKeyErrors();

  /**
   * @return rest.li request method
   */
  String getRestLiRequestMethod();

  /**
   * @return Rest.li protocol version used by the client sending the request
   */
  ProtocolVersion getRestliProtocolVersion();

  /**
   * Sets the MIME type of the request. This is the value from the Content-Type header.
   */
  void setRequestMimeType(String type);

  /**
   * Gets the MIME type of the request. This is the value from the Content-Type header and it can be null.
   */
  String getRequestMimeType();

  /**
   * Set the MIME type that that has been chosen as the response MIME type.
   * @param type Selected MIME type.
   */
  void setResponseMimeType(String type);

  /**
   * @return response MIME type.
   */
  String getResponseMimeType();

  /**
   * Sets the {@link RestLiAttachmentReader}. The attachment reader is not available when the ServerResourceContext is
   * constructed during routing and can only be set after the stream content is inspected.
   */
  void setRequestAttachmentReader(RestLiAttachmentReader requestAttachmentReader);

  /**
   * Returns a {@link com.linkedin.restli.common.attachments.RestLiAttachmentReader} if there are attachments
   * available to asynchronously walk through in the incoming request. If no attachments are present in the incoming
   * request, null is returned.
   *
   * @return the {@link com.linkedin.restli.common.attachments.RestLiAttachmentReader} to walk through all possible attachments
   * in the request if any exist, or null otherwise.
   */
  RestLiAttachmentReader getRequestAttachmentReader();

  /**
   * Set a {@link EntityStream} for this response.
   */
  void setResponseEntityStream(EntityStream<ByteString> entityStream);

  /**
   * Returns the response's {@link EntityStream}. For any other cases, this returns null.
   */
  EntityStream<ByteString> getResponseEntityStream();

  /**
   * Set a {@link EntityStream} for this request.
   */
  void setRequestEntityStream(EntityStream<ByteString> entityStream);

  /**
   * Returns the request's {@link EntityStream}. For any other cases, this returns null.
   */
  EntityStream<ByteString> getRequestEntityStream();


  /**
   * Sets the specified projection mask for root object entities in the response. Setting the projection mask to
   * {@code null} implies all fields should be projected.
   *
   * @param projectionMask Projection mask to use for root object entities
   */
  void setProjectionMask(MaskTree projectionMask);

  /**
   * Sets the specified projection mask for CollectionResult metadata in the response. Setting the projection mask to
   * {@code null} implies all fields should be projected.
   *
   * @param metadataProjectionMask Projection mask to use for CollectionResult metadata
   */
  void setMetadataProjectionMask(MaskTree metadataProjectionMask);

  /**
   * Sets the specified projection mask for paging metadata in the response (applies only for collection responses).
   * Setting the projection mask to {@code null} implies all fields should be projected.
   *
   * @param pagingProjectionMask Projection mask to use for paging metadata
   */
  void setPagingProjectionMask(MaskTree pagingProjectionMask);

  /**
   * Sets the list of fields that should be included when projection mask is applied. These fields override the
   * projection mask and will be included in the result even if the mask doesn't request them.
   *
   * @param alwaysProjectedFields Set of fields to include when projection is applied.
   */
  void setAlwaysProjectedFields(Set<String> alwaysProjectedFields);
}