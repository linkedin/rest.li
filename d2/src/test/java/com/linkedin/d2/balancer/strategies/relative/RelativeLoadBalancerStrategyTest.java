/*
   Copyright (c) 2026 LinkedIn Corp.

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

import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.util.hashing.RandomHash;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;


/**
 * Tests that {@link RelativeLoadBalancerStrategy} passes the right concrete {@code Collection} type
 * to {@link StateUpdater#updateState} based on the {@code enableRelativeStrategyDeferredAllocation}
 * flag.
 */
public class RelativeLoadBalancerStrategyTest
{
  private static final long CLUSTER_GENERATION_ID = 0L;
  private static final int PARTITION_ID = 0;

  private static class Fixture
  {
    final Map<URI, TrackerClient> trackerClients;
    final StateUpdater stateUpdater;
    final ClientSelector clientSelector;

    Fixture(int clientCount)
    {
      this.trackerClients = buildTrackerClients(clientCount);
      URI anyUri = trackerClients.keySet().iterator().next();
      Ring<URI> ring = Mockito.mock(Ring.class);
      Mockito.when(ring.get(anyInt())).thenReturn(anyUri);
      this.stateUpdater = Mockito.mock(StateUpdater.class);
      Mockito.when(stateUpdater.getRing(anyInt())).thenReturn(ring);
      this.clientSelector = new ClientSelector(new RandomHash());
    }
  }

  @Test
  public void testFlagOffPassesHashSet()
  {
    Fixture f = new Fixture(2);
    RelativeLoadBalancerStrategy strategy =
        new RelativeLoadBalancerStrategy(f.stateUpdater, f.clientSelector, false /* deferred off */);

    strategy.getTrackerClient(Mockito.mock(Request.class), new RequestContext(),
        CLUSTER_GENERATION_ID, PARTITION_ID, f.trackerClients, false);

    Collection<TrackerClient> captured = captureUpdateStateArg(f.stateUpdater);
    assertTrue(captured instanceof Set, "flag off should wrap into a Set (HashSet) at the call site");
  }

  @Test
  public void testFlagOnPassesValuesView()
  {
    Fixture f = new Fixture(2);
    RelativeLoadBalancerStrategy strategy =
        new RelativeLoadBalancerStrategy(f.stateUpdater, f.clientSelector, true /* deferred on */);

    strategy.getTrackerClient(Mockito.mock(Request.class), new RequestContext(),
        CLUSTER_GENERATION_ID, PARTITION_ID, f.trackerClients, false);

    Collection<TrackerClient> captured = captureUpdateStateArg(f.stateUpdater);
    assertFalse(captured instanceof Set,
        "flag on should pass Map.values() (a non-Set Collection) without wrapping");
  }

  @Test
  public void testGetRingFlagOffPassesHashSet()
  {
    Fixture f = new Fixture(2);
    RelativeLoadBalancerStrategy strategy =
        new RelativeLoadBalancerStrategy(f.stateUpdater, f.clientSelector, false);

    strategy.getRing(CLUSTER_GENERATION_ID, PARTITION_ID, f.trackerClients, false);

    Collection<TrackerClient> captured = captureUpdateStateArg(f.stateUpdater);
    assertTrue(captured instanceof Set);
  }

  @Test
  public void testGetRingFlagOnPassesValuesView()
  {
    Fixture f = new Fixture(2);
    RelativeLoadBalancerStrategy strategy =
        new RelativeLoadBalancerStrategy(f.stateUpdater, f.clientSelector, true);

    strategy.getRing(CLUSTER_GENERATION_ID, PARTITION_ID, f.trackerClients, false);

    Collection<TrackerClient> captured = captureUpdateStateArg(f.stateUpdater);
    assertFalse(captured instanceof Set);
  }

  @Test
  public void testDefaultConstructorDefaultsFlagOff()
  {
    Fixture f = new Fixture(2);
    // Two-arg constructor must preserve legacy behavior (flag defaults to false).
    RelativeLoadBalancerStrategy strategy = new RelativeLoadBalancerStrategy(f.stateUpdater, f.clientSelector);

    strategy.getTrackerClient(Mockito.mock(Request.class), new RequestContext(),
        CLUSTER_GENERATION_ID, PARTITION_ID, f.trackerClients, false);

    Collection<TrackerClient> captured = captureUpdateStateArg(f.stateUpdater);
    assertTrue(captured instanceof Set,
        "two-arg ctor must default to flag=false (legacy HashSet-wrapping behavior)");
  }

  @SuppressWarnings("unchecked")
  private static Collection<TrackerClient> captureUpdateStateArg(StateUpdater stateUpdater)
  {
    ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
    Mockito.verify(stateUpdater).updateState(captor.capture(), anyInt(), anyLong(), anyBoolean());
    return (Collection<TrackerClient>) captor.getValue();
  }

  private static Map<URI, TrackerClient> buildTrackerClients(int count)
  {
    Map<URI, TrackerClient> map = new HashMap<>();
    for (int i = 0; i < count; i++)
    {
      URI uri = URI.create("http://host" + i + ":8080");
      TrackerClient client = Mockito.mock(TrackerClient.class);
      Mockito.when(client.getUri()).thenReturn(uri);
      map.put(uri, client);
    }
    return map;
  }
}
