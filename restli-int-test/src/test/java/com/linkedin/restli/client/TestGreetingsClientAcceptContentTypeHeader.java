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

package com.linkedin.restli.client;


import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.common.ContentType;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.examples.RestLiIntegrationTest;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.GreetingsBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsRequestBuilders;
import com.linkedin.restli.internal.common.TestConstants;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.util.Arrays;
import java.util.Collections;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Tests that correct accept type header is sent to the server, and then correct content type header is sent back to the client.
 *
 * @author Min Chen
 */
public class TestGreetingsClientAcceptContentTypeHeader extends RestLiIntegrationTest
{
  private static final Client            CLIENT      = new TransportClientAdapter(new HttpClientFactory.Builder().build().getClient(Collections.<String, String>emptyMap()));
  private static final String            URI_PREFIX  = "http://localhost:1338/";
  private static final RestClient        REST_CLIENT = new RestClient(CLIENT, URI_PREFIX);

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

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "acceptContentTypeDataProvider")
  private static Object[][] acceptContentTypeDataProvider()
  {
    return new Object[][] {
        {
            new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders(new RestliRequestOptionsBuilder().setAcceptTypes(Collections.emptyList()).build())),
            RestConstants.HEADER_VALUE_APPLICATION_JSON
        },
        {
            new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders(new RestliRequestOptionsBuilder().setAcceptTypes(Collections.singletonList(
                ContentType.ACCEPT_TYPE_ANY)).build())),
            RestConstants.HEADER_VALUE_APPLICATION_JSON
        },
        {
            new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders(new RestliRequestOptionsBuilder().setAcceptTypes(Collections.singletonList(
                ContentType.JSON)).build())),
            RestConstants.HEADER_VALUE_APPLICATION_JSON
        },
        {
            new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders(new RestliRequestOptionsBuilder().setAcceptTypes(Collections.singletonList(
                ContentType.PSON)).build())),
            RestConstants.HEADER_VALUE_APPLICATION_PSON
        },
        {
            new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders(new RestliRequestOptionsBuilder().setAcceptTypes(Arrays.asList(
                ContentType.PSON, ContentType.JSON)).build())),
            RestConstants.HEADER_VALUE_APPLICATION_PSON
        },
        {
            new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders(new RestliRequestOptionsBuilder().setAcceptTypes(Arrays.asList(
                ContentType.JSON, ContentType.PSON)).build())),
            RestConstants.HEADER_VALUE_APPLICATION_JSON
        },
        {
            new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(new RestliRequestOptionsBuilder().setAcceptTypes(Collections.emptyList()).build())),
            RestConstants.HEADER_VALUE_APPLICATION_JSON
        },
        {
            new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(new RestliRequestOptionsBuilder().setAcceptTypes(Collections.singletonList(
                ContentType.ACCEPT_TYPE_ANY)).build())),
            RestConstants.HEADER_VALUE_APPLICATION_JSON
        },
        {
            new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(new RestliRequestOptionsBuilder().setAcceptTypes(Collections.singletonList(
                ContentType.JSON)).build())),
            RestConstants.HEADER_VALUE_APPLICATION_JSON
        },
        {
            new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(new RestliRequestOptionsBuilder().setAcceptTypes(Collections.singletonList(
                ContentType.PSON)).build())),
            RestConstants.HEADER_VALUE_APPLICATION_PSON
        },
        {
            new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(new RestliRequestOptionsBuilder().setAcceptTypes(Arrays.asList(
                ContentType.PSON, ContentType.JSON)).build())),
            RestConstants.HEADER_VALUE_APPLICATION_PSON
        },
        {
            new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(new RestliRequestOptionsBuilder().setAcceptTypes(Arrays.asList(
                ContentType.JSON, ContentType.PSON)).build())),
            RestConstants.HEADER_VALUE_APPLICATION_JSON
        },
        {
            new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(new RestliRequestOptionsBuilder().setAcceptTypes(Collections.singletonList(
                ContentType.LICOR_TEXT)).build())),
            RestConstants.HEADER_VALUE_APPLICATION_LICOR_TEXT
        },
        {
            new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(new RestliRequestOptionsBuilder().setAcceptTypes(Collections.singletonList(
                ContentType.LICOR_BINARY)).build())),
            RestConstants.HEADER_VALUE_APPLICATION_LICOR_BINARY
        },
        {
            new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(new RestliRequestOptionsBuilder().setAcceptTypes(Collections.singletonList(
                ContentType.PROTOBUF)).build())),
            RestConstants.HEADER_VALUE_APPLICATION_PROTOBUF
        },
        {
            new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(new RestliRequestOptionsBuilder().setAcceptTypes(Collections.singletonList(
                ContentType.PROTOBUF2)).build())),
            RestConstants.HEADER_VALUE_APPLICATION_PROTOBUF2
        }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "acceptContentTypeDataProvider")
  public void testAcceptContentTypeHeaderRoundtrip(RootBuilderWrapper<Long, Greeting> builder, String expectedContentType)
      throws RemoteInvocationException
  {
    final Request<Greeting> getRequest = builder.get().id(10L).build();
    ResponseFuture<Greeting> responseFuture = REST_CLIENT.sendRequest(getRequest);
    Response<Greeting> response = responseFuture.getResponse();
    Assert.assertEquals(response.getHeader(RestConstants.HEADER_CONTENT_TYPE), expectedContentType.toString());
    Assert.assertTrue(response.getEntity().getId() == 10L);
  }

}
