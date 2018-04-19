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

import java.util.Arrays;
import java.util.List;

/**
 * Provider of possible Client configurations
 */
public class ClientsProviderConfiguration
{

  public static List<ClientProvider> allHttp1Client()
  {
    return Arrays.asList(
      new Http1ClientProvider(true),
      new Http1ClientProvider(false)
    );
  }

  public static List<ClientProvider> allHttp2Client()
  {
    return Arrays.asList(
      new Http2ClientProvider(true),
      new Http2ClientProvider(false));
  }

  public static List<ClientProvider> allHttps1Client()
  {
    return Arrays.asList(
      new Https1ClientProvider(true),
      new Https1ClientProvider(false)
    );
  }

  public static List<ClientProvider> allHttps2Client()
  {
    return Arrays.asList(
      // Disabling this path which will be soon deprecated
//       new Https2ClientProvider(true)
//       , new Https2ClientProvider(false)
    );
  }

}
