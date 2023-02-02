package com.linkedin.r2.transport.http.server;

import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestAsyncLockup {
  private static final int PORT = 9000;
  private static final String CONTEXT = "/context";
  private static final int THREAD_POOL_SIZE = 20; // must be greater than 8 (minimum supported by QueuedThreadPool)
  private static final String URL = "http://localhost:" + PORT + CONTEXT;
  private static final int TIMEOUT_MILLIS = 1000;

  /*
   * Test a deadlock scenario where all Jetty worker threads are blocked in the SyncIOHandler event loop.
   *
   * 1) Enable Async and Streaming.
   * 2) Occupy all jetty worker threads with requests.
   * 3) Each request returns a response without consuming the request body.
   * 4) All threads are permanently stuck.
   *
   * Even in Async mode, the SyncIOHandler will block the Jetty worker thread until the request body has been fully read
   * by the application. If the application does not read the request body, then the SyncIOHandler will unblock when the
   * final byte of the response has been written. However, a Jetty worker thread is needed to write the response. If all
   * worker threads are stuck in the same situation, then there will be no worker threads available to write a response,
   * and thus no way for any of them to be unblocked.
   *
   * This bug was fixed by using the SyncIOHandler to write the response, eliminating the need to acquire a new Jetty
   * worker thread. This test exists to prevent regression.
   */
  @Test()
  public void testAsyncLockup() throws Exception {
    BarrierDispatcher dispatcher = new BarrierDispatcher();
    HttpJettyServer httpJettyServer = new HttpJettyServer(PORT, CONTEXT, THREAD_POOL_SIZE,
        HttpDispatcherFactory.create(dispatcher), HttpJettyServer.ServletType.ASYNC_EVENT, Integer.MAX_VALUE, true);

    httpJettyServer.start();
    int workers = numWorkerThreads(httpJettyServer.getInternalServer());
    dispatcher.setBarrier(workers);

    List<CompletableFuture<Integer>> responseFutures = new ArrayList<>();
    try (CloseableHttpClient client = HttpClients.custom()
        .setDefaultRequestConfig(RequestConfig.custom().setConnectionRequestTimeout(TIMEOUT_MILLIS).build())
        .setMaxConnTotal(THREAD_POOL_SIZE)
        .setMaxConnPerRoute(THREAD_POOL_SIZE)
        .disableAutomaticRetries()
        .build()) {

      for (int i = 0; i < workers; i++) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        responseFutures.add(future);
        new Thread(() -> {
          try {
            CloseableHttpResponse response = client.execute(new HttpGet(URL));
            int status = response.getStatusLine().getStatusCode();
            future.complete(status);
          } catch (Throwable e) {
            future.completeExceptionally(e);
          }
        }).start();
      }

      for (CompletableFuture<Integer> future : responseFutures) {
        Assert.assertEquals(future.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS).intValue(), 200);
      }
    }

    httpJettyServer.stop();
    httpJettyServer.waitForStop();
  }

  // Calculates the number of worker threads by subtracting acceptor and selector threads.
  // Extracted from Server#onStart.
  private int numWorkerThreads(Server server) {
    int selectors = 0;
    int acceptors = 0;

    for (Connector connector : server.getConnectors())
    {
      if (!(connector instanceof AbstractConnector))
        continue;

      AbstractConnector abstractConnector = (AbstractConnector) connector;
      Executor connectorExecutor = connector.getExecutor();

      if (connectorExecutor != server.getThreadPool()) {
        // Do not count the selectors and acceptors from this connector at server level, because connector uses dedicated executor.
        continue;
      }

      acceptors += abstractConnector.getAcceptors();

      if (connector instanceof ServerConnector) {
        selectors += ((ServerConnector)connector).getSelectorManager().getSelectorCount();
      }
    }

    return THREAD_POOL_SIZE - selectors - acceptors;
  }

  static class BarrierDispatcher implements TransportDispatcher {
    private CyclicBarrier _barrier;

    public void setBarrier(int count) {
      _barrier = new CyclicBarrier(count);
    }

    @Override
    public void handleRestRequest(RestRequest req, Map<String, String> wireAttrs, RequestContext requestContext,
        TransportCallback<RestResponse> callback) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void handleStreamRequest(StreamRequest req, Map<String, String> wireAttrs, RequestContext requestContext,
        TransportCallback<StreamResponse> callback) {
      try {
        _barrier.await();
      } catch (InterruptedException | BrokenBarrierException e) {
        throw new RuntimeException(e);
      }
      callback.onResponse(TransportResponseImpl.success(new StreamResponseBuilder().build(EntityStreams.emptyStream())));
    }
  }
}
