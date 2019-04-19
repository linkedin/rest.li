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

package test.r2.integ.clientserver.providers;

import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.message.stream.StreamFilterAdapters;
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.sample.echo.EchoServiceImpl;
import com.linkedin.r2.sample.echo.OnExceptionEchoService;
import com.linkedin.r2.sample.echo.ThrowingEchoService;
import com.linkedin.r2.sample.echo.rest.RestEchoClient;
import com.linkedin.r2.sample.echo.rest.RestEchoServer;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import java.net.URI;
import test.r2.integ.clientserver.providers.client.ClientProvider;
import test.r2.integ.clientserver.providers.server.ServerProvider;
import test.r2.integ.helper.CaptureWireAttributesFilter;
import test.r2.integ.helper.LogEntityLengthFilter;
import test.r2.integ.helper.SendWireAttributeFilter;

/**
 * @author Steven Ihde
 * @author Nizar Mankulangara
 * @version $Revision: $
 */
public abstract class AbstractEchoServiceTest extends AbstractServiceTest
{
  protected CaptureWireAttributesFilter _serverCaptureFilter;
  protected CaptureWireAttributesFilter _clientCaptureFilter;
  protected LogEntityLengthFilter _serverLengthFilter;
  protected LogEntityLengthFilter _clientLengthFilter;

  private static final URI ECHO_URI = URI.create("/echo");
  private static final URI ON_EXCEPTION_ECHO_URI = URI.create("/on-exception-echo");
  private static final URI THROWING_ECHO_URI = URI.create("/throwing-echo");

  protected final String _toServerKey = "to-server";
  protected final String _toServerValue = "this value goes to the server";

  protected final String _toClientKey = "to-client";
  protected final String _toClientValue = "this value goes to the client";

  public AbstractEchoServiceTest(ClientProvider clientProvider, ServerProvider serverProvider, int port)
  {
    super(clientProvider, serverProvider, port);
  }

  @Override
  protected FilterChain getClientFilterChain()
  {
    _clientCaptureFilter = new CaptureWireAttributesFilter();
    _clientLengthFilter = new LogEntityLengthFilter();
    final SendWireAttributeFilter clientWireFilter = new SendWireAttributeFilter(_toServerKey, _toServerValue, true);

    return FilterChains.empty()
      .addFirstRest(_clientCaptureFilter)
      .addLastRest(_clientLengthFilter)
      .addLastRest(clientWireFilter)
      .addFirst(_clientCaptureFilter)
      // test adapted rest filter works fine in rest over stream setting
      .addLast(StreamFilterAdapters.adaptRestFilter(_clientLengthFilter))
      .addLast(clientWireFilter);
  }

  @Override
  protected FilterChain getServerFilterChain()
  {
    _serverCaptureFilter = new CaptureWireAttributesFilter();
    _serverLengthFilter = new LogEntityLengthFilter();
    final SendWireAttributeFilter serverWireFilter = new SendWireAttributeFilter(_toClientKey, _toClientValue, false);

    return FilterChains.empty()
      .addFirstRest(_serverCaptureFilter)
      .addLastRest(_serverLengthFilter)
      .addLastRest(serverWireFilter)
      .addFirst(_serverCaptureFilter)
      // test adapted rest filter works fine in rest over stream setting
      .addLast(StreamFilterAdapters.adaptRestFilter(_serverLengthFilter))
      .addLast(serverWireFilter);
  }

  public RestEchoClient getEchoClient(Client client, URI relativeUri)
  {
    return new RestEchoClient(Bootstrap.createURI(_port, relativeUri, _serverProvider.isSsl()), client);
  }

  @Override
  protected TransportDispatcher getTransportDispatcher()
  {
    return new TransportDispatcherBuilder()
        .addRestHandler(ECHO_URI, new RestEchoServer(new EchoServiceImpl()))
        .addRestHandler(ON_EXCEPTION_ECHO_URI, new RestEchoServer(new OnExceptionEchoService()))
        .addRestHandler(THROWING_ECHO_URI, new RestEchoServer(new ThrowingEchoService()))
        .build();
  }
}
