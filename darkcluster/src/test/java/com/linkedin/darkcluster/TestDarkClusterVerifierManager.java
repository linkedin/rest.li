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

package com.linkedin.darkcluster;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.darkcluster.api.DarkClusterVerifier;
import com.linkedin.darkcluster.api.DarkClusterVerifierManager;
import com.linkedin.darkcluster.impl.DarkClusterVerifierManagerImpl;
import com.linkedin.darkcluster.impl.SafeDarkClusterVerifier;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;

import static org.testng.Assert.fail;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestDarkClusterVerifierManager
{
  private static final String DARK_CLUSTER1_NAME = "darkCluster1";

  @Test
  void testVerifierEnabled()
    throws InterruptedException
  {
    TestVerifier verifier = new TestVerifier(true);
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    DarkClusterVerifierManager verifierManager = new DarkClusterVerifierManagerImpl(verifier, executorService);
    RestRequest req = new TestRestRequest();
    RestResponse res = new TestRestResponse();
    verifierManager.onDarkResponse(req, res, DARK_CLUSTER1_NAME);
    verifierManager.onDarkResponse(req, res, DARK_CLUSTER1_NAME);
    verifierManager.onResponse(req, res);

    // because it takes some time execute the previous three tasks on the executor, add a 4th one
    // that can signal we are done, given the executor is single threaded and will process them in order.
    final CountDownLatch latch = new CountDownLatch(1);
    Runnable myCallable = latch::countDown;
    executorService.submit(myCallable);
    if (!latch.await(30, TimeUnit.SECONDS))
    {
      fail("unable to execute task on executor");
    }
    Assert.assertEquals(verifier.onResponseCount, 1, "expected on response count of 1");
    Assert.assertEquals(verifier.onDarkResponseCount, 2, "expected on dark response count of 2");
  }

  @Test
  void testVerifierDisabled()
    throws InterruptedException
  {
    TestVerifier verifier = new TestVerifier(false);
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    DarkClusterVerifierManager verifierManager = new DarkClusterVerifierManagerImpl(verifier, executorService);
    RestRequest req = new TestRestRequest();
    RestResponse res = new TestRestResponse();
    verifierManager.onDarkResponse(req, res, DARK_CLUSTER1_NAME);
    verifierManager.onDarkResponse(req, res, DARK_CLUSTER1_NAME);
    verifierManager.onResponse(req, res);

    // because it takes some time execute the previous three tasks on the executor, add a 4th one
    // that can signal we are done, given the executor is single threaded and will process them in order.
    final CountDownLatch latch = new CountDownLatch(1);
    Runnable myCallable = latch::countDown;
    executorService.submit(myCallable);
    if (!latch.await(30, TimeUnit.SECONDS))
    {
      fail("unable to execute task on executor");
    }
    Assert.assertEquals(verifier.onResponseCount, 0, "expected on response count of 0");
    Assert.assertEquals(verifier.onDarkResponseCount, 0, "expected on dark response count of 0");
  }

  @Test
  void testVerifierErrorHandling()
    throws InterruptedException
  {
    TestVerifier verifier = new TestVerifier(true);
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    DarkClusterVerifierManager verifierManager = new DarkClusterVerifierManagerImpl(verifier, executorService);
    RestRequest req = new TestRestRequest();
    RestResponse res = new TestRestResponse();
    verifierManager.onDarkError(req, new Throwable(), DARK_CLUSTER1_NAME);
    verifierManager.onDarkError(req, new Throwable(), DARK_CLUSTER1_NAME);
    verifierManager.onError(req, new Throwable());

    // because it takes some time execute the previous three tasks on the executor, add a 4th one
    // that can signal we are done, given the executor is single threaded and will process them in order.
    final CountDownLatch latch = new CountDownLatch(1);
    Runnable myCallable = latch::countDown;
    executorService.submit(myCallable);
    if (!latch.await(30, TimeUnit.SECONDS))
    {
      fail("unable to execute task on executor");
    }
    Assert.assertEquals(verifier.onResponseCount, 1, "expected on response count of 1");
    Assert.assertEquals(verifier.onDarkResponseCount, 2, "expected on dark response count of 2");
  }

  @Test
  void testSafeVerifier()
    throws InterruptedException
  {
    DarkClusterVerifier verifier = new SafeDarkClusterVerifier(new TestThrowingVerifier());
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    DarkClusterVerifierManager verifierManager = new DarkClusterVerifierManagerImpl(verifier, executorService);
    RestRequest req = new TestRestRequest();
    RestResponse res = new TestRestResponse();
    verifierManager.onDarkResponse(req, res, DARK_CLUSTER1_NAME);
    verifierManager.onDarkResponse(req, res, DARK_CLUSTER1_NAME);
    verifierManager.onResponse(req, res);

    // because it takes some time execute the previous three tasks on the executor, add a 4th one
    // that can signal we are done, given the executor is single threaded and will process them in order.
    final CountDownLatch latch = new CountDownLatch(1);
    Runnable myCallable = latch::countDown;
    executorService.submit(myCallable);
    if (!latch.await(30, TimeUnit.SECONDS))
    {
      fail("unable to execute task on executor");
    }
    // if we got here, we successfully caught the exceptions

    // now retry without the SafeDarkClusterVerifier
    DarkClusterVerifier verifier2 = new TestThrowingVerifier();
    DarkClusterVerifierManager verifierManager2 = new DarkClusterVerifierManagerImpl(verifier2, executorService);
    RestRequest req2 = new TestRestRequest();
    RestResponse res2 = new TestRestResponse();
    try
    {
      verifierManager2.onDarkResponse(req2, res2, DARK_CLUSTER1_NAME);
      verifierManager2.onDarkResponse(req2, res2, DARK_CLUSTER1_NAME);
      verifierManager2.onResponse(req2, res2);

      // because it takes some time execute the previous three tasks on the executor, add a 4th one
      // that can signal we are done, given the executor is single threaded and will process them in order.
      final CountDownLatch latch2 = new CountDownLatch(1);
      Runnable myRunnable2 = latch2::countDown;
      executorService.submit(myRunnable2);
      if (!latch2.await(30, TimeUnit.SECONDS))
      {
        fail("unable to execute task on executor");
      }
      // we shouldn't get here, should have thrown exception
      fail("shouldn't have gotten here");
    }
    catch (Throwable t)
    {
      // expected, because we aren't using the SafeDarkClusterVerifier here
    }

  }

  static class TestVerifier implements DarkClusterVerifier
  {
    private boolean _isEnabled;
    int onResponseCount;
    int onDarkResponseCount;

    TestVerifier(boolean isEnabled)
    {
      _isEnabled = isEnabled;
    }

    @Override
    public void onResponse(RestRequest request, Response response)
    {
      onResponseCount++;
    }

    @Override
    public void onDarkResponse(RestRequest request, DarkResponse darkResponse)
    {
      onDarkResponseCount++;
    }

    @Override
    public boolean isEnabled()
    {
      return _isEnabled;
    }
  }

  static class TestThrowingVerifier implements DarkClusterVerifier
  {

    @Override
    public void onResponse(RestRequest request, Response response)
    {
      throw new RuntimeException("bad response");
    }

    @Override
    public void onDarkResponse(RestRequest request, DarkResponse darkResponse)
    {
      throw new RuntimeException("bad dark response");
    }

    @Override
    public boolean isEnabled()
    {
      return true;
    }
  }
}
