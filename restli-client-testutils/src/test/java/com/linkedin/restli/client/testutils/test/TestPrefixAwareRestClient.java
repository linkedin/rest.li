/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.restli.client.testutils.test;

import com.linkedin.r2.transport.common.Client;
import com.linkedin.restli.client.testutils.PrefixAwareRestClient;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Sean Sheng
 */
public class TestPrefixAwareRestClient
{
  private static final String URI_PREFIX = "protocol://uri-prefix";

  @Test
  public void testGetPrefix()
  {
    Client underlying = Mockito.mock(Client.class);
    PrefixAwareRestClient client = new PrefixAwareRestClient(underlying, URI_PREFIX);
    Assert.assertEquals(client.getPrefix(), URI_PREFIX);
  }
}
