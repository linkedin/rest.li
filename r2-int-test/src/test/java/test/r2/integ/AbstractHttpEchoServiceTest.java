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

/* $Id$ */
package test.r2.integ;

import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.Server;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public abstract class AbstractHttpEchoServiceTest extends AbstractEchoServiceTest
{
  private final boolean _clientROS;
  private final boolean _serverROS;
  private final int _port;

  protected AbstractHttpEchoServiceTest(boolean clientROS, boolean serverROS, int port)
  {
    _clientROS = clientROS;
    _serverROS = serverROS;
    _port = port;
  }

  @Override
  protected Client createClient(FilterChain filters)
  {
    return Bootstrap.createHttpClient(filters, _clientROS);
  }

  @Override
  protected Server createServer(FilterChain filters)
  {
    return Bootstrap.createHttpServer(_port, filters, _serverROS);
  }
}
