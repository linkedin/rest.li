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

import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;

import java.util.Map;

/**
 * A chain of {@link Filter}s that get a chance to process or modify the request, wire attributes,
 * and local attributes.<p/>
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
 *  <dt>Filter Priority</dt>
 *  <dd>
 *      It is possible to implement a generic message filter and a message filter specific to a
 *      request type (RPC or REST). In general, this should be avoided. If a filter implements both
 *      types of interfaces then the more specific one is invoked and the less specific one is
 *      ignored.
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
 */
public interface FilterChain
{
  /**
   * Returns a copy of this filter chain with the supplied filter inserted at the beginning of the
   * chain.
   *
   * @param filter the filter to insert
   * @return the new filter chain
   */
  FilterChain addFirst(Filter filter);

  /**
   * Returns a copy of this filter chain with the supplied filter inserted at the end of the filter
   * chain.
   *
   * @param filter the filter to insert
   * @return the new filter chain
   */
  FilterChain addLast(Filter filter);

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
}
