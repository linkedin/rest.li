/*
   Copyright (c) 2013 LinkedIn Corp.

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
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.RequestBuilder;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.BatchCollectionResponse;
import com.linkedin.restli.common.BatchFinderCriteriaResult;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.MessageCriteria;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.AssociationsBuilders;
import com.linkedin.restli.examples.greetings.client.AssociationsRequestBuilders;
import com.linkedin.restli.examples.greetings.client.AssociationsSubBuilders;
import com.linkedin.restli.examples.greetings.client.AssociationsSubRequestBuilders;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.restli.examples.AssociationResourceHelpers.DB;
import static com.linkedin.restli.examples.AssociationResourceHelpers.SIMPLE_COMPOUND_KEY;
import static com.linkedin.restli.examples.AssociationResourceHelpers.URL_COMPOUND_KEY;


public class TestAssociationsResource extends RestLiIntegrationTest
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

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider", expectedExceptions = UnsupportedOperationException.class)
  @SuppressWarnings("deprecation")
  public void testCreate(RootBuilderWrapper<CompoundKey, Message> builders) throws Exception
  {
    // Associations should never support create operations. This is a bug in Rest.li that will be fixed. For now we want
    // to make sure that creating and then calling getId() on the response throws an exception.
    Request<EmptyRecord> request = builders.create().input(new Message().setMessage("foo")).build();
    getClient().sendRequest(request).getResponse().getId();
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testOptionalAssociationKeyInFinder(RootBuilderWrapper<CompoundKey, Message> builders) throws Exception
  {
    // optional and present
    RequestBuilder<? extends Request<CollectionResponse<Message>>> finder = builders.findBy("AssocKeyFinderOpt").assocKey("src", "KEY1").getBuilder();
    Assert.assertEquals(200, getClient().sendRequest(finder).getResponse().getStatus());

    // optional and not present
    RequestBuilder<? extends Request<CollectionResponse<Message>>> finderNoAssocKey = builders.findBy("AssocKeyFinderOpt").getBuilder();
    Assert.assertEquals(200, getClient().sendRequest(finderNoAssocKey).getResponse().getStatus());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testRequiredAssociationKeyInFinder(RootBuilderWrapper<CompoundKey, Message> builders) throws Exception
  {
    // required and present
    RequestBuilder<? extends Request<CollectionResponse<Message>>> finder = builders.findBy("AssocKeyFinder").assocKey("src", "KEY1").getBuilder();
    Assert.assertEquals(200, getClient().sendRequest(finder).getResponse().getStatus());

    // required and not present
    RequestBuilder<? extends Request<CollectionResponse<Message>>> finderNoAssocKey = builders.findBy("AssocKeyFinder").getBuilder();
    try
    {
      getClient().sendRequest(finderNoAssocKey).getResponse();
      Assert.fail("Calling a finder without a required association key should throw RestLiResponseException with status code 400");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(400, e.getStatus());
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testBatchFinder(RootBuilderWrapper<CompoundKey, Message> builders) throws RemoteInvocationException
  {
    MessageCriteria m1 = new MessageCriteria().setMessage("hello").setTone(Tone.FRIENDLY);
    MessageCriteria m2 = new MessageCriteria().setMessage("world").setTone(Tone.SINCERE);
    Request<BatchCollectionResponse<Message>> request = builders.batchFindBy("searchMessages").assocKey("src", "KEY1")
        .setQueryParam("criteria", Arrays.asList(m1, m2)).build();
    ResponseFuture<BatchCollectionResponse<Message>> future = getClient().sendRequest(request);
    BatchCollectionResponse<Message> response = future.getResponse().getEntity();

    List<BatchFinderCriteriaResult<Message>> batchResult = response.getResults();
    // on success
    List<Message> messages= batchResult.get(0).getElements();
    Assert.assertTrue(messages.get(0).hasTone());
    Assert.assertTrue(messages.get(0).getTone().equals(Tone.FRIENDLY));

    // on error
    Assert.assertTrue(batchResult.get(1).isError());
    ErrorResponse error = batchResult.get(1).getError();
    Assert.assertEquals(error.getMessage(), "Failed to find message!");
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  public void testBatchGet(RestliRequestOptions requestOptions) throws RemoteInvocationException
  {
    Request<BatchKVResponse<CompoundKey, Message>> request = new AssociationsBuilders(requestOptions).batchGet().ids(DB.keySet()).buildKV();
    ResponseFuture<BatchKVResponse<CompoundKey, Message>> responseFuture = getClient().sendRequest(request);
    Response<BatchKVResponse<CompoundKey, Message>> response = responseFuture.getResponse();

    BatchKVResponse<CompoundKey, Message> entity = response.getEntity();

    Assert.assertEquals(entity.getErrors().size(), 0);
    Assert.assertEquals(entity.getResults().size(), 2);
    for (CompoundKey id: DB.keySet())
    {
      Assert.assertTrue(entity.getResults().containsKey(id));
      Assert.assertEquals(entity.getResults().get(id), DB.get(id));
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  public void testBatchGetEntity(RestliRequestOptions requestOptions) throws RemoteInvocationException
  {
    Request<BatchKVResponse<CompoundKey, EntityResponse<Message>>> request = new AssociationsRequestBuilders(requestOptions).batchGet().ids(DB.keySet()).build();
    ResponseFuture<BatchKVResponse<CompoundKey, EntityResponse<Message>>> responseFuture = getClient().sendRequest(request);
    Response<BatchKVResponse<CompoundKey, EntityResponse<Message>>> response = responseFuture.getResponse();

    BatchKVResponse<CompoundKey, EntityResponse<Message>> entityResponse = response.getEntity();

    Assert.assertEquals(entityResponse.getErrors().size(), 0);
    Assert.assertEquals(entityResponse.getResults().size(), 2);
    for (CompoundKey id: DB.keySet())
    {
      EntityResponse<Message> single = entityResponse.getResults().get(id);
      Assert.assertTrue(entityResponse.getResults().containsKey(id));
      Assert.assertEquals(single.getEntity(), DB.get(id));
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestSubBuilderDataProvider")
  public void testSubresourceGet(RootBuilderWrapper<String, Message> builders) throws RemoteInvocationException
  {
    Request<Message> request = builders.get().setPathKey("dest", "dest").setPathKey("src", "src").id("id").build();
    Message message = getClient().sendRequest(request).getResponse().getEntity();
    Assert.assertEquals(message.getId(), "src");
    Assert.assertEquals(message.getMessage(), "dest");
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestSubBuilderDataProvider")
  public void testSubresourceGetForbiddenCharacters(RootBuilderWrapper<String, Message> builders) throws RemoteInvocationException
  {
    Request<Message> request = builders.get().setPathKey("dest", "d&est").setPathKey("src", "s&rc").id("id").build();
    Message message = getClient().sendRequest(request).getResponse().getEntity();
    Assert.assertEquals(message.getId(), "s&rc");
    Assert.assertEquals(message.getMessage(), "d&est");
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestSubBuilderDataProvider")
  public void testSubresourceFinder(RootBuilderWrapper<String, Message> builders) throws RemoteInvocationException
  {
    Request<CollectionResponse<Message>> request = builders.findBy("Tone").setPathKey("dest", "dest").setPathKey("src", "src").setQueryParam("tone", Tone.FRIENDLY).build();
    List<Message> messages = getClient().sendRequest(request).getResponse().getEntity().getElements();

    for (Message message : messages)
    {
      Assert.assertEquals(message.getTone(), Tone.FRIENDLY);
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestSubBuilderDataProvider")
  public void testSubresourceAction(RootBuilderWrapper<CompoundKey, Message> builders) throws RemoteInvocationException
  {
    Request<Integer> request = builders.<Integer>action("Action").setPathKey("dest", "dest").setPathKey("src", "src").build();
    Integer integer = getClient().sendRequest(request).getResponse().getEntity();

    Assert.assertEquals(integer, Integer.valueOf(1));
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestSubBuilderDataProvider")
  public void testSubresourcePathKeyAction(RootBuilderWrapper<CompoundKey, Message> builders) throws RemoteInvocationException
  {
    Request<String> request = builders.<String>action("GetSource").setPathKey("dest", "dest").setPathKey("src", "src").build();
    String source = getClient().sendRequest(request).getResponse().getEntity();

    Assert.assertEquals(source, "src");
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestSubBuilderDataProvider")
  public void testSubresourcePathKeySingularAction(RootBuilderWrapper<CompoundKey, Message> builders) throws RemoteInvocationException
  {
    String srcValue = "src-test";
    String destValue = "dest-test";
    Request<String> request = builders.<String>action("ConcatenateStrings").setPathKey("dest", destValue).setPathKey("src", srcValue).build();
    String returnValue = getClient().sendRequest(request).getResponse().getEntity();

    Assert.assertEquals(returnValue, srcValue + destValue);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testBatchUpdate(RootBuilderWrapper<CompoundKey, Message> builders)
      throws RemoteInvocationException
  {
    Request<BatchKVResponse<CompoundKey, UpdateStatus>> request = builders.batchUpdate().inputs(DB).build();

    BatchKVResponse<CompoundKey, UpdateStatus> entities =
        getClient().sendRequest(request).getResponse().getEntity();
    runAssertions(entities);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testBatchPartialUpdate(RootBuilderWrapper<CompoundKey, PatchRequest<Message>> builders)
      throws RemoteInvocationException
  {
    Map<CompoundKey, PatchRequest<Message>> patches = new HashMap<>();
    patches.put(URL_COMPOUND_KEY, new PatchRequest<>());
    patches.put(SIMPLE_COMPOUND_KEY, new PatchRequest<>());

    Request<BatchKVResponse<CompoundKey, UpdateStatus>> request = builders.batchPartialUpdate().inputs(patches).build();

    BatchKVResponse<CompoundKey, UpdateStatus> entities =
        getClient().sendRequest(request).getResponse().getEntity();
    runAssertions(entities);
  }

  private void runAssertions(BatchKVResponse<CompoundKey, UpdateStatus> entities)
  {
    Assert.assertEquals(entities.getErrors().size(), 0);
    Assert.assertEquals(entities.getResults().keySet(), DB.keySet());
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  private static Object[][] requestBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<CompoundKey, Message>(new AssociationsBuilders()) },
      { new RootBuilderWrapper<CompoundKey, Message>(new AssociationsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<CompoundKey, Message>(new AssociationsRequestBuilders()) },
      { new RootBuilderWrapper<CompoundKey, Message>(new AssociationsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) }
    };
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  private static Object[][] requestOptionsDataProvider()
  {
    return new Object[][] {
      { RestliRequestOptions.DEFAULT_OPTIONS },
      { TestConstants.FORCE_USE_NEXT_OPTIONS }
    };
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestSubBuilderDataProvider")
  private static Object[][] requestSubBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<String, Message>(new AssociationsSubBuilders()) },
      { new RootBuilderWrapper<String, Message>(new AssociationsSubBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<String, Message>(new AssociationsSubRequestBuilders()) },
      { new RootBuilderWrapper<String, Message>(new AssociationsSubRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) }
    };
  }
}
