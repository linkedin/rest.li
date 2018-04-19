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

package test.r2.integ.clientserver.providers;

import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.testng.annotations.DataProvider;
import test.r2.integ.clientserver.providers.client.ClientProvider;
import test.r2.integ.clientserver.providers.client.ClientsProviderConfiguration;
import test.r2.integ.clientserver.providers.server.ServerProvider;
import test.r2.integ.clientserver.providers.server.ServerProviderConfiguration;

/**
 * Provider of Client-Server combinations that can be used in tests as dataProvider
 */
public class ClientServerConfiguration
{

  // ############ aggregations ############

  @DataProvider
  public static Object[][] allCombinations()
  {
    return ArrayUtils.addAll(allHttp(), allHttps());
  }

  @DataProvider
  public static Object[][] allHttp()
  {
    return ArrayUtils.addAll(allHttp1(), allHttp2());
  }

  @DataProvider
  public static Object[][] allHttps()
  {
    return ArrayUtils.addAll(allHttps1()
      // disabling https2 tests until ALPN will be integrated in the JDK.
      // Https2 can be re-enabled on local machine for testing purpose after adding ALPN library
      // , allHttps2()
    );
  }

  // ############ client - server matchings ############

  @DataProvider
  public static Object[][] allHttp1()
  {
    return combinations(ClientsProviderConfiguration.allHttp1Client(), ServerProviderConfiguration.allHttp1Server());
  }

  @DataProvider
  public static Object[][] allHttp2()
  {
    return combinations(ClientsProviderConfiguration.allHttp2Client(), ServerProviderConfiguration.allHttp2Server());
  }

  @DataProvider
  public static Object[][] allHttps1()
  {
    return combinations(ClientsProviderConfiguration.allHttps1Client(), ServerProviderConfiguration.allHttps1Server());
  }

  @DataProvider
  public static Object[][] allHttps2()
  {
    return combinations(ClientsProviderConfiguration.allHttps2Client()
      , ServerProviderConfiguration.allHttps2Server()
    );
  }

  // ############ utils ############
  static int PORT = 5921;

  static Object[][] combinations(List<ClientProvider> clientProviders, List<ServerProvider> serverProviders)
  {
    Object[][] combinations = new Object[clientProviders.size() * serverProviders.size()][3];
    int index = 0;
    for (ClientProvider clientProvider : clientProviders)
    {
      for (ServerProvider serverProvider : serverProviders)
      {
        combinations[index][0] = clientProvider;
        combinations[index][1] = serverProvider;
        combinations[index][2] = PORT++;
        index++;
      }
    }
    return combinations;
  }
}
