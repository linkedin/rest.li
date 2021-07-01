/*
   Copyright (c) 2021 LinkedIn Corp.

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

package com.linkedin.d2.balancer.util.hashing;

import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.r2.message.Request;
import com.linkedin.util.degrader.CallTrackerImpl;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * The util class that picks the host from the hash ring.
 *
 * The original power of two algorithm will always pick the host that has lower load between the 2 random hosts,
 * however, this makes the host with highest load always receives the least load in the next interval, and the received
 * traffic keeps going up and down. To make the traffic more stable and make a host easy to recover, we just pick the
 * host using load-based probability. If host A has load x, and host B has load y, the probability of host A being
 * selected is y / (x + y).
 */
public class PowerOfTwoHashRingUtil
{
  public static TrackerClient getTrackerClientPowerOfTwo(Request request, Ring<URI> ring,
      Map<URI, TrackerClient> trackerClients, HashFunction<Request> requestHashFunction, Set<URI> excludedUris)
  {
    int hashCode1 = requestHashFunction.hash(request);
    int hashCode2 = requestHashFunction.hash(request);
    URI uri1 = ring.get(hashCode1);
    URI uri2 = ring.get(hashCode2);

    TrackerClient trackerClient1 = trackerClients.get(uri1);
    TrackerClient trackerClient2 = trackerClients.get(uri2);
    TrackerClient trackerClient;
    if (trackerClient1 == null || trackerClient2 == null) {
      trackerClient = trackerClient1 == null ? trackerClient2 : trackerClient1;
    } else
    {
      if (shouldPickFirstHost(trackerClient1.getLatestCallStats().getServerReportedLoad(),
            trackerClient2.getLatestCallStats().getServerReportedLoad())) {
        trackerClient = trackerClient1;
      } else
      {
        trackerClient = trackerClient2;
      }
    }

    if (trackerClient == null || excludedUris.contains(trackerClient.getUri()))
    {
      // Find next available URI.
      Iterator<URI> ringIterator = ring.getIterator(hashCode1);

      while (ringIterator.hasNext())
      {
        URI uri = ringIterator.next();
        trackerClient = trackerClients.get(uri);

        if (trackerClient != null && !excludedUris.contains(uri))
        {
          break;
        } else {
          trackerClient = null;
        }
      }
    }
    return trackerClient;
  }

  static boolean shouldPickFirstHost(int serverReportedLoad1, int serverReportedLoad2)
  {
    if (serverReportedLoad1 == CallTrackerImpl.DEFAULT_SERVER_REPORTED_LOAD
        || serverReportedLoad2 == CallTrackerImpl.DEFAULT_SERVER_REPORTED_LOAD
        || serverReportedLoad1 + serverReportedLoad2 == 0)
    {
      // If the server side is not sending meaningful reported load yet, we just pick one randomly
      double random = Math.random();
      return random < 0.5;
    }
    double pickFirstOneProbability = (double) serverReportedLoad2 / (serverReportedLoad1 + serverReportedLoad2);
    double random = Math.random();
    return random <= pickFirstOneProbability;
  }
}
