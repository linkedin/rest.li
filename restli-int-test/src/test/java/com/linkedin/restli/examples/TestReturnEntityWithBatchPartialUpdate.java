/*
   Copyright (c) 2018 LinkedIn Corp.

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

import com.linkedin.data.ByteString;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.GetMode;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.BatchPartialUpdateEntityRequest;
import com.linkedin.restli.client.BatchPartialUpdateEntityRequestBuilder;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.UpdateEntityStatus;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.PartialUpdateGreetingRequestBuilders;
import com.linkedin.restli.internal.common.DataMapConverter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import javax.activation.MimeTypeParseException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Integration tests that ensure {@link ResourceMethod#BATCH_PARTIAL_UPDATE} methods can return the patched entities.
 * Also effectively tests the request builder and decoding logic for this scenario.
 *
 * @author Evan Williams
 */
public class TestReturnEntityWithBatchPartialUpdate extends RestLiIntegrationTest
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

  /**
   * Sends batch partial update requests to the server and ensures that the returned entities for each request are
   * consistent with the expected state of the server's entities.
   *
   * @param patches patches to send for this request
   * @param expectedGreetings expected response entities for this request
   */
  @Test(dataProvider = "batchPartialUpdateData")
  public void testBatchPartialUpdateEntities(Map<Long, PatchRequest<Greeting>> patches, Map<Long, Greeting> expectedGreetings) throws RemoteInvocationException
  {
    BatchPartialUpdateEntityRequest<Long, Greeting> request = new PartialUpdateGreetingRequestBuilders().batchPartialUpdateAndGet()
        .inputs(patches)
        .build();

    Response<BatchKVResponse<Long, UpdateEntityStatus<Greeting>>> response = getClient().sendRequest(request).getResponse();
    Assert.assertNotNull(response);

    BatchKVResponse<Long, UpdateEntityStatus<Greeting>> batchKVResponse = response.getEntity();
    Assert.assertNotNull(batchKVResponse);
    Assert.assertTrue(batchKVResponse.getErrors().isEmpty());

    Map<Long, UpdateEntityStatus<Greeting>> greetings = batchKVResponse.getResults();
    Assert.assertNotNull(greetings);

    for (Long key : greetings.keySet()) {
      Assert.assertTrue(expectedGreetings.containsKey(key));

      UpdateEntityStatus<Greeting> updateEntityStatus = greetings.get(key);
      Assert.assertNotNull(updateEntityStatus);
      Assert.assertEquals(updateEntityStatus.getStatus().intValue(), HttpStatus.S_200_OK.getCode());
      Assert.assertTrue(updateEntityStatus.hasEntity());
      Assert.assertFalse(updateEntityStatus.hasError());

      Greeting greeting = updateEntityStatus.getEntity();
      Greeting expectedGreeting = expectedGreetings.get(key);
      Assert.assertNotNull(greeting);
      Assert.assertNotNull(expectedGreeting);

      Assert.assertEquals(greeting.getId(), expectedGreeting.getId());
      Assert.assertEquals(greeting.getMessage(), expectedGreeting.getMessage());
      Assert.assertEquals(greeting.getTone(), expectedGreeting.getTone());
    }
  }

  /**
   * Same as {@link this#testBatchPartialUpdateEntities}, except the fields of the returned entities are projected.
   *
   * @param patches patches to send for this request
   * @param expectedGreetings expected response entities for this request
   */
  @Test(dataProvider = "batchPartialUpdateData")
  public void testBatchPartialUpdateEntitiesWithProjection(Map<Long, PatchRequest<Greeting>> patches, Map<Long, Greeting> expectedGreetings) throws RemoteInvocationException
  {
    final Greeting.Fields fields = Greeting.fields();

    BatchPartialUpdateEntityRequest<Long, Greeting> request = new PartialUpdateGreetingRequestBuilders().batchPartialUpdateAndGet()
        .inputs(patches)
        .fields(fields.id(), fields.message())
        .build();

    Response<BatchKVResponse<Long, UpdateEntityStatus<Greeting>>> response = getClient().sendRequest(request).getResponse();
    Assert.assertNotNull(response);

    BatchKVResponse<Long, UpdateEntityStatus<Greeting>> batchKVResponse = response.getEntity();
    Assert.assertNotNull(batchKVResponse);
    Assert.assertTrue(batchKVResponse.getErrors().isEmpty());

    Map<Long, UpdateEntityStatus<Greeting>> greetings = batchKVResponse.getResults();
    Assert.assertNotNull(greetings);

    for (Long key : greetings.keySet())
    {
      Assert.assertTrue(expectedGreetings.containsKey(key));

      UpdateEntityStatus<Greeting> updateEntityStatus = greetings.get(key);
      Assert.assertNotNull(updateEntityStatus);
      Assert.assertEquals(updateEntityStatus.getStatus().intValue(), HttpStatus.S_200_OK.getCode());
      Assert.assertTrue(updateEntityStatus.hasEntity());
      Assert.assertFalse(updateEntityStatus.hasError());

      Greeting greeting = updateEntityStatus.getEntity();
      Greeting expectedGreeting = expectedGreetings.get(key);
      Assert.assertNotNull(greeting);
      Assert.assertNotNull(expectedGreeting);

      Assert.assertTrue(greeting.hasId(), "Response record should include an id field.");
      Assert.assertTrue(greeting.hasMessage(), "Response record should include a message field.");
      Assert.assertFalse(greeting.hasTone(), "Response record should not include a tone field due to projection.");

      Assert.assertEquals(greeting.getId(), expectedGreeting.getId());
      Assert.assertEquals(greeting.getMessage(), expectedGreeting.getMessage());
      Assert.assertNull(greeting.getTone(GetMode.NULL), "Response record should have a null tone field due to projection.");
    }
  }

  @DataProvider(name = "batchPartialUpdateData")
  private Object[][] provideBatchPartialUpdateData() throws MimeTypeParseException, IOException
  {
    final Long id1 = 1L;
    final Long id2 = 2L;

    Map<Long, PatchRequest<Greeting>> patches = new HashMap<>();
    patches.put(id1, makeGreetingMessagePatch("Patched message via batch partial update"));
    patches.put(id2, makeGreetingMessagePatch("Yet another patched message"));

    // Revert the patch so that the entity is left in its original state, this prevents conflicts between test methods
    Map<Long, PatchRequest<Greeting>> revertPatches = new HashMap<>();
    revertPatches.put(id1, makeGreetingMessagePatch("Message 1"));
    revertPatches.put(id2, makeGreetingMessagePatch("Message 2"));

    Map<Long, PatchRequest<Greeting>> emptyPatches = new HashMap<>();
    emptyPatches.put(id1, new PatchRequest<>());
    emptyPatches.put(id2, new PatchRequest<>());

    Map<Long, Greeting> oldGreetings = new HashMap<>();
    oldGreetings.put(id1, new Greeting()
        .setId(id1)
        .setMessage("Message 1")
        .setTone(Tone.FRIENDLY));
    oldGreetings.put(id2, new Greeting()
        .setId(id2)
        .setMessage("Message 2")
        .setTone(Tone.FRIENDLY));

    Map<Long, Greeting> newGreetings = new HashMap<>();
    newGreetings.put(id1, new Greeting()
        .setId(id1)
        .setMessage("Patched message via batch partial update")
        .setTone(Tone.FRIENDLY));
    newGreetings.put(id2, new Greeting()
        .setId(id2)
        .setMessage("Yet another patched message")
        .setTone(Tone.FRIENDLY));

    return new Object[][]
        {
            { emptyPatches,   oldGreetings },
            { patches,        newGreetings },
            { emptyPatches,   newGreetings },
            { revertPatches,  oldGreetings }
        };
  }

  /**
   * Ensures that uncaught errors and handled correctly by the server resource and sent back as error responses.
   * This test coerces the server resource to throw an uncaught 500 error.
   */
  @Test
  public void testBatchPartialUpdateError() throws RemoteInvocationException, MimeTypeParseException, IOException
  {
    BatchPartialUpdateEntityRequest<Long, Greeting> request = new PartialUpdateGreetingRequestBuilders().batchPartialUpdateAndGet()
        .input(3L, makeGreetingMessagePatch(";DROP TABLE"))
        .build();

    try
    {
      getClient().sendRequest(request).getResponse();
      Assert.fail("Expected error response.");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
    }
  }

  /**
   * Ensures that individual errors are handled correctly and sent back to the client in the batch response.
   * This test coerces the server resource to return 404 errors after trying to patch nonexistent entities.
   */
  @Test
  public void testBatchPartialUpdateErrorMap() throws RemoteInvocationException
  {
    Map<Long, PatchRequest<Greeting>> patches = new HashMap<>();
    patches.put(2147L, new PatchRequest<>());
    patches.put(2148L, new PatchRequest<>());

    BatchPartialUpdateEntityRequest<Long, Greeting> request = new PartialUpdateGreetingRequestBuilders().batchPartialUpdateAndGet()
        .inputs(patches)
        .returnEntity(true)
        .build();

    Response<BatchKVResponse<Long, UpdateEntityStatus<Greeting>>> response = getClient().sendRequest(request).getResponse();
    Assert.assertNotNull(response);

    BatchKVResponse<Long, UpdateEntityStatus<Greeting>> batchKVResponse = response.getEntity();
    Assert.assertNotNull(batchKVResponse);

    Map<Long, UpdateEntityStatus<Greeting>> greetings = batchKVResponse.getResults();
    Assert.assertNotNull(greetings);

    for (UpdateEntityStatus<Greeting> updateEntityStatus : batchKVResponse.getResults().values())
    {
      Assert.assertFalse(updateEntityStatus.hasEntity());
      Assert.assertEquals(updateEntityStatus.getStatus().intValue(), HttpStatus.S_404_NOT_FOUND.getCode());
      Assert.assertTrue(updateEntityStatus.hasError());

      ErrorResponse error = updateEntityStatus.getError();
      Assert.assertNotNull(error);
      Assert.assertEquals(updateEntityStatus.getError().getStatus().intValue(), HttpStatus.S_404_NOT_FOUND.getCode());
    }

    Map<Long, ErrorResponse> errors = batchKVResponse.getErrors();
    Assert.assertNotNull(errors);

    for (ErrorResponse error : errors.values())
    {
      Assert.assertEquals(error.getStatus().intValue(), HttpStatus.S_404_NOT_FOUND.getCode());
    }
  }

  /**
   * Ensures that different usages of {@link BatchPartialUpdateEntityRequestBuilder#returnEntity(boolean)} are handled
   * correctly and that the response appropriately contains the entities or nothing depending on how and if the provided
   * method is used.
   * @param returnEntity value of the {@link RestConstants#RETURN_ENTITY_PARAM} parameter of this request
   * @param expectReturnEntities whether or not the patched entities are expected in the response
   */
  @Test(dataProvider = "returnEntityOnDemandData")
  public void testReturnEntityOnDemand(Boolean returnEntity, boolean expectReturnEntities) throws RemoteInvocationException
  {
    final long expectedId1 = 8L;
    final long expectedId2 = 9L;

    Map<Long, PatchRequest<Greeting>> patches = new HashMap<>();
    patches.put(expectedId1, new PatchRequest<>());
    patches.put(expectedId2, new PatchRequest<>());

    Map<Long, Greeting> expectedGreetings = new HashMap<>();
    expectedGreetings.put(expectedId1, new Greeting().setId(expectedId1).setMessage("Message " + expectedId1).setTone(Tone.FRIENDLY));
    expectedGreetings.put(expectedId2, new Greeting().setId(expectedId2).setMessage("Message " + expectedId2).setTone(Tone.FRIENDLY));

    BatchPartialUpdateEntityRequestBuilder<Long, Greeting> requestBuilder = new PartialUpdateGreetingRequestBuilders().batchPartialUpdateAndGet()
        .inputs(patches);
    if (returnEntity != null)
    {
      requestBuilder.returnEntity(returnEntity);
    }
    BatchPartialUpdateEntityRequest<Long, Greeting> request = requestBuilder.build();

    Response<BatchKVResponse<Long, UpdateEntityStatus<Greeting>>> response = getClient().sendRequest(request).getResponse();

    BatchKVResponse<Long, UpdateEntityStatus<Greeting>> batchKVResponse = response.getEntity();
    Assert.assertNotNull(batchKVResponse);

    Map<Long, UpdateEntityStatus<Greeting>> greetings = batchKVResponse.getResults();
    Assert.assertNotNull(greetings);

    for (Long key : greetings.keySet())
    {
      Assert.assertTrue(expectedGreetings.containsKey(key), "Encountered unexpected ID in batch response.");

      UpdateEntityStatus<Greeting> updateEntityStatus = greetings.get(key);
      Assert.assertNotNull(updateEntityStatus);
      Assert.assertEquals(updateEntityStatus.getStatus().intValue(), HttpStatus.S_200_OK.getCode());
      Assert.assertFalse(updateEntityStatus.hasError());

      if (expectReturnEntities)
      {
        Assert.assertTrue(updateEntityStatus.hasEntity());

        Greeting returnedEntity = updateEntityStatus.getEntity();
        Greeting expectedEntity = expectedGreetings.get(key);
        Assert.assertNotNull(returnedEntity, "RecordTemplate entity in response should not be null.");
        Assert.assertEquals(returnedEntity.getId(), expectedEntity.getId(), "Expected returned entity ID to match original.");
        Assert.assertEquals(returnedEntity.getMessage(), expectedEntity.getMessage(), "Expected returned entity message to match original.");
        Assert.assertEquals(returnedEntity.getTone(), expectedEntity.getTone(), "Expected returned entity tone to match original.");
      }
      else
      {
        Assert.assertFalse(updateEntityStatus.hasEntity());
        Assert.assertNull(updateEntityStatus.getEntity());
      }
    }
  }

  @DataProvider(name = "returnEntityOnDemandData")
  private Object[][] provideReturnEntityOnDemandData()
  {
    return new Object[][]
        {
            { true, true },
            { false, false },
            { null, true }
        };
  }

  /**
   * Ensures that using an invalid value for the {@link RestConstants#RETURN_ENTITY_PARAM} query parameter results
   * in a 400 bad request error response for BATCH_PARTIAL_UPDATE.
   */
  @Test
  @SuppressWarnings({"Duplicates"})
  public void testInvalidReturnEntityParameter() throws RemoteInvocationException
  {
    final long expectedId = 8L;
    Greeting expectedGreeting = new Greeting();
    expectedGreeting.setMessage("Message " + expectedId);
    expectedGreeting.setTone(Tone.FRIENDLY);

    final String invalidParamValue = "NOTaBoolean";
    BatchPartialUpdateEntityRequest<Long, Greeting> request = new PartialUpdateGreetingRequestBuilders().batchPartialUpdateAndGet()
        .input(expectedId, new PatchRequest<>())
        .setParam(RestConstants.RETURN_ENTITY_PARAM, invalidParamValue)
        .build();

    try
    {
      getClient().sendRequest(request).getResponse();
      Assert.fail(String.format("Query parameter should cause an exception: %s=%s", RestConstants.RETURN_ENTITY_PARAM, invalidParamValue));
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_400_BAD_REQUEST.getCode(), "Invalid response status.");
      Assert.assertTrue(e.getServiceErrorMessage().contains(String.format("Invalid \"%s\" parameter: %s", RestConstants.RETURN_ENTITY_PARAM, invalidParamValue)), "Invalid error response message");
    }
  }

  /**
   * Constructs a patch request that patches the message attribute of a Greeting.
   * @param message new message
   * @return patch request
   */
  private PatchRequest<Greeting> makeGreetingMessagePatch(String message) throws MimeTypeParseException, IOException
  {
    DataMap patchDoc = DataMapConverter.bytesToDataMap(RestConstants.HEADER_VALUE_APPLICATION_JSON,
        ByteString.copyString(String.format("{\"$set\":{\"message\":\"%s\"}}", message), Charset.defaultCharset()));
    return PatchRequest.createFromPatchDocument(patchDoc);
  }
}
