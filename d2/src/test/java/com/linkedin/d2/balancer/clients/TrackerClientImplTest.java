package com.linkedin.d2.balancer.clients;

import java.net.URI;
import java.util.HashMap;

import com.linkedin.util.clock.SystemClock;

import org.junit.Assert;
import org.testng.annotations.Test;

/**
 * Tests {@link TrackerClientImpl}.
 */
public class TrackerClientImplTest
{
  private TrackerClientImpl _trackerClient;

  @Test
  public void testDoNotLoadBalance()
  {
    boolean doNotLoadBalance = true;
    _trackerClient = new TrackerClientImpl(URI.create("uri"), new HashMap<>(), null, SystemClock.instance(), 1000, (test) -> false, false, false, doNotLoadBalance);

    Assert.assertEquals(_trackerClient.doNotLoadBalance(), doNotLoadBalance);

    doNotLoadBalance = false;
    _trackerClient = new TrackerClientImpl(URI.create("uri"), new HashMap<>(), null, SystemClock.instance(), 1000, (test) -> false, false, false, doNotLoadBalance);

    Assert.assertEquals(_trackerClient.doNotLoadBalance(), doNotLoadBalance);
  }
}
