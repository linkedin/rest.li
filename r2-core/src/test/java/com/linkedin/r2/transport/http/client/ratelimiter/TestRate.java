package com.linkedin.r2.transport.http.client.ratelimiter;

import java.util.Arrays;
import org.testng.annotations.Test;


public class TestRate
{

  @Test
  public void testPrecisionAdjustments()
  {
    for (double qps: Arrays.asList(0.05d, 0.5d, 5d, 5.05d, 5.5d, 50.5d))
    {
      Rate rate = new Rate(qps, 1000, 1);
      assert ((float) rate.getEvents() / rate.getPeriod()) / (rate.getEventsRaw() / rate.getPeriodRaw()) > Rate.PRECISION_TARGET;
    }
  }
}
