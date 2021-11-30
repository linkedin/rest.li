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
import com.linkedin.d2.balancer.Facilities;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.clients.DegraderTrackerClientTest.TestCallback;
import com.linkedin.d2.balancer.clients.stub.DirectoryProviderMock;
import com.linkedin.d2.balancer.clients.stub.KeyMapperProviderMock;
import com.linkedin.d2.balancer.clients.stub.LoadBalancerMock;
import com.linkedin.d2.balancer.util.ClientFactoryProvider;
import com.linkedin.d2.balancer.util.DelegatingFacilities;
import com.linkedin.d2.balancer.util.DirectoryProvider;
import com.linkedin.d2.balancer.util.KeyMapperProvider;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.mockito.Mockito;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


public class DynamicClientTest
{
  @Test(groups = { "small", "back-end" }, dataProvider = "restOverStreamSwitch")
  @SuppressWarnings("deprecation")
  public void testClient(boolean restOverStream) throws URISyntaxException
  {
    LoadBalancerMock balancer = new LoadBalancerMock(false);
    DirectoryProvider dirProvider = new DirectoryProviderMock();
    KeyMapperProvider keyMapperProvider = new KeyMapperProviderMock();
    ClientFactoryProvider clientFactoryProvider = Mockito.mock(ClientFactoryProvider.class);
    Facilities facilities = new DelegatingFacilities(dirProvider, keyMapperProvider, clientFactoryProvider);
    DynamicClient client = new DynamicClient(balancer, facilities, restOverStream);
    URI uri = URI.create("d2://test");
    RestRequest restRequest = new RestRequestBuilder(uri).build();
    TestCallback<RestResponse> restCallback = new TestCallback<>();

    client.restRequest(restRequest, restCallback);

    assertNull(restCallback.e);
    assertNotNull(restCallback.t);

    Facilities myFacilities = client.getFacilities();
    assertNotNull(facilities, "facilities should not be null");
  }

  @Test(groups = { "small", "back-end" }, dataProvider = "restOverStreamSwitch")
  public void testUnavailable(boolean restOverStream) throws URISyntaxException
  {
    LoadBalancerMock balancer = new LoadBalancerMock(true);
    DynamicClient client = new DynamicClient(balancer, null, restOverStream);
    URI uri = URI.create("d2://test");
    RestRequest restRequest = new RestRequestBuilder(uri).build();
    TestCallback<RestResponse> restCallback = new TestCallback<>();

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
    LoadBalancerMock balancer = new LoadBalancerMock(true);
    DynamicClient client = new DynamicClient(balancer, null, true);
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

  @DataProvider(name="restOverStreamSwitch")
  public static Object[][] restOverStreamSwitch()
  {
    return new Object[][] {{true}, {false}};
  }

}
