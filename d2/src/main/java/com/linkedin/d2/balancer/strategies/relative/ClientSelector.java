/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.d2.balancer.strategies.relative;

import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.util.hashing.HashFunction;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Select host from the hash ring for a request
 */
public class ClientSelector
{
  private static final Logger LOG = LoggerFactory.getLogger(ClientSelector.class.getName());

  private final HashFunction<Request> _requestHashFunction;

  public ClientSelector(HashFunction<Request> requestHashFunction)
  {
    _requestHashFunction = requestHashFunction;
  }

  /**
   * Pick a {@link TrackerClient} from the ring for the given request
   *
   * @param request The request to be routed by D2
   * @param requestContext The request context of the request
   * @param ring A hash ring of URIs
   * @param trackerClients A list of server tracker clients to pick
   * @return The picked server to route the traffic to
   */
  @Nullable
  public TrackerClient getTrackerClient(Request request,
                                        RequestContext requestContext,
                                        Ring<URI> ring,
                                        Map<URI, TrackerClient> trackerClients)
  {
    TrackerClient trackerClient;

    URI targetHostUri = KeyMapper.TargetHostHints.getRequestContextTargetHost(requestContext);

    if (targetHostUri != null)
    {
      trackerClient = getTrackerClientFromTarget(targetHostUri, requestContext, trackerClients);
    }
    else
    {
      trackerClient = getTrackerClientFromRing(request, requestContext, ring, trackerClients);
    }
    addToExcludedHosts(trackerClient, requestContext);

    return trackerClient;
  }

  private void addToExcludedHosts(TrackerClient trackerClient, RequestContext requestContext)
  {
    if (trackerClient != null)
    {
      LoadBalancerStrategy.ExcludedHostHints.addRequestContextExcludedHost(requestContext, trackerClient.getUri());
    }
  }

  private TrackerClient getTrackerClientFromTarget(URI targetHostUri, RequestContext requestContext, Map<URI, TrackerClient> trackerClients)
  {
    TrackerClient trackerClient = trackerClients.get(targetHostUri);
    if (trackerClient == null)
    {
      LOG.warn("No client found for ", targetHostUri, ". Target host specified is no longer part of cluster");
    }

    return trackerClient;
  }

  private TrackerClient getTrackerClientFromRing(Request request,
                                                 RequestContext requestContext,
                                                 Ring<URI> ring,
                                                 Map<URI, TrackerClient> trackerClients)
  {
    Set<URI> excludedUris = LoadBalancerStrategy.ExcludedHostHints.getRequestContextExcludedHosts(requestContext) == null
        ? new HashSet<>()
        : LoadBalancerStrategy.ExcludedHostHints.getRequestContextExcludedHosts(requestContext);
    int hashCode = _requestHashFunction.hash(request);
    URI uri = ring.get(hashCode);

    TrackerClient trackerClient = trackerClients.get(uri);

    if (trackerClient == null || excludedUris.contains(uri))
    {
      // Find next available URI.
      Iterator<URI> ringIterator = ring.getIterator(hashCode);

      while (ringIterator.hasNext())
      {
        uri = ringIterator.next();
        trackerClient = trackerClients.get(uri);

        if (trackerClient != null && !excludedUris.contains(uri))
        {
          break;
        }
      }
      trackerClient = null;
    }

    if (trackerClient == null)
    {
      // Pick one from the tracker clients passed from the request if the ring is completely out of date
      trackerClient = trackerClients.values().stream()
          .filter(latestTrackerClient -> !excludedUris.contains(latestTrackerClient.getUri()))
          .findAny().orElse(null);
      if (trackerClient != null)
      {
        LOG.warn("Did not find a valid client from the ring, picked {} instead", trackerClient.getUri());
      }
    }

    return trackerClient;
  }

  /**
   * Get the hash function of the hash ring
   *
   * @return The hash function of the hash ring
   */
  public HashFunction<Request> getRequestHashFunction()
  {
    return _requestHashFunction;
  }
}
