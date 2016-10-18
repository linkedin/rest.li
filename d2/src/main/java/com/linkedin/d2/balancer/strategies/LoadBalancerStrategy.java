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
import java.util.HashSet;
import java.util.List;
import java.util.Set;


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

  /**
   * Shutdown loadBalanceStrategy
   */
  default void shutdown()
  {
  }

  class ExcludedHostHints
  {
    private static final String EXCLUDED_HOST_KEY_NAME = "D2-Hint-ExcludedHosts";

    /**
     * Inserts a hint in RequestContext instructing D2 to avoid specified hosts. This hint can hold a set of
     * hosts, and the request won't be routed to any of hosts in the set. This method adds one host to the set.
     * Warning: This is an internal D2 hint. Please do not use it outside.
     * @param context RequestContext for the request which will be made
     * @param excludedHost host's URI to be added to the set
     */
    public static void addRequestContextExcludedHost(RequestContext context, URI excludedHost)
    {
      Set<URI> excludedHosts = getRequestContextExcludedHosts(context);
      if (excludedHosts == null)
      {
        excludedHosts = new HashSet<URI>();
        context.putLocalAttr(EXCLUDED_HOST_KEY_NAME, excludedHosts);
      }
      excludedHosts.add(excludedHost);
    }

    /**
     * Retrieve the excluded hosts hint in the RequestContext, returning it if found, or null if no
     * hint is present.
     * @param context RequestContext for the request
     * @return Set of excluded hosts
     */
    @SuppressWarnings("unchecked")
    public static Set<URI> getRequestContextExcludedHosts(RequestContext context)
    {
      return (Set<URI>)context.getLocalAttr(EXCLUDED_HOST_KEY_NAME);
    }

    /**
     * Clear the excluded hosts hint from RequestContext.
     * @param context RequestContest from which the hint to be removed
     */
    public static void clearRequestContextExcludedHosts(RequestContext context)
    {
      context.removeLocalAttr(EXCLUDED_HOST_KEY_NAME);
    }
  }
}
