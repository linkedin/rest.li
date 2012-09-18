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

package com.linkedin.restli.examples;

import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.FindRequest;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.examples.custom.types.CustomLong;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.CustomTypesBuilders;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class TestCustomTypesClient extends RestLiIntegrationTest
{
  private static final Client CLIENT = new TransportClientAdapter(new HttpClientFactory().getClient(
          Collections.<String, String>emptyMap()));
  private static final String URI_PREFIX = "http://localhost:1338/";
  private static final RestClient REST_CLIENT = new RestClient(CLIENT, URI_PREFIX);
  private static final CustomTypesBuilders CUSTOM_TYPES_BUILDERS = new CustomTypesBuilders();

  @Test
  public void testCustomLong() throws RemoteInvocationException
  {
    FindRequest<Greeting> request = CUSTOM_TYPES_BUILDERS.findByCustomLong().lParam(new CustomLong(5L)).build();
    List<Greeting> elements = REST_CLIENT.sendRequest(request).getResponse().getEntity().getElements();
    Assert.assertEquals(elements.size(), 0);
  }

  @Test
  public void testCustomLongArray() throws RemoteInvocationException
  {
    List<CustomLong> ls = new ArrayList<CustomLong>(2);
    ls.add(new CustomLong(1L));
    ls.add(new CustomLong(2L));

    FindRequest<Greeting> request = CUSTOM_TYPES_BUILDERS.findByCustomLongArray().lsParam(ls).build();
    List<Greeting> elements = REST_CLIENT.sendRequest(request).getResponse().getEntity().getElements();
    Assert.assertEquals(elements.size(), 0);
  }

  @Test void testDate() throws RemoteInvocationException
  {
    FindRequest<Greeting> request = CUSTOM_TYPES_BUILDERS.findByDate().dParam(new Date(100)).build();
    List<Greeting> elements = REST_CLIENT.sendRequest(request).getResponse().getEntity().getElements();
    Assert.assertEquals(elements.size(), 0);
  }

}
