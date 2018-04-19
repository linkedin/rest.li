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

import java.util.Arrays;
import java.util.List;

/**
 * Provider of possible Server configurations
 */
public class ServerProviderConfiguration
{

  public static List<ServerProvider> allHttp1Server()
  {
    return Arrays.asList(
      new Http1JettyServerProvider(true),
      new Http1JettyServerProvider(false),
      new Http1NettyServerProvider()
    );
  }

  public static List<ServerProvider> allHttp2Server()
  {
    return Arrays.asList(
      new Http2JettyServerProvider(true),
      new Http2JettyServerProvider(false)
    );
  }


  public static List<ServerProvider> allHttps1Server()
  {
    return Arrays.asList(
      new Https1JettyServerProvider(true),
      new Https1JettyServerProvider(false),
      new Https1NettyServerProvider()
    );
  }

  public static List<ServerProvider> allHttps2Server()
  {
    return Arrays.asList(
      new Https2JettyServerProvider(true),
      new Https2JettyServerProvider(false)
    );
  }

}
