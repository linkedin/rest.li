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

package com.linkedin.d2.balancer.strategies.random;

import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;

import java.net.URI;
import java.util.List;
import java.util.Random;

public class RandomLoadBalancerStrategy implements LoadBalancerStrategy
{
  private final Random _random;

  public RandomLoadBalancerStrategy()
  {
    _random = new Random();
  }

  @Override
  public Ring<URI> getRing(long clusterGenerationId, int partitionId, List<TrackerClient> trackerClients)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public TrackerClient getTrackerClient(Request request,
                                        RequestContext requestContext,
                                        long clusterGenerationId,
                                        int partitionId,
                                        List<TrackerClient> trackerClients)
  {
    if (trackerClients.size() > 0)
    {
      return trackerClients.get(_random.nextInt(trackerClients.size()));
    }

    return null;
  }
}
