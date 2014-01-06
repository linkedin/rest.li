/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.client;

import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.GetRequest;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.examples.RestLiIntegrationTest;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.GreetingsBuilders;
import java.util.Collections;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author kparikh
 */
public class TestGreetingsClientProtocolVersionHeader extends RestLiIntegrationTest
{
  private static final Client CLIENT = new TransportClientAdapter(new HttpClientFactory().getClient(
      Collections.<String, String>emptyMap()));
  private static final String URI_PREFIX = "http://localhost:1338/";
  private final GreetingsBuilders GREETINGS_BUILDERS = new GreetingsBuilders("greetings");

  @BeforeClass
  public void initClass() throws Exception
  {
    super.init();
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  @DataProvider(name = "protocolVersionClient")
  public Object[][] getProtocolVersionClient()
  {
    return new Object[][]
        {
            { new RestClient(CLIENT, URI_PREFIX), RestConstants.DEFAULT_PROTOCOL_VERSION.toString() },
            { new RestClient(CLIENT,
                             URI_PREFIX,
                             RestClient.ContentType.JSON,
                             Collections.<RestClient.AcceptType>emptyList(),
                             Collections.singletonMap("greetings", "42.42.42")),
                "42.42.42" }
        };
  }

  @Test(dataProvider = "protocolVersionClient")
  public void testProtocolVersionHeader(RestClient restClient, String expectedProtocolVersion)
      throws RemoteInvocationException
  {
    GetRequest<Greeting> request = GREETINGS_BUILDERS.get().id(1L).build();
    Response<Greeting> response = restClient.sendRequest(request).getResponse();
    Assert.assertEquals(response.getHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION), expectedProtocolVersion);
  }
}
