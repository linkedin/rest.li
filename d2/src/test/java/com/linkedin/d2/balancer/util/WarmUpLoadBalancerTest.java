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


import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.Directory;
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.LoadBalancer;
import com.linkedin.d2.balancer.LoadBalancerWithFacilities;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.WarmUpService;
import com.linkedin.d2.balancer.clients.TrackerClientTest.TestClient;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.util.partitions.PartitionInfoProvider;
import com.linkedin.d2.discovery.event.PropertyEventThread.PropertyEventShutdownCallback;
import com.linkedin.d2.discovery.stores.file.FileStoreTest;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import org.junit.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;


public class WarmUpLoadBalancerTest
{
  public static final String MY_SERVICES_FS = "myServices";

  // files with the wrong extension
  private static final List<String> UNVALID_FILES = Arrays.asList("unvalidServiceFile4.indgi", "unvalidServiceFile5.inagi");
  private static final List<String> VALID_FILES = Arrays.asList(
    "service1" + FileStoreTest.FILE_STORE_EXTENSION,
    "service2" + FileStoreTest.FILE_STORE_EXTENSION,
    "service3" + FileStoreTest.FILE_STORE_EXTENSION
  );

  private static final List<String> VALID_AND_UNVALID_FILES = new ArrayList<>();

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
      _tmpdir.getAbsolutePath(), MY_SERVICES_FS, WarmUpLoadBalancer.DEFAULT_SEND_REQUESTS_TIMEOUT_SECONDS,
      WarmUpLoadBalancer.DEFAULT_CONCURRENT_REQUESTS);

    FutureCallback<None> callback = new FutureCallback<>();
    warmUpLoadBalancer.start(callback);
    callback.get(5000, TimeUnit.MILLISECONDS);

    Assert.assertEquals(VALID_FILES.size(), requestCount.get());
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
      _tmpdir.getAbsolutePath(), MY_SERVICES_FS, WarmUpLoadBalancer.DEFAULT_SEND_REQUESTS_TIMEOUT_SECONDS,
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
  public void testThrottling() throws URISyntaxException, InterruptedException, ExecutionException, TimeoutException
  {
    int NRequests = 500;
    createNServicesIniFiles(NRequests);

    TestLoadBalancer balancer = new TestLoadBalancer(50);
    AtomicInteger requestCount = balancer.getRequestCount();
    LoadBalancer warmUpLoadBalancer = new WarmUpLoadBalancer(balancer, balancer, Executors.newSingleThreadScheduledExecutor(),
      _tmpdir.getAbsolutePath(), MY_SERVICES_FS, WarmUpLoadBalancer.DEFAULT_SEND_REQUESTS_TIMEOUT_SECONDS,
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
      _tmpdir.getAbsolutePath(), MY_SERVICES_FS, WarmUpLoadBalancer.DEFAULT_SEND_REQUESTS_TIMEOUT_SECONDS,
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
      _tmpdir.getAbsolutePath(), MY_SERVICES_FS, warmUpTimeout,
      concurrentRequests);

    FutureCallback<None> callback = new FutureCallback<>();
    warmUpLoadBalancer.start(callback);

    callback.get();
    Assert.assertTrue("Expected # of requests between " + expectedRequests + " +/-" + deviation + ", found:" + requestCount.get(),
      expectedRequests - deviation < requestCount.get()
        && expectedRequests + deviation > requestCount.get());
  }


  @BeforeMethod
  public void createTempdir() throws IOException
  {
    _tmpdir = LoadBalancerUtil.createTempDirectory("d2FileStore");
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
      files.add("randomFile" + i + FileStoreTest.FILE_STORE_EXTENSION);
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

  /**
   * Dummy LoadBalancer counting the number of requests done
   */
  public static class TestLoadBalancer implements LoadBalancerWithFacilities, WarmUpService
  {

    private final AtomicInteger _requestCount = new AtomicInteger();
    private final AtomicInteger _completedRequestCount = new AtomicInteger();
    private int _delayMs = 0;
    private final int DELAY_STANDARD_DEVIATION = 10; //ms
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public TestLoadBalancer()
    {
    }

    public TestLoadBalancer(int delayMs)
    {
      _delayMs = delayMs;
    }

    @Override
    public TransportClient getClient(Request request, RequestContext requestContext) throws ServiceUnavailableException
    {
      return new TestClient();
    }

    @Override
    public void warmUpService(String serviceName, Callback<None> callback)
    {
      _requestCount.incrementAndGet();
      executorService.schedule(() ->
      {
        _completedRequestCount.incrementAndGet();
        callback.onSuccess(None.none());
      }, Math.max(0, _delayMs
        // any kind of random delay works for the test
        + ((int) new Random().nextGaussian() * DELAY_STANDARD_DEVIATION)), TimeUnit.MILLISECONDS);
    }

    @Override
    public void start(Callback<None> callback)
    {
      callback.onSuccess(None.none());
    }

    @Override
    public void shutdown(PropertyEventShutdownCallback shutdown)
    {
      shutdown.done();
    }

    @Override
    public ServiceProperties getLoadBalancedServiceProperties(String serviceName)
      throws ServiceUnavailableException
    {
      return null;
    }

    AtomicInteger getRequestCount()
    {
      return _requestCount;
    }

    AtomicInteger getCompletedRequestCount()
    {
      return _completedRequestCount;
    }

    @Override
    public Directory getDirectory()
    {
      return null;
    }

    @Override
    public PartitionInfoProvider getPartitionInfoProvider()
    {
      return null;
    }

    @Override
    public KeyMapper getKeyMapper()
    {
      return null;
    }

    @Override
    public TransportClientFactory getClientFactory(String scheme)
    {
      return null;
    }
  }
}
