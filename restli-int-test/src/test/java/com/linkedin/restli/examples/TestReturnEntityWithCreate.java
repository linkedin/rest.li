package com.linkedin.restli.examples;

import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.CreateIdEntityRequest;
import com.linkedin.restli.client.CreateIdRequest;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.common.IdEntityResponse;
import com.linkedin.restli.common.IdResponse;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.CreateGreetingRequestBuilders;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * Test createEntity request set.
 *
 * @author Boyang Chen
 */
public class TestReturnEntityWithCreate extends RestLiIntegrationTest
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

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "newBuildersClientDataDataProvider")
  public void testCreateIdEntity(RestClient restClient, String expectedContentType, CreateGreetingRequestBuilders builders) throws RemoteInvocationException
  {
    Greeting greeting = new Greeting();
    greeting.setMessage("second time!");
    greeting.setTone(Tone.FRIENDLY);

    CreateIdEntityRequest<Long, Greeting> createIdEntityRequest = builders.createAndGet().input(greeting).build();
    Response<IdEntityResponse<Long, Greeting>> response = restClient.sendRequest(createIdEntityRequest).getResponse();

    long id = response.getEntity().getId();
    @SuppressWarnings("deprecation")
    String stringId = response.getId();
    Assert.assertEquals(id, Long.parseLong(stringId));
    Assert.assertEquals("second time!", ((Greeting) response.getEntity().getEntity()).getMessage());
  }

  /**
   * Test for backward compatibility of create id request.
   * @param restClient
   * @param expectedContentType
   * @param builders
   * @throws RemoteInvocationException
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "newBuildersClientDataDataProvider")
  public void testCreateId(RestClient restClient, String expectedContentType, CreateGreetingRequestBuilders builders) throws RemoteInvocationException
  {
    Greeting greeting = new Greeting();
    greeting.setMessage("first time!");
    greeting.setTone(Tone.FRIENDLY);

    CreateIdRequest<Long, Greeting> createIdRequest = builders.create().input(greeting).build();
    Response<IdResponse<Long>> response = restClient.sendRequest(createIdRequest).getResponse();

    long id = response.getEntity().getId();
    @SuppressWarnings("deprecation")
    String stringId = response.getId();
    Assert.assertEquals(id, Long.parseLong(stringId));
  }


  @SuppressWarnings("deprecation")
  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "newBuildersClientDataDataProvider")
  public Object[][] newBuildersClientDataDataProvider()
  {
    return new Object[][]
    {
      {new RestClient(getDefaultTransportClient(),
                      URI_PREFIX), "application/json", new CreateGreetingRequestBuilders()}, // default client
      {new RestClient(getDefaultTransportClient(), URI_PREFIX), "application/json", new CreateGreetingRequestBuilders(
          TestConstants.FORCE_USE_NEXT_OPTIONS)}, // default client
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Collections.singletonList(RestClient.AcceptType.PSON)),
          "application/x-pson",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Collections.singletonList(RestClient.AcceptType.PSON)),
          "application/x-pson",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Collections.singletonList(RestClient.AcceptType.PSON)),
          "application/x-pson",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Collections.singletonList(RestClient.AcceptType.JSON)),
          "application/json",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Collections.singletonList(RestClient.AcceptType.JSON)),
          "application/json",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(), URI_PREFIX,
                         Collections.singletonList(
                             RestClient.AcceptType.ANY)),
          "application/json",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(), URI_PREFIX,
                         Collections.singletonList(
                             RestClient.AcceptType.ANY)),
          "application/json",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.JSON)),
          "application/x-pson",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.JSON)),
          "application/x-pson",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.PSON)),
          "application/json",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.PSON)),
          "application/json",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.ANY)),
          "application/x-pson",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.ANY)),
          "application/x-pson",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.ANY)),
          "application/json",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.ANY)),
          "application/json",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.PSON)),
          "application/x-pson",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.PSON)),
          "application/x-pson",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.JSON)),
          "application/json",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.JSON)),
          "application/json",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      // accept types and content types
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.JSON, Collections.<RestClient.AcceptType>emptyList()),
          "application/json",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.JSON, Collections.<RestClient.AcceptType>emptyList()),
          "application/json",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.JSON, Collections.singletonList(RestClient.AcceptType.JSON)),
          "application/json",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.JSON, Collections.singletonList(RestClient.AcceptType.JSON)),
          "application/json",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.JSON, Collections.singletonList(RestClient.AcceptType.PSON)),
          "application/x-pson",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.JSON, Collections.singletonList(RestClient.AcceptType.PSON)),
          "application/x-pson",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.PSON, Collections.<RestClient.AcceptType>emptyList()),
          "application/json",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.PSON, Collections.<RestClient.AcceptType>emptyList()),
          "application/json",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.PSON, Collections.singletonList(RestClient.AcceptType.JSON)),
          "application/json",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.PSON, Collections.singletonList(RestClient.AcceptType.JSON)),
          "application/json",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.PSON, Collections.singletonList(RestClient.AcceptType.PSON)),
          "application/x-pson",
          new CreateGreetingRequestBuilders()
      },
      {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.PSON, Collections.singletonList(RestClient.AcceptType.PSON)),
          "application/x-pson",
          new CreateGreetingRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
      }
    };
  }
}
