/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.r2.disruptor;

import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Sean Sheng
 * @version $Revision$
 */
public class TestDisruptContexts
{
  @Test
  public void testLatency()
  {
    final long latency = 4200;
    DisruptContexts.DelayDisruptContext context =
        (DisruptContexts.DelayDisruptContext) DisruptContexts.delay(latency);

    Assert.assertEquals(context.mode(), DisruptMode.DELAY);
    Assert.assertEquals(context.delay(), latency);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testLatencyIllegal()
  {
    final long latency = -4200;
    DisruptContexts.delay(latency);
  }

  @Test
  public void testTimeout()
  {
    DisruptContexts.TimeoutDisruptContext context =
        (DisruptContexts.TimeoutDisruptContext) DisruptContexts.timeout();
    Assert.assertEquals(context.mode(), DisruptMode.TIMEOUT);
  }

  @Test
  public void testError()
  {
    final long latency = 4200;
    DisruptContexts.ErrorDisruptContext context =
        (DisruptContexts.ErrorDisruptContext) DisruptContexts.error(latency);

    Assert.assertEquals(context.mode(), DisruptMode.ERROR);
    Assert.assertEquals(context.latency(), latency);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testErrorIllegal()
  {
    final long latency = -4200;
    DisruptContexts.error(latency);
  }
}
