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

import com.linkedin.data.schema.PathSpec;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.common.BatchCollectionResponse;
import com.linkedin.restli.common.BatchFinderCriteriaResult;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.GreetingCriteria;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.BatchfindersBuilders;
import com.linkedin.restli.examples.greetings.client.BatchfindersRequestBuilders;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestBatchFinderResource extends RestLiIntegrationTest
{
  private static final Client CLIENT = new TransportClientAdapter(new HttpClientFactory.Builder().build().getClient(
      Collections.<String, String>emptyMap()));
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

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchFindersRequestBuilderDataProvider")
  public void testBatchFinder(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    GreetingCriteria c1 = new GreetingCriteria().setId(1L).setTone(Tone.SINCERE);
    GreetingCriteria c2 = new GreetingCriteria().setId(2L).setTone(Tone.FRIENDLY);

    Request<BatchCollectionResponse<Greeting>> request = builders.batchFindBy("searchGreetings").setQueryParam("criteria",
        Arrays.asList(c1, c2)).setQueryParam("message", "hello world").build();
    ResponseFuture<BatchCollectionResponse<Greeting>> future = getClient().sendRequest(request);
    BatchCollectionResponse<Greeting> response = future.getResponse().getEntity();

    List<BatchFinderCriteriaResult<Greeting>> batchResult = response.getResults();

    List<Greeting> greetings1 = batchResult.get(0).getElements();
    Assert.assertTrue(greetings1.get(0).hasTone());
    Assert.assertTrue(greetings1.get(0).getTone().equals(Tone.SINCERE));

    List<Greeting> greetings2 = batchResult.get(1).getElements();
    Assert.assertTrue(greetings2.get(0).hasId());
    Assert.assertTrue(greetings2.get(0).getTone().equals(Tone.FRIENDLY));
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchFindersRequestBuilderDataProvider")
  public void testBatchFinderWithProjection(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    GreetingCriteria c1 = new GreetingCriteria().setId(1L).setTone(Tone.SINCERE);
    GreetingCriteria c2 = new GreetingCriteria().setId(2L).setTone(Tone.FRIENDLY);

    Request<BatchCollectionResponse<Greeting>> request = builders.batchFindBy("searchGreetings")
        .setQueryParam("criteria", Arrays.asList(c1, c2))
        .setQueryParam("message", "hello world")
        .fields(Greeting.fields().id())
        .build();
    ResponseFuture<BatchCollectionResponse<Greeting>> future = getClient().sendRequest(request);
    BatchCollectionResponse<Greeting> response = future.getResponse().getEntity();

    List<BatchFinderCriteriaResult<Greeting>> batchResult = response.getResults();


    List<Greeting> greetings1 = batchResult.get(0).getElements();
    Assert.assertFalse(greetings1.get(0).hasTone());
    Assert.assertTrue(greetings1.get(0).hasId());

    List<Greeting> greetings2 = batchResult.get(1).getElements();
    Assert.assertTrue(greetings2.get(0).hasId());
    Assert.assertFalse(greetings2.get(0).hasTone());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchFindersRequestBuilderDataProvider")
  public void testBatchFinderWithError(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    GreetingCriteria c3 = new GreetingCriteria().setId(100L);

    Request<BatchCollectionResponse<Greeting>> request = builders.batchFindBy("searchGreetings")
        .addQueryParam("criteria", c3).setQueryParam("message", "hello world").build();

    ResponseFuture<BatchCollectionResponse<Greeting>> future = getClient().sendRequest(request);
    BatchCollectionResponse<Greeting> response = future.getResponse().getEntity();
    List<BatchFinderCriteriaResult<Greeting>> batchResult = response.getResults();

    Assert.assertEquals(batchResult.size(), 1);
    ErrorResponse error = batchResult.get(0).getError();
    Assert.assertEquals(error.getMessage(), "Fail to find Greeting!");
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchFindersRequestBuilderDataProvider")
  public void testBatchFinderWithErrorAndProjection(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    GreetingCriteria c3 = new GreetingCriteria().setId(100L);

    Request<BatchCollectionResponse<Greeting>> request = builders.batchFindBy("searchGreetings")
        .addQueryParam("criteria", c3).setQueryParam("message", "hello world").fields(Greeting.fields().id()).build();

    ResponseFuture<BatchCollectionResponse<Greeting>> future = getClient().sendRequest(request);
    BatchCollectionResponse<Greeting> response = future.getResponse().getEntity();
    List<BatchFinderCriteriaResult<Greeting>> batchResult = response.getResults();

    Assert.assertEquals(batchResult.size(), 1);
    ErrorResponse error = batchResult.get(0).getError();
    Assert.assertEquals(error.getMessage(), "Fail to find Greeting!");
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchFindersRequestBuilderDataProvider")
  public void testBatchFinderWithNotFoundCriteria(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException {
    GreetingCriteria c4 = new GreetingCriteria().setId(0L);

    Request<BatchCollectionResponse<Greeting>> request = builders.batchFindBy("searchGreetings")
        .addQueryParam("criteria", c4).setQueryParam("message", "hello world").build();

    ResponseFuture<BatchCollectionResponse<Greeting>> future = getClient().sendRequest(request);
    BatchCollectionResponse<Greeting> response = future.getResponse().getEntity();
    List<BatchFinderCriteriaResult<Greeting>> batchResult = response.getResults();

    Assert.assertEquals(batchResult.size(), 1);
    ErrorResponse error = batchResult.get(0).getError();
    Assert.assertEquals(error.getMessage(), "The server didn't find a representation for this criteria");
  }

  @Test
  public void testUsingResourceSpecificBuilder() throws RemoteInvocationException {
    GreetingCriteria c1 = new GreetingCriteria().setId(1L).setTone(Tone.SINCERE);
    GreetingCriteria c2 = new GreetingCriteria().setId(2L).setTone(Tone.FRIENDLY);
    GreetingCriteria c3 = new GreetingCriteria().setId(100);
    Request<BatchCollectionResponse<Greeting>> req = new BatchfindersRequestBuilders().batchFindBySearchGreetings()
        .criteriaParam(Arrays.asList(c1, c2, c3)).messageParam("hello world").build();
    Response<BatchCollectionResponse<Greeting>> resp = REST_CLIENT.sendRequest(req).getResponse();
    BatchCollectionResponse<Greeting> response = resp.getEntity();

    List<BatchFinderCriteriaResult<Greeting>> batchResult = response.getResults();

    Assert.assertEquals(batchResult.size(), 3);

    // on success
    List<Greeting> greetings1 = batchResult.get(0).getElements();
    Assert.assertTrue(greetings1.get(0).hasTone());
    Assert.assertTrue(greetings1.get(0).getTone().equals(Tone.SINCERE));

    // on error
    ErrorResponse error = batchResult.get(2).getError();
    Assert.assertTrue(batchResult.get(2).isError());
    Assert.assertEquals(error.getMessage(), "Fail to find Greeting!");
  }

  @Test
  public void testUsingResourceSpecificBuilderWithProjection() throws RemoteInvocationException {
    GreetingCriteria c1 = new GreetingCriteria().setId(1L).setTone(Tone.SINCERE);
    GreetingCriteria c2 = new GreetingCriteria().setId(2L).setTone(Tone.FRIENDLY);
    GreetingCriteria c3 = new GreetingCriteria().setId(100);
    Request<BatchCollectionResponse<Greeting>> req = new BatchfindersRequestBuilders().batchFindBySearchGreetings()
        .criteriaParam(Arrays.asList(c1, c2, c3)).fields(Greeting.fields().tone()).messageParam("hello world").build();
    Response<BatchCollectionResponse<Greeting>> resp = REST_CLIENT.sendRequest(req).getResponse();
    BatchCollectionResponse<Greeting> response = resp.getEntity();

    List<BatchFinderCriteriaResult<Greeting>> batchResult = response.getResults();

    Assert.assertEquals(batchResult.size(), 3);

    // on success
    List<Greeting> greetings1 = batchResult.get(0).getElements();
    Assert.assertTrue(greetings1.get(0).hasTone());
    Assert.assertTrue(greetings1.get(0).getTone().equals(Tone.SINCERE));
    Assert.assertFalse(greetings1.get(0).hasId());

    List<Greeting> greetings2 = batchResult.get(1).getElements();
    Assert.assertTrue(greetings2.get(0).hasTone());
    Assert.assertTrue(greetings2.get(0).getTone().equals(Tone.FRIENDLY));
    Assert.assertFalse(greetings2.get(0).hasId());

    // on error
    ErrorResponse error = batchResult.get(2).getError();
    Assert.assertTrue(batchResult.get(2).isError());
    Assert.assertEquals(error.getMessage(), "Fail to find Greeting!");
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchFindersRequestBuilderDataProvider")
  private static Object[][] batchFindersRequestBuilderDataProvider()
  {
    return new Object[][] {
        { new RootBuilderWrapper<Long, Greeting>(new BatchfindersBuilders())},
        { new RootBuilderWrapper<Long, Greeting>(new BatchfindersRequestBuilders())}
    };
  }
}
