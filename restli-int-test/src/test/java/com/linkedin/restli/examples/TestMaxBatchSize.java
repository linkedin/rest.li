/*
   Copyright (c) 2021 LinkedIn Corp.

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
import com.linkedin.restli.client.*;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.client.util.PatchGenerator;
import com.linkedin.restli.common.*;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.GreetingCriteria;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.BatchGreetingRequestBuilders;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Integration tests to test MaxBatchSize annotation on different batch methods (batchGet, batchCreate, batchUpdate,
 * batchPartialUpdate, batchDelete, batchFinder).
 * These integration tests send requests to {@link com.linkedin.restli.examples.greetings.server.BatchGreetingResource}
 *
 * @author Yingjie Bi
 */
public class TestMaxBatchSize extends RestLiIntegrationTest
{

  private static final Greeting GREETING_ONE;
  private static final Greeting GREETING_TWO;
  private static final Greeting GREETING_THREE;
  private static final GreetingCriteria CRITERIA_ONE;
  private static final GreetingCriteria CRITERIA_TWO;
  private static final GreetingCriteria CRITERIA_THREE;

  static
  {
    GREETING_ONE = new Greeting();
    GREETING_ONE.setTone(Tone.INSULTING);
    GREETING_ONE.setId(1l);
    GREETING_ONE.setMessage("Hi");

    GREETING_TWO = new Greeting();
    GREETING_TWO.setTone(Tone.FRIENDLY);
    GREETING_TWO.setId(2l);
    GREETING_TWO.setMessage("Hello");

    GREETING_THREE = new Greeting();
    GREETING_THREE.setTone(Tone.SINCERE);
    GREETING_THREE.setId(3l);
    GREETING_THREE.setMessage("How are you?");

    CRITERIA_ONE = new GreetingCriteria().setId(1l);
    CRITERIA_TWO = new GreetingCriteria().setId(2l);
    CRITERIA_THREE = new GreetingCriteria().setId(3l);
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
  public void testBatchCreateWithMaxBatchSizeUnderLimitation() throws RemoteInvocationException
  {
    BatchCreateIdRequest<Long, Greeting> request = new BatchGreetingRequestBuilders()
        .batchCreate()
        .inputs(Arrays.asList(GREETING_ONE, GREETING_TWO))
        .build();

    Response<BatchCreateIdResponse<Long>> createResponse =
        getClient().sendRequest(request).getResponse();
    Assert.assertEquals(createResponse.getStatus(), 200);
  }

  @Test
  public void testBatchCreateWithMaxBatchSizeBeyondLimitation() throws RemoteInvocationException
  {
    BatchCreateIdRequest<Long, Greeting> request = new BatchGreetingRequestBuilders()
        .batchCreate()
        .inputs(Arrays.asList(GREETING_ONE, GREETING_TWO, GREETING_THREE))
        .build();

    try
    {
      getClient().sendRequest(request).getResponse();
      Assert.fail("The batch size is larger than the allowed max batch size should cause an exception.");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_400_BAD_REQUEST.getCode());
      Assert.assertEquals(e.getServiceErrorMessage(),"The request batch size: "
          + "3 is larger than the allowed max batch size: 2 for method: batch_create");
    }
  }

  @Test
  public void testBatchGetWithMaxBatchSizeUnderLimitation() throws RemoteInvocationException
  {
    BatchGetEntityRequest<Long, Greeting> request = new BatchGreetingRequestBuilders().batchGet().ids(1l, 2l).build();

    Response<BatchKVResponse<Long, EntityResponse<Greeting>>> response = getClient().sendRequest(request).getResponse();

    Assert.assertEquals(response.getStatus(), 200);
    Assert.assertEquals(response.getEntity().getResults().keySet().size(), 2);
  }

  @Test
  public void testBatchGetWithMaxBatchSizeBeyondLimitation() throws RemoteInvocationException
  {
    BatchGetEntityRequest<Long, Greeting> request = new BatchGreetingRequestBuilders().batchGet().ids(1l, 2l, 3l).build();

    try
    {
      getClient().sendRequest(request).getResponse();
      Assert.fail("The batch size is larger than the allowed max batch size should cause an exception.");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_400_BAD_REQUEST.getCode());
      Assert.assertEquals(e.getServiceErrorMessage(), "The request batch size: "
          + "3 is larger than the allowed max batch size: 2 for method: batch_get");
    }
  }

  @Test
  public void testBatchUpdateWithMaxBatchSizeUnderLimitation() throws RemoteInvocationException
  {
    Request<BatchKVResponse<Long, UpdateStatus>> request = new BatchGreetingRequestBuilders()
        .batchUpdate()
        .input(1L, GREETING_ONE)
        .input(2L, GREETING_TWO)
        .build();

    Response<BatchKVResponse<Long, UpdateStatus>> response = getClient().sendRequest(request).getResponse();
    Assert.assertEquals(response.getStatus(), 200);
    Assert.assertEquals(response.getEntity().getResults().keySet().size(), 2);
    Assert.assertEquals(response.getEntity().getResults().get(1L).getStatus().intValue(), 204);
    Assert.assertEquals(response.getEntity().getResults().get(2L).getStatus().intValue(), 204);
  }

  @Test
  public void testBatchUpdateWithMaxBatchSizeBeyondLimitationButValidationOff()
      throws RemoteInvocationException
  {
    Request<BatchKVResponse<Long, UpdateStatus>> request = new BatchGreetingRequestBuilders()
        .batchUpdate()
        .input(1L, GREETING_ONE)
        .input(2L, GREETING_TWO)
        .input(3L, GREETING_THREE)
        .build();

    Response<BatchKVResponse<Long, UpdateStatus>> response = getClient().sendRequest(request).getResponse();

    Assert.assertEquals(response.getStatus(), 200);
    Assert.assertEquals(response.getEntity().getResults().keySet().size(), 3);
    Assert.assertEquals(response.getEntity().getResults().get(1L).getStatus().intValue(), 204);
    Assert.assertEquals(response.getEntity().getResults().get(2L).getStatus().intValue(), 204);
    Assert.assertEquals(response.getEntity().getResults().get(3L).getStatus().intValue(), 204);
  }

  @Test
  public void testBatchPartialUpdateWithMaxBatchSizeUnderLimitation()
      throws RemoteInvocationException
  {
    Greeting patchedGreetingOne = new Greeting().setTone(Tone.INSULTING).setId(1L).setMessage("Hello");
    Greeting patchedGreetingTwo = new Greeting().setTone(Tone.FRIENDLY).setId(2L).setMessage("Hi");

    Map<Long, PatchRequest<Greeting>> patchInputs = new HashMap<>();
    patchInputs.put(1L, PatchGenerator.diff(GREETING_ONE, patchedGreetingOne));
    patchInputs.put(2L, PatchGenerator.diff(GREETING_TWO, patchedGreetingTwo));

    Request<BatchKVResponse<Long, UpdateStatus>> request = new BatchGreetingRequestBuilders()
        .batchPartialUpdate()
        .inputs(patchInputs)
        .build();

    Response<BatchKVResponse<Long, UpdateStatus>> response = getClient().sendRequest(request).getResponse();

    Assert.assertEquals(response.getStatus(), 200);
    Assert.assertEquals(response.getEntity().getResults().keySet().size(), 2);
    Assert.assertEquals(response.getEntity().getResults().get(1L).getStatus().intValue(), 204);
    Assert.assertEquals(response.getEntity().getResults().get(2L).getStatus().intValue(), 204);
  }

  @Test
  public void testBatchPartialUpdateWithMaxBatchSizeBeyondLimitation()
      throws RemoteInvocationException
  {
    Greeting patchedGreetingOne = new Greeting().setTone(Tone.INSULTING).setId(1L).setMessage("Hello");
    Greeting patchedGreetingTwo = new Greeting().setTone(Tone.FRIENDLY).setId(2L).setMessage("Hi");
    Greeting patchedGreetingThree = new Greeting().setTone(Tone.SINCERE).setId(3L).setMessage("Hello world");

    Map<Long, PatchRequest<Greeting>> patchInputs = new HashMap<>();
    patchInputs.put(1L, PatchGenerator.diff(GREETING_ONE, patchedGreetingOne));
    patchInputs.put(2L, PatchGenerator.diff(GREETING_TWO, patchedGreetingTwo));
    patchInputs.put(3L, PatchGenerator.diff(GREETING_THREE, patchedGreetingThree));

    Request<BatchKVResponse<Long, UpdateStatus>> request = new BatchGreetingRequestBuilders()
        .batchPartialUpdate()
        .inputs(patchInputs)
        .build();

    try
    {
      getClient().sendRequest(request).getResponse();
      Assert.fail("The batch size is larger than the allowed max batch size should cause an exception.");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_400_BAD_REQUEST.getCode());
      Assert.assertEquals(e.getServiceErrorMessage(), "The request batch size: "
          + "3 is larger than the allowed max batch size: 2 for method: batch_partial_update");
    }
  }

  @Test
  public void testBatchDeleteWithMaxBatchSizeUnderLimitation() throws RemoteInvocationException
  {
    Request<BatchKVResponse<Long, UpdateStatus>> request = new BatchGreetingRequestBuilders()
        .batchDelete()
        .ids(1L, 2L)
        .build();

    Response<BatchKVResponse<Long, UpdateStatus>> response = getClient().sendRequest(request).getResponse();

    Assert.assertEquals(response.getStatus(), 200);
    Assert.assertEquals(response.getEntity().getResults().get(1L).getStatus().intValue(), 204);
    Assert.assertEquals(response.getEntity().getResults().get(2L).getStatus().intValue(), 204);
  }

  @Test
  public void testBatchDeleteWithMaxBatchSizeBeyondLimitation() throws RemoteInvocationException
  {
    Request<BatchKVResponse<Long, UpdateStatus>> request = new BatchGreetingRequestBuilders()
        .batchDelete()
        .ids(1L, 2L, 3L)
        .build();

    try
    {
      getClient().sendRequest(request).getResponse();
      Assert.fail("The batch size is larger than the allowed max batch size should cause an exception.");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_400_BAD_REQUEST.getCode());
      Assert.assertEquals(e.getServiceErrorMessage(), "The request batch size: "
          + "3 is larger than the allowed max batch size: 2 for method: batch_delete");
    }
  }

  @Test
  public void testBatchFinderWithOneCriteriaWithMaxBatchSizeUnderLimitation() throws RemoteInvocationException
  {
    Request<BatchCollectionResponse<Greeting>> request = new BatchGreetingRequestBuilders()
        .batchFindBySearchGreetings()
        .addCriteriaParam(CRITERIA_ONE)
        .build();

    BatchCollectionResponse<Greeting> response = getClient().sendRequest(request).getResponse().getEntity();

    List<BatchFinderCriteriaResult<Greeting>> batchResult = response.getResults();

    Assert.assertEquals(batchResult.size(), 1);

    List<Greeting> greetings = batchResult.get(0).getElements();
    Assert.assertTrue(greetings.get(0).hasTone());
    Assert.assertTrue(greetings.get(0).getTone().equals(Tone.INSULTING));
  }

  @Test
  public void testBatchFinderWithMaxBatchSizeUnderLimitation() throws RemoteInvocationException
  {
    Request<BatchCollectionResponse<Greeting>> request = new BatchGreetingRequestBuilders()
        .batchFindBySearchGreetings()
        .criteriaParam(Arrays.asList(CRITERIA_ONE, CRITERIA_TWO))
        .build();

    BatchCollectionResponse<Greeting> response = getClient().sendRequest(request).getResponse().getEntity();

    List<BatchFinderCriteriaResult<Greeting>> batchResult = response.getResults();

    Assert.assertEquals(batchResult.size(), 2);

    List<Greeting> greetings1 = batchResult.get(0).getElements();
    Assert.assertTrue(greetings1.get(0).hasTone());
    Assert.assertTrue(greetings1.get(0).getTone().equals(Tone.INSULTING));

    List<Greeting> greetings2 = batchResult.get(1).getElements();
    Assert.assertTrue(greetings2.get(0).hasId());
    Assert.assertTrue(greetings2.get(0).getTone().equals(Tone.FRIENDLY));
  }

  @Test
  public void testBatchFinderWithMaxBatchSizeBeyondLimitation() throws RemoteInvocationException
  {
    Request<BatchCollectionResponse<Greeting>> request = new BatchGreetingRequestBuilders()
        .batchFindBySearchGreetings()
        .criteriaParam(Arrays.asList(CRITERIA_ONE, CRITERIA_TWO, CRITERIA_THREE))
        .build();

    try
    {
      getClient().sendRequest(request).getResponse();
      Assert.fail("The batch size is larger than the allowed max batch size should cause an exception.");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_400_BAD_REQUEST.getCode());
      Assert.assertEquals(e.getServiceErrorMessage(), "The request batch size: "
          + "3 is larger than the allowed max batch size: 2 for method: searchGreetings");
    }
  }
}
