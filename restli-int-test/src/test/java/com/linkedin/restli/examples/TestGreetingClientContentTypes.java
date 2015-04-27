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
import com.linkedin.restli.client.CreateIdRequest;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.client.response.CreateResponse;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.IdResponse;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.GreetingsBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsRequestBuilders;
import com.linkedin.restli.examples.groups.api.TransferOwnershipRequest;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class TestGreetingClientContentTypes extends RestLiIntegrationTest
{
  private static final List<RestClient.AcceptType> ACCEPT_TYPES = Collections.singletonList(RestClient.AcceptType.JSON);

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

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientDataDataProvider")
  public void testGet(RestClient restClient, RootBuilderWrapper<Long, Greeting> builders) throws
    RemoteInvocationException
  {
    Request<Greeting> request = builders.get().id(1L).build();
    Response<Greeting> response = restClient.sendRequest(request).getResponse();
    Greeting greeting = response.getEntity();
    Assert.assertEquals(greeting.getId(), new Long(1));
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientDataBatchDataProvider")
  public void testBatchGet(RestClient restClient, RestliRequestOptions requestOptions)
    throws RemoteInvocationException
  {
    List<Long> ids = Arrays.asList(1L, 2L, 3L, 4L);
    Request<BatchResponse<Greeting>> request = new GreetingsBuilders(requestOptions).batchGet().ids(ids).build();

    Response<BatchResponse<Greeting>> response = restClient.sendRequest(request).getResponse();
    BatchResponse<Greeting> batchResponse = response.getEntity();
    Assert.assertEquals(batchResponse.getResults().size(), ids.size());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientDataBatchDataProvider")
  public void testBatchGetKV(RestClient restClient, RestliRequestOptions requestOptions)
    throws RemoteInvocationException
  {
    List<Long> ids = Arrays.asList(1L, 2L, 3L, 4L);
    Request<BatchKVResponse<Long, Greeting>> request = new GreetingsBuilders(requestOptions).batchGet().ids(ids).buildKV();

    Response<BatchKVResponse<Long, Greeting>> response = restClient.sendRequest(request).getResponse();
    BatchKVResponse<Long, Greeting> batchResponse = response.getEntity();
    Assert.assertEquals(batchResponse.getResults().size(), ids.size());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientDataBatchDataProvider")
  public void testBatchGetEntity(RestClient restClient, RestliRequestOptions requestOptions)
    throws RemoteInvocationException
  {
    List<Long> ids = Arrays.asList(1L, 2L, 3L, 4L);
    Request<BatchKVResponse<Long, EntityResponse<Greeting>>> request = new GreetingsRequestBuilders(requestOptions).batchGet().ids(ids).build();

    Response<BatchKVResponse<Long, EntityResponse<Greeting>>> response = restClient.sendRequest(request).getResponse();
    BatchKVResponse<Long, EntityResponse<Greeting>> batchResponse = response.getEntity();
    Assert.assertEquals(batchResponse.getResults().size(), ids.size());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientDataDataProvider")
  public void testFinder(RestClient restClient, RootBuilderWrapper<Long, Greeting> builders)
    throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> request = builders.findBy("Search").setQueryParam("tone", Tone.SINCERE).paginate(1, 2).build();

    Response<CollectionResponse<Greeting>> response = restClient.sendRequest(request).getResponse();

    CollectionResponse<Greeting> collectionResponse = response.getEntity();
    List<Greeting> greetings = collectionResponse.getElements();

    for(Greeting g : greetings)
    {
      Assert.assertEquals(g.getTone(), Tone.SINCERE);
    }
    collectionResponse.getPaging().getLinks();
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientDataDataProvider")
  public void testAction(RestClient restClient, RootBuilderWrapper<Long, Greeting> builders)
    throws RemoteInvocationException
  {
    Request<Greeting> request = builders.<Greeting>action("SomeAction").id(1L)
      .setActionParam("A", 1)
      .setActionParam("B", "")
      .setActionParam("C", new TransferOwnershipRequest())
      .setActionParam("D", new TransferOwnershipRequest())
      .setActionParam("E", 3)
      .build();

    Response<Greeting> response = restClient.sendRequest(request).getResponse();

    Greeting greeting = response.getEntity();
    Assert.assertEquals(greeting.getMessage(), "This is a newly created greeting");
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "buildersClientDataDataProvider")
  public void testCreate(RestClient restClient, RestliRequestOptions requestOptions) throws RemoteInvocationException
  {
    Greeting greeting = new Greeting();
    greeting.setMessage("Hello there!");
    greeting.setTone(Tone.FRIENDLY);

    final GreetingsBuilders builders = new GreetingsBuilders(requestOptions);

    Request<EmptyRecord> createRequest = builders.create().input(greeting).build();
    Response<EmptyRecord> response = restClient.sendRequest(createRequest).getResponse();
    Assert.assertNull(response.getHeader(RestConstants.HEADER_CONTENT_TYPE));
    @SuppressWarnings("unchecked")
    CreateResponse<Long> createResponse = (CreateResponse<Long>)response.getEntity();
    long id = createResponse.getId();
    @SuppressWarnings("deprecation")
    String stringId = response.getId();
    Assert.assertEquals(id, Long.parseLong(stringId));

    Request<Greeting> getRequest = builders.get().id(id).build();
    Response<Greeting> getResponse = restClient.sendRequest(getRequest).getResponse();
    Greeting responseGreeting = getResponse.getEntity();

    Assert.assertEquals(responseGreeting.getMessage(), greeting.getMessage());
    Assert.assertEquals(responseGreeting.getTone(), greeting.getTone());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "buildersClientDataDataProvider")
  public void testCreateId(RestClient restClient, RestliRequestOptions requestOptions) throws RemoteInvocationException
  {
    Greeting greeting = new Greeting();
    greeting.setMessage("Hello there!");
    greeting.setTone(Tone.FRIENDLY);

    final GreetingsRequestBuilders builders = new GreetingsRequestBuilders(requestOptions);

    CreateIdRequest<Long, Greeting> createRequest = builders.create().input(greeting).build();
    Response<IdResponse<Long>> response = restClient.sendRequest(createRequest).getResponse();
    Assert.assertNull(response.getHeader(RestConstants.HEADER_CONTENT_TYPE));
    @SuppressWarnings("unchecked")
    long id = response.getEntity().getId();
    @SuppressWarnings("deprecation")
    String stringId = response.getId();
    Assert.assertEquals(id, Long.parseLong(stringId));

    Request<Greeting> getRequest = builders.get().id(id).build();
    Response<Greeting> getResponse = restClient.sendRequest(getRequest).getResponse();
    Greeting responseGreeting = getResponse.getEntity();

    Assert.assertEquals(responseGreeting.getMessage(), greeting.getMessage());
    Assert.assertEquals(responseGreeting.getTone(), greeting.getTone());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientDataDataProvider")
  public void testUpdate(RestClient restClient, RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException, CloneNotSupportedException
  {
    // GET
    Request<Greeting> request = builders.get().id(1L).build();
    Response<Greeting> greetingResponse1 = restClient.sendRequest(request).getResponse();

    String response1 = greetingResponse1.getEntity().getMessage();
    Assert.assertNotNull(response1);

    // POST
    Greeting greeting = new Greeting(greetingResponse1.getEntity().data().copy());
    greeting.setMessage(response1 + "Again");

    Request<EmptyRecord> writeRequest = builders.update().id(1L).input(greeting).build();
    Response<EmptyRecord> updateResponse = restClient.sendRequest(writeRequest).getResponse();
    Assert.assertNull(updateResponse.getHeader(RestConstants.HEADER_CONTENT_TYPE));

    // GET again, to verify that our POST worked.
    Request<Greeting> request2 = builders.get().id(1L).build();
    Response<Greeting> greetingResponse2 = restClient.sendRequest(request2).getResponse();

    String response2 = greetingResponse2.getEntity().getMessage();
    Assert.assertEquals(response2, response1 + "Again");
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testPostsWithCharset(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<Greeting> request = builders.<Greeting>action("SomeAction").id(1L)
      .setActionParam("A", 1)
      .setActionParam("B", "")
      .setActionParam("C", new TransferOwnershipRequest())
      .setActionParam("D", new TransferOwnershipRequest())
      .setActionParam("E", 3)
      .setHeader("Content-Type", "application/json; charset=UTF-8")
      .build();

    Response<Greeting> response = getClient().sendRequest(request).getResponse();

    Greeting actionGreeting = response.getEntity();
    Assert.assertEquals(actionGreeting.getMessage(), "This is a newly created greeting");

    Greeting createGreeting = new Greeting();
    createGreeting.setMessage("Hello there!");
    createGreeting.setTone(Tone.FRIENDLY);

    Request<EmptyRecord> createRequest = builders
      .create()
      .input(createGreeting)
      .setHeader("Content-Type", "application/json; charset=UTF-8")
      .build();
    Response<EmptyRecord> emptyRecordResponse = getClient().sendRequest(createRequest).getResponse();
    Assert.assertNull(emptyRecordResponse.getHeader(RestConstants.HEADER_CONTENT_TYPE));
  }

  @SuppressWarnings("deprecation")
  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientDataDataProvider")
  public Object[][] clientDataDataProvider()
  {
    return new Object[][]
      {
        { new RestClient(getDefaultTransportClient(), URI_PREFIX), new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders()) }, // default client
        { new RestClient(getDefaultTransportClient(), URI_PREFIX), new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) }, // default client
        { new RestClient(getDefaultTransportClient(), URI_PREFIX), new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders()) }, // default client
        { new RestClient(getDefaultTransportClient(), URI_PREFIX), new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) }, // default client
        { new RestClient(getDefaultTransportClient(), URI_PREFIX, RestClient.ContentType.JSON, ACCEPT_TYPES), new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders()) },
        { new RestClient(getDefaultTransportClient(), URI_PREFIX, RestClient.ContentType.JSON, ACCEPT_TYPES), new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
        { new RestClient(getDefaultTransportClient(), URI_PREFIX, RestClient.ContentType.JSON, ACCEPT_TYPES), new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders()) },
        { new RestClient(getDefaultTransportClient(), URI_PREFIX, RestClient.ContentType.JSON, ACCEPT_TYPES), new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
        { new RestClient(getDefaultTransportClient(), URI_PREFIX, RestClient.ContentType.PSON, ACCEPT_TYPES), new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders()) },
        { new RestClient(getDefaultTransportClient(), URI_PREFIX, RestClient.ContentType.PSON, ACCEPT_TYPES), new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
        { new RestClient(getDefaultTransportClient(), URI_PREFIX, RestClient.ContentType.PSON, ACCEPT_TYPES), new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders()) },
        { new RestClient(getDefaultTransportClient(), URI_PREFIX, RestClient.ContentType.PSON, ACCEPT_TYPES), new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) }
      };
  }

  @SuppressWarnings("deprecation")
  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientDataBatchDataProvider")
  public Object[][] clientDataBatchDataProvider()
  {
    return new Object[][]
      {
        { new RestClient(getDefaultTransportClient(), URI_PREFIX), RestliRequestOptions.DEFAULT_OPTIONS },
        { new RestClient(getDefaultTransportClient(), URI_PREFIX), TestConstants.FORCE_USE_NEXT_OPTIONS },
        { new RestClient(getDefaultTransportClient(), URI_PREFIX, RestClient.ContentType.JSON, ACCEPT_TYPES), RestliRequestOptions.DEFAULT_OPTIONS },
        { new RestClient(getDefaultTransportClient(), URI_PREFIX, RestClient.ContentType.JSON, ACCEPT_TYPES), TestConstants.FORCE_USE_NEXT_OPTIONS },
        { new RestClient(getDefaultTransportClient(), URI_PREFIX, RestClient.ContentType.PSON, ACCEPT_TYPES), RestliRequestOptions.DEFAULT_OPTIONS },
        { new RestClient(getDefaultTransportClient(), URI_PREFIX, RestClient.ContentType.PSON, ACCEPT_TYPES), TestConstants.FORCE_USE_NEXT_OPTIONS },
      };
  }

  @SuppressWarnings("deprecation")
  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "buildersClientDataDataProvider")
  public Object[][] buildersClientDataDataProvider()
  {
    return new Object[][]
      {
        { new RestClient(getDefaultTransportClient(), URI_PREFIX), RestliRequestOptions.DEFAULT_OPTIONS }, // default client
        { new RestClient(getDefaultTransportClient(), URI_PREFIX), TestConstants.FORCE_USE_NEXT_OPTIONS }, // default client
        { new RestClient(getDefaultTransportClient(), URI_PREFIX, RestClient.ContentType.JSON, ACCEPT_TYPES), RestliRequestOptions.DEFAULT_OPTIONS },
        { new RestClient(getDefaultTransportClient(), URI_PREFIX, RestClient.ContentType.JSON, ACCEPT_TYPES), TestConstants.FORCE_USE_NEXT_OPTIONS },
        { new RestClient(getDefaultTransportClient(), URI_PREFIX, RestClient.ContentType.PSON, ACCEPT_TYPES), RestliRequestOptions.DEFAULT_OPTIONS },
        { new RestClient(getDefaultTransportClient(), URI_PREFIX, RestClient.ContentType.PSON, ACCEPT_TYPES), TestConstants.FORCE_USE_NEXT_OPTIONS }
      };
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  private static Object[][] requestBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) }
    };
  }
}
