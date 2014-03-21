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
import com.linkedin.restli.examples.greetings.client.GreetingRequestBuilders;
import com.linkedin.restli.examples.greetings.client.SubgreetingsBuilders;
import com.linkedin.restli.examples.greetings.client.SubgreetingsRequestBuilders;
import com.linkedin.restli.examples.greetings.client.SubsubgreetingBuilders;
import com.linkedin.restli.examples.greetings.client.SubsubgreetingRequestBuilders;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
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
  public void testRootSimpleResourceGet(RootBuilderWrapper<Void, Greeting> builders) throws RemoteInvocationException
  {
    Request<Greeting> request = builders.get().build();
    Response<Greeting> response = REST_CLIENT.sendRequest(request).getResponse();
    Greeting greeting = response.getEntity();
    Assert.assertEquals(greeting.getId().longValue(), 12345L);
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testRootSimpleResourceUpdate(RootBuilderWrapper<Void, Greeting> builders) throws RemoteInvocationException
  {
    Greeting greeting = new Greeting();
    greeting.setMessage("Message1");
    greeting.setTone(Tone.INSULTING);
    greeting.setId(12345L);

    // PUT
    Request<EmptyRecord> writeRequest = builders.update().input(greeting).build();
    REST_CLIENT.sendRequest(writeRequest).getResponse();

    // GET again, to verify that our PUT worked.
    Request<Greeting> request = builders.get().build();
    Response<Greeting> response = REST_CLIENT.sendRequest(request).getResponse();
    greeting = response.getEntity();

    Assert.assertEquals(greeting.getTone(), Tone.INSULTING);
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testRootSimpleResourcePartialUpdate(RootBuilderWrapper<Void, Greeting> builders) throws RemoteInvocationException
  {
    Greeting greeting = new Greeting();
    greeting.setMessage("Message1");
    greeting.setTone(Tone.SINCERE);
    greeting.setId(12345L);
    PatchRequest<Greeting> patch = PatchGenerator.diffEmpty(greeting);

    // PUT
    Request<EmptyRecord> writeRequest = builders.partialUpdate().input(patch).build();
    REST_CLIENT.sendRequest(writeRequest).getResponse();

    // GET again, to verify that our PUT worked.
    Request<Greeting> request = builders.get().build();
    Response<Greeting> response = REST_CLIENT.sendRequest(request).getResponse();
    greeting = response.getEntity();

    Assert.assertEquals(greeting.getTone(), Tone.SINCERE);
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testRootSimpleResourceDelete(RootBuilderWrapper<Void, Greeting> builders) throws RemoteInvocationException
  {
    // DELETE
    Request<EmptyRecord> writeRequest = builders.delete().build();
    REST_CLIENT.sendRequest(writeRequest).getResponse();

    // GET again, to verify that our DELETE worked.
    try
    {
      Request<Greeting> request = builders.get().build();
      Response<Greeting> response = REST_CLIENT.sendRequest(request).getResponse();
      Greeting greeting = response.getEntity();
      Assert.fail("Entity should have been removed.");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_404_NOT_FOUND.getCode());
    }

    //Restore initial state
    testRootSimpleResourceUpdate(builders);
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testRootSimpleResourceIntAction(RootBuilderWrapper<Void, Greeting> builders) throws RemoteInvocationException
  {
    Request<Integer> request = builders.<Integer>action("ExampleAction").setActionParam("Param1", 1).build();
    ResponseFuture<Integer> responseFuture = REST_CLIENT.sendRequest(request);
    Assert.assertEquals(responseFuture.getResponse().getStatus(), 200);
    Assert.assertEquals(responseFuture.getResponse().getEntity().intValue(), 10);
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testRootSimpleResourceActionException(RootBuilderWrapper<Void, Greeting> builders) throws RemoteInvocationException
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

  @Test(dataProvider = "requestSubBuilderDataProvider")
  public void testSubCollectionGet(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<Greeting> request = builders.get().id(1L).build();
    Response<Greeting> response = REST_CLIENT.sendRequest(request).getResponse();
    Greeting greeting = response.getEntity();
    Assert.assertEquals(greeting.getId().longValue(), 1L);
  }

  @Test(dataProvider = "requestSubBuilderDataProvider")
  public void testSubCollectionBatchGet(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    List<Long> ids = Arrays.asList(1L, 2L, 3L, 4L);
    Request<BatchResponse<Greeting>> request = builders.batchGet().ids(ids).build();

    Response<BatchResponse<Greeting>> response = REST_CLIENT.sendRequest(request).getResponse();
    BatchResponse<Greeting> batchResponse = response.getEntity();
    Assert.assertEquals(batchResponse.getResults().size(), ids.size());
  }

  @Test(dataProvider = "requestSubBuilderDataProvider")
  public void testSubCollectionFinder(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> request = builders.findBy("Search").setQueryParam("tone", Tone.SINCERE).paginate(1, 2).build();

    Response<CollectionResponse<Greeting>> response = REST_CLIENT.sendRequest(request).getResponse();

    CollectionResponse<Greeting> collectionResponse = response.getEntity();
    List<Greeting> greetings = collectionResponse.getElements();

    for(Greeting g : greetings)
    {
      Assert.assertEquals(g.getTone(), Tone.SINCERE);
    }
  }

  @Test(dataProvider = "requestSubBuilderDataProvider")
  public void testSubCollectionUpdate(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException, CloneNotSupportedException, URISyntaxException
  {
    // GET
    Request<Greeting> request = builders.get().id(1L).build();
    ResponseFuture<Greeting> future = REST_CLIENT.sendRequest(request);
    Response<Greeting> greetingResponse = future.getResponse();

    String response1 = greetingResponse.getEntity().getMessage();
    Assert.assertNotNull(response1);

    // PUT
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

  @Test(dataProvider = "requestSubBuilderDataProvider")
  public void testSubCollectionPartialUpdate(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException, CloneNotSupportedException, URISyntaxException
  {
    // GET
    Request<Greeting> request = builders.get().id(1L).build();
    ResponseFuture<Greeting> future = REST_CLIENT.sendRequest(request);
    Response<Greeting> greetingResponse = future.getResponse();

    Greeting original = greetingResponse.getEntity();

    // PUT
    Greeting greeting = new Greeting(original.data().copy());
    greeting.setMessage(original.getMessage() + " Again");

    PatchRequest<Greeting> patch = PatchGenerator.diff(original, greeting);

    Request<EmptyRecord> writeRequest = builders.partialUpdate().id(1L).input(patch).build();
    int status = REST_CLIENT.sendRequest(writeRequest).getResponse().getStatus();
    Assert.assertEquals(status, HttpStatus.S_204_NO_CONTENT.getCode());

    // GET again, to verify that our PUT worked.
    Request<Greeting> request2 = builders.get().id(1L).build();
    ResponseFuture<Greeting> future2 = REST_CLIENT.sendRequest(request2);
    String response2 = future2.getResponse().getEntity().getMessage();

    Assert.assertEquals(response2, greeting.getMessage());
  }

  @Test(dataProvider = "requestSubBuilderDataProvider")
  public void testSubCollectionCreate(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Greeting greeting = new Greeting();
    greeting.setMessage("Hello there!");
    greeting.setTone(Tone.FRIENDLY);

    //POST
    Request<EmptyRecord> createRequest = builders.create().input(greeting).build();
    Response<EmptyRecord> emptyRecordResponse = REST_CLIENT.sendRequest(createRequest).getResponse();
    Assert.assertNull(emptyRecordResponse.getHeader(RestConstants.HEADER_CONTENT_TYPE));
    long id = Long.parseLong(emptyRecordResponse.getId());

    //GET again to verify that the create has worked.
    Request<Greeting> getRequest = builders.get().id(id).build();
    Response<Greeting> getResponse = REST_CLIENT.sendRequest(getRequest).getResponse();
    Greeting responseGreeting = getResponse.getEntity();

    Assert.assertEquals(responseGreeting.getMessage(), greeting.getMessage());
    Assert.assertEquals(responseGreeting.getTone(), greeting.getTone());
  }

  @Test(dataProvider = "requestSubBuilderDataProvider")
  public void testSubCollectionBatchCreate(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
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
    Request<CollectionResponse<CreateStatus>> batchCreateRequest = builders.batchCreate().inputs(greetings).build();
    Response<CollectionResponse<CreateStatus>> batchCreateResponse = REST_CLIENT.sendRequest(batchCreateRequest).getResponse();

    ArrayList<Long> ids = new ArrayList<Long>();

    for(CreateStatus status : batchCreateResponse.getEntity().getElements())
    {
      ids.add(Long.parseLong(status.getId()));
    }

    //GET again to verify that the create has worked.
    Request<BatchResponse<Greeting>> request = builders.batchGet().ids(ids).build();

    Response<BatchResponse<Greeting>> response = REST_CLIENT.sendRequest(request).getResponse();
    BatchResponse<Greeting> batchResponse = response.getEntity();
    Assert.assertEquals(batchResponse.getResults().size(), ids.size());
  }

  @Test(dataProvider = "requestSubBuilderDataProvider")
  public void testSubCollectionActionException(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
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

  @Test(dataProvider = "requestSubBuilderDataProvider")
  public void testSubCollectionIntAction(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<Integer> request = builders.<Integer>action("Purge").build();
    ResponseFuture<Integer> responseFuture = REST_CLIENT.sendRequest(request);
    Assert.assertEquals(responseFuture.getResponse().getStatus(), 200);
    Assert.assertEquals(responseFuture.getResponse().getEntity().intValue(), 100);
  }

  @Test(dataProvider = "requestSubSubBuilderDataProvider")
  public void testSubsubsimpleResourceGet(RootBuilderWrapper<Void, Greeting> builders) throws RemoteInvocationException
  {
    Request<Greeting> request = builders.get().setPathKey("subgreetingsId", 1L).build();
    Response<Greeting> response = REST_CLIENT.sendRequest(request).getResponse();
    Greeting greeting = response.getEntity();
    Assert.assertEquals(greeting.getId().longValue(), 10L);
  }

  @Test(dataProvider = "requestSubSubBuilderDataProvider")
  public void testSubsubsimpleResourceUpdate(RootBuilderWrapper<Void, Greeting> builders) throws RemoteInvocationException
  {
    Greeting greeting = new Greeting();
    greeting.setMessage("Message1");
    greeting.setTone(Tone.INSULTING);
    greeting.setId(1L);

    // PUT
    Request<EmptyRecord> writeRequest = builders.update().setPathKey("subgreetingsId", 1L).input(greeting).build();
    REST_CLIENT.sendRequest(writeRequest).getResponse();

    // GET again, to verify that our POST worked.
    Request<Greeting> request = builders.get().setPathKey("subgreetingsId", 1L).build();
    Response<Greeting> response = REST_CLIENT.sendRequest(request).getResponse();
    greeting = response.getEntity();

    Assert.assertEquals(greeting.getTone(), Tone.INSULTING);
  }

  @Test(dataProvider = "requestSubSubBuilderDataProvider")
  public void testSubsubsimpleResourcePartialUpdate(RootBuilderWrapper<Void, Greeting> builders) throws RemoteInvocationException
  {
    Greeting greeting = new Greeting();
    greeting.setMessage("Message1");
    greeting.setTone(Tone.SINCERE);
    greeting.setId(1L);
    PatchRequest<Greeting> patch = PatchGenerator.diffEmpty(greeting);

    // PUT
    Request<EmptyRecord> writeRequest =
        builders.partialUpdate().setPathKey("subgreetingsId", 1L).input(patch).build();
    REST_CLIENT.sendRequest(writeRequest).getResponse();

    // GET again, to verify that our POST worked.
    Request<Greeting> request = builders.get().setPathKey("subgreetingsId", 1L).build();
    Response<Greeting> response = REST_CLIENT.sendRequest(request).getResponse();
    greeting = response.getEntity();

    Assert.assertEquals(greeting.getTone(), Tone.SINCERE);
  }

  @Test(dataProvider = "requestSubSubBuilderDataProvider")
  public void testSubsubsimpleResourceDelete(RootBuilderWrapper<Void, Greeting> builders) throws RemoteInvocationException
  {
    // DELETE
    Request<EmptyRecord> writeRequest = builders.delete().setPathKey("subgreetingsId", 1L).build();
    REST_CLIENT.sendRequest(writeRequest).getResponse();

    // GET again, to verify that our DELETE worked.
    try
    {
      Request<Greeting> request = builders.get().setPathKey("subgreetingsId", 1L).build();
      Response<Greeting> response = REST_CLIENT.sendRequest(request).getResponse();
      Greeting greeting = response.getEntity();
      Assert.fail("Entity should have been removed.");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_404_NOT_FOUND.getCode());
    }

    //Restore initial state
    testSubsubsimpleResourceUpdate(builders);
  }

  @Test(dataProvider = "requestSubSubBuilderDataProvider")
  public void testSubsubsimpleResourceIntAction(RootBuilderWrapper<Void, Greeting> builders) throws RemoteInvocationException
  {
    Request<Integer> request = builders.<Integer>action("ExampleAction")
                                       .setPathKey("subgreetingsId", 1L)
                                       .setActionParam("Param1", 1)
                                       .build();

    ResponseFuture<Integer> responseFuture = REST_CLIENT.sendRequest(request);
    Assert.assertEquals(responseFuture.getResponse().getStatus(), 200);
    Assert.assertEquals(responseFuture.getResponse().getEntity().intValue(), 10);
  }

  @Test(dataProvider = "requestSubSubBuilderDataProvider")
  public void testSubsubsimpleResourceActionException(RootBuilderWrapper<Void, Greeting> builders) throws RemoteInvocationException
  {
    try
    {
      Request<Void> request = builders.<Void>action("ExceptionTest").setPathKey("subgreetingsId", 1L).build();
      REST_CLIENT.sendRequest(request).getResponse().getEntity();
      Assert.fail("expected exception");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
      Assert.assertTrue(e.getServiceErrorMessage().contains("Test Exception"));
    }
  }

  @DataProvider
  @SuppressWarnings("rawtypes")
  private static Object[][] requestBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper(new GreetingBuilders()) },
      { new RootBuilderWrapper(new GreetingRequestBuilders()) }
    };
  }

  @DataProvider
  @SuppressWarnings("rawtypes")
  private static Object[][] requestSubBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper(new SubgreetingsBuilders()) },
      { new RootBuilderWrapper(new SubgreetingsRequestBuilders()) }
    };
  }

  @DataProvider
  @SuppressWarnings("rawtypes")
  private static Object[][] requestSubSubBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper(new SubsubgreetingBuilders()) },
      { new RootBuilderWrapper(new SubsubgreetingRequestBuilders()) }
    };
  }
}
