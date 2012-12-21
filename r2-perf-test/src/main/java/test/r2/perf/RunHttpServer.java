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
