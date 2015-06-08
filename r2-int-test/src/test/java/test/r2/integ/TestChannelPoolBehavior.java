package test.r2.integ;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.data.ByteString;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.DrainReader;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.ReadHandle;
import com.linkedin.r2.message.stream.entitystream.Reader;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.r2.message.stream.entitystream.Writer;
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.StreamRequestHandler;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.server.HttpServer;
import com.linkedin.r2.transport.http.server.HttpServerFactory;
import junit.framework.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Zhenkai Zhu
 */
public class TestChannelPoolBehavior
{
  private static final int PORT = 8099;
  private static final URI NOT_FOUND_URI = URI.create("/not_found");
  private static final URI NORMAL_URI = URI.create("/normal");
  private static final long WRITER_DELAY = 500;

  private HttpServer _server;
  private TransportClientFactory _clientFactory;
  private Client _client1;
  private Client _client2;
  private ScheduledExecutorService _scheduler;

  @BeforeClass
  public void setup() throws IOException
  {
    _scheduler = Executors.newSingleThreadScheduledExecutor();
    _clientFactory = new HttpClientFactory();
    _client1 = new TransportClientAdapter(_clientFactory.getClient(getClientProperties()), true);
    _client2 = new TransportClientAdapter(_clientFactory.getClient(getClientProperties()), true);
    _server = new HttpServerFactory().createServer(PORT, getTransportDispatcher(), true);
    _server.start();
  }

  @AfterClass
  public void tearDown() throws Exception
  {

    final FutureCallback<None> client1ShutdownCallback = new FutureCallback<None>();
    _client1.shutdown(client1ShutdownCallback);
    client1ShutdownCallback.get();
    final FutureCallback<None> client2ShutdownCallback = new FutureCallback<None>();
    _client2.shutdown(client2ShutdownCallback);
    client2ShutdownCallback.get();

    final FutureCallback<None> factoryShutdownCallback = new FutureCallback<None>();
    _clientFactory.shutdown(factoryShutdownCallback);
    factoryShutdownCallback.get();

    _scheduler.shutdown();
    if (_server != null) {
      _server.stop();
      _server.waitForStop();
    }
  }

  private TransportDispatcher getTransportDispatcher()
  {
    _scheduler = Executors.newSingleThreadScheduledExecutor();
    return new TransportDispatcherBuilder()
        .addStreamHandler(NOT_FOUND_URI, new NotFoundServerHandler())
        .addStreamHandler(NORMAL_URI, new NormalServerHandler())
        .build();
  }

  @Test
  public void testChannelBlocked() throws Exception
  {
    _client1.streamRequest(new StreamRequestBuilder(Bootstrap.createHttpURI(PORT, NOT_FOUND_URI))
        .build(EntityStreams.newEntityStream(new SlowWriter())), new Callback<StreamResponse>()
    {
      @Override
      public void onError(Throwable e)
      {
        throw new RuntimeException(e);
      }

      @Override
      public void onSuccess(StreamResponse result)
      {
        result.getEntityStream().setReader(new DrainReader());
      }
    });

    Future<RestResponse> responseFuture = _client1.restRequest(new RestRequestBuilder(Bootstrap.createHttpURI(PORT, NORMAL_URI)).build());
    try
    {
      responseFuture.get(WRITER_DELAY/2 , TimeUnit.MILLISECONDS);
      Assert.fail();
    }
    catch (TimeoutException ex)
    {
      // expected
    }
  }

  @Test
  public void testChannelReuse() throws Exception
  {
    _client2.streamRequest(new StreamRequestBuilder(Bootstrap.createHttpURI(PORT, NOT_FOUND_URI))
        .build(EntityStreams.newEntityStream(new SlowWriter())), new Callback<StreamResponse>()
    {
      @Override
      public void onError(Throwable e)
      {
        throw new RuntimeException(e);
      }

      @Override
      public void onSuccess(StreamResponse result)
      {
        result.getEntityStream().setReader(new DrainReader());
      }
    });

    Future<RestResponse> responseFuture = _client2.restRequest(new RestRequestBuilder(Bootstrap.createHttpURI(PORT, NORMAL_URI)).build());
    RestResponse response = responseFuture.get(WRITER_DELAY * 2 , TimeUnit.MILLISECONDS);
    Assert.assertEquals(response.getStatus(), RestStatus.OK);

  }

  private Map<String, String> getClientProperties()
  {
    Map<String, String> clientProperties = new HashMap<String, String>();
    clientProperties.put(HttpClientFactory.HTTP_POOL_SIZE, "1");
    clientProperties.put(HttpClientFactory.HTTP_POOL_MIN_SIZE, "1");
    return clientProperties;
  }

  private class NormalServerHandler implements StreamRequestHandler
  {
    @Override
    public void handleRequest(StreamRequest request, RequestContext requestContext, Callback<StreamResponse> callback)
    {
      callback.onSuccess(new StreamResponseBuilder().setStatus(RestStatus.OK).build(EntityStreams.emptyStream()));
      request.getEntityStream().setReader(new DrainReader());
    }
  }

  private class NotFoundServerHandler implements StreamRequestHandler
  {
    @Override
    public void handleRequest(StreamRequest request, RequestContext requestContext, Callback<StreamResponse> callback)
    {
      callback.onSuccess(new StreamResponseBuilder().setStatus(RestStatus.NOT_FOUND).build(EntityStreams.emptyStream()));
      request.getEntityStream().setReader(new Reader()
      {
        @Override
        public void onInit(ReadHandle rh)
        {
          rh.cancel();
        }

        @Override
        public void onDataAvailable(ByteString data)
        {

        }

        @Override
        public void onDone()
        {

        }

        @Override
        public void onError(Throwable e)
        {

        }
      });
    }
  }

  private class SlowWriter implements Writer
  {

    private WriteHandle _wh;
    @Override
    public void onInit(final WriteHandle wh)
    {
      _wh = wh;
    }

    @Override
    public void onWritePossible()
    {
      _scheduler.schedule(new Runnable()
      {
        @Override
        public void run()
        {
          _wh.done();
        }
      }, WRITER_DELAY, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onAbort(Throwable e)
    {

    }
  }
}
