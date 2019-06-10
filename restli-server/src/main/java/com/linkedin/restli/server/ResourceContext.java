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

package com.linkedin.restli.server;


import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;

import java.net.HttpCookie;
import java.util.List;
import java.util.Map;


/**
 * Captures nested/scoped resource context.
 *
 * @author dellamag
 */
public interface ResourceContext extends CustomRequestContext
{
  /**
   * Get the RestRequest which caused the current context to be created. The entity may not be populated.
   *
   * @return RestRequest for the current context
   * @deprecated No replacement. Application should avoid building any business logic based on the raw request.
   */
  @Deprecated
  RestRequest getRawRequest();

  /**
   * Get the HTTP request method for the current context.
   *
   * @return String representation of HTTP request method, per RFC 2616
   */
  String getRequestMethod();

  /**
   * Get the PathKeys parsed from the URI path.
   *
   * @return PathKeys for this context.
   */
  PathKeys getPathKeys();

  /**
   * Get the projection mask parsed from the query for root object entities.
   *
   * @return MaskTree parsed from query, or null if no root object projection mask was requested.
   */
  MaskTree getProjectionMask();

  /**
   * Get the projection mask parsed from the query for CollectionResult metadata
   *
   * @return MaskTree parsed from query, or null if no metadata projection mask was requested.
   */
  MaskTree getMetadataProjectionMask();

  /**
   * Get the projection mask parsed from the query for paging (CollectionMetadata)
   *
   * Note that there is no get/set projection mode for paging because paging is fully automatic. Clients can choose
   * whether or not to pass a non-null total in the CollectionResult based on their paging MaskTree, but restli will
   * always automatically project paging.
   *
   * @return MaskTree parsed from query, or null if no paging projection mask was requested.
   */
  MaskTree getPagingProjectionMask();

  /**
   * Check whether a given query parameter was present in the request.
   *
   * @param key - the name of the parameter
   * @return true if the request contains the specified parameter
   */
  boolean hasParameter(String key);

  /**
   * Get the value of a given query parameter from the request. If multiple values were
   * specified in the request, only the first will be returned.
   *
   * @param key - the name of the query parameter
   * @return value of the query parameter from the request, or null if the query parameter was not
   *         present in the request
   */
  String getParameter(String key);

  /**
   * Get the value a given query parameter from the request as Object. Supports structured
   * DataTemplate-backed query parameters.
   *
   * @param key - the name of the query parameter
   * @return value of the query parameter from the request, or null if the query parameter was not
   *         present in the request
   */
  Object getStructuredParameter(String key);

  /**
   * Get all values for a given query parameter from the request.
   *
   * @param key - the name of the query parameter
   * @return list of values for the query parameter in the request, or null if the query parameter was
   *         not present in the request
   */
  List<String> getParameterValues(String key);

  /**
   * Get all headers from the request.
   *
   * @return a map of header name -> header value
   */
  Map<String, String> getRequestHeaders();

  /**
   * Set a header to be sent in the response message.
   *
   * @param name - the name of the header
   * @param value - the value of the header
   */
  void setResponseHeader(String name, String value);

  /**
   * Retrieve the cookies from the underlying RestRequest
   *
   * @return the request cookie field
   */
  List<HttpCookie> getRequestCookies();

  /**
   * Pass the cookie to the underlying RestResponse through RestResponseBuilder
   *
   * @param cookie the cookie string to be processed
   */
  void addResponseCookie(HttpCookie cookie);

  /**
   * Get the RequestContext associated with this request.
   *
   * @return RequestContext for the current context
   */
  RequestContext getRawRequestContext();

  /**
   * Get the projection mode to be applied to the response body for root object entities.
   * @return Projection mode for the response body for root object entities.
   */
  ProjectionMode getProjectionMode();

  /**
   * Set the projection mode to be applied to the response body for root object entities.
   * @param mode Projection mode for the response body for root object entities.
   */
  void setProjectionMode(ProjectionMode mode);

  /**
   * Get the projection mode to be applied to the response body for the CollectionResult metadata.
   * @return Projection mode for the response body for the CollectionResult metadata.
   */
  ProjectionMode getMetadataProjectionMode();

  /**
   * Set the projection mode to be applied to the response body for the CollectionResult metadata.
   * @param mode Projection mode for the response body for the CollectionResult metadata.
   */
  void setMetadataProjectionMode(ProjectionMode mode);

  /**
   * Returns whether or not attachments are permissible to send back in the response to the client. This is based on
   * whether or not the client specified they could handle attachments in the Accept-Type header of their request. Users
   * of this API should first check this, and if this returns true, continue by using
   * {@link ResourceContext#setResponseAttachments(com.linkedin.restli.server.RestLiResponseAttachments)}.
   *
   * @return true if response attachments are permissible and false if they are not.
   */
  boolean responseAttachmentsSupported();

  /**
   * Sets the {@link com.linkedin.restli.server.RestLiResponseAttachments} to be attached and sent back in the response
   * to the client's request. Note that this can only be used if {@link ResourceContext#responseAttachmentsSupported()}
   * returns true. Failure to follow this will result in an  {@link java.lang.IllegalStateException}.
   *
   * @param responseAttachments the {@link com.linkedin.restli.server.RestLiResponseAttachments} to send back in the response.
   */
  void setResponseAttachments(final RestLiResponseAttachments responseAttachments) throws IllegalStateException;

  /**
   * Get the {@link com.linkedin.restli.server.RestLiResponseAttachments} which will be sent back in the response.
   *
   * @return the {@link com.linkedin.restli.server.RestLiResponseAttachments}.
   */
  RestLiResponseAttachments getResponseAttachments();

  /**
   * @deprecated Use {@link #isReturnEntityRequested()} instead.
   * @return whether the request specifies that the resource should return an entity
   */
  @Deprecated
  boolean shouldReturnEntity();

  /**
   * Returns whether or not the client is requesting that the entity (or entities) be returned. Reads the appropriate
   * query parameter to determine this information, defaults to true if the query parameter isn't present, and throws
   * an exception if the parameter's value is not a boolean value. Keep in mind that the value of this method is
   * inconsequential if the resource method at hand doesn't have a "return entity" method signature.
   *
   * @return whether the request specifies that the resource should return an entity
   */
  boolean isReturnEntityRequested();
}