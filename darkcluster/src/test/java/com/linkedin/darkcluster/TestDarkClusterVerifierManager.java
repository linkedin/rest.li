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

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.linkedin.darkcluster.api.DarkClusterVerifier;
import com.linkedin.darkcluster.api.DarkClusterVerifierManager;
import com.linkedin.darkcluster.impl.DarkClusterVerifierManagerImpl;
import com.linkedin.darkcluster.impl.SafeDarkClusterVerifier;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;

import static org.testng.Assert.fail;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestDarkClusterVerifierManager
{
  private static final String DARK_CLUSTER1_NAME = "darkCluster1";
  private ExecutorService _executorService;
  private DarkClusterVerifierManager _verifierManager;
  private TestVerifier _verifier;

  private void setup(boolean verifierEnabled)
  {
    _verifier = new TestVerifier(verifierEnabled);
    _executorService = Executors.newSingleThreadExecutor();
    _verifierManager = new DarkClusterVerifierManagerImpl(_verifier, _executorService);
  }

  @Test
  public void testVerifierEnabled()
    throws InterruptedException
  {
    setup(true);
    RestRequest dummyRestRequest = new RestRequestBuilder(URI.create("foo")).build();
    RestResponse res = new RestResponseBuilder().build();
    _verifierManager.onDarkResponse(dummyRestRequest, res, DARK_CLUSTER1_NAME);
    _verifierManager.onDarkResponse(dummyRestRequest, res, DARK_CLUSTER1_NAME);
    _verifierManager.onResponse(dummyRestRequest, res);

    waitForLatch();
    Assert.assertEquals(_verifier.onResponseCount, 1, "expected on response count of 1");
    Assert.assertEquals(_verifier.onDarkResponseCount, 2, "expected on dark response count of 2");
  }

  @Test
  void testVerifierDisabled()
    throws InterruptedException
  {
    setup(false);
    RestRequest req = new RestRequestBuilder(URI.create("foo")).build();
    RestResponse res = new RestResponseBuilder().build();
    _verifierManager.onDarkResponse(req, res, DARK_CLUSTER1_NAME);
    _verifierManager.onDarkResponse(req, res, DARK_CLUSTER1_NAME);
    _verifierManager.onResponse(req, res);

    waitForLatch();
    Assert.assertEquals(_verifier.onResponseCount, 0, "expected on response count of 0");
    Assert.assertEquals(_verifier.onDarkResponseCount, 0, "expected on dark response count of 0");
  }

  @Test
  void testVerifierErrorHandling()
    throws InterruptedException
  {
    setup(true);
    RestRequest req = new RestRequestBuilder(URI.create("foo")).build();
    _verifierManager.onDarkError(req, new Throwable(), DARK_CLUSTER1_NAME);
    _verifierManager.onDarkError(req, new Throwable(), DARK_CLUSTER1_NAME);
    _verifierManager.onError(req, new Throwable());

    waitForLatch();
    Assert.assertEquals(_verifier.onResponseCount, 1, "expected on response count of 1");
    Assert.assertEquals(_verifier.onDarkResponseCount, 2, "expected on dark response count of 2");
  }

  @Test
  void testSafeVerifier()
    throws InterruptedException
  {
    // only use this to set up the executor service.
    setup(false);
    DarkClusterVerifier verifier = new SafeDarkClusterVerifier(new TestThrowingVerifier());
    DarkClusterVerifierManager verifierManager = new DarkClusterVerifierManagerImpl(verifier, _executorService);
    RestRequest req = new RestRequestBuilder(URI.create("foo")).build();
    RestResponse res = new RestResponseBuilder().build();
    verifierManager.onDarkResponse(req, res, DARK_CLUSTER1_NAME);
    verifierManager.onDarkResponse(req, res, DARK_CLUSTER1_NAME);
    verifierManager.onResponse(req, res);

    waitForLatch();
    // if we got here, we successfully caught the exceptions

    // now retry without the SafeDarkClusterVerifier
    DarkClusterVerifier verifier2 = new TestThrowingVerifier();
    DarkClusterVerifierManager verifierManager2 = new DarkClusterVerifierManagerImpl(verifier2, _executorService);
    RestRequest req2 = new RestRequestBuilder(URI.create("foo")).build();
    RestResponse res2 = new RestResponseBuilder().build();
    try
    {
      verifierManager2.onDarkResponse(req2, res2, DARK_CLUSTER1_NAME);
      verifierManager2.onDarkResponse(req2, res2, DARK_CLUSTER1_NAME);
      verifierManager2.onResponse(req2, res2);

      waitForLatch();
      // we shouldn't get here, should have thrown exception
      fail("shouldn't have gotten here");
    }
    catch (Throwable t)
    {
      // expected, because we aren't using the SafeDarkClusterVerifier here
    }

  }

  private void waitForLatch()
    throws InterruptedException
  {
    // because it takes some time execute the previous three tasks on the executor, add a 4th one
    // that can signal we are done, given the executor is single threaded and will process them in order.
    final CountDownLatch latch = new CountDownLatch(1);
    Runnable myCallable = latch::countDown;
    _executorService.submit(myCallable);
    if (!latch.await(60, TimeUnit.SECONDS))
    {
      fail("unable to execute task on executor");
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
