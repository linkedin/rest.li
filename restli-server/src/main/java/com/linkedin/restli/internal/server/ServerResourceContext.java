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
import com.linkedin.restli.common.attachments.RestLiAttachmentReader;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiServiceException;

import java.net.HttpCookie;
import java.net.URI;
import java.util.List;
import java.util.Map;


/**
 * Richer resource context used inside server framework.
 *
 * @author Josh Walker
 * @version $Revision: $
 */
public interface ServerResourceContext extends ResourceContext
{
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
   * Set a {@link EntityStream} for this request.
   */
  void setEntityStream(EntityStream<ByteString> entityStream);

  /**
   * Returns the {@link EntityStream}. For any other cases, this returns null.
   */
  EntityStream<ByteString> getEntityStream();

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
}