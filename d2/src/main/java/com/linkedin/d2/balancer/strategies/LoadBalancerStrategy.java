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

package com.linkedin.d2.balancer.strategies;

import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;

import java.net.URI;
import java.util.List;


/**
 * Interface for a strategy to choose a client given a list of clients.
 *
 * @author David Hoa (dhoa@linkedin.com)
 * @author Oby Sumampouw (osumampouw@linkedin.com)
 */
public interface LoadBalancerStrategy
{

  /**
   * Given a list of tracker clients this return one tracker client to use
   *
   * @param request
   * @param requestContext
   * @param clusterGenerationId
   * @param partitionId
   * @param trackerClients
   * @return TrackerClient
   */
  TrackerClient getTrackerClient(Request request,
                                 RequestContext requestContext,
                                 long clusterGenerationId,
                                 int partitionId,
                                 List<TrackerClient> trackerClients);

  /**
   * Returns a ring that can be used to choose a host. The ring will contain all the
   * tracker clients passed as the argument.
   * This method is optional hence some implementation may throw UnsupportedOperationException.
   *
   * Certain implementation of this interface updates its internal state using the trackerClients
   * passed in.
   *
   * @param clusterGenerationId
   * @param partitionId
   * @param trackerClients
   * @return Ring
   */
  Ring<URI> getRing(long clusterGenerationId,
                    int partitionId,
                    List<TrackerClient> trackerClients);
}
