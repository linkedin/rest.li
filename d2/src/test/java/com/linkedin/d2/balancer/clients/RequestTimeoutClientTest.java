/*
   Copyright (c) 2018 LinkedIn Corp.

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

import com.linkedin.d2.balancer.D2Client;
import com.linkedin.d2.balancer.Facilities;
import com.linkedin.d2.balancer.clients.stub.DirectoryProviderMock;
import com.linkedin.d2.balancer.clients.stub.KeyMapperProviderMock;
import com.linkedin.d2.balancer.clients.stub.LoadBalancerMock;
import com.linkedin.d2.balancer.simple.LoadBalancerSimulator;
import com.linkedin.d2.balancer.util.ClientFactoryProvider;
import com.linkedin.d2.balancer.util.DelegatingFacilities;
import com.linkedin.d2.balancer.util.DirectoryProvider;
import com.linkedin.d2.balancer.util.KeyMapperProvider;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import java.net.URI;
import java.util.concurrent.TimeoutException;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.d2.balancer.clients.TestClient.DEFAULT_REQUEST_TIMEOUT;
import static org.testng.Assert.assertNull;


/**
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class RequestTimeoutClientTest
{
  @DataProvider
  public static Object[][] allCombinations3x()
  {
    return new Object[][]{
      // isHigherThanDefault, ignoreTimeoutIfHigher, expectedTimeout
      {false, false, 400},
      {false, true, 400},
      {true, false, 600},
      {true, true, 500},
    };
  }

  /**
   * Check that the timeout are the expected ones
   */
  @Test(groups = {"small"}, dataProvider = "allCombinations3x")
  @SuppressWarnings("deprecation")
  public void testRequestTimeoutAllowed(boolean isHigherThanDefault, boolean ignoreTimeoutIfHigher, int expectedTimeout) throws Exception
  {
    LoadBalancerSimulator.ClockedExecutor clockedExecutor = new LoadBalancerSimulator.ClockedExecutor();

    LoadBalancerMock balancer = new LoadBalancerMock(false, true, clockedExecutor);
    DirectoryProvider dirProvider = new DirectoryProviderMock();
    KeyMapperProvider keyMapperProvider = new KeyMapperProviderMock();
    ClientFactoryProvider clientFactoryProvider = Mockito.mock(ClientFactoryProvider.class);
    Facilities facilities = new DelegatingFacilities(dirProvider, keyMapperProvider, clientFactoryProvider);
    D2Client client = new DynamicClient(balancer, facilities, true);
    URI uri = URI.create("d2://test");
    RestRequest restRequest = new RestRequestBuilder(uri).build();

    client = new RequestTimeoutClient(client, balancer, clockedExecutor);

    RequestContext requestContext = new RequestContext();

    int requestTimeout = isHigherThanDefault ? DEFAULT_REQUEST_TIMEOUT + 100 : DEFAULT_REQUEST_TIMEOUT - 100;

    TrackerClientTest.TestCallback<RestResponse> restCallback = new TrackerClientTest.TestCallback<>();
    requestContext.putLocalAttr(R2Constants.REQUEST_TIMEOUT, requestTimeout);
    if (ignoreTimeoutIfHigher)
    {
      requestContext.putLocalAttr(R2Constants.REQUEST_TIMEOUT_IGNORE_IF_HIGHER_THAN_DEFAULT, ignoreTimeoutIfHigher);
    }
    client.restRequest(restRequest, requestContext, restCallback);

    clockedExecutor.run(expectedTimeout - 10).get();
    Assert.assertFalse(checkTimeoutFired(restCallback));
    checkRequestTimeoutOrViewSet(requestContext);

    clockedExecutor.run(expectedTimeout + 10).get();
    Assert.assertTrue(checkTimeoutFired(restCallback));
    checkRequestTimeoutOrViewSet(requestContext);

  }

  boolean checkTimeoutFired(TrackerClientTest.TestCallback<RestResponse> restCallback)
  {
    assertNull(restCallback.t);
    return restCallback.e instanceof TimeoutException;
  }

  void checkRequestTimeoutOrViewSet(RequestContext restCallback)
  {
    Assert.assertTrue(restCallback.getLocalAttr(R2Constants.REQUEST_TIMEOUT) != null
        || restCallback.getLocalAttr(R2Constants.CLIENT_REQUEST_TIMEOUT_VIEW) != null,
      "Either the REQUEST_TIMEOUT or CLIENT_REQUEST_TIMEOUT_VIEW should always be set," +
        " in such a way that parts of code that don't have access to the default timeout, can still know " +
        "the expected timeout");
  }
}
