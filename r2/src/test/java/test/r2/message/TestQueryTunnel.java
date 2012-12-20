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

import com.linkedin.data.ByteString;
import com.linkedin.r2.message.rest.QueryTunnelUtil;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Doug Young
 * @version $Revision$
 */
public class TestQueryTunnel
{
  @Test
  public void testSimpleGetNoArgs() throws Exception
  {
    RestRequest request = new RestRequestBuilder(new URI("http://localhost:7279"))
                                       .setMethod("GET").build();

    // Pass to encode, but there are no query args, so nothing should change
    RestRequest encoded = QueryTunnelUtil.encode(request, 0);
    Assert.assertEquals(request.getURI(), encoded.getURI());
    Assert.assertEquals(request.getMethod(), encoded.getMethod());

    // Pass the request - either one will do - to the decode side. Again, nothing should change
    RestRequest decoded = QueryTunnelUtil.decode(encoded);
    Assert.assertEquals(request.getURI(), decoded.getURI());
    Assert.assertEquals(request.getMethod(), decoded.getMethod());
    Assert.assertEquals(request.getHeader("Content-Type"), decoded.getHeader("Content-Type"));
  }

  @Test
  public void testSimpleGetWithArgs() throws Exception
  {
    RestRequest request = new RestRequestBuilder(new URI("http://localhost:7279?q=one&x=10&y=15"))
                                       .setMethod("GET").build();

    // Process GET request with params. Set threshold to 0, which should convert the result to a POST
    // with no query, and a body
    RestRequest encoded = QueryTunnelUtil.encode(request, 0);
    Assert.assertEquals(encoded.getMethod(), "POST");
    Assert.assertEquals(encoded.getURI().toString(), "http://localhost:7279");
    Assert.assertTrue(encoded.getEntity().length() > 0);

    // Decode, and we should get the original request back
    RestRequest decoded = QueryTunnelUtil.decode(encoded);
    Assert.assertEquals(request.getURI(), decoded.getURI());
    Assert.assertEquals(request.getMethod(), decoded.getMethod());
    Assert.assertEquals(request.getHeader("Content-Type"), decoded.getHeader("Content-Type"));
  }

  @Test
  public void testNonAsciiParams() throws Exception
  {
    StringBuilder sb = new StringBuilder("http://localhost:7279?");
    sb.append("eWithAcuteAccent=\u00e9");
    sb.append("&");
    sb.append("chineseCharacter=\u4e80");

    RestRequest req = new RestRequestBuilder(new URI(sb.toString())).setMethod("GET").build();

    RestRequest roundTrip = QueryTunnelUtil.decode(QueryTunnelUtil.encode(req, 0));

    Assert.assertEquals(req, roundTrip);
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
    RestRequest encoded = QueryTunnelUtil.encode(request, 0);
    Assert.assertEquals(encoded.getMethod(), "POST");
    Assert.assertEquals(encoded.getURI().toString(), "http://localhost:7279");
    Assert.assertTrue(encoded.getEntity().length() > 0);
    Assert.assertTrue(encoded.getHeader("Content-Type").startsWith("multipart/mixed"));

    // Decode, and we should get the original request back
    RestRequest decoded = QueryTunnelUtil.decode(encoded);
    Assert.assertEquals(request.getURI(), decoded.getURI());
    Assert.assertEquals(request.getMethod(), decoded.getMethod());
    Assert.assertEquals(request.getEntity(), decoded.getEntity());
    Assert.assertEquals(request.getHeader("Content-Type"), decoded.getHeader("Content-Type"));
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
    RestRequest encoded = QueryTunnelUtil.encode(request, Integer.MAX_VALUE);
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

    RestRequest tunneled = QueryTunnelUtil.decode(QueryTunnelUtil.encode(request, 0));
    Assert.assertEquals(request.getURI(), tunneled.getURI());
    Assert.assertEquals(request.getMethod(), tunneled.getMethod());
    Assert.assertEquals(request.getEntity(), tunneled.getEntity());
    Assert.assertEquals(request.getHeader("Content-Type"), tunneled.getHeader("Content-Type"));
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
      QueryTunnelUtil.encode(request, 0);
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

    RestRequest decoded = QueryTunnelUtil.decode(request);
    Assert.assertEquals(decoded.getURI().toString(), "http://localhost:7279?q=123");
    Assert.assertEquals(decoded.getMethod(), "GET");
  }

  @Test
  public void testNestedMultiPartBody() throws Exception
  {
    // Construct a request with multi-part entity as a body and make sure it can be tunneled
    // The entity will be nested - the original multi-part body will become the 2nd part of the tunneled
    // body with the query passed as the form-encoded url

    MimeMultipart multi = new MimeMultipart("mixed");

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
    RestRequest request = new RestRequestBuilder(new URI("http://localhost:7279?args=xyz"))
        .setMethod("PUT")
        .setHeader("Content-Type", multi.getContentType())
        .setEntity(ByteString.copy(os.toByteArray())).build();

    // Encode and verify
    RestRequest encoded = QueryTunnelUtil.encode(request, 0);
    Assert.assertEquals(encoded.getMethod(), "POST");
    Assert.assertEquals(encoded.getURI().toString(), "http://localhost:7279");
    Assert.assertTrue(encoded.getEntity().length() > 0);
    Assert.assertTrue(encoded.getHeader("Content-Type").startsWith("multipart/mixed"));

    // Decode and make sure we have the original request back
    RestRequest decoded = QueryTunnelUtil.decode(request);
    Assert.assertEquals(decoded.getURI().toString(), "http://localhost:7279?args=xyz");
    Assert.assertEquals(decoded.getMethod(), "PUT");
    Assert.assertEquals(decoded.getEntity(), request.getEntity());
    Assert.assertTrue(encoded.getHeader("Content-Type").startsWith("multipart/mixed"));
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

    RestRequest encoded = QueryTunnelUtil.encode(request, query.length()-1);  // Set threshold < query length
    Assert.assertEquals(encoded.getMethod(), "POST");
    Assert.assertEquals(encoded.getURI().toString(), "http://localhost:7279");
    Assert.assertTrue(encoded.getEntity().length() == query.length());
    Assert.assertEquals(encoded.getHeader("Content-Type"), "application/x-www-form-urlencoded");

    RestRequest decoded = QueryTunnelUtil.decode(encoded);
    Assert.assertEquals(decoded.getURI(), request.getURI());
    Assert.assertEquals(decoded.getMethod(), "GET");
  }

}





