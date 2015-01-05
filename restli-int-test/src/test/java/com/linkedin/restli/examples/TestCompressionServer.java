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


import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.compression.Bzip2Compressor;
import com.linkedin.r2.filter.compression.CompressionException;
import com.linkedin.r2.filter.compression.Compressor;
import com.linkedin.r2.filter.compression.DeflateCompressor;
import com.linkedin.r2.filter.compression.GzipCompressor;
import com.linkedin.r2.filter.compression.SnappyCompressor;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.common.HttpConstants;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.client.util.PatchGenerator;
import com.linkedin.restli.common.BatchCreateIdResponse;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.CreateStatus;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.GreetingsBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsRequestBuilders;
import com.linkedin.restli.examples.greetings.server.CompressionResource;
import com.linkedin.restli.examples.groups.api.TransferOwnershipRequest;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Same as TestGreetingsClient, but uses compression
 */
public class TestCompressionServer extends RestLiIntegrationTest
{
  private static final String URI_PREFIX = "http://localhost:1338/";
  private static final String URI_PREFIX_WITHOUT_COMPRESSION = "http://localhost:1339/"; //This server does no compression

  @DataProvider
  public Object[][] compressorDataProvider()
  {
    return new Object[][]
      {
        { new SnappyCompressor() },
        { new Bzip2Compressor() },
        { new GzipCompressor() },
        { new DeflateCompressor()}
      };
  }

  /**
   * Provides clients with no response compression
   */
  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientsCookbookDataProvider")
  public Object[][] clientsCookbookDataProvider()
  {
    // need separate TransportClientAdapter for shutdown test
    TransportClientAdapter clientAdapter1 = new TransportClientAdapter(new HttpClientFactory(FilterChains.empty()).
      getClient(Collections.<String, String>emptyMap()));
    TransportClientAdapter clientAdapter2 = new TransportClientAdapter(new HttpClientFactory(FilterChains.empty()).
      getClient(Collections.<String, String>emptyMap()));
    return new Object[][]
      {
        { new RestClient(clientAdapter1, URI_PREFIX), RestliRequestOptions.DEFAULT_OPTIONS },
        { new RestClient(clientAdapter2, URI_PREFIX), TestConstants.FORCE_USE_NEXT_OPTIONS },
      };
  }

  /**
   * Provides clients with no response compression
   */
  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientsNoCompressedResponsesDataProvider")
  public Object[][] clientsNoCompressedResponsesDataProvider()
  {
    // need separate TransportClientAdapter for shutdown test
    TransportClientAdapter clientAdapter1 = new TransportClientAdapter(new HttpClientFactory(FilterChains.empty()).
        getClient(Collections.<String, String>emptyMap()));
    TransportClientAdapter clientAdapter2 = new TransportClientAdapter(new HttpClientFactory(FilterChains.empty()).
      getClient(Collections.<String, String>emptyMap()));
    TransportClientAdapter clientAdapter3 = new TransportClientAdapter(new HttpClientFactory(FilterChains.empty()).
      getClient(Collections.<String, String>emptyMap()));
    TransportClientAdapter clientAdapter4 = new TransportClientAdapter(new HttpClientFactory(FilterChains.empty()).
      getClient(Collections.<String, String>emptyMap()));
    return new Object[][]
      {
        { new RestClient(clientAdapter1, URI_PREFIX), new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders()) },
        { new RestClient(clientAdapter2, URI_PREFIX), new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
        { new RestClient(clientAdapter3, URI_PREFIX), new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders()) },
        { new RestClient(clientAdapter4, URI_PREFIX), new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) }
      };
  }

  @DataProvider
  //Tests for content negotiation, including order preference, q value preference, identity and * cases
  public Object[][] contentNegotiationDataProvider()
  {
    return new Object[][]
        {
        //Basic sanity checks
        {"gzip", "gzip"},
        {"deflate", "deflate"},
        {"snappy", "snappy"},
        {"bzip2", "bzip2"},
        {"deflate, nonexistentcompression", "deflate"},
        {"blablabla, dEflate", "deflate"},

        //Test quality values preference
        {"gzip, deflate;q=0.5", "gzip"},
        {"deflate;q=0.5, gzip", "gzip"},
        {"gzip,trololol, deflate;q=0.5", "gzip"},
        {"trololo, gzip;q=0.5, deflate;q=1.0", "deflate"},
        {"*,gzip;q=0.5,identity;q=0","gzip"},

        {"  tRoLolo  ,  gZiP ;  q=0.5,  DeflAte ;  q=1.0  ", "deflate"}, //test case and whitespace insensitivity

        //* cases and identity cases
        {"", null}, //null for no content-encoding
        {"*;q=0.5, gzip;q=1.0", "gzip"},
        {"*,gzip;q=0, snappy;q=0, bzip2;q=0 ", null},
        {"gzip;q=0, snappy;q=0, bzip2;q=0, deflate; q=0, *", null}
        };
  }

  //Provides tests for 406 errors
  @DataProvider
  public Object[][] error406DataProvider()
  {
    return new Object[][]
        {
        {"identity;q=0"},
        {"*;q=0.5, identity;q=0"},
        {"*;q=0, identity;q=0.0"},
        {"*;q=0"}
        };
  }

  @BeforeClass
  public void initClass() throws Exception
  {
    super.init(false, true);
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  public static void addCompressionHeaders(HttpGet getMessage, String acceptEncoding)
  {
    getMessage.addHeader(HttpConstants.ACCEPT_ENCODING, acceptEncoding);
    getMessage.addHeader(HttpConstants.HEADER_RESPONSE_COMPRESSION_THRESHOLD, "0");
  }

  @Test(dataProvider = "contentNegotiationDataProvider")
  //This is meant to test for when server is NOT configured to compress anything.
  public void testCompatibleDefault(String acceptEncoding, String contentEncoding) throws HttpException, IOException, URISyntaxException
  {
    String path = CompressionResource.getPath();
    HttpClient client = HttpClientBuilder.create()
            .disableContentCompression()
            .build();

    HttpGet get = new HttpGet(URI_PREFIX_WITHOUT_COMPRESSION + path + CompressionResource.getRedundantQueryExample());
    addCompressionHeaders(get, acceptEncoding);

    HttpResponse response = client.execute(get);

    Assert.assertNull(response.getFirstHeader(HttpConstants.CONTENT_ENCODING));
  }

  //Tests for when compression should be applied
  @Test(dataProvider = "compressorDataProvider")
  public void testCompressionBetter(Compressor compressor) throws RemoteInvocationException, HttpException, IOException, CompressionException, URISyntaxException
  {
    String path = CompressionResource.getPath();
    HttpClient client = HttpClientBuilder.create()
            .disableContentCompression()
            .build();

    //Get the result uncompressed
    HttpGet get = new HttpGet(URI_PREFIX + path + CompressionResource.getRedundantQueryExample());
    HttpResponse response = client.execute(get);
    byte[] original = EntityUtils.toString(response.getEntity()).getBytes();

    get.releaseConnection();
    //Ensure uncompressed
    Assert.assertTrue(response.getFirstHeader(HttpConstants.CONTENT_ENCODING) == null);

    addCompressionHeaders(get, compressor.getContentEncodingName());
    response = client.execute(get);

    byte[] compressed = compressor.inflate(response.getEntity().getContent());
    Assert.assertEquals(compressor.getContentEncodingName(), response.getFirstHeader(HttpConstants.CONTENT_ENCODING).getValue());

    Assert.assertEquals(original, compressed);
    Assert.assertTrue(response.getEntity().getContentLength() < original.length);
  }

  //Test compression when it is worse (lengthwise)
  @Test(dataProvider = "compressorDataProvider")
  public void testCompressionWorse(Compressor compressor) throws RemoteInvocationException, HttpException, IOException, URISyntaxException
  {
    String path = CompressionResource.getPath();
    HttpClient client = HttpClientBuilder.create()
            .disableContentCompression()
            .build();

    //Get the result uncompressed
    HttpGet get = new HttpGet(URI_PREFIX + path + CompressionResource.getNoRedundantQueryExample());
    HttpResponse response = client.execute(get);
    String original = EntityUtils.toString(response.getEntity());

    //Ensure uncompressed
    Assert.assertTrue(response.getFirstHeader(HttpConstants.CONTENT_ENCODING) == null);

    addCompressionHeaders(get, compressor.getContentEncodingName());
    response = client.execute(get);
    String compressed = EntityUtils.toString(response.getEntity());

    Assert.assertEquals(null, response.getFirstHeader(HttpConstants.CONTENT_ENCODING));

    //Ensure the results are the same
    Assert.assertEquals(original, compressed);
  }

  //Test server response parsings
  @Test(dataProvider = "contentNegotiationDataProvider")
  public void testAcceptEncoding(String acceptedEncoding, String contentEncoding) throws HttpException, IOException, URISyntaxException
  {
    String path = CompressionResource.getPath();
    HttpClient client = HttpClientBuilder.create()
            .disableContentCompression()
            .build();

    HttpGet get = new HttpGet(URI_PREFIX + path + CompressionResource.getRedundantQueryExample());
    addCompressionHeaders(get, acceptedEncoding);

    HttpResponse response = client.execute(get);

    if(contentEncoding == null)
    {
      Assert.assertNull(response.getFirstHeader(HttpConstants.CONTENT_ENCODING));
    }
    else
    {
      Assert.assertEquals(contentEncoding, response.getFirstHeader(HttpConstants.CONTENT_ENCODING).getValue());
    }
  }

  @Test(dataProvider = "error406DataProvider")
  public void test406Error(String acceptContent) throws HttpException, IOException, URISyntaxException
  {
    String path = CompressionResource.getPath();
    HttpClient client = HttpClientBuilder.create()
            .disableContentCompression()
            .build();

    HttpGet get = new HttpGet(URI_PREFIX + path + CompressionResource.getRedundantQueryExample());
    addCompressionHeaders(get, acceptContent);

    HttpResponse response = client.execute(get);

    Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpConstants.NOT_ACCEPTABLE);
    Assert.assertEquals(response.getEntity().getContentLength(), 0);
  }

  private <T> void checkContentEncodingHeaderIsAbsent(Response<T> response)
  {
    Assert.assertFalse(response.getHeaders().containsKey(HttpConstants.CONTENT_ENCODING));
  }

  //Existing tests to ensure compatibility
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientsNoCompressedResponsesDataProvider")
  public void testIntAction(RestClient client, RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<Integer> request = builders.<Integer>action("Purge").build();
    ResponseFuture<Integer> responseFuture = client.sendRequest(request);

    Assert.assertEquals(responseFuture.getResponse().getStatus(), 200);
    Assert.assertEquals(responseFuture.getResponse().getEntity().intValue(), 100);
    checkContentEncodingHeaderIsAbsent(responseFuture.getResponse());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientsNoCompressedResponsesDataProvider")
  public void testRecordAction(RestClient client, RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<Greeting> request = builders.<Greeting>action("SomeAction")
        .id(1L)
        .setActionParam("A", 1)
        .setActionParam("B", "")
        .setActionParam("C", new TransferOwnershipRequest())
        .setActionParam("D", new TransferOwnershipRequest())
        .setActionParam("E", 3)
        .build();
    ResponseFuture<Greeting> responseFuture = client.sendRequest(request);
    Assert.assertEquals(responseFuture.getResponse().getStatus(), 200);
    Assert.assertNotNull(responseFuture.getResponse().getEntity());
    checkContentEncodingHeaderIsAbsent(responseFuture.getResponse());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientsNoCompressedResponsesDataProvider")
  public void testUpdateToneAction(RestClient client, RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<Greeting> request = builders.<Greeting>action("UpdateTone")
        .id(1L)
        .setActionParam("NewTone", Tone.SINCERE)
        .setActionParam("DelOld", false)
        .build();
    ResponseFuture<Greeting> responseFuture = client.sendRequest(request);
    Assert.assertEquals(responseFuture.getResponse().getStatus(), 200);
    final Greeting newGreeting = responseFuture.getResponse().getEntity();
    Assert.assertNotNull(newGreeting);
    Assert.assertEquals(newGreeting.getId().longValue(), 1L);
    Assert.assertEquals(newGreeting.getTone(), Tone.SINCERE);
    checkContentEncodingHeaderIsAbsent(responseFuture.getResponse());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientsNoCompressedResponsesDataProvider")
  //test update on retrieved entity
  public void testUpdate(RestClient client, RootBuilderWrapper<Long, Greeting> builders)
    throws RemoteInvocationException, CloneNotSupportedException, URISyntaxException
  {
    // GET
    Request<Greeting> request = builders.get().id(1L).build();
    ResponseFuture<Greeting> future = client.sendRequest(request);
    Response<Greeting> greetingResponse = future.getResponse();

    String response1 = greetingResponse.getEntity().getMessage();
    Assert.assertNotNull(response1);
    checkContentEncodingHeaderIsAbsent(future.getResponse());

    // POST
    Greeting greeting = new Greeting(greetingResponse.getEntity().data().copy());
    greeting.setMessage(response1 + "Again");

    Request<EmptyRecord> writeRequest = builders.update().id(1L).input(greeting).build();
    client.sendRequest(writeRequest).getResponse();

    // GET again, to verify that our POST worked.
    Request<Greeting> request2 = builders.get().id(1L).build();
    ResponseFuture<Greeting> future2 = client.sendRequest(request2);
    String response2 = future2.getResponse().getEntity().getMessage();

    Assert.assertEquals(response2, response1 + "Again");
    checkContentEncodingHeaderIsAbsent(future2.getResponse());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientsNoCompressedResponsesDataProvider")
  //test update on retrieved entity
  public void testGet(RestClient client, RootBuilderWrapper<Long, Greeting> builders)
    throws RemoteInvocationException, CloneNotSupportedException, URISyntaxException
  {
    // GET
    Request<Greeting> request = builders.get().id(1L).build();
    ResponseFuture<Greeting> future = client.sendRequest(request);
    Response<Greeting> greetingResponse = future.getResponse();

    String response1 = greetingResponse.getEntity().getMessage();
    Assert.assertNotNull(response1);
    checkContentEncodingHeaderIsAbsent(greetingResponse);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientsNoCompressedResponsesDataProvider")
  public void testPartialUpdate(RestClient client, RootBuilderWrapper<Long, Greeting> builders)
    throws RemoteInvocationException, CloneNotSupportedException, URISyntaxException
  {
    // GET
    Request<Greeting> request = builders.get().id(1L).build();
    ResponseFuture<Greeting> future = client.sendRequest(request);
    Response<Greeting> greetingResponse = future.getResponse();

    Greeting original = greetingResponse.getEntity();
    checkContentEncodingHeaderIsAbsent(greetingResponse);

    // POST
    Greeting greeting = new Greeting(original.data().copy());
    greeting.setMessage(original.getMessage() + " Again");

    PatchRequest<Greeting> patch = PatchGenerator.diff(original, greeting);

    Request<EmptyRecord> writeRequest = builders.partialUpdate().id(1L).input(patch).build();
    int status = client.sendRequest(writeRequest).getResponse().getStatus();
    Assert.assertEquals(status, HttpStatus.S_204_NO_CONTENT.getCode());

    // GET again, to verify that our POST worked.
    Request<Greeting> request2 = builders.get().id(1L).build();
    ResponseFuture<Greeting> future2 = client.sendRequest(request2);
    String response2 = future2.getResponse().getEntity().getMessage();

    Assert.assertEquals(response2, greeting.getMessage());
    checkContentEncodingHeaderIsAbsent(future2.getResponse());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientsNoCompressedResponsesDataProvider")
  //test cookbook example from quickstart wiki
  public void testCookbook(RestClient restClient, RootBuilderWrapper<Long, Greeting> builders) throws Exception
  {
    // GET
    Request<Greeting> request = builders.get().id(1L).build();
    ResponseFuture<Greeting> future = restClient.sendRequest(request);
    Response<Greeting> greetingResponse = future.getResponse();

    Assert.assertNotNull(greetingResponse.getEntity().getMessage());
    checkContentEncodingHeaderIsAbsent(greetingResponse);

    // POST
    Greeting greeting = new Greeting(greetingResponse.getEntity().data().copy());
    final String NEW_MESSAGE = "This is a new message!";
    greeting.setMessage(NEW_MESSAGE);

    Request<EmptyRecord> writeRequest = builders.update().id(1L).input(greeting).build();
    restClient.sendRequest(writeRequest).getResponse();

    // GET again, to verify that our POST worked.
    Request<Greeting> request2 = builders.get().id(1L).build();
    ResponseFuture<Greeting> future2 = restClient.sendRequest(request2);
    greetingResponse = future2.get();

    Assert.assertEquals(greetingResponse.getEntity().getMessage(), NEW_MESSAGE);
    checkContentEncodingHeaderIsAbsent(greetingResponse);

    // shut down client
    FutureCallback<None> futureCallback = new FutureCallback<None>();
    restClient.shutdown(futureCallback);
    futureCallback.get();
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientsCookbookDataProvider")
  public void testOldCookbookInBatch(RestClient client, RestliRequestOptions requestOptions) throws Exception
  {
    final GreetingsBuilders builders = new GreetingsBuilders(requestOptions);

    // GET
    Greeting greetingResult = getOldCookbookBatchGetResult(client, requestOptions);

    // POST
    Greeting greeting = new Greeting(greetingResult.data().copy());
    greeting.setMessage("This is a new message!");

    Request<BatchKVResponse<Long, UpdateStatus>> writeRequest = builders.batchUpdate().input(1L, greeting).build();
    client.sendRequest(writeRequest).getResponse();

    // GET again, to verify that our POST worked.
    getOldCookbookBatchGetResult(client, requestOptions);

    // batch Create
    Greeting repeatedGreeting = new Greeting();
    repeatedGreeting.setMessage("Hello Hello");
    repeatedGreeting.setTone(Tone.SINCERE);
    List<Greeting> entities = Arrays.asList(repeatedGreeting, repeatedGreeting);
    Request<CollectionResponse<CreateStatus>> batchCreateRequest = builders.batchCreate().inputs(entities).build();
    List<CreateStatus> statuses = client.sendRequest(batchCreateRequest).getResponse().getEntity().getElements();
    for (CreateStatus status : statuses)
    {
      Assert.assertEquals(status.getStatus().intValue(), HttpStatus.S_201_CREATED.getCode());
      @SuppressWarnings("deprecation")
      String id = status.getId();
      Assert.assertNotNull(id);
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientsCookbookDataProvider")
  public void testNewCookbookInBatch(RestClient client, RestliRequestOptions requestOptions) throws Exception
  {
    final GreetingsRequestBuilders builders = new GreetingsRequestBuilders(requestOptions);

    // GET
    Greeting greetingResult = getNewCookbookBatchGetResult(client, requestOptions);

    // POST
    Greeting greeting = new Greeting(greetingResult.data().copy());
    greeting.setMessage("This is a new message!");

    Request<BatchKVResponse<Long, UpdateStatus>> writeRequest = builders.batchUpdate().input(1L, greeting).build();
    client.sendRequest(writeRequest).getResponse();

    // GET again, to verify that our POST worked.
    getNewCookbookBatchGetResult(client, requestOptions);

    // batch Create
    Greeting repeatedGreeting = new Greeting();
    repeatedGreeting.setMessage("Hello Hello");
    repeatedGreeting.setTone(Tone.SINCERE);
    List<Greeting> entities = Arrays.asList(repeatedGreeting, repeatedGreeting);
    Request<BatchCreateIdResponse<Long>> batchCreateRequest = builders.batchCreate().inputs(entities).build();
    List<CreateIdStatus<Long>> statuses = client.sendRequest(batchCreateRequest).getResponse().getEntity().getElements();
    for (CreateIdStatus<Long> status : statuses)
    {
      Assert.assertEquals(status.getStatus().intValue(), HttpStatus.S_201_CREATED.getCode());
      @SuppressWarnings("deprecation")
      String id = status.getId();
      Assert.assertEquals(status.getKey().longValue(), Long.parseLong(id));
      Assert.assertNotNull(status.getKey());
    }
  }

  private Greeting getOldCookbookBatchGetResult(RestClient client, RestliRequestOptions requestOptions) throws RemoteInvocationException
  {
    Request<BatchResponse<Greeting>> request = new GreetingsBuilders(requestOptions).batchGet().ids(1L).build();
    ResponseFuture<BatchResponse<Greeting>> future = client.sendRequest(request);
    Response<BatchResponse<Greeting>> greetingResponse = future.getResponse();
    checkContentEncodingHeaderIsAbsent(greetingResponse);
    return greetingResponse.getEntity().getResults().get("1");
  }

  private Greeting getNewCookbookBatchGetResult(RestClient client, RestliRequestOptions requestOptions) throws RemoteInvocationException
  {
    Request<BatchKVResponse<Long, EntityResponse<Greeting>>> request = new GreetingsRequestBuilders(requestOptions).batchGet().ids(1L).build();
    ResponseFuture<BatchKVResponse<Long, EntityResponse<Greeting>>> future = client.sendRequest(request);
    Response<BatchKVResponse<Long, EntityResponse<Greeting>>> greetingResponse = future.getResponse();
    checkContentEncodingHeaderIsAbsent(greetingResponse);
    return greetingResponse.getEntity().getResults().get(1L).getEntity();
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientsNoCompressedResponsesDataProvider")
  public void testMalformedPagination(RestClient client, RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    expectPaginationError("-1", client, builders);
    expectPaginationError("abc", client, builders);
  }

  private void expectPaginationError(String count, RestClient client, RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    try
    {
      Request<CollectionResponse<Greeting>> request = builders.findBy("Search").name("search").setParam("count", count).build();
      client.sendRequest(request).getResponse();
      Assert.fail("expected exception");
    }
    catch (RestException e)
    {
      Assert.assertEquals(e.getResponse().getStatus(), HttpStatus.S_400_BAD_REQUEST.getCode(), "expected 400 status");
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientsNoCompressedResponsesDataProvider")
  public void testException(RestClient client, RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    try
    {
      Request<Void> request = builders.<Void>action("ExceptionTest").build();
      client.sendRequest(request).getResponse().getEntity();
      Assert.fail("expected exception");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
      Assert.assertTrue(e.getServiceErrorMessage().contains("Test Exception"));
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientsNoCompressedResponsesDataProvider")
  public void test404(RestClient client, RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<Greeting> request = builders.get().id(999L).build();
    ResponseFuture<Greeting> future = client.sendRequest(request);
    try
    {
      future.getResponse();
      Assert.fail("expected 404");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_404_NOT_FOUND.getCode());
    }
  }
}
