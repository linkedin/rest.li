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
import com.linkedin.restli.client.BatchGetKVRequest;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.RequestBuilder;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.CreateStatus;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.examples.custom.types.CustomLong;
import com.linkedin.restli.examples.custom.types.CustomNonNegativeLong;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.ChainedTyperefsBuilders;
import com.linkedin.restli.examples.greetings.client.ChainedTyperefsRequestBuilders;
import com.linkedin.restli.examples.greetings.client.CustomTypes2Builders;
import com.linkedin.restli.examples.greetings.client.CustomTypes2RequestBuilders;
import com.linkedin.restli.examples.greetings.client.CustomTypes3Builders;
import com.linkedin.restli.examples.greetings.client.CustomTypes3RequestBuilders;
import com.linkedin.restli.examples.greetings.client.CustomTypes4Builders;
import com.linkedin.restli.examples.greetings.client.CustomTypes4RequestBuilders;
import com.linkedin.restli.examples.greetings.client.CustomTypesBuilders;
import com.linkedin.restli.examples.greetings.client.CustomTypesRequestBuilders;
import com.linkedin.restli.examples.typeref.api.CustomLongRefArray;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

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
  public void testCustomLong(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> request = builders.findBy("CustomLong").setQueryParam("l", new CustomLong(5L)).build();
    List<Greeting> elements = REST_CLIENT.sendRequest(request).getResponse().getEntity().getElements();
    Assert.assertEquals(0, elements.size());
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testCustomLongArray(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    List<CustomLong> ls = new ArrayList<CustomLong>(2);
    ls.add(new CustomLong(1L));
    ls.add(new CustomLong(2L));

    Request<CollectionResponse<Greeting>> request = builders.findBy("CustomLongArray").setQueryParam("ls", ls).build();
    List<Greeting> elements = REST_CLIENT.sendRequest(request).getResponse().getEntity().getElements();
    Assert.assertEquals(0, elements.size());
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testCustomLongArrayOBO(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> request = builders.findBy("CustomLongArray").addQueryParam("Ls", new CustomLong(1L)).addQueryParam("Ls", new CustomLong(2L)).build();
    List<Greeting> elements = REST_CLIENT.sendRequest(request).getResponse().getEntity().getElements();
    Assert.assertEquals(0, elements.size());
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testDate(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> request = builders.findBy("Date").setQueryParam("d", new Date(100)).build();
    List<Greeting> elements = REST_CLIENT.sendRequest(request).getResponse().getEntity().getElements();
    Assert.assertEquals(0, elements.size());
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testAction(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Long lo = 5L;
    Request<Long> request = builders.<Long>action("Action").setActionParam("L", new CustomLong(lo)).build();
    Long result = REST_CLIENT.sendRequest(request).getResponse().getEntity();

    Assert.assertEquals(lo, result);
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testCustomLongArrayOnAction(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    CustomLongRefArray ls = new CustomLongRefArray();
    ls.add(new CustomLong(1L));
    ls.add(new CustomLong(2L));

    Request<CustomLongRefArray> request = builders.<CustomLongRefArray>action("ArrayAction").setActionParam("Ls", ls).build();
    CustomLongRefArray elements = REST_CLIENT.sendRequest(request).getResponse().getEntity();
    Assert.assertEquals(2, elements.size());
    Assert.assertEquals(1L, elements.get(0).toLong().longValue());
    Assert.assertEquals(2L, elements.get(1).toLong().longValue());
  }

  @Test(dataProvider = "request2BuilderDataProvider")
  public void testCollectionGet(RootBuilderWrapper<CustomLong, Greeting> builders) throws RemoteInvocationException
  {
    Long lo = 5L;
    Request<Greeting> request = builders.get().id(new CustomLong(lo)).build();
    Greeting result = REST_CLIENT.sendRequest(request).getResponse().getEntity();

    Assert.assertEquals(lo, result.getId());
  }

  @Test(dataProvider = "request2BuilderDataProvider")
  public void testCollectionBatchGet(RootBuilderWrapper<CustomLong, Greeting> builders) throws RemoteInvocationException
  {
    Request<BatchResponse<Greeting>> request = builders.batchGet().ids(new CustomLong(1L), new CustomLong(2L), new CustomLong(3L)).build();
    Map<String,Greeting> greetings = REST_CLIENT.sendRequest(request).getResponse().getEntity().getResults();

    Assert.assertEquals(3, greetings.size());
    Assert.assertEquals(1L, greetings.get("1").getId().longValue());
    Assert.assertEquals(2L, greetings.get("2").getId().longValue());
    Assert.assertEquals(3L, greetings.get("3").getId().longValue());
  }

  @Test(dataProvider = "request2BuilderDataProvider")
  public void testCollectionBatchKVGet(RootBuilderWrapper<CustomLong, Greeting> builders) throws RemoteInvocationException
  {
    BatchGetKVRequest<CustomLong, Greeting> request = builders.batchGet().ids(new CustomLong(1L), new CustomLong(2L), new CustomLong(3L)).buildKV();
    Map<CustomLong, Greeting> greetings = REST_CLIENT.sendRequest(request).getResponse().getEntity().getResults();

    Assert.assertEquals(3, greetings.size());
    Assert.assertEquals(1L, greetings.get(new CustomLong(1L)).getId().longValue());
    Assert.assertEquals(2L, greetings.get(new CustomLong(2L)).getId().longValue());
    Assert.assertEquals(3L, greetings.get(new CustomLong(3L)).getId().longValue());
  }

  @Test(dataProvider = "request2BuilderDataProvider")
  public void testCollectionBatchDelete(RootBuilderWrapper<CustomLong, Greeting> builders) throws RemoteInvocationException
  {
    Request<BatchKVResponse<CustomLong, UpdateStatus>> request = builders.batchDelete().ids(new CustomLong(1L), new CustomLong(2L), new CustomLong(3L)).build();
    Map<CustomLong, UpdateStatus> statuses = REST_CLIENT.sendRequest(request).getResponse().getEntity().getResults();

    Assert.assertEquals(3, statuses.size());
    Assert.assertEquals(HttpStatus.S_204_NO_CONTENT.getCode(), statuses.get(new CustomLong(1L)).getStatus().intValue());
    Assert.assertEquals(HttpStatus.S_204_NO_CONTENT.getCode(), statuses.get(new CustomLong(2L)).getStatus().intValue());
    Assert.assertEquals(HttpStatus.S_204_NO_CONTENT.getCode(), statuses.get(new CustomLong(3L)).getStatus().intValue());
  }

  @Test(dataProvider = "request2BuilderDataProvider")
  public void testCollectionBatchUpdate(RootBuilderWrapper<CustomLong, Greeting> builders) throws RemoteInvocationException
  {
    RequestBuilder<? extends Request<BatchKVResponse<CustomLong, UpdateStatus>>> request = builders.batchUpdate().input(new CustomLong(1L), new Greeting().setId(1)).input(new CustomLong(2L), new Greeting().setId(2)).getBuilder();
    Map<CustomLong, UpdateStatus> statuses = REST_CLIENT.sendRequest(request).getResponse().getEntity().getResults();

    Assert.assertEquals(2, statuses.size());
    Assert.assertEquals(HttpStatus.S_204_NO_CONTENT.getCode(), statuses.get(new CustomLong(1L)).getStatus().intValue());
    Assert.assertEquals(HttpStatus.S_204_NO_CONTENT.getCode(), statuses.get(new CustomLong(2L)).getStatus().intValue());
  }

  @Test(dataProvider = "request2BuilderDataProvider")
  public void testCollectionBatchPartialUpdate(RootBuilderWrapper<CustomLong, Greeting> builders) throws RemoteInvocationException
  {
    RequestBuilder<? extends Request<BatchKVResponse<CustomLong, UpdateStatus>>> request = builders.batchPartialUpdate().input(new CustomLong(1L), new PatchRequest<Greeting>()).input(new CustomLong(2L), new PatchRequest<Greeting>()).getBuilder();
    Map<CustomLong, UpdateStatus> statuses = REST_CLIENT.sendRequest(request).getResponse().getEntity().getResults();

    Assert.assertEquals(2, statuses.size());
    Assert.assertEquals(HttpStatus.S_204_NO_CONTENT.getCode(), statuses.get(new CustomLong(1L)).getStatus().intValue());
    Assert.assertEquals(HttpStatus.S_204_NO_CONTENT.getCode(), statuses.get(new CustomLong(2L)).getStatus().intValue());
  }

  @Test(dataProvider = "request2BuilderDataProvider")
  public void testCollectionBatchCreate(RootBuilderWrapper<CustomLong, Greeting> builders) throws RemoteInvocationException
  {
    RequestBuilder<? extends Request<CollectionResponse<CreateStatus>>> request = builders.batchCreate().input(new Greeting().setId(1)).input(new Greeting().setId(2)).getBuilder();
    List<CreateStatus> results = REST_CLIENT.sendRequest(request).getResponse().getEntity().getElements();

    Assert.assertEquals(2, results.size());
    Assert.assertEquals(HttpStatus.S_204_NO_CONTENT.getCode(), results.get(0).getStatus().intValue());
    Assert.assertEquals("1", results.get(0).getId());

    Assert.assertEquals(HttpStatus.S_204_NO_CONTENT.getCode(), results.get(1).getStatus().intValue());
    Assert.assertEquals("2", results.get(1).getId());
  }

  @Test(dataProvider = "request4BuilderDataProvider")
  public void testCollectionSubResourceGet(RootBuilderWrapper<CustomLong, Greeting> builders) throws RemoteInvocationException
  {
    Long id2 = 2L;
    Long id4 = 4L;
    Request<Greeting> request = builders.get().setPathKey("customTypes2Id", new CustomLong(id2)).id(new CustomLong(id4)).build();
    Greeting result = REST_CLIENT.sendRequest(request).getResponse().getEntity();

    Assert.assertEquals(new Long(id2*id4), result.getId());
  }

  @Test(dataProvider = "request3BuilderDataProvider")
  public void testAssociationGet(RootBuilderWrapper<CompoundKey, Greeting> builders) throws RemoteInvocationException
  {
    Long lo = 5L;
    Long date = 13L;
    CustomTypes3Builders.Key key = new CustomTypes3Builders.Key().setLongId(new CustomLong(lo)).setDateId(new Date(date));

    Request<Greeting> request = builders.get().id(key).build();
    Greeting result = REST_CLIENT.sendRequest(request).getResponse().getEntity();

    Assert.assertEquals( new Long(lo+date), result.getId());
  }

  @Test(dataProvider = "request3BuilderDataProvider")
  public void testBatchUpdate(RootBuilderWrapper<CompoundKey, Greeting> builders) throws RemoteInvocationException
  {
    Long lo = 5L;
    Long date = 13L;
    CustomTypes3Builders.Key key = new CustomTypes3Builders.Key().setLongId(new CustomLong(lo)).setDateId(new Date(date));

    RequestBuilder<? extends Request<BatchKVResponse<CompoundKey, UpdateStatus>>> batchUpdateRequest = builders.batchUpdate().input(key, new Greeting().setId(1).setMessage("foo")).getBuilder();
    BatchKVResponse<CompoundKey, UpdateStatus> response = REST_CLIENT.sendRequest(batchUpdateRequest).getResponse().getEntity();

    Assert.assertEquals(response.getResults().keySet().size(), 1);
    CompoundKey expected = new CompoundKey();
    expected.append("dateId", new Date(date));
    expected.append("longId", new CustomLong(lo));
    Assert.assertEquals(response.getResults().keySet().iterator().next(), expected);
  }

  @Test(dataProvider = "requestChainedTyperefsBuilderDataProvider")
  public void testBatchUpdateForChainedRefs(RootBuilderWrapper<CompoundKey, Greeting> builders) throws RemoteInvocationException
  {
    Long lo = 29L;
    Long date = 10L;
    ChainedTyperefsBuilders.Key key = new ChainedTyperefsBuilders.Key().setAge(new CustomNonNegativeLong(lo)).setBirthday(new Date(date));

    RequestBuilder<? extends Request<BatchKVResponse<CompoundKey, UpdateStatus>>> batchUpdateRequest = builders.batchUpdate().input(key, new Greeting().setId(1).setMessage("foo")).getBuilder();
    BatchKVResponse<CompoundKey, UpdateStatus> response = REST_CLIENT.sendRequest(batchUpdateRequest).getResponse().getEntity();

    Assert.assertEquals(response.getResults().keySet().size(), 1);
    CompoundKey expected = new CompoundKey();
    expected.append("birthday", new Date(date));
    expected.append("age", new CustomNonNegativeLong(lo));
    CompoundKey result = response.getResults().keySet().iterator().next();
    Assert.assertEquals(result, expected);
  }

  @Test(dataProvider = "request3BuilderDataProvider")
  public void testAssocKey(RootBuilderWrapper<CompoundKey, Greeting> builders) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> request = builders.findBy("DateOnly").setPathKey("dateId", new Date(13L)).build();
    List<Greeting> response = REST_CLIENT.sendRequest(request).getResponse().getEntity().getElements();

    Assert.assertEquals(0, response.size());
  }

  @DataProvider
  @SuppressWarnings("rawtypes")
  private static Object[][] requestBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper(new CustomTypesBuilders()) },
      { new RootBuilderWrapper(new CustomTypesRequestBuilders()) }
    };
  }

  @DataProvider
  @SuppressWarnings("rawtypes")
  private static Object[][] request2BuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper(new CustomTypes2Builders()) },
      { new RootBuilderWrapper(new CustomTypes2RequestBuilders()) }
    };
  }

  @DataProvider
  @SuppressWarnings("rawtypes")
  private static Object[][] request3BuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper(new CustomTypes3Builders()) },
      { new RootBuilderWrapper(new CustomTypes3RequestBuilders()) }
    };
  }

  @DataProvider
  @SuppressWarnings("rawtypes")
  private static Object[][] request4BuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper(new CustomTypes4Builders()) },
      { new RootBuilderWrapper(new CustomTypes4RequestBuilders()) }
    };
  }

  @DataProvider
  @SuppressWarnings("rawtypes")
  private static Object[][] requestChainedTyperefsBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper(new ChainedTyperefsBuilders()) },
      { new RootBuilderWrapper(new ChainedTyperefsRequestBuilders()) }
    };
  }
}
