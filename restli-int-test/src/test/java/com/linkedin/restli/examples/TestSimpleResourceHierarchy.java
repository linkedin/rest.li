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
import com.linkedin.restli.client.ActionRequest;
import com.linkedin.restli.client.BatchCreateRequest;
import com.linkedin.restli.client.BatchGetRequest;
import com.linkedin.restli.client.CreateRequest;
import com.linkedin.restli.client.FindRequest;
import com.linkedin.restli.client.GetRequest;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.client.util.PatchGenerator;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CreateStatus;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.GreetingBuilders;
import com.linkedin.restli.examples.greetings.client.SubgreetingsBuilders;
import com.linkedin.restli.examples.greetings.client.SubsubgreetingBuilders;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * Test class for simple resource hierarchy, root simple resource has a collection sub resource; collection sub resource
 * has a simple sub resource.
 */
public class TestSimpleResourceHierarchy extends RestLiIntegrationTest
{
  private static final Client CLIENT = new TransportClientAdapter(new HttpClientFactory().getClient(Collections.<String, String>emptyMap()));
  private static final String URI_PREFIX = "http://localhost:1338/";
  private static final RestClient REST_CLIENT = new RestClient(CLIENT, URI_PREFIX);

  private final GreetingBuilders _greetingBuilders;
  private final SubgreetingsBuilders _subgreetingsBuilders;
  private final SubsubgreetingBuilders _subsubgreetingBuilders;

  public TestSimpleResourceHierarchy()
  {
    _greetingBuilders = new GreetingBuilders();
    _subgreetingsBuilders = new SubgreetingsBuilders();
    _subsubgreetingBuilders = new SubsubgreetingBuilders();
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

  @Test()
  public void testRootSimpleResourceGet() throws RemoteInvocationException
  {
    GetRequest<Greeting> request = _greetingBuilders.get().build();
    Response<Greeting> response = REST_CLIENT.sendRequest(request).getResponse();
    Greeting greeting = response.getEntity();
    Assert.assertEquals(greeting.getId().longValue(), 12345L);
  }

  @Test()
  public void testRootSimpleResourceUpdate() throws RemoteInvocationException
  {
    Greeting greeting = new Greeting();
    greeting.setMessage("Message1");
    greeting.setTone(Tone.INSULTING);
    greeting.setId(12345L);

    // PUT
    greeting.setTone(Tone.INSULTING);
    Request<EmptyRecord> writeRequest = _greetingBuilders.update().input(greeting).build();
    REST_CLIENT.sendRequest(writeRequest).getResponse();

    // GET again, to verify that our PUT worked.
    GetRequest<Greeting> request = _greetingBuilders.get().build();
    Response<Greeting> response = REST_CLIENT.sendRequest(request).getResponse();
    greeting = response.getEntity();

    Assert.assertEquals(greeting.getTone(), Tone.INSULTING);
  }

  @Test()
  public void testRootSimpleResourceDelete() throws RemoteInvocationException
  {
    // DELETE
    Request<EmptyRecord> writeRequest = _greetingBuilders.delete().build();
    REST_CLIENT.sendRequest(writeRequest).getResponse();

    // GET again, to verify that our DELETE worked.
    try
    {
      GetRequest<Greeting> request = _greetingBuilders.get().build();
      Response<Greeting> response = REST_CLIENT.sendRequest(request).getResponse();
      Greeting greeting = response.getEntity();
      Assert.fail("Entity should have been removed.");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_404_NOT_FOUND.getCode());
    }

    //Restore initial state
    testRootSimpleResourceUpdate();
  }

  @Test
  public void testRootSimpleResourceIntAction() throws RemoteInvocationException
  {
    ActionRequest<Integer> request = _greetingBuilders.actionExampleAction().paramParam1(1).build();
    ResponseFuture<Integer> responseFuture = REST_CLIENT.sendRequest(request);
    Assert.assertEquals(responseFuture.getResponse().getStatus(), 200);
    Assert.assertEquals(responseFuture.getResponse().getEntity().intValue(), 10);
  }

  @Test
  public void testRootSimpleResourceActionException() throws RemoteInvocationException
  {
    try
    {
      ActionRequest<Void> request = _greetingBuilders.actionExceptionTest().build();
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
  public void testSubCollectionGet() throws RemoteInvocationException
  {
    GetRequest<Greeting> request = _subgreetingsBuilders.get().id(1L).build();
    Response<Greeting> response = REST_CLIENT.sendRequest(request).getResponse();
    Greeting greeting = response.getEntity();
    Assert.assertEquals(greeting.getId().longValue(), 1L);
  }

  @Test
  public void testSubCollectionBatchGet() throws RemoteInvocationException
  {
    List<Long> ids = Arrays.asList(1L, 2L, 3L, 4L);
    BatchGetRequest<Greeting> request = _subgreetingsBuilders.batchGet().ids(ids).build();

    Response<BatchResponse<Greeting>> response = REST_CLIENT.sendRequest(request).getResponse();
    BatchResponse<Greeting> batchResponse = response.getEntity();
    Assert.assertEquals(batchResponse.getResults().size(), ids.size());
  }

  @Test
  public void testSubCollectionFinder() throws RemoteInvocationException
  {
    FindRequest<Greeting> request = _subgreetingsBuilders.findBySearch().toneParam(Tone.SINCERE).paginate(1, 2).build();

    Response<CollectionResponse<Greeting>> response = REST_CLIENT.sendRequest(request).getResponse();

    CollectionResponse<Greeting> collectionResponse = response.getEntity();
    List<Greeting> greetings = collectionResponse.getElements();

    for(Greeting g : greetings)
    {
      Assert.assertEquals(g.getTone(), Tone.SINCERE);
    }
  }

  @Test
  public void testSubCollectionUpdate() throws RemoteInvocationException, CloneNotSupportedException, URISyntaxException
  {
    // GET
    Request<Greeting> request = _subgreetingsBuilders.get().id(1L).build();
    ResponseFuture<Greeting> future = REST_CLIENT.sendRequest(request);
    Response<Greeting> greetingResponse = future.getResponse();

    String response1 = greetingResponse.getEntity().getMessage();
    Assert.assertNotNull(response1);

    // PUT
    Greeting greeting = new Greeting(greetingResponse.getEntity().data().copy());
    greeting.setMessage(response1 + "Again");

    Request<EmptyRecord> writeRequest = _subgreetingsBuilders.update().id(1L).input(greeting).build();
    REST_CLIENT.sendRequest(writeRequest).getResponse();

    // GET again, to verify that our POST worked.
    Request<Greeting> request2 = _subgreetingsBuilders.get().id(1L).build();
    ResponseFuture<Greeting> future2 = REST_CLIENT.sendRequest(request2);
    String response2 = future2.getResponse().getEntity().getMessage();

    Assert.assertEquals(response2, response1 + "Again");
  }

  @Test
  public void testSubCollectionPartialUpdate() throws RemoteInvocationException, CloneNotSupportedException, URISyntaxException
  {
    // GET
    Request<Greeting> request = _subgreetingsBuilders.get().id(1L).build();
    ResponseFuture<Greeting> future = REST_CLIENT.sendRequest(request);
    Response<Greeting> greetingResponse = future.getResponse();

    Greeting original = greetingResponse.getEntity();

    // PUT
    Greeting greeting = new Greeting(original.data().copy());
    greeting.setMessage(original.getMessage() + " Again");

    PatchRequest<Greeting> patch = PatchGenerator.diff(original, greeting);

    Request<EmptyRecord> writeRequest = _subgreetingsBuilders.partialUpdate().id(1L).input(patch).build();
    int status = REST_CLIENT.sendRequest(writeRequest).getResponse().getStatus();
    Assert.assertEquals(status, HttpStatus.S_204_NO_CONTENT.getCode());

    // GET again, to verify that our PUT worked.
    Request<Greeting> request2 = _subgreetingsBuilders.get().id(1L).build();
    ResponseFuture<Greeting> future2 = REST_CLIENT.sendRequest(request2);
    String response2 = future2.getResponse().getEntity().getMessage();

    Assert.assertEquals(response2, greeting.getMessage());
  }

  @Test
  public void testSubCollectionCreate() throws RemoteInvocationException
  {
    Greeting greeting = new Greeting();
    greeting.setMessage("Hello there!");
    greeting.setTone(Tone.FRIENDLY);

    //POST
    CreateRequest<Greeting> createRequest = _subgreetingsBuilders.create().input(greeting).build();
    Response<EmptyRecord> emptyRecordResponse = REST_CLIENT.sendRequest(createRequest).getResponse();
    Assert.assertNull(emptyRecordResponse.getHeader(RestConstants.HEADER_CONTENT_TYPE));
    long id = Long.parseLong(emptyRecordResponse.getId());

    //GET again to verify that the create has worked.
    GetRequest<Greeting> getRequest = _subgreetingsBuilders.get().id(id).build();
    Response<Greeting> getResponse = REST_CLIENT.sendRequest(getRequest).getResponse();
    Greeting responseGreeting = getResponse.getEntity();

    Assert.assertEquals(responseGreeting.getMessage(), greeting.getMessage());
    Assert.assertEquals(responseGreeting.getTone(), greeting.getTone());
  }

  @Test
  public void testSubCollectionBatchCreate() throws RemoteInvocationException
  {
    Greeting greeting = new Greeting();
    greeting.setMessage("Message1");
    greeting.setTone(Tone.FRIENDLY);

    Greeting greeting2 = new Greeting();
    greeting.setMessage("Message2");
    greeting.setTone(Tone.FRIENDLY);

    ArrayList<Greeting> greetings = new ArrayList<Greeting>();
    greetings.add(greeting);
    greetings.add(greeting2);

    //POST
    BatchCreateRequest<Greeting> batchCreateRequest = _subgreetingsBuilders.batchCreate().inputs(greetings).build();
    Response<CollectionResponse<CreateStatus>> batchCreateResponse = REST_CLIENT.sendRequest(batchCreateRequest).getResponse();

    ArrayList<Long> ids = new ArrayList<Long>();

    for(CreateStatus status : batchCreateResponse.getEntity().getElements())
    {
      ids.add(Long.parseLong(status.getId()));
    }

    //GET again to verify that the create has worked.
    BatchGetRequest<Greeting> request = _subgreetingsBuilders.batchGet().ids(ids).build();

    Response<BatchResponse<Greeting>> response = REST_CLIENT.sendRequest(request).getResponse();
    BatchResponse<Greeting> batchResponse = response.getEntity();
    Assert.assertEquals(batchResponse.getResults().size(), ids.size());
  }

  @Test
  public void testSubCollectionActionException() throws RemoteInvocationException
  {
    try
    {
      ActionRequest<Void> request = _subgreetingsBuilders.actionExceptionTest().build();
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
  public void testSubCollectionIntAction() throws RemoteInvocationException
  {
    ActionRequest<Integer> request = _subgreetingsBuilders.actionPurge().build();
    ResponseFuture<Integer> responseFuture = REST_CLIENT.sendRequest(request);
    Assert.assertEquals(responseFuture.getResponse().getStatus(), 200);
    Assert.assertEquals(responseFuture.getResponse().getEntity().intValue(), 100);
  }

  @Test()
  public void testSubsubsimpleResourceGet() throws RemoteInvocationException
  {
    GetRequest<Greeting> request = _subsubgreetingBuilders.get().subgreetingsIdKey(1L).build();
    Response<Greeting> response = REST_CLIENT.sendRequest(request).getResponse();
    Greeting greeting = response.getEntity();
    Assert.assertEquals(greeting.getId().longValue(), 10L);
  }

  @Test()
  public void testSubsubsimpleResourceUpdate() throws RemoteInvocationException
  {
    Greeting greeting = new Greeting();
    greeting.setMessage("Message1");
    greeting.setTone(Tone.INSULTING);
    greeting.setId(1L);

    // PUT
    greeting.setTone(Tone.INSULTING);
    Request<EmptyRecord> writeRequest = _subsubgreetingBuilders.update().subgreetingsIdKey(1L).input(greeting).build();
    REST_CLIENT.sendRequest(writeRequest).getResponse();

    // GET again, to verify that our POST worked.
    GetRequest<Greeting> request = _subsubgreetingBuilders.get().subgreetingsIdKey(1L).build();
    Response<Greeting> response = REST_CLIENT.sendRequest(request).getResponse();
    greeting = response.getEntity();

    Assert.assertEquals(greeting.getTone(), Tone.INSULTING);
  }

  @Test()
  public void testSubsubsimpleResourceDelete() throws RemoteInvocationException
  {
    // DELETE
    Request<EmptyRecord> writeRequest = _subsubgreetingBuilders.delete().subgreetingsIdKey(1L).build();
    REST_CLIENT.sendRequest(writeRequest).getResponse();

    // GET again, to verify that our DELETE worked.
    try
    {
      GetRequest<Greeting> request = _subsubgreetingBuilders.get().subgreetingsIdKey(1L).build();
      Response<Greeting> response = REST_CLIENT.sendRequest(request).getResponse();
      Greeting greeting = response.getEntity();
      Assert.fail("Entity should have been removed.");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_404_NOT_FOUND.getCode());
    }

    //Restore initial state
    testSubsubsimpleResourceUpdate();
  }

  @Test
  public void testSubsubsimpleResourceIntAction() throws RemoteInvocationException
  {
    ActionRequest<Integer> request = _subsubgreetingBuilders.actionExampleAction()
                                                            .subgreetingsIdKey(1L)
                                                            .paramParam1(1)
                                                            .build();

    ResponseFuture<Integer> responseFuture = REST_CLIENT.sendRequest(request);
    Assert.assertEquals(responseFuture.getResponse().getStatus(), 200);
    Assert.assertEquals(responseFuture.getResponse().getEntity().intValue(), 10);
  }

  @Test
  public void testSubsubsimpleResourceActionException() throws RemoteInvocationException
  {
    try
    {
      ActionRequest<Void> request = _subsubgreetingBuilders.actionExceptionTest().subgreetingsIdKey(1L).build();
      REST_CLIENT.sendRequest(request).getResponse().getEntity();
      Assert.fail("expected exception");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
      Assert.assertTrue(e.getServiceErrorMessage().contains("Test Exception"));
    }
  }
}
