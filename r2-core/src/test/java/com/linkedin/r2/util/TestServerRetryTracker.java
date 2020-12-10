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

package com.linkedin.r2.util;

import com.linkedin.r2.filter.transport.ServerRetryFilter;
import com.linkedin.util.clock.SettableClock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class TestServerRetryTracker
{
  private ServerRetryTracker _serverRetryTracker;
  private SettableClock _clock;

  @BeforeMethod
  public void setUp()
  {
    _clock = new SettableClock();
    _serverRetryTracker = new ServerRetryTracker(ServerRetryFilter.DEFAULT_RETRY_LIMIT,
        ServerRetryFilter.DEFAULT_AGGREGATED_INTERVAL_NUM, ServerRetryFilter.DEFAULT_MAX_REQUEST_RETRY_RATIO,
        ServerRetryFilter.DEFAULT_UPDATE_INTERVAL_MS, _clock);
  }

  @Test
  public void testEmptyServerRetryTracker()
  {
    for (int i = 0; i < 10; i++)
    {
      Assert.assertTrue(_serverRetryTracker.isBelowRetryRatio());
      Assert.assertEquals(_serverRetryTracker.getRetryRatio(),0.0, 0.0001);
      _clock.addDuration(ServerRetryFilter.DEFAULT_UPDATE_INTERVAL_MS);
    }
  }

  @Test
  public void testServerRetryTracker()
  {
    _serverRetryTracker.add(0);
    _serverRetryTracker.add(0);
    _serverRetryTracker.add(1);

    Assert.assertTrue(_serverRetryTracker.isBelowRetryRatio());
    Assert.assertEquals(_serverRetryTracker.getRetryRatio(),0.0, 0.0001);

    _clock.addDuration(ServerRetryFilter.DEFAULT_UPDATE_INTERVAL_MS);
    _serverRetryTracker.updateRetryDecision();

    Assert.assertFalse(_serverRetryTracker.isBelowRetryRatio());
    // The aggregated retry counter is [2, 1, 0]. Retry ratio = 1/2 = 0.5
    Assert.assertEquals(_serverRetryTracker.getRetryRatio(),0.5, 0.0001);

    _serverRetryTracker.add(1);
    _serverRetryTracker.add(2);
    _serverRetryTracker.add(2);
    _clock.addDuration(ServerRetryFilter.DEFAULT_UPDATE_INTERVAL_MS);
    _serverRetryTracker.updateRetryDecision();

    Assert.assertFalse(_serverRetryTracker.isBelowRetryRatio());
    // Now the aggregated retry counter is [2, 2, 2]. Retry ratio = 1.0
    Assert.assertEquals(_serverRetryTracker.getRetryRatio(),1.0, 0.0001);

    for (int i = 0; i < 8; i++)
    {
      _serverRetryTracker.add(0);
    }
    _clock.addDuration(ServerRetryFilter.DEFAULT_UPDATE_INTERVAL_MS);
    _serverRetryTracker.updateRetryDecision();

    Assert.assertFalse(_serverRetryTracker.isBelowRetryRatio());
    // Now the aggregated retry counter is [10, 2, 2]. Retry ratio = ((2/10) + (2/2)) / 2 = 0.6
    Assert.assertEquals(_serverRetryTracker.getRetryRatio(),0.6, 0.0001);

    _clock.addDuration(ServerRetryFilter.DEFAULT_UPDATE_INTERVAL_MS);
    _clock.addDuration(ServerRetryFilter.DEFAULT_UPDATE_INTERVAL_MS);
    _clock.addDuration(ServerRetryFilter.DEFAULT_UPDATE_INTERVAL_MS);
    _serverRetryTracker.updateRetryDecision();

    Assert.assertFalse(_serverRetryTracker.isBelowRetryRatio());
    // Now the first interval is discarded, and the aggregated retry counter is [8, 1, 2]. Retry ratio = 0.5625
    Assert.assertEquals(_serverRetryTracker.getRetryRatio(),0.5625, 0.0001);

    _clock.addDuration(ServerRetryFilter.DEFAULT_UPDATE_INTERVAL_MS);
    _clock.addDuration(ServerRetryFilter.DEFAULT_UPDATE_INTERVAL_MS);
    _serverRetryTracker.updateRetryDecision();

    Assert.assertTrue(_serverRetryTracker.isBelowRetryRatio());
    // Now all the previous intervals are discarded, and the aggregated retry counter is [0, 0, 0]. Retry ratio = 0
    Assert.assertEquals(_serverRetryTracker.getRetryRatio(),0.0, 0.0001);
  }
}