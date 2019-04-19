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

package test.r2.integ.clientserver.providers.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provider of possible Client configurations
 * @author Francesco Capponi (fcapponi@linkedin.com)
 * @author Nizar Mankulangara
 */
public class ClientsProviderConfiguration
{

  public static List<ClientProvider> allHttp1Client()
  {
    List<ClientProvider> result = new ArrayList<>();
    result.addAll(allHttp1StreamClient());
    result.addAll(allHttp1RestClient());
    return result;
  }

  public static List<ClientProvider> allHttp1StreamClient()
  {
    return Arrays.asList(
        new Http1ClientProvider(true),
        new Http1ClientProvider(true, true)
    );
  }

  public static List<ClientProvider> allHttp1RestClient()
  {
    return Arrays.asList(
        new Http1ClientProvider(false),
        new Http1ClientProvider(false, true)
    );
  }


  public static List<ClientProvider> allHttp2Client()
  {
    return Arrays.asList(
      new Http2ClientProvider(true),
      new Http2ClientProvider(false),
      new Http2ClientProvider(true, true),
      new Http2ClientProvider(false, true)
    );
  }

  public static List<ClientProvider> allHttp2StreamClient()
  {
    return Arrays.asList(
        new Http2ClientProvider(true),
        new Http2ClientProvider(true, true)
    );
  }

  public static List<ClientProvider> allHttp2RestClient()
  {
    return Arrays.asList(
        new Http2ClientProvider(false),
        new Http2ClientProvider(false, true)
    );
  }

  public static List<ClientProvider> allHttps1Client()
  {
    List<ClientProvider> result = new ArrayList<>();
    result.addAll(allHttps1StreamClient());
    result.addAll(allHttps1RestClient());
    return result;
  }

  public static List<ClientProvider> allHttps1StreamClient()
  {
    return Arrays.asList(
        new Https1ClientProvider(true),
        new Https1ClientProvider(true, true)
    );
  }

  public static List<ClientProvider> allHttps1RestClient()
  {
    return Arrays.asList(
        new Https1ClientProvider(false),
        new Https1ClientProvider(false, true)
    );
  }

  public static List<ClientProvider> allHttps2Client()
  {
    return Arrays.asList(
      new Https2ClientProvider(true),
      /*new Https2ClientProvider(false), currently not supported on H2 protocol*/
      new Https2ClientProvider(true, true),
      new Https2ClientProvider(false, true)
    );
  }

  public static List<ClientProvider> allHttps2StreamClient()
  {
    return Arrays.asList(
        new Https2ClientProvider(true),
        new Https2ClientProvider(true, true)
    );
  }

  public static List<ClientProvider> allHttps2RestClient()
  {
    return Arrays.asList(
        /*new Https2ClientProvider(false), currently not supported on H2 protocol*/
        new Https2ClientProvider(false, true)
    );
  }
}
