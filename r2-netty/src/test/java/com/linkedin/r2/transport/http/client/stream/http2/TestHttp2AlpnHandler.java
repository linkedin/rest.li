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
import io.netty.handler.ssl.SslContext;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Sean Sheng
 */
@SuppressWarnings("rawtypes")
public class TestHttp2AlpnHandler
{
  @Test
  public void testWriteBeforeNegotiation() throws Exception
  {
    SslContext sslContext = Mockito.mock(SslContext.class);
    Http2StreamCodec http2StreamCodec = Mockito.mock(Http2StreamCodec.class);

    Http2AlpnHandler handler = new Http2AlpnHandler(sslContext, http2StreamCodec, true, Integer.MAX_VALUE);
    EmbeddedChannel channel = new EmbeddedChannel(handler);

    // Write should not succeed before negotiation completes
    RequestWithCallback request = Mockito.mock(RequestWithCallback.class);
    Assert.assertFalse(channel.writeOutbound(request));
    Assert.assertFalse(channel.finish());
  }

  @Test(timeOut = 10000)
  @SuppressWarnings("unchecked")
  public void testChannelCloseBeforeNegotiation() throws Exception {
    SslContext sslContext = Mockito.mock(SslContext.class);
    Http2StreamCodec http2StreamCodec = Mockito.mock(Http2StreamCodec.class);

    Http2AlpnHandler handler = new Http2AlpnHandler(sslContext, http2StreamCodec, true, Integer.MAX_VALUE);
    EmbeddedChannel channel = new EmbeddedChannel(handler);

    RequestWithCallback request = Mockito.mock(RequestWithCallback.class);
    TimeoutAsyncPoolHandle handle = Mockito.mock(TimeoutAsyncPoolHandle.class);
    TimeoutTransportCallback callback = Mockito.mock(TimeoutTransportCallback.class);

    Mockito.when(request.handle()).thenReturn(handle);
    Mockito.when(request.callback()).thenReturn(callback);

    // Write should not succeed before negotiation completes
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
