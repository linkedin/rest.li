/*
   Copyright (c) 2014 LinkedIn Corp.

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


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.linkedin.data.template.StringArray;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.*;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.*;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.NullGreetingRequestBuilders;
import com.linkedin.restli.server.filter.Filter;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResponseContext;
import com.linkedin.restli.internal.server.response.ErrorResponseBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.CompletableFuture;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Series of tests to verify that restli handles nulls correctly from resource classes on the server side.
 * Note that some of these tests will fail when run in Intellij until this is fixed:
 * http://youtrack.jetbrains.com/issue/IDEA-102461#u=1401303768694
 *
 * @author Karim Vidhani
 */
public class TestNullGreetingsClient extends RestLiIntegrationTest
{
  @BeforeClass
  public void initClass() throws Exception
  {
    super.init(ImmutableList.of(new Filter() {
      @Override
      public CompletableFuture<Void> onResponse(FilterRequestContext requestContext,
                                                FilterResponseContext responseContext) {
        //Add a custom header to the response to make sure that 404s/500s returned by
        //nulls in resource methods are also given a chance to experience the filter
        responseContext.getResponseData().getHeaders().put("X-Null-Greetings-Filter", "Ack");
        return CompletableFuture.completedFuture(null);
      }
      @Override
      public CompletableFuture<Void> onError(Throwable t, FilterRequestContext requestContext,
                                             FilterResponseContext responseContext) {
        //Add a custom header to the response to make sure that 404s/500s returned by
        //nulls in resource methods are also given a chance to experience the filter
        responseContext.getResponseData().getHeaders().put("X-Null-Greetings-Filter", "Ack");
        CompletableFuture<Void> future = new CompletableFuture<Void>();
        future.completeExceptionally(t);
        return future;
      }
    }));
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  /*
   * Tests for nulls in CreateResponse
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "requestBuilderDataProvider")
  public void testNullCreateResponse(final NullGreetingRequestBuilders builders)
      throws RemoteInvocationException
  {
    //Forces the server to return a null CreateResponse
    final Greeting illGreeting = new Greeting().setMessage("nullCreateResponse").setTone(Tone.INSULTING);
    createAndAssertNullMessages(builders, illGreeting);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "requestBuilderDataProvider")
  public void testNullHttpStatusCreateResponse(final NullGreetingRequestBuilders builders)
      throws RemoteInvocationException
  {
    //Forces the server to return a valid CreateResponse but with a null HttpStatus inside of it
    final Greeting illGreeting = new Greeting().setMessage("nullHttpStatus").setTone(Tone.INSULTING);
    createAndAssertNullMessages(builders, illGreeting);
  }

  private void createAndAssertNullMessages(
      final NullGreetingRequestBuilders builders,
      final Greeting illGreeting)
      throws RemoteInvocationException
  {
    final CreateIdRequest<Long, Greeting> request = builders.create().input(illGreeting).build();
    try
    {
      getClient().sendRequest(request).getResponse();
      Assert.fail("We should not reach here!");
    }
    catch (final RestLiResponseException responseException)
    {
      assertCorrectInternalServerMessageForNull(responseException, "create");
    }
  }

  /*
   * Tests for nulls in List responses from Finders
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "requestBuilderDataProvider")
  public void testFinderNullListResponse(final NullGreetingRequestBuilders builders)
      throws RemoteInvocationException
  {
    //Here we force the server resource class to return null list
    finderAndAssertNullMessages(builders.findBySearchReturnNullList().toneParam(Tone.INSULTING).build());

    //Here we force the server resource class to return a list with a null element inside of it
    finderAndAssertNullMessages(builders.findBySearchReturnNullList().toneParam(Tone.SINCERE).build());
  }

  /*
   * Tests for nulls in CollectionResult responses from Finders using callbacks
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "requestBuilderDataProvider")
  public void testCallBackFinderNullListResponse(final NullGreetingRequestBuilders builders)
      throws RemoteInvocationException
  {
    //Here we force the server resource class to return null list from the callback
    finderAndAssertNullMessages(
        builders.findByFinderCallbackNullList().toneParam(Tone.INSULTING).build());

    //Here we force the server resource class to return a list from the callback with a null element inside of it
    finderAndAssertNullMessages(builders.findByFinderCallbackNullList().toneParam(Tone.SINCERE).build());
  }

  /*
   * Tests for nulls in CollectionResult responses from Finders using promises
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "requestBuilderDataProvider")
  public void testPromiseFinderNullListResponse(final NullGreetingRequestBuilders builders)
      throws RemoteInvocationException
  {
    //Here we force the server resource class to return null list from the promise
    finderAndAssertNullMessages(builders.findByFinderPromiseNullList().toneParam(Tone.INSULTING).build());

    //Here we force the server resource class to return a list from the promise with a null element inside of it
    finderAndAssertNullMessages(builders.findByFinderPromiseNullList().toneParam(Tone.SINCERE).build());
  }

  /*
   * Tests for nulls in CollectionResult responses from Finders using tasks
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "requestBuilderDataProvider")
  public void testTaskFinderNullListResponse(final NullGreetingRequestBuilders builders)
      throws RemoteInvocationException
  {
    //Here we force the server resource class to return null list from the promise
    finderAndAssertNullMessages(builders.findByFinderTaskNullList().toneParam(Tone.INSULTING).build());

    //Here we force the server resource class to return a list from the promise with a null element inside of it
    finderAndAssertNullMessages(builders.findByFinderTaskNullList().toneParam(Tone.SINCERE).build());
  }

  /*
   * Tests for nulls in CollectionResult responses from Finders using normal sync
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "requestBuilderDataProvider")
  public void testNullsInCollectionResult(final NullGreetingRequestBuilders builders)
      throws RemoteInvocationException
  {
    //Here we force the server resource class to return null CollectionResult
    finderAndAssertNullMessages(
        builders.findBySearchReturnNullCollectionList().toneParam(Tone.INSULTING).build());

    //Here we force the server resource class to return a CollectionResult that has a null list inside of it
    finderAndAssertNullMessages(
        builders.findBySearchReturnNullCollectionList().toneParam(Tone.SINCERE).build());

    //Here we force the server resource class to return a CollectionResult that has a list inside of it which contains a
    //null element
    finderAndAssertNullMessages(
        builders.findBySearchReturnNullCollectionList().toneParam(Tone.FRIENDLY).build());
  }

  private void finderAndAssertNullMessages(final Request<CollectionResponse<Greeting>> req)
      throws RemoteInvocationException
  {
    try
    {
      getClient().sendRequest(req).getResponse();
      Assert.fail("We should not reach here!");
    }
    catch (final RestLiResponseException responseException)
    {
      assertCorrectInternalServerMessageForNull(responseException, "finder");
    }
  }

  /*
   * Tests for nulls as a response from a Get method from a resource class
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "requestBuilderDataProvider")
  public void testGetNull(final NullGreetingRequestBuilders builders)
      throws RemoteInvocationException
  {
    try
    {
      //Forces the server to return null from the resource method
      getClient().sendRequest(builders.get().id(1L).build()).getResponse();
      Assert.fail("We should not reach here!");
    }
    catch (final RestLiResponseException responseException)
    {
      Assert.assertEquals(responseException.getStatus(), 404, "We should have gotten a 404 back");
      Assert.assertNotNull(responseException.getResponse().getHeader("X-Null-Greetings-Filter"), "We should have" +
          " a header applied by the filter");
      Assert.assertEquals(responseException.getResponse().getHeader("X-Null-Greetings-Filter"), "Ack",
                          "The value of the header applied by the response filter should be correct");
    }
  }

  /*
   * Tests for nulls as a response from GET_ALL method from a resource class. Note that tests for finder above
   * exercise the same code path on the server when List/CollectionResult are valid, but have have null
   * elements inside of them. Hence we only exercise a null returned directly by the getAll() method in this test.
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "requestBuilderDataProvider")
  public void testGetAllNull(final NullGreetingRequestBuilders builders)
      throws RemoteInvocationException
  {
    try
    {
      //Forces the server to return null from the resource method
      getClient().sendRequest(builders.getAll().build()).getResponse();
      Assert.fail("We should not reach here!");
    }
    catch (final RestLiResponseException responseException)
    {
      assertCorrectInternalServerMessageForNull(responseException, "get");
    }
  }

  /*
   * Tests for nulls as a response from a update
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "requestBuilderDataProvider")
  public void testUpdateNull(final NullGreetingRequestBuilders builders)
      throws RemoteInvocationException
  {
    //Force the server to return a null UpdateResponse
    sendUpdateAndAssert(builders, 1L);

    //Forces the server to return a valid UpdateResponse but with a null status
    sendUpdateAndAssert(builders, 2L);
  }

  private void sendUpdateAndAssert(final NullGreetingRequestBuilders builders, Long id)
      throws RemoteInvocationException
  {
    try
    {
      final Greeting someGreeting = new Greeting().setMessage("Hello").setTone(Tone.INSULTING);
      getClient().sendRequest(builders.update().id(id).input(someGreeting).build()).getResponse();
    }
    catch (final RestLiResponseException responseException)
    {
      assertCorrectInternalServerMessageForNull(responseException, "update");
    }
  }

  /*
   * Tests for nulls as a response from a BatchUpdate. This test also validates proper error maps.
   * Note that the server side validation for proper error maps could happen through other code paths as well.
   * However here we will just test using BatchUpdate.
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "requestBuilderDataProvider")
  public void testBatchUpdate(final NullGreetingRequestBuilders builders)
      throws RemoteInvocationException
  {
    //Forces the server to return a null BatchUpdateResult
    sendBatchUpdateAndAssert(builders, 1L);

    //Forces the server to return a BatchUpdateResult with a null Map
    sendBatchUpdateAndAssert(builders, 2L);

    //Forces the server to return a BatchUpdateResult with a null errors Map
    sendBatchUpdateAndAssert(builders, 3L);

    //Forces the server to return a BatchUpdateResult with an errors Map that has a null key
    sendBatchUpdateAndAssert(builders, 4L);

    //Forces the server to return a BatchUpdateResult with an errors Map that has a null value
    sendBatchUpdateAndAssert(builders, 5L);

    //Forces the server to return a BatchUpdateResult with a results Map that has a null key in it
    sendBatchUpdateAndAssert(builders, 6L);
  }

  private void sendBatchUpdateAndAssert(final NullGreetingRequestBuilders builders, Long id)
      throws RemoteInvocationException
  {
    try
    {
      final Greeting someGreeting = new Greeting().setMessage("Hello").setTone(Tone.INSULTING);
      Request<BatchKVResponse<Long, UpdateStatus>> writeRequest =
          builders.batchUpdate().input(id, someGreeting).build();
      getClient().sendRequest(writeRequest).getResponse();
    }
    catch (final RestLiResponseException responseException)
    {
      assertCorrectInternalServerMessageForNull(responseException, "batch_update");
    }
  }

  /*
   * This test is one of the few areas in this test suite where we don't expect an exception.
   * The purpose of this test is to make sure Rest.li can handle java.util.concurrent.ConcurrentHashMap(s) sent
   * back by resource methods.
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "requestBuilderDataProvider")
  public void testBatchUpdateUnsupportedNullKeyMap(final NullGreetingRequestBuilders builders)
      throws RemoteInvocationException
  {
    Greeting someGreeting = new Greeting().setMessage("Hello").setTone(Tone.INSULTING);
    Request<BatchKVResponse<Long, UpdateStatus>> writeRequest = builders.batchUpdate().input(7L, someGreeting).build();
    Response<BatchKVResponse<Long, UpdateStatus>> response = getClient().sendRequest(writeRequest).getResponse();

    Map<Long, ErrorResponse> actualErrors = response.getEntity().getErrors();
    Assert.assertEquals(actualErrors.size(), 0, "Errors map should be empty");

    Map<Long, UpdateStatus> actualResults = response.getEntity().getResults();
    Map<Long, UpdateStatus> expectedResults = new HashMap<Long, UpdateStatus>();
    UpdateStatus updateStatus = new UpdateStatus().setStatus(201);
    expectedResults.put(3L, updateStatus);
    Assert.assertEquals(actualResults, expectedResults, "The results map should be correct");
  }

  /*
   * Tests for nulls as a response from a BatchPartialUpdate
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "requestBuilderDataProvider")
  public void testBatchPartialUpdate(final NullGreetingRequestBuilders builders)
      throws RemoteInvocationException
  {
    //Forces the server to return a null BatchUpdateResult
    sendBatchPartialUpdateAndAssert(builders, 1L);

    //Forces the server to return a BatchUpdateResult with a null Map
    sendBatchPartialUpdateAndAssert(builders, 2L);

    //Forces the server to return a BatchUpdateResult with a null errors Map
    sendBatchPartialUpdateAndAssert(builders, 3L);

    //Forces the server to return a BatchUpdateResult with a map that has a null key in it
    sendBatchPartialUpdateAndAssert(builders, 4L);
  }

  private void sendBatchPartialUpdateAndAssert(final NullGreetingRequestBuilders builders, Long id)
      throws RemoteInvocationException
  {
    try
    {
      final Map<Long, PatchRequest<Greeting>> patchedGreetingsDiffs = new HashMap<>();
      patchedGreetingsDiffs.put(id, new PatchRequest<>());
      final Request<BatchKVResponse<Long, UpdateStatus>> batchUpdateRequest =
          builders.batchPartialUpdate().inputs(patchedGreetingsDiffs).build();
      getClient().sendRequest(batchUpdateRequest).getResponse();
    }
    catch (final RestLiResponseException responseException)
    {
      assertCorrectInternalServerMessageForNull(responseException, "batch_partial_update");
    }
  }

  /*
   * Tests for nulls from a delete
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "requestBuilderDataProvider")
  public void testDeleteNull(final NullGreetingRequestBuilders builders)
      throws RemoteInvocationException
  {
    try
    {
      //Forces the server to return null from the resource method
      getClient().sendRequest(builders.delete().id(1L).build()).getResponse();
      Assert.fail("We should not reach here!");
    }
    catch (final RestLiResponseException responseException)
    {
      assertCorrectInternalServerMessageForNull(responseException, "delete");
    }
  }

  /*
   * Tests for nulls from a batch delete
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "requestBuilderDataProvider")
  public void testBatchDeleteNull(final NullGreetingRequestBuilders builders)
      throws RemoteInvocationException
  {
    try
    {
      //Forces the server to return null from the resource method
      final Request<BatchKVResponse<Long, UpdateStatus>> deleteRequest =
          builders.batchDelete().ids(ImmutableList.of(1L)).build();
      getClient().sendRequest(deleteRequest).getResponse();
      Assert.fail("We should not reach here!");
    }
    catch (final RestLiResponseException responseException)
    {
      assertCorrectInternalServerMessageForNull(responseException, "batch_delete");
    }
  }

  /*
   * These followings tests test all the various ways we can return nulls from actions
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "requestBuilderDataProvider")
  public void testNullStringArray(final NullGreetingRequestBuilders builders)
      throws RemoteInvocationException
  {
    //This forces the server to return a null StringArray from the resource method
    final Request<StringArray> request = builders.actionReturnNullStringArray().build();
    final Response<StringArray> response = getClient().sendRequest(request).getResponse();
    Assert.assertEquals(response.getStatus(), 200, "We should have gotten a 200 back");
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "requestBuilderDataProvider")
  public void testNullActionResult(final NullGreetingRequestBuilders builders)
      throws RemoteInvocationException
  {
    //This forces the server to return a null ActionResult from the resource method
    final Request<Integer> request = builders.actionReturnNullActionResult().build();
    final Response<Integer> response = getClient().sendRequest(request).getResponse();
    Assert.assertEquals(response.getStatus(), 200, "We should have gotten a 200 back");
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "requestBuilderDataProvider")
  public void testStringArrayNullElement(final NullGreetingRequestBuilders builders)
      throws RemoteInvocationException
  {
    try
    {
      //This forces the server to return a valid StringArray but with a null element inside of it
      final Request<StringArray> request = builders.actionReturnStringArrayWithNullElement().build();
      getClient().sendRequest(request).getResponse();
      Assert.fail("We should not reach here!");
    }
    catch (RestLiResponseException responseException)
    {
      Assert.assertEquals(responseException.getStatus(), 500, "We should have gotten a 500 back");
      Assert.assertTrue(responseException.getMessage().contains(ErrorResponseBuilder.DEFAULT_INTERNAL_ERROR_MESSAGE), "The error message should be correct");
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "requestBuilderDataProvider")
  public void testActionResultNullValue(final NullGreetingRequestBuilders builders)
      throws RemoteInvocationException
  {
    try
    {
      //This forces the server to return an ActionResult with a null value inside of it
      final Request<Integer> request = builders.actionReturnActionResultWithNullValue().build();
      getClient().sendRequest(request).getResponse();
      Assert.fail("We should not reach here!");
    }
    catch (RestLiResponseException responseException)
    {
      Assert.assertEquals(responseException.getStatus(), 500, "We should have gotten a 500 back");
      Assert.assertTrue(
          responseException.getMessage().contains("Cannot set field value of com.linkedin.restli.common.ActionResponse to null"),
          "The error message should be correct");
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "requestBuilderDataProvider")
  public void testActionResultNullStatus(final NullGreetingRequestBuilders builders)
      throws RemoteInvocationException
  {
    try
    {
      //This forces the server to return an ActionResult with a null HttpStatus inside of it
      final Request<Integer> request = builders.actionReturnActionResultWithNullStatus().build();
      getClient().sendRequest(request).getResponse();
      Assert.fail("We should not reach here!");
    }
    catch (RestLiResponseException responseException)
    {
      assertCorrectInternalServerMessageForNull(responseException, "action");
    }
  }

  /*
   * Tests for nulls as a response from a BatchCreate
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "requestBuilderDataProvider")
  public void testBatchCreateNulls(final NullGreetingRequestBuilders builders)
      throws RemoteInvocationException
  {
    final Greeting firstGreeting = new Greeting().setMessage("first").setTone(Tone.INSULTING);
    final Greeting secondGreeting = new Greeting().setMessage("first").setTone(Tone.INSULTING);

    //Forces the server to return a null list
    sendBatchCreateAndAssert(builders, Collections.<Greeting>emptyList());

    //Forces the server to return a BatchCreateResult with a null list in it
    sendBatchCreateAndAssert(builders, ImmutableList.of(firstGreeting));

    //Forces the server to return a BatchCreateResult with a list that has a null element in it
    sendBatchCreateAndAssert(builders, ImmutableList.of(firstGreeting, secondGreeting));
  }

  private void sendBatchCreateAndAssert(final NullGreetingRequestBuilders builders,
                                        final List<Greeting> greetingList)
      throws RemoteInvocationException
  {
    try
    {
      BatchCreateIdRequest<Long, Greeting> request = builders
          .batchCreate()
          .inputs(greetingList)
          .build();

      getClient().sendRequest(request).getResponse();

      Assert.fail("We should not reach here!");
    }
    catch (final RestLiResponseException responseException)
    {
      assertCorrectInternalServerMessageForNull(responseException, "batch_create");
    }
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "requestBuilderDataProvider")
  private static Object[][] requestBuilderDataProvider()
  {
    return new Object[][]
        {
            {
                new NullGreetingRequestBuilders()
            },
            {
                new NullGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
            }
        };
  }

  private void assertCorrectInternalServerMessageForNull(final RestLiResponseException responseException, final String type)
  {
    Assert.assertEquals(responseException.getStatus(), 500, "We should have gotten a 500 back");
    Assert.assertTrue(responseException.getMessage().contains("Unexpected null encountered"), "The error message should be correct");
    Assert.assertTrue(responseException.getMessage().contains("type="+type.toLowerCase()), "The type should be correct");
    Assert.assertTrue(responseException.getMessage().contains("resourceName=nullGreeting"), "The resourceName should be correct");
    Assert.assertNotNull(responseException.getResponse().getHeader("X-Null-Greetings-Filter"), "We should have" +
        " a header applied by the filter");
    Assert.assertEquals(responseException.getResponse().getHeader("X-Null-Greetings-Filter"), "Ack", "The value" +
        " of the header applied by the response filter should be correct");
  }

  /*
   * Tests for nulls in BatchResult returned by BatchGet
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "batchGetRequestBuilderDataProvider")
  public void testBatchGetNullBatchResult(final BatchGetEntityRequestBuilder<Long, Greeting> builder)
      throws RemoteInvocationException
  {
    try
    {
      //Here we force the server resource class to return null BatchResult
      BatchGetEntityRequest<Long, Greeting> request =
          builder.ids(ImmutableSet.of(1l)).fields(Greeting.fields().id(), Greeting.fields().message()).build();
      getClient().sendRequest(request).getResponse();
      Assert.fail("We should not reach here!");
    }
    catch (final RestLiResponseException responseException)
    {
      assertCorrectInternalServerMessageForNull(responseException, "batch_get");
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "batchGetRequestBuilderDataProvider")
  public void testBatchGetNullBatchResultInternals(final BatchGetEntityRequestBuilder<Long, Greeting> builder)
      throws RemoteInvocationException
  {
    //Here we force the server to place nulls for all of the maps in the BatchResult constructor
    BatchGetEntityRequest<Long, Greeting> request =
        builder.ids(ImmutableSet.of(2l)).fields(Greeting.fields().id(), Greeting.fields().message()).build();
    Response<BatchKVResponse<Long, EntityResponse<Greeting>>> response = getClient().sendRequest(request).getResponse();
    Assert.assertEquals(response.getStatus(), 200, "We should have gotten a 200 here!");
    Assert.assertNotNull(response.getHeader("X-Null-Greetings-Filter"), "We should have" +
        " a header applied by the filter");
    Assert.assertEquals(response.getHeader("X-Null-Greetings-Filter"), "Ack", "The value" +
        " of the header applied by the response filter should be correct");
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "batchGetRequestBuilderDataProvider")
  public void testBatchGetNullKeyBatchResultStatusMap(final BatchGetEntityRequestBuilder<Long, Greeting> builder)
      throws RemoteInvocationException
  {
    //Here we force the server resource class to return a BatchResult that has a status map inside of it
    //which contains a null key
    try
    {
      BatchGetEntityRequest<Long, Greeting> request =
          builder.ids(ImmutableSet.of(3l)).fields(Greeting.fields().id(), Greeting.fields().message()).build();
      getClient().sendRequest(request).getResponse();
      Assert.fail("We should not reach here!");
    }
    catch (final RestLiResponseException responseException)
    {
      assertCorrectInternalServerMessageForNull(responseException, "batch_get");
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "batchGetRequestBuilderDataProvider")
  public void testBatchGetNullKeyBatchResultMap(final BatchGetEntityRequestBuilder<Long, Greeting> builder)
      throws RemoteInvocationException
  {
    //Here we force the server resource class to return a BatchResult that has a map inside of it which contains a null key
    try
    {
      BatchGetEntityRequest<Long, Greeting> request =
          builder.ids(ImmutableSet.of(4l)).fields(Greeting.fields().id(), Greeting.fields().message()).build();
      getClient().sendRequest(request).getResponse();
      Assert.fail("We should not reach here!");
    }
    catch (final RestLiResponseException responseException)
    {
      assertCorrectInternalServerMessageForNull(responseException, "batch_get");
    }
  }

  /*
   * This test is one of the few areas in this test suite where we don't expect an exception.
   * The purpose of this test is to make sure Rest.li can handle java.util.concurrent.ConcurrentHashMap(s) sent
   * back by resource methods.
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "batchGetRequestBuilderDataProvider")
  public void testBatchGetUnsupportedNullKeyMap(final BatchGetEntityRequestBuilder<Long, Greeting> builder)
      throws RemoteInvocationException
  {
    BatchGetEntityRequest<Long, Greeting> request =
        builder.ids(ImmutableSet.of(5l)).fields(Greeting.fields().id(), Greeting.fields().message()).build();
    Response<BatchKVResponse<Long, EntityResponse<Greeting>>> response = getClient().sendRequest(request).getResponse();
    final Greeting actualGreeting = response.getEntity().getResults().get(0l).getEntity();
    Assert.assertEquals(actualGreeting.getMessage(), "Good morning!", "We should get the correct Greeting back");
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "batchGetRequestBuilderDataProvider")
  private static Object[][] batchGetRequestBuilderDataProvider()
  {
    return new Object[][]
        {
            {
                new NullGreetingRequestBuilders().batchGet()
            },
            {
                new NullGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS).batchGet()
            }
        };
  }
}
