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

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.data.ByteString;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.GetMode;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.client.PartialUpdateRequestBuilder;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.ResourceSpecImpl;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.internal.common.DataMapConverter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import javax.activation.MimeTypeParseException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Integration tests that ensure {@link ResourceMethod#PARTIAL_UPDATE} methods can return the patched entity.
 *
 * @author Evan Williams
 */
public class TestReturnEntityWithPartialUpdate extends RestLiIntegrationTest
{
  private static ResourceSpec _resourceSpec;

  @BeforeClass
  public void initClass() throws Exception
  {
    super.init();

    _resourceSpec = new ResourceSpecImpl(new HashSet<>(Collections.singletonList(ResourceMethod.PARTIAL_UPDATE)),
        new HashMap<>(),
        new HashMap<>(),
        Long.class,
        Greeting.class,
        new HashMap<>());
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  /**
   * TODO: Update this once client-side support for this feature is implemented. Currently reads the rest response directly.
   *
   * @param patch patch to send for this request
   * @param expectedGreeting expected response entity for this request
   */
  @Test(dataProvider = "partialUpdateData")
  @SuppressWarnings("deprecation")
  public void testPartialUpdateEntity(PatchRequest<Greeting> patch, Greeting expectedGreeting)
  {
    Request<EmptyRecord> request = new PartialUpdateRequestBuilder<Long, Greeting>("partialUpdateGreeting", Greeting.class, _resourceSpec, null)
        .id(1L)
        .input(patch)
        .build();

    final FutureCallback<RestResponse> future = new FutureCallback<>();

    getClient().sendRestRequest(request, new RequestContext(), future);

    RestResponse response = null;
    try
    {
      response = future.get();
    }
    catch (Throwable e)
    {
      Assert.fail(e.getMessage());
    }

    Assert.assertNotNull(response, "Rest response should not be null.");


    Greeting greeting = null;
    try
    {
      DataMap responseData = DataMapConverter.bytesToDataMap(response.getHeader(RestConstants.HEADER_CONTENT_TYPE), response.getEntity());
      greeting = new Greeting(responseData);
    }
    catch (Exception e)
    {
      Assert.fail("Encountered error while parsing rest response data: " + e.getMessage());
    }

    Assert.assertNotNull(greeting, "Response record should not be null.");
    Assert.assertNotNull(expectedGreeting, "Expected record from data provider should not be null.");

    Assert.assertEquals(greeting.getId(), expectedGreeting.getId());
    Assert.assertEquals(greeting.getMessage(), expectedGreeting.getMessage());
    Assert.assertEquals(greeting.getTone(), expectedGreeting.getTone());
  }

  /**
   * TODO: Update this once client-side support for this feature is implemented. Currently reads the rest response directly.
   *
   * @param patch patch to send for this request
   * @param expectedGreeting expected response entity for this request
   */
  @Test(dataProvider = "partialUpdateData")
  @SuppressWarnings("deprecation")
  public void testPartialUpdateEntityWithProjection(PatchRequest<Greeting> patch, Greeting expectedGreeting)
  {
    final Greeting.Fields fields = Greeting.fields();

    Request<EmptyRecord> request = new PartialUpdateRequestBuilder<Long, Greeting>("partialUpdateGreeting", Greeting.class, _resourceSpec, null)
        .id(1L)
        .input(patch)
        .setParam(RestConstants.FIELDS_PARAM, new HashSet<>(Arrays.asList(fields.id(), fields.tone())))
        .build();

    final FutureCallback<RestResponse> future = new FutureCallback<>();

    getClient().sendRestRequest(request, new RequestContext(), future);

    RestResponse response = null;
    try
    {
      response = future.get();
    }
    catch (Throwable e)
    {
      Assert.fail(e.getMessage());
    }

    Assert.assertNotNull(response, "Rest response should not be null.");


    Greeting greeting = null;
    try
    {
      DataMap responseData = DataMapConverter.bytesToDataMap(response.getHeader(RestConstants.HEADER_CONTENT_TYPE), response.getEntity());
      greeting = new Greeting(responseData);
    }
    catch (Exception e)
    {
      Assert.fail("Encountered error while parsing rest response data: " + e.getMessage());
    }

    Assert.assertNotNull(greeting, "Response record should not be null.");
    Assert.assertNotNull(expectedGreeting, "Expected record from data provider should not be null.");

    Assert.assertTrue(greeting.hasId(), "Response record should include an id field.");
    Assert.assertFalse(greeting.hasMessage(), "Response record should not include a message field due to projection.");
    Assert.assertTrue(greeting.hasTone(), "Response record should include a tone field.");

    Assert.assertEquals(greeting.getId(), expectedGreeting.getId());
    Assert.assertNull(greeting.getMessage(GetMode.NULL), "Response record should have a null message field due to projection.");
    Assert.assertEquals(greeting.getTone(), expectedGreeting.getTone());
  }

  @DataProvider(name = "partialUpdateData")
  private Object[][] providePartialUpdateData() throws MimeTypeParseException, IOException
  {
    DataMap patchDoc = DataMapConverter.bytesToDataMap(RestConstants.HEADER_VALUE_APPLICATION_JSON, ByteString.copyString("{\"$set\":{\"message\":\"Patched message\"}}", Charset.defaultCharset()));
    PatchRequest<Greeting> patch = PatchRequest.createFromPatchDocument(patchDoc);

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
            { new PatchRequest<Greeting>(), oldGreeting },
            { patch,                        newGreeting },
            { new PatchRequest<Greeting>(), newGreeting }
        };
  }

  /**
   * Ensures that error responses are handled correctly and as expected.
   * This test coerces the server resource to return a 404 error after trying to patch a nonexistent entity.
   *
   * TODO: Update this once client-side support for this feature is implemented. Currently reads the rest response directly.
   */
  @Test
  @SuppressWarnings("deprecation")
  public void testPartialUpdateError()
  {
    Request<EmptyRecord> request = new PartialUpdateRequestBuilder<Long, Greeting>("partialUpdateGreeting", Greeting.class, _resourceSpec, null)
        .id(2147L)
        .input(new PatchRequest<>())
        .build();

    final FutureCallback<RestResponse> future = new FutureCallback<>();

    // This should fail because the entity we are trying to patch does not exist
    getClient().sendRestRequest(request, new RequestContext(), future);

    try
    {
      future.get();
      Assert.fail("Expected error response.");
    }
    catch (Throwable e)
    {
      RestResponse errorResponse = ((RestException) e.getCause()).getResponse();
      Assert.assertNotNull(errorResponse, "Error rest response should not be null.");
      Assert.assertEquals(errorResponse.getStatus(), HttpStatus.S_404_NOT_FOUND.getCode());
    }
  }

  /**
   * Ensures that the response builder correctly handles different values of {@link RestConstants#RETURN_ENTITY_PARAM},
   * and that the response appropriately contains the entity, nothing, or an error.
   *
   * TODO: Update this once client-side support for this feature is implemented. Currently reads the rest response directly.
   */
  @Test(dataProvider = "returnEntityOnDemandData")
  @SuppressWarnings("deprecation")
  public void testReturnEntityOnDemand(Object paramValue, Boolean expectReturnEntity, boolean expectException)
  {
    final long expectedId = 8L;
    Greeting expectedGreeting = new Greeting();
    expectedGreeting.setMessage("Message " + expectedId);
    expectedGreeting.setTone(Tone.FRIENDLY);

    PartialUpdateRequestBuilder<Long, Greeting> requestBuilder = new PartialUpdateRequestBuilder<Long, Greeting>("partialUpdateGreeting", Greeting.class, _resourceSpec, null)
        .id(expectedId)
        .input(new PatchRequest<>());
    if (paramValue != null)
    {
      requestBuilder.setParam(RestConstants.RETURN_ENTITY_PARAM, paramValue);
    }
    Request<EmptyRecord> request = requestBuilder.build();

    final FutureCallback<RestResponse> future = new FutureCallback<>();

    getClient().sendRestRequest(request, new RequestContext(), future);

    RestResponse response = null;
    try
    {
      response = future.get();

      if (expectException)
      {
        Assert.fail(String.format("Query parameter should cause an exception: %s=%s", RestConstants.RETURN_ENTITY_PARAM, paramValue));
      }
    }
    catch (Throwable e)
    {
      if (!expectException)
      {
        Assert.fail(String.format("Query parameter shouldn't cause an exception: %s=%s", RestConstants.RETURN_ENTITY_PARAM, paramValue));
      }
      return;
    }

    Assert.assertEquals(response.getStatus(), HttpStatus.S_200_OK.getCode());

    if (expectReturnEntity)
    {
      Greeting returnedEntity = null;
      try
      {
        DataMap responseData = DataMapConverter.bytesToDataMap(response.getHeader(RestConstants.HEADER_CONTENT_TYPE), response.getEntity());
        returnedEntity = new Greeting(responseData);
      }
      catch (Exception e)
      {
        Assert.fail("Encountered error while parsing rest response data: " + e.getMessage());
      }

      Assert.assertNotNull(returnedEntity, "RecordTemplate entity in response should not be null.");
      Assert.assertEquals((long) returnedEntity.getId(), expectedId, "Expect returned entity ID to match original.");
      Assert.assertEquals(returnedEntity.getMessage(), expectedGreeting.getMessage(), "Expect returned entity message to match original.");
      Assert.assertEquals(returnedEntity.getTone(), expectedGreeting.getTone(), "Expect returned entity tone to match original.");
    }
    else
    {
      Assert.assertTrue(response.getEntity().isEmpty(), "RecordTemplate entity in response should be null.");
    }
  }

  @DataProvider(name = "returnEntityOnDemandData")
  private Object[][] provideReturnEntityOnDemandData()
  {
    return new Object[][]
        {
            { true, true, false },
            { false, false, false },
            { "foo", null, true },
            { null, true, false }
        };
  }
}
