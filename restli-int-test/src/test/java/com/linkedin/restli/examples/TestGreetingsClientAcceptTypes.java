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
import com.linkedin.restli.client.CreateIdRequest;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.client.response.CreateResponse;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.IdResponse;
import com.linkedin.restli.common.Link;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.GreetingsBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsRequestBuilders;
import com.linkedin.restli.examples.groups.api.TransferOwnershipRequest;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class TestGreetingsClientAcceptTypes extends RestLiIntegrationTest
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

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientDataDataProvider")
  public void testGet(RestClient restClient, String expectedContentType, RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<Greeting> request = builders.get().id(1L).build();

    Response<Greeting> response = restClient.sendRequest(request).getResponse();
    Assert.assertEquals(response.getHeader("Content-Type"), expectedContentType);
    Greeting greeting = response.getEntity();
    Assert.assertEquals(greeting.getId(), new Long(1));
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "oldBuildersClientDataDataProvider")
  public void testBatchGet(RestClient restClient, String expectedContentType, GreetingsBuilders builders)
          throws RemoteInvocationException
  {
    List<Long> ids = Arrays.asList(1L, 2L, 3L, 4L);
    Request<BatchResponse<Greeting>> request = builders.batchGet().ids(ids).build();

    Response<BatchResponse<Greeting>> response = restClient.sendRequest(request).getResponse();
    Assert.assertEquals(response.getHeader(RestConstants.HEADER_CONTENT_TYPE), expectedContentType);
    BatchResponse<Greeting> batchResponse = response.getEntity();
    Assert.assertEquals(batchResponse.getResults().size(), ids.size());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "newBuildersClientDataDataProvider")
  public void testBatchGetEntity(RestClient restClient, String expectedContentType, GreetingsRequestBuilders builders)
    throws RemoteInvocationException
  {
    List<Long> ids = Arrays.asList(1L, 2L, 3L, 4L);
    Request<BatchKVResponse<Long, EntityResponse<Greeting>>> request = builders.batchGet().ids(ids).build();

    Response<BatchKVResponse<Long, EntityResponse<Greeting>>> response = restClient.sendRequest(request).getResponse();
    Assert.assertEquals(response.getHeader(RestConstants.HEADER_CONTENT_TYPE), expectedContentType);
    BatchKVResponse<Long, EntityResponse<Greeting>> batchResponse = response.getEntity();
    Assert.assertEquals(batchResponse.getResults().size(), ids.size());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientDataDataProvider")
  public void testFinder(RestClient restClient, String expectedContentType, RootBuilderWrapper<Long, Greeting> builders)
          throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> request = builders.findBy("Search").setQueryParam("tone", Tone.SINCERE).paginate(1, 2).build();

    Response<CollectionResponse<Greeting>> response = restClient.sendRequest(request).getResponse();
    Assert.assertEquals(response.getHeader("Content-Type"), expectedContentType);

    CollectionResponse<Greeting> collectionResponse = response.getEntity();
    List<Greeting> greetings = collectionResponse.getElements();

    for(Greeting g : greetings)
    {
      Assert.assertEquals(g.getTone(), Tone.SINCERE);
    }
    collectionResponse.getPaging().getLinks();

    for (Link link : collectionResponse.getPaging().getLinks())
    {
      Assert.assertEquals(link.getType(), expectedContentType);
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientDataDataProvider")
  public void testAction(RestClient restClient, String expectedContentType, RootBuilderWrapper<Long, Greeting> builders)
          throws RemoteInvocationException
  {
    Request<Greeting> request = builders.<Greeting>action("SomeAction").id(1L)
            .setActionParam("A", 1)
            .setActionParam("B", "")
            .setActionParam("C", new TransferOwnershipRequest())
            .setActionParam("D", new TransferOwnershipRequest())
            .setActionParam("E", 3)
            .build();

    Response<Greeting> response = restClient.sendRequest(request).getResponse();
    Assert.assertEquals(response.getHeader("Content-Type"), expectedContentType);

    Greeting greeting = response.getEntity();
    Assert.assertEquals(greeting.getMessage(), "This is a newly created greeting");
  }

  @SuppressWarnings("deprecation")
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "oldBuildersClientDataDataProvider")
  public void testCreate(RestClient restClient, String expectedContentType, GreetingsBuilders builders) throws RemoteInvocationException
  {
    Greeting greeting = new Greeting();
    greeting.setMessage("Hello there!");
    greeting.setTone(Tone.FRIENDLY);

    Request<EmptyRecord> createRequest = builders.create().input(greeting).build();
    Response<EmptyRecord> response = restClient.sendRequest(createRequest).getResponse();
    Assert.assertNull(response.getHeader(RestConstants.HEADER_CONTENT_TYPE));
    @SuppressWarnings("unchecked")
    CreateResponse<Long> createResponse = (CreateResponse<Long>)response.getEntity();
    long id = createResponse.getId();
    @SuppressWarnings("deprecation")
    String stringId = response.getId();
    Assert.assertEquals(id, Long.parseLong(stringId));

    Request<Greeting> getRequest = builders.get().id(id).build();
    Response<Greeting> getResponse = restClient.sendRequest(getRequest).getResponse();
    Assert.assertEquals(getResponse.getHeader(RestConstants.HEADER_CONTENT_TYPE), expectedContentType);
    Greeting responseGreeting = getResponse.getEntity();

    Assert.assertEquals(responseGreeting.getMessage(), greeting.getMessage());
    Assert.assertEquals(responseGreeting.getTone(), greeting.getTone());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "newBuildersClientDataDataProvider")
  public void testCreateId(RestClient restClient, String expectedContentType, GreetingsRequestBuilders builders) throws RemoteInvocationException
  {
    Greeting greeting = new Greeting();
    greeting.setMessage("Hello there!");
    greeting.setTone(Tone.FRIENDLY);

    CreateIdRequest<Long, Greeting> createRequest = builders.create().input(greeting).build();
    Response<IdResponse<Long>> response = restClient.sendRequest(createRequest).getResponse();
    Assert.assertNull(response.getHeader(RestConstants.HEADER_CONTENT_TYPE));
    long id = response.getEntity().getId();
    @SuppressWarnings("deprecation")
    String stringId = response.getId();
    Assert.assertEquals(id, Long.parseLong(stringId));

    Request<Greeting> getRequest = builders.get().id(id).build();
    Response<Greeting> getResponse = restClient.sendRequest(getRequest).getResponse();
    Assert.assertEquals(getResponse.getHeader(RestConstants.HEADER_CONTENT_TYPE), expectedContentType);
    Greeting responseGreeting = getResponse.getEntity();

    Assert.assertEquals(responseGreeting.getMessage(), greeting.getMessage());
    Assert.assertEquals(responseGreeting.getTone(), greeting.getTone());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientDataDataProvider")
  public void testUpdate(RestClient restClient, String expectedContentType, RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException, CloneNotSupportedException
  {
    // GET
    Request<Greeting> request = builders.get().id(1L).build();
    Response<Greeting> greetingResponse1 = restClient.sendRequest(request).getResponse();
    Assert.assertEquals(greetingResponse1.getHeader("Content-Type"), expectedContentType);

    String response1 = greetingResponse1.getEntity().getMessage();
    Assert.assertNotNull(response1);

    // POST
    Greeting greeting = new Greeting(greetingResponse1.getEntity().data().copy());
    greeting.setMessage(response1 + "Again");

    Request<EmptyRecord> writeRequest = builders.update().id(1L).input(greeting).build();
    Response<EmptyRecord> updateResponse = restClient.sendRequest(writeRequest).getResponse();
    Assert.assertNull(updateResponse.getHeader(RestConstants.HEADER_CONTENT_TYPE));

    // GET again, to verify that our POST worked.
    Request<Greeting> request2 = builders.get().id(1L).build();
    Response<Greeting> greetingResponse2 = restClient.sendRequest(request2).getResponse();
    Assert.assertEquals(greetingResponse2.getHeader("Content-Type"), expectedContentType);

    String response2 = greetingResponse2.getEntity().getMessage();
    Assert.assertEquals(response2, response1 + "Again");
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientDataDataProvider")
  public Object[][] clientDataDataProvider()
  {
    // combine oldBuildersClientDataDataProvider and newBuildersClientDataDataProvider and wrap the builders in RootBuilderWrappers
    final int builderIndex = 2;
    Object[][] oldBuildersDataProvider = oldBuildersClientDataDataProvider();
    Object[][] newBuildersDataProvider = newBuildersClientDataDataProvider();
    Object[][] result = new Object[oldBuildersDataProvider.length + newBuildersDataProvider.length][];

    int currResultIndex = 0;
    for (Object[] arguments : (Object[][]) ArrayUtils.addAll(oldBuildersDataProvider, newBuildersDataProvider))
    {
      Object[] newArguments = arguments;
      newArguments[builderIndex] = new RootBuilderWrapper<Long, Greeting>(newArguments[builderIndex]);
      result[currResultIndex] = newArguments;
      currResultIndex++;
    }

    return result;
  }

  @SuppressWarnings("deprecation")
  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "oldBuildersClientDataDataProvider")
  public Object[][] oldBuildersClientDataDataProvider()
  {
    return new Object[][]
      {
        { new RestClient(getDefaultTransportClient(), URI_PREFIX), "application/json", new GreetingsBuilders() }, // default client
        { new RestClient(getDefaultTransportClient(), URI_PREFIX), "application/json", new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS) }, // default client
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Collections.singletonList(RestClient.AcceptType.PSON)),
          "application/x-pson",
          new GreetingsBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Collections.singletonList(RestClient.AcceptType.PSON)),
          "application/x-pson",
          new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Collections.singletonList(RestClient.AcceptType.PSON)),
          "application/x-pson",
          new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Collections.singletonList(RestClient.AcceptType.JSON)),
          "application/json",
          new GreetingsBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Collections.singletonList(RestClient.AcceptType.JSON)),
          "application/json",
          new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(), URI_PREFIX,
                         Collections.singletonList(
                           RestClient.AcceptType.ANY)),
          "application/json",
          new GreetingsBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(), URI_PREFIX,
                         Collections.singletonList(
                           RestClient.AcceptType.ANY)),
          "application/json",
          new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.JSON)),
          "application/x-pson",
          new GreetingsBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.JSON)),
          "application/x-pson",
          new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.JSON,RestClient.AcceptType.PSON)),
          "application/json",
          new GreetingsBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.JSON,RestClient.AcceptType.PSON)),
          "application/json",
          new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.ANY)),
          "application/x-pson",
          new GreetingsBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.ANY)),
          "application/x-pson",
          new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.ANY)),
          "application/json",
          new GreetingsBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.ANY)),
          "application/json",
          new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.PSON)),
          "application/x-pson",
          new GreetingsBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.PSON)),
          "application/x-pson",
          new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.JSON)),
          "application/json",
          new GreetingsBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.JSON)),
          "application/json",
          new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        // accept types and content types
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.JSON, Collections.<RestClient.AcceptType>emptyList()),
          "application/json",
          new GreetingsBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.JSON, Collections.<RestClient.AcceptType>emptyList()),
          "application/json",
          new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.JSON, Collections.singletonList(RestClient.AcceptType.JSON)),
          "application/json",
          new GreetingsBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.JSON, Collections.singletonList(RestClient.AcceptType.JSON)),
          "application/json",
          new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.JSON, Collections.singletonList(RestClient.AcceptType.PSON)),
          "application/x-pson",
          new GreetingsBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.JSON, Collections.singletonList(RestClient.AcceptType.PSON)),
          "application/x-pson",
          new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.PSON, Collections.<RestClient.AcceptType>emptyList()),
          "application/json",
          new GreetingsBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.PSON, Collections.<RestClient.AcceptType>emptyList()),
          "application/json",
          new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.PSON, Collections.singletonList(RestClient.AcceptType.JSON)),
          "application/json",
          new GreetingsBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.PSON, Collections.singletonList(RestClient.AcceptType.JSON)),
          "application/json",
          new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.PSON, Collections.singletonList(RestClient.AcceptType.PSON)),
          "application/x-pson",
          new GreetingsBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.PSON, Collections.singletonList(RestClient.AcceptType.PSON)),
          "application/x-pson",
          new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        }
      };
  }

  @SuppressWarnings("deprecation")
  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "newBuildersClientDataDataProvider")
  public Object[][] newBuildersClientDataDataProvider()
  {
    return new Object[][]
      {
        { new RestClient(getDefaultTransportClient(), URI_PREFIX), "application/json", new GreetingsRequestBuilders() }, // default client
        { new RestClient(getDefaultTransportClient(), URI_PREFIX), "application/json", new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS) }, // default client
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Collections.singletonList(RestClient.AcceptType.PSON)),
          "application/x-pson",
          new GreetingsRequestBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Collections.singletonList(RestClient.AcceptType.PSON)),
          "application/x-pson",
          new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Collections.singletonList(RestClient.AcceptType.PSON)),
          "application/x-pson",
          new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Collections.singletonList(RestClient.AcceptType.JSON)),
          "application/json",
          new GreetingsRequestBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Collections.singletonList(RestClient.AcceptType.JSON)),
          "application/json",
          new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(), URI_PREFIX,
                         Collections.singletonList(
                           RestClient.AcceptType.ANY)),
          "application/json",
          new GreetingsRequestBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(), URI_PREFIX,
                         Collections.singletonList(
                           RestClient.AcceptType.ANY)),
          "application/json",
          new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.JSON)),
          "application/x-pson",
          new GreetingsRequestBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.JSON)),
          "application/x-pson",
          new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.JSON,RestClient.AcceptType.PSON)),
          "application/json",
          new GreetingsRequestBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.JSON,RestClient.AcceptType.PSON)),
          "application/json",
          new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.ANY)),
          "application/x-pson",
          new GreetingsRequestBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.ANY)),
          "application/x-pson",
          new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.ANY)),
          "application/json",
          new GreetingsRequestBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.ANY)),
          "application/json",
          new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.PSON)),
          "application/x-pson",
          new GreetingsRequestBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.PSON)),
          "application/x-pson",
          new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.JSON)),
          "application/json",
          new GreetingsRequestBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.JSON)),
          "application/json",
          new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        // accept types and content types
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.JSON, Collections.<RestClient.AcceptType>emptyList()),
          "application/json",
          new GreetingsRequestBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.JSON, Collections.<RestClient.AcceptType>emptyList()),
          "application/json",
          new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.JSON, Collections.singletonList(RestClient.AcceptType.JSON)),
          "application/json",
          new GreetingsRequestBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.JSON, Collections.singletonList(RestClient.AcceptType.JSON)),
          "application/json",
          new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.JSON, Collections.singletonList(RestClient.AcceptType.PSON)),
          "application/x-pson",
          new GreetingsRequestBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.JSON, Collections.singletonList(RestClient.AcceptType.PSON)),
          "application/x-pson",
          new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.PSON, Collections.<RestClient.AcceptType>emptyList()),
          "application/json",
          new GreetingsRequestBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.PSON, Collections.<RestClient.AcceptType>emptyList()),
          "application/json",
          new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.PSON, Collections.singletonList(RestClient.AcceptType.JSON)),
          "application/json",
          new GreetingsRequestBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.PSON, Collections.singletonList(RestClient.AcceptType.JSON)),
          "application/json",
          new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.PSON, Collections.singletonList(RestClient.AcceptType.PSON)),
          "application/x-pson",
          new GreetingsRequestBuilders()
        },
        {
          new RestClient(getDefaultTransportClient(),
                         URI_PREFIX,
                         RestClient.ContentType.PSON, Collections.singletonList(RestClient.AcceptType.PSON)),
          "application/x-pson",
          new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)
        }
      };
  }

}
