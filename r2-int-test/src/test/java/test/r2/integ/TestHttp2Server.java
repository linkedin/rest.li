package test.r2.integ;

import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import com.linkedin.r2.transport.http.server.HttpJettyServer;
import com.linkedin.r2.transport.http.server.HttpServer;
import com.linkedin.r2.transport.http.server.HttpServerFactory;
import java.io.IOException;
import java.net.URI;
import org.testng.annotations.Factory;


/**
 * @author Sean Sheng
 * @version $Revision: $
 */

public class TestHttp2Server extends AbstractHttpServerTest
{
  private final boolean _restOverStream;
  private final HttpJettyServer.ServletType _servletType;
  private final int _port;

  private HttpServer _server;

  @Factory(dataProvider = "configs")
  public TestHttp2Server(boolean restOverStream, HttpJettyServer.ServletType servletType, int port)
  {
    super();
    _restOverStream = restOverStream;
    _servletType = servletType;
    _port = port;
  }

  @Override
  protected void doSetup() throws IOException
  {
    final TransportDispatcher dispatcher = new TransportDispatcherBuilder(_restOverStream)
        .addRestHandler(URI.create("/error"), new ErrorHandler())
        .addRestHandler(URI.create("/headerEcho"), new HeaderEchoHandler())
        .addRestHandler(URI.create("/foobar"), new FoobarHandler(_scheduler))
        .build();

    _server = new HttpServerFactory(_servletType).createH2cServer(_port, dispatcher, _restOverStream);
    _server.start();
  }

  @Override
  protected void doTearDown() throws IOException
  {
    if (_server != null) {
      _server.stop();
    }
  }

  @Override
  protected int getPort()
  {
    return _port;
  }
}
