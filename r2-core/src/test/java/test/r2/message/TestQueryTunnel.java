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

/* $Id$ */

package test.r2.message;

import com.linkedin.common.callback.Callback;
import com.linkedin.data.ByteString;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.QueryTunnelUtil;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.stream.StreamRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

/**
 * @author Doug Young
 * @version $Revision$
 */
public class TestQueryTunnel
{
  private final String _requestType;

  @Factory(dataProvider = "requestType")
  public TestQueryTunnel(String requestType)
  {
    _requestType = requestType;
  }

  @DataProvider
  public static Object[][] requestType()
  {
    return new Object[][] {{"Rest"}, {"Stream"}};
  }

  @Test
  public void testSimpleGetNoArgs() throws Exception
  {
    RestRequest request = new RestRequestBuilder(new URI("http://localhost:7279"))
                                       .setMethod("GET").build();

    // Pass to encode, but there are no query args, so nothing should change
    RestRequest encoded = encode(request, 0);
    Assert.assertEquals(request.getURI(), encoded.getURI());
    Assert.assertEquals(request.getMethod(), encoded.getMethod());

    // Pass the request - either one will do - to the decode side. Again, nothing should change
    RequestContext requestContext = new RequestContext();
    RestRequest decoded = decode(encoded, requestContext);
    Assert.assertEquals(request.getURI(), decoded.getURI());
    Assert.assertEquals(request.getMethod(), decoded.getMethod());
    Assert.assertEquals(request.getHeader("Content-Type"), decoded.getHeader("Content-Type"));
    Assert.assertNull(requestContext.getLocalAttr(R2Constants.IS_QUERY_TUNNELED));
  }

  @Test
  public void testSimpleGetWithArgs() throws Exception
  {
    RestRequest request = new RestRequestBuilder(new URI("http://localhost:7279?q=one&x=10&y=15"))
                                       .setMethod("GET").build();

    // Process GET request with params. Set threshold to 0, which should convert the result to a POST
    // with no query, and a body
    RestRequest encoded = encode(request, 0);
    Assert.assertEquals(encoded.getMethod(), "POST");
    Assert.assertEquals(encoded.getURI().toString(), "http://localhost:7279");
    Assert.assertTrue(encoded.getEntity().length() > 0);

    // Decode, and we should get the original request back
    RequestContext requestContext = new RequestContext();
    RestRequest decoded = decode(encoded, requestContext);
    Assert.assertEquals(request.getURI(), decoded.getURI());
    Assert.assertEquals(request.getMethod(), decoded.getMethod());
    Assert.assertEquals(request.getHeader("Content-Type"), decoded.getHeader("Content-Type"));
    Assert.assertTrue((Boolean) requestContext.getLocalAttr(R2Constants.IS_QUERY_TUNNELED));
  }

  @Test
  public void testNonAsciiParams() throws Exception
  {
    StringBuilder sb = new StringBuilder("http://localhost:7279?");
    sb.append("eWithAcuteAccent=\u00e9");
    sb.append("&");
    sb.append("chineseCharacter=\u4e80");

    RestRequest req = new RestRequestBuilder(new URI(sb.toString())).setMethod("GET").build();

    RequestContext requestContext = new RequestContext();
    RestRequest roundTrip = decode(encode(req, 0), requestContext);

    Assert.assertEquals(req, roundTrip);
    Assert.assertTrue((Boolean) requestContext.getLocalAttr(R2Constants.IS_QUERY_TUNNELED));
  }

  @Test
  public void testPostWithEntity() throws Exception
  {
    // Test a request with an entity and a query string, to be encoded as multipart/mixed
    RestRequest request = new RestRequestBuilder(new URI("http://localhost:7279?q=one&x=10&y=15"))
                                       .setMethod("POST")
                                       .setEntity(new String("{\name\":\"value\"}").getBytes())
                                       .setHeader("Content-Type", "application/json").build();

    // Test Conversion, should have a multipart body
    RestRequest encoded = encode(request, 0);
    Assert.assertEquals(encoded.getMethod(), "POST");
    Assert.assertEquals(encoded.getURI().toString(), "http://localhost:7279");
    Assert.assertTrue(encoded.getEntity().length() > 0);
    Assert.assertTrue(encoded.getHeader("Content-Type").startsWith("multipart/mixed"));
    Assert.assertEquals(encoded.getHeader("Content-Length"), Integer.toString(encoded.getEntity().length()));

    // Decode, and we should get the original request back
    RequestContext requestContext = new RequestContext();
    RestRequest decoded = decode(encoded, requestContext);
    Assert.assertEquals(request.getURI(), decoded.getURI());
    Assert.assertEquals(request.getMethod(), decoded.getMethod());
    Assert.assertEquals(request.getEntity(), decoded.getEntity());
    Assert.assertEquals(request.getHeader("Content-Type"), decoded.getHeader("Content-Type"));
    Assert.assertTrue((Boolean) requestContext.getLocalAttr(R2Constants.IS_QUERY_TUNNELED));
  }

  @Test
  public void testPostWithEntityAndContentLength() throws Exception
  {
    // Test a request with an entity and a query string, to be encoded as multipart/mixed
    RestRequest request = new RestRequestBuilder(new URI("http://localhost:7279?q=one&x=10&y=15"))
        .setMethod("POST")
        .setEntity(new String("{\name\":\"value\"}").getBytes())
        .setHeader("Content-Length", "15")
        .setHeader("Content-Type", "application/json").build();

    // Test Conversion, should have a multipart body
    RestRequest encoded = encode(request, 0);
    Assert.assertEquals(encoded.getMethod(), "POST");
    Assert.assertEquals(encoded.getURI().toString(), "http://localhost:7279");
    Assert.assertTrue(encoded.getEntity().length() > 0);
    Assert.assertTrue(encoded.getHeader("Content-Type").startsWith("multipart/mixed"));
    Assert.assertEquals(encoded.getHeader("Content-Length"), Integer.toString(encoded.getEntity().length()));

    // Decode, and we should get the original request back
    RequestContext requestContext = new RequestContext();
    RestRequest decoded = decode(encoded, requestContext);
    Assert.assertEquals(request.getURI(), decoded.getURI());
    Assert.assertEquals(request.getMethod(), decoded.getMethod());
    Assert.assertEquals(request.getEntity(), decoded.getEntity());
    Assert.assertEquals(request.getHeader("Content-Type"), decoded.getHeader("Content-Type"));
    Assert.assertTrue((Boolean) requestContext.getLocalAttr(R2Constants.IS_QUERY_TUNNELED));
  }

  @Test
  public void testPassThru() throws Exception
  {
    // Test a request with a query and body, with the threshold set to max, which should do nothing
    RestRequest request = new RestRequestBuilder(new URI("http://localhost:7279?q=one&x=10&y=15"))
                                       .setMethod("GET")
                                       .setEntity(new String("{\name\":\"value\"}").getBytes())
                                       .setHeader("Content-Type", "application/json").build();

    // Should do nothing, request should come back unchanged
    RestRequest encoded = encode(request, Integer.MAX_VALUE);
    Assert.assertEquals(request.getURI(), encoded.getURI());
    Assert.assertEquals(request.getMethod(), encoded.getMethod());
    Assert.assertEquals(request.getEntity(), encoded.getEntity());
    Assert.assertEquals(request.getHeader("Content-Type"), encoded.getHeader("Content-Type"));
  }

  @Test
  public void testTunneledPut() throws Exception
  {
    // Just test another request type
    RestRequest request = new RestRequestBuilder(new URI("http://localhost:7279?q=one&x=10&y=15"))
                                       .setMethod("PUT")
                                       .setEntity(new String("{\name\":\"value\"}").getBytes())
                                       .setHeader("Content-Type", "application/json").build();

    RequestContext requestContext = new RequestContext();
    RestRequest tunneled = decode(encode(request, 0), requestContext);
    Assert.assertEquals(request.getURI(), tunneled.getURI());
    Assert.assertEquals(request.getMethod(), tunneled.getMethod());
    Assert.assertEquals(request.getEntity(), tunneled.getEntity());
    Assert.assertEquals(request.getHeader("Content-Type"), tunneled.getHeader("Content-Type"));
    Assert.assertEquals(request.getEntity().length(), Integer.parseInt(tunneled.getHeader("Content-Length")));
    Assert.assertTrue((Boolean) requestContext.getLocalAttr(R2Constants.IS_QUERY_TUNNELED));
  }

  @Test
  public void testTunneledMissingContentType() throws Exception
  {
    // When using R2 to encode, there should always be a Content-Type in any encoded request
    // But someone could hand-construct a query without one, so test that we catch it and throw an
    // exception
    RestRequest request = new RestRequestBuilder(new URI("http://localhost:7279?q=one&x=10&y=15"))
        .setMethod("PUT")
        .setEntity(new String("{\"name\":\"value\"}").getBytes()).build();

    try {
      // Should fail because there is no Content-Type specified
      encode(request, 0);
      Assert.fail("Expected Exception");
    }
    catch(Exception e)
    {
    }
  }

  @Test
  public void testTunneledHandConstructedOverride() throws Exception
  {
    // Example of a hand-constructed "encoded" request
    RestRequest request = new RestRequestBuilder(new URI("http://localhost:7279"))
        .setMethod("POST")
        .setHeader("X-HTTP-Method-Override", "GET")
        .setHeader("Content-Type", "application/x-www-form-urlencoded")
        .setEntity(new String("q=123").getBytes()).build();

    RequestContext requestContext = new RequestContext();
    RestRequest decoded = decode(request, requestContext);
    Assert.assertEquals(decoded.getURI().toString(), "http://localhost:7279?q=123");
    Assert.assertEquals(decoded.getMethod(), "GET");
    Assert.assertTrue((Boolean) requestContext.getLocalAttr(R2Constants.IS_QUERY_TUNNELED));
  }

  @Test
  public void testNestedMultiPartBody() throws Exception
  {
    // Construct a request with multi-part entity as a body and make sure it can be tunneled
    // The entity will be nested - the original multi-part body will become the 2nd part of the tunneled
    // body with the query passed as the form-encoded url

    final MimeMultipart multi = new MimeMultipart("mixed");

    MimeBodyPart partOne = new MimeBodyPart();
    partOne.setContent(new String("{\"name\":\"value\"}").getBytes(), "application/json");
    partOne.setHeader("Content-Type", "application/json");

    // Encode query params as form-urlencoded
    MimeBodyPart partTwo = new MimeBodyPart();
    partTwo.setContent("x=y&z=w&q=10", "application/x-www-form-urlencoded");
    partTwo.setHeader("Content-Type", "application /x-www-form-urlencoded");

    multi.addBodyPart(partTwo);
    multi.addBodyPart(partOne);

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    multi.writeTo(os);

    // Create request with multi-part body and query args
    final RestRequest request = new RestRequestBuilder(new URI("http://localhost:7279?args=xyz"))
        .setMethod("PUT")
        .setHeader("Content-Type", multi.getContentType())
        .setEntity(ByteString.copy(os.toByteArray())).build();

    // Encode and verify
    RestRequest encoded = encode(request, 0);
    Assert.assertEquals(encoded.getMethod(), "POST");
    Assert.assertEquals(encoded.getURI().toString(), "http://localhost:7279");
    Assert.assertTrue(encoded.getEntity().length() > 0);
    Assert.assertTrue(encoded.getHeader("Content-Type").startsWith("multipart/mixed"));
    Assert.assertEquals(encoded.getHeader("Content-Length"), Integer.toString(encoded.getEntity().length()));

    // Decode and make sure we have the original request back
    RequestContext requestContext = new RequestContext();
    RestRequest decoded = decode(encoded, requestContext);
    Assert.assertEquals(decoded.getURI().toString(), "http://localhost:7279?args=xyz");
    Assert.assertEquals(decoded.getMethod(), "PUT");
    Assert.assertEquals(decoded.getEntity(), request.getEntity());
    Assert.assertTrue(encoded.getHeader("Content-Type").startsWith("multipart/mixed"));
    Assert.assertTrue((Boolean) requestContext.getLocalAttr(R2Constants.IS_QUERY_TUNNELED));
    Assert.assertEquals(decoded.getHeader("Content-Length"), Integer.toString(request.getEntity().length()));
  }

  @Test
  public void testTunneledLongQuery() throws Exception
  {
    // Just test a true long query, to go beyond the simple 0 threshold tests above

    String query= "q=queryString";
    for(int i=0; i<10000; i++)
      query += "&a="+i;

    RestRequest request = new RestRequestBuilder(new URI("http://localhost:7279?" + query))
                                      .setMethod("GET").build();

    RestRequest encoded = encode(request, query.length()-1);  // Set threshold < query length
    Assert.assertEquals(encoded.getMethod(), "POST");
    Assert.assertEquals(encoded.getURI().toString(), "http://localhost:7279");
    Assert.assertTrue(encoded.getEntity().length() == query.length());
    Assert.assertEquals(encoded.getHeader("Content-Type"), "application/x-www-form-urlencoded");
    Assert.assertEquals(encoded.getHeader("Content-Length"), Integer.toString(encoded.getEntity().length()));

    RequestContext requestContext = new RequestContext();
    RestRequest decoded = decode(encoded, requestContext);
    Assert.assertEquals(decoded.getURI(), request.getURI());
    Assert.assertEquals(decoded.getMethod(), "GET");
    Assert.assertTrue((Boolean) requestContext.getLocalAttr(R2Constants.IS_QUERY_TUNNELED));
  }

  @Test
  public void testModifiedQuestionQuery() throws Exception
  {
    // Test case where someone has added a "?" underneath us with no args - make sure
    // we re-append the query correctly
    // This test is motivated by some test frameworks that add params to queries in EI

    String query= "q=queryString&a=1&b=2";

    RestRequest request = new RestRequestBuilder(new URI("http://localhost:7279?" + query))
        .setMethod("GET").build();

    RestRequest encoded = encode(request, query.length()-1);  // Set threshold < query length
    Assert.assertEquals(encoded.getMethod(), "POST");
    Assert.assertEquals(encoded.getURI().toString(), "http://localhost:7279");
    Assert.assertTrue(encoded.getEntity().length() == query.length());
    Assert.assertEquals(encoded.getHeader("Content-Type"), "application/x-www-form-urlencoded");

    RestRequestBuilder rb = encoded.builder()
                             .setURI(new URI(encoded.getURI().toString() + "?"));
    RestRequest modified = rb.build();

    RequestContext requestContext = new RequestContext();
    RestRequest decoded = decode(modified, requestContext);
    Assert.assertEquals(decoded.getURI().toString(), "http://localhost:7279?" + query);
    Assert.assertTrue((Boolean) requestContext.getLocalAttr(R2Constants.IS_QUERY_TUNNELED));
  }

  @Test
  public void testModifiedQuery() throws Exception
  {
    // Test case where someone has added a query param to the request underneath us
    // This test is motivated by some test frameworks that add params to queries in EI

    String query= "q=queryString&a=1&b=2";

    RestRequest request = new RestRequestBuilder(new URI("http://localhost:7279?" + query))
        .setMethod("GET").build();

    RestRequest encoded = encode(request, query.length()-1);  // Set threshold < query length
    Assert.assertEquals(encoded.getMethod(), "POST");
    Assert.assertEquals(encoded.getURI().toString(), "http://localhost:7279");
    Assert.assertTrue(encoded.getEntity().length() == query.length());
    Assert.assertEquals(encoded.getHeader("Content-Type"), "application/x-www-form-urlencoded");

    RestRequestBuilder rb = encoded.builder()
        .setURI(new URI(encoded.getURI().toString() + "?uh=f"));
    RestRequest modified = rb.build();

    RequestContext requestContext = new RequestContext();
    RestRequest decoded = decode(modified, requestContext);
    Assert.assertEquals(decoded.getURI().toString(), "http://localhost:7279?uh=f&" + query);
    Assert.assertTrue((Boolean) requestContext.getLocalAttr(R2Constants.IS_QUERY_TUNNELED));
  }


  @Test
  public void testEmptyArgsQuery() throws Exception
  {
    // test query with "?" but nothing after it

    RestRequest request = new RestRequestBuilder(new URI("http://localhost:7279/foo?"))
        .setMethod("GET").build();

    RestRequest encoded = encode(request, 0);
    Assert.assertEquals(encoded.getMethod(), "GET"); // SHould not actually encode this case
    Assert.assertEquals(encoded.getURI().toString(), "http://localhost:7279/foo?");

    RequestContext requestContext = new RequestContext();
    RestRequest decoded = decode(encoded, requestContext);
    Assert.assertEquals(decoded.getURI().toString(), "http://localhost:7279/foo?");
    Assert.assertNull(requestContext.getLocalAttr(R2Constants.IS_QUERY_TUNNELED));
  }

  @Test
  public void testForceQueryTunnelFlagSet() throws Exception
  {
    RestRequest request = new RestRequestBuilder(new URI("http://localhost:7279?q=one&x=10&y=15"))
        .setMethod("GET")
        .setEntity(new String("{\name\":\"value\"}").getBytes())
        .setHeader("Content-Type", "application/json").build();

    RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(R2Constants.FORCE_QUERY_TUNNEL, true);

    // should encode
    RestRequest encoded = encode(request, requestContext, Integer.MAX_VALUE);
    Assert.assertEquals(encoded.getMethod(), "POST");
    Assert.assertEquals(encoded.getURI().toString(), "http://localhost:7279");
    Assert.assertTrue(encoded.getEntity().length() > 0);
  }

  @Test
  public void testForceQueryTunnelFlagNotSetOrFalse() throws Exception
  {
    RestRequest request = new RestRequestBuilder(new URI("http://localhost:7279?q=one&x=10&y=15"))
        .setMethod("GET")
        .setEntity(new String("{\name\":\"value\"}").getBytes())
        .setHeader("Content-Type", "application/json").build();

    // should not encode
    RequestContext emptyContext = new RequestContext();
    RestRequest encodedWithEmptyContext = encode(request, emptyContext, Integer.MAX_VALUE);
    Assert.assertEquals(request.getURI(), encodedWithEmptyContext.getURI());
    Assert.assertEquals(request.getMethod(), encodedWithEmptyContext.getMethod());
    Assert.assertEquals(request.getEntity(), encodedWithEmptyContext.getEntity());

    // should not encode
    RequestContext falseFlag = new RequestContext();
    falseFlag.putLocalAttr(R2Constants.FORCE_QUERY_TUNNEL, false);
    RestRequest encodedWithFalseFlag = encode(request, falseFlag, Integer.MAX_VALUE);
    Assert.assertEquals(request.getURI(), encodedWithFalseFlag.getURI());
    Assert.assertEquals(request.getMethod(), encodedWithFalseFlag.getMethod());
    Assert.assertEquals(request.getEntity(), encodedWithFalseFlag.getEntity());
  }

  @Test
  public void testXHttpMethodOverrideHeaderRemoved() throws Exception
  {
    RestRequest request = new RestRequestBuilder(new URI("http://localhost:7279/foo?"))
        .setMethod("GET")
        .setHeader("content-type", "application/json")
        .setHeader("x-http-method-override", "GET").build();

    RestRequest decoded = decode(request);
    Assert.assertNull(decoded.getHeader("x-http-method-override"), "Did not remove X-Http-Method-Override headers");
  }

  @Test
  public void testDecodeRequestTwice() throws Exception
  {
    RestRequest request = new RestRequestBuilder(new URI("http://localhost:7279/foo?"))
        .setMethod("GET")
        .setHeader("content-type", "application/json")
        .setHeader("x-http-method-override", "GET").build();

    RestRequest decoded = decode(request);
    Assert.assertEquals(decoded, decode(decoded), "Decoded RestRequest did not stay the same after another decode");
  }

  @Test
  public void testEncodeRejectsInvalidVerbs() throws Exception
  {
    RestRequest request = new RestRequestBuilder(new URI("http://localhost:7279?q=123"))
        .setMethod("Invalid")
        .setHeader("Content-Type", "application/x-www-form-urlencoded")
        .setEntity(new String("q=123").getBytes()).build();
    Assert.assertThrows(() -> {
      QueryTunnelUtil.encode(request, 1);
    });
  }

  @Test
  public void testDecodeRejectsInvalidVerbs() throws Exception
  {
    RestRequest request = new RestRequestBuilder(new URI("http://localhost:7279"))
        .setMethod("POST")
        .setHeader("X-HTTP-Method-Override", "Invalid")
        .setHeader("Content-Type", "application/x-www-form-urlencoded")
        .setEntity(new String("q=123").getBytes()).build();

    RequestContext requestContext = new RequestContext();
    Assert.assertThrows(() -> {
      RestRequest decoded = decode(request, requestContext);
    });
  }

  @DataProvider
  public static Object[][] mixedCaseHeaders(){
    return new Object[][] {{"CoNtEnT-TyPe"}, {"contenT-typE"}, {"cOntEnt-tYpE"}};
  }
  @Test(dataProvider = "mixedCaseHeaders")
  public void testCaseInsensitiveHeaders(String header) throws Exception
  {
    RestRequest request = new RestRequestBuilder(new URI("http://localhost:7279/foo?"))
        .setEntity("hello_world".getBytes())
        .setMethod("GET")
        .setHeader("x-http-method-override", "GET")
        .setHeader("Content-Length", "12")
        .setHeader(header, "application/x-www-form-urlencoded").build();

    // Should remove header even with mixed content
    RestRequest decoded = decode(request);
    Assert.assertNull(decoded.getHeader(header), "Mixed case 'content-type' header failed to be decoded");
  }

  @Test
  public void testTunneledLongQueryWithRestLiSpecialEncodedCharactersInPath() throws Exception
  {
    // Make sure the URI path includes Rest.li special encoded characters, and create a true long query
    StringBuilder query = new StringBuilder("q=queryString");
    for (int i = 0; i < 10000; i++) {
      query.append("&a=");
      query.append(i);
    }

    // Special characters with their encoding:
    // ',' - %2C
    // '(' - %28
    // ')' - %29
    String uriPathKeyString = "/foo/(bar:biz%3Aabc%28%29,baz:xyz%3A%2C)";

    RestRequest request = new RestRequestBuilder(new URI(("http://localhost:7279/" + uriPathKeyString + "?" + query.toString())))
                                                .setMethod("GET").build();
    RestRequest encoded = encode(request, 1);  // Set threshold to 1, so we force query tunneling

    Assert.assertEquals(encoded.getMethod(), "POST");
    Assert.assertEquals(request.getURI().getRawPath(), encoded.getURI().getRawPath());
    Assert.assertTrue(encoded.getEntity().length() == query.length());
    Assert.assertEquals(encoded.getHeader("Content-Type"), "application/x-www-form-urlencoded");
    Assert.assertEquals(encoded.getHeader("Content-Length"), Integer.toString(query.length()));

    RequestContext requestContext = new RequestContext();
    RestRequest decoded = decode(encoded, requestContext);
    Assert.assertEquals(decoded.getURI(), request.getURI());
    Assert.assertEquals(decoded.getMethod(), "GET");
    Assert.assertTrue((Boolean) requestContext.getLocalAttr(R2Constants.IS_QUERY_TUNNELED));
  }

  private RestRequest encode(RestRequest request, int threshold) throws Exception
  {
    return encode(request, new RequestContext(), threshold);
  }

  private RestRequest encode(RestRequest request, RequestContext requestContext, int threshold) throws Exception
  {
    if ("Stream".equals(_requestType))
    {
      TestCallback callback = new TestCallback();
      QueryTunnelUtil.encode(Messages.toStreamRequest(request), requestContext, threshold, callback);
      return callback.getRestRequest();
    }

    if ("Rest".equals(_requestType))
    {
      return QueryTunnelUtil.encode(request, requestContext, threshold);
    }

    throw new IllegalArgumentException("Unknown request type: " + _requestType);
  }

  private RestRequest decode(RestRequest request) throws Exception
  {
    return decode(request, new RequestContext());
  }

  private RestRequest decode(RestRequest request, final RequestContext requestContext) throws Exception
  {
    if ("Stream".equals(_requestType)) {
      TestCallback callback = new TestCallback();
      QueryTunnelUtil.decode(Messages.toStreamRequest(request), requestContext, callback);
      return callback.getRestRequest();
    }

    if ("Rest".equals(_requestType))
    {
      return QueryTunnelUtil.decode(request, requestContext);
    }

    throw new IllegalArgumentException("Unknown request type: " + _requestType);
  }

  private static class TestCallback implements Callback<StreamRequest>
  {
    private Throwable _error;
    private RestRequest _request;
    private final CountDownLatch _latch = new CountDownLatch(1);

    @Override
    public void onError(Throwable e)
    {
      _error = e;
      _latch.countDown();
    }

    @Override
    public void onSuccess(StreamRequest request)
    {
      Messages.toRestRequest(request, new Callback<RestRequest>()
      {
        @Override
        public void onError(Throwable e)
        {
          _error = e;
          _latch.countDown();
        }

        @Override
        public void onSuccess(RestRequest result)
        {
          _request = result;
          _latch.countDown();
        }
      });
    }

    public RestRequest getRestRequest() throws Exception
    {
      if(_latch.await(500, TimeUnit.MILLISECONDS))
      {
        if (_error != null)
        {
          throw new Exception(_error);
        }
        return _request;
      }
      else
      {
        throw new TimeoutException();
      }
    }
  }
}
