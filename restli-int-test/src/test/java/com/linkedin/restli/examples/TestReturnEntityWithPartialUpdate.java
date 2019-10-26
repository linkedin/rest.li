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
import com.linkedin.restli.client.PartialUpdateEntityRequest;
import com.linkedin.restli.client.PartialUpdateEntityRequestBuilder;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.PartialUpdateGreetingRequestBuilders;
import com.linkedin.restli.examples.greetings.server.PartialUpdateGreetingResource;
import com.linkedin.restli.internal.common.DataMapConverter;
import com.linkedin.restli.server.validation.RestLiValidationFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import javax.activation.MimeTypeParseException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Integration tests that ensure {@link ResourceMethod#PARTIAL_UPDATE} methods can return the patched entity.
 * Also effectively tests the request builder and decoding logic for this scenario.
 *
 * These integration tests send requests to {@link PartialUpdateGreetingResource}.
 *
 * @author Evan Williams
 */
public class TestReturnEntityWithPartialUpdate extends RestLiIntegrationTest
{
  @BeforeClass
  public void initClass() throws Exception
  {
    super.init(Collections.singletonList(new RestLiValidationFilter()));
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  /**
   * Sends partial update requests to the server and ensures that the returned entity for each request is
   * consistent with the expected state of the server's entities.
   *
   * @param patch patch to send for this request
   * @param expectedGreeting expected response entity for this request
   */
  @Test(dataProvider = "partialUpdateData")
  public void testPartialUpdateEntity(PatchRequest<Greeting> patch, Greeting expectedGreeting) throws RemoteInvocationException
  {
    PartialUpdateEntityRequest<Greeting> request = new PartialUpdateGreetingRequestBuilders().partialUpdateAndGet()
        .id(1L)
        .input(patch)
        .build();

    Response<Greeting> response = getClient().sendRequest(request).getResponse();
    Assert.assertNotNull(response, "Response should not be null.");

    Greeting greeting = response.getEntity();
    Assert.assertNotNull(greeting, "Response record should not be null.");
    Assert.assertNotNull(expectedGreeting, "Expected record from data provider should not be null.");

    Assert.assertEquals(greeting.getId(), expectedGreeting.getId());
    Assert.assertEquals(greeting.getMessage(), expectedGreeting.getMessage());
    Assert.assertEquals(greeting.getTone(), expectedGreeting.getTone());
  }

  /**
   * Same as {@link this#testPartialUpdateEntity}, except the fields of the returned entity are projected.
   *
   * @param patch patch to send for this request
   * @param expectedGreeting expected response entity for this request
   */
  @Test(dataProvider = "partialUpdateData")
  public void testPartialUpdateEntityWithProjection(PatchRequest<Greeting> patch, Greeting expectedGreeting) throws RemoteInvocationException
  {
    final Greeting.Fields fields = Greeting.fields();

    PartialUpdateEntityRequest<Greeting> request = new PartialUpdateGreetingRequestBuilders().partialUpdateAndGet()
        .id(1L)
        .input(patch)
        .fields(fields.id(), fields.message())
        .build();

    Response<Greeting> response = getClient().sendRequest(request).getResponse();
    Assert.assertNotNull(response, "Response should not be null.");

    Greeting greeting = response.getEntity();
    Assert.assertNotNull(greeting, "Response record should not be null.");
    Assert.assertNotNull(expectedGreeting, "Expected record from data provider should not be null.");

    Assert.assertTrue(greeting.hasId(), "Response record should include an id field.");
    Assert.assertTrue(greeting.hasMessage(), "Response record should include a message field.");
    Assert.assertFalse(greeting.hasTone(), "Response record should not include a tone field due to projection.");

    Assert.assertEquals(greeting.getId(), expectedGreeting.getId());
    Assert.assertEquals(greeting.getMessage(), expectedGreeting.getMessage());
    Assert.assertNull(greeting.getTone(GetMode.NULL), "Response record should have a null tone field due to projection.");
  }

  @DataProvider(name = "partialUpdateData")
  private Object[][] providePartialUpdateData() throws MimeTypeParseException, IOException
  {
    PatchRequest<Greeting> patch = makeGreetingMessagePatch("Patched message");

    // Revert the patch so that the entity is left in its original state, this prevents conflicts between test methods
    PatchRequest<Greeting> revertPatch = makeGreetingMessagePatch("Message 1");

    Greeting oldGreeting = new Greeting()
        .setId(1L)
        .setMessage("Message 1")
        .setTone(Tone.FRIENDLY);

    Greeting newGreeting = new Greeting()
        .setId(1L)
        .setMessage("Patched message")
        .setTone(Tone.FRIENDLY);

    return new Object[][]
        {
            { PatchRequest.createFromEmptyPatchDocument(), oldGreeting },
            { patch,                        newGreeting },
            { PatchRequest.createFromEmptyPatchDocument(), newGreeting },
            { revertPatch,                  oldGreeting }
        };
  }

  /**
   * Ensures that error responses are handled correctly and as expected.
   * This test coerces the server resource to return a 404 error after trying to patch a nonexistent entity.
   */
  @Test
  public void testPartialUpdateError() throws RemoteInvocationException
  {
    PartialUpdateEntityRequest<Greeting> request = new PartialUpdateGreetingRequestBuilders().partialUpdateAndGet()
        .id(2147L)
        .input(PatchRequest.createFromEmptyPatchDocument())
        .build();

    try
    {
      getClient().sendRequest(request).getResponse();
      Assert.fail("Expected error response.");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_404_NOT_FOUND.getCode());
    }
  }

  /**
   * Ensures that different usages of {@link PartialUpdateEntityRequestBuilder#returnEntity(boolean)} are handled
   * correctly and that the response appropriately contains the entity or nothing depending on how and if the provided
   * method is used.
   */
  @Test(dataProvider = "returnEntityOnDemandData")
  public void testReturnEntityOnDemand(Boolean returnEntity, boolean expectReturnEntity) throws RemoteInvocationException
  {
    final long expectedId = 8L;
    Greeting expectedGreeting = new Greeting();
    expectedGreeting.setMessage("Message " + expectedId);
    expectedGreeting.setTone(Tone.FRIENDLY);

    PartialUpdateEntityRequestBuilder<Long, Greeting> requestBuilder = new PartialUpdateGreetingRequestBuilders().partialUpdateAndGet()
        .id(expectedId)
        .input(PatchRequest.createFromEmptyPatchDocument());
    if (returnEntity != null)
    {
      requestBuilder.returnEntity(returnEntity);
    }
    PartialUpdateEntityRequest<Greeting> request = requestBuilder.build();

    Response<Greeting> response = getClient().sendRequest(request).getResponse();

    Assert.assertEquals(response.getStatus(), HttpStatus.S_200_OK.getCode());

    if (expectReturnEntity)
    {
      Greeting returnedEntity = response.getEntity();
      Assert.assertNotNull(returnedEntity, "RecordTemplate entity in response should not be null.");
      Assert.assertEquals((long) returnedEntity.getId(), expectedId, "Expect returned entity ID to match original.");
      Assert.assertEquals(returnedEntity.getMessage(), expectedGreeting.getMessage(), "Expect returned entity message to match original.");
      Assert.assertEquals(returnedEntity.getTone(), expectedGreeting.getTone(), "Expect returned entity tone to match original.");
    }
    else
    {
      Assert.assertNull(response.getEntity(), "RecordTemplate entity in response should be null.");
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
   * in a 400 bad request error response for PARTIAL_UPDATE.
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
    PartialUpdateEntityRequest<Greeting> request = new PartialUpdateGreetingRequestBuilders().partialUpdateAndGet()
        .id(expectedId)
        .input(PatchRequest.createFromEmptyPatchDocument())
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
    DataMap patchDoc = DataMapConverter.bytesToDataMap(
        Collections.singletonMap(RestConstants.HEADER_CONTENT_TYPE, RestConstants.HEADER_VALUE_APPLICATION_JSON),
        ByteString.copyString(String.format("{\"$set\":{\"message\":\"%s\"}}", message), Charset.defaultCharset()));
    return PatchRequest.createFromPatchDocument(patchDoc);
  }
}
