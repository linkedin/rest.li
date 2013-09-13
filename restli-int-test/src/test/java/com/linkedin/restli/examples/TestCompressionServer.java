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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.compression.Bzip2Compressor;
import com.linkedin.r2.filter.compression.ClientCompressionFilter;
import com.linkedin.r2.filter.compression.CompressionException;
import com.linkedin.r2.filter.compression.Compressor;
import com.linkedin.r2.filter.compression.DeflateCompressor;
import com.linkedin.r2.filter.compression.EncodingType;
import com.linkedin.r2.filter.compression.GzipCompressor;
import com.linkedin.r2.filter.compression.SnappyCompressor;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.common.HttpConstants;
import com.linkedin.restli.client.ActionRequest;
import com.linkedin.restli.client.BatchCreateRequest;
import com.linkedin.restli.client.BatchGetRequest;
import com.linkedin.restli.client.BatchUpdateRequest;
import com.linkedin.restli.client.FindRequest;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.client.util.PatchGenerator;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CreateStatus;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.Link;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.SearchMetadata;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.GreetingsBuilders;
import com.linkedin.restli.examples.greetings.server.CompressionResource;
import com.linkedin.restli.examples.groups.api.TransferOwnershipRequest;

/**
 * Same as TestGreetingsClient, but uses compression
 */
public class TestCompressionServer extends RestLiIntegrationTest
{
  private static final String URI_PREFIX = "http://localhost:1338/";
  private static final String URI_PREFIX_WITHOUT_COMPRESSION = "http://localhost:1339/"; //This server does no compression
  private final GreetingsBuilders GREETINGS_BUILDERS;

  public TestCompressionServer()
  {
    GREETINGS_BUILDERS = new GreetingsBuilders("greetings");
  }

  @DataProvider(name="Compressor")
  public Object[][] provideCompressor(){
    return new Object[][]
        {
        { new SnappyCompressor() },
        { new Bzip2Compressor() },
        { new GzipCompressor() },
        { new DeflateCompressor()}
        };
  }

  //Returns a combination of all possible request/response compression combinations
  @DataProvider(name="ClientsCompressedResponses")
  public Object[][] provideClientsWithExpectedCompressedResponses()
  {
    // sample compression operation config
    String[] compressionOperations = {"*",
                                      "action:*",
                                      "finder:*",
                                      "finder:search",
                                      "get, batch_get, get_all",
                                      "get, batch_get, get_all, batch_create, batch_update, batch_partial_update"};
    int entries = compressionOperations.length;
    Object[][] result = new Object[entries][];

    for (String operation: compressionOperations)
    {
      Map<String, String> clientProperties = new HashMap<String, String>();
      clientProperties.put(HttpClientFactory.HTTP_RESPONSE_COMPRESSION_OPERATIONS, operation);
      TransportClientAdapter clientAdapter = new TransportClientAdapter(new HttpClientFactory(FilterChains.empty()).getClient(clientProperties));
      RestClient client = new RestClient(clientAdapter, URI_PREFIX);
      result[--entries] = new Object[]{client, operation};
    }

    return result;
  }

  /**
   * Provides clients with no response compression
   */
  @DataProvider(name="ClientsNoCompressedResponses")
  public Object[][] provideClientsWithoutCompressedResponses()
  {
    TransportClientAdapter clientAdapter = new TransportClientAdapter(new HttpClientFactory(FilterChains.empty()).
        getClient(Collections.<String, String>emptyMap()));
    return new Object[][] {
        {new RestClient(clientAdapter, URI_PREFIX)}
    };
  }

  @DataProvider(name="ContentNegotiation")
  //Tests for content negotiation, including order preference, q value preference, identity and * cases
  public Object[][] provideContentNegotiation()
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
  @DataProvider(name="Error406")
  public Object[][] provideError406()
  {
    return new Object[][]
        {
        {"identity;q=0"},
        {"*;q=0.5, identity;q=0"},
        {"*;q=0, identity;q=0.0"},
        {"*;q=0"}
        };
  }

  //Provides tests client generating requests.
  //Q values are generated by the formula 1-n/(k+1), where
  //n is the number of places behind the first entry and
  //k is the number of total entries
  @DataProvider(name="ContentEncodingGenerator")
  public Object[][] provideRequestExamples()
  {
    //Length computation:
    //1 for 0 length
    //n for 1 length
    //n^2-n for 2 length since it doesn't make sense for the same scheme to be requested twice
    //1 for 3 corner case.
    int current = 0;
    int length = 1 + EncodingType.values().length*EncodingType.values().length + 1;
    Object[][] encoding = new Object[length][];
    encoding[current++] = new Object[]{new EncodingType[]{}, ""};

    //1's
    for(EncodingType type : EncodingType.values())
    {
      encoding[current++] = new Object[]{
          new EncodingType[]{type},
          type.getHttpName() + ";q=" + "1.00"};
    }

    //2's
    for(EncodingType prev : EncodingType.values())
    {
      for(EncodingType next : EncodingType.values())
      {
        if (prev != next)
        {
          encoding[current++] = new Object[]{new EncodingType[]{prev, next},
              prev.getHttpName() + ";q=" + "1.00" + ","
              + next.getHttpName() + ";q=" + "0.67"
          };
        }
      }
    }

    //One random 3's case
    encoding[current++] = new Object[]{new EncodingType[]{
        EncodingType.DEFLATE, EncodingType.IDENTITY, EncodingType.GZIP},
        "deflate;q=1.00,identity;q=0.75,gzip;q=0.50"};

    return encoding;
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

  @Test(dataProvider = "ContentNegotiation")
  //This is meant to test for when server is NOT configured to compress anything.
  public void testCompatibleDefault(String acceptEncoding, String contentEncoding) throws HttpException, IOException
  {
    String path = CompressionResource.getPath();
    HttpClient client = new HttpClient();
    GetMethod method = new GetMethod(URI_PREFIX_WITHOUT_COMPRESSION + path + CompressionResource.getRedundantQueryExample());
    method.setPath(path);
    method.addRequestHeader(HttpConstants.ACCEPT_ENCODING, acceptEncoding);

    client.executeMethod(method);

    Assert.assertNull(method.getResponseHeader(HttpConstants.CONTENT_ENCODING));
  }

  @Test(dataProvider = "ContentEncodingGenerator")
  public void testEncodingGeneration(EncodingType[] encoding, String acceptEncoding)
  {
    ClientCompressionFilter cf = new ClientCompressionFilter(EncodingType.IDENTITY,
                                                             encoding,
                                                             Arrays.asList(new String[]{"*"}));
    Assert.assertEquals(cf.buildAcceptEncodingHeader(), acceptEncoding);
  }

  //Tests for when compression should be applied
  @Test(dataProvider = "Compressor")
  public void testCompressionBetter(Compressor compressor) throws RemoteInvocationException, HttpException, IOException, CompressionException
  {
    String path = CompressionResource.getPath();
    HttpClient client = new HttpClient();

    //Get the result uncompressed
    GetMethod method = new GetMethod(URI_PREFIX + path + CompressionResource.getRedundantQueryExample());
    method.setPath(path);
    client.executeMethod(method);
    byte[] original = method.getResponseBody().clone();

    method.releaseConnection();
    //Ensure uncompressed
    Assert.assertTrue(method.getResponseHeader(HttpConstants.CONTENT_ENCODING) == null);

    method.addRequestHeader(HttpConstants.ACCEPT_ENCODING, compressor.getContentEncodingName());
    client.executeMethod(method);

    byte[] compressed = compressor.inflate(new ByteArrayInputStream(method.getResponseBody()));
    Assert.assertEquals(compressor.getContentEncodingName(), method.getResponseHeader(HttpConstants.CONTENT_ENCODING).getValue());

    Assert.assertEquals(original, compressed);
    Assert.assertTrue(method.getResponseBody().length < original.length);
  }

  //Test compression when it is worse (lengthwise)
  @Test(dataProvider = "Compressor")
  public void testCompressionWorse(Compressor compressor) throws RemoteInvocationException, HttpException, IOException
  {
    String path = CompressionResource.getPath();
    HttpClient client = new HttpClient();

    //Get the result uncompressed
    GetMethod method = new GetMethod(URI_PREFIX + path + CompressionResource.getNoRedundantQueryExample());
    method.setPath(path);
    client.executeMethod(method);
    byte[] original = method.getResponseBody().clone();

    //Ensure uncompressed
    Assert.assertTrue(method.getResponseHeader(HttpConstants.CONTENT_ENCODING) == null);

    method.addRequestHeader(HttpConstants.ACCEPT_ENCODING, compressor.getContentEncodingName());
    client.executeMethod(method);
    byte[] compressed = method.getResponseBody();

    Assert.assertEquals(null, method.getResponseHeader(HttpConstants.CONTENT_ENCODING));

    //Ensure the results are the same
    Assert.assertEquals(original, compressed);
  }

  //Test server response parsings
  @Test(dataProvider = "ContentNegotiation")
  public void testAcceptEncoding(String acceptedEncoding, String contentEncoding) throws HttpException, IOException
  {
    String path = CompressionResource.getPath();
    HttpClient client = new HttpClient();
    GetMethod method = new GetMethod(URI_PREFIX + path + CompressionResource.getRedundantQueryExample());
    method.setPath(path);
    method.addRequestHeader(HttpConstants.ACCEPT_ENCODING, acceptedEncoding);

    client.executeMethod(method);

    if(contentEncoding == null)
    {
      Assert.assertNull(method.getResponseHeader(HttpConstants.CONTENT_ENCODING));
    }
    else
    {
      Assert.assertEquals(contentEncoding, method.getResponseHeader(HttpConstants.CONTENT_ENCODING).getValue());
    }
  }

  @Test(dataProvider = "Error406")
  public void test406Error(String acceptContent) throws HttpException, IOException
  {
    String path = CompressionResource.getPath();
    HttpClient client = new HttpClient();
    GetMethod method = new GetMethod(URI_PREFIX + path + CompressionResource.getRedundantQueryExample());
    method.setPath(path);
    method.addRequestHeader(HttpConstants.ACCEPT_ENCODING, acceptContent);

    client.executeMethod(method);

    Assert.assertEquals(method.getStatusCode(), HttpConstants.NOT_ACCEPTABLE);
    Assert.assertEquals(method.getResponseBody().length, 0);
  }

  private <T> void checkContentEncodingHeaderIsAbsent(Response<T> response)
  {
    Assert.assertFalse(response.getHeaders().containsKey(HttpConstants.CONTENT_ENCODING));
  }

  //Existing tests to ensure compatibility
  @Test(dataProvider = "ClientsNoCompressedResponses")
  public void testIntAction(RestClient client) throws RemoteInvocationException
  {
    ActionRequest<Integer> request = GREETINGS_BUILDERS.actionPurge().build();
    ResponseFuture<Integer> responseFuture = client.sendRequest(request);

    Assert.assertEquals(responseFuture.getResponse().getStatus(), 200);
    Assert.assertEquals(responseFuture.getResponse().getEntity().intValue(), 100);
    checkContentEncodingHeaderIsAbsent(responseFuture.getResponse());
  }

  @Test(dataProvider = "ClientsNoCompressedResponses")
  public void testRecordAction(RestClient client) throws RemoteInvocationException
  {
    ActionRequest<Greeting> request = GREETINGS_BUILDERS.actionSomeAction()
        .id(1L)
        .paramA(1)
        .paramB("")
        .paramC(new TransferOwnershipRequest())
        .paramD(new TransferOwnershipRequest())
        .paramE(3)
        .build();
    ResponseFuture<Greeting> responseFuture = client.sendRequest(request);
    Assert.assertEquals(responseFuture.getResponse().getStatus(), 200);
    Assert.assertNotNull(responseFuture.getResponse().getEntity());
    checkContentEncodingHeaderIsAbsent(responseFuture.getResponse());
  }

  @Test(dataProvider = "ClientsNoCompressedResponses")
  public void testUpdateToneAction(RestClient client) throws RemoteInvocationException
  {
    ActionRequest<Greeting> request = GREETINGS_BUILDERS.actionUpdateTone()
        .id(1L)
        .paramNewTone(Tone.SINCERE)
        .paramDelOld(false)
        .build();
    ResponseFuture<Greeting> responseFuture = client.sendRequest(request);
    Assert.assertEquals(responseFuture.getResponse().getStatus(), 200);
    final Greeting newGreeting = responseFuture.getResponse().getEntity();
    Assert.assertNotNull(newGreeting);
    Assert.assertEquals(newGreeting.getId().longValue(), 1L);
    Assert.assertEquals(newGreeting.getTone(), Tone.SINCERE);
    checkContentEncodingHeaderIsAbsent(responseFuture.getResponse());
  }

  @Test(dataProvider = "ClientsNoCompressedResponses")
  //test update on retrieved entity
  public void testUpdate(RestClient client) throws RemoteInvocationException, CloneNotSupportedException,
  URISyntaxException
  {
    // GET
    Request<Greeting> request = GREETINGS_BUILDERS.get().id(1L).build();
    ResponseFuture<Greeting> future = client.sendRequest(request);
    Response<Greeting> greetingResponse = future.getResponse();

    String response1 = greetingResponse.getEntity().getMessage();
    Assert.assertNotNull(response1);
    checkContentEncodingHeaderIsAbsent(future.getResponse());

    // POST
    Greeting greeting = new Greeting(greetingResponse.getEntity().data().copy());
    greeting.setMessage(response1 + "Again");

    Request<EmptyRecord> writeRequest = GREETINGS_BUILDERS.update().id(1L).input(greeting).build();
    client.sendRequest(writeRequest).getResponse();

    // GET again, to verify that our POST worked.
    Request<Greeting> request2 = GREETINGS_BUILDERS.get().id(1L).build();
    ResponseFuture<Greeting> future2 = client.sendRequest(request2);
    String response2 = future2.getResponse().getEntity().getMessage();

    Assert.assertEquals(response2, response1 + "Again");
    checkContentEncodingHeaderIsAbsent(future2.getResponse());
  }

  @Test(dataProvider = "ClientsNoCompressedResponses")
  //test update on retrieved entity
  public void testBatchGet(RestClient client) throws RemoteInvocationException, CloneNotSupportedException,
                                                                                    URISyntaxException
  {
    // GET
    Request<Greeting> request = GREETINGS_BUILDERS.get().id(1L).build();
    ResponseFuture<Greeting> future = client.sendRequest(request);
    Response<Greeting> greetingResponse = future.getResponse();

    String response1 = greetingResponse.getEntity().getMessage();
    Assert.assertNotNull(response1);
    checkContentEncodingHeaderIsAbsent(greetingResponse);
  }

  @Test(dataProvider = "ClientsNoCompressedResponses")
  public void testPartialUpdate(RestClient client) throws RemoteInvocationException, CloneNotSupportedException, URISyntaxException
  {
    // GET
    Request<Greeting> request = GREETINGS_BUILDERS.get().id(1L).build();
    ResponseFuture<Greeting> future = client.sendRequest(request);
    Response<Greeting> greetingResponse = future.getResponse();

    Greeting original = greetingResponse.getEntity();
    checkContentEncodingHeaderIsAbsent(greetingResponse);

    // POST
    Greeting greeting = new Greeting(original.data().copy());
    greeting.setMessage(original.getMessage() + " Again");

    PatchRequest<Greeting> patch = PatchGenerator.diff(original, greeting);

    Request<EmptyRecord> writeRequest = GREETINGS_BUILDERS.partialUpdate().id(1L).input(patch).build();
    int status = client.sendRequest(writeRequest).getResponse().getStatus();
    Assert.assertEquals(status, HttpStatus.S_204_NO_CONTENT.getCode());

    // GET again, to verify that our POST worked.
    Request<Greeting> request2 = GREETINGS_BUILDERS.get().id(1L).build();
    ResponseFuture<Greeting> future2 = client.sendRequest(request2);
    String response2 = future2.getResponse().getEntity().getMessage();

    Assert.assertEquals(response2, greeting.getMessage());
    checkContentEncodingHeaderIsAbsent(future2.getResponse());
  }

  @Test(dataProvider = "ClientsNoCompressedResponses")
  //test cookbook example from quickstart wiki
  public void testCookbook(RestClient restClient) throws Exception
  {
    // GET
    Request<Greeting> request = GREETINGS_BUILDERS.get().id(1L).build();
    ResponseFuture<Greeting> future = restClient.sendRequest(request);
    Response<Greeting> greetingResponse = future.getResponse();

    Assert.assertNotNull(greetingResponse.getEntity().getMessage());
    checkContentEncodingHeaderIsAbsent(greetingResponse);

    // POST
    Greeting greeting = new Greeting(greetingResponse.getEntity().data().copy());
    final String NEW_MESSAGE = "This is a new message!";
    greeting.setMessage(NEW_MESSAGE);

    Request<EmptyRecord> writeRequest = GREETINGS_BUILDERS.update().id(1L).input(greeting).build();
    restClient.sendRequest(writeRequest).getResponse();

    // GET again, to verify that our POST worked.
    Request<Greeting> request2 = GREETINGS_BUILDERS.get().id(1L).build();
    ResponseFuture<Greeting> future2 = restClient.sendRequest(request2);
    greetingResponse = future2.get();

    Assert.assertEquals(greetingResponse.getEntity().getMessage(), NEW_MESSAGE);
    checkContentEncodingHeaderIsAbsent(greetingResponse);

    // shut down client
    FutureCallback<None> futureCallback = new FutureCallback<None>();
    restClient.shutdown(futureCallback);
    futureCallback.get();
  }

  @Test(dataProvider = "ClientsNoCompressedResponses")
  public void testCookbookInBatch(RestClient client) throws Exception
  {
    // GET
    BatchGetRequest<Greeting> request = GREETINGS_BUILDERS.batchGet().ids(1L).build();
    ResponseFuture<BatchResponse<Greeting>> future = client.sendRequest(request);
    Response<BatchResponse<Greeting>> greetingResponse = future.getResponse();
    checkContentEncodingHeaderIsAbsent(greetingResponse);

    // POST
    Greeting greeting = new Greeting(greetingResponse.getEntity().getResults().get("1").data().copy());
    greeting.setMessage("This is a new message!");

    BatchUpdateRequest<Long, Greeting> writeRequest = GREETINGS_BUILDERS.batchUpdate().input(1L, greeting).build();
    client.sendRequest(writeRequest).getResponse();

    // GET again, to verify that our POST worked.
    BatchGetRequest<Greeting> request2 = GREETINGS_BUILDERS.batchGet().ids(1L).build();
    ResponseFuture<BatchResponse<Greeting>> future2 = client.sendRequest(request2);
    greetingResponse = future2.get();
    checkContentEncodingHeaderIsAbsent(greetingResponse);

    Greeting repeatedGreeting = new Greeting();
    repeatedGreeting.setMessage("Hello Hello");
    repeatedGreeting.setTone(Tone.SINCERE);
    BatchCreateRequest<Greeting> request3 = GREETINGS_BUILDERS.batchCreate().inputs(Arrays.asList(repeatedGreeting, repeatedGreeting)).build();
    CollectionResponse<CreateStatus> statuses = client.sendRequest(request3).getResponse().getEntity();
    for (CreateStatus status : statuses.getElements())
    {
      Assert.assertEquals(status.getStatus().intValue(), HttpStatus.S_201_CREATED.getCode());
      Assert.assertNotNull(status.getId());
    }
  }

  @Test(dataProvider = "ClientsCompressedResponses")
  public void testSearch(RestClient client, String operationsForCompression) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> findRequest = GREETINGS_BUILDERS.findBySearch().toneParam(
                                                                                                    Tone.FRIENDLY).build();
    Response<CollectionResponse<Greeting>> response = client.sendRequest(findRequest).getResponse();

    checkHeaderForCompression(response, operationsForCompression, "finder:search");

    List<Greeting> greetings = client.sendRequest(findRequest).getResponse().getEntity().getElements();
    for (Greeting g : greetings)
    {
      Assert.assertEquals(g.getTone(), Tone.FRIENDLY);
      Assert.assertNotNull(g.getMessage());
    }
  }

  @Test(dataProvider = "ClientsCompressedResponses")
  public void testSearchWithPostFilter(RestClient client, String operationsForCompression) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> findRequest = GREETINGS_BUILDERS.findBySearchWithPostFilter().paginate(0, 5).build();
    Response<CollectionResponse<Greeting>> response = client.sendRequest(findRequest).getResponse();
    checkHeaderForCompression(response, operationsForCompression, "finder:" + findRequest.getMethodName());
    CollectionResponse<Greeting> entity = response.getEntity();
    CollectionMetadata paging = entity.getPaging();
    Assert.assertEquals(paging.getStart(), 0);
    Assert.assertEquals(paging.getCount(), 5);
    Assert.assertEquals(entity.getElements().size(), 4); // expected to be 4 instead of 5 because of post filter

    // to accommodate post filtering, even though 4 are returned, next page should be 5-10.
    Link next = paging.getLinks().get(0);
    Assert.assertEquals(next.getRel(), "next");
    Assert.assertEquals(next.getHref(), "/greetings?count=5&start=5&q=searchWithPostFilter");
  }

  @Test(dataProvider = "ClientsCompressedResponses")
  public void testSearchWithTones(RestClient client, String operationsForCompression) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> req =
        GREETINGS_BUILDERS.findBySearchWithTones().tonesParam(
                                                              Arrays.asList(Tone.SINCERE, Tone.INSULTING)).build();
    ResponseFuture<CollectionResponse<Greeting>> future = client.sendRequest(req);
    Response<CollectionResponse<Greeting>> response = future.getResponse();
    checkHeaderForCompression(response, operationsForCompression, "finder:" + req.getMethodName());
    List<Greeting> greetings = response.getEntity().getElements();
    for (Greeting greeting : greetings)
    {
      Assert.assertTrue(greeting.hasTone());
      Tone tone = greeting.getTone();
      Assert.assertTrue(Tone.SINCERE.equals(tone) || Tone.INSULTING.equals(tone));
    }
  }

  @Test(dataProvider = "ClientsCompressedResponses")
  public void testSearchFacets(RestClient client, String operationsForCompression) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> req = GREETINGS_BUILDERS.findBySearchWithFacets().toneParam(
                                                                                                      Tone.SINCERE).build();
    ResponseFuture<CollectionResponse<Greeting>> future = client.sendRequest(req);
    Response<CollectionResponse<Greeting>> response = future.getResponse();
    checkHeaderForCompression(response, operationsForCompression, "finder:" + req.getMethodName());
    SearchMetadata metadata = new SearchMetadata(response.getEntity().getMetadataRaw());
    Assert.assertTrue(metadata.getFacets().size() > 0);
    // "randomly" generated data is guaranteed to have positive number of each tone
  }

  @Test(dataProvider = "ClientsCompressedResponses")
  public void testEmptyBatchGetWithProjection(RestClient client, String operationsForCompression) throws RemoteInvocationException
  {
    Request<BatchResponse<Greeting>> request = GREETINGS_BUILDERS.batchGet().ids(1000L,
                                                                                 2000L).fields(Greeting.fields().message()).build();
    Response<BatchResponse<Greeting>> response = client.sendRequest(request).getResponse();
    checkHeaderForCompression(response, operationsForCompression, "batch_get");
    BatchResponse<Greeting> entity = response.getEntity();
    Assert.assertEquals(entity.getResults().size(), 0);
  }

  @Test(dataProvider = "ClientsCompressedResponses")
  public void testBatchGetUsingCollection(RestClient client, String operationsForCompression) throws RemoteInvocationException
  {
    List<Long> ids = Arrays.asList(1L, 2L, 3L, 4L);
    Request<BatchResponse<Greeting>> request = GREETINGS_BUILDERS.batchGet().ids(ids).fields(Greeting.fields().id(), Greeting.fields().message()).build();
    Response<BatchResponse<Greeting>> response = client.sendRequest(request).getResponse();

    checkHeaderForCompression(response, operationsForCompression, "batch_get");

    BatchResponse<Greeting> entity = response.getEntity();
    Assert.assertEquals(entity.getResults().size(), ids.size());
  }

  @Test(dataProvider = "ClientsCompressedResponses")
  public void testBatchGetUsingCollectionKV(RestClient client, String operationsForCompression) throws RemoteInvocationException
  {
    List<Long> ids = Arrays.asList(1L, 2L, 3L, 4L);
    Request<BatchKVResponse<Long, Greeting>> request = GREETINGS_BUILDERS.batchGet().ids(ids).fields(Greeting.fields().id(), Greeting.fields().message()).buildKV();
    Response<BatchKVResponse<Long, Greeting>> response = client.sendRequest(request).getResponse();

    checkHeaderForCompression(response, operationsForCompression, "batch_get");

    BatchKVResponse<Long, Greeting> entity = response.getEntity();
    Assert.assertEquals(entity.getResults().size(), ids.size());
    for(Map.Entry<Long, Greeting> entry : entity.getResults().entrySet())
    {
      Assert.assertEquals(entry.getKey(), entry.getValue().getId());
    }
  }

  /**
   * Ensures that the Content-Encoding header is present, if it should be
   */
  private <T> void checkHeaderForCompression(Response<T> response, String operationsConfig, String methodName)
  {
    String contentEncodingHeader = response.getHeader(HttpConstants.CONTENT_ENCODING);
    String allPossibleAcceptEncodings = "gzip, deflate, bzip2, snappy";

    Map<String, Set<String>> methodsAndFamilies = getCompressionMethods(operationsConfig);
    Set<String> methods = methodsAndFamilies.get("methods");
    Set<String> families = methodsAndFamilies.get("families");

    if (shouldCompress(families, methods, methodName))
    {
      if (contentEncodingHeader == null)
      {
        Assert.fail("Content-Encoding header absent");
      }
      Assert.assertTrue(allPossibleAcceptEncodings.contains(contentEncodingHeader));
    }
    else
    {
      Assert.assertNull(contentEncodingHeader);
    }
  }

  private boolean shouldCompress(Set<String> families, Set<String> methods, String methodName)
  {
    return families.contains("*") ||
           methods.contains(methodName) ||
           (methodName.contains(":") && families.contains(methodName.split(":")[0]));
  }

  /**
   * Converts compression config into a set of methods and families that are supposed to have compression.
   * The returned map has two keys, "methods" and "families".
   * "methods" maps to a Set of all methods for compression
   * "families" maps to a Set of all families for compression. Includes "*" if it is present
   * @param operationsConfig
   * @return
   */
  private Map<String, Set<String>> getCompressionMethods(String operationsConfig)
  {
    Map<String, Set<String>> methodsAndFamilies = new HashMap<String, Set<String>>();
    methodsAndFamilies.put("methods", new HashSet<String>());
    methodsAndFamilies.put("families", new HashSet<String>());
    for (String operation: operationsConfig.split(","))
    {
      operation = operation.trim();
      if (operation.equals("*"))
      {
        // treat "*" as a family for testing
        methodsAndFamilies.get("families").add(operation);
      }
      else if (operation.endsWith(":*"))
      {
        // this is a family
        methodsAndFamilies.get("families").add(operation.split(":")[0]);
      }
      else
      {
        // this is a method
        methodsAndFamilies.get("methods").add(operation);
      }
    }
    return methodsAndFamilies;
  }

  @Test(dataProvider = "ClientsNoCompressedResponses")
  public void testMalformedPagination(RestClient client) throws RemoteInvocationException
  {
    expectPaginationError("-1", client);
    expectPaginationError("abc", client);
  }

  private void expectPaginationError(String count, RestClient client) throws RemoteInvocationException
  {
    try
    {
      FindRequest<Greeting> request = new GreetingsBuilders().findBySearch().name("search").setParam("count", count).build();
      client.sendRequest(request).getResponse();
      Assert.fail("expected exception");
    }
    catch (RestException e)
    {
      Assert.assertEquals(e.getResponse().getStatus(), HttpStatus.S_400_BAD_REQUEST.getCode(), "expected 400 status");
    }
  }

  @Test(dataProvider = "ClientsNoCompressedResponses")
  public void testException(RestClient client) throws RemoteInvocationException
  {
    try
    {
      ActionRequest<Void> request = GREETINGS_BUILDERS.actionExceptionTest().build();
      client.sendRequest(request).getResponse().getEntity();
      Assert.fail("expected exception");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
      Assert.assertTrue(e.getServiceErrorMessage().contains("Test Exception"));
    }
  }

  @Test(dataProvider = "ClientsNoCompressedResponses")
  public void test404(RestClient client) throws RemoteInvocationException
  {
    Request<Greeting> request = GREETINGS_BUILDERS.get().id(999L).build();
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
