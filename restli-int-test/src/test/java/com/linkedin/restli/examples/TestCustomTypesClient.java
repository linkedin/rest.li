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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.linkedin.restli.common.CollectionResponse;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.ActionRequest;
import com.linkedin.restli.client.BatchGetRequest;
import com.linkedin.restli.client.FindRequest;
import com.linkedin.restli.client.GetRequest;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.examples.custom.types.CustomLong;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.CustomTypes2Builders;
import com.linkedin.restli.examples.greetings.client.CustomTypes3Builders;
import com.linkedin.restli.examples.greetings.client.CustomTypes4Builders;
import com.linkedin.restli.examples.greetings.client.CustomTypesBuilders;

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
  private static final CustomTypes2Builders CUSTOM_TYPES_2_BUILDERS = new CustomTypes2Builders();
  private static final CustomTypes3Builders CUSTOM_TYPES_3_BUILDERS = new CustomTypes3Builders();
  private static final CustomTypes4Builders CUSTOM_TYPES_4_BUILDERS = new CustomTypes4Builders();

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
  public void testCustomLong() throws RemoteInvocationException
  {
    FindRequest<Greeting> request = CUSTOM_TYPES_BUILDERS.findByCustomLong().lParam(new CustomLong(5L)).build();
    List<Greeting> elements = REST_CLIENT.sendRequest(request).getResponse().getEntity().getElements();
    Assert.assertEquals(0, elements.size());
  }

  @Test
  public void testCustomLongArray() throws RemoteInvocationException
  {
    List<CustomLong> ls = new ArrayList<CustomLong>(2);
    ls.add(new CustomLong(1L));
    ls.add(new CustomLong(2L));

    FindRequest<Greeting> request = CUSTOM_TYPES_BUILDERS.findByCustomLongArray().lsParam(ls).build();
    List<Greeting> elements = REST_CLIENT.sendRequest(request).getResponse().getEntity().getElements();
    Assert.assertEquals(0, elements.size());
  }

  @Test
  public void testDate() throws RemoteInvocationException
  {
    FindRequest<Greeting> request = CUSTOM_TYPES_BUILDERS.findByDate().dParam(new Date(100)).build();
    List<Greeting> elements = REST_CLIENT.sendRequest(request).getResponse().getEntity().getElements();
    Assert.assertEquals(0, elements.size());
  }

  @Test
  public void testAction() throws RemoteInvocationException
  {
    Long lo = 5L;
    ActionRequest<Long> request = CUSTOM_TYPES_BUILDERS.actionAction().paramL(new CustomLong(lo)).build();
    Long result = REST_CLIENT.sendRequest(request).getResponse().getEntity();

    Assert.assertEquals(lo, result);
  }

  @Test
  public void testCollectionGet() throws RemoteInvocationException
  {
    Long lo = 5L;
    GetRequest<Greeting> request = CUSTOM_TYPES_2_BUILDERS.get().id(new CustomLong(lo)).build();
    Greeting result = REST_CLIENT.sendRequest(request).getResponse().getEntity();

    Assert.assertEquals(lo, result.getId());
  }

  @Test
  public void testCollectionBatchGet() throws RemoteInvocationException
  {
    BatchGetRequest<Greeting> request = CUSTOM_TYPES_2_BUILDERS.batchGet().ids(new CustomLong(1L), new CustomLong(2L), new CustomLong(3L)).build();
    Map<String,Greeting> greetings = REST_CLIENT.sendRequest(request).getResponse().getEntity().getResults();

    Assert.assertEquals(3, greetings.size());
  }

  @Test
  public void testCollectionSubResourceGet() throws RemoteInvocationException
  {
    Long id2 = 2L;
    Long id4 = 4L;
    GetRequest<Greeting> request = CUSTOM_TYPES_4_BUILDERS.get().customTypes2IdKey(new CustomLong(id2)).id(new CustomLong(id4)).build();
    Greeting result = REST_CLIENT.sendRequest(request).getResponse().getEntity();

    Assert.assertEquals(new Long(id2*id4), result.getId());
  }

  @Test
  public void testAssociationGet() throws RemoteInvocationException
  {
    Long lo = 5L;
    Long date = 13L;
    CustomTypes3Builders.Key key = new CustomTypes3Builders.Key().setLongId(new CustomLong(lo)).setDateId(new Date(date));

    GetRequest<Greeting> request = CUSTOM_TYPES_3_BUILDERS.get().id(key).build();
    Greeting result = REST_CLIENT.sendRequest(request).getResponse().getEntity();

    Assert.assertEquals( new Long(lo+date), result.getId());
  }

  @Test
  public void testAssocKey() throws RemoteInvocationException
  {
    FindRequest<Greeting> request = CUSTOM_TYPES_3_BUILDERS.findByDateOnly().dateIdKey(new Date(13L)).build();
    List<Greeting> response = REST_CLIENT.sendRequest(request).getResponse().getEntity().getElements();

    Assert.assertEquals(0, response.size());
  }

}
