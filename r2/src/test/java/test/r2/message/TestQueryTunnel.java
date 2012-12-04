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

import com.linkedin.r2.message.rest.QueryTunnelUtil;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import java.net.URI;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Doug Young
 * @version $Revision$
 */
public class TestQueryTunnel
{
  @Test
  public void testSimpleGet() throws Exception
  {
    RestRequest request = new RestRequestBuilder(new URI("http://localhost:7279"))
                                       .setMethod("GET").build();

    // Set to encode, but there are no query args, so nothing should change
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
    // GET request with params. Set threshold to 0, which should convert the result to a POST
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
  public void testSimpleGetWithEntity() throws Exception
  {
    RestRequest request = new RestRequestBuilder(new URI("http://localhost:7279?q=one&x=10&y=15"))
                                       .setMethod("GET")
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
}





