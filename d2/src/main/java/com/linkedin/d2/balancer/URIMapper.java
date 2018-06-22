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

package com.linkedin.d2.balancer;

import com.linkedin.d2.balancer.util.URIKeyPair;
import com.linkedin.d2.balancer.util.URIMappingResult;
import java.util.List;


public interface URIMapper
{
  /**
   * This methods tries to map d2 requests to hosts based on the underlying strategy. e.g. consistent hashing
   *
   * The requests should be destined for the same service.
   *
   * Each request in the input list will appear in exactly one routing group in the output. Requests in the same routing group will be routed to the same host.
   * The requests in output routing groups are mutually exclusive and collectively exhaustive.
   *
   * NOTE: in context of sticky routing, the routing decision will be made based on request uri instead of keys. This achieves universal stickiness.
   *
   * @param <KEY> type of resource key
   * @param requestUriKeyPairs a list of URIKeyPair, each contains a d2 request uri and a resource key. The resource keys should be unique.
   * @return {@link URIMappingResult} that contains a mapping of host to a set of keys whose corresponding requests will be sent to that host
   *          and a set of unmapped keys.
   * @throws ServiceUnavailableException if the requested service cannot be found
   */
  <KEY> URIMappingResult<KEY> mapUris(List<URIKeyPair<KEY>> requestUriKeyPairs) throws ServiceUnavailableException;

  /**
   * Returns true if sticky routing is enabled (inclusive) OR the cluster of the service has more than one partitions.
   *
   * If sticky routing is enabled, scatter-gather is needed since different keys can be routed to different hosts.
   * If cluster has more than one partitions, scatter-gather is needed since different keys can be routed to different partitions.
   *
   * @param serviceName
   * @return true if sticky routing OR partitioning is enabled.
   * @throws ServiceUnavailableException if the requested service cannot be found
   */
  boolean needScatterGather(String serviceName) throws ServiceUnavailableException;
}
