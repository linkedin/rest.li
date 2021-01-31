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


import com.linkedin.common.callback.FutureCallback;
import com.linkedin.data.DataMap;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.client.BatchCreateIdRequest;
import com.linkedin.restli.client.BatchCreateIdRequestBuilder;
import com.linkedin.restli.client.BatchGetEntityRequest;
import com.linkedin.restli.client.BatchGetEntityRequestBuilder;
import com.linkedin.restli.client.CreateIdRequestBuilder;
import com.linkedin.restli.client.GetRequestBuilder;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.client.base.*;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.client.util.PatchGenerator;
import com.linkedin.restli.common.BatchCreateIdResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.IdResponse;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.api.TwoPartKey;
import com.linkedin.restli.examples.greetings.client.*;
import com.linkedin.restli.internal.server.util.DataMapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestComplexKeysResource extends RestLiIntegrationTest
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

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testGet(ComplexKeysRequestBuilders builders) throws RemoteInvocationException
  {
    testGetMain(builders.get());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestSubBuilderDataProvider")
  public void testSubGet(ComplexKeysSubRequestBuilders builders) throws ExecutionException, InterruptedException
  {
    TwoPartKey key = new TwoPartKey();
    key.setMajor("a");
    key.setMinor("b");
    TwoPartKey param = new TwoPartKey();
    param.setMajor("c");
    param.setMinor("d");
    ComplexResourceKey<TwoPartKey, TwoPartKey> complexKey = new ComplexResourceKey<TwoPartKey, TwoPartKey>(key, param);
    Request<TwoPartKey> request = builders.get().keysKey(complexKey).id("stringKey").build();
    TwoPartKey response = getClient().sendRequest(request).get().getEntity();
    Assert.assertEquals(response.getMajor(), "aANDc");
    Assert.assertEquals(response.getMinor(), "bANDd");
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestSubBuilderDataProvider")
  public void testSubGetWithReservedChars(ComplexKeysSubRequestBuilders builders) throws ExecutionException, InterruptedException
  {
    TwoPartKey key = new TwoPartKey();
    key.setMajor("a&1");
    key.setMinor("b&2");
    TwoPartKey param = new TwoPartKey();
    param.setMajor("c&3");
    param.setMinor("d&4");
    ComplexResourceKey<TwoPartKey, TwoPartKey> complexKey = new ComplexResourceKey<TwoPartKey, TwoPartKey>(key, param);
    Request<TwoPartKey> request = builders.get().keysKey(complexKey).id("stringKey").build();
    TwoPartKey response = getClient().sendRequest(request).get().getEntity();
    Assert.assertEquals(response.getMajor(), "a&1ANDc&3");
    Assert.assertEquals(response.getMinor(), "b&2ANDd&4");
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestAnnotatedBuilderDataProvider")
  public void testPromiseGet(AnnotatedComplexKeysRequestBuilders builders) throws RemoteInvocationException
  {
    testGetMain(builders.get());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  public void testCreate(RestliRequestOptions requestOptions) throws RemoteInvocationException
  {
    final ComplexKeysRequestBuilders builders = new ComplexKeysRequestBuilders(requestOptions);
    testCreateMainBuilders(builders.create(), builders.get());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  public void testPromiseCreate(RestliRequestOptions requestOptions) throws RemoteInvocationException
  {
    final AnnotatedComplexKeysRequestBuilders builders = new AnnotatedComplexKeysRequestBuilders(requestOptions);
    testCreateMainBuilders(builders.create(), builders.get());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  public void testBatchCreate(RestliRequestOptions requestOptions) throws RemoteInvocationException
  {
    final ComplexKeysRequestBuilders builders = new ComplexKeysRequestBuilders(requestOptions);
    testBatchCreateMain(builders.batchCreate(), builders.batchGet());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testPartialUpdate(ComplexKeysRequestBuilders builders) throws RemoteInvocationException
  {
    testPartialUpdateMain(builders.partialUpdate(), builders.get());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestAnnotatedBuilderDataProvider")
  public void testPromisePartialUpdate(AnnotatedComplexKeysRequestBuilders builders) throws RemoteInvocationException
  {
    testPartialUpdateMain(builders.partialUpdate(), builders.get());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testFinder(ComplexKeysRequestBuilders builders) throws RemoteInvocationException
  {
    testFinderMain(builders.findByPrefix().prefixParam(StringTestKeys.SIMPLEKEY));
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestAnnotatedBuilderDataProvider")
  public void testPromiseFinder(AnnotatedComplexKeysRequestBuilders builders) throws RemoteInvocationException
  {
    testFinderMain(builders.findByPrefix().prefixParam(StringTestKeys.SIMPLEKEY));
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  public void testBatchGet(RestliRequestOptions options) throws RemoteInvocationException
  {
    testBatchGetMain(new ComplexKeysRequestBuilders(options).batchGet());
  }

  // this test will only pass in Rest.li protocol version 2.0 or above
  @Test
  public void testBatchGetEmpty() throws Exception
  {
    testBatchGetEmpty(new ComplexKeysRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS).batchGet());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  public void testPromiseBatchGet(RestliRequestOptions options) throws RemoteInvocationException
  {
    testBatchGetMain(new AnnotatedComplexKeysRequestBuilders(options).batchGet());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testBatchUpdate(ComplexKeysRequestBuilders builders) throws RemoteInvocationException
  {
    testBatchUpdateMain(builders.batchUpdate(), new ComplexKeysRequestBuilders(builders.getRequestOptions()).batchGet());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestAnnotatedBuilderDataProvider")
  public void testPromiseBatchUpdate(AnnotatedComplexKeysRequestBuilders builders) throws RemoteInvocationException
  {
    testBatchUpdateMain(builders.batchUpdate(), new AnnotatedComplexKeysRequestBuilders(builders.getRequestOptions()).batchGet());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testBatchPartialUpdate(ComplexKeysRequestBuilders builders) throws RemoteInvocationException
  {
    final RestliRequestOptions requestOptions = builders.getRequestOptions();
    testBatchPartialUpdateMain(builders.batchPartialUpdate(), new ComplexKeysRequestBuilders(requestOptions).batchGet());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestAnnotatedBuilderDataProvider")
  public void testPromiseBatchPartialUpdate(AnnotatedComplexKeysRequestBuilders builders) throws RemoteInvocationException
  {
    testBatchPartialUpdateMain(builders.batchPartialUpdate(), new AnnotatedComplexKeysRequestBuilders(builders.getRequestOptions()).batchGet());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testBatchDelete(ComplexKeysRequestBuilders builders) throws RemoteInvocationException
  {
    final RestliRequestOptions requestOptions = builders.getRequestOptions();
    testBatchDeleteMain(builders.batchDelete(), builders.create(), new ComplexKeysRequestBuilders(requestOptions).batchGet());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestAnnotatedBuilderDataProvider")
  public void testPromiseBatchDelete(AnnotatedComplexKeysRequestBuilders builders) throws RemoteInvocationException
  {
    testBatchDeleteMain(builders.batchDelete(), builders.create(), new ComplexKeysRequestBuilders(builders.getRequestOptions()).batchGet());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testEntityAction(ComplexKeysRequestBuilders builders) throws RemoteInvocationException
  {
    ComplexResourceKey<TwoPartKey, TwoPartKey> key = getComplexKey("major", "minor");
    Request<Integer> actionRequest = builders.actionEntityAction().id(key).build();
    Integer entity = getClient().sendRequest(actionRequest).getResponse().getEntity();
    Assert.assertEquals(entity.longValue(), 1L);
  }

  private void testGetMain(GetRequestBuilderBase<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message, ?> requestBuilder) throws RemoteInvocationException
  {
    Request<Message> request = requestBuilder.id(getComplexKey(
        StringTestKeys.SIMPLEKEY, StringTestKeys.SIMPLEKEY2)).build();
    ResponseFuture<Message> future = getClient().sendRequest(request);
    Response<Message> response = future.getResponse();

    Assert.assertEquals(response.getEntity().getMessage(), StringTestKeys.SIMPLEKEY + " " + StringTestKeys.SIMPLEKEY2);
  }

  @SuppressWarnings("deprecation")
  private void testCreateMainBuilders(CreateIdRequestBuilder<ComplexResourceKey<TwoPartKey, TwoPartKey> , Message> createRequestBuilder,
                                      GetRequestBuilder<ComplexResourceKey<TwoPartKey, TwoPartKey> , Message> getRequestBuilder) throws RemoteInvocationException
  {
    final String messageText = "newMessage";
    Message message = new Message();
    message.setMessage(messageText);

    Request<IdResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>>> request = createRequestBuilder.input(message).build();
    ResponseFuture<IdResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>>> future = getClient().sendRequest(request);
    Response<IdResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>>> response = future.getResponse();
    try
    {
      response.getId();
      Assert.fail("getId() for a complex key is not allowed!");
    }
    catch (UnsupportedOperationException e)
    {
      // expected
    }

    Assert.assertEquals(response.getStatus(), 201);
    IdResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>> entity = response.getEntity();
    ComplexResourceKey<TwoPartKey, TwoPartKey> id = entity.getId();
    Assert.assertEquals(id, getComplexKey(messageText, messageText));

    // attempt to get the record you just created
    @SuppressWarnings("unchecked")
    Request<Message> getRequest = getRequestBuilder.id(id).build();
    ResponseFuture<Message> getFuture = getClient().sendRequest(getRequest);
    Response<Message> getResponse = getFuture.getResponse();

    Assert.assertEquals(getResponse.getEntity().getMessage(), messageText);
  }

  private void testBatchCreateMain(BatchCreateIdRequestBuilder<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchCreateRequestBuilder,
                                   BatchGetEntityRequestBuilder<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchGetRequestBuilder)
    throws RemoteInvocationException
  {
    final String messageText1 = "firstMessage";
    Message message1 = new Message();
    message1.setMessage(messageText1);
    final String messageText2 = "secondMessage";
    Message message2 = new Message();
    message2.setMessage(messageText2);
    List<Message> messages = new ArrayList<Message>(2);
    messages.add(message1);
    messages.add(message2);

    ComplexResourceKey<TwoPartKey, TwoPartKey> expectedComplexKey1 = getComplexKey(messageText1, messageText1);
    ComplexResourceKey<TwoPartKey, TwoPartKey> expectedComplexKey2 = getComplexKey(messageText2, messageText2);

    // test build
    BatchCreateIdRequest<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> request = batchCreateRequestBuilder.inputs(messages).build();
    Response<BatchCreateIdResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>>> response = getClient().sendRequest(request).getResponse();
    Assert.assertEquals(response.getStatus(), 200);
    Set<ComplexResourceKey<TwoPartKey, TwoPartKey>> expectedComplexKeys = new HashSet<ComplexResourceKey<TwoPartKey, TwoPartKey>>(2);
    expectedComplexKeys.add(expectedComplexKey1);
    expectedComplexKeys.add(expectedComplexKey2);
    for (CreateIdStatus<ComplexResourceKey<TwoPartKey, TwoPartKey>> status : response.getEntity().getElements())
    {
      Assert.assertEquals(status.getStatus(), new Integer(201));
      Assert.assertTrue(expectedComplexKeys.contains(status.getKey()));

      try
      {
        @SuppressWarnings("deprecation")
        String id = status.getId();
        Assert.fail("buildReadOnlyId should throw an exception for ComplexKeys");
      }
      catch (UnsupportedOperationException e)
      {
        // expected
      }

      expectedComplexKeys.remove(status.getKey());
    }
    Assert.assertTrue(expectedComplexKeys.isEmpty());

    // attempt to batch get created records
    List<ComplexResourceKey<TwoPartKey, TwoPartKey>> createdKeys = new ArrayList<ComplexResourceKey<TwoPartKey, TwoPartKey>>(2);
    createdKeys.add(expectedComplexKey1);
    createdKeys.add(expectedComplexKey2);
    Request<BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, EntityResponse<Message>>> getRequest = batchGetRequestBuilder.ids(createdKeys).build();
    ResponseFuture<BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, EntityResponse<Message>>> getFuture = getClient().sendRequest(getRequest);
    Response<BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, EntityResponse<Message>>> getResponse = getFuture.getResponse();
    Map<ComplexResourceKey<TwoPartKey, TwoPartKey>, EntityResponse<Message>> getResults = getResponse.getEntity().getResults();
    Assert.assertEquals(getResults.get(expectedComplexKey1).getEntity(), message1);
    Assert.assertEquals(getResults.get(expectedComplexKey2).getEntity(), message2);
    Assert.assertEquals(getResults.size(), 2);
  }

  private void testPartialUpdateMain(
      PartialUpdateRequestBuilderBase<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message, ?> updateRequestBuilder,
      GetRequestBuilderBase<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message, ?> getRequestBuilder) throws RemoteInvocationException
  {
    final String newMessage = "newMessage";
    ComplexResourceKey<TwoPartKey, TwoPartKey> key = getComplexKey(StringTestKeys.SIMPLEKEY, StringTestKeys.SIMPLEKEY2);
    doPartialUpdate(updateRequestBuilder, key, newMessage);

    Request<Message> getRequest = getRequestBuilder.id(key).build();
    ResponseFuture<Message> getFuture = getClient().sendRequest(getRequest);
    Response<Message> getResponse = getFuture.getResponse();

    Assert.assertEquals(getResponse.getEntity().getMessage(), newMessage);

    doPartialUpdate(updateRequestBuilder, key, StringTestKeys.SIMPLEKEY + " " + StringTestKeys.SIMPLEKEY2);
  }

  private ComplexResourceKey<TwoPartKey, TwoPartKey> doPartialUpdate(
      PartialUpdateRequestBuilderBase<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message, ?> updateRequestBuilder,
      ComplexResourceKey<TwoPartKey, TwoPartKey> key,
      String newMessage) throws RemoteInvocationException
  {
    Message message = new Message();
    message.setMessage(newMessage);

    PatchRequest<Message> patch = PatchGenerator.diffEmpty(message);
    Request<EmptyRecord> request = updateRequestBuilder.id(key).input(patch).build();
    ResponseFuture<EmptyRecord> future = getClient().sendRequest(request);
    Response<EmptyRecord> response = future.getResponse();
    Assert.assertEquals(response.getStatus(), 204);
    return key;
  }

  private void testFinderMain(
      FindRequestBuilderBase<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message, ?> requestBuilder)  throws RemoteInvocationException
  {
    Request<CollectionResponse<Message>> request = requestBuilder.build();
    ResponseFuture<CollectionResponse<Message>> future = getClient().sendRequest(request);
    CollectionResponse<Message> response = future.getResponse().getEntity();

    List<Message> results = response.getElements();
    Assert.assertEquals(results.size(), 1);
    Assert.assertEquals(results.get(0).getMessage(), StringTestKeys.SIMPLEKEY + " " + StringTestKeys.SIMPLEKEY2);
  }

  public void testBatchGetMain(BatchGetEntityRequestBuilder<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> builder) throws RemoteInvocationException
  {
    List<ComplexResourceKey<TwoPartKey, TwoPartKey>> ids = getBatchComplexKeys();
    Request<BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, EntityResponse<Message>>> request = builder.ids(ids).build();
    ResponseFuture<BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, EntityResponse<Message>>> future =
      getClient().sendRequest(request);
    BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, EntityResponse<Message>> response =
      future.getResponse().getEntity();

    Assert.assertEquals(response.getResults().size(), 3);
    Assert.assertNotNull(response.getResults().get(ids.get(0)).getEntity());
    Assert.assertNotNull(response.getResults().get(ids.get(1)).getEntity());
    Assert.assertNotNull(response.getResults().get(ids.get(2)).getError());
  }

  @SuppressWarnings("deprecation")
  public void testBatchGetEmpty(BatchGetEntityRequestBuilder<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> builder)
    throws Exception
  {
    final Request<BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, EntityResponse<Message>>> request = builder.build();
    final FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();
    getClient().sendRestRequest(request, new RequestContext(), callback);
    final RestResponse result = callback.get();

    final DataMap responseMap = DataMapUtils.readMap(result);
    final DataMap resultsMap = responseMap.getDataMap(BatchKVResponse.RESULTS);
    Assert.assertNotNull(resultsMap, "Response does not contain results map");
    Assert.assertTrue(resultsMap.isEmpty());
  }

  private void testBatchUpdateMain(
      BatchUpdateRequestBuilderBase<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message, ?> requestBuilder,
      BatchGetEntityRequestBuilder<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchGetRequestBuilder)
      throws RemoteInvocationException
  {
    final String messageText = StringTestKeys.SIMPLEKEY + " " + StringTestKeys.SIMPLEKEY2;
    final Message message = new Message();
    message.setId(StringTestKeys.SIMPLEKEY + " " + StringTestKeys.SIMPLEKEY2);
    message.setMessage(messageText);
    message.setTone(Tone.INSULTING);
    final String messageText2 = StringTestKeys.URL + " " + StringTestKeys.URL2;
    final Message message2 = new Message();
    message2.setId(StringTestKeys.URL + " " + StringTestKeys.URL2);
    message2.setMessage(messageText2);
    message2.setTone(Tone.INSULTING);

    final Map<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> inputs = new HashMap<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message>();
    ComplexResourceKey<TwoPartKey, TwoPartKey> key1 = getComplexKey(StringTestKeys.SIMPLEKEY, StringTestKeys.SIMPLEKEY2);
    ComplexResourceKey<TwoPartKey, TwoPartKey> key2 = getComplexKey(StringTestKeys.URL, StringTestKeys.URL2);
    ComplexResourceKey<TwoPartKey, TwoPartKey> key3 = getComplexKey(StringTestKeys.ERROR, StringTestKeys.ERROR);
    inputs.put(key1, message);
    inputs.put(key2, message2);
    inputs.put(key3, message); // key3 should error anyway, so message is irrelevant.
    final Request<BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus>> request =
        requestBuilder.inputs(inputs).build();
    final ResponseFuture<BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus>> future =
        getClient().sendRequest(request);
    final BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus> response =
        future.getResponse().getEntity();

    Assert.assertEquals(response.getResults().size(), inputs.size());
    for (Map.Entry<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus> resp : response.getResults().entrySet())
    {
      Assert.assertTrue(inputs.containsKey(resp.getKey()));
      final UpdateStatus status = resp.getValue();
      if (status.hasError())
      {
        Assert.assertTrue(status.getStatus() == status.getError().getStatus(), "Update status should match the status of the error, if there is any.");
      }
      else
      {
        Assert.assertEquals((int) status.getStatus(), 200);
      }
    }

    Assert.assertNotNull(response.getResults().get(key1));
    Assert.assertNotNull(response.getResults().get(key2));

    Assert.assertNotNull(response.getErrors().get(key3));

    ArrayList<ComplexResourceKey<TwoPartKey, TwoPartKey>> ids = new ArrayList<ComplexResourceKey<TwoPartKey, TwoPartKey>>();
    ids.add(key1);
    ids.add(key2);
    BatchGetEntityRequest<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchGetRequest =
        batchGetRequestBuilder.ids(ids).build();
    ResponseFuture<BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, EntityResponse<Message>>> batchGetFuture =
        getClient().sendRequest(batchGetRequest);
    BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, EntityResponse<Message>> batchGetResponse =
        batchGetFuture.getResponse().getEntity();

    Assert.assertEquals(batchGetResponse.getResults().get(key1).getEntity().getTone(), Tone.INSULTING);
    Assert.assertEquals(batchGetResponse.getResults().get(key2).getEntity().getTone(), Tone.INSULTING);
  }

  private void testBatchPartialUpdateMain(
      BatchPartialUpdateRequestBuilderBase<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message, ?> requestBuilder,
      BatchGetEntityRequestBuilder<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchGetRequestBuilder)
      throws RemoteInvocationException
  {
    Message message = new Message();
    message.setTone(Tone.FRIENDLY);

    PatchRequest<Message> patch = PatchGenerator.diffEmpty(message);

    final Map<ComplexResourceKey<TwoPartKey, TwoPartKey>, PatchRequest<Message>> inputs =
        new HashMap<ComplexResourceKey<TwoPartKey, TwoPartKey>, PatchRequest<Message>>();
    ComplexResourceKey<TwoPartKey, TwoPartKey> key1 = getComplexKey(StringTestKeys.SIMPLEKEY, StringTestKeys.SIMPLEKEY2);
    ComplexResourceKey<TwoPartKey, TwoPartKey> key2 = getComplexKey(StringTestKeys.URL, StringTestKeys.URL2);
    inputs.put(key1, patch);
    inputs.put(key2, patch);
    final Request<BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus>> request =
        requestBuilder.inputs(inputs).build();

    final ResponseFuture<BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus>> future =
        getClient().sendRequest(request);
    final BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus> response =
        future.getResponse().getEntity();

    for (Map.Entry<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus> resp : response.getResults().entrySet())
    {
      Assert.assertTrue(inputs.containsKey(resp.getKey()));
      Assert.assertEquals(resp.getValue().getStatus().intValue(), 204);
    }

    Assert.assertNotNull(response.getResults().get(key1));
    Assert.assertNotNull(response.getResults().get(key2));

    Assert.assertTrue(response.getErrors().isEmpty());

    ArrayList<ComplexResourceKey<TwoPartKey, TwoPartKey>> ids = new ArrayList<ComplexResourceKey<TwoPartKey, TwoPartKey>>();
    ids.add(key1);
    ids.add(key2);
    Request<BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, EntityResponse<Message>>> batchGetRequest =
        batchGetRequestBuilder.ids(ids).build();
    ResponseFuture<BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, EntityResponse<Message>>> batchGetFuture =
        getClient().sendRequest(batchGetRequest);
    BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, EntityResponse<Message>> batchGetResponse =
        batchGetFuture.getResponse().getEntity();

    Assert.assertEquals(batchGetResponse.getResults().get(key1).getEntity().getTone(), Tone.FRIENDLY);
    Assert.assertEquals(batchGetResponse.getResults().get(key2).getEntity().getTone(), Tone.FRIENDLY);
  }

  private void testBatchDeleteMain(
      BatchDeleteRequestBuilderBase<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message, ?> requestBuilder,
      CreateIdRequestBuilderBase<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message, ?> createRequestBuilder,
      BatchGetEntityRequestBuilder<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchGetRequestBuilder)
      throws RemoteInvocationException
  {
    String messageText = "m1";
    Message message = new Message();
    message.setMessage(messageText);

    Request<?> createRequest = createRequestBuilder.input(message).build();
    ResponseFuture<?> createFuture = getClient().sendRequest(createRequest);
    Response<?> createResponse = createFuture.getResponse();
    Assert.assertEquals(createResponse.getStatus(), 201);

    String messageText2 = "m2";
    message.setMessage(messageText2);

    createRequest = createRequestBuilder.input(message).build();
    createFuture = getClient().sendRequest(createRequest);
    createResponse = createFuture.getResponse();
    Assert.assertEquals(createResponse.getStatus(), 201);

    ComplexResourceKey<TwoPartKey, TwoPartKey> key1 = getComplexKey(messageText, messageText);
    ComplexResourceKey<TwoPartKey, TwoPartKey> key2 = getComplexKey(messageText2, messageText2);

    ArrayList<ComplexResourceKey<TwoPartKey, TwoPartKey>> ids = new ArrayList<ComplexResourceKey<TwoPartKey, TwoPartKey>>();
    ids.add(key1);
    ids.add(key2);
    final Request<BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus>> request =
        requestBuilder.ids(ids).build();

    final ResponseFuture<BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus>> future =
        getClient().sendRequest(request);
    final BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus> response =
        future.getResponse().getEntity();

    for (Map.Entry<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus> resp : response.getResults().entrySet())
    {
      Assert.assertEquals(resp.getValue().getStatus().intValue(), 204);
    }

    Assert.assertNotNull(response.getResults().get(key1));
    Assert.assertNotNull(response.getResults().get(key2));

    Request<BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, EntityResponse<Message>>> batchGetRequest =
        batchGetRequestBuilder.ids(ids).build();
    ResponseFuture<BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, EntityResponse<Message>>> batchGetFuture =
        getClient().sendRequest(batchGetRequest);
    BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, EntityResponse<Message>> batchGetResponse =
        batchGetFuture.getResponse().getEntity();

    Assert.assertEquals(batchGetResponse.getResults().size(), batchGetResponse.getErrors().size());
  }

  private static List<ComplexResourceKey<TwoPartKey, TwoPartKey>> getBatchComplexKeys()
  {
    List<ComplexResourceKey<TwoPartKey, TwoPartKey>> ids =
      new ArrayList<ComplexResourceKey<TwoPartKey, TwoPartKey>>();
    ComplexResourceKey<TwoPartKey, TwoPartKey> key1 = getComplexKey(StringTestKeys.SIMPLEKEY, StringTestKeys.SIMPLEKEY2);
    ComplexResourceKey<TwoPartKey, TwoPartKey> key2 = getComplexKey(StringTestKeys.URL, StringTestKeys.URL2);
    ComplexResourceKey<TwoPartKey, TwoPartKey> key3 = getComplexKey(StringTestKeys.ERROR, StringTestKeys.ERROR);
    ids.add(key1);
    ids.add(key2);
    ids.add(key3);

    return ids;
  }

  private static ComplexResourceKey<TwoPartKey, TwoPartKey> getComplexKey(String major, String minor)
  {
    return new ComplexResourceKey<TwoPartKey, TwoPartKey>(
        new TwoPartKey().setMajor(major).setMinor(minor),
        new TwoPartKey());
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  private static Object[][] requestOptionsDataProvider()
  {
    return new Object[][] {
      { RestliRequestOptions.DEFAULT_OPTIONS },
      { TestConstants.FORCE_USE_NEXT_OPTIONS }
    };
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  private static Object[][] requestBuilderDataProvider()
  {
    return new Object[][] {
      { new ComplexKeysRequestBuilders() },
      { new ComplexKeysRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS) }
    };
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestSubBuilderDataProvider")
  private static Object[][] requestSubBuilderDataProvider()
  {
    return new Object[][] {
      { new ComplexKeysSubRequestBuilders() },
      { new ComplexKeysSubRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS) }
    };
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestAnnotatedBuilderDataProvider")
  private static Object[][] requestAnnotatedBuilderDataProvider()
  {
    return new Object[][] {
      { new AnnotatedComplexKeysRequestBuilders() },
      { new AnnotatedComplexKeysRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS) }
    };
  }
}
