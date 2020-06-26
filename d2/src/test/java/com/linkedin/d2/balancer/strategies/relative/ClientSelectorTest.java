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
import com.linkedin.d2.balancer.util.hashing.DistributionNonDiscreteRing;
import com.linkedin.d2.balancer.util.hashing.RandomHash;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Matchers.anyInt;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


/**
 * Test for {@link ClientSelector}
 */
public class ClientSelectorTest
{
  private static URI URI_1;
  private static URI URI_2;
  private static URI URI_3;
  private static final TrackerClient TRACKER_CLIENT_1 = Mockito.mock(TrackerClient.class);
  private static final TrackerClient TRACKER_CLIENT_2 = Mockito.mock(TrackerClient.class);
  private static final TrackerClient TRACKER_CLIENT_3 = Mockito.mock(TrackerClient.class);
  private static final Map<URI, Integer> DEFAULT_POINTS_MAP = new HashMap<>();
  private static final Ring<URI> DEFAULT_RING;
  private static final Map<URI, TrackerClient> DEFAULT_TRACKER_CLIENT_MAP = new HashMap<>();
  private ClientSelector _clientSelector;
  private Request _request;
  private RequestContext _requestContext;

  static
  {
    URI_1 = URI.create("dummy_uri_1");
    URI_2 = URI.create("dummy_uri_2");
    URI_3 = URI.create("dummy_uri_3");
    Mockito.when(TRACKER_CLIENT_1.getUri()).thenReturn(URI_1);
    Mockito.when(TRACKER_CLIENT_2.getUri()).thenReturn(URI_2);
    Mockito.when(TRACKER_CLIENT_3.getUri()).thenReturn(URI_3);
    DEFAULT_POINTS_MAP.put(URI_1, 60);
    DEFAULT_POINTS_MAP.put(URI_2, 80);
    DEFAULT_POINTS_MAP.put(URI_3, 100);
    DEFAULT_RING = new DistributionNonDiscreteRing<>(DEFAULT_POINTS_MAP);
    DEFAULT_TRACKER_CLIENT_MAP.put(URI_1, TRACKER_CLIENT_1);
    DEFAULT_TRACKER_CLIENT_MAP.put(URI_2, TRACKER_CLIENT_2);
    DEFAULT_TRACKER_CLIENT_MAP.put(URI_3, TRACKER_CLIENT_3);
  }

  @BeforeMethod
  private void setup()
  {
    _clientSelector = new ClientSelector(new RandomHash());
    _request = Mockito.mock(Request.class);
    _requestContext = new RequestContext();
  }

  @Test
  public void testGetTargetHost()
  {
    KeyMapper.TargetHostHints.setRequestContextTargetHost(_requestContext, URI_1);

    TrackerClient trackerClient = _clientSelector.getTrackerClient(_request, _requestContext, DEFAULT_RING, DEFAULT_TRACKER_CLIENT_MAP);
    assertEquals(trackerClient.getUri(), URI_1);
  }

  @Test
  public void testGetTargetHostNotFound()
  {
    URI newUri = URI.create("new_uri");
    KeyMapper.TargetHostHints.setRequestContextTargetHost(_requestContext, newUri);

    TrackerClient trackerClient = _clientSelector.getTrackerClient(_request, _requestContext, DEFAULT_RING, DEFAULT_TRACKER_CLIENT_MAP);
    assertEquals(trackerClient, null);
  }

  @Test
  public void testGetHostFromRing()
  {
    TrackerClient trackerClient = _clientSelector.getTrackerClient(_request, _requestContext, DEFAULT_RING, DEFAULT_TRACKER_CLIENT_MAP);
    assertTrue(DEFAULT_TRACKER_CLIENT_MAP.containsKey(trackerClient.getUri()));
  }

  @Test
  public void testAllClientsExcluded()
  {
    LoadBalancerStrategy.ExcludedHostHints.addRequestContextExcludedHost(_requestContext, URI_1);
    LoadBalancerStrategy.ExcludedHostHints.addRequestContextExcludedHost(_requestContext, URI_2);
    LoadBalancerStrategy.ExcludedHostHints.addRequestContextExcludedHost(_requestContext, URI_3);

    TrackerClient trackerClient = _clientSelector.getTrackerClient(_request, _requestContext, DEFAULT_RING, DEFAULT_TRACKER_CLIENT_MAP);
    assertEquals(trackerClient, null);
  }

  @Test
  public void testClientsPartiallyExcluded()
  {
    LoadBalancerStrategy.ExcludedHostHints.addRequestContextExcludedHost(_requestContext, URI_1);
    LoadBalancerStrategy.ExcludedHostHints.addRequestContextExcludedHost(_requestContext, URI_2);

    TrackerClient trackerClient = _clientSelector.getTrackerClient(_request, _requestContext, DEFAULT_RING, DEFAULT_TRACKER_CLIENT_MAP);
    assertEquals(trackerClient, TRACKER_CLIENT_3);
  }

  @Test
  public void testRingAndHostInconsistency()
  {
    URI newUri = URI.create("new_uri");
    TrackerClient newTrackerClient = Mockito.mock(TrackerClient.class);
    Mockito.when(newTrackerClient.getUri()).thenReturn(newUri);
    Map<URI, TrackerClient> newTrackerClientMap = new HashMap<>();
    newTrackerClientMap.put(newUri, newTrackerClient);

    TrackerClient trackerClient = _clientSelector.getTrackerClient(_request, _requestContext, DEFAULT_RING, newTrackerClientMap);
    assertEquals(trackerClient, newTrackerClient,
        "The host should be picked from the tracker client list passed from the request because the ring is completely out of date");
  }

  @Test
  public void testSubstituteClientFromRing()
  {
    URI newUri = URI.create("new_uri");
    @SuppressWarnings("unchecked")
    Ring<URI> ring = Mockito.mock(Ring.class);
    Mockito.when(ring.get(anyInt())).thenReturn(newUri);
    List<URI> ringIteratierList = Arrays.asList(newUri, URI_1, URI_2, URI_3);
    Mockito.when(ring.getIterator(anyInt())).thenReturn(ringIteratierList.iterator());

    TrackerClient trackerClient = _clientSelector.getTrackerClient(_request, _requestContext, ring, DEFAULT_TRACKER_CLIENT_MAP);
    assertTrue(DEFAULT_TRACKER_CLIENT_MAP.containsKey(trackerClient.getUri()));
  }
}
