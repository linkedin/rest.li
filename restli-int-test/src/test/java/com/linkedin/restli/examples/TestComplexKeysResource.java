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
import com.linkedin.restli.client.BatchDeleteRequest;
import com.linkedin.restli.client.BatchDeleteRequestBuilder;
import com.linkedin.restli.client.BatchGetKVRequest;
import com.linkedin.restli.client.BatchGetRequestBuilder;
import com.linkedin.restli.client.BatchPartialUpdateRequest;
import com.linkedin.restli.client.BatchPartialUpdateRequestBuilder;
import com.linkedin.restli.client.BatchUpdateRequest;
import com.linkedin.restli.client.BatchUpdateRequestBuilder;
import com.linkedin.restli.client.CreateRequest;
import com.linkedin.restli.client.CreateRequestBuilder;
import com.linkedin.restli.client.FindRequest;
import com.linkedin.restli.client.FindRequestBuilder;
import com.linkedin.restli.client.GetRequestBuilder;
import com.linkedin.restli.client.PartialUpdateRequest;
import com.linkedin.restli.client.PartialUpdateRequestBuilder;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.client.util.PatchGenerator;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.api.TwoPartKey;
import com.linkedin.restli.examples.greetings.client.AnnotatedComplexKeysBuilders;
import com.linkedin.restli.examples.greetings.client.ComplexKeysBuilders;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TestComplexKeysResource extends RestLiIntegrationTest
{
  private static final Client CLIENT = new TransportClientAdapter(new HttpClientFactory().getClient(Collections.<String, String>emptyMap()));
  private static final String URI_PREFIX = "http://localhost:1338/";
  private static final RestClient REST_CLIENT = new RestClient(CLIENT, URI_PREFIX);

  private final ComplexKeysBuilders COMPLEX_KEYS_BUILDERS = new ComplexKeysBuilders();
  private final AnnotatedComplexKeysBuilders ANNOTATED_COMPLEX_KEYS_BUILDERS = new AnnotatedComplexKeysBuilders();

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
  public void testGet() throws RemoteInvocationException
  {
    testGetMain(COMPLEX_KEYS_BUILDERS.get());
  }

  @Test
  public void testPromiseGet() throws RemoteInvocationException
  {
    testGetMain(ANNOTATED_COMPLEX_KEYS_BUILDERS.get());
  }

  @Test
  public void testCreate() throws RemoteInvocationException
  {
    testCreateMain(COMPLEX_KEYS_BUILDERS.create(), COMPLEX_KEYS_BUILDERS.get());
  }

  @Test
  public void testPromiseCreate() throws RemoteInvocationException
  {
    testCreateMain(ANNOTATED_COMPLEX_KEYS_BUILDERS.create(), ANNOTATED_COMPLEX_KEYS_BUILDERS.get());
  }

  @Test
  public void testPartialUpdate() throws RemoteInvocationException
  {
    testPartialUpdateMain(COMPLEX_KEYS_BUILDERS.partialUpdate(), COMPLEX_KEYS_BUILDERS.get());
  }

  @Test
  public void testPromisePartialUpdate() throws RemoteInvocationException
  {
    testPartialUpdateMain(ANNOTATED_COMPLEX_KEYS_BUILDERS.partialUpdate(), ANNOTATED_COMPLEX_KEYS_BUILDERS.get());
  }

  @Test
  public void testFinder() throws RemoteInvocationException
  {
    testFinderMain(COMPLEX_KEYS_BUILDERS.findByPrefix().prefixParam(StringTestKeys.SIMPLEKEY));
  }

  @Test
  public void testPromiseFinder() throws RemoteInvocationException
  {
    testFinderMain(ANNOTATED_COMPLEX_KEYS_BUILDERS.findByPrefix().prefixParam(StringTestKeys.SIMPLEKEY));
  }

  @Test
  public void testBatchGet() throws RemoteInvocationException
  {
    testBatchGetMain(COMPLEX_KEYS_BUILDERS.batchGet());
  }

  @Test
  public void testPromiseBatchGet() throws RemoteInvocationException
  {
    testBatchGetMain(ANNOTATED_COMPLEX_KEYS_BUILDERS.batchGet());
  }

  @Test
  public void testBatchUpdate() throws RemoteInvocationException
  {
    testBatchUpdateMain(COMPLEX_KEYS_BUILDERS.batchUpdate(), COMPLEX_KEYS_BUILDERS.batchGet());
  }

  @Test
  public void testPromiseBatchUpdate() throws RemoteInvocationException
  {
    testBatchUpdateMain(ANNOTATED_COMPLEX_KEYS_BUILDERS.batchUpdate(), ANNOTATED_COMPLEX_KEYS_BUILDERS.batchGet());
  }

  @Test
  public void testBatchPartialUpdate() throws RemoteInvocationException
  {
    testBatchPartialUpdateMain(COMPLEX_KEYS_BUILDERS.batchPartialUpdate(), COMPLEX_KEYS_BUILDERS.batchGet());
  }

  @Test
  public void testPromiseBatchPartialUpdate() throws RemoteInvocationException
  {
    testBatchPartialUpdateMain(ANNOTATED_COMPLEX_KEYS_BUILDERS.batchPartialUpdate(),
                               ANNOTATED_COMPLEX_KEYS_BUILDERS.batchGet());
  }

  @Test
  public void testBatchDelete() throws RemoteInvocationException
  {
    testBatchDeleteMain(COMPLEX_KEYS_BUILDERS.batchDelete(),
                        COMPLEX_KEYS_BUILDERS.create(),
                        COMPLEX_KEYS_BUILDERS.batchGet());
  }

  @Test
  public void testPromiseBatchDelete() throws RemoteInvocationException
  {
    testBatchDeleteMain(ANNOTATED_COMPLEX_KEYS_BUILDERS.batchDelete(),
                        ANNOTATED_COMPLEX_KEYS_BUILDERS.create(),
                        ANNOTATED_COMPLEX_KEYS_BUILDERS.batchGet());
  }

  private void testGetMain(
      GetRequestBuilder<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> requestBuilder) throws RemoteInvocationException
  {
    Request<Message> request = requestBuilder.id(getComplexKey(
        StringTestKeys.SIMPLEKEY, StringTestKeys.SIMPLEKEY2)).build();
    ResponseFuture<Message> future = REST_CLIENT.sendRequest(request);
    Response<Message> response = future.getResponse();

    Assert.assertEquals(response.getEntity().getMessage(), StringTestKeys.SIMPLEKEY + " " + StringTestKeys.SIMPLEKEY2);
  }

  private void testCreateMain(
      CreateRequestBuilder<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> createRequestBuilder,
      GetRequestBuilder<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> getRequestBuilder) throws RemoteInvocationException
  {
    final String messageText = "newMessage";
    Message message = new Message();
    message.setMessage(messageText);

    CreateRequest<Message> request = createRequestBuilder.input(message).build();
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
      PartialUpdateRequestBuilder<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> updateRequestBuilder,
      GetRequestBuilder<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> getRequestBuilder) throws RemoteInvocationException
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
      PartialUpdateRequestBuilder<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> updateRequestBuilder,
      ComplexResourceKey<TwoPartKey, TwoPartKey> key,
      String newMessage) throws RemoteInvocationException
  {
    Message message = new Message();
    message.setMessage(newMessage);

    PatchRequest<Message> patch = PatchGenerator.diffEmpty(message);
    PartialUpdateRequest<Message> request = updateRequestBuilder.id(key).input(patch).build();
    ResponseFuture<EmptyRecord> future = REST_CLIENT.sendRequest(request);
    Response<EmptyRecord> response = future.getResponse();
    Assert.assertEquals(response.getStatus(), 204);
    return key;
  }

  private void testFinderMain(
      FindRequestBuilder<ComplexResourceKey<TwoPartKey, TwoPartKey> , Message> requestBuilder)  throws RemoteInvocationException
  {
    FindRequest<Message> request = requestBuilder.build();
    ResponseFuture<CollectionResponse<Message>> future = REST_CLIENT.sendRequest(request);
    CollectionResponse<Message> response = future.getResponse().getEntity();

    List<Message> results = response.getElements();
    Assert.assertEquals(results.size(), 1);
    Assert.assertEquals(results.get(0).getMessage(), StringTestKeys.SIMPLEKEY + " " + StringTestKeys.SIMPLEKEY2);
  }

  private void testBatchGetMain(
      BatchGetRequestBuilder<ComplexResourceKey<TwoPartKey, TwoPartKey> , Message> requestBuilder)  throws RemoteInvocationException
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
      BatchUpdateRequestBuilder<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> requestBuilder,
      BatchGetRequestBuilder<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchGetRequestBuilder)
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
    final BatchUpdateRequest<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> request =
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
      BatchPartialUpdateRequestBuilder<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> requestBuilder,
      BatchGetRequestBuilder<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchGetRequestBuilder)
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
    final BatchPartialUpdateRequest<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> request =
        requestBuilder.inputs(inputs).build();

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
      BatchDeleteRequestBuilder<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> requestBuilder,
      CreateRequestBuilder<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> createRequestBuilder,
      BatchGetRequestBuilder<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchGetRequestBuilder)
      throws RemoteInvocationException
  {
    String messageText = "m1";
    Message message = new Message();
    message.setMessage(messageText);

    CreateRequest<Message> createRequest = createRequestBuilder.input(message).build();
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
    final BatchDeleteRequest<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> request =
        requestBuilder.ids(ids).build();;

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
}
