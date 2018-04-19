/*
   Copyright (c) 2018 LinkedIn Corp.

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

package test.r2.integ.clientserver.providers.server;

import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.transport.common.Server;

public class Http2JettyServerProvider implements ServerProvider
{
  private final boolean _serverROS;

  public Http2JettyServerProvider(boolean serverROS)
  {
    _serverROS = serverROS;
  }

  @Override
  public Server createServer(FilterChain filters, int port)
  {
    return Bootstrap.createH2cServer(port, filters, _serverROS);
  }

  @Override
  public String toString()
  {
    return "[" + getClass().getName() + ", stream=" + _serverROS + "]";
  }
}
