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

package com.linkedin.d2.balancer.subsetting;

import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.clients.TrackerClientImpl;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


public class SubsettingStateTest
{
  private static final int THREAD_NUM = 20;
  private static final String SERVICE_NAME = "test";
  private static final int PARTITION_ID = 0;

  private final AtomicReference<AssertionError> _failure = new AtomicReference<>();

  private SubsettingState _subsettingState;

  @Mock
  private DeterministicSubsettingMetadataProvider _subsettingMetadataProvider;

  @Mock
  private SimpleLoadBalancerState _state;

  @BeforeMethod
  public void setUp()
  {
    MockitoAnnotations.initMocks(this);
    _subsettingState = new SubsettingState(new SubsettingStrategyFactoryImpl(), _subsettingMetadataProvider);
  }

  @Test
  public void testSingleThreadCase()
  {
    Mockito.when(_subsettingMetadataProvider.getSubsettingMetadata(_state))
        .thenReturn(new DeterministicSubsettingMetadata(0, 5, 0));

    SubsettingState.SubsetItem subsetItem = _subsettingState.getClientsSubset(SERVICE_NAME, 4, 0,
        mockTrackerClients(30), 0, _state);

    assertEquals(subsetItem.getWeightedSubset().size(), 6);
    assertTrue(subsetItem.shouldForceUpdate());

    Mockito.when(_subsettingMetadataProvider.getSubsettingMetadata(_state))
        .thenReturn(new DeterministicSubsettingMetadata(0, 4, 1));

    SubsettingState.SubsetItem subsetItem1 = _subsettingState.getClientsSubset(SERVICE_NAME, 4, 0,
        mockTrackerClients(30), 0, _state);

    assertEquals(subsetItem1.getWeightedSubset().size(), 8);
    assertTrue(subsetItem1.shouldForceUpdate());

    SubsettingState.SubsetItem subsetItem2 = _subsettingState.getClientsSubset(SERVICE_NAME, 4, 0,
        mockTrackerClients(28), 2, _state);

    assertEquals(subsetItem2.getWeightedSubset().size(), 7);
    assertTrue(subsetItem2.shouldForceUpdate());

    SubsettingState.SubsetItem subsetItem3 = _subsettingState.getClientsSubset(SERVICE_NAME, 8, 0,
        mockTrackerClients(28), 2, _state);

    assertEquals(subsetItem3.getWeightedSubset().size(), 14);
    assertTrue(subsetItem3.shouldForceUpdate());

    SubsettingState.SubsetItem subsetItem4 = _subsettingState.getClientsSubset(SERVICE_NAME, 8, 0,
        mockTrackerClients(28), 2, _state);
    assertEquals(subsetItem4.getWeightedSubset().size(), 14);
    assertFalse(subsetItem4.shouldForceUpdate());
  }

  @Test
  public void testMultiThreadCase() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(THREAD_NUM * 3);

    Mockito.when(_subsettingMetadataProvider.getSubsettingMetadata(_state))
        .thenReturn(new DeterministicSubsettingMetadata(0, 5, 0));

    for (int i = 0; i < THREAD_NUM; i++)
    {
      new Thread(() ->
      {
        SubsettingState.SubsetItem subsetItem = _subsettingState.getClientsSubset("test", 4, PARTITION_ID,
            mockTrackerClients(30), 0, _state);

        verifySubset(subsetItem.getWeightedSubset().size(), 6);
        latch.countDown();
      }).start();
    }

    Thread.sleep(500);

    Mockito.when(_subsettingMetadataProvider.getSubsettingMetadata(_state))
        .thenReturn(new DeterministicSubsettingMetadata(0, 4, 1));

    for (int i = 0; i < THREAD_NUM; i++)
    {
      new Thread(() ->
      {
        SubsettingState.SubsetItem subsetItem = _subsettingState.getClientsSubset("test", 4, PARTITION_ID,
            mockTrackerClients(30), 0, _state);

        verifySubset(subsetItem.getWeightedSubset().size(), 8);
        latch.countDown();
      }).start();
    }

    Thread.sleep(500);

    for (int i = 0; i < THREAD_NUM; i++)
    {
      new Thread(() ->
      {
        SubsettingState.SubsetItem subsetItem = _subsettingState.getClientsSubset("test", 4, PARTITION_ID,
            mockTrackerClients(28), 2, _state);

        verifySubset(subsetItem.getWeightedSubset().size(), 7);
        latch.countDown();
      }).start();
    }

    if (!latch.await(5, TimeUnit.SECONDS))
    {
      fail("subsetting update failed to finish within 5 seconds");
    }

    if (_failure.get() != null)
      throw _failure.get();
  }

  @Test
  public void testDoNotSlowStart()
  {
    Mockito.when(_subsettingMetadataProvider.getSubsettingMetadata(_state))
        .thenReturn(new DeterministicSubsettingMetadata(0, 5, 0));

    Map<URI, TrackerClient> trackerClients = mockTrackerClients(20);
    SubsettingState.SubsetItem subsetItem = _subsettingState.getClientsSubset(SERVICE_NAME, 4, 0,
        trackerClients, 0, _state);

    Map<URI, TrackerClient> trackerClients1 = mockTrackerClients(40);
    SubsettingState.SubsetItem subsetItem1 = _subsettingState.getClientsSubset(SERVICE_NAME, 4, 0,
        trackerClients1, 1, _state);

    verifyDoNotSlowStart(subsetItem1.getWeightedSubset(), trackerClients);
  }

  private void verifyDoNotSlowStart(Map<URI, TrackerClient> subset, Map<URI, TrackerClient> oldPotentialClients)
  {
    for (Map.Entry<URI, TrackerClient> entry : subset.entrySet())
    {
      if (oldPotentialClients.containsKey(entry.getKey()))
      {
        assertTrue(entry.getValue().doNotSlowStart());
      }
      else
      {
        assertFalse(entry.getValue().doNotSlowStart());
      }
    }
  }

  private void verifySubset(int subsetSize, int expected)
  {
    try
    {
      assertEquals(subsetSize, expected);
    }
    catch (AssertionError e)
    {
      _failure.set(e);
    }
  }

  private Map<URI, TrackerClient> mockTrackerClients(int numTrackerClients)
  {
    Map<URI, TrackerClient> trackerClients = new HashMap<>();
    for (int index = 0; index < numTrackerClients; index ++)
    {
      URI uri = URI.create("URI/" + index);
      TrackerClient trackerClient = Mockito.mock(TrackerClientImpl.class);
      Mockito.doCallRealMethod().when(trackerClient).setDoNotSlowStart(Mockito.anyBoolean());
      Mockito.doCallRealMethod().when(trackerClient).doNotSlowStart();
      Mockito.when(trackerClient.getUri()).thenReturn(uri);
      Mockito.when(trackerClient.getPartitionWeight(Mockito.anyInt())).thenReturn(1.0);
      trackerClients.put(uri, trackerClient);
    }
    return trackerClients;
  }
}