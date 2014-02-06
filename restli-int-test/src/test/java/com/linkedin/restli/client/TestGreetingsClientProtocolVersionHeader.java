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
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rpc.RpcRequest;
import com.linkedin.r2.message.rpc.RpcResponse;
import com.linkedin.r2.transport.common.AbstractClient;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.examples.RestLiIntegrationTest;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.GreetingsBuilders;
import com.linkedin.restli.internal.common.AllProtocolVersions;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * Tests that the protocol version header sent to the server is sent back to the client.
 *
 * @author kparikh
 */
public class TestGreetingsClientProtocolVersionHeader extends RestLiIntegrationTest
{
  private static final RestClient _REST_CLIENT;

  static
  {
    _REST_CLIENT = new RestClient(new PropertyProviderClient(AllProtocolVersions.DEFAULT_PROTOCOL_VERSION.toString()),
                                  "http://localhost:1338/");
  }

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
      __client = new TransportClientAdapter(new HttpClientFactory().getClient(Collections.<String, String>emptyMap()));
    }

    @Override
    public void restRequest(RestRequest request, RequestContext requestContext, Callback<RestResponse> callback)
    {
      __client.restRequest(request, requestContext, callback);
    }

    @Override
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

  @Test
  public void testProtocolVersionHeaderRoundtrip()
      throws RemoteInvocationException
  {
    GreetingsBuilders defaultBuilder = new GreetingsBuilders();
    GetRequest<Greeting> getRequest = defaultBuilder.get().id(1L).build();
    checkProtocolVersionHeader(getRequest, AllProtocolVersions.DEFAULT_PROTOCOL_VERSION);

    GreetingsBuilders forceBuilders =
        new GreetingsBuilders(new RestliRequestOptionsBuilder().setProtocolVersionOption(ProtocolVersionOption.FORCE_USE_LATEST).build());
    getRequest = forceBuilders.get().id(1L).build();
    checkProtocolVersionHeader(getRequest, AllProtocolVersions.LATEST_PROTOCOL_VERSION);
  }

  private void checkProtocolVersionHeader(GetRequest<Greeting> request,
                                          ProtocolVersion expectedProtocolVersion)
      throws RemoteInvocationException
  {
    ResponseFuture<Greeting> responseFuture = _REST_CLIENT.sendRequest(request);
    Assert.assertEquals(responseFuture.getResponse().getHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION),
                        expectedProtocolVersion.toString());
  }
}
