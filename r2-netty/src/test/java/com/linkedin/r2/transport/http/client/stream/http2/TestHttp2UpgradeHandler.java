/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.client.stream.http2;

import com.linkedin.r2.transport.common.bridge.common.RequestWithCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import com.linkedin.r2.transport.http.client.TimeoutAsyncPoolHandle;
import com.linkedin.r2.transport.http.client.TimeoutTransportCallback;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Sean Sheng
 */
@SuppressWarnings("rawtypes")
public class TestHttp2UpgradeHandler
{
  private static final String HOST = "localhost";
  private static final String PATH = "*";
  private static final int PORT = 8080;

  @Test
  public void testInitialization() throws Exception
  {
    Http2UpgradeHandler handler = new Http2UpgradeHandler(HOST, PORT);
    EmbeddedChannel channel = new EmbeddedChannel(handler);

    Assert.assertTrue(channel.finish());

    Assert.assertEquals(channel.outboundMessages().size(), 1);

    DefaultFullHttpRequest message = channel.readOutbound();
    Assert.assertNotNull(message);
    Assert.assertEquals(message.method(), HttpMethod.OPTIONS);
    Assert.assertEquals(message.uri(), PATH);
    Assert.assertEquals(message.headers().get(HttpHeaderNames.HOST), HOST + ":" + PORT);
  }

  @Test
  public void testWriteBeforeUpgrade() throws Exception {
    Http2UpgradeHandler handler = new Http2UpgradeHandler(HOST, PORT);
    EmbeddedChannel channel = new EmbeddedChannel(handler);

    // Reads the upgrade request from the outbound buffer to ensure nothing in the buffer
    Assert.assertEquals(channel.outboundMessages().size(), 1);
    Assert.assertNotNull(channel.readOutbound());
    Assert.assertTrue(channel.outboundMessages().isEmpty());

    // Write should not succeed before upgrade completes
    RequestWithCallback request = Mockito.mock(RequestWithCallback.class);
    Assert.assertFalse(channel.writeOutbound(request));
    Assert.assertFalse(channel.finish());
  }

  @Test(timeOut = 10000)
  @SuppressWarnings("unchecked")
  public void testChannelCloseBeforeUpgrade() throws Exception {
    Http2UpgradeHandler handler = new Http2UpgradeHandler(HOST, PORT);
    EmbeddedChannel channel = new EmbeddedChannel(handler);

    // Reads the upgrade request from the outbound buffer to ensure nothing in the buffer
    Assert.assertEquals(channel.outboundMessages().size(), 1);
    Assert.assertNotNull(channel.readOutbound());
    Assert.assertTrue(channel.outboundMessages().isEmpty());

    RequestWithCallback request = Mockito.mock(RequestWithCallback.class);
    TimeoutAsyncPoolHandle handle = Mockito.mock(TimeoutAsyncPoolHandle.class);
    TimeoutTransportCallback callback = Mockito.mock(TimeoutTransportCallback.class);

    Mockito.when(request.handle()).thenReturn(handle);
    Mockito.when(request.callback()).thenReturn(callback);

    // Write should not succeed before upgrade completes
    Assert.assertFalse(channel.writeOutbound(request));
    Assert.assertFalse(channel.finish());

    // Synchronously waiting for channel to close
    channel.close().sync();

    Mockito.verify(request).handle();
    Mockito.verify(request).callback();
    Mockito.verify(handle).dispose();
    Mockito.verify(callback).onResponse(Mockito.any(TransportResponse.class));
  }
}
