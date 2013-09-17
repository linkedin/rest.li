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
import com.linkedin.restli.client.BatchGetRequest;
import com.linkedin.restli.client.BatchGetRequestBuilder;
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
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.examples.greetings.api.Message;
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
  public void testBatchUpdate() throws RemoteInvocationException
  {
    testBatchUpdateMain(COMPLEX_KEYS_BUILDERS.batchUpdate());
  }

  @Test
  public void testPromiseBatchGet() throws RemoteInvocationException
  {
    testBatchGetMain(ANNOTATED_COMPLEX_KEYS_BUILDERS.batchGet());
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
    ArrayList<ComplexResourceKey<TwoPartKey, TwoPartKey>> ids = new ArrayList<ComplexResourceKey<TwoPartKey, TwoPartKey>>();
    ids.add(getComplexKey(StringTestKeys.SIMPLEKEY, StringTestKeys.SIMPLEKEY2));
    ids.add(getComplexKey(StringTestKeys.URL, StringTestKeys.URL2));
    BatchGetRequest<Message> request = requestBuilder.ids(ids).build();
    ResponseFuture<BatchResponse<Message>> future = REST_CLIENT.sendRequest(request);
    BatchResponse<Message> response = future.getResponse().getEntity();

    Assert.assertEquals(response.getResults().size(), ids.size());
  }

  private void testBatchUpdateMain(
      BatchUpdateRequestBuilder<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> requestBuilder)  throws RemoteInvocationException
  {
    final String messageText = "newMessage";
    final Message message = new Message();
    message.setMessage(messageText);

    final Map<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> inputs = new HashMap<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message>();
    inputs.put(getComplexKey(StringTestKeys.SIMPLEKEY, StringTestKeys.SIMPLEKEY2), message);
    inputs.put(getComplexKey(StringTestKeys.URL, StringTestKeys.URL2), message);
    final BatchUpdateRequest<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> request = requestBuilder.inputs(inputs).build();
    final ResponseFuture<BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus>> future = REST_CLIENT.sendRequest(request);
    final BatchKVResponse<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus> response = future.getResponse().getEntity();

    for (Map.Entry<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus> resp : response.getResults().entrySet())
    {
      Assert.assertTrue(inputs.containsKey(resp.getKey()));
      Assert.assertEquals(resp.getValue().getStatus().intValue(), 200);
    }

    Assert.assertTrue(response.getErrors().isEmpty());
  }

  private static ComplexResourceKey<TwoPartKey, TwoPartKey> getComplexKey(
      String major, String minor)
  {
    return new ComplexResourceKey<TwoPartKey, TwoPartKey>(
        new TwoPartKey().setMajor(major).setMinor(minor),
        new TwoPartKey());
  }
}
