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

package test.r2.integ.clientserver.providers;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.message.stream.StreamFilterAdapters;
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.sample.echo.rest.RestEchoClient;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.Server;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import test.r2.integ.clientserver.providers.client.ClientProvider;
import test.r2.integ.clientserver.providers.server.ServerProvider;
import test.r2.integ.helper.CaptureWireAttributesFilter;
import test.r2.integ.helper.LogEntityLengthFilter;
import test.r2.integ.helper.SendWireAttributeFilter;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */
public abstract class AbstractEchoServiceTest
{
  protected static final Logger LOG = LoggerFactory.getLogger(AbstractEchoServiceTest.class);

  private final String _toServerKey = "to-server";
  private final String _toServerValue = "this value goes to the server";

  private final String _toClientKey = "to-client";
  private final String _toClientValue = "this value goes to the client";

  protected final static String ECHO_MSG = "This is a simple echo message";
  protected final ClientProvider _clientProvider;
  protected final ServerProvider _serverProvider;
  protected final int _port;

  protected Client _client;
  protected Server _server;

  protected CaptureWireAttributesFilter _serverCaptureFilter;
  protected CaptureWireAttributesFilter _clientCaptureFilter;
  protected LogEntityLengthFilter _serverLengthFilter;
  protected LogEntityLengthFilter _clientLengthFilter;

  public AbstractEchoServiceTest(ClientProvider clientProvider, ServerProvider serverProvider, int port)
  {
    _clientProvider = clientProvider;
    _serverProvider = serverProvider;
    _port = port;
  }

  @BeforeClass
  protected void setUp() throws Exception
  {

    _client = createClient();
    _server = createServer();
    _server.start();
  }

  @AfterClass
  protected void tearDown() throws Exception
  {
    tearDown(_client, _server);
  }

  protected void tearDown(Client client, Server server) throws Exception
  {
    final FutureCallback<None> callback = new FutureCallback<>();
    client.shutdown(callback);

    try
    {
      callback.get();
    }
    finally
    {
      if (server != null)
      {
        server.stop();
        server.waitForStop();
      }
    }
  }

  protected FilterChain getClientFilters()
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

  protected FilterChain getServerFilters()
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
    System.out.println("Testing " + _clientProvider + " with " + _serverProvider + " on port " + _port);
    return new RestEchoClient(Bootstrap.createURI(_port, relativeUri, _serverProvider.isSsl()), client);
  }

  protected Client createClient() throws Exception
  {
    System.out.println("Testing " + _clientProvider + " with " + _serverProvider + " on port " + _port);
    return _clientProvider.createClient(getClientFilters());
  }

  protected Server createServer() throws Exception
  {
    return _serverProvider.createServer(getServerFilters(), _port);
  }

}
