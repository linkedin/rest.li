/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.r2.message.timing;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.message.RequestContext;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Tests for {@link TimingCallback}.
 *
 * @author Evan Williams
 */
public class TestTimingCallback
{
  private static final TimingKey KEY_H = TimingKey.registerNewKey("test/h", TimingImportance.HIGH);
  private static final TimingKey KEY_M = TimingKey.registerNewKey("test/m", TimingImportance.MEDIUM);
  private static final TimingKey KEY_L = TimingKey.registerNewKey("test/l", TimingImportance.LOW);

  /**
   * Ensures that timing keys are marked in the same order that they are added.
   */
  @Test
  public void testOrdering()
  {
    final RequestContext requestContext = new RequestContext();
    final Callback<Long> callback = new Callback<Long>()
    {
      @Override
      public void onSuccess(Long result)
      {
        Map<TimingKey, TimingContextUtil.TimingContext> timings = TimingContextUtil.getTimingsMap(requestContext);

        // Ensure all keys are present
        Assert.assertTrue(timings.containsKey(KEY_H));
        Assert.assertTrue(timings.containsKey(KEY_M));
        Assert.assertTrue(timings.containsKey(KEY_L));

        // Ensure timing start times/durations are consistent based on their ordering in the callback
        TimingContextUtil.TimingContext contextH = timings.get(KEY_H);
        TimingContextUtil.TimingContext contextM = timings.get(KEY_M);
        TimingContextUtil.TimingContext contextL = timings.get(KEY_L);
        Assert.assertTrue(contextM.getStartTimeNano() < contextL.getStartTimeNano());
        Assert.assertTrue(contextL.getStartTimeNano() < contextH.getStartTimeNano());
        Assert.assertTrue(contextL.getDurationNano() < contextM.getDurationNano());
        Assert.assertTrue(contextH.getDurationNano() < contextM.getDurationNano());
      }

      @Override
      public void onError(Throwable e) {}
    };

    final Callback<Long> timingCallback = new TimingCallback.Builder<>(callback, requestContext)
        .addBeginTimingKey(KEY_M)
        .addBeginTimingKey(KEY_L)
        .addEndTimingKey(KEY_L)
        .addBeginTimingKey(KEY_H)
        .addEndTimingKey(KEY_H)
        .addEndTimingKey(KEY_M)
        .build();

    timingCallback.onSuccess(1L);
  }

  @DataProvider(name = "timingImportanceThreshold")
  private Object[][] provideTimingImportanceThresholdData()
  {
    return new Object[][]
        {
            { null },
            { TimingImportance.LOW },
            { TimingImportance.MEDIUM },
            { TimingImportance.HIGH }
        };
  }

  /**
   * Ensures that the builder can correctly determine how to filter out timing keys based on the current timing
   * importance threshold, and that it can correctly determine when to return the original callback rather than wrapping
   * it with a new one.
   * @param timingImportanceThreshold timing importance threshold
   */
  @Test(dataProvider = "timingImportanceThreshold")
  public void testBuilder(TimingImportance timingImportanceThreshold)
  {
    final RequestContext requestContext = new RequestContext();
    if (timingImportanceThreshold != null)
    {
      requestContext.putLocalAttr(TimingContextUtil.TIMING_IMPORTANCE_THRESHOLD_KEY_NAME, timingImportanceThreshold);
    }

    final Callback<Long> callback = new Callback<Long>()
    {
      @Override
      public void onSuccess(Long result)
      {
        Map<TimingKey, TimingContextUtil.TimingContext> timings = TimingContextUtil.getTimingsMap(requestContext);
        // Ensure that keys have been filtered out correctly
        if (timingImportanceThreshold == null || TimingImportance.LOW.isAtLeast(timingImportanceThreshold))
        {
          Assert.assertTrue(timings.containsKey(KEY_L));
          Assert.assertTrue(timings.containsKey(KEY_M));
        }
        else if (TimingImportance.MEDIUM.isAtLeast(timingImportanceThreshold))
        {
          Assert.assertFalse(timings.containsKey(KEY_L));
          Assert.assertTrue(timings.containsKey(KEY_M));
        }
        else
        {
          Assert.assertFalse(timings.containsKey(KEY_L));
          Assert.assertFalse(timings.containsKey(KEY_M));
        }
      }

      @Override
      public void onError(Throwable e) {}
    };

    final Callback<Long> timingCallback = new TimingCallback.Builder<>(callback, requestContext)
        .addBeginTimingKey(KEY_L)
        .addBeginTimingKey(KEY_M)
        .addEndTimingKey(KEY_L)
        .addEndTimingKey(KEY_M)
        .build();

    // Ensure that the builder can correctly determine when to return the original callback
    if (timingImportanceThreshold == null || !timingImportanceThreshold.equals(TimingImportance.HIGH))
    {
      Assert.assertTrue(timingCallback instanceof TimingCallback);
    }
    else
    {
      Assert.assertFalse(timingCallback instanceof TimingCallback);
    }

    timingCallback.onSuccess(1L);
  }
}
