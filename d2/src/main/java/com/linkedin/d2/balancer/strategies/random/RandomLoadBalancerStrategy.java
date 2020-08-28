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
import com.linkedin.d2.balancer.util.hashing.HashFunction;
import com.linkedin.d2.balancer.util.hashing.RandomHash;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.annotation.Nonnull;

public class RandomLoadBalancerStrategy implements LoadBalancerStrategy
{
  public static final String RANDOM_STRATEGY_NAME = "random";

  private final Random _random;

  public RandomLoadBalancerStrategy()
  {
    _random = new Random();
  }

  @Nonnull
  @Override
  public Ring<URI> getRing(long clusterGenerationId, int partitionId, Map<URI, TrackerClient> trackerClients)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public HashFunction<Request> getHashFunction()
  {
    return new RandomHash();
  }

  @Override
  public TrackerClient getTrackerClient(Request request,
                                        RequestContext requestContext,
                                        long clusterGenerationId,
                                        int partitionId,
                                        Map<URI, TrackerClient> trackerClients)
  {
    int size = trackerClients.size();
    if (size > 0)
    {
      List<TrackerClient> trackerClientList = new ArrayList<>(trackerClients.values());
      return trackerClientList.get(_random.nextInt(trackerClients.size()));
    }

    return null;
  }

  @Override
  public String getName()
  {
    return RANDOM_STRATEGY_NAME;
  }
}
