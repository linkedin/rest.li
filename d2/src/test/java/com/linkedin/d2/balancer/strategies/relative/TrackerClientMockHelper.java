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

package com.linkedin.d2.balancer.strategies.relative;

import com.linkedin.common.stats.LongStats;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.util.degrader.CallTracker;
import com.linkedin.util.degrader.CallTrackerImpl;
import com.linkedin.util.degrader.ErrorType;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mockito.Mockito;

import static org.mockito.Matchers.anyInt;


/**
 * The helper class that builds quick mock of {@link TrackerClient}
 */
public class TrackerClientMockHelper
{
  public static List<TrackerClient> mockTrackerClients(int numTrackerClients)
  {
    List<TrackerClient> trackerClients = new ArrayList<>();
    for (int index = 0; index < numTrackerClients; index ++)
    {
      URI uri = URI.create("URI/" + index);
      TrackerClient trackerClient = Mockito.mock(TrackerClient.class);
      Mockito.when(trackerClient.getCallTracker()).thenReturn(new CallTrackerImpl(RelativeLoadBalancerStrategyFactory.DEFAULT_UPDATE_INTERVAL_MS));
      Mockito.when(trackerClient.getUri()).thenReturn(uri);
      Mockito.when(trackerClient.getPartitionWeight(anyInt())).thenReturn(1.0);
      trackerClients.add(trackerClient);
    }
    return trackerClients;
  }

  public static List<TrackerClient> mockTrackerClients(int numTrackerClients, List<Integer> callCountList,
      List<Integer> outstandingCallCountList, List<Long> latencyList, List<Long> outstandingLatencyList,
      List<Integer> errorCountList)
  {
    List<TrackerClient> trackerClients = new ArrayList<>();
    for (int index = 0; index < numTrackerClients; index ++)
    {
      URI uri = URI.create("URI/" + index);
      TrackerClient trackerClient = Mockito.mock(TrackerClient.class);
      CallTracker callTracker = Mockito.mock(CallTracker.class);
      LongStats longStats = new LongStats(callCountList.get(index), latencyList.get(index), 0, 0, 0, 0, 0, 0, 0);
      Map<ErrorType, Integer> errorTypeCounts = new HashMap<>();
      errorTypeCounts.put(ErrorType.SERVER_ERROR, errorCountList.get(index));

      CallTrackerImpl.CallTrackerStats callStats = new CallTrackerImpl.CallTrackerStats(RelativeLoadBalancerStrategyFactory.DEFAULT_UPDATE_INTERVAL_MS,
      0,
          RelativeLoadBalancerStrategyFactory.DEFAULT_UPDATE_INTERVAL_MS,
      callCountList.get(index),
      0,
      0,
      errorCountList.get(index),
      errorCountList.get(index),
      1,
          RelativeLoadBalancerStrategyFactory.DEFAULT_UPDATE_INTERVAL_MS - outstandingLatencyList.get(index),
      outstandingCallCountList.get(index),
      longStats,
      errorTypeCounts,
      errorTypeCounts);

      Mockito.when(trackerClient.getCallTracker()).thenReturn(callTracker);
      Mockito.when(callTracker.getCallStats()).thenReturn(callStats);
      Mockito.when(trackerClient.getUri()).thenReturn(uri);
      Mockito.when(trackerClient.getPartitionWeight(anyInt())).thenReturn(1.0);
      trackerClients.add(trackerClient);
    }
    return trackerClients;
  }
}
