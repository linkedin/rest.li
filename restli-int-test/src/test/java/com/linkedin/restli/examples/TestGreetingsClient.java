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

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.ActionRequest;
import com.linkedin.restli.client.BatchCreateRequest;
import com.linkedin.restli.client.BatchGetRequest;
import com.linkedin.restli.client.BatchUpdateRequest;
import com.linkedin.restli.client.FindRequest;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.client.util.PatchGenerator;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CreateStatus;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.Link;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.SearchMetadata;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.GreetingsBuilders;
import com.linkedin.restli.examples.groups.api.TransferOwnershipRequest;

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

  private final GreetingsBuilders GREETINGS_BUILDERS;

  public TestGreetingsClient(String resName)
  {
    GREETINGS_BUILDERS = new GreetingsBuilders("greetings");
  }

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
  public void testIntAction() throws RemoteInvocationException
  {
    ActionRequest<Integer> request = GREETINGS_BUILDERS.actionPurge().build();
    ResponseFuture<Integer> responseFuture = REST_CLIENT.sendRequest(request);
    Assert.assertEquals(responseFuture.getResponse().getStatus(), 200);
    Assert.assertEquals(responseFuture.getResponse().getEntity().intValue(), 100);
  }

  @Test
  public void testRecordAction() throws RemoteInvocationException
  {
    ActionRequest<Greeting> request = GREETINGS_BUILDERS.actionSomeAction()
            .id(1L)
            .paramA(1)
            .paramB("")
            .paramC(new TransferOwnershipRequest())
            .paramD(new TransferOwnershipRequest())
            .paramE(3)
            .build();
    ResponseFuture<Greeting> responseFuture = REST_CLIENT.sendRequest(request);
    Assert.assertEquals(responseFuture.getResponse().getStatus(), 200);
    Assert.assertNotNull(responseFuture.getResponse().getEntity());
  }

  @Test
  public void testUpdateToneAction() throws RemoteInvocationException
  {
    ActionRequest<Greeting> request = GREETINGS_BUILDERS.actionUpdateTone()
            .id(1L)
            .paramNewTone(Tone.SINCERE)
            .paramDelOld(false)
            .build();
    ResponseFuture<Greeting> responseFuture = REST_CLIENT.sendRequest(request);
    Assert.assertEquals(responseFuture.getResponse().getStatus(), 200);
    final Greeting newGreeting = responseFuture.getResponse().getEntity();
    Assert.assertNotNull(newGreeting);
    Assert.assertEquals(newGreeting.getId().longValue(), 1L);
    Assert.assertEquals(newGreeting.getTone(), Tone.SINCERE);
  }

  @Test
  //test update on retrieved entity
  public void testUpdate() throws RemoteInvocationException, CloneNotSupportedException,
          URISyntaxException
  {
    // GET
    Request<Greeting> request = GREETINGS_BUILDERS.get().id(1L).build();
    ResponseFuture<Greeting> future = REST_CLIENT.sendRequest(request);
    Response<Greeting> greetingResponse = future.getResponse();

    String response1 = greetingResponse.getEntity().getMessage();
    Assert.assertNotNull(response1);

    // POST
    Greeting greeting = new Greeting(greetingResponse.getEntity().data().copy());
    greeting.setMessage(response1 + "Again");

    Request<EmptyRecord> writeRequest = GREETINGS_BUILDERS.update().id(1L).input(greeting).build();
    REST_CLIENT.sendRequest(writeRequest).getResponse();

    // GET again, to verify that our POST worked.
    Request<Greeting> request2 = GREETINGS_BUILDERS.get().id(1L).build();
    ResponseFuture<Greeting> future2 = REST_CLIENT.sendRequest(request2);
    String response2 = future2.getResponse().getEntity().getMessage();

    Assert.assertEquals(response2, response1 + "Again");
  }

  @Test
  public void testPartialUpdate() throws RemoteInvocationException, CloneNotSupportedException, URISyntaxException
  {
    // GET
    Request<Greeting> request = GREETINGS_BUILDERS.get().id(1L).build();
    ResponseFuture<Greeting> future = REST_CLIENT.sendRequest(request);
    Response<Greeting> greetingResponse = future.getResponse();

    Greeting original = greetingResponse.getEntity();

    // POST
    Greeting greeting = new Greeting(original.data().copy());
    greeting.setMessage(original.getMessage() + " Again");

    PatchRequest<Greeting> patch = PatchGenerator.diff(original, greeting);

    Request<EmptyRecord> writeRequest = GREETINGS_BUILDERS.partialUpdate().id(1L).input(patch).build();
    int status = REST_CLIENT.sendRequest(writeRequest).getResponse().getStatus();
    Assert.assertEquals(status, HttpStatus.S_204_NO_CONTENT.getCode());

    // GET again, to verify that our POST worked.
    Request<Greeting> request2 = GREETINGS_BUILDERS.get().id(1L).build();
    ResponseFuture<Greeting> future2 = REST_CLIENT.sendRequest(request2);
    String response2 = future2.getResponse().getEntity().getMessage();

    Assert.assertEquals(response2, greeting.getMessage());
  }

  @Test
  //test cookbook example from quickstart wiki
  public void testCookbook() throws Exception
  {
    Client r2Client = new TransportClientAdapter(new HttpClientFactory().getClient(Collections.<String, String>emptyMap()));
    RestClient restClient = new RestClient(r2Client, "http://localhost:1338/");

    // GET
    Request<Greeting> request = GREETINGS_BUILDERS.get().id(1L).build();
    ResponseFuture<Greeting> future = restClient.sendRequest(request);
    Response<Greeting> greetingResponse = future.getResponse();

    Assert.assertNotNull(greetingResponse.getEntity().getMessage());

    // POST
    Greeting greeting = new Greeting(greetingResponse.getEntity().data().copy());
    final String NEW_MESSAGE = "This is a new message!";
    greeting.setMessage(NEW_MESSAGE);

    Request<EmptyRecord> writeRequest = GREETINGS_BUILDERS.update().id(1L).input(greeting).build();
    restClient.sendRequest(writeRequest).getResponse();

    // GET again, to verify that our POST worked.
    Request<Greeting> request2 = GREETINGS_BUILDERS.get().id(1L).build();
    ResponseFuture<Greeting> future2 = restClient.sendRequest(request2);
    greetingResponse = future2.get();

    Assert.assertEquals(greetingResponse.getEntity().getMessage(), NEW_MESSAGE);

    // shut down client
    FutureCallback<None> futureCallback = new FutureCallback<None>();
    r2Client.shutdown(futureCallback);
    futureCallback.get();
  }

  @Test
  public void testCookbookInBatch() throws Exception
  {
    // GET
    BatchGetRequest<Greeting> request = GREETINGS_BUILDERS.batchGet().ids(1L).build();
    ResponseFuture<BatchResponse<Greeting>> future = REST_CLIENT.sendRequest(request);
    Response<BatchResponse<Greeting>> greetingResponse = future.getResponse();

    // POST
    Greeting greeting = new Greeting(greetingResponse.getEntity().getResults().get("1").data().copy());
    greeting.setMessage("This is a new message!");

    BatchUpdateRequest<Long, Greeting> writeRequest = GREETINGS_BUILDERS.batchUpdate().input(1L, greeting).build();
    REST_CLIENT.sendRequest(writeRequest).getResponse();

    // GET again, to verify that our POST worked.
    BatchGetRequest<Greeting> request2 = GREETINGS_BUILDERS.batchGet().ids(1L).build();
    ResponseFuture<BatchResponse<Greeting>> future2 = REST_CLIENT.sendRequest(request2);
    greetingResponse = future2.get();

    Greeting repeatedGreeting = new Greeting();
    repeatedGreeting.setMessage("Hello Hello");
    repeatedGreeting.setTone(Tone.SINCERE);
    BatchCreateRequest<Greeting> request3 = GREETINGS_BUILDERS.batchCreate().inputs(Arrays.asList(repeatedGreeting, repeatedGreeting)).build();
    CollectionResponse<CreateStatus> statuses = REST_CLIENT.sendRequest(request3).getResponse().getEntity();
    for (CreateStatus status : statuses.getElements())
    {
      Assert.assertEquals(status.getStatus().intValue(), HttpStatus.S_201_CREATED.getCode());
      Assert.assertNotNull(status.getId());
    }
  }

  @Test
  public void testSearch() throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> findRequest = GREETINGS_BUILDERS.findBySearch().toneParam(
            Tone.FRIENDLY).build();
    List<Greeting> greetings = REST_CLIENT.sendRequest(findRequest).getResponse().getEntity().getElements();
    for (Greeting g : greetings)
    {
      Assert.assertEquals(g.getTone(), Tone.FRIENDLY);
      Assert.assertNotNull(g.getMessage());
    }
  }

  @Test
  public void testSearchWithPostFilter() throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> findRequest = GREETINGS_BUILDERS.findBySearchWithPostFilter().paginate(0, 5).build();
    CollectionResponse<Greeting> entity = REST_CLIENT.sendRequest(findRequest).getResponse().getEntity();
    CollectionMetadata paging = entity.getPaging();
    Assert.assertEquals(paging.getStart(), 0);
    Assert.assertEquals(paging.getCount(), 5);
    Assert.assertEquals(entity.getElements().size(), 4); // expected to be 4 instead of 5 because of post filter

    // to accommodate post filtering, even though 4 are returned, next page should be 5-10.
    Link next = paging.getLinks().get(0);
    Assert.assertEquals(next.getRel(), "next");
    Assert.assertEquals(next.getHref(), "/greetings?count=5&start=5&q=searchWithPostFilter");
  }

  @Test
  public void testSearchWithTones() throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> req =
        GREETINGS_BUILDERS.findBySearchWithTones().tonesParam(
                Arrays.asList(Tone.SINCERE, Tone.INSULTING)).build();
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

  @Test
  public void testSearchFacets() throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> req = GREETINGS_BUILDERS.findBySearchWithFacets().toneParam(
            Tone.SINCERE).build();
    ResponseFuture<CollectionResponse<Greeting>> future = REST_CLIENT.sendRequest(req);
    Response<CollectionResponse<Greeting>> response = future.getResponse();
    SearchMetadata metadata = new SearchMetadata(response.getEntity().getMetadataRaw());
    Assert.assertTrue(metadata.getFacets().size() > 0);
    // "randomly" generated data is guaranteed to have positive number of each tone
  }

  @Test
  public void testEmptyBatchGetWithProjection() throws RemoteInvocationException
  {
    Request<BatchResponse<Greeting>> request = GREETINGS_BUILDERS.batchGet().ids(1000L,
                                                                                 2000L).fields(Greeting.fields().message()).build();
    BatchResponse<Greeting> response = REST_CLIENT.sendRequest(request).getResponse().getEntity();
    Assert.assertEquals(response.getResults().size(), 0);
  }

  @Test
  public void testBatchGetUsingCollection() throws RemoteInvocationException
  {
    List<Long> ids = Arrays.asList(1L, 2L, 3L, 4L);
    Request<BatchResponse<Greeting>> request = GREETINGS_BUILDERS.batchGet().ids(ids).fields(Greeting.fields().id(), Greeting.fields().message()).build();
    BatchResponse<Greeting> response = REST_CLIENT.sendRequest(request).getResponse().getEntity();
    Assert.assertEquals(response.getResults().size(), ids.size());
  }

  @Test
  public void testBatchGetUsingCollectionKV() throws RemoteInvocationException
  {
    List<Long> ids = Arrays.asList(1L, 2L, 3L, 4L);
    Request<BatchKVResponse<Long, Greeting>> request = GREETINGS_BUILDERS.batchGet().ids(ids).fields(Greeting.fields().id(), Greeting.fields().message()).buildKV();
    BatchKVResponse<Long, Greeting> response = REST_CLIENT.sendRequest(request).getResponse().getEntity();
    Assert.assertEquals(response.getResults().size(), ids.size());
    for (Map.Entry<Long, Greeting> entry : response.getResults().entrySet())
    {
      Assert.assertEquals(entry.getKey(), entry.getValue().getId());
    }
  }

  @Test
  public void testMalformedPagination() throws RemoteInvocationException
  {
    expectPaginationError("-1");
    expectPaginationError("abc");
  }

  private void expectPaginationError(String count) throws RemoteInvocationException
  {
    try
    {
      FindRequest<Greeting> request = new GreetingsBuilders().findBySearch().name("search").param("count", count).build();
      REST_CLIENT.sendRequest(request).getResponse();
      Assert.fail("expected exception");
    }
    catch (RestException e)
    {
      Assert.assertEquals(e.getResponse().getStatus(), HttpStatus.S_400_BAD_REQUEST.getCode(), "expected 400 status");
    }
  }

  @Test
  public void testException() throws RemoteInvocationException
  {
    try
    {
      ActionRequest<Void> request = GREETINGS_BUILDERS.actionExceptionTest().build();
      REST_CLIENT.sendRequest(request).getResponse().getEntity();
      Assert.fail("expected exception");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
      Assert.assertTrue(e.getServiceErrorMessage().contains("Test Exception"));
    }
  }

  @Test
  public void test404() throws RemoteInvocationException
  {
    Request<Greeting> request = GREETINGS_BUILDERS.get().id(999L).build();
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

}
