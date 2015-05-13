/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.examples;


import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.GetRequest;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.client.TyperefPrimitiveLongAssociationKeyResourceBuilders;
import com.linkedin.restli.internal.common.TestConstants;

import java.util.Collections;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author nshakar
 */
public class TestTyperefPrimitiveLongAssociationKeyResource extends RestLiIntegrationTest
{
  private static final Client CLIENT =
      new TransportClientAdapter(new HttpClientFactory().getClient(Collections.<String, String> emptyMap()));
  private static final String URI_PREFIX = "http://localhost:1338/";
  private static final RestClient REST_CLIENT = new RestClient(CLIENT, URI_PREFIX);

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

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  public void testGet(RestliRequestOptions requestOptions) throws RemoteInvocationException
  {
    GetRequest<Message> req =
        new TyperefPrimitiveLongAssociationKeyResourceBuilders(requestOptions).get()
                                                                              .id(new CompoundKey().append("src", 1)
                                                                                                   .append("dest", 2))
                                                                              .build();
    Response<Message> resp = REST_CLIENT.sendRequest(req).getResponse();
    Message result = resp.getEntity();
    Assert.assertEquals(result.getId(), "1->2");
    Assert.assertEquals(result.getMessage(), "I need some $20");
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  private static Object[][] requestOptionsDataProvider()
  {
    return new Object[][] { { RestliRequestOptions.DEFAULT_OPTIONS },
        { com.linkedin.restli.examples.TestConstants.FORCE_USE_NEXT_OPTIONS } };
  }
}
