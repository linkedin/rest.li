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


import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.OptionsRequestBuilder;
import com.linkedin.restli.client.ProtocolVersionOption;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.client.RestliRequestOptionsBuilder;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.client.util.PatchGenerator;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CreateStatus;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.Link;
import com.linkedin.restli.common.OptionsResponse;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.ResourceSpecImpl;
import com.linkedin.restli.common.RestConstants;
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
import com.linkedin.restli.internal.client.EntityResponseDecoder;
import com.linkedin.restli.internal.client.RestResponseDecoder;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  private static final Client CLIENT = new TransportClientAdapter(new HttpClientFactory().getClient(Collections.<String, String>emptyMap()));
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

  @Test(dataProvider = "requestBuilderWithOptionsDataProvider")
  public void testActionRequestOptionsPropagation(RootBuilderWrapper<Long, Greeting> builders, RestliRequestOptions options)
  {
    Request<Integer> request = builders.<Integer>action("Purge").build();
    Assert.assertEquals(request.getRequestOptions(), options);
  }

  @Test(dataProvider = "requestBuilderWithOptionsDataProvider")
  public void testGetRequestOptionsPropagation(RootBuilderWrapper<Long, Greeting> builders, RestliRequestOptions options)
  {
    Request<Greeting> request = builders.get().id(1L).build();
    Assert.assertEquals(request.getRequestOptions(), options);
  }

  @Test(dataProvider = "requestBuilderWithOptionsDataProvider")
  public void testFinderRequestOptionsPropagation(RootBuilderWrapper<Long, Greeting> builders, RestliRequestOptions options)
  {
    Request<CollectionResponse<Greeting>> findRequest =
        builders.findBy("Search").setQueryParam("tone", Tone.FRIENDLY).build();
    Assert.assertEquals(findRequest.getRequestOptions(), options);
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testIntAction(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<Integer> request = builders.<Integer>action("Purge").build();
    ResponseFuture<Integer> responseFuture = REST_CLIENT.sendRequest(request);
    Assert.assertEquals(responseFuture.getResponse().getStatus(), 200);
    Assert.assertEquals(responseFuture.getResponse().getEntity().intValue(), 100);
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testDeprecatedRequestCreation()
      throws URISyntaxException, RemoteInvocationException
  {
    URI uri = new URI("greetings/1");
    ResourceMethod method = ResourceMethod.GET;
    RecordTemplate inputRecord = null;
    Map<String, String> headers = Collections.emptyMap();
    RestResponseDecoder<Greeting> decoder = new EntityResponseDecoder<Greeting>(Greeting.class);
    ResourceSpec resourceSpec = new ResourceSpecImpl(EnumSet.of(method), null, null, Long.class, Greeting.class, Collections.<String, Object>emptyMap());
    Request<Greeting> request = new Request<Greeting>(uri, method, inputRecord, headers, decoder, resourceSpec);
    ResponseFuture<Greeting> responseFuture = REST_CLIENT.sendRequest(request);
    Greeting entity = responseFuture.getResponse().getEntity();
    Assert.assertEquals(entity.getId(), new Long(1L));
    Assert.assertEquals(responseFuture.getResponse().getHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION),
                        AllProtocolVersions.BASELINE_PROTOCOL_VERSION.toString());
  }

  @Test(dataProvider = "requestBuilderDataProvider")
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
    ResponseFuture<Greeting> responseFuture = REST_CLIENT.sendRequest(request);
    Assert.assertEquals(responseFuture.getResponse().getStatus(), 200);
    Assert.assertNotNull(responseFuture.getResponse().getEntity());
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testUpdateToneAction(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<Greeting> request = builders.<Greeting>action("UpdateTone")
            .id(1L)
            .setActionParam("NewTone", Tone.SINCERE)
            .setActionParam("DelOld", false)
            .build();
    ResponseFuture<Greeting> responseFuture = REST_CLIENT.sendRequest(request);
    Assert.assertEquals(responseFuture.getResponse().getStatus(), 200);
    final Greeting newGreeting = responseFuture.getResponse().getEntity();
    Assert.assertNotNull(newGreeting);
    Assert.assertEquals(newGreeting.getId().longValue(), 1L);
    Assert.assertEquals(newGreeting.getTone(), Tone.SINCERE);
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  //test update on retrieved entity
  public void testUpdate(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException, CloneNotSupportedException,
          URISyntaxException
  {
    // GET
    Request<Greeting> request = builders.get().id(1L).build();
    ResponseFuture<Greeting> future = REST_CLIENT.sendRequest(request);
    Response<Greeting> greetingResponse = future.getResponse();

    String response1 = greetingResponse.getEntity().getMessage();
    Assert.assertNotNull(response1);

    // POST
    Greeting greeting = new Greeting(greetingResponse.getEntity().data().copy());
    greeting.setMessage(response1 + "Again");

    Request<EmptyRecord> writeRequest = builders.update().id(1L).input(greeting).build();
    REST_CLIENT.sendRequest(writeRequest).getResponse();

    // GET again, to verify that our POST worked.
    Request<Greeting> request2 = builders.get().id(1L).build();
    ResponseFuture<Greeting> future2 = REST_CLIENT.sendRequest(request2);
    String response2 = future2.getResponse().getEntity().getMessage();

    Assert.assertEquals(response2, response1 + "Again");
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testPartialUpdate(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException, CloneNotSupportedException, URISyntaxException
  {
    // GET
    Request<Greeting> request = builders.get().id(1L).build();
    ResponseFuture<Greeting> future = REST_CLIENT.sendRequest(request);
    Response<Greeting> greetingResponse = future.getResponse();

    Greeting original = greetingResponse.getEntity();

    // POST
    Greeting greeting = new Greeting(original.data().copy());
    greeting.setMessage(original.getMessage() + " Again");

    PatchRequest<Greeting> patch = PatchGenerator.diff(original, greeting);

    Request<EmptyRecord> writeRequest = builders.partialUpdate().id(1L).input(patch).build();
    int status = REST_CLIENT.sendRequest(writeRequest).getResponse().getStatus();
    Assert.assertEquals(status, HttpStatus.S_204_NO_CONTENT.getCode());

    // GET again, to verify that our POST worked.
    Request<Greeting> request2 = builders.get().id(1L).build();
    ResponseFuture<Greeting> future2 = REST_CLIENT.sendRequest(request2);
    String response2 = future2.getResponse().getEntity().getMessage();

    Assert.assertEquals(response2, greeting.getMessage());
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  //test cookbook example from quickstart wiki
  public void testCookbook(RootBuilderWrapper<Long, Greeting> builders) throws Exception
  {
    Client r2Client = new TransportClientAdapter(new HttpClientFactory().getClient(Collections.<String, String>emptyMap()));
    RestClient restClient = new RestClient(r2Client, "http://localhost:1338/");

    // GET
    Request<Greeting> request = builders.get().id(1L).build();
    ResponseFuture<Greeting> future = restClient.sendRequest(request);
    Response<Greeting> greetingResponse = future.getResponse();

    Assert.assertNotNull(greetingResponse.getEntity().getMessage());

    // POST
    Greeting greeting = new Greeting(greetingResponse.getEntity().data().copy());
    final String NEW_MESSAGE = "This is a new message!";
    greeting.setMessage(NEW_MESSAGE);

    Request<EmptyRecord> writeRequest = builders.update().id(1L).input(greeting).build();
    restClient.sendRequest(writeRequest).getResponse();

    // GET again, to verify that our POST worked.
    Request<Greeting> request2 = builders.get().id(1L).build();
    ResponseFuture<Greeting> future2 = restClient.sendRequest(request2);
    greetingResponse = future2.get();

    Assert.assertEquals(greetingResponse.getEntity().getMessage(), NEW_MESSAGE);

    // shut down client
    FutureCallback<None> futureCallback = new FutureCallback<None>();
    r2Client.shutdown(futureCallback);
    futureCallback.get();
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testCookbookInBatch(RootBuilderWrapper<Long, Greeting> builders) throws Exception
  {
    // GET
    Request<BatchResponse<Greeting>> request = builders.batchGet().ids(1L).build();
    ResponseFuture<BatchResponse<Greeting>> future = REST_CLIENT.sendRequest(request);
    Response<BatchResponse<Greeting>> greetingResponse = future.getResponse();

    // POST
    Greeting greeting = new Greeting(greetingResponse.getEntity().getResults().get("1").data().copy());
    greeting.setMessage("This is a new message!");

    Request<BatchKVResponse<Long, UpdateStatus>> writeRequest = builders.batchUpdate().input(1L, greeting).build();
    REST_CLIENT.sendRequest(writeRequest).getResponse();

    // GET again, to verify that our POST worked.
    Request<BatchResponse<Greeting>> request2 = builders.batchGet().ids(1L).build();
    ResponseFuture<BatchResponse<Greeting>> future2 = REST_CLIENT.sendRequest(request2);
    greetingResponse = future2.get();

    Greeting repeatedGreeting = new Greeting();
    repeatedGreeting.setMessage("Hello Hello");
    repeatedGreeting.setTone(Tone.SINCERE);
    Request<CollectionResponse<CreateStatus>> request3 = builders.batchCreate().inputs(Arrays.asList(repeatedGreeting, repeatedGreeting)).build();
    CollectionResponse<CreateStatus> statuses = REST_CLIENT.sendRequest(request3).getResponse().getEntity();
    for (CreateStatus status : statuses.getElements())
    {
      Assert.assertEquals(status.getStatus().intValue(), HttpStatus.S_201_CREATED.getCode());
      Assert.assertNotNull(status.getId());
    }
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testSearch(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> findRequest = builders.findBy("Search").setQueryParam("tone", Tone.FRIENDLY).build();
    List<Greeting> greetings = REST_CLIENT.sendRequest(findRequest).getResponse().getEntity().getElements();
    for (Greeting g : greetings)
    {
      Assert.assertEquals(g.getTone(), Tone.FRIENDLY);
      Assert.assertNotNull(g.getMessage());
    }
  }

  @Test(dataProvider = "requestBuilderWithResourceNameDataProvider")
  public void testSearchWithPostFilter(RootBuilderWrapper<Long, Greeting> builders, String resourceName) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> findRequest = builders.findBy("SearchWithPostFilter").paginate(0, 5).build();
    CollectionResponse<Greeting> entity = REST_CLIENT.sendRequest(findRequest).getResponse().getEntity();
    CollectionMetadata paging = entity.getPaging();
    Assert.assertEquals(paging.getStart().intValue(), 0);
    Assert.assertEquals(paging.getCount().intValue(), 5);
    Assert.assertEquals(entity.getElements().size(), 4); // expected to be 4 instead of 5 because of post filter

    // to accommodate post filtering, even though 4 are returned, next page should be 5-10.
    Link next = paging.getLinks().get(0);
    Assert.assertEquals(next.getRel(), "next");
    Assert.assertEquals(next.getHref(), "/" + resourceName + "?count=5&start=5&q=searchWithPostFilter");
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void TestPagination(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> findRequest = builders.findBy("SearchWithPostFilter").paginateStart(1).build();
    CollectionResponse<Greeting> entity = REST_CLIENT.sendRequest(findRequest).getResponse().getEntity();
    CollectionMetadata paging = entity.getPaging();
    Assert.assertEquals(paging.getStart().intValue(), 1);
    Assert.assertEquals(paging.getCount().intValue(), 10);
    Assert.assertEquals(entity.getElements().size(), 9); // expected to be 9 instead of 10 because of post filter

    findRequest = builders.findBy("SearchWithPostFilter").paginateCount(5).build();
    entity = REST_CLIENT.sendRequest(findRequest).getResponse().getEntity();
    paging = entity.getPaging();
    Assert.assertEquals(paging.getStart().intValue(), 0);
    Assert.assertEquals(paging.getCount().intValue(), 5);
    Assert.assertEquals(entity.getElements().size(), 4); // expected to be 4 instead of 5 because of post filter
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testSearchWithTones(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> req =
        builders.findBy("SearchWithTones").setQueryParam("tones", Arrays.asList(Tone.SINCERE, Tone.INSULTING)).build();
    ResponseFuture<CollectionResponse<Greeting>> future = REST_CLIENT.sendRequest(req);
    Response<CollectionResponse<Greeting>> response = future.getResponse();
    List<Greeting> greetings = response.getEntity().getElements();
    for (Greeting greeting : greetings)
    {
      Assert.assertTrue(greeting.hasTone());
      Tone tone = greeting.getTone();
      Assert.assertTrue(Tone.SINCERE.equals(tone) || Tone.INSULTING.equals(tone));
    }
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testSearchFacets(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> req = builders.findBy("SearchWithFacets").setQueryParam("tone", Tone.SINCERE).build();
    ResponseFuture<CollectionResponse<Greeting>> future = REST_CLIENT.sendRequest(req);
    Response<CollectionResponse<Greeting>> response = future.getResponse();
    SearchMetadata metadata = new SearchMetadata(response.getEntity().getMetadataRaw());
    Assert.assertTrue(metadata.getFacets().size() > 0);
    // "randomly" generated data is guaranteed to have positive number of each tone
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testEmptyBatchGetWithProjection(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<BatchResponse<Greeting>> request = builders.batchGet().ids(1000L, 2000L).fields(Greeting.fields().message()).build();
    BatchResponse<Greeting> response = REST_CLIENT.sendRequest(request).getResponse().getEntity();
    Assert.assertEquals(response.getResults().size(), 0);
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testBatchGetUsingCollection(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    List<Long> ids = Arrays.asList(1L, 2L, 3L, 4L);
    Request<BatchResponse<Greeting>> request = builders.batchGet().ids(ids).fields(Greeting.fields().id(), Greeting.fields().message()).build();
    BatchResponse<Greeting> response = REST_CLIENT.sendRequest(request).getResponse().getEntity();
    Assert.assertEquals(response.getResults().size(), ids.size());
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testBatchGetUsingCollectionKV(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    List<Long> ids = Arrays.asList(1L, 2L, 3L, 4L);
    Request<BatchKVResponse<Long, Greeting>> request = builders.batchGet().ids(ids).fields(Greeting.fields().id(), Greeting.fields().message()).buildKV();
    BatchKVResponse<Long, Greeting> response = REST_CLIENT.sendRequest(request).getResponse().getEntity();
    Assert.assertEquals(response.getResults().size(), ids.size());
    for (Map.Entry<Long, Greeting> entry : response.getResults().entrySet())
    {
      Assert.assertEquals(entry.getKey(), entry.getValue().getId());
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
      ResponseFuture<EmptyRecord> future = REST_CLIENT.sendRequest(request);
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
        ResponseFuture<Greeting> future = REST_CLIENT.sendRequest(request);
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
      Request<EmptyRecord> request = builders.create().input(greeting).build();
      String createdId = REST_CLIENT.sendRequest(request).getResponse().getId();
      createdIds.add(Long.parseLong(createdId));
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

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testBatchCreate(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    List<Greeting> greetings = generateBatchTestData(3, "BatchCreate", Tone.FRIENDLY);

    Request<CollectionResponse<CreateStatus>> batchRequest = builders.batchCreate().inputs(greetings).build();
    CollectionResponse<CreateStatus> results = REST_CLIENT.sendRequest(batchRequest).getResponse().getEntity();
    List<Long> createdIds = new ArrayList<Long>();

    Assert.assertEquals(results.getElements().size(), greetings.size());
    for (CreateStatus status: results.getElements())
    {
      Assert.assertEquals(status.getStatus().intValue(), HttpStatus.S_201_CREATED.getCode());
      createdIds.add(Long.parseLong(status.getId()));
    }

    getAndVerifyBatchTestDataSerially(builders, createdIds, greetings, Arrays.asList("id"));
    deleteAndVerifyBatchTestDataSerially(builders, createdIds);
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testBatchDelete(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    List<Greeting> greetings = generateBatchTestData(3, "BatchDelete", Tone.FRIENDLY);
    List<Long> createdIds = createBatchTestDataSerially(builders, greetings);

    // Batch delete the created Greetings
    Request<BatchKVResponse<Long, UpdateStatus>> deleteRequest =
        builders.batchDelete().ids(createdIds).build();
    BatchKVResponse<Long, UpdateStatus> responses = REST_CLIENT.sendRequest(deleteRequest).getResponse().getEntity();

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

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testBatchUpdate(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException, CloneNotSupportedException
  {
    List<Greeting> greetings = generateBatchTestData(3, "BatchUpdate", Tone.FRIENDLY);
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
        REST_CLIENT.sendRequest(batchUpdateRequest).getResponse().getEntity().getResults();

    Assert.assertEquals(results.size(), updatedGreetings.size());
    for (UpdateStatus status: results.values())
    {
      Assert.assertEquals(status.getStatus().intValue(), HttpStatus.S_204_NO_CONTENT.getCode());
    }

    getAndVerifyBatchTestDataSerially(builders, createdIds, updatedGreetings, null);
    deleteAndVerifyBatchTestDataSerially(builders, createdIds);
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testBatchPartialUpdate(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException, CloneNotSupportedException
  {
    List<Greeting> greetings = generateBatchTestData(3, "BatchPatch", Tone.FRIENDLY);
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
        REST_CLIENT.sendRequest(batchUpdateRequest).getResponse().getEntity().getResults();

    Assert.assertEquals(results.size(), patchedGreetingsDiffs.size());
    for (UpdateStatus status: results.values())
    {
      Assert.assertEquals(status.getStatus().intValue(), HttpStatus.S_204_NO_CONTENT.getCode());
    }

    getAndVerifyBatchTestDataSerially(builders, createdIds, patchedGreetings, null);
    deleteAndVerifyBatchTestDataSerially(builders, createdIds);
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testGetAll(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    List<Greeting> greetings = generateBatchTestData(3, "GetAll", Tone.FRIENDLY);
    List<Long> createdIds = createBatchTestDataSerially(builders, greetings);
    addIdsToGeneratedGreetings(createdIds, greetings);

    Request<CollectionResponse<Greeting>> getAllRequest = builders.getAll().build();
    List<Greeting> getAllReturnedGreetings = REST_CLIENT.
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

  @Test(dataProvider = "requestBuilderDataProvider")
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
      REST_CLIENT.sendRequest(request).getResponse();
      Assert.fail("expected exception");
    }
    catch (RestException e)
    {
      Assert.assertEquals(e.getResponse().getStatus(), HttpStatus.S_400_BAD_REQUEST.getCode(), "expected 400 status");
    }
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testException(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    try
    {
      Request<Void> request = builders.<Void>action("ExceptionTest").build();
      REST_CLIENT.sendRequest(request).getResponse().getEntity();
      Assert.fail("expected exception");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
      Assert.assertTrue(e.getServiceErrorMessage().contains("Test Exception"));
    }
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void test404(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<Greeting> request = builders.get().id(999L).build();
    ResponseFuture<Greeting> future = REST_CLIENT.sendRequest(request);
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

  @Test(dataProvider = "requestBuilderWithResourceNameDataProvider")
  public void testOptions(RootBuilderWrapper<Long, Greeting> builders, String resourceName)
      throws RemoteInvocationException, URISyntaxException, IOException
  {
    Request<OptionsResponse> optionsRequest = builders.options().build();
    OptionsResponse optionsResponse = REST_CLIENT.sendRequest(optionsRequest).getResponse().getEntity();
    Map<String, ResourceSchema> resources = optionsResponse.getResourceSchemas();
    Assert.assertEquals(resources.size(), 1);
    ResourceSchema resourceSchema = resources.get("com.linkedin.restli.examples.greetings.client." + resourceName);
    // sanity check the resource schema
    Assert.assertEquals(resourceSchema.getName(), resourceName);
    Assert.assertTrue(resourceSchema.hasCollection());
  }

  @DataProvider
  protected Object[][] requestBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseRequestBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackRequestBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxRequestBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskRequestBuilders()) }
    };
  }

  @DataProvider
  protected Object[][] requestBuilderWithOptionsDataProvider()
  {
    final RestliRequestOptions customRequestOptions =
      new RestliRequestOptionsBuilder().setProtocolVersionOption(ProtocolVersionOption.FORCE_USE_NEXT).build();
    return new Object[][] {
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders()), RestliRequestOptions.DEFAULT_OPTIONS },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders(customRequestOptions)), customRequestOptions },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders()), RestliRequestOptions.DEFAULT_OPTIONS },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(customRequestOptions)), customRequestOptions },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseBuilders()), RestliRequestOptions.DEFAULT_OPTIONS },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseBuilders(customRequestOptions)), customRequestOptions },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseRequestBuilders()), RestliRequestOptions.DEFAULT_OPTIONS },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseRequestBuilders(customRequestOptions)), customRequestOptions },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackBuilders()), RestliRequestOptions.DEFAULT_OPTIONS },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackBuilders(customRequestOptions)), customRequestOptions },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackRequestBuilders()), RestliRequestOptions.DEFAULT_OPTIONS },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackRequestBuilders(customRequestOptions)), customRequestOptions },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxBuilders()), RestliRequestOptions.DEFAULT_OPTIONS },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxBuilders(customRequestOptions)), customRequestOptions },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxRequestBuilders()), RestliRequestOptions.DEFAULT_OPTIONS },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxRequestBuilders(customRequestOptions)), customRequestOptions },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskBuilders()), RestliRequestOptions.DEFAULT_OPTIONS },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskBuilders(customRequestOptions)), customRequestOptions },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskRequestBuilders()), RestliRequestOptions.DEFAULT_OPTIONS },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskRequestBuilders(customRequestOptions)), customRequestOptions }
    };
  }

  @DataProvider
  protected Object[][] requestBuilderWithResourceNameDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders()), "greetings" },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders()), "greetings" },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseBuilders()), "greetingsPromise" },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseRequestBuilders()), "greetingsPromise" },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackBuilders()), "greetingsCallback" },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackRequestBuilders()), "greetingsCallback" },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxBuilders()), "greetingsPromiseCtx" },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxRequestBuilders()), "greetingsPromiseCtx" },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskBuilders()), "greetingsTask" },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskRequestBuilders()), "greetingsTask" },
    };
  }

  @DataProvider
  public Object[][] optionsData()
  {
    return new Object[][] {
        new Object[] { new GreetingsBuilders().options(), new NamedDataSchema[] {
            new Greeting().schema(),
            new TransferOwnershipRequest().schema(),
            new SearchMetadata().schema(),
            new Empty().schema(),
            (NamedDataSchema)DataTemplateUtil.getSchema(Tone.class)
        }},
        new Object[] { new GreetingsRequestBuilders().options(), new NamedDataSchema[] {
          new Greeting().schema(),
          new TransferOwnershipRequest().schema(),
          new SearchMetadata().schema(),
          new Empty().schema(),
          (NamedDataSchema)DataTemplateUtil.getSchema(Tone.class)
        }},
        new Object[] { new ActionsBuilders().options(), new NamedDataSchema[] {
            new Message().schema(),
            (NamedDataSchema)DataTemplateUtil.getSchema(Tone.class)
        }},
        new Object[] { new ActionsRequestBuilders().options(), new NamedDataSchema[] {
          new Message().schema(),
          (NamedDataSchema)DataTemplateUtil.getSchema(Tone.class)
        }}
    };
  }

  @Test(dataProvider = "optionsData")
  public void testModelsForOptionsRequest(OptionsRequestBuilder request, NamedDataSchema[] schemas)
      throws RemoteInvocationException, URISyntaxException, IOException
  {
    Request<OptionsResponse> optionsRequest = request.build();
    OptionsResponse optionsResponse = REST_CLIENT.sendRequest(optionsRequest).getResponse().getEntity();
    Map<String, DataSchema> rawDataSchemas = optionsResponse.getDataSchemas();

    for(NamedDataSchema dataSchema: schemas)
    {
      DataSchema optionsDataSchema = rawDataSchemas.get(dataSchema.getFullName());
      Assert.assertEquals(optionsDataSchema, dataSchema);
    }
  }
}
