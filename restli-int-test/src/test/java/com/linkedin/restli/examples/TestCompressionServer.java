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
import com.linkedin.r2.filter.CompressionConfig;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.compression.Bzip2Compressor;
import com.linkedin.r2.filter.compression.ClientCompressionFilter;
import com.linkedin.r2.filter.compression.CompressionException;
import com.linkedin.r2.filter.compression.Compressor;
import com.linkedin.r2.filter.compression.DeflateCompressor;
import com.linkedin.r2.filter.compression.EncodingType;
import com.linkedin.r2.filter.compression.GzipCompressor;
import com.linkedin.r2.filter.compression.ServerCompressionFilter;
import com.linkedin.r2.filter.compression.SnappyCompressor;
import com.linkedin.r2.filter.logging.SimpleLoggingFilter;
import com.linkedin.r2.filter.message.rest.RestResponseFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.common.HttpConstants;
import com.linkedin.r2.util.RequestContextUtil;
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
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.CreateStatus;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.Link;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.SearchMetadata;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.GreetingsBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsRequestBuilders;
import com.linkedin.restli.examples.greetings.server.CompressionResource;
import com.linkedin.restli.examples.groups.api.TransferOwnershipRequest;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.testutils.URIDetails;
import com.linkedin.restli.server.filter.RequestFilter;
import com.linkedin.restli.server.filter.ResponseFilter;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
  private static final String URI_PREFIX = RestLiIntegrationTest.FILTERS_URI_PREFIX;
  private static final String URI_PREFIX_WITHOUT_COMPRESSION = RestLiIntegrationTest.NO_COMPRESSION_PREFIX; //This server does no compression
  private static final String CONTENT_ENCODING_SAVED = "Content-Encoding-Saved";

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

  //Returns a combination of all possible request/response compression combinations
  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientsCompressedResponsesBatchDataProvider")
  public Object[][] clientsCompressedResponsesBatchDataProvider()
  {
    // sample compression operation config
    String[] compressionOperations = {"*",
                                      "action:*",
                                      "finder:*",
                                      "finder:search",
                                      "get, batch_get, get_all",
                                      "get, batch_get, get_all, batch_create, batch_update, batch_partial_update"};
    int entries = compressionOperations.length;
    Object[][] result = new Object[entries * 4][];

    int index = entries * 4 - 1;
    for (String operation: compressionOperations)
    {
      Map<String, String> clientProperties = new HashMap<String, String>();
      clientProperties.put(HttpClientFactory.HTTP_RESPONSE_COMPRESSION_OPERATIONS, operation);
      RestClient client = new RestClient(newTransportClient(clientProperties), URI_PREFIX);
      result[index--] = new Object[]{ client, operation, RestliRequestOptions.DEFAULT_OPTIONS, Arrays.asList(1000L, 2000L), 0 };
      result[index--] = new Object[]{ client, operation, TestConstants.FORCE_USE_NEXT_OPTIONS, Arrays.asList(1000L, 2000L), 0 };
      result[index--] = new Object[]{ client, operation, RestliRequestOptions.DEFAULT_OPTIONS, Arrays.asList(1L, 2L, 3L, 4L), 4 };
      result[index--] = new Object[]{ client, operation, TestConstants.FORCE_USE_NEXT_OPTIONS, Arrays.asList(1L, 2L, 3L, 4L), 4 };
    }

    return result;
  }

  //Returns a combination of all possible request/response compression combinations
  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientsCompressedResponsesBuilderDataProvider")
  public Object[][] clientsCompressedResponsesBuilderDataProvider()
  {
    // sample compression operation config
    String[] compressionOperations = {"*",
                                      "action:*",
                                      "finder:*",
                                      "finder:search",
                                      "get, batch_get, get_all",
                                      "get, batch_get, get_all, batch_create, batch_update, batch_partial_update"};
    int entries = compressionOperations.length;
    Object[][] result = new Object[entries * 4][];

    int index = entries * 4 - 1;
    for (String operation: compressionOperations)
    {
      Map<String, String> clientProperties = new HashMap<String, String>();
      clientProperties.put(HttpClientFactory.HTTP_RESPONSE_COMPRESSION_OPERATIONS, operation);
      RestClient client = new RestClient(newTransportClient(clientProperties), URI_PREFIX);
      result[index--] = new Object[]{ client, operation, new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders()),
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion()};
      result[index--] = new Object[]{ client, operation, new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)),
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion()};
      result[index--] = new Object[]{ client, operation, new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders()),
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion()};
      result[index--] = new Object[]{ client, operation, new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)),
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion()};
    }

    return result;
  }
  /**
   * Provides clients with no response compression
   */
  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientsCookbookDataProvider")
  public Object[][] clientsCookbookDataProvider()
  {
    return new Object[][]
      {
        { new RestClient(getDefaultTransportClient(), URI_PREFIX), RestliRequestOptions.DEFAULT_OPTIONS },
        { new RestClient(getDefaultTransportClient(), URI_PREFIX), TestConstants.FORCE_USE_NEXT_OPTIONS },
      };
  }

  /**
   * Provides clients with no response compression
   */
  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientsNoCompressedResponsesDataProvider")
  public Object[][] clientsNoCompressedResponsesDataProvider()
  {
    return new Object[][]
      {
        { new RestClient(getDefaultTransportClient(), URI_PREFIX), new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders()) },
        { new RestClient(getDefaultTransportClient(), URI_PREFIX), new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
        { new RestClient(getDefaultTransportClient(), URI_PREFIX), new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders()) },
        { new RestClient(getDefaultTransportClient(), URI_PREFIX), new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) }
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

  //Provides tests client generating requests.
  //Q values are generated by the formula 1-n/(k+1), where
  //n is the number of places behind the first entry and
  //k is the number of total entries
  @DataProvider
  public Object[][] contentEncodingGeneratorDataProvider()
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
    // Because the Content-Encoding header is removed when content is decompressed,
    // we need to save the value to another header to check whether the response was compressed or not.
    class SaveContentEncodingHeaderFilter implements RestResponseFilter
    {
      @Override
      public void onRestResponse(RestResponse res,
                          RequestContext requestContext,
                          Map<String, String> wireAttrs,
                          NextFilter<RestRequest, RestResponse> nextFilter)
      {
        String contentEncoding = res.getHeader(HttpConstants.CONTENT_ENCODING);
        if (contentEncoding != null)
        {
          res = res.builder().addHeaderValue(CONTENT_ENCODING_SAVED, contentEncoding).build();
        }
        nextFilter.onResponse(res, requestContext, wireAttrs);
      }

      @Override
      public void onRestError(Throwable ex,
                              RequestContext requestContext,
                              Map<String, String> wireAttrs,
                              NextFilter<RestRequest, RestResponse> nextFilter)
      {
        nextFilter.onError(ex, requestContext, wireAttrs);
      }
    }
    super.init(Collections.<RequestFilter>emptyList(),
               Collections.<ResponseFilter>emptyList(),
               FilterChains.empty().addLast(new SaveContentEncodingHeaderFilter())
                   .addLast(new ServerCompressionFilter(RestLiIntTestServer.supportedCompression))
                   .addLast(new SimpleLoggingFilter()),
               true);
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
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
    get.addHeader(HttpConstants.ACCEPT_ENCODING, acceptEncoding);

    HttpResponse response = client.execute(get);

    Assert.assertNull(response.getFirstHeader(CONTENT_ENCODING_SAVED));
  }

  @Test(dataProvider = "contentEncodingGeneratorDataProvider")
  public void testEncodingGeneration(EncodingType[] encoding, String acceptEncoding)
  {
    ClientCompressionFilter cf = new ClientCompressionFilter(EncodingType.IDENTITY,
                                                             new CompressionConfig(Integer.MAX_VALUE),
                                                             encoding,
                                                             Arrays.asList(new String[]{"*"}));
    Assert.assertEquals(cf.buildAcceptEncodingHeader(), acceptEncoding);
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
    Assert.assertTrue(response.getFirstHeader(CONTENT_ENCODING_SAVED) == null);

    get.addHeader(HttpConstants.ACCEPT_ENCODING, compressor.getContentEncodingName());
    response = client.execute(get);

    byte[] compressed = compressor.inflate(response.getEntity().getContent());
    Assert.assertEquals(compressor.getContentEncodingName(), response.getFirstHeader(CONTENT_ENCODING_SAVED).getValue());

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
    Assert.assertTrue(response.getFirstHeader(CONTENT_ENCODING_SAVED) == null);

    get.addHeader(HttpConstants.ACCEPT_ENCODING, compressor.getContentEncodingName());
    response = client.execute(get);
    String compressed = EntityUtils.toString(response.getEntity());

    Assert.assertEquals(null, response.getFirstHeader(CONTENT_ENCODING_SAVED));

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
    get.addHeader(HttpConstants.ACCEPT_ENCODING, acceptedEncoding);

    HttpResponse response = client.execute(get);

    if(contentEncoding == null)
    {
      Assert.assertNull(response.getFirstHeader(CONTENT_ENCODING_SAVED));
    }
    else
    {
      Assert.assertEquals(contentEncoding, response.getFirstHeader(CONTENT_ENCODING_SAVED).getValue());
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
    get.addHeader(HttpConstants.ACCEPT_ENCODING, acceptContent);

    HttpResponse response = client.execute(get);

    Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpConstants.NOT_ACCEPTABLE);
    Assert.assertEquals(response.getEntity().getContentLength(), 0);
  }

  private <T> void checkContentEncodingHeaderIsAbsent(Response<T> response)
  {
    Assert.assertFalse(response.getHeaders().containsKey(CONTENT_ENCODING_SAVED));
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

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientsCompressedResponsesBuilderDataProvider")
  public void testSearch(RestClient client, String operationsForCompression, RootBuilderWrapper<Long, Greeting> builders,
      ProtocolVersion protocolVersion) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> findRequest = builders.findBy("Search").setQueryParam("tone", Tone.FRIENDLY).build();
    Response<CollectionResponse<Greeting>> response = client.sendRequest(findRequest).getResponse();

    checkHeaderForCompression(response, operationsForCompression, "finder:search");

    List<Greeting> greetings = client.sendRequest(findRequest).getResponse().getEntity().getElements();
    for (Greeting g : greetings)
    {
      Assert.assertEquals(g.getTone(), Tone.FRIENDLY);
      Assert.assertNotNull(g.getMessage());
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientsCompressedResponsesBuilderDataProvider")
  public void testSearchWithoutDecompression(RestClient client, String operationsForCompression, RootBuilderWrapper<Long, Greeting> builders,
                                             ProtocolVersion protocolVersion) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> findRequest = builders.findBy("Search").setQueryParam("tone", Tone.FRIENDLY).build();
    RequestContext requestContext = new RequestContext();
    RequestContextUtil.turnOffResponseDecompression(requestContext);

    Map<String, Set<String>> methodsAndFamilies = getCompressionMethods(operationsForCompression);
    Set<String> methods = methodsAndFamilies.get("methods");
    Set<String> families = methodsAndFamilies.get("families");

    if (shouldCompress(families, methods, "finder:search"))
    {
      // The server sends a compressed response, but the client does not decompress it so it cannot read the response.
      try
      {
        client.sendRequest(findRequest, requestContext).getResponse();
        Assert.fail("Expected RemoteInvocationException, but getResponse() succeeded.");
      }
      catch (RemoteInvocationException e)
      {
        Assert.assertEquals(e.getCause().getMessage(), "Could not decode REST response");
      }
    }
    else
    {
      // The server doesn't compress the response in the first place, so the client can read the response.
      client.sendRequest(findRequest, requestContext).getResponse();
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientsCompressedResponsesBuilderDataProvider")
  public void testSearchWithPostFilter(RestClient client, String operationsForCompression, RootBuilderWrapper<Long, Greeting> builders,
      ProtocolVersion protocolVersion) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> findRequest = builders.findBy("SearchWithPostFilter").paginate(0, 5).build();
    Response<CollectionResponse<Greeting>> response = client.sendRequest(findRequest).getResponse();
    checkHeaderForCompression(response, operationsForCompression, "finder:" + findRequest.getMethodName());
    CollectionResponse<Greeting> entity = response.getEntity();
    CollectionMetadata paging = entity.getPaging();
    Assert.assertEquals(paging.getStart().intValue(), 0);
    Assert.assertEquals(paging.getCount().intValue(), 5);
    Assert.assertEquals(entity.getElements().size(), 4); // expected to be 4 instead of 5 because of post filter

    // to accommodate post filtering, even though 4 are returned, next page should be 5-10.
    Link next = paging.getLinks().get(0);
    Assert.assertEquals(next.getRel(), "next");

    //Query parameter order is non deterministic
    //greetings?count=5&start=5&q=searchWithPostFilter"
    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("count", "5");
    queryParamsMap.put("start", "5");
    queryParamsMap.put("q", "searchWithPostFilter");

    final URIDetails uriDetails = new URIDetails(protocolVersion, "/greetings", null, queryParamsMap, null);
    URIDetails.testUriGeneration(next.getHref(), uriDetails);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientsCompressedResponsesBuilderDataProvider")
  public void testSearchWithTones(RestClient client, String operationsForCompression, RootBuilderWrapper<Long, Greeting> builders,
      ProtocolVersion protocolVersion) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> req =
        builders.findBy("SearchWithTones").setQueryParam("tones", Arrays.asList(Tone.SINCERE, Tone.INSULTING)).build();
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

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientsCompressedResponsesBuilderDataProvider")
  public void testSearchFacets(RestClient client, String operationsForCompression, RootBuilderWrapper<Long, Greeting> builders,
      ProtocolVersion protocolVersion) throws RemoteInvocationException
  {
    Request<CollectionResponse<Greeting>> req = builders.findBy("SearchWithFacets").setQueryParam("tone", Tone.SINCERE).build();
    ResponseFuture<CollectionResponse<Greeting>> future = client.sendRequest(req);
    Response<CollectionResponse<Greeting>> response = future.getResponse();
    checkHeaderForCompression(response, operationsForCompression, "finder:" + req.getMethodName());
    SearchMetadata metadata = new SearchMetadata(response.getEntity().getMetadataRaw());
    Assert.assertTrue(metadata.getFacets().size() > 0);
    // "randomly" generated data is guaranteed to have positive number of each tone
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientsCompressedResponsesBatchDataProvider")
  public void testBatchGet(RestClient client,
                           String operationsForCompression,
                           RestliRequestOptions requestOptions,
                           List<Long> ids,
                           int expectedSuccessSize) throws RemoteInvocationException
  {
    final Request<?> request = new GreetingsBuilders(requestOptions).batchGet().ids(ids).fields(Greeting.fields().id(), Greeting.fields().message()).build();
    @SuppressWarnings("unchecked")
    final BatchResponse<Greeting> response = (BatchResponse<Greeting>) getBatchGetResponse(client, operationsForCompression, request);
    Assert.assertEquals(response.getResults().size(), expectedSuccessSize);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientsCompressedResponsesBatchDataProvider")
  public void testBatchGetKV(RestClient client,
                             String operationsForCompression,
                             RestliRequestOptions requestOptions,
                             List<Long> ids,
                             int expectedSuccessSize) throws RemoteInvocationException
  {
    final Request<?> request = new GreetingsBuilders(requestOptions).batchGet().ids(ids).fields(Greeting.fields().id(), Greeting.fields().message()).buildKV();
    @SuppressWarnings("unchecked")
    final BatchKVResponse<Long, Greeting> response = (BatchKVResponse<Long, Greeting>) getBatchGetResponse(client, operationsForCompression, request);
    Assert.assertEquals(response.getResults().size(), expectedSuccessSize);

    for (Map.Entry<Long, Greeting> entry : response.getResults().entrySet())
    {
      Assert.assertEquals(entry.getKey(), entry.getValue().getId());
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "clientsCompressedResponsesBatchDataProvider")
  public void testBatchGetEntity(RestClient client,
                                 String operationsForCompression,
                                 RestliRequestOptions requestOptions,
                                 List<Long> ids,
                                 int expectedSuccessSize) throws RemoteInvocationException
  {
    final Request<?> request = new GreetingsRequestBuilders(requestOptions).batchGet().ids(ids).fields(Greeting.fields().id(), Greeting.fields().message()).build();
    @SuppressWarnings("unchecked")
    final BatchKVResponse<Long, EntityResponse<Greeting>> response = (BatchKVResponse<Long, EntityResponse<Greeting>>) getBatchGetResponse(client, operationsForCompression, request);
    Assert.assertEquals(response.getResults().size() - response.getErrors().size(), expectedSuccessSize);

    for (Map.Entry<Long, EntityResponse<Greeting>> entry : response.getResults().entrySet())
    {
      if (entry.getValue().hasEntry())
      {
        Assert.assertEquals(entry.getKey(), entry.getValue().getEntity().getId());
      }
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

  private Object getBatchGetResponse(RestClient client, String operationsForCompression, Request<?> request) throws RemoteInvocationException
  {
    final Response<?> response = client.sendRequest(request).getResponse();
    checkHeaderForCompression(response, operationsForCompression, "batch_get");
    return response.getEntity();
  }

  /**
   * Ensures that the Content-Encoding header is present, if it should be
   */
  private <T> void checkHeaderForCompression(Response<T> response, String operationsConfig, String methodName)
  {
    String contentEncodingHeader = response.getHeader(CONTENT_ENCODING_SAVED);
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
