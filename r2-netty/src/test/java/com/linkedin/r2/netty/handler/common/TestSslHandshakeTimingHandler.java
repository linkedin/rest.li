/*
   Copyright (c) 2026 LinkedIn Corp.

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

package com.linkedin.r2.netty.handler.common;

import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.netty.common.NettyChannelAttributes;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.Attribute;
import java.security.cert.Certificate;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * Tests for {@link SslHandshakeTimingHandler#getSslTimingCallback}, verifying server certificate
 * resolution for both HTTP/1.1 (cert on channel) and HTTP/2 (cert on parent channel).
 */
public class TestSslHandshakeTimingHandler
{
  /**
   * HTTP/1.1: cert is set directly on the channel by CertificateHandler.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testHttp1ReadsCertFromChannel()
  {
    EmbeddedChannel channel = new EmbeddedChannel();
    Certificate[] mockCerts = new Certificate[] { Mockito.mock(Certificate.class) };
    channel.attr(NettyChannelAttributes.SERVER_CERTIFICATES).set(mockCerts);

    RequestContext ctx = new RequestContext();
    TransportCallback<Object> wrapped = SslHandshakeTimingHandler.getSslTimingCallback(channel, ctx, response -> {});

    wrapped.onResponse(Mockito.mock(TransportResponse.class));

    Assert.assertSame(ctx.getLocalAttr(R2Constants.SERVER_CERT), mockCerts);
    channel.finish();
  }

  /**
   * HTTP/2: cert is on the parent TCP channel, not the child stream channel.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testHttp2ReadsCertFromParentChannel()
  {
    EmbeddedChannel parentChannel = new EmbeddedChannel();
    Certificate[] mockCerts = new Certificate[] { Mockito.mock(Certificate.class) };
    parentChannel.attr(NettyChannelAttributes.SERVER_CERTIFICATES).set(mockCerts);

    Channel childChannel = Mockito.mock(Channel.class);
    Mockito.when(childChannel.parent()).thenReturn(parentChannel);

    // Child channel has no cert and no handshake timing
    Attribute mockCertAttr = Mockito.mock(Attribute.class);
    Mockito.when(mockCertAttr.get()).thenReturn(null);
    Mockito.when(childChannel.attr(NettyChannelAttributes.SERVER_CERTIFICATES)).thenReturn(mockCertAttr);

    Attribute mockTimingAttr = Mockito.mock(Attribute.class);
    Mockito.when(mockTimingAttr.getAndSet(null)).thenReturn(null);
    Mockito.when(childChannel.attr(SslHandshakeTimingHandler.SSL_HANDSHAKE_START_TIME)).thenReturn(mockTimingAttr);

    RequestContext ctx = new RequestContext();
    TransportCallback<Object> wrapped = SslHandshakeTimingHandler.getSslTimingCallback(childChannel, ctx, response -> {});

    wrapped.onResponse(Mockito.mock(TransportResponse.class));

    Assert.assertSame(ctx.getLocalAttr(R2Constants.SERVER_CERT), mockCerts);
    parentChannel.finish();
  }
}
