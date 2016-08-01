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

import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import com.linkedin.r2.transport.http.server.HttpJettyServer;
import com.linkedin.r2.transport.http.server.HttpServer;
import com.linkedin.r2.transport.http.server.HttpServerFactory;

import org.testng.annotations.Factory;

import java.io.IOException;
import java.net.URI;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class TestHttpServer extends AbstractHttpServerTest
{
  private final boolean _restOverStream;
  private final HttpJettyServer.ServletType _servletType;
  private final int _port;

  private HttpServer _server;

  @Factory(dataProvider = "configs")
  public TestHttpServer(boolean restOverStream, HttpJettyServer.ServletType servletType, int port)
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

    _server = new HttpServerFactory(_servletType).createServer(_port, dispatcher, _restOverStream);
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
