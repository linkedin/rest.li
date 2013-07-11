/*
   Copyright (c) 2012 LinkedIn Corp.

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

/**
 * $Id: $
 */

package test.r2.integ;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.data.ByteString;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.client.TestServer;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class TestHttpClient
{
  private HttpClientFactory _clientFactory;
  private TestServer _testServer;

  @BeforeSuite
  private void init() throws Exception
  {
    _testServer = new TestServer();
    _clientFactory = new HttpClientFactory();
  }

  @AfterSuite
  private void cleanup() throws Exception
  {
    final FutureCallback<None> callback = new FutureCallback<None>();
    _clientFactory.shutdown(callback);
    callback.get();

    _testServer.shutdown();
  }

  @Test
  public void testClient() throws Exception
  {
    final TransportClient transportClient = _clientFactory.getClient(new HashMap<String, String>());
    final Client client = new TransportClientAdapter(transportClient);

    RestRequestBuilder rb = new RestRequestBuilder(_testServer.getRequestURI());
    rb.setMethod("GET");
    RestRequest request = rb.build();
    Future<RestResponse> f = client.restRequest(request);

    // This will block
    RestResponse response = f.get();
    final ByteString entity = response.getEntity();
    if (entity != null) {
      System.out.println(entity.asString("UTF-8"));
    } else {
      System.out.println("NOTHING!");
    }

    assertEquals(response.getStatus(), 200);

    final FutureCallback<None> callback = new FutureCallback<None>();
    client.shutdown(callback);
    callback.get();
  }

  @Test
  public void testRequestContextReuse() throws Exception
  {
    final Integer REQUEST_TIMEOUT = 1000;
    final TransportClient transportClient =
        _clientFactory.getClient(Collections.singletonMap(HttpClientFactory.HTTP_REQUEST_TIMEOUT,
                                                          Integer.toString(REQUEST_TIMEOUT)));
    final Client client = new TransportClientAdapter(transportClient);

    RestRequestBuilder rb = new RestRequestBuilder(_testServer.getRequestURI());
    rb.setMethod("GET");
    RestRequest request = rb.build();

    final RequestContext context = new RequestContext();
    Future<RestResponse> f = client.restRequest(request, context);
    Future<RestResponse> f2 = client.restRequest(request, context);

    // This will block
    RestResponse response = f.get(REQUEST_TIMEOUT, TimeUnit.MILLISECONDS);
    assertEquals(response.getStatus(), 200);

    response = f2.get(REQUEST_TIMEOUT, TimeUnit.MILLISECONDS);
    assertEquals(response.getStatus(), 200);

    final Integer iterations = 5;
    final CountDownLatch latch = new CountDownLatch(iterations);
    //Test that sending multiple requests with the same request context works correctly, without
    //modifying the original request context.
    for (int i=0; i<iterations; ++i)
    {
      client.restRequest(request, context, new Callback<RestResponse>()
      {
        @Override
        public void onError(Throwable e)
        {
          Assert.fail("Expected success, received: " + e);
          latch.countDown();
        }

        @Override
        public void onSuccess(RestResponse result)
        {
          Assert.assertEquals(result.getStatus(), 200);
          latch.countDown();
        }
      });
    }
    latch.await(REQUEST_TIMEOUT, TimeUnit.MILLISECONDS);

    Assert.assertTrue(context.getLocalAttrs().isEmpty());

    final FutureCallback<None> callback = new FutureCallback<None>();
    client.shutdown(callback);
    callback.get();
  }

  // Disabled this test for now because due to VMWare/Solaris x86 bugs, ScheduledExecutor
  // does not work correctly on the hudson builds.  Reenable it when we move our Hudson jobs
  // to a correctly functioning operating system.
  @Test(enabled=false)
  public void testFailBackoff() throws Exception
  {
    final int WARM_UP = 10;
    final int N = 5;
    final int REQUEST_TIMEOUT = 1000;

    // Specify the get timeout; we know the max rate will be half the get timeout
    final TransportClient transportClient =
        new HttpClientFactory().getClient(Collections.singletonMap(HttpClientFactory.HTTP_REQUEST_TIMEOUT,
                                                                   Integer.toString(REQUEST_TIMEOUT)));
    final Client client = new TransportClientAdapter(transportClient);

    final ServerSocket ss = new ServerSocket();
    ss.bind(null);
    final CountDownLatch latch = new CountDownLatch(N + WARM_UP);
    Thread t = new Thread(new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          for (;;)
          {
          Socket s = ss.accept();
          s.close();
          latch.countDown();
          System.err.println("!!! Got a connect, " + latch.getCount() + " to go!");
          }
        }
        catch (IOException e)
        {
          e.printStackTrace();
        }
      }
    });
    t.start();


    RestRequest r = new RestRequestBuilder(URI.create("http://localhost:" + ss.getLocalPort() + "/")).setMethod("GET").build();

    // First ensure a bunch fail to get the rate limiting going
    List<FutureCallback<RestResponse>> callbacks = dispatchRequests(client, r, WARM_UP);
    for (FutureCallback<RestResponse> callback : callbacks)
    {
      try
      {
        callback.get();
      }
      catch (Exception e)
      {
        // ignore
      }
    }

    // Now we should be rate limited


    long start = System.currentTimeMillis();
    System.err.println("Starting at " + start);
    dispatchRequests(client, r, N);
    long lowTolerance = N * REQUEST_TIMEOUT / 2 * 4 / 5;
    long highTolerance = N * REQUEST_TIMEOUT / 2 * 5 / 4;
    Assert.assertTrue(latch.await(highTolerance, TimeUnit.MILLISECONDS), "Should have finished within " + highTolerance + "ms");
    long elapsed = System.currentTimeMillis() - start;
    Assert.assertTrue(elapsed > lowTolerance, "Should have finished after " + lowTolerance + "ms (took " + elapsed +")");
  }

  private List<FutureCallback<RestResponse>> dispatchRequests(Client client, RestRequest r, int n)
  {
    List<FutureCallback<RestResponse>> callbacks = new ArrayList<FutureCallback<RestResponse>>(n);
    for (int i = 0; i < n; i++)
    {
      FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();
      callbacks.add(callback);
      client.restRequest(r, callback);
    }
    return callbacks;
  }

  @Test
  public void testSimpleURI() throws Exception
  {
    final TransportClient transportClient = _clientFactory.getClient(new HashMap<String, String>());
    final Client client = new TransportClientAdapter(transportClient);

    // Note no trailing slash; the point of the test is to ensure this URI will
    // send a Request-URI of "/".
    URI uri = URI.create("http://localhost:" + _testServer.getPort());
    RestRequestBuilder rb = new RestRequestBuilder(uri);
    rb.setMethod("GET");
    RestRequest request = rb.build();
    Future<RestResponse> f = client.restRequest(request);

    // This will block
    RestResponse response = f.get();

    assertEquals(response.getStatus(), 200);

    String requestString = _testServer.getLastRequest();
    assertTrue(requestString.startsWith("GET / HTTP"), "Request '" + requestString +
            "' should have started with 'GET / HTTP'");

    final FutureCallback<None> callback = new FutureCallback<None>();
    client.shutdown(callback);
    callback.get();

  }
}
