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
  private static final TransportClientFactory CLIENT_FACTORY = new HttpClientFactory.Builder().build();
  private static final String URI_PREFIX = "http://localhost:1338/";

  private static final PropertyProviderClient BASELINE_PROVIDER =
      new PropertyProviderClient(AllProtocolVersions.BASELINE_PROTOCOL_VERSION.toString());

  private static final RestClient PROPERTY_PROVIDING_REST_CLIENT = new RestClient(BASELINE_PROVIDER, URI_PREFIX);
  private static final RestClient NO_PROPERTY_REST_CLIENT = new RestClient(new PropertyProviderClient(), URI_PREFIX);

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
    private final Map<String, Object> _metadata;
    private final Client _client;

    public PropertyProviderClient()
    {
      this(null);
    }

    public PropertyProviderClient(String restliProtocolVersion)
    {
      _metadata = new HashMap<>();
      if (restliProtocolVersion != null)
      {
        _metadata.put(RestConstants.RESTLI_PROTOCOL_VERSION_PROPERTY, restliProtocolVersion);
      }
      _client = new TransportClientAdapter(CLIENT_FACTORY.getClient(Collections.<String, String>emptyMap()));
    }

    @Override
    public void restRequest(RestRequest request, RequestContext requestContext, Callback<RestResponse> callback)
    {
      _client.restRequest(request, requestContext, callback);
    }

    @Override
    public void shutdown(Callback<None> callback)
    {
      _client.shutdown(callback);
    }

    @Override
    public void getMetadata(URI uri, Callback<Map<String, Object>> callback)
    {
      callback.onSuccess(_metadata);
    }
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testProtocolVersionHeaderRoundtrip(RootBuilderWrapper<Long, Greeting> builders, ProtocolVersion version)
      throws RemoteInvocationException
  {
    final Request<Greeting> getRequest = builders.get().id(1L).build();
    checkProtocolVersionHeader(PROPERTY_PROVIDING_REST_CLIENT, getRequest, version);
    checkProtocolVersionHeader(NO_PROPERTY_REST_CLIENT, getRequest, version);
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testForceUseNextVersionOverride(RootBuilderWrapper<Long, Greeting> builders, ProtocolVersion version)
      throws RemoteInvocationException
  {
    final Request<Greeting> getRequest = builders.get().id(1L).build();

    testForceUseNextVersionOverride(getRequest, "true", AllProtocolVersions.NEXT_PROTOCOL_VERSION);
    // version is the version we expect if the standard handshake takes place
    testForceUseNextVersionOverride(getRequest, "false", version);
    // null is used to simulate the scenario where no value has been set for this system property
    testForceUseNextVersionOverride(getRequest, null, version);
  }

  private void testForceUseNextVersionOverride(Request<Greeting> request,
                                               String override,
                                               ProtocolVersion expectedProtocolVersion)
      throws RemoteInvocationException
  {
    if (override != null)
    {
      System.setProperty(RestConstants.RESTLI_FORCE_USE_NEXT_VERSION_OVERRIDE, override);
    }

    RestClient restClient = new RestClient(BASELINE_PROVIDER, URI_PREFIX);
    checkProtocolVersionHeader(restClient, request, expectedProtocolVersion);

    System.clearProperty(RestConstants.RESTLI_FORCE_USE_NEXT_VERSION_OVERRIDE);
  }

  private void checkProtocolVersionHeader(RestClient restClient,
                                          Request<Greeting> request,
                                          ProtocolVersion expectedProtocolVersion)
      throws RemoteInvocationException
  {
    ResponseFuture<Greeting> responseFuture = restClient.sendRequest(request);
    Assert.assertEquals(responseFuture.getResponse().getHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION), expectedProtocolVersion.toString());
  }

  @Test
  public void testNoProtocolVersionHeaderSuccess() throws InterruptedException, ExecutionException
  {
    final TransportClientAdapter client = new TransportClientAdapter(CLIENT_FACTORY.getClient(Collections.<String, String>emptyMap()));
    final RestRequestBuilder requestBuilder = new RestRequestBuilder(URI.create(URI_PREFIX + "greetings/1"));
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
    final TransportClientAdapter client = new TransportClientAdapter(CLIENT_FACTORY.getClient(Collections.<String, String>emptyMap()));
    final RestRequestBuilder requestBuilder = new RestRequestBuilder(URI.create(URI_PREFIX));
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

      final DataMap exceptionDetail = DataMapUtils.readMap(response.getEntity().asInputStream(), response.getHeaders());
      Assert.assertEquals(exceptionDetail.getString("exceptionClass"), RestLiServiceException.class.getName());
    }
  }

  @DataProvider
  private static Object[][] requestBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(new RestliRequestOptionsBuilder().setProtocolVersionOption(ProtocolVersionOption.USE_LATEST_IF_AVAILABLE).build())), AllProtocolVersions.BASELINE_PROTOCOL_VERSION },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(new RestliRequestOptionsBuilder().setProtocolVersionOption(ProtocolVersionOption.FORCE_USE_LATEST).build())), AllProtocolVersions.LATEST_PROTOCOL_VERSION },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(new RestliRequestOptionsBuilder().setProtocolVersionOption(ProtocolVersionOption.FORCE_USE_NEXT).build())), AllProtocolVersions.NEXT_PROTOCOL_VERSION },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(new RestliRequestOptionsBuilder().setProtocolVersionOption(ProtocolVersionOption.FORCE_USE_PREVIOUS).build())), AllProtocolVersions.PREVIOUS_PROTOCOL_VERSION }
    };
  }
}
