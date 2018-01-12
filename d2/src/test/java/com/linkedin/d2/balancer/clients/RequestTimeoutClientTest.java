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
import com.linkedin.d2.balancer.util.ClientFactoryProvider;
import com.linkedin.d2.balancer.util.DelegatingFacilities;
import com.linkedin.d2.balancer.util.DirectoryProvider;
import com.linkedin.d2.balancer.util.KeyMapperProvider;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.test.util.AssertionMethods;
import com.linkedin.test.util.DataGeneration;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import org.mockito.Mockito;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.d2.balancer.clients.TrackerClientTest.TestClient.DEFAULT_REQUEST_TIMEOUT;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;


/**
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class RequestTimeoutClientTest
{
  @DataProvider
  public static Object[][] allCombinations2x()
  {
    return DataGeneration.generateAllBooleanCombinationMatrix(2);
  }

  /**
   * It's expected to be able to make a request with any request timeout
   */
  @Test(groups = {"small"}, dataProvider = "allCombinations2x")
  @SuppressWarnings("deprecation")
  public void testRequestTimeoutAllowed(boolean restOverStream, boolean isHigherThanDefault) throws Exception
  {
    LoadBalancerMock balancer = new LoadBalancerMock(false, true);
    DirectoryProvider dirProvider = new DirectoryProviderMock();
    KeyMapperProvider keyMapperProvider = new KeyMapperProviderMock();
    ClientFactoryProvider clientFactoryProvider = Mockito.mock(ClientFactoryProvider.class);
    Facilities facilities = new DelegatingFacilities(dirProvider, keyMapperProvider, clientFactoryProvider);
    D2Client client = new DynamicClient(balancer, facilities, restOverStream);
    URI uri = URI.create("d2://test");
    RestRequest restRequest = new RestRequestBuilder(uri).build();

    client = new RequestTimeoutClient(client, balancer, Executors.newSingleThreadScheduledExecutor());

    RequestContext requestContext = new RequestContext();

    int requestTimeout = isHigherThanDefault ? DEFAULT_REQUEST_TIMEOUT + 100 : DEFAULT_REQUEST_TIMEOUT - 100;

    TrackerClientTest.TestCallback<RestResponse> restCallback = new TrackerClientTest.TestCallback<>();
    requestContext.putLocalAttr(R2Constants.REQUEST_TIMEOUT, requestTimeout);
    client.restRequest(restRequest, requestContext, restCallback);

    // it should always throw a timeout exception
    AssertionMethods.assertWithTimeout(2000, () -> assertTrue(restCallback.e instanceof TimeoutException));
    assertNull(restCallback.t);
  }
}
