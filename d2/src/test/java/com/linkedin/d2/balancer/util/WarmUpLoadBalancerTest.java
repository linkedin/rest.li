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

package com.linkedin.d2.balancer.util;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.LoadBalancer;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.util.downstreams.DownstreamServicesFetcher;
import com.linkedin.d2.balancer.util.downstreams.FSBasedDownstreamServicesFetcher;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class WarmUpLoadBalancerTest
{
  public static final String MY_SERVICES_FS = "myServices";

  // files with the wrong extension
  private static final List<String> UNVALID_FILES = Arrays.asList("unvalidServiceFile4.indgi", "unvalidServiceFile5.inagi");
  private static final List<String> VALID_FILES = Arrays.asList(
    "service1" + FileSystemDirectory.FILE_STORE_EXTENSION,
    "service2" + FileSystemDirectory.FILE_STORE_EXTENSION,
    "service3" + FileSystemDirectory.FILE_STORE_EXTENSION
  );

  private static final List<String> VALID_AND_UNVALID_FILES = new ArrayList<>();
  private FSBasedDownstreamServicesFetcher _FSBasedDownstreamServicesFetcher;

  static
  {
    VALID_AND_UNVALID_FILES.addAll(VALID_FILES);
    VALID_AND_UNVALID_FILES.addAll(UNVALID_FILES);
  }

  private File _tmpdir;

  @Test(timeOut = 10000)
  public void testMakingWarmUpRequests() throws URISyntaxException, InterruptedException, ExecutionException, TimeoutException
  {
    createDefaultServicesIniFiles();

    TestLoadBalancer balancer = new TestLoadBalancer();
    AtomicInteger requestCount = balancer.getRequestCount();
    LoadBalancer warmUpLoadBalancer = new WarmUpLoadBalancer(balancer, balancer, Executors.newSingleThreadScheduledExecutor(),
      _tmpdir.getAbsolutePath(), MY_SERVICES_FS, _FSBasedDownstreamServicesFetcher,
      WarmUpLoadBalancer.DEFAULT_SEND_REQUESTS_TIMEOUT_SECONDS,
      WarmUpLoadBalancer.DEFAULT_CONCURRENT_REQUESTS);

    FutureCallback<None> callback = new FutureCallback<>();
    warmUpLoadBalancer.start(callback);
    callback.get(5000, TimeUnit.MILLISECONDS);

    Assert.assertEquals(VALID_FILES.size(), requestCount.get());
  }

  @Test(timeOut = 10000, groups = { "ci-flaky" })
  public void testDeletingFilesAfterShutdown() throws InterruptedException, ExecutionException, TimeoutException
  {
    createDefaultServicesIniFiles();
    TestLoadBalancer balancer = new TestLoadBalancer();

    List<String> allServicesBeforeShutdown = getAllDownstreamServices();
    List<String> partialServices = getPartialDownstreams();

    DownstreamServicesFetcher returnPartialDownstreams = callback -> callback.onSuccess(partialServices);

    LoadBalancer warmUpLoadBalancer = new WarmUpLoadBalancer(balancer, balancer, Executors.newSingleThreadScheduledExecutor(),
      _tmpdir.getAbsolutePath(), MY_SERVICES_FS, returnPartialDownstreams,
      WarmUpLoadBalancer.DEFAULT_SEND_REQUESTS_TIMEOUT_SECONDS,
      WarmUpLoadBalancer.DEFAULT_CONCURRENT_REQUESTS);

    FutureCallback<None> callback = new FutureCallback<>();
    warmUpLoadBalancer.start(callback);
    callback.get(5000, TimeUnit.MILLISECONDS);

    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    warmUpLoadBalancer.shutdown(() -> shutdownCallback.onSuccess(None.none()));
    shutdownCallback.get(5000, TimeUnit.MILLISECONDS);

    List<String> allServicesAfterShutdown = getAllDownstreamServices();

    Assert.assertTrue(allServicesBeforeShutdown.size() > partialServices.size(),
      "After shutdown the unused services should have been deleted. Expected lower number of:" + allServicesBeforeShutdown.size()
        + ", actual " + partialServices.size());

    Assert.assertTrue(partialServices.containsAll(allServicesAfterShutdown)
        && allServicesAfterShutdown.containsAll(partialServices),
      "There should be just the services that were passed by the partial fetcher");
  }

  /**
   * Since the list might from the fetcher might not be complete (update service, old data, etc.., and the user might
   * require additional services at runtime, we have to check that those services are not cleared from the cache
   * otherwise it would incur in a penalty at the next deployment
   */
  @Test(timeOut = 10000, dataProvider = "isD2CallAsync")
  public void testNotDeletingFilesGetClient(boolean isD2CallAsync) throws InterruptedException, ExecutionException, TimeoutException, ServiceUnavailableException
  {
    createDefaultServicesIniFiles();
    TestLoadBalancer balancer = new TestLoadBalancer();

    List<String> allServicesBeforeShutdown = getAllDownstreamServices();
    DownstreamServicesFetcher returnNoDownstreams = callback -> callback.onSuccess(Collections.emptyList());

    String pickOneService = allServicesBeforeShutdown.get(0);

    LoadBalancer warmUpLoadBalancer = new WarmUpLoadBalancer(balancer, balancer, Executors.newSingleThreadScheduledExecutor(),
      _tmpdir.getAbsolutePath(), MY_SERVICES_FS, returnNoDownstreams,
      WarmUpLoadBalancer.DEFAULT_SEND_REQUESTS_TIMEOUT_SECONDS,
      WarmUpLoadBalancer.DEFAULT_CONCURRENT_REQUESTS);

    FutureCallback<None> callback = new FutureCallback<>();
    warmUpLoadBalancer.start(callback);
    callback.get(5000, TimeUnit.MILLISECONDS);

    if (isD2CallAsync)
    {
      FutureCallback<TransportClient> clientCallback = new FutureCallback<>();
      warmUpLoadBalancer.getClient(new URIRequest("d2://" + pickOneService), new RequestContext(), clientCallback);
      clientCallback.get();
    }
    else
    {
      warmUpLoadBalancer.getClient(new URIRequest("d2://" + pickOneService), new RequestContext());
    }


    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    warmUpLoadBalancer.shutdown(() -> shutdownCallback.onSuccess(None.none()));
    shutdownCallback.get(5000, TimeUnit.MILLISECONDS);

    List<String> allServicesAfterShutdown = getAllDownstreamServices();

    Assert.assertEquals(1, allServicesAfterShutdown.size(), "After shutdown there should be just one service, the one that we 'get the client' on");
  }

  @DataProvider(name = "isD2CallAsync")
  public Object[][] isD2CallAsync()
  {
    return new Object[][]
        {
            {true},
            {false}
        };
  }

  private List<String> getAllDownstreamServices() throws InterruptedException, ExecutionException, TimeoutException
  {
    FutureCallback<List<String>> services = new FutureCallback<>();
    _FSBasedDownstreamServicesFetcher.getServiceNames(services);
    return services.get(5, TimeUnit.SECONDS);
  }

  /**
   * Return a partial list of the downstreams
   */
  private List<String> getPartialDownstreams() throws InterruptedException, ExecutionException, TimeoutException
  {
    List<String> allServices = getAllDownstreamServices();

    // if there are less than 2 services, it doesn't remove anything
    assert allServices.size() >= 2;
    //remove half of the services
    for (int i = 0; i < allServices.size() / 2; i++)
    {
      allServices.remove(0);
    }
    return allServices;
  }

  /**
   * If there are 0 valid files, no requests should be triggered
   */
  @Test
  public void testNoMakingWarmUpRequestsWithoutValidFiles() throws URISyntaxException, InterruptedException, ExecutionException, TimeoutException
  {
    createServicesIniFiles(UNVALID_FILES);

    TestLoadBalancer balancer = new TestLoadBalancer();
    AtomicInteger requestCount = balancer.getRequestCount();
    LoadBalancer warmUpLoadBalancer = new WarmUpLoadBalancer(balancer, balancer, Executors.newSingleThreadScheduledExecutor(),
      _tmpdir.getAbsolutePath(), MY_SERVICES_FS, _FSBasedDownstreamServicesFetcher,
      WarmUpLoadBalancer.DEFAULT_SEND_REQUESTS_TIMEOUT_SECONDS,
      WarmUpLoadBalancer.DEFAULT_CONCURRENT_REQUESTS);

    FutureCallback<None> callback = new FutureCallback<>();
    warmUpLoadBalancer.start(callback);
    callback.get(5000, TimeUnit.MILLISECONDS);

    Assert.assertEquals(0, requestCount.get());
  }

  /**
   * Should not send warm up requests if we are NOT using the WarmUpLoadBalancer
   */
  @Test
  public void testNoMakingWarmUpRequestsWithoutWarmUp() throws URISyntaxException, InterruptedException, ExecutionException, TimeoutException
  {
    createDefaultServicesIniFiles();

    TestLoadBalancer balancer = new TestLoadBalancer();
    AtomicInteger requestCount = balancer.getRequestCount();

    FutureCallback<None> callback = new FutureCallback<>();
    balancer.start(callback);
    callback.get(5000, TimeUnit.MILLISECONDS);

    Assert.assertEquals(0, requestCount.get());
  }

  @Test(timeOut = 10000)
  public void testThrottling() throws InterruptedException
  {
    int NRequests = 100;
    createNServicesIniFiles(NRequests);

    TestLoadBalancer balancer = new TestLoadBalancer(50);
    AtomicInteger requestCount = balancer.getRequestCount();
    LoadBalancer warmUpLoadBalancer = new WarmUpLoadBalancer(balancer, balancer, Executors.newSingleThreadScheduledExecutor(),
      _tmpdir.getAbsolutePath(), MY_SERVICES_FS, _FSBasedDownstreamServicesFetcher,
      WarmUpLoadBalancer.DEFAULT_SEND_REQUESTS_TIMEOUT_SECONDS,
      WarmUpLoadBalancer.DEFAULT_CONCURRENT_REQUESTS);

    FutureCallback<None> callback = new FutureCallback<>();
    warmUpLoadBalancer.start(callback);

    boolean triggeredAtLeastOnce = false;
    while (!callback.isDone())
    {
      triggeredAtLeastOnce = true;
      int currentConcurrentRequests = balancer.getRequestCount().get() - balancer.getCompletedRequestCount().get();
      if (currentConcurrentRequests > WarmUpLoadBalancer.DEFAULT_CONCURRENT_REQUESTS)
      {
        Assert.fail("The concurrent requests (" + currentConcurrentRequests
          + ") are greater than the allowed (" + WarmUpLoadBalancer.DEFAULT_CONCURRENT_REQUESTS + ")");
      }
      Thread.sleep(50);
    }

    Assert.assertTrue(triggeredAtLeastOnce);
    Assert.assertEquals(NRequests, requestCount.get());
  }

  /**
   * Tests that if the requests are not throttled it makes a large amount of concurrent calls
   */
  @Test(timeOut = 10000)
  public void testThrottlingUnlimitedRequests() throws URISyntaxException, InterruptedException, ExecutionException, TimeoutException
  {
    int NRequests = 500;
    createNServicesIniFiles(NRequests);

    int concurrentRequestsHugeNumber = 999999999;
    int concurrentRequestsCheckHigher = WarmUpLoadBalancer.DEFAULT_CONCURRENT_REQUESTS;

    TestLoadBalancer balancer = new TestLoadBalancer(50);
    AtomicInteger requestCount = balancer.getRequestCount();
    LoadBalancer warmUpLoadBalancer = new WarmUpLoadBalancer(balancer, balancer, Executors.newSingleThreadScheduledExecutor(),
      _tmpdir.getAbsolutePath(), MY_SERVICES_FS, _FSBasedDownstreamServicesFetcher,
      WarmUpLoadBalancer.DEFAULT_SEND_REQUESTS_TIMEOUT_SECONDS,
      concurrentRequestsHugeNumber);

    FutureCallback<None> callback = new FutureCallback<>();
    warmUpLoadBalancer.start(callback);

    boolean triggeredAtLeastOnce = false;
    while (!callback.isDone())
    {
      int currentConcurrentRequests = balancer.getRequestCount().get() - balancer.getCompletedRequestCount().get();
      if (currentConcurrentRequests > concurrentRequestsCheckHigher)
      {
        triggeredAtLeastOnce = true;
      }
      Thread.sleep(50);
    }

    Assert.assertTrue(triggeredAtLeastOnce);
    Assert.assertEquals(NRequests, requestCount.get());
  }

  @Test(timeOut = 10000)
  public void testHitTimeout() throws URISyntaxException, InterruptedException, ExecutionException, TimeoutException
  {
    int NRequests = 5000;
    int warmUpTimeout = 2;
    int concurrentRequests = 5;
    int requestTime = 100;

    float requestsPerSecond = 1000 / requestTime * concurrentRequests;
    int expectedRequests = (int) (requestsPerSecond * warmUpTimeout);
    int deviation = (int) requestsPerSecond; // we allow inaccuracies of 1s

    createNServicesIniFiles(NRequests);

    TestLoadBalancer balancer = new TestLoadBalancer(requestTime);
    AtomicInteger requestCount = balancer.getRequestCount();
    LoadBalancer warmUpLoadBalancer = new WarmUpLoadBalancer(balancer, balancer, Executors.newSingleThreadScheduledExecutor(),
      _tmpdir.getAbsolutePath(), MY_SERVICES_FS, _FSBasedDownstreamServicesFetcher, warmUpTimeout, concurrentRequests);

    FutureCallback<None> callback = new FutureCallback<>();
    warmUpLoadBalancer.start(callback);

    callback.get();
    Assert.assertTrue(expectedRequests - deviation < requestCount.get()
        && expectedRequests + deviation > requestCount.get(),
      "Expected # of requests between " + expectedRequests + " +/-" + deviation + ", found:" + requestCount.get());
  }


  @BeforeMethod
  public void createTempdir() throws IOException
  {
    _tmpdir = LoadBalancerUtil.createTempDirectory("d2FileStore");
    _FSBasedDownstreamServicesFetcher = new FSBasedDownstreamServicesFetcher(_tmpdir.getAbsolutePath(), MY_SERVICES_FS);
  }

  @AfterMethod
  public void removeTempdir() throws IOException
  {
    if (_tmpdir != null)
    {
      rmrf(_tmpdir);
      _tmpdir = null;
    }
  }

  // ############################# Util Section #############################

  private void rmrf(File f) throws IOException
  {
    if (f.isDirectory())
    {
      for (File contained : f.listFiles())
      {
        rmrf(contained);
      }
    }
    if (!f.delete())
    {
      throw new IOException("Failed to delete file: " + f);
    }
  }

  /**
   * Creates default files
   */
  private void createDefaultServicesIniFiles()
  {
    createServicesIniFiles(VALID_AND_UNVALID_FILES);
  }

  /**
   * Creates n random service files
   */
  private void createNServicesIniFiles(int n)
  {
    List<String> files = new ArrayList<>();
    for (int i = 0; i < n; i++)
    {
      files.add("randomFile" + i + FileSystemDirectory.FILE_STORE_EXTENSION);
    }
    createServicesIniFiles(files);
  }

  /**
   * Creates all the dummy INI file in the Services directory
   */
  private void createServicesIniFiles(List<String> files)
  {
    String dir = FileSystemDirectory.getServiceDirectory(_tmpdir.getAbsolutePath(), MY_SERVICES_FS);

    for (String path : files)
    {
      File f = new File(dir + File.separator + path);
      f.getParentFile().mkdirs();
      try
      {
        if (!f.createNewFile())
        {
          throw new RuntimeException("Unable to create the file " + f.getAbsolutePath());
        }
      }
      catch (IOException e)
      {
        throw new RuntimeException(e);
      }
    }
  }
}
