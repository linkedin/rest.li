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

import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
        createUris(30), 0, _state);

    assertEquals(subsetItem.getWeightedUriSubset().size(), 6);
    assertTrue(subsetItem.shouldForceUpdate());

    Mockito.when(_subsettingMetadataProvider.getSubsettingMetadata(_state))
        .thenReturn(new DeterministicSubsettingMetadata(0, 4, 1));

    SubsettingState.SubsetItem subsetItem1 = _subsettingState.getClientsSubset(SERVICE_NAME, 4, 0,
        createUris(30), 0, _state);

    assertEquals(subsetItem1.getWeightedUriSubset().size(), 8);
    assertTrue(subsetItem1.shouldForceUpdate());

    SubsettingState.SubsetItem subsetItem2 = _subsettingState.getClientsSubset(SERVICE_NAME, 4, 0,
        createUris(28), 2, _state);

    assertEquals(subsetItem2.getWeightedUriSubset().size(), 7);
    assertTrue(subsetItem2.shouldForceUpdate());

    SubsettingState.SubsetItem subsetItem3 = _subsettingState.getClientsSubset(SERVICE_NAME, 8, 0,
        createUris(28), 2, _state);

    assertEquals(subsetItem3.getWeightedUriSubset().size(), 14);
    assertTrue(subsetItem3.shouldForceUpdate());

    SubsettingState.SubsetItem subsetItem4 = _subsettingState.getClientsSubset(SERVICE_NAME, 8, 0,
        createUris(28), 2, _state);
    assertEquals(subsetItem4.getWeightedUriSubset().size(), 14);
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
            createUris(30), 0, _state);

        verifySubset(subsetItem.getWeightedUriSubset().size(), 6);
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
            createUris(30), 0, _state);

        verifySubset(subsetItem.getWeightedUriSubset().size(), 8);
        latch.countDown();
      }).start();
    }

    Thread.sleep(500);

    for (int i = 0; i < THREAD_NUM; i++)
    {
      new Thread(() ->
      {
        SubsettingState.SubsetItem subsetItem = _subsettingState.getClientsSubset("test", 4, PARTITION_ID,
            createUris(28), 2, _state);

        verifySubset(subsetItem.getWeightedUriSubset().size(), 7);
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

    Map<URI, Double> weightedUris = createUris(20);
    SubsettingState.SubsetItem subsetItem = _subsettingState.getClientsSubset(SERVICE_NAME, 4, 0,
        weightedUris, 0, _state);

    Map<URI, Double> weightedUris1 = createUris(40);
    SubsettingState.SubsetItem subsetItem1 = _subsettingState.getClientsSubset(SERVICE_NAME, 4, 0,
        weightedUris1, 1, _state);

    verifyDoNotSlowStart(subsetItem1.getWeightedUriSubset(), subsetItem1.getDoNotSlowStartUris(), weightedUris);
  }

  private void verifyDoNotSlowStart(Map<URI, Double> subset, Set<URI> doNotSlowStartUris, Map<URI, Double> oldPotentialClients)
  {
    for (URI uri : subset.keySet())
    {
      if (oldPotentialClients.containsKey(uri))
      {
        assertTrue(doNotSlowStartUris.contains(uri));
      }
      else
      {
        assertFalse(doNotSlowStartUris.contains(uri));
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

  private Map<URI, Double> createUris(int numUris)
  {
    Map<URI, Double> weightedUris = new HashMap<>();
    for (int index = 0; index < numUris; index ++)
    {
      URI uri = URI.create("URI/" + index);
      weightedUris.put(uri, 1.0);
    }
    return weightedUris;
  }
}