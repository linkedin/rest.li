/*
   Copyright (c) 2013 LinkedIn Corp.

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
package com.linkedin.restli.examples;

import java.util.Collections;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.examples.greetings.client.AssociationsBuilders;
import com.linkedin.restli.examples.greetings.client.AssociationsFindByAssocKeyFinderBuilder;
import com.linkedin.restli.examples.greetings.client.AssociationsFindByAssocKeyFinderOptBuilder;

public class TestAssociationsResource extends RestLiIntegrationTest
{
  private static final Client            CLIENT      = new TransportClientAdapter(new HttpClientFactory().getClient(Collections.<String, String> emptyMap()));
  private static final String            URI_PREFIX  = "http://localhost:1338/";
  private static final RestClient        REST_CLIENT = new RestClient(CLIENT, URI_PREFIX);

  @BeforeClass
  public void initClass() throws Exception
  {
    super.init();
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  @Test
  public void testOptionalAssociationKeyInFinder() throws Exception
  {
    // optional and present
    AssociationsFindByAssocKeyFinderOptBuilder finder = new AssociationsBuilders().findByAssocKeyFinderOpt().assocKey("src", "KEY1");
    Assert.assertEquals(200, REST_CLIENT.sendRequest(finder).getResponse().getStatus());

    // optional and not present
    AssociationsFindByAssocKeyFinderOptBuilder finderNoAssocKey = new AssociationsBuilders().findByAssocKeyFinderOpt();
    Assert.assertEquals(200, REST_CLIENT.sendRequest(finderNoAssocKey).getResponse().getStatus());
  }

  @Test
  public void testRequiredAssociationKeyInFinder() throws Exception
  {
    // required and present
    AssociationsFindByAssocKeyFinderBuilder finder = new AssociationsBuilders().findByAssocKeyFinder().assocKey("src", "KEY1");
    Assert.assertEquals(200, REST_CLIENT.sendRequest(finder).getResponse().getStatus());

    // required and not present
    AssociationsFindByAssocKeyFinderBuilder finderNoAssocKey = new AssociationsBuilders().findByAssocKeyFinder();
    try
    {
      REST_CLIENT.sendRequest(finderNoAssocKey).getResponse();
      Assert.fail("Calling a finder without a required association key should throw RestLiResponseException with status code 400");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(400, e.getStatus());
    }
  }
}
