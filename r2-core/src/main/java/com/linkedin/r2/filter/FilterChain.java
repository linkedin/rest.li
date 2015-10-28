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

package com.linkedin.r2.filter;

import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.filter.message.stream.StreamFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;

import java.util.Map;

/**
 * A chain of {@link RestFilter}s and {@link StreamFilter}s that get a chance to process or modify the request/response,
 * wire attributes, and local attributes. The {@link RestFilter}s would only be applied to rest request/response. The
 * {@link StreamFilter}s would only be applied to stream request/response. <p/>
 *
 * <dl>
 *  <dt>Immutability</dt>
 *  <dd>
 *      Filter chains are immutable. Adding a new filter does not change the filter chain but
 *      instead produces a copy of the filter chain with the new filter inserted.
 *  </dd>
 *
 *  <dt>Filter Order</dt>
 *  <dd>
 *      Requests are processed starting from the beginning of the chain and move towards the end of
 *      the filter chain. Responses are processed from the end of the filter chain and move back
 *      towards the beginning of the filter chain.<p/>
 *
 *      If a request filter throws an exception, calls {@code nextFilter.onResponse(...)}, or
 *      calls {@code nextFilter.onError(..)} then that filter will have the appropriate response
 *      method invoked (onResponse or onError). The response will flow back towards the beginning
 *      of the filter chain. Filters later in the chain will not get a change to process the
 *      request or the response.
 *  </dd>
 *
 *  <dt>Wire Attributes</dt>
 *  <dd>
 *      Wire attributes provide a mechanism for sending request or response metadata to the remote
 *      endpoint. As an example, this facility can be used to send a unique request identifier from
 *      a client to a server.
 *  </dd>
 *
 *  <dt>Local Attributes</dt>
 *  <dd>
 *      Filters must be stateless, so local attributes provide a mechanism for saving some state
 *      during the request which can be used during response processing. As an example, this
 *      facility can be used to store the time at which a request began so that total request
 *      latency can be logged when the response is received.
 *  </dd>
 * </dl>
 *
 * @author Chris Pettitt
 * @author Zhenkai Zhu
 */
public interface FilterChain
{
  /**
   * Returns a copy of this filter chain with the supplied filter inserted at the beginning of the
   * chain.
   *
   * @param filter the filter to insert
   * @return the new filter chain
   * @throws java.lang.IllegalArgumentException if filter is null
   */
  FilterChain addFirstRest(RestFilter filter);

  /**
   * Returns a copy of this filter chain with the supplied filter inserted at the end of the filter
   * chain.
   *
   * @param filter the filter to insert
   * @return the new filter chain
   * @throws java.lang.IllegalArgumentException if filter is null
   */
  FilterChain addLastRest(RestFilter filter);

  /**
   * Returns a copy of this filter chain with the supplied filter inserted at the beginning of the
   * chain.
   *
   * @param filter the filter to insert
   * @return the new filter chain
   * @throws java.lang.IllegalArgumentException if filter is null
   */
  FilterChain addFirst(StreamFilter filter);

  /**
   * Returns a copy of this filter chain with the supplied filter inserted at the end of the filter
   * chain.
   *
   * @param filter the filter to insert
   * @return the new filter chain
   * @throws java.lang.IllegalArgumentException if filter is null
   */
  FilterChain addLast(StreamFilter filter);

  /**
   * Runs the request through the filter chain with the supplied wire attributes and local
   * attributes. See interface-level documentation for details about wire attributes and local
   * attributes.
   *
   * @param req the request to send through the filter chain
   * @param requestContext context for the request
   * @param wireAttrs the initial set of wire attributes
   */
  void onRestRequest(RestRequest req,
                     RequestContext requestContext,
                     Map<String, String> wireAttrs);

  /**
   * Runs the response through the filter chain with the supplied wire attributes and local
   * attributes. See interface-level documentation for details about wire attributes and local
   * attributes.
   *
   * @param res the response to send through the filter chain
   * @param requestContext context for the request
   * @param wireAttrs the initial set of wire attributes
   */
  void onRestResponse(RestResponse res,
                      RequestContext requestContext,
                      Map<String, String> wireAttrs);

  /**
   * Runs the error through the filter chain with the supplied wire attributes and local
   * attributes. See interface-level documentation for details about wire attributes and local
   * attributes.
   *
   * @param ex the error to send through the filter chain
   * @param requestContext context for the request
   * @param wireAttrs the initial set of wire attributes
   */
  void onRestError(Exception ex,
                   RequestContext requestContext,
                   Map<String, String> wireAttrs);

  /**
   * Runs the request through the filter chain with the supplied wire attributes and local
   * attributes. See interface-level documentation for details about wire attributes and local
   * attributes.
   *
   * @param req the request to send through the filter chain
   * @param requestContext context for the request
   * @param wireAttrs the initial set of wire attributes
   */
  void onStreamRequest(StreamRequest req,
                     RequestContext requestContext,
                     Map<String, String> wireAttrs);

  /**
   * Runs the response through the filter chain with the supplied wire attributes and local
   * attributes. See interface-level documentation for details about wire attributes and local
   * attributes.
   *
   * @param res the response to send through the filter chain
   * @param requestContext context for the request
   * @param wireAttrs the initial set of wire attributes
   */
  void onStreamResponse(StreamResponse res,
                      RequestContext requestContext,
                      Map<String, String> wireAttrs);

  /**
   * Runs the error through the filter chain with the supplied wire attributes and local
   * attributes. See interface-level documentation for details about wire attributes and local
   * attributes.
   *
   * @param ex the error to send through the filter chain
   * @param requestContext context for the request
   * @param wireAttrs the initial set of wire attributes
   */
  void onStreamError(Exception ex,
                   RequestContext requestContext,
                   Map<String, String> wireAttrs);
}
