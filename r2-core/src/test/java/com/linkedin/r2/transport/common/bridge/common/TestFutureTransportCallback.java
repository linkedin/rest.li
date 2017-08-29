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

package com.linkedin.r2.transport.common.bridge.common;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.testng.annotations.Test;


/**
 * @author Sean Sheng
 */
public class TestFutureTransportCallback
{
  private static final Object ITEM = new Object();

  @Test(timeOut = 1000L)
  public void testGet() throws Exception
  {
    FutureTransportCallback<Object> futureTransportCallback = new FutureTransportCallback<>();

    // At this time the future is neither done or cancelled
    Assert.assertFalse(futureTransportCallback.isDone());
    Assert.assertFalse(futureTransportCallback.isCancelled());

    futureTransportCallback.onResponse(TransportResponseImpl.success(ITEM));

    // At this time the future is done but not cancelled
    Assert.assertTrue(futureTransportCallback.isDone());
    Assert.assertFalse(futureTransportCallback.isCancelled());

    TransportResponse<Object> transportResponse = futureTransportCallback.get();

    Assert.assertNotNull(transportResponse);
    Assert.assertNotNull(transportResponse.getResponse());
    Assert.assertSame(transportResponse.getResponse(), ITEM);
  }

  @Test(timeOut = 1000L, expectedExceptions = TimeoutException.class)
  public void testGetTimeout() throws Exception
  {
    FutureTransportCallback<Object> futureTransportCallback = new FutureTransportCallback<>();
    futureTransportCallback.get(0, TimeUnit.MILLISECONDS);
  }
}
