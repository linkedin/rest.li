/*
   Copyright (c) 2015 LinkedIn Corp.

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
import com.linkedin.restli.client.BatchCreateIdEntityRequest;
import com.linkedin.restli.client.BatchCreateIdRequest;
import com.linkedin.restli.client.CreateIdEntityRequest;
import com.linkedin.restli.client.CreateIdRequest;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.common.BatchCreateIdEntityResponse;
import com.linkedin.restli.common.BatchCreateIdResponse;
import com.linkedin.restli.common.ContentType;
import com.linkedin.restli.common.CreateIdEntityStatus;
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.IdEntityResponse;
import com.linkedin.restli.common.IdResponse;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.CreateGreetingBatchCreateAndGetRequestBuilder;
import com.linkedin.restli.examples.greetings.client.CreateGreetingCreateAndGetRequestBuilder;
import com.linkedin.restli.examples.greetings.client.CreateGreetingRequestBuilders;
import com.linkedin.restli.examples.greetings.server.CreateGreetingResource;

import com.linkedin.restli.server.validation.RestLiValidationFilter;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Integration tests that ensure {@link ResourceMethod#CREATE} and {@link ResourceMethod#BATCH_CREATE} methods can
 * return the created entity/entities. Also effectively tests the request builder and decoding logic for this scenario.
 *
 * These integration tests send requests to {@link CreateGreetingResource}.
 *
 * @author Boyang Chen
 */
public class TestReturnEntityWithCreate extends RestLiIntegrationTest
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

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "newBuildersClientDataDataProvider")
  public void testCreateEntity(RestClient restClient, String expectedContentType, CreateGreetingRequestBuilders builders) throws RemoteInvocationException
  {
    Greeting greeting = new Greeting();
    greeting.setMessage("second time!");
    greeting.setTone(Tone.FRIENDLY);

    CreateIdEntityRequest<Long, Greeting> createIdEntityRequest = builders.createAndGet().input(greeting).build();
    Response<IdEntityResponse<Long, Greeting>> response = restClient.sendRequest(createIdEntityRequest).getResponse();

    long id = response.getEntity().getId();
    @SuppressWarnings("deprecation")
    String stringId = response.getId();
    Assert.assertEquals(response.getHeader(RestConstants.HEADER_CONTENT_TYPE), expectedContentType);
    Assert.assertEquals(response.getHeader(RestConstants.HEADER_LOCATION), "/" + builders.getPrimaryResource() + "/" + id);
    Assert.assertEquals(id, Long.parseLong(stringId));
    Assert.assertEquals("second time!", response.getEntity().getEntity().getMessage());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "newBuildersClientDataDataProvider")
  public void testEntityWithProjection(RestClient restClient, String expectedContentType, CreateGreetingRequestBuilders builders) throws RemoteInvocationException
  {
    Greeting greeting = new Greeting();
    greeting.setMessage("second time!");
    greeting.setTone(Tone.FRIENDLY);

    CreateIdEntityRequest<Long, Greeting> createIdEntityRequest = builders.createAndGet().fields(Greeting.fields().tone(),
                                                                                                 Greeting.fields().id()).input(greeting).build();
    Response<IdEntityResponse<Long, Greeting>> response = restClient.sendRequest(createIdEntityRequest).getResponse();

    long id = response.getEntity().getId();
    @SuppressWarnings("deprecation")
    String stringId = response.getId();
    Assert.assertEquals(response.getHeader(RestConstants.HEADER_CONTENT_TYPE), expectedContentType);
    Assert.assertEquals(response.getHeader(RestConstants.HEADER_LOCATION), "/" + builders.getPrimaryResource() + "/" + id);
    Assert.assertEquals(id, Long.parseLong(stringId));
    Assert.assertEquals(false, response.getEntity().getEntity().hasMessage());
    Assert.assertEquals(Tone.FRIENDLY, response.getEntity().getEntity().getTone());
  }

  @Test (dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "newBuildersClientDataDataProvider")
  public void testBatchCreateWithEntity(RestClient restClient, String expectedContentType, CreateGreetingRequestBuilders builders) throws RemoteInvocationException
  {
    Greeting greeting = new Greeting();
    greeting.setMessage("second time!");
    greeting.setTone(Tone.FRIENDLY);

    Greeting greeting2 = new Greeting();
    greeting2.setMessage("first time!");
    greeting2.setTone(Tone.FRIENDLY);
    List<Greeting> greetings = new ArrayList<Greeting>();
    greetings.add(greeting);
    greetings.add(greeting2);

    BatchCreateIdEntityRequest<Long, Greeting> batchCreateIdEntityRequest = builders.batchCreateAndGet().inputs(greetings).build();
    Response<BatchCreateIdEntityResponse<Long, Greeting>> response = restClient.sendRequest(batchCreateIdEntityRequest).getResponse();
    BatchCreateIdEntityResponse<Long, Greeting> entityResponses = response.getEntity();
    Assert.assertEquals(response.getHeader(RestConstants.HEADER_CONTENT_TYPE), expectedContentType);

    for (CreateIdEntityStatus<?, ?> singleResponse : entityResponses.getElements())
    {
      @SuppressWarnings("deprecation")
      String id = singleResponse.getId();
      Assert.assertNotNull(id);
      Assert.assertEquals(singleResponse.getLocation(), "/" + builders.getPrimaryResource() + "/" + id);
      Greeting entity = (Greeting)singleResponse.getEntity();
      Assert.assertEquals(entity.getTone(), Tone.FRIENDLY);
      Assert.assertEquals(singleResponse.getStatus().intValue(), HttpStatus.S_201_CREATED.getCode());
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "newBuildersClientDataDataProvider")
  public void testBatchCreateEntityWithProjection(RestClient restClient, String expectedContentType, CreateGreetingRequestBuilders builders) throws RemoteInvocationException
  {
    Greeting greeting = new Greeting();
    greeting.setMessage("projection time!");
    greeting.setTone(Tone.FRIENDLY);

    BatchCreateIdEntityRequest<Long, Greeting> batchCreateIdEntityRequest = builders.batchCreateAndGet().fields(Greeting.fields().tone(),
                                                                                                                Greeting.fields().id()).inputs(
                                                                                                                Arrays.asList(greeting, greeting)).build();
    Response<BatchCreateIdEntityResponse<Long, Greeting>> response = restClient.sendRequest(batchCreateIdEntityRequest).getResponse();
    BatchCreateIdEntityResponse<Long, Greeting> entityResponses = response.getEntity();

    Assert.assertEquals(response.getHeader(RestConstants.HEADER_CONTENT_TYPE), expectedContentType);
    for (CreateIdEntityStatus<?, ?> singleResponse : entityResponses.getElements())
    {
      @SuppressWarnings("deprecation")
      String id = singleResponse.getId();
      Assert.assertNotNull(id);
      Assert.assertEquals(singleResponse.getLocation(), "/" + builders.getPrimaryResource() + "/" + id);
      Greeting entity = (Greeting)singleResponse.getEntity();
      Assert.assertEquals(entity.hasMessage(), false);
      Assert.assertEquals(entity.hasId(), true);
      Assert.assertEquals(entity.getTone(), Tone.FRIENDLY);
    }
  }

  @Test (dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "newBuildersClientDataDataProvider")
  public void testBatchCreateWithEntityWithError(RestClient restClient, String expectedContentType, CreateGreetingRequestBuilders builders) throws RemoteInvocationException
  {
    Greeting greeting = new Greeting();
    greeting.setMessage("good!");
    greeting.setTone(Tone.FRIENDLY);

    Greeting greeting2 = new Greeting();
    greeting2.setMessage("too much!");
    greeting2.setTone(Tone.FRIENDLY);
    List<Greeting> greetings = new ArrayList<Greeting>(Arrays.asList(greeting, greeting, greeting, greeting2));


    BatchCreateIdEntityRequest<Long, Greeting> batchCreateIdEntityRequest = builders.batchCreateAndGet().inputs(
        greetings).build();
    Response<BatchCreateIdEntityResponse<Long, Greeting>> response = restClient.sendRequest(batchCreateIdEntityRequest).getResponse();
    BatchCreateIdEntityResponse<Long, Greeting> entityResponses = response.getEntity();
    Assert.assertEquals(response.getHeader(RestConstants.HEADER_CONTENT_TYPE), expectedContentType);

    int numOfElem = 0;
    for (CreateIdEntityStatus<?, ?> singleResponse : entityResponses.getElements())
    {
      if (numOfElem > 2)
      {
        Assert.assertTrue(singleResponse.hasError());
        Assert.assertNull(singleResponse.getLocation());
        Assert.assertEquals(singleResponse.getStatus().intValue(), HttpStatus.S_400_BAD_REQUEST.getCode());
        Assert.assertEquals(singleResponse.getError().getMessage(), "exceed quota"); // More than 3 elements were sent, should trigger exception.
      }
      else
      {
        @SuppressWarnings("deprecation")
        String id = singleResponse.getId();
        Assert.assertNotNull(id);
        Assert.assertEquals(singleResponse.getLocation(), "/" + builders.getPrimaryResource() + "/" + id);
        Greeting entity = (Greeting)singleResponse.getEntity();
        Assert.assertEquals(entity.getTone(), Tone.FRIENDLY);
        Assert.assertEquals(singleResponse.getStatus().intValue(), HttpStatus.S_201_CREATED.getCode());
      }
      numOfElem++;
    }
  }


  /**
   * Test for backward compatibility of create id request.
   * @param restClient
   * @param expectedContentType
   * @param builders
   * @throws RemoteInvocationException
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "newBuildersClientDataDataProvider")
  public void testCreate(RestClient restClient, String expectedContentType, CreateGreetingRequestBuilders builders) throws RemoteInvocationException
  {
    Greeting greeting = new Greeting();
    greeting.setMessage("first time!");
    greeting.setTone(Tone.FRIENDLY);

    CreateIdRequest<Long, Greeting> createIdRequest = builders.create().input(greeting).build();
    Response<IdResponse<Long>> response = restClient.sendRequest(createIdRequest).getResponse();

    Assert.assertEquals(response.getHeader(RestConstants.HEADER_CONTENT_TYPE), expectedContentType);

    long id = response.getEntity().getId();
    @SuppressWarnings("deprecation")
    String stringId = response.getId();
    Assert.assertEquals(id, Long.parseLong(stringId));
    Assert.assertEquals(response.getHeader(RestConstants.HEADER_LOCATION), "/" + builders.getPrimaryResource() + "/" + id);
  }

  /**
   * Test for backward compatibility of batch create id request.
   * @param restClient
   * @param expectedContentType
   * @param builders
   * @throws RemoteInvocationException
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "newBuildersClientDataDataProvider")
  public void testBatchCreate(RestClient restClient, String expectedContentType, CreateGreetingRequestBuilders builders) throws RemoteInvocationException
  {
    Greeting greeting = new Greeting();
    greeting.setMessage("first time!");
    greeting.setTone(Tone.FRIENDLY);

    BatchCreateIdRequest<Long, Greeting> batchCreateIdRequest = builders.batchCreate().inputs(Arrays.asList(greeting,
                                                                                                            greeting)).build();
    Response<BatchCreateIdResponse<Long>> response = restClient.sendRequest(batchCreateIdRequest).getResponse();

    Assert.assertEquals(response.getHeader(RestConstants.HEADER_CONTENT_TYPE), expectedContentType);
    List<CreateIdStatus<Long>> elems = response.getEntity().getElements();
    Assert.assertEquals(elems.get(0).getStatus().intValue(), HttpStatus.S_201_CREATED.getCode());
    Assert.assertEquals(elems.get(0).getLocation(), "/" + builders.getPrimaryResource() + "/" + elems.get(0).getKey());
    Assert.assertEquals(elems.get(1).getStatus().intValue(), HttpStatus.S_201_CREATED.getCode());
    Assert.assertEquals(elems.get(1).getLocation(), "/" + builders.getPrimaryResource() + "/" + elems.get(1).getKey());
  }

  @SuppressWarnings("deprecation")
  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "newBuildersClientDataDataProvider")
  public Object[][] newBuildersClientDataDataProvider()
  {
    return new Object[][]
    {
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX),
          "application/json",
          new CreateGreetingRequestBuilders()
      }, // default client
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX),
          "application/json",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      }, // default client
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         Collections.singletonList(ContentType.PSON)),
          "application/x-pson",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         Collections.singletonList(ContentType.PSON)),
          "application/x-pson",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         Collections.singletonList(ContentType.PSON)),
          "application/x-pson",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         Collections.singletonList(ContentType.JSON)),
          "application/json",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         Collections.singletonList(ContentType.JSON)),
          "application/json",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(), FILTERS_URI_PREFIX,
                         Collections.singletonList(
                             ContentType.ACCEPT_TYPE_ANY)),
          "application/json",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(), FILTERS_URI_PREFIX,
                         Collections.singletonList(
                             ContentType.ACCEPT_TYPE_ANY)),
          "application/json",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         Arrays.asList(ContentType.PSON, ContentType.JSON)),
          "application/x-pson",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         Arrays.asList(ContentType.PSON, ContentType.JSON)),
          "application/x-pson",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         Arrays.asList(ContentType.JSON, ContentType.PSON)),
          "application/json",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         Arrays.asList(ContentType.JSON, ContentType.PSON)),
          "application/json",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         Arrays.asList(ContentType.PSON, ContentType.ACCEPT_TYPE_ANY)),
          "application/x-pson",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         Arrays.asList(ContentType.PSON, ContentType.ACCEPT_TYPE_ANY)),
          "application/x-pson",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         Arrays.asList(ContentType.JSON, ContentType.ACCEPT_TYPE_ANY)),
          "application/json",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         Arrays.asList(ContentType.JSON, ContentType.ACCEPT_TYPE_ANY)),
          "application/json",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         Arrays.asList(ContentType.ACCEPT_TYPE_ANY, ContentType.PSON)),
          "application/x-pson",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         Arrays.asList(ContentType.ACCEPT_TYPE_ANY, ContentType.PSON)),
          "application/x-pson",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         Arrays.asList(ContentType.ACCEPT_TYPE_ANY, ContentType.JSON)),
          "application/json",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         Arrays.asList(ContentType.ACCEPT_TYPE_ANY, ContentType.JSON)),
          "application/json",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      // accept types and content types
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         ContentType.JSON, Collections.<ContentType>emptyList()),
          "application/json",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         ContentType.JSON, Collections.<ContentType>emptyList()),
          "application/json",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         ContentType.JSON, Collections.singletonList(ContentType.JSON)),
          "application/json",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         ContentType.JSON, Collections.singletonList(ContentType.JSON)),
          "application/json",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         ContentType.JSON, Collections.singletonList(ContentType.PSON)),
          "application/x-pson",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         ContentType.JSON, Collections.singletonList(ContentType.PSON)),
          "application/x-pson",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         ContentType.PSON, Collections.<ContentType>emptyList()),
          "application/json",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         ContentType.PSON, Collections.<ContentType>emptyList()),
          "application/json",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         ContentType.PSON, Collections.singletonList(ContentType.JSON)),
          "application/json",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         ContentType.PSON, Collections.singletonList(ContentType.JSON)),
          "application/json",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         ContentType.PSON, Collections.singletonList(ContentType.PSON)),
          "application/x-pson",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         FILTERS_URI_PREFIX,
                         ContentType.PSON, Collections.singletonList(ContentType.PSON)),
          "application/x-pson",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      }
    };
  }

  /**
   * Ensures that different usages of {@link com.linkedin.restli.client.CreateIdEntityRequestBuilder#returnEntity(boolean)} are handled
   * correctly and that the response appropriately contains the entity or nothing depending on how and if the provided
   * method is used.
   */
  @Test(dataProvider = "returnEntityOnDemandData")
  public void testReturnEntityOnDemand(Boolean returnEntity, boolean expectReturnEntity) throws RemoteInvocationException
  {
    Greeting greeting = new Greeting();
    greeting.setMessage("second time!");
    greeting.setTone(Tone.FRIENDLY);

    CreateGreetingRequestBuilders builders = new CreateGreetingRequestBuilders();

    CreateGreetingCreateAndGetRequestBuilder builder = builders.createAndGet().input(greeting);
    if (returnEntity != null)
    {
      builder.returnEntity(returnEntity);
    }
    CreateIdEntityRequest<Long, Greeting> createIdEntityRequest = builder.build();

    Response<IdEntityResponse<Long, Greeting>> response = getClient().sendRequest(createIdEntityRequest).getResponse();

    long id = response.getEntity().getId();
    @SuppressWarnings("deprecation")
    String stringId = response.getId();
    Assert.assertEquals(response.getStatus(), HttpStatus.S_201_CREATED.getCode());
    Assert.assertEquals(response.getHeader(RestConstants.HEADER_LOCATION), "/" + CreateGreetingRequestBuilders.getPrimaryResource() + "/" + id);
    Assert.assertEquals(id, Long.parseLong(stringId));

    if (expectReturnEntity)
    {
      Greeting returnedEntity = response.getEntity().getEntity();
      Assert.assertNotNull(returnedEntity, "RecordTemplate entity in response should not be null.");
      Assert.assertEquals(returnedEntity.getMessage(), greeting.getMessage(), "Expect returned entity message to match original.");
      Assert.assertEquals(returnedEntity.getTone(), greeting.getTone(), "Expect returned entity tone to match original.");
    }
    else
    {
      Assert.assertNull(response.getEntity().getEntity(), "RecordTemplate entity in response should be null.");
    }
  }

  /**
   * Ensures that different usages of {@link com.linkedin.restli.client.CreateIdEntityRequestBuilder#returnEntity(boolean)} are handled
   * correctly and that the response appropriately contains the entities or nothing depending on how and if the provided
   * method is used.
   */
  @Test(dataProvider = "returnEntityOnDemandData")
  public void testBatchCreateReturnEntityOnDemand(Boolean returnEntity, boolean expectReturnEntity) throws RemoteInvocationException
  {
    Greeting greeting = new Greeting();
    greeting.setMessage("second time!");
    greeting.setTone(Tone.FRIENDLY);

    Greeting greeting2 = new Greeting();
    greeting2.setMessage("first time!");
    greeting2.setTone(Tone.FRIENDLY);
    List<Greeting> greetings = new ArrayList<>();
    greetings.add(greeting);
    greetings.add(greeting2);

    CreateGreetingRequestBuilders builders = new CreateGreetingRequestBuilders();

    CreateGreetingBatchCreateAndGetRequestBuilder builder = builders.batchCreateAndGet().inputs(greetings);
    if (returnEntity != null)
    {
      builder.returnEntity(returnEntity);
    }
    BatchCreateIdEntityRequest<Long, Greeting> batchCreateIdEntityRequest = builder.build();

    Response<BatchCreateIdEntityResponse<Long, Greeting>> response = getClient().sendRequest(batchCreateIdEntityRequest).getResponse();

    List<CreateIdEntityStatus<Long, Greeting>> createIdEntityStatuses = response.getEntity().getElements();
    Assert.assertEquals(createIdEntityStatuses.size(), greetings.size(), "Expected size of batch response list to match size of input entity list.");

    for (int i = 0; i < createIdEntityStatuses.size(); i++)
    {
      CreateIdEntityStatus<Long, Greeting> createIdEntityStatus = createIdEntityStatuses.get(i);
      Greeting expectedGreeting = greetings.get(i);

      long id = createIdEntityStatus.getKey();
      Assert.assertEquals((int) createIdEntityStatus.getStatus(), HttpStatus.S_201_CREATED.getCode());
      Assert.assertEquals(createIdEntityStatus.getLocation(), "/" + CreateGreetingRequestBuilders.getPrimaryResource() + "/" + id);

      if (expectReturnEntity)
      {
        Greeting returnedEntity = createIdEntityStatus.getEntity();
        Assert.assertNotNull(returnedEntity, "RecordTemplate entity in response should not be null.");
        Assert.assertEquals(returnedEntity.getMessage(), expectedGreeting.getMessage(), "Expect returned entity message to match original.");
        Assert.assertEquals(returnedEntity.getTone(), expectedGreeting.getTone(), "Expect returned entity tone to match original.");
      }
      else
      {
        Assert.assertNull(createIdEntityStatus.getEntity(), "RecordTemplate entity in response should be null.");
      }
    }
  }

  @DataProvider(name = "returnEntityOnDemandData")
  public Object[][] provideReturnEntityOnDemandData()
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
   * in a 400 bad request error response for CREATE.
   */
  @Test
  @SuppressWarnings({"Duplicates"})
  public void testInvalidReturnEntityParameter() throws RemoteInvocationException
  {
    Greeting greeting = new Greeting();
    greeting.setMessage("second time!");
    greeting.setTone(Tone.FRIENDLY);

    final String invalidParamValue = "NOTaBoolean";
    CreateGreetingRequestBuilders builders = new CreateGreetingRequestBuilders();
    CreateIdEntityRequest<Long, Greeting> createIdEntityRequest = builders.createAndGet()
        .input(greeting)
        .setParam(RestConstants.RETURN_ENTITY_PARAM, invalidParamValue)
        .build();

    try
    {
      getClient().sendRequest(createIdEntityRequest).getResponse();
      Assert.fail(String.format("Query parameter should cause an exception: %s=%s", RestConstants.RETURN_ENTITY_PARAM, invalidParamValue));
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_400_BAD_REQUEST.getCode(), "Invalid response status.");
      Assert.assertTrue(e.getServiceErrorMessage().contains(String.format("Invalid \"%s\" parameter: %s", RestConstants.RETURN_ENTITY_PARAM, invalidParamValue)), "Invalid error response message");
    }
  }

  /**
   * Ensures that using an invalid value for the {@link RestConstants#RETURN_ENTITY_PARAM} query parameter results
   * in a 400 bad request error response for BATCH_CREATE.
   */
  @Test
  @SuppressWarnings({"Duplicates"})
  public void testBatchCreateInvalidReturnEntityParameter() throws RemoteInvocationException
  {
    Greeting greeting = new Greeting();
    greeting.setMessage("second time!");
    greeting.setTone(Tone.FRIENDLY);

    Greeting greeting2 = new Greeting();
    greeting2.setMessage("first time!");
    greeting2.setTone(Tone.FRIENDLY);
    List<Greeting> greetings = new ArrayList<>();
    greetings.add(greeting);
    greetings.add(greeting2);

    final String invalidParamValue = "NOTaBoolean";
    CreateGreetingRequestBuilders builders = new CreateGreetingRequestBuilders();
    BatchCreateIdEntityRequest<Long, Greeting> batchCreateIdEntityRequest = builders.batchCreateAndGet()
        .inputs(greetings)
        .setParam(RestConstants.RETURN_ENTITY_PARAM, invalidParamValue)
        .build();

    try
    {
      getClient().sendRequest(batchCreateIdEntityRequest).getResponse();
      Assert.fail(String.format("Query parameter should cause an exception: %s=%s", RestConstants.RETURN_ENTITY_PARAM, invalidParamValue));
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_400_BAD_REQUEST.getCode(), "Invalid response status.");
      Assert.assertTrue(e.getServiceErrorMessage().contains(String.format("Invalid \"%s\" parameter: %s", RestConstants.RETURN_ENTITY_PARAM, invalidParamValue)), "Invalid error response message");
    }
  }
}
