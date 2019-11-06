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

package com.linkedin.r2.util.finalizer;

import java.util.concurrent.atomic.AtomicInteger;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


/**
 * Tests for {@link RequestFinalizerManagerImpl}.
 *
 * @author Chris Zhang
 */
public class TestRequestFinalizerManager
{
  private RequestFinalizerManagerImpl _manager;
  private AtomicInteger _atomicInteger;

  @BeforeMethod
  public void setup()
  {
    _manager = new RequestFinalizerManagerImpl(null, null);
    _atomicInteger = new AtomicInteger(0);
  }

  @Test
  public void testFinalizeRequest()
  {
    final int numFinalizers = 10;

    for (int i = 0; i < numFinalizers; i++)
    {
      _manager.registerRequestFinalizer((request, response, requestContext, error) -> _atomicInteger.incrementAndGet());
    }

    _manager.finalizeRequest(null, null);

    Assert.assertEquals(_atomicInteger.get(), numFinalizers,
        "Expected all " + numFinalizers + " request finalizers to be run.");
  }

  @Test
  public void testFinalizeRequestTwice()
  {
    final int numFinalizers = 10;

    for (int i = 0; i < numFinalizers; i++)
    {
      _manager.registerRequestFinalizer((request, response, requestContext, error) -> _atomicInteger.incrementAndGet());
    }
    Assert.assertTrue(_manager.finalizeRequest(null, null),
        "finalizeRequest should return true the first time it is invoked.");
    Assert.assertEquals(_atomicInteger.get(), numFinalizers,
        "Expected all " + numFinalizers + " request finalizers to be run.");


    final int numFinalizeRequestInvocations = 10;

    for (int i = 0; i < numFinalizeRequestInvocations; i++)
    {
      Assert.assertFalse(_manager.finalizeRequest(null, null),
          "finalizeRequest should return false for any subsequent invocations.");
      Assert.assertEquals(_atomicInteger.get(), numFinalizers,
          "Expected no additional request finalizers to be run.");
    }
  }

  @Test
  public void testRegisterAfterFinalizeRequest()
  {
    final int numFinalizers = 10;

    for (int i = 0; i < numFinalizers; i++)
    {
      _manager.registerRequestFinalizer((request, response, requestContext, error) -> _atomicInteger.incrementAndGet());
    }
    Assert.assertTrue(_manager.finalizeRequest(null, null),
        "finalizeRequest should return true the first time it is invoked.");
    Assert.assertEquals(_atomicInteger.get(), numFinalizers,
        "Expected all " + numFinalizers + " request finalizers to be run.");


    Assert.assertFalse(_manager.registerRequestFinalizer((request, response, requestContext, error) -> _atomicInteger.incrementAndGet()),
        "RequestFinalizer should have failed to register.");
    Assert.assertFalse(_manager.finalizeRequest(null, null),
        "finalizeRequest should return false for any subsequent invocations.");
    Assert.assertEquals(_atomicInteger.get(), numFinalizers,
        "Expected no additional request finalizers to be run.");

  }

  @Test
  public void testRequestFinalizerThrowsException()
  {
    final int numFinalizers = 10;

    for (int i = 0; i < numFinalizers; i++)
    {
      _manager.registerRequestFinalizer((request, response, requestContext, error) -> _atomicInteger.incrementAndGet());
      _manager.registerRequestFinalizer((request, response, requestContext, error) -> {
        throw new RuntimeException("Expected exception.");
      });
    }

    _manager.finalizeRequest(null, null);

    Assert.assertEquals(_atomicInteger.get(), numFinalizers,
        "Expected all " + numFinalizers + " request finalizers to be run.");
  }

  @Test
  public void testRequestFinalizerOrdering()
  {
    final AtomicInteger executionOrder = new AtomicInteger(0);

    final int numFinalizers = 10;

    for (int i = 0; i < numFinalizers; i++)
    {
      final int registrationOrder = i;
      _manager.registerRequestFinalizer((request, response, requestContext, error) ->
          Assert.assertEquals(executionOrder.getAndIncrement(), registrationOrder,
              "Expected request finalizers to be executed in the order that they were registered in."));
    }

    _manager.finalizeRequest(null, null);
  }
}
