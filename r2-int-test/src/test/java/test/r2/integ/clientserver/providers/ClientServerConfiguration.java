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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.testng.annotations.DataProvider;
import test.r2.integ.clientserver.providers.client.ClientProvider;
import test.r2.integ.clientserver.providers.client.ClientsProviderConfiguration;
import test.r2.integ.clientserver.providers.server.ServerProvider;
import test.r2.integ.clientserver.providers.server.ServerProviderConfiguration;

/**
 * Provider of Client-Server combinations that can be used in tests as dataProvider
 * @author Francesco Capponi (fcapponi@linkedin.com)
 * @author Nizar Mankulangara
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
  public static Object[][] allMixedCombinations()
  {
    return allMixedCombinations(allCombinations());
  }

  @DataProvider
  public static Object[][] allStreamCombinations()
  {
    return ArrayUtils.addAll(allHttpStream(), allHttpsStream());
  }

  @DataProvider
  public static Object[][] allRestCombinations()
  {
    return ArrayUtils.addAll(allHttpRest(), allHttps1Rest());
  }

  @DataProvider
  public static Object[][] allHttp()
  {
    return ArrayUtils.addAll(allHttp1(), allHttp2());
  }

  @DataProvider
  public static Object[][] allHttpStream()
  {
    return ArrayUtils.addAll(allHttp1Stream(), allHttp2Stream());
  }

  @DataProvider
  public static Object[][] allHttpsStream()
  {
    return ArrayUtils.addAll(allHttps1Stream()
        // To enable HTTP/2 tests with TLS in Java 8, add the JDK version specific alpn-boot jar to the
        // boostrap classpath. Java 9 and above should automatically include the alpn-boot libraries.
        // e.g. JDK 8u121, -Xbootclasspath/p:-Xbootclasspath/p:/Library/Java/Boot/1_8_0_121/alpn-boot-8.1.11.v20170118.jar
        //, allHttps2Stream()
        );
  }

  @DataProvider
  public static Object[][] allHttpRest()
  {
    return ArrayUtils.addAll(allHttp1Rest(), allHttp2Rest());
  }

  @DataProvider
  public static Object[][] allHttpAsync()
  {
    return ArrayUtils.addAll(allHttp1Async(), allHttp2Async());
  }


  @DataProvider
  public static Object[][] allHttps()
  {
    return ArrayUtils.addAll(
      allHttps1()
      // To enable HTTP/2 tests with TLS in Java 8, add the JDK version specific alpn-boot jar to the
      // boostrap classpath. Java 9 and above should automatically include the alpn-boot libraries.
      // e.g. JDK 8u121, -Xbootclasspath/p:-Xbootclasspath/p:/Library/Java/Boot/1_8_0_121/alpn-boot-8.1.11.v20170118.jar
      //, allHttps2()
    );
  }


  // ############ client - server matchings ############

  @DataProvider
  public static Object[][] allHttp1()
  {
    return combinations(ClientsProviderConfiguration.allHttp1Client(), ServerProviderConfiguration.allHttp1Server());
  }

  @DataProvider
  public static Object[][] allHttp1Async()
  {
    return combinations(ClientsProviderConfiguration.allHttp1StreamClient(), ServerProviderConfiguration.allHttp1AsyncServer());
  }

  @DataProvider
  public static Object[][] allHttp1Stream()
  {
    return combinations(ClientsProviderConfiguration.allHttp1StreamClient(), ServerProviderConfiguration.allHttp1StreamServer());
  }

  @DataProvider
  public static Object[][] allHttp1Rest()
  {
    return combinations(ClientsProviderConfiguration.allHttp1RestClient(), ServerProviderConfiguration.allHttp1RestServer());
  }

  @DataProvider
  public static Object[][] allHttp2()
  {
    return combinations(ClientsProviderConfiguration.allHttp2Client(), ServerProviderConfiguration.allHttp2Server());
  }

  @DataProvider
  public static Object[][] allHttp2Stream()
  {
    return combinations(ClientsProviderConfiguration.allHttp2StreamClient(), ServerProviderConfiguration.allHttp2StreamServer());
  }

  @DataProvider
  public static Object[][] allHttp2Rest()
  {
    return combinations(ClientsProviderConfiguration.allHttp2RestClient(), ServerProviderConfiguration.allHttp2RestServer());
  }


  @DataProvider
  public static Object[][] allHttp2Async()
  {
    return combinations(ClientsProviderConfiguration.allHttp2StreamClient(), ServerProviderConfiguration.allHttp2AsyncServer());
  }


  @DataProvider
  public static Object[][] allHttps1()
  {
    return combinations(ClientsProviderConfiguration.allHttps1Client(), ServerProviderConfiguration.allHttps1Server());
  }

  @DataProvider
  public static Object[][] allHttps1Stream()
  {
    return combinations(ClientsProviderConfiguration.allHttps1StreamClient(), ServerProviderConfiguration.allHttps1StreamServer());
  }

  @DataProvider
  public static Object[][] allHttps1Rest()
  {
    return combinations(ClientsProviderConfiguration.allHttps1RestClient(), ServerProviderConfiguration.allHttps1RestServer());
  }

  @DataProvider
  public static Object[][] allHttps2()
  {
    return combinations(ClientsProviderConfiguration.allHttps2Client(), ServerProviderConfiguration.allHttps2Server());
  }

  @DataProvider
  public static Object[][] allHttps2Stream()
  {
    return combinations(ClientsProviderConfiguration.allHttps2StreamClient(), ServerProviderConfiguration.allHttps2StreamServer());
  }

  @DataProvider
  public static Object[][] allHttps2Async()
  {
    return combinations(ClientsProviderConfiguration.allHttps2StreamClient(), ServerProviderConfiguration.allHttps2AsyncServer());
  }

  @DataProvider
  public static Object[][] allHttps2Rest()
  {
    return combinations(ClientsProviderConfiguration.allHttps2RestClient(), ServerProviderConfiguration.allHttps2RestServer());
  }


  // ############ utils ############
  static int PORT = 15001;

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

  static Object[][] allMixedCombinations(Object[][] allCombination)
  {
    Set<ClientProvider> clientProviders = new HashSet<>();
    Set<ServerProvider> serverProviders = new HashSet<>();

    for (Object[] objects : allCombination)
    {
      clientProviders.add((ClientProvider) objects[0]);
      serverProviders.add((ServerProvider) objects[1]);
    }

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
