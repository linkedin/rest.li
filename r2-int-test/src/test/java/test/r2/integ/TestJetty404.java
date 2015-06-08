package test.r2.integ;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.message.stream.StreamException;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.DrainReader;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.server.HttpServer;
import com.linkedin.r2.transport.http.server.HttpServerFactory;
import junit.framework.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Zhenkai Zhu
 */
public class TestJetty404
{
  protected static final int PORT = 8099;
  protected HttpServer _server;
  protected TransportClientFactory _clientFactory;
  protected Client _client;

  @BeforeClass
  public void setup() throws IOException
  {
    _clientFactory = new HttpClientFactory();
    _client = new TransportClientAdapter(_clientFactory.getClient(Collections.<String, String>emptyMap()), true);
    _server = new HttpServerFactory().createServer(PORT, "/correct-path", 50, new TransportDispatcher()
    {
      @Override
      public void handleRestRequest(RestRequest req, Map<String, String> wireAttrs,
                                           RequestContext requestContext, TransportCallback<RestResponse> callback)
      {
        callback.onResponse(TransportResponseImpl.success(new RestResponseBuilder().build()));
      }

      @Override
      public void handleStreamRequest(StreamRequest req, Map<String, String> wireAttrs, RequestContext requestContext, TransportCallback<StreamResponse> callback)
      {
        req.getEntityStream().setReader(new DrainReader());
        callback.onResponse(TransportResponseImpl.success(new StreamResponseBuilder().build(EntityStreams.emptyStream())));
      }
    }, true);
    _server.start();
  }

  // make sure jetty's default behavior will read all the request bytes in case of 404
  @Test
  public void testJetty404() throws Exception
  {
    BytesWriter writer = new BytesWriter(200 * 1024, (byte)100);
    final AtomicReference<Throwable> exRef = new AtomicReference<Throwable>();
    final CountDownLatch latch = new CountDownLatch(1);
    _client.streamRequest(new StreamRequestBuilder(Bootstrap.createHttpURI(PORT, URI.create("/wrong-path")))
        .build(EntityStreams.newEntityStream(writer)), new Callback<StreamResponse>()
    {
      @Override
      public void onError(Throwable e)
      {
        exRef.set(e);
        latch.countDown();
      }

      @Override
      public void onSuccess(StreamResponse result)
      {
        latch.countDown();
      }
    });

    latch.await(500, TimeUnit.MILLISECONDS);
    Assert.assertTrue(writer.isDone());
    Throwable ex = exRef.get();
    Assert.assertTrue(ex instanceof StreamException);
    StreamResponse response = ((StreamException) ex).getResponse();
    Assert.assertEquals(response.getStatus(), RestStatus.NOT_FOUND);
    response.getEntityStream().setReader(new DrainReader());
  }

  @AfterClass
  public void tearDown() throws Exception
  {

    final FutureCallback<None> clientShutdownCallback = new FutureCallback<None>();
    _client.shutdown(clientShutdownCallback);
    clientShutdownCallback.get();

    final FutureCallback<None> factoryShutdownCallback = new FutureCallback<None>();
    _clientFactory.shutdown(factoryShutdownCallback);
    factoryShutdownCallback.get();

    if (_server != null) {
      _server.stop();
      _server.waitForStop();
    }
  }

}
