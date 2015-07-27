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

package com.linkedin.multipart.integ;


import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.server.HttpServer;
import com.linkedin.r2.transport.http.server.HttpServerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;


/**
 * Abstract class for async multipart mime integration tests.
 *
 * @author Karim Vidhani
 */
public abstract class AbstractMIMEIntegrationStreamTest
{
  protected static final int PORT = 8388;
  protected static final int TEST_TIMEOUT = 30000;
  protected HttpServer _server;
  protected TransportClientFactory _clientFactory;
  protected Client _client;

  @BeforeMethod
  public void setup() throws IOException
  {
    _clientFactory = getClientFactory();
    _client = new TransportClientAdapter(_clientFactory.getClient(getClientProperties()));
    _server = getServerFactory().createServer(PORT, getTransportDispatcher(), true);
    _server.start();
  }

  @AfterMethod
  public void tearDown() throws Exception
  {
    final FutureCallback<None> clientShutdownCallback = new FutureCallback<None>();
    _client.shutdown(clientShutdownCallback);
    clientShutdownCallback.get();

    final FutureCallback<None> factoryShutdownCallback = new FutureCallback<None>();
    _clientFactory.shutdown(factoryShutdownCallback);
    factoryShutdownCallback.get();

    if (_server != null)
    {
      _server.stop();
      _server.waitForStop();
    }
  }

  protected abstract TransportDispatcher getTransportDispatcher();

  protected TransportClientFactory getClientFactory()
  {
    return new HttpClientFactory();
  }

  protected Map<String, String> getClientProperties()
  {
    return Collections.emptyMap();
  }

  protected HttpServerFactory getServerFactory()
  {
    return new HttpServerFactory();
  }

  protected static Callback<StreamResponse> expectSuccessCallback(final CountDownLatch latch,
      final AtomicInteger status, final Map<String, String> headers)
  {
    return new Callback<StreamResponse>()
    {
      @Override
      public void onError(Throwable e)
      {
        latch.countDown();
      }

      @Override
      public void onSuccess(StreamResponse result)
      {
        status.set(result.getStatus());
        headers.putAll(result.getHeaders());
        latch.countDown();
      }
    };
  }
}