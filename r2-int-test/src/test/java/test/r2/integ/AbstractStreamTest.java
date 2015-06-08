package test.r2.integ;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.server.HttpServer;
import com.linkedin.r2.transport.http.server.HttpServerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


/**
 * @author Zhenkai Zhu
 */
public abstract class AbstractStreamTest
{
  protected static final int PORT = 8099;
  protected static final long LARGE_BYTES_NUM = 1024 * 1024 * 1024;
  protected static final long SMALL_BYTES_NUM = 1024 * 1024 * 32;
  protected static final long TINY_BYTES_NUM = 1024 * 64;
  protected static final byte BYTE = 100;
  protected static final long INTERVAL = 20;
  protected HttpServer _server;
  protected TransportClientFactory _clientFactory;
  protected Client _client;
  protected ScheduledExecutorService _scheduler;

  @BeforeClass
  public void setup() throws IOException
  {
    _scheduler = Executors.newSingleThreadScheduledExecutor();
    _clientFactory = getClientFactory();
    _client = new TransportClientAdapter(_clientFactory.getClient(getClientProperties()), true);
    _server = getServerFactory().createServer(PORT, getTransportDispatcher(), true);
    _server.start();
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

    _scheduler.shutdown();
    if (_server != null) {
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

}
