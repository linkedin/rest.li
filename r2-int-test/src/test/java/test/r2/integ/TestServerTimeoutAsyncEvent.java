package test.r2.integ;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.data.ByteString;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.message.stream.StreamFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.message.stream.StreamRequest;
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
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.r2.transport.common.bridge.server.TransportCallbackAdapter;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.server.HttpJettyServer;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author Zhenkai Zhu
 */
public class TestServerTimeoutAsyncEvent
{
  private static final int PORT = 10001;
  private static final URI TIMEOUT_BEFORE_SENDING_RESPONSE_SERVER_URI = URI.create("/timeout-before-sending-response");
  private static final URI TIMEOUT_AFTER_SENDING_RESPONSE_SERVER_URI = URI.create("/timeout-after-sending-response");
  private static final URI THROW_BUT_SHOULD_NOT_TIMEOUT_URI = URI.create("/throw-but-should-not-timeout");
  private static final URI BUGGY_FILTER_URI = URI.create("/buggy-filter");
  private static final URI STREAM_EXCEPTION_FILTER_URI = URI.create("/stream-exception-filter");
  private static final int ASYNC_EVENT_TIMEOUT = 2000;
  private static final int RESPONSE_SIZE_WRITTEN_SO_FAR = 50 * 1024;
  private HttpClientFactory _clientFactory;
  private Client _client;
  private HttpServer _server;
  private ExecutorService _asyncExecutor;

  @BeforeClass
  public void setup() throws IOException
  {
    _clientFactory = new HttpClientFactory();
    Map<String, Object> clientProperties = new HashMap<String, Object>();
    clientProperties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, String.valueOf(ASYNC_EVENT_TIMEOUT * 20));
    clientProperties.put(HttpClientFactory.HTTP_POOL_MIN_SIZE, "1");
    clientProperties.put(HttpClientFactory.HTTP_POOL_SIZE, "1");
    _client = new TransportClientAdapter(_clientFactory.getClient(clientProperties), true);
    final Map<URI, StreamRequestHandler> handlers = new HashMap<URI, StreamRequestHandler>();
    handlers.put(TIMEOUT_BEFORE_SENDING_RESPONSE_SERVER_URI, new TimeoutBeforeRespondingRequestHandler());
    handlers.put(TIMEOUT_AFTER_SENDING_RESPONSE_SERVER_URI, new TimeoutAfterRespondingRequestHandler());
    handlers.put(THROW_BUT_SHOULD_NOT_TIMEOUT_URI, new ThrowHandler());
    handlers.put(BUGGY_FILTER_URI, new NormalHandler());
    TransportDispatcher transportDispatcher = new TransportDispatcher()
    {
      @Override
      public void handleRestRequest(RestRequest req, Map<String, String> wireAttrs,
                                    RequestContext requestContext, TransportCallback<RestResponse> callback)
      {
        throw new UnsupportedOperationException("This dispatcher only supports stream");
      }

      @Override
      public void handleStreamRequest(StreamRequest req, Map<String, String> wireAttrs, RequestContext requestContext, TransportCallback<StreamResponse> callback)
      {
        StreamRequestHandler handler = handlers.get(req.getURI());
        if (handler != null)
        {
          handler.handleRequest(req, requestContext, new TransportCallbackAdapter<StreamResponse>(callback));
        }
        else
        {
          req.getEntityStream().setReader(new DrainReader());
          callback.onResponse(TransportResponseImpl.<StreamResponse>error(new IllegalStateException("Handler not found for URI " + req.getURI())));
        }
      }
    };
    FilterChain filterChain = FilterChains.createStreamChain(new BuggyFilter());
    _server = new HttpServerFactory(filterChain, HttpJettyServer.ServletType.ASYNC_EVENT).createServer(PORT, transportDispatcher, ASYNC_EVENT_TIMEOUT, true);
    _server.start();
    _asyncExecutor = Executors.newSingleThreadExecutor();
  }

  @Test
  public void testServerTimeoutAfterResponding() throws Exception
  {
    Future<RestResponse> futureResponse =
        _client.restRequest(new RestRequestBuilder(Bootstrap.createHttpURI(PORT, TIMEOUT_AFTER_SENDING_RESPONSE_SERVER_URI)).build());

    // server should timeout so get should succeed
    RestResponse response = futureResponse.get(ASYNC_EVENT_TIMEOUT * 2, TimeUnit.MILLISECONDS);
    Assert.assertEquals(response.getStatus(), RestStatus.OK);
    Assert.assertEquals(response.getEntity().length(), RESPONSE_SIZE_WRITTEN_SO_FAR);
  }

  @Test
  public void testServerTimeoutBeforeResponding() throws Exception
  {
    Future<RestResponse> futureResponse =
        _client.restRequest(new RestRequestBuilder(Bootstrap.createHttpURI(PORT, TIMEOUT_BEFORE_SENDING_RESPONSE_SERVER_URI)).build());

    try
    {
      futureResponse.get(ASYNC_EVENT_TIMEOUT * 2, TimeUnit.MILLISECONDS);
      Assert.fail("Should have thrown exception");
    }
    catch (ExecutionException ex)
    {
      Throwable cause = ex.getCause();
      Assert.assertNotNull(cause);
      Assert.assertTrue(cause instanceof RestException);
      RestException restException = (RestException) cause;
      Assert.assertEquals(restException.getResponse().getStatus(), RestStatus.INTERNAL_SERVER_ERROR);
      Assert.assertEquals(restException.getResponse().getEntity().asString("UTF8"), "Server timeout");
    }
  }

  @Test
  public void testServerThrowButShouldNotTimeout() throws Exception
  {
    RestRequest request = new RestRequestBuilder(Bootstrap.createHttpURI(PORT, THROW_BUT_SHOULD_NOT_TIMEOUT_URI))
        .setEntity(new byte[10240]).build();

    _client.restRequest(request);
    Future<RestResponse> futureResponse = _client.restRequest(request);
    // if server times out, our second request would fail with TimeoutException because it's blocked by first one
    try
    {
      futureResponse.get(ASYNC_EVENT_TIMEOUT / 2, TimeUnit.MILLISECONDS);
      Assert.fail("Should fail with ExecutionException");
    }
    catch (ExecutionException ex)
    {
      Assert.assertTrue(ex.getCause() instanceof RestException);
      RestException restException = (RestException)ex.getCause();
      Assert.assertTrue(restException.getResponse().getEntity().asString("UTF8").contains("Server throw for test."));
    }
  }

  @Test
  public void testFilterThrowButShouldNotTimeout() throws Exception
  {
    RestRequest request = new RestRequestBuilder(Bootstrap.createHttpURI(PORT, BUGGY_FILTER_URI))
        .setEntity(new byte[10240]).build();

    _client.restRequest(request);
    Future<RestResponse> futureResponse = _client.restRequest(request);
    // if server times out, our second request would fail with TimeoutException because it's blocked by first one
    try
    {
      futureResponse.get(ASYNC_EVENT_TIMEOUT / 2, TimeUnit.MILLISECONDS);
      Assert.fail("Should fail with ExecutionException");
    }
    catch (ExecutionException ex)
    {
      Assert.assertTrue(ex.getCause() instanceof RestException);
      RestException restException = (RestException)ex.getCause();
      Assert.assertTrue(restException.getResponse().getEntity().asString("UTF8").contains("Buggy filter throws."));
    }
  }

  @Test
  public void testFilterNotCancelButShouldNotTimeout() throws Exception
  {
    RestRequest request = new RestRequestBuilder(Bootstrap.createHttpURI(PORT, STREAM_EXCEPTION_FILTER_URI))
        .setEntity(new byte[10240]).build();

    _client.restRequest(request);
    Future<RestResponse> futureResponse = _client.restRequest(request);
    // if server times out, our second request would fail with TimeoutException because it's blocked by first one
    try
    {
      futureResponse.get(ASYNC_EVENT_TIMEOUT / 2, TimeUnit.MILLISECONDS);
      Assert.fail("Should fail with ExecutionException");
    }
    catch (ExecutionException ex)
    {
      Assert.assertTrue(ex.getCause() instanceof RestException);
      RestException restException = (RestException)ex.getCause();
      Assert.assertTrue(restException.getResponse().getEntity().asString("UTF8").contains("StreamException in filter."));
    }
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
    _asyncExecutor.shutdown();
  }

  private class ThrowHandler implements StreamRequestHandler
  {
    @Override
    public void handleRequest(final StreamRequest request, RequestContext requestContext, Callback<StreamResponse> callback)
    {
      throw new RuntimeException("Server throw for test.");
    }
  }

  private class NormalHandler implements StreamRequestHandler
  {
    @Override
    public void handleRequest(final StreamRequest request, RequestContext requestContext, Callback<StreamResponse> callback)
    {
      request.getEntityStream().setReader(new DrainReader());
      callback.onSuccess(new StreamResponseBuilder().build(EntityStreams.emptyStream()));
    }
  }

  private class TimeoutBeforeRespondingRequestHandler implements StreamRequestHandler
  {
    @Override
    public void handleRequest(final StreamRequest request, RequestContext requestContext, Callback<StreamResponse> callback)
    {
      _asyncExecutor.execute(new Runnable()
      {
        @Override
        public void run()
        {
          request.getEntityStream().setReader(new Reader()
          {
            @Override
            public void onInit(ReadHandle rh)
            {

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
      });
    }
  }

  private class TimeoutAfterRespondingRequestHandler implements StreamRequestHandler
  {
    @Override
    public void handleRequest(final StreamRequest request, RequestContext requestContext, final Callback<StreamResponse> callback)
    {
      _asyncExecutor.execute(new Runnable()
      {
        @Override
        public void run()
        {
          request.getEntityStream().setReader(new DrainReader());
          Writer noFinishWriter = new Writer()
          {
            private WriteHandle _wh;
            boolean _written = false;

            @Override
            public void onInit(WriteHandle wh)
            {
              _wh = wh;
            }

            @Override
            public void onWritePossible()
            {
              if (!_written)
              {
                _wh.write(ByteString.copy(new byte[RESPONSE_SIZE_WRITTEN_SO_FAR]));
              }
            }

            @Override
            public void onAbort(Throwable e)
            {

            }
          };

          callback.onSuccess(new StreamResponseBuilder().build(EntityStreams.newEntityStream(noFinishWriter)));
        }
      });

    }
  }

  private class BuggyFilter implements StreamFilter
  {
    @Override
    public void onStreamRequest(StreamRequest req,
                          RequestContext requestContext,
                          Map<String, String> wireAttrs,
                          NextFilter<StreamRequest, StreamResponse> nextFilter)
    {
      if (req.getURI().equals(BUGGY_FILTER_URI))
      {
        throw new RuntimeException("Buggy filter throws.");
      }

      if (req.getURI().equals(STREAM_EXCEPTION_FILTER_URI))
      {
        nextFilter.onError(Messages.toStreamException(RestException.forError(500, "StreamException in filter.")), requestContext, wireAttrs);
        return;
      }

      nextFilter.onRequest(req, requestContext, wireAttrs);
    }
  }
}
