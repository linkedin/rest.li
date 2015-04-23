/*
   Copyright (c) 2012 LinkedIn Corp.

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

package com.linkedin.d2.balancer.clients;


import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.Directory;
import com.linkedin.d2.balancer.Facilities;
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.LoadBalancer;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.clients.TrackerClientTest.TestCallback;
import com.linkedin.d2.balancer.clients.TrackerClientTest.TestClient;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.util.ClientFactoryProvider;
import com.linkedin.d2.balancer.util.DelegatingFacilities;
import com.linkedin.d2.balancer.util.DirectoryProvider;
import com.linkedin.d2.balancer.util.HostToKeyMapper;
import com.linkedin.d2.balancer.util.KeyMapperProvider;
import com.linkedin.d2.balancer.util.MapKeyResult;
import com.linkedin.d2.discovery.event.PropertyEventThread.PropertyEventShutdownCallback;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


public class DynamicClientTest
{
  @Test(groups = { "small", "back-end" })
  @SuppressWarnings("deprecation")
  public void testClient() throws URISyntaxException
  {
    TestLoadBalancer balancer = new TestLoadBalancer(false);
    DirectoryProvider dirProvider = new TestDirectoryProvider();
    KeyMapperProvider keyMapperProvider = new TestKeyMapperProvider();
    ClientFactoryProvider clientFactoryProvider = new TestClientFactoryProvider();
    Facilities facilities = new DelegatingFacilities(dirProvider, keyMapperProvider, clientFactoryProvider);
    DynamicClient client = new DynamicClient(balancer, facilities);
    URI uri = URI.create("d2://test");
    RestRequest restRequest = new RestRequestBuilder(uri).build();
    TestCallback<RestResponse> restCallback = new TestCallback<RestResponse>();

    client.restRequest(restRequest, restCallback);

    assertNull(restCallback.e);
    assertNotNull(restCallback.t);

    Facilities myFacilities = client.getFacilities();
    assertNotNull(facilities, "facilities should not be null");
  }

  @Test(groups = { "small", "back-end" })
  public void testUnavailable() throws URISyntaxException
  {
    TestLoadBalancer balancer = new TestLoadBalancer(true);
    DynamicClient client = new DynamicClient(balancer, null);
    URI uri = URI.create("d2://test");
    RestRequest restRequest = new RestRequestBuilder(uri).build();
    TestCallback<RestResponse> restCallback = new TestCallback<RestResponse>();

    client.restRequest(restRequest, restCallback);

    assertNotNull(restCallback.e);
    assertTrue(restCallback.e instanceof ServiceUnavailableException);
    assertNull(restCallback.t);

    Facilities facilities = client.getFacilities();
    assertNull(facilities, "facilities should be null");
  }

  @Test(groups = { "small", "back-end" })
  public void testShutdown() throws URISyntaxException,
      InterruptedException
  {
    TestLoadBalancer balancer = new TestLoadBalancer(true);
    DynamicClient client = new DynamicClient(balancer, null);
    final CountDownLatch latch = new CountDownLatch(1);

    assertFalse(balancer.shutdown);

    client.shutdown(new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
      }

      @Override
      public void onSuccess(None t)
      {
        latch.countDown();
      }
    });

    if (!latch.await(5, TimeUnit.SECONDS))
    {
      fail("unable to shut down dynamic client");
    }

    assertTrue(balancer.shutdown);
  }

  public static class TestLoadBalancer implements LoadBalancer
  {
    private boolean _serviceUnavailable;
    public boolean  shutdown = false;

    public TestLoadBalancer(boolean serviceUnavailable)
    {
      _serviceUnavailable = serviceUnavailable;
    }

    @Override
    public TransportClient getClient(Request request, RequestContext requestContext) throws ServiceUnavailableException
    {
      if (_serviceUnavailable)
      {
        throw new ServiceUnavailableException("bad", "bad");
      }

      return new TestClient();
    }

    @Override
    public void start(Callback<None> callback)
    {
      callback.onSuccess(None.none());
    }

    @Override
    public void shutdown(PropertyEventShutdownCallback shutdown)
    {
      this.shutdown = true;
      shutdown.done();
    }

    @Override
    public ServiceProperties getLoadBalancedServiceProperties(String serviceName)
        throws ServiceUnavailableException
    {
      return null;
    }
  }

  public static class TestDirectory implements Directory
  {

    @Override
    public void getServiceNames(Callback<List<String>> callback)
    {

    }

    @Override
    public void getClusterNames(Callback<List<String>> callback)
    {

    }
  }

  public static class TestKeyMapper implements KeyMapper
  {
    @Override
    public <K> MapKeyResult<URI, K> mapKeysV2(URI serviceUri, Iterable<K> keys)
        throws ServiceUnavailableException
    {
      return null;
    }

    @Override
    public <K> HostToKeyMapper<K> mapKeysV3(URI serviceUri, Collection<K> keys, int limitNumHostsPerPartition)
        throws ServiceUnavailableException
    {
      return null;
    }

    @Override
    public <K, S> HostToKeyMapper<K> mapKeysV3(URI serviceUri,
                                                 Collection<K> keys,
                                                 int limitNumHostsPerPartition,
                                                 S stickyKey)
        throws ServiceUnavailableException
    {
      return null;
    }

    @Override
    public HostToKeyMapper<Integer> getAllPartitionsMultipleHosts(URI serviceUri, int numHostPerPartition) throws ServiceUnavailableException
    {
      return null;
    }

    @Override
    public <S> HostToKeyMapper<Integer> getAllPartitionsMultipleHosts(URI serviceUri, int limitHostPerPartition, S stickyKey) throws ServiceUnavailableException
    {
      return null;
    }

  }

  public static class TestDirectoryProvider implements DirectoryProvider
  {

    @Override
    public Directory getDirectory()
    {
      return new TestDirectory();
    }
  }

  public static class TestKeyMapperProvider implements KeyMapperProvider
  {

    @Override
    public KeyMapper getKeyMapper()
    {
      return new TestKeyMapper();
    }
  }

  public static class TestClientFactoryProvider implements ClientFactoryProvider
  {
    @Override
    public TransportClientFactory getClientFactory(String scheme)
    {
      return null;
    }
  }
}
