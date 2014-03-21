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


import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.data.DataMap;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rpc.RpcRequest;
import com.linkedin.r2.message.rpc.RpcResponse;
import com.linkedin.r2.transport.common.AbstractClient;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.examples.RestLiIntegrationTest;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.GreetingsBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsRequestBuilders;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Tests that the protocol version header sent to the server is sent back to the client.
 *
 * @author kparikh
 */
public class TestGreetingsClientProtocolVersionHeader extends RestLiIntegrationTest
{
  private static final TransportClientFactory clientFactory = new HttpClientFactory();
  private static final String uriPrefix = "http://localhost:1338/";
  private static final RestClient _REST_CLIENT = new RestClient(new PropertyProviderClient(AllProtocolVersions.BASELINE_PROTOCOL_VERSION.toString()),
                                                                uriPrefix);

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

  private static class PropertyProviderClient extends AbstractClient
  {
    private final Map<String, Object> __metadata;
    private final Client __client;

    public PropertyProviderClient(String restliProtocolVersion)
    {
      __metadata = new HashMap<String, Object>();
      __metadata.put(RestConstants.RESTLI_PROTOCOL_VERSION_PROPERTY, restliProtocolVersion);
      __client = new TransportClientAdapter(clientFactory.getClient(Collections.<String, String>emptyMap()));
    }

    @Override
    public void restRequest(RestRequest request, RequestContext requestContext, Callback<RestResponse> callback)
    {
      __client.restRequest(request, requestContext, callback);
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public void rpcRequest(RpcRequest request, RequestContext requestContext, Callback<RpcResponse> callback)
    {
      __client.rpcRequest(request, requestContext, callback);
    }

    @Override
    public void shutdown(Callback<None> callback)
    {
      __client.shutdown(callback);
    }

    @Override
    public Map<String, Object> getMetadata(URI uri)
    {
      return __metadata;
    }
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testProtocolVersionHeaderRoundtrip(RootBuilderWrapper<Long, Greeting> builders, ProtocolVersion version)
      throws RemoteInvocationException
  {
    final Request<Greeting> getRequest = builders.get().id(1L).build();
    checkProtocolVersionHeader(getRequest, version);
  }

  private void checkProtocolVersionHeader(Request<Greeting> request,
                                          ProtocolVersion expectedProtocolVersion)
      throws RemoteInvocationException
  {
    ResponseFuture<Greeting> responseFuture = _REST_CLIENT.sendRequest(request);
    Assert.assertEquals(responseFuture.getResponse().getHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION), expectedProtocolVersion.toString());
  }

  @Test
  public void testNoProtocolVersionHeaderSuccess() throws InterruptedException, ExecutionException
  {
    final TransportClientAdapter client = new TransportClientAdapter(clientFactory.getClient(Collections.<String, String>emptyMap()));
    final RestRequestBuilder requestBuilder = new RestRequestBuilder(URI.create(uriPrefix + "greetings/1"));
    final RestRequest request = requestBuilder.build();
    Assert.assertTrue(request.getHeaders().isEmpty());

    final RestResponse response = client.restRequest(request).get();
    Assert.assertEquals(response.getStatus(), HttpStatus.S_200_OK.getCode());
    Assert.assertEquals(response.getHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION),
                        AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion().toString());
  }

  @Test
  public void testNoProtocolVersionHeaderFail() throws InterruptedException
  {
    final TransportClientAdapter client = new TransportClientAdapter(clientFactory.getClient(Collections.<String, String>emptyMap()));
    final RestRequestBuilder requestBuilder = new RestRequestBuilder(URI.create(uriPrefix));
    final RestRequest request = requestBuilder.build();
    Assert.assertTrue(request.getHeaders().isEmpty());

    try
    {
      client.restRequest(request).get();
    }
    catch (ExecutionException e)
    {
      final RestException exception = (RestException) e.getCause();
      final RestResponse response = exception.getResponse();
      Assert.assertEquals(response.getStatus(), HttpStatus.S_404_NOT_FOUND.getCode());
      Assert.assertEquals(response.getHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION),
                          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion().toString());

      final DataMap exceptionDetail = DataMapUtils.readMap(response.getEntity().asInputStream());
      Assert.assertEquals(exceptionDetail.getString("exceptionClass"), RestLiServiceException.class.getName());
    }
  }

  @DataProvider
  @SuppressWarnings("rawtypes")
  private static Object[][] requestBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper(new GreetingsBuilders(new RestliRequestOptionsBuilder().setProtocolVersionOption(ProtocolVersionOption.USE_LATEST_IF_AVAILABLE).build())), AllProtocolVersions.BASELINE_PROTOCOL_VERSION },
      { new RootBuilderWrapper(new GreetingsBuilders(new RestliRequestOptionsBuilder().setProtocolVersionOption(ProtocolVersionOption.FORCE_USE_LATEST).build())), AllProtocolVersions.LATEST_PROTOCOL_VERSION },
      { new RootBuilderWrapper(new GreetingsRequestBuilders(new RestliRequestOptionsBuilder().setProtocolVersionOption(ProtocolVersionOption.USE_LATEST_IF_AVAILABLE).build())), AllProtocolVersions.BASELINE_PROTOCOL_VERSION },
      { new RootBuilderWrapper(new GreetingsRequestBuilders(new RestliRequestOptionsBuilder().setProtocolVersionOption(ProtocolVersionOption.FORCE_USE_LATEST).build())), AllProtocolVersions.LATEST_PROTOCOL_VERSION }
    };
  }
}
