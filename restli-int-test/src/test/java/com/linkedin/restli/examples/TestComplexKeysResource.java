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
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.BatchGetKVRequest;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.client.util.PatchGenerator;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.api.TwoPartKey;
import com.linkedin.restli.examples.greetings.client.AnnotatedComplexKeysBuilders;
import com.linkedin.restli.examples.greetings.client.AnnotatedComplexKeysRequestBuilders;
import com.linkedin.restli.examples.greetings.client.ComplexKeysBuilders;
import com.linkedin.restli.examples.greetings.client.ComplexKeysRequestBuilders;
import com.linkedin.restli.examples.greetings.client.ComplexKeysSubBuilders;
import com.linkedin.restli.examples.greetings.client.ComplexKeysSubRequestBuilders;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestComplexKeysResource extends RestLiIntegrationTest
{
  private static final Client CLIENT = new TransportClientAdapter(new HttpClientFactory().getClient(Collections.<String, String>emptyMap()));
  private static final String URI_PREFIX = "http://localhost:1338/";
  private static final RestClient REST_CLIENT = new RestClient(CLIENT, URI_PREFIX);

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

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testGet(RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> builders) throws RemoteInvocationException
  {
    testGetMain(builders.get());
  }

  @Test(dataProvider = "requestSubBuilderDataProvider")
  public void testSubGet(RootBuilderWrapper<String, TwoPartKey> builders) throws ExecutionException, InterruptedException
  {
    TwoPartKey key = new TwoPartKey();
    key.setMajor("a");
    key.setMinor("b");
    TwoPartKey param = new TwoPartKey();
    param.setMajor("c");
    param.setMinor("d");
    ComplexResourceKey<TwoPartKey, TwoPartKey> complexKey = new ComplexResourceKey<TwoPartKey, TwoPartKey>(key, param);
    Request<TwoPartKey> request = builders.get().setPathKey("keys", complexKey).id("stringKey").build();
    TwoPartKey response = REST_CLIENT.sendRequest(request).get().getEntity();
    Assert.assertEquals(response.getMajor(), "aANDc");
    Assert.assertEquals(response.getMinor(), "bANDd");
  }

  @Test(dataProvider = "requestSubBuilderDataProvider")
  public void testSubGetWithReservedChars(RootBuilderWrapper<String, TwoPartKey> builders) throws ExecutionException, InterruptedException
  {
    TwoPartKey key = new TwoPartKey();
    key.setMajor("a&1");
    key.setMinor("b&2");
    TwoPartKey param = new TwoPartKey();
    param.setMajor("c&3");
    param.setMinor("d&4");
    ComplexResourceKey<TwoPartKey, TwoPartKey> complexKey = new ComplexResourceKey<TwoPartKey, TwoPartKey>(key, param);
    Request<TwoPartKey> request = builders.get().setPathKey("keys", complexKey).id("stringKey").build();
    TwoPartKey response = REST_CLIENT.sendRequest(request).get().getEntity();
    Assert.assertEquals(response.getMajor(), "a&1ANDc&3");
    Assert.assertEquals(response.getMinor(), "b&2ANDd&4");
  }

  @Test(dataProvider = "requestAnnotatedBuilderDataProvider")
  public void testPromiseGet(RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> builders) throws RemoteInvocationException
  {
    testGetMain(builders.get());
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testCreate(RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> builders) throws RemoteInvocationException
  {
    testCreateMain(builders.create(), builders.get());
  }

  @Test(dataProvider = "requestAnnotatedBuilderDataProvider")
  public void testPromiseCreate(RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> builders) throws RemoteInvocationException
  {
    testCreateMain(builders.create(), builders.get());
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testPartialUpdate(RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> builders) throws RemoteInvocationException
  {
    testPartialUpdateMain(builders.partialUpdate(), builders.get());
  }

  @Test(dataProvider = "requestAnnotatedBuilderDataProvider")
  public void testPromisePartialUpdate(RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> builders) throws RemoteInvocationException
  {
    testPartialUpdateMain(builders.partialUpdate(), builders.get());
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testFinder(RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> builders) throws RemoteInvocationException
  {
    testFinderMain(builders.findBy("Prefix").setQueryParam("prefix", StringTestKeys.SIMPLEKEY));
  }

  @Test(dataProvider = "requestAnnotatedBuilderDataProvider")
  public void testPromiseFinder(RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> builders) throws RemoteInvocationException
  {
    testFinderMain(builders.findBy("Prefix").setQueryParam("prefix", StringTestKeys.SIMPLEKEY));
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testBatchGet(RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> builders) throws RemoteInvocationException
  {
    testBatchGetMain(builders.batchGet());
  }

  @Test(dataProvider = "requestAnnotatedBuilderDataProvider")
  public void testPromiseBatchGet(RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> builders) throws RemoteInvocationException
  {
    testBatchGetMain(builders.batchGet());
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testBatchUpdate(RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> builders) throws RemoteInvocationException
  {
    testBatchUpdateMain(builders.batchUpdate(), builders.batchGet());
  }

  @Test(dataProvider = "requestAnnotatedBuilderDataProvider")
  public void testPromiseBatchUpdate(RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> builders) throws RemoteInvocationException
  {
    testBatchUpdateMain(builders.batchUpdate(), builders.batchGet());
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testBatchPartialUpdate(RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> builders) throws RemoteInvocationException
  {
    testBatchPartialUpdateMain(builders.batchPartialUpdate(), builders.batchGet());
  }

  @Test(dataProvider = "requestAnnotatedBuilderDataProvider")
  public void testPromiseBatchPartialUpdate(RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> builders) throws RemoteInvocationException
  {
    testBatchPartialUpdateMain(builders.batchPartialUpdate(), builders.batchGet());
  }

  @Test(dataProvider = "requestBuilderDataProvider")
  public void testBatchDelete(RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> builders) throws RemoteInvocationException
  {
    testBatchDeleteMain(builders.batchDelete(), builders.create(), builders.batchGet());
  }

  @Test(dataProvider = "requestAnnotatedBuilderDataProvider")
  public void testPromiseBatchDelete(RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> builders) throws RemoteInvocationException
  {
    testBatchDeleteMain(builders.batchDelete(), builders.create(), builders.batchGet());
  }

  private void testGetMain(
      RootBuilderWrapper.MethodBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message, Message> requestBuilder) throws RemoteInvocationException
  {
    Request<Message> request = requestBuilder.id(getComplexKey(
        StringTestKeys.SIMPLEKEY, StringTestKeys.SIMPLEKEY2)).build();
    ResponseFuture<Message> future = REST_CLIENT.sendRequest(request);
    Response<Message> response = future.getResponse();

    Assert.assertEquals(response.getEntity().getMessage(), StringTestKeys.SIMPLEKEY + " " + StringTestKeys.SIMPLEKEY2);
  }

  private void testCreateMain(
      RootBuilderWrapper.MethodBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message, EmptyRecord> createRequestBuilder,
      RootBuilderWrapper.MethodBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message, Message> getRequestBuilder) throws RemoteInvocationException
  {
    final String messageText = "newMessage";
    Message message = new Message();
    message.setMessage(messageText);

    Request<EmptyRecord> request = createRequestBuilder.input(message).build();
    ResponseFuture<EmptyRecord> future = REST_CLIENT.sendRequest(request);
    Response<EmptyRecord> response = future.getResponse();
    Assert.assertEquals(response.getStatus(), 201);

    @SuppressWarnings("unchecked")
    Request<Message> getRequest = getRequestBuilder.id(
        getComplexKey(messageText, messageText)).build();
    ResponseFuture<Message> getFuture = REST_CLIENT.sendRequest(getRequest);
    Response<Message> getResponse = getFuture.getResponse();

    Assert.assertEquals(getResponse.getEntity().getMessage(), messageText);
  }

  private void testPartialUpdateMain(
      RootBuilderWrapper.MethodBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message, EmptyRecord> updateRequestBuilder,
      RootBuilderWrapper.MethodBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message, Message> getRequestBuilder) throws RemoteInvocationException
  {
    final String newMessage = "newMessage";
    ComplexResourceKey<TwoPartKey, TwoPartKey> key = getComplexKey(StringTestKeys.SIMPLEKEY, StringTestKeys.SIMPLEKEY2);
    doPartialUpdate(updateRequestBuilder, key, newMessage);

    @SuppressWarnings("unchecked")
    Request<Message> getRequest = getRequestBuilder.id(key).build();
    ResponseFuture<Message> getFuture = REST_CLIENT.sendRequest(getRequest);
    Response<Message> getResponse = getFuture.getResponse();

    Assert.assertEquals(getResponse.getEntity().getMessage(), newMessage);

    doPartialUpdate(updateRequestBuilder, key, StringTestKeys.SIMPLEKEY + " " + StringTestKeys.SIMPLEKEY2);
  }

  private ComplexResourceKey<TwoPartKey, TwoPartKey> doPartialUpdate(
      RootBuilderWrapper.MethodBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message, EmptyRecord> updateRequestBuilder,
      ComplexResourceKey<TwoPartKey, TwoPartKey> key,
      String newMessage) throws RemoteInvocationException
  {
    Message message = new Message();
    message.setMessage(newMessage);

    PatchRequest<Message> patch = PatchGenerator.diffEmpty(message);
    Request<EmptyRecord> request = updateRequestBuilder.id(key).input(patch).build();
    ResponseFuture<EmptyRecord> future = REST_CLIENT.sendRequest(request);
    Response<EmptyRecord> response = future.getResponse();
    Assert.assertEquals(response.getStatus(), 204);
    return key;
  }

  private void testFinderMain(
      RootBuilderWrapper.MethodBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey> , Message, CollectionResponse<Message>> requestBuilder)  throws RemoteInvocationException
  {
    Request<CollectionResponse<Message>> request = requestBuilder.build();
    ResponseFuture<CollectionResponse<Message>> future = REST_CLIENT.sendRequest(request);
    CollectionResponse<Message> response = future.getResponse().getEntity();

    List<Message> results = response.getElements();
    Assert.assertEquals(results.size(), 1);
    Assert.assertEquals(results.get(0).getMessage(), StringTestKeys.SIMPLEKEY + " " + StringTestKeys.SIMPLEKEY2);
  }

  private void testBatchGetMain(
      RootBuilderWrapper.MethodBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey> , Message, BatchResponse<Message>> requestBuilder)  throws RemoteInvocationException
  {
    ArrayList<ComplexResourceKey<TwoPartKey, TwoPartKey>> ids =
        new ArrayList<ComplexResourceKey<TwoPartKey, TwoPartKey>>();
    ComplexResourceKey<TwoPartKey, TwoPartKey> key1 = getComplexKey(StringTestKeys.SIMPLEKEY, StringTestKeys.SIMPLEKEY2);
    ComplexResourceKey<TwoPartKey, TwoPartKey> key2 = getComplexKey(StringTestKeys.URL, StringTestKeys.URL2);
    ids.add(key1);
    ids.add(key2);
    BatchGetKVRequest<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> request = requestBuilder.ids(ids).buildKV();
    ResponseFuture<BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message>> future =
        REST_CLIENT.sendRequest(request);
    BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> response =
        future.getResponse().getEntity();

    Assert.assertEquals(response.getResults().size(), ids.size());
    Assert.assertNotNull(response.getResults().get(key1));
    Assert.assertNotNull(response.getResults().get(key2));
  }

  private void testBatchUpdateMain(
      RootBuilderWrapper.MethodBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message, BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus>> requestBuilder,
      RootBuilderWrapper.MethodBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message, BatchResponse<Message>> batchGetRequestBuilder)
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
    inputs.put(key1, message);
    inputs.put(key2, message2);
    final Request<BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus>> request =
        requestBuilder.inputs(inputs).build();
    final ResponseFuture<BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus>> future =
        REST_CLIENT.sendRequest(request);
    final BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus> response =
        future.getResponse().getEntity();

    for (Map.Entry<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus> resp : response.getResults().entrySet())
    {
      Assert.assertTrue(inputs.containsKey(resp.getKey()));
      Assert.assertEquals(resp.getValue().getStatus().intValue(), 200);
    }

    Assert.assertNotNull(response.getResults().get(key1));
    Assert.assertNotNull(response.getResults().get(key2));

    Assert.assertTrue(response.getErrors().isEmpty());

    ArrayList<ComplexResourceKey<TwoPartKey, TwoPartKey>> ids = new ArrayList<ComplexResourceKey<TwoPartKey, TwoPartKey>>();
    ids.add(key1);
    ids.add(key2);
    BatchGetKVRequest<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchGetRequest =
        batchGetRequestBuilder.ids(ids).buildKV();
    ResponseFuture<BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message>> batchGetFuture =
        REST_CLIENT.sendRequest(batchGetRequest);
    BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchGetResponse =
        batchGetFuture.getResponse().getEntity();

    Assert.assertEquals(batchGetResponse.getResults().get(key1).getTone(), Tone.INSULTING);
    Assert.assertEquals(batchGetResponse.getResults().get(key2).getTone(), Tone.INSULTING);
  }

  private void testBatchPartialUpdateMain(
      RootBuilderWrapper.MethodBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message, BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus>> requestBuilder,
      RootBuilderWrapper.MethodBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message, BatchResponse<Message>> batchGetRequestBuilder)
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
        requestBuilder.patchInputs(inputs).build();

    final ResponseFuture<BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus>> future =
        REST_CLIENT.sendRequest(request);
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
    BatchGetKVRequest<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchGetRequest =
        batchGetRequestBuilder.ids(ids).buildKV();
    ResponseFuture<BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message>> batchGetFuture =
        REST_CLIENT.sendRequest(batchGetRequest);
    BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchGetResponse =
        batchGetFuture.getResponse().getEntity();

    Assert.assertEquals(batchGetResponse.getResults().get(key1).getTone(), Tone.FRIENDLY);
    Assert.assertEquals(batchGetResponse.getResults().get(key2).getTone(), Tone.FRIENDLY);
  }

  private void testBatchDeleteMain(
      RootBuilderWrapper.MethodBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message, BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus>> requestBuilder,
      RootBuilderWrapper.MethodBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message, EmptyRecord> createRequestBuilder,
      RootBuilderWrapper.MethodBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message, BatchResponse<Message>> batchGetRequestBuilder)
      throws RemoteInvocationException
  {
    String messageText = "m1";
    Message message = new Message();
    message.setMessage(messageText);

    Request<EmptyRecord> createRequest = createRequestBuilder.input(message).build();
    ResponseFuture<EmptyRecord> createFuture = REST_CLIENT.sendRequest(createRequest);
    Response<EmptyRecord> createResponse = createFuture.getResponse();
    Assert.assertEquals(createResponse.getStatus(), 201);

    String messageText2 = "m2";
    message.setMessage(messageText2);

    createRequest = createRequestBuilder.input(message).build();
    createFuture = REST_CLIENT.sendRequest(createRequest);
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
        REST_CLIENT.sendRequest(request);
    final BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus> response =
        future.getResponse().getEntity();

    for (Map.Entry<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus> resp : response.getResults().entrySet())
    {
      Assert.assertEquals(resp.getValue().getStatus().intValue(), 204);
    }

    Assert.assertNotNull(response.getResults().get(key1));
    Assert.assertNotNull(response.getResults().get(key2));

    BatchGetKVRequest<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchGetRequest =
        batchGetRequestBuilder.ids(ids).buildKV();
    ResponseFuture<BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message>> batchGetFuture =
        REST_CLIENT.sendRequest(batchGetRequest);
    BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchGetResponse =
        batchGetFuture.getResponse().getEntity();

    Assert.assertTrue(batchGetResponse.getResults().isEmpty());
  }

  private static ComplexResourceKey<TwoPartKey, TwoPartKey> getComplexKey(
      String major, String minor)
  {
    return new ComplexResourceKey<TwoPartKey, TwoPartKey>(
        new TwoPartKey().setMajor(major).setMinor(minor),
        new TwoPartKey());
  }

  @DataProvider
  private static Object[][] requestBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message>(new ComplexKeysBuilders()) },
      { new RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message>(new ComplexKeysRequestBuilders()) }
    };
  }

  @DataProvider
  private static Object[][] requestSubBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<String, TwoPartKey>(new ComplexKeysSubBuilders()) },
      { new RootBuilderWrapper<String, TwoPartKey>(new ComplexKeysSubRequestBuilders()) }
    };
  }

  @DataProvider
  private static Object[][] requestAnnotatedBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message>(new AnnotatedComplexKeysBuilders()) },
      { new RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message>(new AnnotatedComplexKeysRequestBuilders()) }
    };
  }
}
