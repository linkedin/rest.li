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


import com.linkedin.common.callback.Callback;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.template.BooleanArray;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.StringMap;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.restli.client.BatchGetEntityRequestBuilder;
import com.linkedin.restli.client.BatchGetRequestBuilder;
import com.linkedin.restli.client.CreateIdRequest;
import com.linkedin.restli.client.CreateIdRequestBuilder;
import com.linkedin.restli.client.OptionsRequestBuilder;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.client.response.CreateResponse;
import com.linkedin.restli.client.util.PatchGenerator;
import com.linkedin.restli.common.BatchCreateIdResponse;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.CreateStatus;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.IdResponse;
import com.linkedin.restli.common.Link;
import com.linkedin.restli.common.OptionsResponse;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.examples.greetings.api.Empty;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.SearchMetadata;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.ActionsBuilders;
import com.linkedin.restli.examples.greetings.client.ActionsRequestBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsCallbackBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsCallbackRequestBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsPromiseBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsPromiseCtxBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsPromiseCtxRequestBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsPromiseRequestBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsRequestBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsTaskBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsTaskRequestBuilders;
import com.linkedin.restli.examples.groups.api.TransferOwnershipRequest;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.testutils.URIDetails;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.test.util.BatchCreateHelper;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Base class for greetings testers. This is an integration test, requiring that you have
 * the greetings server running.
 *
 * @author Eran Leshem
 */
public class TestGreetingsClient extends RestLiIntegrationTest
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
  public void testActionRequestOptionsPropagation(RootBuilderWrapper<Long, Greeting> builders)
  {
    Request<Integer> request = builders.<Integer>action("Purge").build();
    Assert.assertEquals(request.getRequestOptions(), builders.getRequestOptions());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testGetRequestOptionsPropagation(RootBuilderWrapper<Long, Greeting> builders)
  {
    Request<Greeting> request = builders.get().id(1L).build();
    Assert.assertEquals(request.getRequestOptions(), builders.getRequestOptions());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testFinderRequestOptionsPropagation(RootBuilderWrapper<Long, Greeting> builders)
  {
    Request<CollectionResponse<Greeting>> findRequest =
        builders.findBy("Search").setQueryParam("tone", Tone.FRIENDLY).build();
    Assert.assertEquals(findRequest.getRequestOptions(), builders.getRequestOptions());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testIntAction(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<Integer> request = builders.<Integer>action("Purge").build();
    ResponseFuture<Integer> responseFuture = getClient().sendRequest(request);
    Assert.assertEquals(responseFuture.getResponse().getStatus(), 200);
    Assert.assertEquals(responseFuture.getResponse().getEntity().intValue(), 100);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testRecordAction(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<Greeting> request = builders.<Greeting>action("SomeAction")
            .id(1L)
            .setActionParam("A", 1)
            .setActionParam("B", "")
            .setActionParam("C", new TransferOwnershipRequest())
            .setActionParam("D", new TransferOwnershipRequest())
            .setActionParam("E", 3)
            .build();
    ResponseFuture<Greeting> responseFuture = getClient().sendRequest(request);
    Assert.assertEquals(responseFuture.getResponse().getStatus(), 200);
    Assert.assertNotNull(responseFuture.getResponse().getEntity());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testRecordActionVoidReturn(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<Void> requestVoid = builders.<Void>action("AnotherAction")
        .setActionParam("bitfield", new BooleanArray())
        .setActionParam("request", new TransferOwnershipRequest())
        .setActionParam("someString", new String(""))
        .setActionParam("stringMap", new StringMap())
        .build();
    ResponseFuture<Void> responseFutureVoid = getClient().sendRequest(requestVoid);
    Assert.assertEquals(responseFutureVoid.getResponse().getStatus(), 200);
    Assert.assertNull(responseFutureVoid.getResponse().getEntity());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testUpdateToneAction(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<Greeting> request = builders.<Greeting>action("UpdateTone")
            .id(1L)
            .setActionParam("NewTone", Tone.SINCERE)
            .setActionParam("DelOld", false)
            .build();
    ResponseFuture<Greeting> responseFuture = getClient().sendRequest(request);
    Assert.assertEquals(responseFuture.getResponse().getStatus(), 200);
    final Greeting newGreeting = responseFuture.getResponse().getEntity();
    Assert.assertNotNull(newGreeting);
    Assert.assertEquals(newGreeting.getId().longValue(), 1L);
    Assert.assertEquals(newGreeting.getTone(), Tone.SINCERE);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  //test update on retrieved entity
  public void testUpdate(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException, CloneNotSupportedException,
          URISyntaxException
  {
    // GET
    Request<Greeting> request = builders.get().id(1L).build();
    ResponseFuture<Greeting> future = getClient().sendRequest(request);
    Response<Greeting> greetingResponse = future.getResponse();

    String response1 = greetingResponse.getEntity().getMessage();
    Assert.assertNotNull(response1);

    // POST
    Greeting greeting = new Greeting(greetingResponse.getEntity().data().copy());
    greeting.setMessage(response1 + "Again");

    Request<EmptyRecord> writeRequest = builders.update().id(1L).input(greeting).build();
    getClient().sendRequest(writeRequest).getResponse();

    // GET again, to verify that our POST worked.
    Request<Greeting> request2 = builders.get().id(1L).build();
    ResponseFuture<Greeting> future2 = getClient().sendRequest(request2);
    String response2 = future2.getResponse().getEntity().getMessage();

    Assert.assertEquals(response2, response1 + "Again");
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testPartialUpdate(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException, CloneNotSupportedException, URISyntaxException
  {
    // GET
    Request<Greeting> request = builders.get().id(1L).build();
    ResponseFuture<Greeting> future = getClient().sendRequest(request);
    Response<Greeting> greetingResponse = future.getResponse();

    Greeting original = greetingResponse.getEntity();

    // POST
    Greeting greeting = new Greeting(original.data().copy());
    greeting.setMessage(original.getMessage() + " Again");

    PatchRequest<Greeting> patch = PatchGenerator.diff(original, greeting);

    Request<EmptyRecord> writeRequest = builders.partialUpdate().id(1L).input(patch).build();
    int status = getClient().sendRequest(writeRequest).getResponse().getStatus();
    Assert.assertEquals(status, HttpStatus.S_204_NO_CONTENT.getCode());

    // GET again, to verify that our POST worked.
    Request<Greeting> request2 = builders.get().id(1L).build();
    ResponseFuture<Greeting> future2 = getClient().sendRequest(request2);
    String response2 = future2.getResponse().getEntity().getMessage();

    Assert.assertEquals(response2, greeting.getMessage());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  //test cookbook example from quickstart wiki
  public void testCookbook(RootBuilderWrapper<Long, Greeting> builders) throws Exception
  {
    // GET
    Request<Greeting> request = builders.get().id(1L).build();
    ResponseFuture<Greeting> future = getClient().sendRequest(request);
    Response<Greeting> greetingResponse = future.getResponse();

    Assert.assertNotNull(greetingResponse.getEntity().getMessage());

    // POST
    Greeting greeting = new Greeting(greetingResponse.getEntity().data().copy());
    final String NEW_MESSAGE = "This is a new message!";
    greeting.setMessage(NEW_MESSAGE);

    Request<EmptyRecord> writeRequest = builders.update().id(1L).input(greeting).build();
    getClient().sendRequest(writeRequest).getResponse();

    // GET again, to verify that our POST worked.
    Request<Greeting> request2 = builders.get().id(1L).build();
    ResponseFuture<Greeting> future2 = getClient().sendRequest(request2);
    greetingResponse = future2.get();

    Assert.assertEquals(greetingResponse.getEntity().getMessage(), NEW_MESSAGE);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  public void testOldCookbookInBatch(RestliRequestOptions requestOptions) throws Exception
  {
    final GreetingsBuilders builders = new GreetingsBuilders(requestOptions);

    // GET
    final BatchGetRequestBuilder<Long, Greeting> batchGetBuilder = builders.batchGet();
    Request<BatchResponse<Greeting>> request = batchGetBuilder.ids(1L).build();
    ResponseFuture<BatchResponse<Greeting>> future = getClient().sendRequest(request);
    Response<BatchResponse<Greeting>> greetingResponse = future.getResponse();

    // PUT
    Greeting greeting = new Greeting(greetingResponse.getEntity().getResults().get("1").data().copy());
    greeting.setMessage("This is a new message!");

    Request<BatchKVResponse<Long, UpdateStatus>> writeRequest = builders.batchUpdate().input(1L, greeting).build();
    getClient().sendRequest(writeRequest).getResponse();

    // GET again, to verify that our POST worked.
    Request<BatchResponse<Greeting>> request2 = builders.batchGet().ids(1L).build();
    ResponseFuture<BatchResponse<Greeting>> future2 = getClient().sendRequest(request2);
    greetingResponse = future2.get();

    Greeting repeatedGreeting = new Greeting();
    repeatedGreeting.setMessage("Hello Hello");
    repeatedGreeting.setTone(Tone.SINCERE);
    Request<CollectionResponse<CreateStatus>> request3 = builders.batchCreate().inputs(Arrays.asList(repeatedGreeting, repeatedGreeting)).build();
    CollectionResponse<CreateStatus> statuses = getClient().sendRequest(request3).getResponse().getEntity();
    for (CreateStatus status : statuses.getElements())
    {
      Assert.assertEquals(status.getStatus().intValue(), HttpStatus.S_201_CREATED.getCode());
      @SuppressWarnings("deprecation")
      String id = status.getId();
      Assert.assertNotNull(id);
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  public void testNewCookbookInBatch(RestliRequestOptions requestOptions) throws Exception
  {
    final GreetingsRequestBuilders builders = new GreetingsRequestBuilders(requestOptions);

    // GET
    final BatchGetEntityRequestBuilder<Long, Greeting> batchGetBuilder = builders.batchGet();
    Request<BatchKVResponse<Long, EntityResponse<Greeting>>> request = batchGetBuilder.ids(1L).build();
    ResponseFuture<BatchKVResponse<Long, EntityResponse<Greeting>>> future = getClient().sendRequest(request);
    Response<BatchKVResponse<Long, EntityResponse<Greeting>>> greetingResponse = future.getResponse();

    // PUT
    Greeting greeting = new Greeting(greetingResponse.getEntity().getResults().get(1L).getEntity().data().copy());
    greeting.setMessage("This is a new message!");

    Request<BatchKVResponse<Long, UpdateStatus>> writeRequest = builders.batchUpdate().input(1L, greeting).build();
    getClient().sendRequest(writeRequest).getResponse();

    // GET again, to verify that our POST worked.
    Request<BatchKVResponse<Long, EntityResponse<Greeting>>> request2 = builders.batchGet().ids(1L).build();
    ResponseFuture<BatchKVResponse<Long, EntityResponse<Greeting>>> future2 = getClient().sendRequest(request2);
    greetingResponse = future2.get();

    Greeting repeatedGreeting = new Greeting();
    repeatedGreeting.setMessage("Hello Hello");
    repeatedGreeting.setTone(Tone.SINCERE);
    List<Greeting> entities = Arrays.asList(repeatedGreeting, repeatedGreeting);

    Request<BatchCreateIdResponse<Long>> request3 = builders.batchCreate().inputs(entities).build();
    BatchCreateIdResponse<Long> statuses = getClient().sendRequest(request3).getResponse().getEntity();
    for (CreateIdStatus<Long> status : statuses.getElements())
    {
      Assert.assertEquals(status.getStatus().intValue(), HttpStatus.S_201_CREATED.getCode());
      @SuppressWarnings("deprecation")
      String id = status.getId();
      Assert.assertEquals(status.getKey().longValue(), Long.parseLong(id));
      Assert.assertNotNull(status.getKey());
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testSearch(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> findRequest = builders.findBy("Search").setQueryParam("tone", Tone.FRIENDLY).build();
    List<Greeting> greetings = getClient().sendRequest(findRequest).getResponse().getEntity().getElements();
    for (Greeting g : greetings)
    {
      Assert.assertEquals(g.getTone(), Tone.FRIENDLY);
      Assert.assertNotNull(g.getMessage());
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderWithResourceNameDataProvider")
  public void testSearchWithPostFilter(RootBuilderWrapper<Long, Greeting> builders, String resourceName,
      ProtocolVersion protocolVersion) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> findRequest = builders.findBy("SearchWithPostFilter").paginate(0, 5).build();
    CollectionResponse<Greeting> entity = getClient().sendRequest(findRequest).getResponse().getEntity();
    CollectionMetadata paging = entity.getPaging();
    Assert.assertEquals(paging.getStart().intValue(), 0);
    Assert.assertEquals(paging.getCount().intValue(), 5);
    Assert.assertEquals(entity.getElements().size(), 4); // expected to be 4 instead of 5 because of post filter

    // to accommodate post filtering, even though 4 are returned, next page should be 5-10.
    Link next = paging.getLinks().get(0);
    Assert.assertEquals(next.getRel(), "next");

    //Query parameter order is non deterministic
    //"/" + resourceName + "?count=5&start=5&q=searchWithPostFilter";
    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("count", "5");
    queryParamsMap.put("start", "5");
    queryParamsMap.put("q", "searchWithPostFilter");

    final URIDetails uriDetails = new URIDetails(protocolVersion, "/" + resourceName, null, queryParamsMap, null);
    URIDetails.testUriGeneration(next.getHref(), uriDetails);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void TestPagination(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> findRequest = builders.findBy("SearchWithPostFilter").paginateStart(1).build();
    CollectionResponse<Greeting> entity = getClient().sendRequest(findRequest).getResponse().getEntity();
    CollectionMetadata paging = entity.getPaging();
    Assert.assertEquals(paging.getStart().intValue(), 1);
    Assert.assertEquals(paging.getCount().intValue(), 10);
    Assert.assertEquals(entity.getElements().size(), 9); // expected to be 9 instead of 10 because of post filter

    findRequest = builders.findBy("SearchWithPostFilter").paginateCount(5).build();
    entity = getClient().sendRequest(findRequest).getResponse().getEntity();
    paging = entity.getPaging();
    Assert.assertEquals(paging.getStart().intValue(), 0);
    Assert.assertEquals(paging.getCount().intValue(), 5);
    Assert.assertEquals(entity.getElements().size(), 4); // expected to be 4 instead of 5 because of post filter
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testSearchWithTones(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> req =
        builders.findBy("SearchWithTones").setQueryParam("tones", Arrays.asList(Tone.SINCERE, Tone.INSULTING)).build();
    ResponseFuture<CollectionResponse<Greeting>> future = getClient().sendRequest(req);
    Response<CollectionResponse<Greeting>> response = future.getResponse();
    List<Greeting> greetings = response.getEntity().getElements();
    for (Greeting greeting : greetings)
    {
      Assert.assertTrue(greeting.hasTone());
      Tone tone = greeting.getTone();
      Assert.assertTrue(Tone.SINCERE.equals(tone) || Tone.INSULTING.equals(tone));
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testSearchFacets(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> req = builders.findBy("SearchWithFacets").setQueryParam("tone", Tone.SINCERE).build();
    ResponseFuture<CollectionResponse<Greeting>> future = getClient().sendRequest(req);
    Response<CollectionResponse<Greeting>> response = future.getResponse();
    SearchMetadata metadata = new SearchMetadata(response.getEntity().getMetadataRaw());
    Assert.assertTrue(metadata.getFacets().size() > 0);
    // "randomly" generated data is guaranteed to have positive number of each tone
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "oldBatchGetRequestBuilderDataProvider")
  public void testBatchGet(BatchGetRequestBuilder<Long, Greeting> builder,
                           List<Long> ids,
                           int expectedSuccessSize) throws RemoteInvocationException
  {
    Request<BatchResponse<Greeting>> request = builder.ids(ids).fields(Greeting.fields().id(), Greeting.fields().message()).build();
    BatchResponse<Greeting> response = getClient().sendRequest(request).getResponse().getEntity();
    Assert.assertEquals(response.getResults().size(), expectedSuccessSize);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "oldBatchGetRequestBuilderDataProvider")
  public void testBatchGetKV(BatchGetRequestBuilder<Long, Greeting> builder,
                             List<Long> ids,
                             int expectedSuccessSize) throws RemoteInvocationException
  {
    Request<BatchKVResponse<Long, Greeting>> request = builder.ids(ids).fields(Greeting.fields().id(), Greeting.fields().message()).buildKV();
    BatchKVResponse<Long, Greeting> response = getClient().sendRequest(request).getResponse().getEntity();
    Assert.assertEquals(response.getResults().size(), expectedSuccessSize);

    for (Map.Entry<Long, Greeting> entry : response.getResults().entrySet())
    {
      Assert.assertEquals(entry.getKey(), entry.getValue().getId());
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "newBatchGetRequestBuilderDataProvider")
  public void testBatchGetEntity(BatchGetEntityRequestBuilder<Long, Greeting> builder,
                                 List<Long> ids,
                                 int expectedSuccessSize) throws RemoteInvocationException
  {
    Request<BatchKVResponse<Long, EntityResponse<Greeting>>> request = builder.ids(ids).fields(Greeting.fields().id(), Greeting.fields().message()).build();
    BatchKVResponse<Long, EntityResponse<Greeting>> response = getClient().sendRequest(request).getResponse().getEntity();
    Assert.assertEquals(response.getResults().size() - response.getErrors().size(), expectedSuccessSize);

    for (Map.Entry<Long, EntityResponse<Greeting>> entry : response.getResults().entrySet())
    {
      if (entry.getValue().hasEntry())
      {
        Assert.assertEquals(entry.getKey(), entry.getValue().getEntity().getId());
      }
    }
  }

  /**
   * Generates a test Greeting
   *
   * @param message the message for the Greeting
   * @param tone the tone for the Greeting
   *
   * @return the newly generated Greeting
   */
  private Greeting generateTestGreeting(String message, Tone tone)
  {
    return new Greeting().setMessage(message).setTone(tone);
  }

  /*
   HELPER METHODS FOR TESTING BATCH OPERATIONS

   These helper methods do all operations serially. This is because we want to restrict out batch operation only to the
   method we are current testing. E.g. testBatchCreate only uses the batchCreate method and uses non batch operations
   to fetch and delete data.
   */

  /**
   * Deletes data on the server and verifies that the HTTP response is correct
   *
   * @param ids the ids for the Greetings to delete
   * @throws RemoteInvocationException
   */
  private void deleteAndVerifyBatchTestDataSerially(RootBuilderWrapper<Long, Greeting> builders, List<Long> ids)
      throws RemoteInvocationException
  {
    for (Long id: ids)
    {
      Request<EmptyRecord> request = builders.delete().id(id).build();
      ResponseFuture<EmptyRecord> future = getClient().sendRequest(request);
      Response<EmptyRecord> response = future.getResponse();

      Assert.assertEquals(response.getStatus(), HttpStatus.S_204_NO_CONTENT.getCode());
    }
  }

  /**
   * Fetches Greetings from the server serially.
   *
   * @param idsToGet the ids for the Greetings to fetch
   *
   * @return the fetched Greetings
   * @throws RemoteInvocationException
   */
  private List<Greeting> getBatchTestDataSerially(RootBuilderWrapper<Long, Greeting> builders, List<Long> idsToGet)
      throws RemoteInvocationException
  {
    List<Greeting> fetchedGreetings = new ArrayList<Greeting>();
    for (int i = 0; i < idsToGet.size(); i++)
    {
      try
      {
        Long id = idsToGet.get(i);
        Request<Greeting> request = builders.get().id(id).build();
        ResponseFuture<Greeting> future = getClient().sendRequest(request);
        Response<Greeting> greetingResponse = future.getResponse();

        Greeting fetchedGreeting = greetingResponse.getEntity();

        fetchedGreetings.add(fetchedGreeting);
      }
      catch (RestLiResponseException ex)
      {
        if (ex.getStatus() == HttpStatus.S_404_NOT_FOUND.getCode())
        {
          fetchedGreetings.add(null);
        }
        else
        {
          throw ex;
        }
      }
    }
    return fetchedGreetings;
  }

  /**
   * Fetches Greetings from the server and verifies that the fetched Greetings are as expected
   *
   * @param idsToGet the ids for the Greetings to get
   * @param expectedGreetings the Greetings we expect the server to return
   * @param fieldsToIgnore a list of fields to ignore while verifying the fetched Greetings. If the list is null or
   *                       empty then the Greeting.equals method will be used. Otherwise, the datamaps will be checked
   *                       for equality on a per field basis, ignoring the fields in fieldsToIgnore
   *
   * @throws RemoteInvocationException
   */
  private void getAndVerifyBatchTestDataSerially(RootBuilderWrapper<Long, Greeting> builders,
                                                 List<Long> idsToGet,
                                                 List<Greeting> expectedGreetings,
                                                 List<String> fieldsToIgnore)
      throws RemoteInvocationException
  {
    List<Greeting> fetchedGreetings = getBatchTestDataSerially(builders, idsToGet);
    Assert.assertEquals(fetchedGreetings.size(), expectedGreetings.size());
    for (int i = 0; i < fetchedGreetings.size(); i++)
    {
      Greeting fetchedGreeting = fetchedGreetings.get(i);
      Greeting expectedGreeting = expectedGreetings.get(i);

      // Make sure the types of the fetched Greeting match the types in the schema. This happens as a side effect of the
      // validate method
      ValidateDataAgainstSchema.validate(fetchedGreeting.data(), fetchedGreeting.schema(), new ValidationOptions());

      if (fieldsToIgnore == null || fieldsToIgnore.isEmpty())
      {
        Assert.assertEquals(fetchedGreeting, expectedGreeting);
      }
      else
      {
        DataMap fetchedDataMap = fetchedGreeting.data();
        DataMap expectedDataMap = expectedGreeting.data();
        for (String field: fetchedDataMap.keySet())
        {
          if (!fieldsToIgnore.contains(field))
          {
            Assert.assertEquals(fetchedDataMap.get(field), expectedDataMap.get(field));
          }
        }
      }
    }
  }

  /**
   * Generates batch test data.
   *
   * @param numItems the number of items in the batch
   * @param baseMessage the base message of the Greeting. Each Greeting will have a message baseMessage + item #
   * @param tone the tone for the Greeting to create
   *
   * @return the batch data to send to the server
   */
  private List<Greeting> generateBatchTestData(int numItems, String baseMessage, Tone tone)
  {
    List<Greeting> greetings = new ArrayList<Greeting>();
    for (int i = 0; i < numItems; i++)
    {
      greetings.add(generateTestGreeting(baseMessage + " " + i, tone));
    }
    return greetings;
  }

  /**
   * Creates batch data.
   *
   * @param greetings the greetings that we want to create
   *
   * @return the ids of the created Greetings
   * @throws RemoteInvocationException
   */
  private List<Long> createBatchTestDataSerially(RootBuilderWrapper<Long, Greeting> builders, List<Greeting> greetings)
      throws RemoteInvocationException
  {
    List<Long> createdIds = new ArrayList<Long>();

    for (Greeting greeting: greetings)
    {
      RootBuilderWrapper.MethodBuilderWrapper<Long, Greeting, EmptyRecord> createBuilder = builders.create();
      Long createdId;
      if (createBuilder.isRestLi2Builder())
      {
        Object objBuilder = createBuilder.getBuilder();
        @SuppressWarnings("unchecked")
        CreateIdRequestBuilder<Long, Greeting> createIdRequestBuilder = (CreateIdRequestBuilder<Long, Greeting>) objBuilder;
        CreateIdRequest<Long, Greeting> request = createIdRequestBuilder.input(greeting).build();
        Response<IdResponse<Long>> response = getClient().sendRequest(request).getResponse();
        createdId = response.getEntity().getId();
      }
      else
      {
        Request<EmptyRecord> request = createBuilder.input(greeting).build();
        Response<EmptyRecord> response = getClient().sendRequest(request).getResponse();
        @SuppressWarnings("unchecked")
        CreateResponse<Long> createResponse = (CreateResponse<Long>)response.getEntity();
        createdId = createResponse.getId();
      }
      createdIds.add(createdId);
    }

    return createdIds;
  }

  /**
   * Adds ids (returned from the server) to the locally generated Greetings.
   * It modifies the greetings parameter in place to add the id
   *
   * @param ids the ids returned from the server
   * @param greetings the Greetings we generated locally.
   */
  private void addIdsToGeneratedGreetings(List<Long> ids, List<Greeting> greetings)
  {
    Assert.assertEquals(ids.size(), greetings.size());
    for (int i = 0; i < greetings.size(); i++)
    {
      greetings.get(i).setId(ids.get(i));
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testBatchCreate(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    List<Greeting> greetings = generateBatchTestData(3, "BatchCreate", Tone.FRIENDLY);

    List<CreateIdStatus<Long>> statuses = BatchCreateHelper.batchCreate(getClient(), builders, greetings);
    List<Long> createdIds = new ArrayList<Long>(statuses.size());

    for (CreateIdStatus<Long> status: statuses)
    {
      Assert.assertEquals(status.getStatus().intValue(), HttpStatus.S_201_CREATED.getCode());
      @SuppressWarnings("deprecation")
      String id = status.getId();
      Assert.assertEquals(status.getKey().longValue(), Long.parseLong(id));
      createdIds.add(status.getKey());
    }

    getAndVerifyBatchTestDataSerially(builders, createdIds, greetings, Arrays.asList("id"));
    deleteAndVerifyBatchTestDataSerially(builders, createdIds);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testBatchDelete(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    List<Greeting> greetings = generateBatchTestData(3, "BatchDelete", Tone.FRIENDLY);
    @SuppressWarnings("unchecked")
    List<Long> createdIds = createBatchTestDataSerially(builders, greetings);

    // Batch delete the created Greetings
    Request<BatchKVResponse<Long, UpdateStatus>> deleteRequest =
        builders.batchDelete().ids(createdIds).build();
    BatchKVResponse<Long, UpdateStatus> responses = getClient().sendRequest(deleteRequest).getResponse().getEntity();

    Assert.assertEquals(responses.getResults().size(), createdIds.size()); // we deleted the Messages we created
    for (UpdateStatus status: responses.getResults().values())
    {
      Assert.assertEquals(status.getStatus().intValue(), HttpStatus.S_204_NO_CONTENT.getCode());
    }

    List<Greeting> deletedGreetings = getBatchTestDataSerially(builders, createdIds);
    Assert.assertEquals(deletedGreetings.size(), createdIds.size());
    for (Greeting greeting: deletedGreetings)
    {
      Assert.assertNull(greeting);
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testBatchUpdate(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException, CloneNotSupportedException
  {
    List<Greeting> greetings = generateBatchTestData(3, "BatchUpdate", Tone.FRIENDLY);
    @SuppressWarnings("unchecked")
    List<Long> createdIds = createBatchTestDataSerially(builders, greetings);
    addIdsToGeneratedGreetings(createdIds, greetings);

    // Update the created greetings
    List<Greeting> updatedGreetings = new ArrayList<Greeting>();
    Map<Long, Greeting> updateGreetingsRequestMap = new HashMap<Long, Greeting>();

    for (Greeting greeting: greetings)
    {
      Greeting updatedGreeting = new Greeting(greeting.data().copy());
      updatedGreeting.setMessage(updatedGreeting.getMessage().toUpperCase());
      updatedGreeting.setTone(Tone.SINCERE);
      updatedGreetings.add(updatedGreeting);
      updateGreetingsRequestMap.put(updatedGreeting.getId(), updatedGreeting);
    }

    // Batch update
    Request<BatchKVResponse<Long, UpdateStatus>> batchUpdateRequest =
        builders.batchUpdate().inputs(updateGreetingsRequestMap).build();
    Map<Long, UpdateStatus> results =
        getClient().sendRequest(batchUpdateRequest).getResponse().getEntity().getResults();

    Assert.assertEquals(results.size(), updatedGreetings.size());
    for (UpdateStatus status: results.values())
    {
      Assert.assertEquals(status.getStatus().intValue(), HttpStatus.S_204_NO_CONTENT.getCode());
    }

    getAndVerifyBatchTestDataSerially(builders, createdIds, updatedGreetings, null);
    deleteAndVerifyBatchTestDataSerially(builders, createdIds);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testBatchPartialUpdate(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException, CloneNotSupportedException
  {
    List<Greeting> greetings = generateBatchTestData(3, "BatchPatch", Tone.FRIENDLY);
    @SuppressWarnings("unchecked")
    List<Long> createdIds = createBatchTestDataSerially(builders, greetings);
    addIdsToGeneratedGreetings(createdIds, greetings);

    // Patch the created Greetings
    Map<Long, PatchRequest<Greeting>> patchedGreetingsDiffs = new HashMap<Long, PatchRequest<Greeting>>();
    List<Greeting> patchedGreetings = new ArrayList<Greeting>();

    for (Greeting greeting: greetings)
    {
      Greeting patchedGreeting = new Greeting(greeting.data().copy());
      patchedGreeting.setMessage(patchedGreeting.getMessage().toUpperCase());

      PatchRequest<Greeting> patchRequest = PatchGenerator.diff(greeting, patchedGreeting);
      patchedGreetingsDiffs.put(patchedGreeting.getId(), patchRequest);
      patchedGreetings.add(patchedGreeting);
    }

    // Batch patch
    Request<BatchKVResponse<Long, UpdateStatus>> batchUpdateRequest =
        builders.batchPartialUpdate().patchInputs(patchedGreetingsDiffs).build();
    Map<Long, UpdateStatus> results =
        getClient().sendRequest(batchUpdateRequest).getResponse().getEntity().getResults();

    Assert.assertEquals(results.size(), patchedGreetingsDiffs.size());
    for (UpdateStatus status: results.values())
    {
      Assert.assertEquals(status.getStatus().intValue(), HttpStatus.S_204_NO_CONTENT.getCode());
    }

    getAndVerifyBatchTestDataSerially(builders, createdIds, patchedGreetings, null);
    deleteAndVerifyBatchTestDataSerially(builders, createdIds);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testGetAll(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    List<Greeting> greetings = generateBatchTestData(3, "GetAll", Tone.FRIENDLY);
    @SuppressWarnings("unchecked")
    List<Long> createdIds = createBatchTestDataSerially(builders, greetings);
    addIdsToGeneratedGreetings(createdIds, greetings);

    Request<CollectionResponse<Greeting>> getAllRequest = builders.getAll().build();
    List<Greeting> getAllReturnedGreetings = getClient().
        sendRequest(getAllRequest).getResponse().getEntity().getElements();

    // the current implementation of getAll should return all those Greetings with the String "GetAll"
    // in them. Thus, fetchedGreetings and getAllGreetings should be the same
    Assert.assertEquals(getAllReturnedGreetings.size(), greetings.size());
    for (int i = 0; i < greetings.size(); i++)
    {
      Greeting getAllReturnedGreeting = getAllReturnedGreetings.get(i);
      Greeting greeting = greetings.get(i);
      // Make sure the types of the fetched Greeting match the types in the schema. This happens as a side effect of the
      // validate method
      // This is why we can't do Assert.assertEquals(getAllReturnedGreetings, greetings) directly
      ValidateDataAgainstSchema.validate(getAllReturnedGreeting.data(),
                                         getAllReturnedGreeting.schema(),
                                         new ValidationOptions());
      Assert.assertEquals(getAllReturnedGreeting, greeting);
    }

    deleteAndVerifyBatchTestDataSerially(builders, createdIds);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testMalformedPagination(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    expectPaginationError("-1", builders);
    expectPaginationError("abc", builders);
  }

  private void expectPaginationError(String count, RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    try
    {
      Request<CollectionResponse<Greeting>> request = builders.findBy("Search").name("search").setParam("count", count).build();
      getClient().sendRequest(request).getResponse();
      Assert.fail("expected exception");
    }
    catch (RestException e)
    {
      Assert.assertEquals(e.getResponse().getStatus(), HttpStatus.S_400_BAD_REQUEST.getCode(), "expected 400 status");
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testException(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    try
    {
      Request<Void> request = builders.<Void>action("ExceptionTest").build();
      getClient().sendRequest(request).getResponse().getEntity();
      Assert.fail("expected exception");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
      Assert.assertTrue(e.getServiceErrorMessage().contains("Test Exception"));
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void test404(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<Greeting> request = builders.get().id(999L).build();
    ResponseFuture<Greeting> future = getClient().sendRequest(request);
    try
    {
      future.getResponse();
      Assert.fail("expected 404");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_404_NOT_FOUND.getCode());
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void test404Callback(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException, InterruptedException
  {
    final CountDownLatch countDownLatch = new CountDownLatch(1);
    final Request<Greeting> request = builders.get().id(999L).build();
    getClient().sendRequest(request, new Callback<Response<Greeting>>()
    {
      @Override
      public void onError(Throwable e)
      {
        Assert.assertNotNull(e, "We should have gotten an error!");
        countDownLatch.countDown();
      }

      @Override
      public void onSuccess(Response<Greeting> result)
      {
        Assert.fail("We should not have received a success!");
        countDownLatch.countDown();
      }
    });
    countDownLatch.await();
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderWithResourceNameDataProvider")
  public void testOptions(RootBuilderWrapper<Long, Greeting> builders, String resourceName, ProtocolVersion protocolVersion)
      throws RemoteInvocationException, URISyntaxException, IOException
  {
    Request<OptionsResponse> optionsRequest = builders.options().build();
    OptionsResponse optionsResponse = getClient().sendRequest(optionsRequest).getResponse().getEntity();
    Map<String, ResourceSchema> resources = optionsResponse.getResourceSchemas();
    Assert.assertEquals(resources.size(), 1);
    ResourceSchema resourceSchema = resources.get("com.linkedin.restli.examples.greetings.client." + resourceName);
    // sanity check the resource schema
    Assert.assertEquals(resourceSchema.getName(), resourceName);
    Assert.assertTrue(resourceSchema.hasCollection());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "optionsData")
  public void testModelsForOptionsRequest(OptionsRequestBuilder request, NamedDataSchema[] schemas)
      throws RemoteInvocationException, URISyntaxException, IOException
  {
    Request<OptionsResponse> optionsRequest = request.build();
    OptionsResponse optionsResponse = getClient().sendRequest(optionsRequest).getResponse().getEntity();
    Map<String, DataSchema> rawDataSchemas = optionsResponse.getDataSchemas();

    for(NamedDataSchema dataSchema: schemas)
    {
      DataSchema optionsDataSchema = rawDataSchemas.get(dataSchema.getFullName());
      Assert.assertEquals(optionsDataSchema, dataSchema);
    }
  }

  @SuppressWarnings("deprecation")
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testCreateWithNullId(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    Request<EmptyRecord> request = builders.create()
                                           .input(new Greeting().setId(1L).setMessage("foo"))
                                           .setQueryParam("isNullId", true)
                                           .build();
    Response<EmptyRecord> response = getClient().sendRequest(request).getResponse();
    Assert.assertNull(response.getId());
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  private static Object[][] requestOptionsDataProvider()
  {
    return new Object[][] {
      { RestliRequestOptions.DEFAULT_OPTIONS },
      { TestConstants.FORCE_USE_NEXT_OPTIONS }
    };
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "oldBatchGetRequestBuilderDataProvider")
  private Object[][] oldBatchGetRequestBuilderDataProvider()
  {
    return new Object[][] {
      { new GreetingsBuilders().batchGet(), Arrays.asList(1000L, 2000L), 0 },
      { new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS).batchGet(), Arrays.asList(1000L, 2000L), 0 },
      { new GreetingsBuilders().batchGet(), Arrays.asList(1L, 2L, 3L, 4L), 4 },
      { new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS).batchGet(), Arrays.asList(1L, 2L, 3L, 4L), 4 },
      { new GreetingsPromiseBuilders().batchGet(), Arrays.asList(1000L, 2000L), 0 },
      { new GreetingsPromiseBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS).batchGet(), Arrays.asList(1000L, 2000L), 0 },
      { new GreetingsPromiseBuilders().batchGet(), Arrays.asList(1L, 2L, 3L, 4L), 4 },
      { new GreetingsPromiseBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS).batchGet(), Arrays.asList(1L, 2L, 3L, 4L), 4 },
      { new GreetingsCallbackBuilders().batchGet(), Arrays.asList(1000L, 2000L), 0 },
      { new GreetingsCallbackBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS).batchGet(), Arrays.asList(1000L, 2000L), 0 },
      { new GreetingsCallbackBuilders().batchGet(), Arrays.asList(1L, 2L, 3L, 4L), 4 },
      { new GreetingsCallbackBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS).batchGet(), Arrays.asList(1L, 2L, 3L, 4L), 4 },
      { new GreetingsPromiseCtxBuilders().batchGet(), Arrays.asList(1000L, 2000L), 0 },
      { new GreetingsPromiseCtxBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS).batchGet(), Arrays.asList(1000L, 2000L), 0 },
      { new GreetingsPromiseCtxBuilders().batchGet(), Arrays.asList(1L, 2L, 3L, 4L), 4 },
      { new GreetingsPromiseCtxBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS).batchGet(), Arrays.asList(1L, 2L, 3L, 4L), 4 },
      { new GreetingsTaskBuilders().batchGet(), Arrays.asList(1000L, 2000L), 0 },
      { new GreetingsTaskBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS).batchGet(), Arrays.asList(1000L, 2000L), 0 },
      { new GreetingsTaskBuilders().batchGet(), Arrays.asList(1L, 2L, 3L, 4L), 4 },
      { new GreetingsTaskBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS).batchGet(), Arrays.asList(1L, 2L, 3L, 4L), 4 }
    };
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "newBatchGetRequestBuilderDataProvider")
  private static Object[][] newBatchGetRequestBuilderDataProvider()
  {
    return new Object[][] {
      { new GreetingsRequestBuilders().batchGet(), Arrays.asList(1000L, 2000L), 0 },
      { new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS).batchGet(), Arrays.asList(1000L, 2000L), 0 },
      { new GreetingsRequestBuilders().batchGet(), Arrays.asList(1L, 2L, 3L, 4L), 4 },
      { new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS).batchGet(), Arrays.asList(1L, 2L, 3L, 4L), 4 },
      { new GreetingsPromiseRequestBuilders().batchGet(), Arrays.asList(1000L, 2000L), 0 },
      { new GreetingsPromiseRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS).batchGet(), Arrays.asList(1000L, 2000L), 0 },
      { new GreetingsPromiseRequestBuilders().batchGet(), Arrays.asList(1L, 2L, 3L, 4L), 4 },
      { new GreetingsPromiseRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS).batchGet(), Arrays.asList(1L, 2L, 3L, 4L), 4 },
      { new GreetingsCallbackRequestBuilders().batchGet(), Arrays.asList(1000L, 2000L), 0 },
      { new GreetingsCallbackRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS).batchGet(), Arrays.asList(1000L, 2000L), 0 },
      { new GreetingsCallbackRequestBuilders().batchGet(), Arrays.asList(1L, 2L, 3L, 4L), 4 },
      { new GreetingsCallbackRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS).batchGet(), Arrays.asList(1L, 2L, 3L, 4L), 4 },
      { new GreetingsPromiseCtxRequestBuilders().batchGet(), Arrays.asList(1000L, 2000L), 0 },
      { new GreetingsPromiseCtxRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS).batchGet(), Arrays.asList(1000L, 2000L), 0 },
      { new GreetingsPromiseCtxRequestBuilders().batchGet(), Arrays.asList(1L, 2L, 3L, 4L), 4 },
      { new GreetingsPromiseCtxRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS).batchGet(), Arrays.asList(1L, 2L, 3L, 4L), 4 },
      { new GreetingsTaskRequestBuilders().batchGet(), Arrays.asList(1000L, 2000L), 0 },
      { new GreetingsTaskRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS).batchGet(), Arrays.asList(1000L, 2000L), 0 },
      { new GreetingsTaskRequestBuilders().batchGet(), Arrays.asList(1L, 2L, 3L, 4L), 4 },
      { new GreetingsTaskRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS).batchGet(), Arrays.asList(1L, 2L, 3L, 4L), 4 }
    };
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  private static Object[][] requestBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseRequestBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackRequestBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxRequestBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskRequestBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) }
    };
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderWithResourceNameDataProvider")
  private static Object[][] requestBuilderWithResourceNameDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders()), "greetings" ,
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion()},
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)), "greetings",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion()},
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders()), "greetings",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion()},
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)), "greetings",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion()},
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseBuilders()), "greetingsPromise",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion()},
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)), "greetingsPromise",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion()},
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseRequestBuilders()), "greetingsPromise",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion()},
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)), "greetingsPromise",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion()},
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackBuilders()), "greetingsCallback",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion()},
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)), "greetingsCallback",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion()},
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackRequestBuilders()), "greetingsCallback",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion()},
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)), "greetingsCallback",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion()},
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxBuilders()), "greetingsPromiseCtx",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion()},
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)), "greetingsPromiseCtx",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion()},
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxRequestBuilders()), "greetingsPromiseCtx",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion()},
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)), "greetingsPromiseCtx",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion()},
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskBuilders()), "greetingsTask",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion()},
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)), "greetingsTask",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion()},
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskRequestBuilders()), "greetingsTask",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion()},
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)), "greetingsTask",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion()},
    };
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "optionsData")
  private static Object[][] optionsData()
  {
    return new Object[][]
      {
        new Object[]
          {
            new GreetingsBuilders().options(),
            new NamedDataSchema[]
              {
                new Greeting().schema(),
                new TransferOwnershipRequest().schema(),
                new SearchMetadata().schema(),
                new Empty().schema(),
                (NamedDataSchema)DataTemplateUtil.getSchema(Tone.class)
              }
          },
        new Object[]
          {
            new GreetingsRequestBuilders().options(),
            new NamedDataSchema[]
              {
                new Greeting().schema(),
                new TransferOwnershipRequest().schema(),
                new SearchMetadata().schema(),
                new Empty().schema(),
                (NamedDataSchema)DataTemplateUtil.getSchema(Tone.class)
              }
          },
        new Object[]
          {
            new ActionsBuilders().options(),
            new NamedDataSchema[]
              {
                new Message().schema(),
                (NamedDataSchema)DataTemplateUtil.getSchema(Tone.class)
              }
          },
        new Object[]
          {
            new ActionsRequestBuilders().options(),
            new NamedDataSchema[]
              {
                new Message().schema(),
                (NamedDataSchema)DataTemplateUtil.getSchema(Tone.class)
              }
          }
      };
  }
}
