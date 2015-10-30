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
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.rest.RestMethod;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URI;

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
  public void testChainBuildStreamRequestFromStreamRequestBuilder()
  {
    final StreamRequest req = new StreamRequestBuilder(URI.create("test"))
        .setHeader("k1", "v1")
        .setMethod(RestMethod.PUT)
        .build(EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copy(new byte[] {1,2,3,4}))))
        .builder()
        .setHeader("k2", "v2")
        .setMethod(RestMethod.POST)
        .setURI(URI.create("anotherURI"))
        .build(EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copy(new byte[] {5,6,7,8}))));

    Messages.toRestRequest(req, new Callback<RestRequest>()
    {
      @Override
      public void onError(Throwable e)
      {
        Assert.fail();
      }

      @Override
      public void onSuccess(RestRequest result)
      {
        Assert.assertEquals(new byte[] {5,6,7,8}, result.getEntity().copyBytes());
        Assert.assertEquals("v1", req.getHeader("k1"));
        Assert.assertEquals("v2", req.getHeader("k2"));
        Assert.assertEquals(RestMethod.POST, req.getMethod());
        Assert.assertEquals(URI.create("anotherURI"), req.getURI());
      }
    });

  }

  @Test
  public void testChainBuildStreamResponseFromStreamResponseBuilder()
  {
    final StreamResponse res = new StreamResponseBuilder()
        .setHeader("k1", "v1")
        .setStatus(300)
        .build(EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copy(new byte[] {1,2,3,4}))))
        .builder()
        .setHeader("k2", "v2")
        .setStatus(400)
        .build(EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copy(new byte[] {5,6,7,8}))));

    Messages.toRestResponse(res, new Callback<RestResponse>()
    {
      @Override
      public void onError(Throwable e)
      {
        Assert.fail();
      }

      @Override
      public void onSuccess(RestResponse result)
      {
        Assert.assertEquals(new byte[] {5,6,7,8}, result.getEntity().copyBytes());
        Assert.assertEquals("v1", res.getHeader("k1"));
        Assert.assertEquals("v2", res.getHeader("k2"));
        Assert.assertEquals(400, res.getStatus());
      }
    });
  }
}
