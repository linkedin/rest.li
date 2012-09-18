package test.r2.perf;

import com.linkedin.r2.transport.common.Server;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import com.linkedin.r2.transport.http.server.HttpServerFactory;
import com.linkedin.r2.sample.echo.rest.RestEchoServer;
import com.linkedin.r2.sample.echo.EchoServiceImpl;

import java.io.IOException;
import java.net.URI;


public class RunHttpServer implements TestConstants
{
  public static void main(String[] args) throws IOException
  {
    final int port = Integer.parseInt(System.getProperty(SERVER_PORT_PROP_NAME, DEFAULT_PORT));
    final URI relativeUri = MiscUtil.getUri(DEFAULT_RELATIVE_URI);
    final TransportDispatcher dispatcher = new TransportDispatcherBuilder()
    .addRestHandler(relativeUri, new RestEchoServer(new EchoServiceImpl()))
    .build();

    final Server server = new HttpServerFactory().createServer(port, dispatcher);
    //final Server server = new HttpServerFactory().createServer(port, createDispatcher(relativeUri));
    server.start();
  }

  private static TransportDispatcher createDispatcher(URI uri)
  {
	return new TransportDispatcherBuilder().addRestHandler(uri, new RestEchoServer(new EchoServiceImpl())).build();
  }
}
