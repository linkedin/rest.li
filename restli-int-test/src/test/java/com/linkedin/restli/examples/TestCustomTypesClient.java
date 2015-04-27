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
import com.linkedin.restli.client.BatchCreateIdRequest;
import com.linkedin.restli.client.BatchCreateRequest;
import com.linkedin.restli.client.CreateIdRequest;
import com.linkedin.restli.client.CreateRequest;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.RequestBuilder;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.client.response.CreateResponse;
import com.linkedin.restli.common.BatchCreateIdResponse;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.CreateStatus;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.IdResponse;
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
import com.linkedin.restli.internal.common.ProtocolVersionUtil;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testCustomLong(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> request = builders.findBy("CustomLong").setQueryParam("l", new CustomLong(5L)).build();
    List<Greeting> elements = getClient().sendRequest(request).getResponse().getEntity().getElements();
    Assert.assertEquals(elements.size(), 0);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testCustomLongArray(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    List<CustomLong> ls = new ArrayList<CustomLong>(2);
    ls.add(new CustomLong(1L));
    ls.add(new CustomLong(2L));

    Request<CollectionResponse<Greeting>> request = builders.findBy("CustomLongArray").setQueryParam("ls", ls).build();
    List<Greeting> elements = getClient().sendRequest(request).getResponse().getEntity().getElements();
    Assert.assertEquals(elements.size(), 0);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testCustomLongArrayOBO(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> request = builders.findBy("CustomLongArray").addQueryParam("Ls", new CustomLong(1L)).addQueryParam("Ls", new CustomLong(2L)).build();
    List<Greeting> elements = getClient().sendRequest(request).getResponse().getEntity().getElements();
    Assert.assertEquals(elements.size(), 0);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testDate(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> request = builders.findBy("Date").setQueryParam("d", new Date(100)).build();
    List<Greeting> elements = getClient().sendRequest(request).getResponse().getEntity().getElements();
    Assert.assertEquals(elements.size(), 0);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testAction(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Long lo = 5L;
    Request<Long> request = builders.<Long>action("Action").setActionParam("L", new CustomLong(lo)).build();
    Long result = getClient().sendRequest(request).getResponse().getEntity();

    Assert.assertEquals(result, lo);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testCustomLongArrayOnAction(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    CustomLongRefArray ls = new CustomLongRefArray();
    ls.add(new CustomLong(1L));
    ls.add(new CustomLong(2L));

    Request<CustomLongRefArray> request = builders.<CustomLongRefArray>action("ArrayAction").setActionParam("Ls", ls).build();
    CustomLongRefArray elements = getClient().sendRequest(request).getResponse().getEntity();
    Assert.assertEquals(elements.size(), 2);
    Assert.assertEquals(elements.get(0).toLong().longValue(), 1L);
    Assert.assertEquals(elements.get(1).toLong().longValue(), 2L);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "request2BuilderDataProvider")
  public void testCollectionGet(RootBuilderWrapper<CustomLong, Greeting> builders) throws RemoteInvocationException
  {
    Long lo = 5L;
    Request<Greeting> request = builders.get().id(new CustomLong(lo)).build();
    Greeting result = getClient().sendRequest(request).getResponse().getEntity();

    Assert.assertEquals(result.getId(), lo);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  public void testCollectionBatchGet(RestliRequestOptions requestOptions) throws RemoteInvocationException
  {
    Request<BatchResponse<Greeting>> request =
      new CustomTypes2Builders(requestOptions).batchGet().ids(new CustomLong(1L), new CustomLong(2L), new CustomLong(3L)).build();
    Map<String, Greeting> greetings = getClient().sendRequest(request).getResponse().getEntity().getResults();

    Assert.assertEquals(greetings.size(), 3);
    Assert.assertEquals(greetings.get("1").getId().longValue(), 1L);
    Assert.assertEquals(greetings.get("2").getId().longValue(), 2L);
    Assert.assertEquals(greetings.get("3").getId().longValue(), 3L);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  public void testCollectionBatchGetKV(RestliRequestOptions requestOptions) throws RemoteInvocationException
  {
    Request<BatchKVResponse<CustomLong, Greeting>> request =
      new CustomTypes2Builders(requestOptions).batchGet().ids(new CustomLong(1L), new CustomLong(2L), new CustomLong(3L)).buildKV();
    Map<CustomLong, Greeting> greetings = getClient().sendRequest(request).getResponse().getEntity().getResults();

    Assert.assertEquals(greetings.size(), 3);
    Assert.assertEquals(greetings.get(new CustomLong(1L)).getId().longValue(), 1L);
    Assert.assertEquals(greetings.get(new CustomLong(2L)).getId().longValue(), 2L);
    Assert.assertEquals(greetings.get(new CustomLong(3L)).getId().longValue(), 3L);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  public void testCollectionBatchGetEntity(RestliRequestOptions requestOptions) throws RemoteInvocationException
  {
    Request<BatchKVResponse<CustomLong, EntityResponse<Greeting>>> request =
      new CustomTypes2RequestBuilders(requestOptions).batchGet().ids(new CustomLong(1L), new CustomLong(2L), new CustomLong(3L)).build();
    Map<CustomLong, EntityResponse<Greeting>> greetings = getClient().sendRequest(request).getResponse().getEntity().getResults();

    Assert.assertEquals(greetings.size(), 3);
    Assert.assertEquals(greetings.get(new CustomLong(1L)).getEntity().getId().longValue(), 1L);
    Assert.assertEquals(greetings.get(new CustomLong(2L)).getEntity().getId().longValue(), 2L);
    Assert.assertEquals(greetings.get(new CustomLong(3L)).getEntity().getId().longValue(), 3L);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "request2BuilderDataProvider")
  public void testCollectionBatchDelete(RootBuilderWrapper<CustomLong, Greeting> builders) throws RemoteInvocationException
  {
    Request<BatchKVResponse<CustomLong, UpdateStatus>> request = builders.batchDelete().ids(new CustomLong(1L), new CustomLong(2L), new CustomLong(3L)).build();
    Map<CustomLong, UpdateStatus> statuses = getClient().sendRequest(request).getResponse().getEntity().getResults();

    Assert.assertEquals(statuses.size(), 3);
    Assert.assertEquals(statuses.get(new CustomLong(1L)).getStatus().intValue(), HttpStatus.S_204_NO_CONTENT.getCode());
    Assert.assertEquals(statuses.get(new CustomLong(2L)).getStatus().intValue(), HttpStatus.S_204_NO_CONTENT.getCode());
    Assert.assertEquals(statuses.get(new CustomLong(3L)).getStatus().intValue(), HttpStatus.S_204_NO_CONTENT.getCode());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "request2BuilderDataProvider")
  public void testCollectionBatchUpdate(RootBuilderWrapper<CustomLong, Greeting> builders) throws RemoteInvocationException
  {
    RequestBuilder<? extends Request<BatchKVResponse<CustomLong, UpdateStatus>>> request =
      builders.batchUpdate().input(new CustomLong(1L), new Greeting().setId(1)).input(new CustomLong(2L), new Greeting().setId(2)).getBuilder();
    Map<CustomLong, UpdateStatus> statuses = getClient().sendRequest(request).getResponse().getEntity().getResults();

    Assert.assertEquals(statuses.size(), 2);
    Assert.assertEquals(statuses.get(new CustomLong(1L)).getStatus().intValue(), HttpStatus.S_204_NO_CONTENT.getCode());
    Assert.assertEquals(statuses.get(new CustomLong(2L)).getStatus().intValue(), HttpStatus.S_204_NO_CONTENT.getCode());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "request2BuilderDataProvider")
  public void testCollectionBatchPartialUpdate(RootBuilderWrapper<CustomLong, Greeting> builders) throws RemoteInvocationException
  {
    RequestBuilder<? extends Request<BatchKVResponse<CustomLong, UpdateStatus>>> request = builders.batchPartialUpdate().input(new CustomLong(1L),
                                                                                                                               new PatchRequest<Greeting>()).input(new CustomLong(2L), new PatchRequest<Greeting>()).getBuilder();
    Map<CustomLong, UpdateStatus> statuses = getClient().sendRequest(request).getResponse().getEntity().getResults();

    Assert.assertEquals(statuses.size(), 2);
    Assert.assertEquals(statuses.get(new CustomLong(1L)).getStatus().intValue(), HttpStatus.S_204_NO_CONTENT.getCode());
    Assert.assertEquals(statuses.get(new CustomLong(2L)).getStatus().intValue(), HttpStatus.S_204_NO_CONTENT.getCode());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  public void testCollectionCreate(RestliRequestOptions requestOptions) throws RemoteInvocationException
  {
    CreateRequest<Greeting> request = new CustomTypes2Builders(requestOptions).create().input(new Greeting().setId(10)).build();
    Response<EmptyRecord> response = getClient().sendRequest(request).getResponse();

    Assert.assertEquals(response.getStatus(), HttpStatus.S_201_CREATED.getCode());

    @SuppressWarnings("unchecked")
    CreateResponse<CustomLong> createResponse = (CreateResponse<CustomLong>)response.getEntity();
    Assert.assertEquals(createResponse.getId(), new CustomLong(10L));
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  public void testCollectionCreateId(RestliRequestOptions requestOptions) throws RemoteInvocationException
  {
    CreateIdRequest<CustomLong, Greeting> request = new CustomTypes2RequestBuilders(requestOptions).create().input(new Greeting().setId(10)).build();
    Response<IdResponse<CustomLong>> response = getClient().sendRequest(request).getResponse();

    Assert.assertEquals(response.getStatus(), HttpStatus.S_201_CREATED.getCode());
    Assert.assertEquals(response.getEntity().getId(), new CustomLong(10L));
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  public void testCollectionBatchCreate(RestliRequestOptions options) throws RemoteInvocationException
  {
    CustomTypes2Builders builders = new CustomTypes2Builders(options);
    BatchCreateRequest<Greeting> request = builders.batchCreate().input(new Greeting().setId(1)).input(new Greeting().setId(2)).build();
    Response<CollectionResponse<CreateStatus>> response = getClient().sendRequest(request).getResponse();
    List<CreateStatus> results = response.getEntity().getElements();

    Set<CustomLong> expectedKeys = new HashSet<CustomLong>();
    expectedKeys.add(new CustomLong(1L));
    expectedKeys.add(new CustomLong(2L));

    for(CreateStatus status: results)
    {
      @SuppressWarnings("unchecked")
      CreateIdStatus<CustomLong> createIdStatus = (CreateIdStatus<CustomLong>) status;
      Assert.assertEquals(createIdStatus.getStatus().intValue(), HttpStatus.S_204_NO_CONTENT.getCode());
      Assert.assertTrue(expectedKeys.contains(createIdStatus.getKey()));
      @SuppressWarnings("deprecation")
      String id = createIdStatus.getId();
      Assert.assertEquals(BatchResponse.keyToString(createIdStatus.getKey(), ProtocolVersionUtil.extractProtocolVersion(response.getHeaders())),
                          id);
      expectedKeys.remove(createIdStatus.getKey());
    }
    Assert.assertTrue(expectedKeys.isEmpty());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  public void testCollectionBatchCreateId(RestliRequestOptions options) throws RemoteInvocationException
  {
    CustomTypes2RequestBuilders builders = new CustomTypes2RequestBuilders(options);
    BatchCreateIdRequest<CustomLong, Greeting> request = builders.batchCreate().input(new Greeting().setId(1)).input(new Greeting().setId(2)).build();
    Response<BatchCreateIdResponse<CustomLong>> response = getClient().sendRequest(request).getResponse();
    List<CreateIdStatus<CustomLong>> results = response.getEntity().getElements();

    Set<CustomLong> expectedKeys = new HashSet<CustomLong>();
    expectedKeys.add(new CustomLong(1L));
    expectedKeys.add(new CustomLong(2L));

    for(CreateIdStatus<CustomLong> status: results)
    {
      Assert.assertEquals(status.getStatus().intValue(), HttpStatus.S_204_NO_CONTENT.getCode());
      Assert.assertTrue(expectedKeys.contains(status.getKey()));
      @SuppressWarnings("deprecation")
      String id = status.getId();
      Assert.assertEquals(BatchResponse.keyToString(status.getKey(), ProtocolVersionUtil.extractProtocolVersion(response.getHeaders())),
                          id);
      expectedKeys.remove(status.getKey());
    }
    Assert.assertTrue(expectedKeys.isEmpty());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "request4BuilderDataProvider")
  public void testCollectionSubResourceGet(RootBuilderWrapper<CustomLong, Greeting> builders) throws RemoteInvocationException
  {
    Long id2 = 2L;
    Long id4 = 4L;
    Request<Greeting> request = builders.get().setPathKey("customTypes2Id", new CustomLong(id2)).id(new CustomLong(id4)).build();
    Greeting result = getClient().sendRequest(request).getResponse().getEntity();

    Assert.assertEquals(result.getId(), new Long(id2*id4));
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "request3BuilderDataProvider")
  public void testAssociationGet(RootBuilderWrapper<CompoundKey, Greeting> builders) throws RemoteInvocationException
  {
    Long lo = 5L;
    Long date = 13L;
    CustomTypes3Builders.Key key = new CustomTypes3Builders.Key().setLongId(new CustomLong(lo)).setDateId(new Date(date));

    Request<Greeting> request = builders.get().id(key).build();
    Greeting result = getClient().sendRequest(request).getResponse().getEntity();

    Assert.assertEquals(result.getId(),  new Long(lo+date));
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "request3BuilderDataProvider")
  public void testBatchUpdate(RootBuilderWrapper<CompoundKey, Greeting> builders) throws RemoteInvocationException
  {
    Long lo = 5L;
    Long date = 13L;
    CustomTypes3Builders.Key key = new CustomTypes3Builders.Key().setLongId(new CustomLong(lo)).setDateId(new Date(date));

    RequestBuilder<? extends Request<BatchKVResponse<CompoundKey, UpdateStatus>>> batchUpdateRequest = builders.batchUpdate().input(key, new Greeting().setId(1).setMessage("foo")).getBuilder();
    BatchKVResponse<CompoundKey, UpdateStatus> response = getClient().sendRequest(batchUpdateRequest).getResponse().getEntity();

    Assert.assertEquals(response.getResults().keySet().size(), 1);
    CompoundKey expected = new CompoundKey();
    expected.append("dateId", new Date(date));
    expected.append("longId", new CustomLong(lo));
    Assert.assertEquals(response.getResults().keySet().iterator().next(), expected);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestChainedTyperefsBuilderDataProvider")
  public void testBatchUpdateForChainedRefs(RootBuilderWrapper<CompoundKey, Greeting> builders) throws RemoteInvocationException
  {
    Long lo = 29L;
    Long date = 10L;
    ChainedTyperefsBuilders.Key key = new ChainedTyperefsBuilders.Key().setAge(new CustomNonNegativeLong(lo)).setBirthday(new Date(date));

    RequestBuilder<? extends Request<BatchKVResponse<CompoundKey, UpdateStatus>>> batchUpdateRequest = builders.batchUpdate().input(key, new Greeting().setId(1).setMessage("foo")).getBuilder();
    BatchKVResponse<CompoundKey, UpdateStatus> response = getClient().sendRequest(batchUpdateRequest).getResponse().getEntity();

    Assert.assertEquals(1, response.getResults().keySet().size());
    CompoundKey expected = new CompoundKey();
    expected.append("birthday", new Date(date));
    expected.append("age", new CustomNonNegativeLong(lo));
    CompoundKey result = response.getResults().keySet().iterator().next();
    Assert.assertEquals(result, expected);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "request3BuilderDataProvider")
  public void testAssocKey(RootBuilderWrapper<CompoundKey, Greeting> builders) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> request = builders.findBy("DateOnly").setPathKey("dateId", new Date(13L)).build();
    List<Greeting> response = getClient().sendRequest(request).getResponse().getEntity().getElements();

    Assert.assertEquals(response.size(), 0);
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  private static Object[][] requestOptionsDataProvider()
  {
    return new Object[][] {
      { RestliRequestOptions.DEFAULT_OPTIONS },
      { com.linkedin.restli.examples.TestConstants.FORCE_USE_NEXT_OPTIONS }
    };
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  private static Object[][] requestBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<Long, Greeting>(new CustomTypesBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new CustomTypesBuilders(com.linkedin.restli.examples.TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<Long, Greeting>(new CustomTypesRequestBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new CustomTypesRequestBuilders(com.linkedin.restli.examples.TestConstants.FORCE_USE_NEXT_OPTIONS)) }
    };
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "request2BuilderDataProvider")
  private static Object[][] request2BuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<CustomLong, Greeting>(new CustomTypes2Builders()) },
      { new RootBuilderWrapper<CustomLong, Greeting>(new CustomTypes2Builders(com.linkedin.restli.examples.TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<CustomLong, Greeting>(new CustomTypes2RequestBuilders()) },
      { new RootBuilderWrapper<CustomLong, Greeting>(new CustomTypes2RequestBuilders(com.linkedin.restli.examples.TestConstants.FORCE_USE_NEXT_OPTIONS)) }
    };
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "request3BuilderDataProvider")
  private static Object[][] request3BuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<CompoundKey, Greeting>(new CustomTypes3Builders()) },
      { new RootBuilderWrapper<CompoundKey, Greeting>(new CustomTypes3Builders(com.linkedin.restli.examples.TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<CompoundKey, Greeting>(new CustomTypes3RequestBuilders()) },
      { new RootBuilderWrapper<CompoundKey, Greeting>(new CustomTypes3RequestBuilders(com.linkedin.restli.examples.TestConstants.FORCE_USE_NEXT_OPTIONS)) }
    };
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "request4BuilderDataProvider")
  private static Object[][] request4BuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<CustomLong, Greeting>(new CustomTypes4Builders()) },
      { new RootBuilderWrapper<CustomLong, Greeting>(new CustomTypes4Builders(com.linkedin.restli.examples.TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<CustomLong, Greeting>(new CustomTypes4RequestBuilders()) },
      { new RootBuilderWrapper<CustomLong, Greeting>(new CustomTypes4RequestBuilders(com.linkedin.restli.examples.TestConstants.FORCE_USE_NEXT_OPTIONS)) }
    };
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestChainedTyperefsBuilderDataProvider")
  private static Object[][] requestChainedTyperefsBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<CompoundKey, Greeting>(new ChainedTyperefsBuilders()) },
      { new RootBuilderWrapper<CompoundKey, Greeting>(new ChainedTyperefsBuilders(com.linkedin.restli.examples.TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<CompoundKey, Greeting>(new ChainedTyperefsRequestBuilders()) },
      { new RootBuilderWrapper<CompoundKey, Greeting>(new ChainedTyperefsRequestBuilders(com.linkedin.restli.examples.TestConstants.FORCE_USE_NEXT_OPTIONS)) }
    };
  }
}
