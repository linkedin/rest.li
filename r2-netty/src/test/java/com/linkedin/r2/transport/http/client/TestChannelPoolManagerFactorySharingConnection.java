/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.client;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.testutils.server.HttpServerBuilder;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;

import io.netty.handler.codec.http.HttpMethod;
import org.eclipse.jetty.server.Server;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class TestChannelPoolManagerFactorySharingConnection
{
  /**
   * END-TO-END test section
   */
  private static final int TRIALS = 3;
  private static final int TYPE_PER_TRIAL = 2;
  private static final int NUMBER_OF_CLIENTS = TRIALS * TYPE_PER_TRIAL;
  private static final int NUMBER_OF_REQUESTS = TRIALS * TYPE_PER_TRIAL;

  // Value=NUMBER_OF_CLIENTS because each getClient creates a new pool which establish a separate connection
  private static final int OPENED_CONNECTIONS_WITHOUT_SHARING = NUMBER_OF_CLIENTS;
  // Value=2 because there are only two (out of 3) types of configuration that generate a new client
  private static final int OPENED_CONNECTIONS_WITH_SHARING = 2;

  @DataProvider
  public static Object[][] configsOpenedConnections()
  {
    return new Object[][]{
      // restOverStream, protocolVersion, shareConnection
      {true, TestHttpClientFactory.HTTP_1_1, false},
      {true, TestHttpClientFactory.HTTP_2, false},
      {false, TestHttpClientFactory.HTTP_1_1, false},
      {false, TestHttpClientFactory.HTTP_2, false},
      {true, TestHttpClientFactory.HTTP_1_1, true},
      {true, TestHttpClientFactory.HTTP_2, true},
      {false, TestHttpClientFactory.HTTP_1_1, true},
      {false, TestHttpClientFactory.HTTP_2, true}
    };
  }

  /**
   * End to end test. Testing all the client combinations (http/https stream/rest sharing/not sharing) and check they
   * are using the same channelPoolManager
   */
  @Test(dataProvider = "configsOpenedConnections")
  public void testSuccessfulRequests(boolean restOverStream, String protocolVersion, boolean shareConnection) throws Exception
  {

    makeRequestsWithClients(shareConnection, (clients, clientFactory) ->
      {
        // standard
        HashMap<String, String> properties = new HashMap<>();
        properties.put(HttpClientFactory.HTTP_PROTOCOL_VERSION, protocolVersion);
        properties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, String.valueOf(10000));
        clients.add(new TransportClientAdapter(clientFactory.getClient(properties), restOverStream));

        // with parameter that should NOT create a new ChannelPoolManager
        properties = new HashMap<>();
        properties.put(HttpClientFactory.HTTP_PROTOCOL_VERSION, protocolVersion);
        properties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, String.valueOf(2000)); // property NOT of the ChannelPoolManager
        clients.add(new TransportClientAdapter(clientFactory.getClient(properties), restOverStream));
      },
      // since the two clients have the same settings, with sharing, it should just open 1 connections
      1);
  }

  /**
   * End to end test. Testing all the client combinations (http/https stream/rest sharing/not sharing) and check they
   * are NOT using the same channelPoolManager
   */
  @Test(dataProvider = "configsOpenedConnections")
  public void testSuccessfulRequestds(boolean restOverStream, String protocolVersion, boolean shareConnection) throws Exception
  {
    makeRequestsWithClients(shareConnection, (clients, clientFactory) ->
      {
        // standard
        HashMap<String, String> properties = new HashMap<>();
        properties.put(HttpClientFactory.HTTP_PROTOCOL_VERSION, protocolVersion);
        properties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, String.valueOf(10000));
        clients.add(new TransportClientAdapter(clientFactory.getClient(properties), restOverStream));


        // with parameter that SHOULD create a new ChannelPoolManager
        properties = new HashMap<>();
        properties.put(HttpClientFactory.HTTP_PROTOCOL_VERSION, protocolVersion);
        properties.put(HttpClientFactory.HTTP_MAX_CHUNK_SIZE, String.valueOf(100)); // property of the ChannelPoolManager
        properties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, String.valueOf(10000));
        clients.add(new TransportClientAdapter(clientFactory.getClient(properties), restOverStream));
      },
      // since the two clients have different settings, with sharing, it should just open 2 connections
      2
    );

  }


  /**
   * Helper function that creates clientFactory, makes request sequentially, checks the result and shutdowns everything
   */
  public void makeRequestsWithClients(boolean shareConnection, ClientGenerator clientGenerator, int expectedConnectionsWithSharing) throws Exception
  {
    HttpClientFactory clientFactory = new HttpClientFactory.Builder().setShareConnection(shareConnection).build();

    HttpServerBuilder.HttpServerStatsProvider httpServerStatsProvider = getHttpServerStatsProviderIgnoringOptions();

    Server server = new HttpServerBuilder().serverStatsProvider(httpServerStatsProvider).build();
    try
    {
      server.start();
      List<Client> clients = new ArrayList<>();
      for (int i = 0; i < TRIALS; i++)
      {
        clientGenerator.populate(clients, clientFactory);
      }

      for (Client c : clients)
      {
        RestRequest r = new RestRequestBuilder(new URI(TestHttpClientFactory.URI)).build();
        c.restRequest(r).get(30, TimeUnit.SECONDS);

        FutureCallback<None> shutdownCallback = new FutureCallback<>();
        c.shutdown(shutdownCallback);
        shutdownCallback.get(20, TimeUnit.SECONDS);

      }
      Assert.assertEquals(httpServerStatsProvider.requestCount(), NUMBER_OF_REQUESTS);

      int expectedOpenedConnections = shareConnection ? expectedConnectionsWithSharing : OPENED_CONNECTIONS_WITHOUT_SHARING;

      Assert.assertEquals(httpServerStatsProvider.clientConnections().size(), expectedOpenedConnections);
    }
    finally
    {
      server.stop();
    }

    // shutdown the client factory which will trigger the ChannelPoolManagerFactorySharingConnecion shutdown
    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    clientFactory.shutdown(shutdownCallback);
    shutdownCallback.get(10, TimeUnit.SECONDS);
  }

  interface ClientGenerator
  {
    void populate(List<Client> clients, HttpClientFactory clientFactory);
  }

  /**
   * Http2 connections make also OPTIONS requests, that we don't want to count
   */
  private HttpServerBuilder.HttpServerStatsProvider getHttpServerStatsProviderIgnoringOptions()
  {
    return new HttpServerBuilder.HttpServerStatsProvider(req -> !req.getMethod().equals(HttpMethod.OPTIONS.name()));
  }
}
