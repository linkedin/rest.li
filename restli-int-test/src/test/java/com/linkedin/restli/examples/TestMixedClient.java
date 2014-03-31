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

package com.linkedin.restli.examples;


import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.MixedBuilders;
import com.linkedin.restli.examples.greetings.client.MixedRequestBuilders;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Verify that the server correctly handles resources that mix interface types.
 *
 * @author jnwang
 */
public class TestMixedClient extends RestLiIntegrationTest
{
  private static final Client        CLIENT      = new TransportClientAdapter(new HttpClientFactory().getClient(Collections.<String, String> emptyMap()));
  private static final String        URI_PREFIX  = "http://localhost:1338/";
  private static final RestClient    REST_CLIENT = new RestClient(CLIENT, URI_PREFIX);

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

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testGet(RootBuilderWrapper<Long, Greeting> builders) throws InterruptedException,
      ExecutionException
  {
    Request<Greeting> req = builders.get().id(42L).build();
    ResponseFuture<Greeting> response = REST_CLIENT.sendRequest(req);
    String g = response.get().getEntity().getMessage();
    Assert.assertEquals(g, "42");
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testCreate(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException,
      InterruptedException,
      ExecutionException
  {
    Request<EmptyRecord> req = builders.create().input(new Greeting()).build();
    ResponseFuture<EmptyRecord> response = REST_CLIENT.sendRequest(req);
    response.getResponse().getHeaders();
    response.get().getEntity();
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testUpdate(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException,
      InterruptedException,
      ExecutionException
  {
    Request<EmptyRecord> req = builders.update().id(1L).input(new Greeting()).build();
    ResponseFuture<EmptyRecord> response = REST_CLIENT.sendRequest(req);
    response.getResponse().getHeaders();
    response.get().getEntity();
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testDelete(RootBuilderWrapper<Long, Greeting> builders) throws InterruptedException,
      ExecutionException,
      RemoteInvocationException
  {
    Request<EmptyRecord> req = builders.delete().id(1L).build();
    ResponseFuture<EmptyRecord> response = REST_CLIENT.sendRequest(req);
    response.getResponse().getHeaders();
    response.get().getEntity();
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testSearch(RootBuilderWrapper<Long, Greeting> builders) throws InterruptedException,
      ExecutionException
  {
    Request<CollectionResponse<Greeting>> req = builders.findBy("Search").setQueryParam("what", "yay").build();
    ResponseFuture<CollectionResponse<Greeting>> response = REST_CLIENT.sendRequest(req);
    List<Greeting> elems = response.get().getEntity().getElements();
    Assert.assertEquals(elems.size(), 1);
    String g = elems.get(0).getMessage();
    Assert.assertEquals(g, "yay");
  }

  @DataProvider
  private static Object[][] requestBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<Long, Greeting>(new MixedBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new MixedRequestBuilders()) }
    };
  }
}
