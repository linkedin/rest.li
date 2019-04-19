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

package test.r2.integ.clientserver;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.disruptor.DisruptContexts;
import com.linkedin.r2.disruptor.DisruptedException;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.testng.Assert;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import test.r2.integ.clientserver.providers.AbstractServiceTest;
import test.r2.integ.clientserver.providers.ClientServerConfiguration;
import test.r2.integ.clientserver.providers.client.ClientProvider;
import test.r2.integ.clientserver.providers.server.ServerProvider;


/**
 * @author Sean Sheng
 * @author Nizar Mankulangara
 * @version $Revision$
 */
public class TestDisruptor extends AbstractServiceTest
{
  private static final String DISRUPT_CONTEXT_KEY = "R2_DISRUPT_CONTEXT";
  private static final String PATH = "/headerEcho";
  private static final int REQUEST_TIMEOUT = 0;
  private static final long REQUEST_LATENCY = 0;
  private static final int TEST_TIMEOUT = 5000;

  @Factory(dataProvider = "allStreamCombinations", dataProviderClass = ClientServerConfiguration.class)
  public TestDisruptor(ClientProvider clientProvider, ServerProvider serverProvider, int port)
  {
    super(clientProvider, serverProvider, port);
  }

  @Override
  protected TransportDispatcher getTransportDispatcher()
  {
    return new TransportDispatcherBuilder(true)
        //.addRestHandler(URI.create(PATH), new HeaderEchoHandler())
        .addStreamHandler(URI.create(PATH), new HeaderEchoHandler())
        .build();
  }

  @Test
  public void testRestNoDisrupt() throws Exception
  {
    System.out.println(_serverProvider);

    final RequestContext requestContext = new RequestContext();

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean success = new AtomicBoolean(false);
    _client.restRequest(new RestRequestBuilder(getHttpURI()).build(),
        requestContext,
          new Callback<RestResponse>(){
            @Override
            public void onSuccess(RestResponse result)
            {
              success.set(true);
              latch.countDown();
            }
            @Override
            public void onError(Throwable e)
            {
              success.set(false);
              latch.countDown();
            }
          });


    Assert.assertTrue(latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS), "Test execution timeout");
    Assert.assertTrue(success.get(), "Unexpected transport response");
  }

  private URI getHttpURI() {
    return _clientProvider.createHttpURI(_port, URI.create(PATH));
  }

  @Test
  public void testStreamNoDisrupt() throws Exception
  {
    final RequestContext requestContext = new RequestContext();

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean success = new AtomicBoolean(false);
    _client.streamRequest(new StreamRequestBuilder(getHttpURI()).build(EntityStreams.emptyStream()),
        requestContext, new Callback<StreamResponse>(){
          @Override
          public void onSuccess(StreamResponse result)
          {
            success.set(true);
            latch.countDown();
          }
          @Override
          public void onError(Throwable e)
          {
            success.set(false);
            latch.countDown();
          }
        });

    Assert.assertTrue(latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS), "Test execution timeout");
    Assert.assertTrue(success.get(), "Unexpected transport response");
  }

  @Test
  public void testRestLatencyDisrupt() throws Exception
  {
    final RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(DISRUPT_CONTEXT_KEY, DisruptContexts.delay(REQUEST_LATENCY));

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean success = new AtomicBoolean(false);
    _client.restRequest(new RestRequestBuilder(getHttpURI()).build(), requestContext,
        new Callback<RestResponse>(){
          @Override
          public void onSuccess(RestResponse result)
          {
            success.set(true);
            latch.countDown();
          }
          @Override
          public void onError(Throwable e)
          {
            success.set(false);
            latch.countDown();
          }
        });

    Assert.assertTrue(latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS), "Test execution timeout");
    Assert.assertTrue(success.get(), "Unexpected transport response");
  }


  @Test
  public void testStreamLatencyDisrupt() throws Exception
  {
    final RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(DISRUPT_CONTEXT_KEY, DisruptContexts.delay(REQUEST_LATENCY));

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean success = new AtomicBoolean(false);
    _client.streamRequest(new StreamRequestBuilder(getHttpURI()).build(EntityStreams.emptyStream()),
        requestContext, new Callback<StreamResponse>(){
          @Override
          public void onSuccess(StreamResponse result)
          {
            success.set(true);
            latch.countDown();
          }
          @Override
          public void onError(Throwable e)
          {
            success.set(false);
            latch.countDown();
          }
        });

    Assert.assertTrue(latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS), "Test execution timeout");
    Assert.assertTrue(success.get(), "Unexpected transport response");
  }

  @Test
  public void testRestTimeoutDisrupt() throws Exception
  {

    final Map<String, Object> properties = new HashMap<>();
    properties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, String.valueOf(REQUEST_TIMEOUT));
    final Client client = _clientProvider.createClient(FilterChains.empty(), properties);

    final RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(DISRUPT_CONTEXT_KEY, DisruptContexts.timeout());

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean success = new AtomicBoolean(false);
    client.restRequest(new RestRequestBuilder(getHttpURI()).build(), requestContext,
        new Callback<RestResponse>(){
          @Override
          public void onSuccess(RestResponse result)
          {
            latch.countDown();
          }
          @Override
          public void onError(Throwable e)
          {
            success.set(e instanceof TimeoutException);
            latch.countDown();
          }
        });

    Assert.assertTrue(latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS), "Test execution timeout");
    Assert.assertTrue(success.get(), "Unexpected transport response");
  }


  @Test
  public void testStreamTimeoutDisrupt() throws Exception
  {
    final Map<String, Object> properties = new HashMap<>();
    properties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, String.valueOf(REQUEST_TIMEOUT));
    final Client client = _clientProvider.createClient(FilterChains.empty(), properties);

    final RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(DISRUPT_CONTEXT_KEY, DisruptContexts.timeout());

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean success = new AtomicBoolean(false);
    client.streamRequest(new StreamRequestBuilder(getHttpURI()).build(EntityStreams.emptyStream()),
        requestContext, new Callback<StreamResponse>(){
          @Override
          public void onSuccess(StreamResponse result)
          {
            latch.countDown();
          }
          @Override
          public void onError(Throwable e)
          {
            success.set(e instanceof TimeoutException);
            latch.countDown();
          }
        });

    Assert.assertTrue(latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS), "Test execution timeout");
    Assert.assertTrue(success.get(), "Unexpected transport response");
  }


  @Test
  public void testRestErrorDisrupt() throws Exception
  {
    final Map<String, Object> properties = new HashMap<>();
    properties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, String.valueOf(REQUEST_TIMEOUT));
    final Client client = _clientProvider.createClient(FilterChains.empty(), properties);

    final RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(DISRUPT_CONTEXT_KEY, DisruptContexts.error(REQUEST_LATENCY));

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean success = new AtomicBoolean(false);
    client.restRequest(new RestRequestBuilder(getHttpURI()).build(), requestContext,
        new Callback<RestResponse>(){
          @Override
          public void onSuccess(RestResponse result)
          {
            latch.countDown();
          }
          @Override
          public void onError(Throwable e)
          {
            success.set(e instanceof DisruptedException);
            latch.countDown();
          }
        });

    Assert.assertTrue(latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS), "Test execution timeout");
    Assert.assertTrue(success.get(), "Unexpected transport response");
  }


  @Test
  public void testStreamErrorDisrupt() throws Exception
  {
    final Map<String, Object> properties = new HashMap<>();
    properties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, String.valueOf(REQUEST_TIMEOUT));
    final Client client = _clientProvider.createClient(FilterChains.empty(), properties);

    final RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(DISRUPT_CONTEXT_KEY, DisruptContexts.error(REQUEST_LATENCY));

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean success = new AtomicBoolean(false);
    client.streamRequest(new StreamRequestBuilder(getHttpURI()).build(EntityStreams.emptyStream()), requestContext,
        new Callback<StreamResponse>(){
          @Override
          public void onSuccess(StreamResponse result)
          {
            latch.countDown();
          }
          @Override
          public void onError(Throwable e)
          {
            success.set(e instanceof DisruptedException);
            latch.countDown();
          }
        });

    Assert.assertTrue(latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS), "Test execution timeout");
    Assert.assertTrue(success.get(), "Unexpected transport response");
  }
}
