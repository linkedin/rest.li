/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.restli.client;

import com.linkedin.common.callback.Callback;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.util.URIKeyPair;
import com.linkedin.d2.balancer.util.URIMappingResult;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.restli.common.ProtocolVersion;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Request decompose and aggregation strategy interface. We provided {@link DefaultScatterGatherStrategy} as
 * default scatter-gather strategy for non BATCH_CREATE batch request. Users can implement customized strategy to
 * handle their own special partition needs. In most cases, custom ScatterGatherStrategy can be implemented by
 * override {@link DefaultScatterGatherStrategy#getUris(Request, ProtocolVersion)} and
 * {@link DefaultScatterGatherStrategy#onAllResponsesReceived(Request, ProtocolVersion, Map, Map, Set, Callback)}.
 *
 * @author mnchen
 */
public interface ScatterGatherStrategy
{

  /**
   * Check if the given request needs scatter gather. We need to perform scatter-gather for the given request
   * 1) when the service has specified partitioning or sticky routing.
   * 2) custom strategy can handle scatter-gather for the given request.
   * @param request incoming request.
   * @return true if we need to support scatter gather for this request.
   */
  <T> boolean needScatterGather(Request<T> request);

  /**
   * Prepare a list of URIs to be scattered. Each URI needs to be associated with a resource key.
   * These URIs are used to determine partitioning and stickiness the same way they are determined
   * for normal requests. Alternatively, each URI can (optionally) be associated with a set of partition
   * Ids to bypass the partitioning by D2 later, in which case a singleton list of URIKeyPair should be
   * returned whose resource key has to be null.
   *
   * @param request rest.li request to be scattered
   * @param version protocol version
   * @param <K> batch request key type.
   * @return List of (URI, key) pair, where URI in each pair is the request uri for the individual key.
   */
  <K, T> List<URIKeyPair<K>> getUris(Request<T> request, ProtocolVersion version);

  /**
   * Maps a request to several hosts according to the underlying load balancing strategy.
   * @param uris List of (URI, key) pair, where URI in each pair is an individual request uri.
   * @param <K> batch request key type.
   * @return list of URI mapping result, including both mapped and unmapped keys.
   * @throws ServiceUnavailableException if the service is unavailable.
   */
  <K> URIMappingResult<K> mapUris(List<URIKeyPair<K>> uris) throws ServiceUnavailableException;

  /**
   * Disassemble a request to individual request per key or other custom partition ids, given d2 routing information.
   * Returns a map of {@link Request}, one per host to be sent to. Keys routed to this host will be
   * included in this request by setting target host hint.
   * @param request The request to be disassembled
   * @param mappingKeys mapping between target host and mapped batch keys. An empty set in the entry value
   *                    indicates the case where custom partition ids are specified in {@link URIKeyPair}.
   * @param <K> batch request key type.
   */
  <K, T> List<RequestInfo> scatterRequest(Request<T> request, RequestContext requestContext,
                                          Map<URI, Set<K>> mappingKeys);


  /**
   * Merge all responses from scattered requests and unmapped keys into a final response, and invoke callback based
   * on your business needs. This method should normally perform the following steps:
   * <p><ul>
   * <li>Initialize an empty final response container</li>
   * <li>Accumulate success response</li>
   * <li>Accumulate failure response (exceptions)</li>
   * <li>Accumulate unmapped keys and handle them</li>
   * <li>Invoke callback when all scattered responses arrive</li>
   * </ul></p>
   * @param request original request
   * @param protocolVersion rest.li protocol version
   * @param successResponses map of successful scattered request and its response
   * @param failureResponses map of failure scattered request and its error
   * @param unmappedKeys unmapped keys (may be empty for non-batch requests)
   * @param callback callback to invoke on completion
   * @param <K> resource key type
   * @param <T> response type
   */
  <K, T> void onAllResponsesReceived(Request<T> request, ProtocolVersion protocolVersion,
                                     Map<RequestInfo, Response<T>> successResponses,
                                     Map<RequestInfo, Throwable> failureResponses,
                                     Set<K> unmappedKeys,
                                     Callback<Response<T>> callback);
}
