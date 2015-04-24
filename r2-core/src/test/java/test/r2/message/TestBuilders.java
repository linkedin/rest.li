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


import com.linkedin.r2.message.Message;
import com.linkedin.r2.message.MessageBuilder;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.rest.RestMessage;
import com.linkedin.r2.message.rest.RestMethod;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;

import java.net.URI;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class TestBuilders
{
  @Test
  public void testChainBuildRestRequestFromRestRequestBuilder()
  {
    final RestRequest req = new RestRequestBuilder(URI.create("test"))
            .setEntity(new byte[] {1,2,3,4})
            .setHeader("k1", "v1")
            .setMethod(RestMethod.PUT)
            .build()
            .builder()
              .setEntity(new byte[] {5,6,7,8})
              .setHeader("k2", "v2")
              .setMethod(RestMethod.POST)
              .setURI(URI.create("anotherURI"))
              .build();

    Assert.assertEquals(new byte[] {5,6,7,8}, req.getEntity().copyBytes());
    Assert.assertEquals("v1", req.getHeader("k1"));
    Assert.assertEquals("v2", req.getHeader("k2"));
    Assert.assertEquals(RestMethod.POST, req.getMethod());
    Assert.assertEquals(URI.create("anotherURI"), req.getURI());
  }

  @Test
  public void testChainBuildRestRequestFromRequestBuilder()
  {
    final Request req = new RestRequestBuilder(URI.create("test"))
            .setEntity(new byte[] {1,2,3,4})
            .setHeader("k1", "v1")
            .setMethod(RestMethod.PUT)
            .build()
            .requestBuilder()
              .setEntity(new byte[] {5,6,7,8})
              .setURI(URI.create("anotherURI"))
              .build();

    Assert.assertEquals(new byte[] {5,6,7,8}, req.getEntity().copyBytes());
    Assert.assertEquals(URI.create("anotherURI"), req.getURI());

    Assert.assertTrue(req instanceof RestRequest);
    final RestRequest restReq = (RestRequest)req;
    Assert.assertEquals("v1", restReq.getHeader("k1"));
    Assert.assertEquals(RestMethod.PUT, restReq.getMethod());
  }

  @Test
  public void testChainBuildRestRequestFromRestBuilder()
  {
    final RestMessage req = new RestRequestBuilder(URI.create("test"))
            .setEntity(new byte[] {1,2,3,4})
            .setHeader("k1", "v1")
            .setMethod(RestMethod.PUT)
            .build()
            .restBuilder()
              .setEntity(new byte[] {5,6,7,8})
              .setHeader("k2", "v2")
              .build();

    Assert.assertEquals(new byte[] {5,6,7,8}, req.getEntity().copyBytes());
    Assert.assertEquals("v1", req.getHeader("k1"));
    Assert.assertEquals("v2", req.getHeader("k2"));

    Assert.assertTrue(req instanceof RestRequest);
    final RestRequest restReq = (RestRequest)req;
    Assert.assertEquals(RestMethod.PUT, restReq.getMethod());
    Assert.assertEquals(URI.create("test"), restReq.getURI());
  }

  @Test
  public void testChainBuildRestRequestFromMessageBuilder()
  {
    final MessageBuilder<?> builder = new RestRequestBuilder(URI.create("test"))
            .setEntity(new byte[] {1,2,3,4})
            .setHeader("k1", "v1")
            .setMethod(RestMethod.PUT)
            .build()
            .builder();

    final Message req = builder
              .setEntity(new byte[] {5,6,7,8})
              .build();

    Assert.assertEquals(new byte[] {5,6,7,8}, req.getEntity().copyBytes());

    Assert.assertTrue(req instanceof RestRequest);
    final RestRequest restReq = (RestRequest)req;
    Assert.assertEquals(RestMethod.PUT, restReq.getMethod());
    Assert.assertEquals(URI.create("test"), restReq.getURI());
    Assert.assertEquals("v1", restReq.getHeader("k1"));
  }

  @Test
  public void testChainBuildRestResponseFromRestResponseBuilder()
  {
    final RestResponse res = new RestResponseBuilder()
            .setEntity(new byte[] {1,2,3,4})
            .setHeader("k1", "v1")
            .setStatus(300)
            .build()
            .builder()
              .setEntity(new byte[] {5,6,7,8})
              .setHeader("k2", "v2")
              .setStatus(400)
              .build();

    Assert.assertEquals(new byte[] {5,6,7,8}, res.getEntity().copyBytes());
    Assert.assertEquals("v1", res.getHeader("k1"));
    Assert.assertEquals("v2", res.getHeader("k2"));
    Assert.assertEquals(400, res.getStatus());
  }

  @Test
  public void testChainBuildRestResponseFromResponseBuilder()
  {
    final Response res = new RestResponseBuilder()
            .setEntity(new byte[] {1,2,3,4})
            .setHeader("k1", "v1")
            .setStatus(300)
            .build()
            .responseBuilder()
              .setEntity(new byte[] {5,6,7,8})
              .build();

    Assert.assertEquals(new byte[] {5,6,7,8}, res.getEntity().copyBytes());

    Assert.assertTrue(res instanceof RestResponse);
    final RestResponse restRes = (RestResponse)res;
    Assert.assertEquals("v1", restRes.getHeader("k1"));
    Assert.assertEquals(300, restRes.getStatus());
  }

  @Test
  public void testChainBuildRestResponseFromRestBuilder()
  {
    final RestMessage res = new RestResponseBuilder()
            .setEntity(new byte[] {1,2,3,4})
            .setHeader("k1", "v1")
            .setStatus(300)
            .build()
            .restBuilder()
              .setEntity(new byte[] {5,6,7,8})
              .setHeader("k2", "v2")
              .build();

    Assert.assertEquals(new byte[] {5,6,7,8}, res.getEntity().copyBytes());
    Assert.assertEquals("v1", res.getHeader("k1"));
    Assert.assertEquals("v2", res.getHeader("k2"));

    Assert.assertTrue(res instanceof RestResponse);
    final RestResponse restRes = (RestResponse)res;
    Assert.assertEquals(300, restRes.getStatus());
  }

  @Test
  public void testChainBuildRestResponseFromMessageBuilder()
  {
    final MessageBuilder<?> builder = new RestResponseBuilder()
            .setEntity(new byte[] {1,2,3,4})
            .setHeader("k1", "v1")
            .setStatus(300)
            .build()
            .builder();

    final Message res = builder
              .setEntity(new byte[] {5,6,7,8})
              .build();

    Assert.assertEquals(new byte[] {5,6,7,8}, res.getEntity().copyBytes());

    Assert.assertTrue(res instanceof RestResponse);
    final RestResponse restRes = (RestResponse)res;
    Assert.assertEquals("v1", restRes.getHeader("k1"));
    Assert.assertEquals(300, restRes.getStatus());
  }
}
