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
import com.linkedin.d2.balancer.dualread.DualReadModeProvider;
import com.linkedin.d2.balancer.dualread.DualReadStateManager;
import com.linkedin.d2.balancer.util.downstreams.DownstreamServicesFetcher;
import com.linkedin.d2.balancer.util.downstreams.FSBasedDownstreamServicesFetcher;
import com.linkedin.d2.util.TestDataHelper;
import com.linkedin.r2.message.RequestContext;
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
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.d2.balancer.dualread.DualReadModeProvider.DualReadMode.*;
import static org.mockito.Mockito.*;


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
  private DualReadModeProvider _dualReadModeProvider;
  private DualReadStateManager _dualReadStateManager;
  private static final int TIME_FREEZED_CALL = 5; // the first call in warmUpServices which sets timeout

  @BeforeMethod
  public void beforeTest() throws IOException
  {
    _tmpdir = LoadBalancerUtil.createTempDirectory("d2FileStore");
    _FSBasedDownstreamServicesFetcher = new FSBasedDownstreamServicesFetcher(_tmpdir.getAbsolutePath(), MY_SERVICES_FS);

    _dualReadModeProvider = Mockito.mock(DualReadModeProvider.class);
    _dualReadStateManager = Mockito.mock(DualReadStateManager.class);
    when(_dualReadStateManager.getDualReadModeProvider()).thenReturn(_dualReadModeProvider);
    doNothing().when(_dualReadStateManager).updateService(any(), any());
    doNothing().when(_dualReadStateManager).updateCluster(any(), any());
  }

  private void setDualReadMode(DualReadModeProvider.DualReadMode mode)
  {
    when(_dualReadModeProvider.getDualReadMode(any())).thenReturn(mode);
    when(_dualReadStateManager.getServiceDualReadMode(any())).thenReturn(mode);
  }

  @AfterMethod
  public void afterTest() throws IOException
  {
    if (_tmpdir != null)
    {
      rmrf(_tmpdir);
      _tmpdir = null;
    }

    _dualReadModeProvider = null;
    _dualReadStateManager = null;
  }

  @Test
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
    callback.get(30, TimeUnit.MILLISECONDS); // 3 services should take at most 3 * 5ms

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
  @Test(timeOut = 10000)
  public void testNotDeletingFilesGetClient() throws InterruptedException, ExecutionException, TimeoutException, ServiceUnavailableException
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

    warmUpLoadBalancer.getClient(new URIRequest("d2://" + pickOneService), new RequestContext());

    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    warmUpLoadBalancer.shutdown(() -> shutdownCallback.onSuccess(None.none()));
    shutdownCallback.get(5000, TimeUnit.MILLISECONDS);

    List<String> allServicesAfterShutdown = getAllDownstreamServices();

    Assert.assertEquals(1, allServicesAfterShutdown.size(), "After shutdown there should be just one service, the one that we 'get the client' on");
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

  @DataProvider // to test dual read modes under which the specific type of warmup load balancer should do warmup
  public Object[][] modesToWarmUpDataProvider()
  {
    return new Object[][]
        {// @params: {dual read mode, isIndis}
            {NEW_LB_ONLY, true},
            {OLD_LB_ONLY, false},
            // under dual read mode, both INDIS and ZK warmup should do warmup
            {DUAL_READ, true},
            {DUAL_READ, false}
        };
  }

  @Test(dataProvider = "modesToWarmUpDataProvider")
  public void testSuccessWithDualRead(DualReadModeProvider.DualReadMode mode, Boolean isIndis)
      throws InterruptedException, ExecutionException, TimeoutException
  {
    int timeoutMillis = 65;
    createDefaultServicesIniFiles();
    setDualReadMode(mode);

    // 3 dual read fetches take 30ms, 3 warmups take at most 3 * (5 +/- 5) ms. Total at most is 60 ms.
    TestLoadBalancer balancer = new TestLoadBalancer(5, 10);
    AtomicInteger completedWarmUpCount = balancer.getCompletedRequestCount();
    LoadBalancer warmUpLb = new WarmUpLoadBalancer(balancer, balancer, Executors.newSingleThreadScheduledExecutor(),
        _tmpdir.getAbsolutePath(), MY_SERVICES_FS, _FSBasedDownstreamServicesFetcher, timeoutMillis,
        WarmUpLoadBalancer.DEFAULT_CONCURRENT_REQUESTS, _dualReadStateManager, isIndis,
        TestDataHelper.getTimeSupplier(10, TIME_FREEZED_CALL));

    FutureCallback<None> callback = new FutureCallback<>();
    warmUpLb.start(callback);

    callback.get(timeoutMillis, TimeUnit.MILLISECONDS);
    // all dual read (service data) fetched
    verify(_dualReadStateManager, times(VALID_FILES.size())).updateCluster(any(), any());
    // all warmups completed
    Assert.assertEquals(completedWarmUpCount.get(), VALID_FILES.size());
  }

  @Test(dataProvider = "modesToWarmUpDataProvider")
  public void testDualReadHitTimeout(DualReadModeProvider.DualReadMode mode, Boolean isIndis)
      throws InterruptedException, ExecutionException, TimeoutException
  {
    int timeoutMillis = 15;
    createDefaultServicesIniFiles();
    setDualReadMode(mode);

    // 3 dual read fetches take 90ms
    TestLoadBalancer balancer = new TestLoadBalancer(0, 10);
    AtomicInteger completedWarmUpCount = balancer.getCompletedRequestCount();
    LoadBalancer warmUpLb = new WarmUpLoadBalancer(balancer, balancer, Executors.newSingleThreadScheduledExecutor(),
        _tmpdir.getAbsolutePath(), MY_SERVICES_FS, _FSBasedDownstreamServicesFetcher, timeoutMillis,
        WarmUpLoadBalancer.DEFAULT_CONCURRENT_REQUESTS, _dualReadStateManager, isIndis,
        TestDataHelper.getTimeSupplier(10, TIME_FREEZED_CALL));

    FutureCallback<None> callback = new FutureCallback<>();
    warmUpLb.start(callback);

    callback.get(timeoutMillis, TimeUnit.MILLISECONDS);
    // verify that at most 2 service data were fetched within the timeout
    verify(_dualReadStateManager, atMost(2)).updateCluster(any(), any());
    // warmups are not started
    Assert.assertEquals(completedWarmUpCount.get(), 0);
  }

  @Test(dataProvider = "modesToWarmUpDataProvider")
  public void testDualReadCompleteWarmUpHitTimeout(DualReadModeProvider.DualReadMode mode, Boolean isIndis)
      throws InterruptedException, ExecutionException, TimeoutException
  {
    int timeoutMillis = 40;
    createDefaultServicesIniFiles();
    setDualReadMode(mode);

    // 3 dual read fetches take 30ms, 3 warmups take 3 * (10 +/- 5) ms
    TestLoadBalancer balancer = new TestLoadBalancer(10, 10);
    AtomicInteger completedWarmUpCount = balancer.getCompletedRequestCount();
    LoadBalancer warmUpLb = new WarmUpLoadBalancer(balancer, balancer, Executors.newSingleThreadScheduledExecutor(),
        _tmpdir.getAbsolutePath(), MY_SERVICES_FS, _FSBasedDownstreamServicesFetcher, timeoutMillis,
        WarmUpLoadBalancer.DEFAULT_CONCURRENT_REQUESTS, _dualReadStateManager, isIndis,
        TestDataHelper.getTimeSupplier(10, TIME_FREEZED_CALL));

    FutureCallback<None> callback = new FutureCallback<>();
    warmUpLb.start(callback);

    callback.get(timeoutMillis, TimeUnit.MILLISECONDS);
    // verify dual read (service data) are all fetched
    verify(_dualReadStateManager, times(VALID_FILES.size())).updateCluster(any(), any());
    // only partial warmups completed
    Assert.assertTrue(completedWarmUpCount.get() < VALID_FILES.size());
  }

  @DataProvider // to test dual read modes under which the specific type of warmup load balancer should skip warmup
  public Object[][] modesToSkipDataProvider()
  {
    return new Object[][]
        { // @params: {dual read mode, isIndis}
            {NEW_LB_ONLY, false},
            {OLD_LB_ONLY, true}
        };
  }
  @Test(dataProvider = "modesToSkipDataProvider")
  public void testSkipWarmup(DualReadModeProvider.DualReadMode mode, Boolean isIndis)
      throws ExecutionException, InterruptedException, TimeoutException {
    int timeoutMillis = 40;
    createDefaultServicesIniFiles();
    setDualReadMode(mode);

    TestLoadBalancer balancer = new TestLoadBalancer(0, 0);
    AtomicInteger completedWarmUpCount = balancer.getCompletedRequestCount();
    LoadBalancer warmUpLb = new WarmUpLoadBalancer(balancer, balancer, Executors.newSingleThreadScheduledExecutor(),
        _tmpdir.getAbsolutePath(), MY_SERVICES_FS, _FSBasedDownstreamServicesFetcher, timeoutMillis,
        WarmUpLoadBalancer.DEFAULT_CONCURRENT_REQUESTS, _dualReadStateManager, isIndis,
        TestDataHelper.getTimeSupplier(0, TIME_FREEZED_CALL));

    FutureCallback<None> callback = new FutureCallback<>();
    warmUpLb.start(callback);

    callback.get(timeoutMillis, TimeUnit.MILLISECONDS); // skipping warmup should call back nearly immediately
    // no service data fetched
    verify(_dualReadStateManager, never()).updateCluster(any(), any());
    // warmups are not started
    Assert.assertEquals(completedWarmUpCount.get(), 0);
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
