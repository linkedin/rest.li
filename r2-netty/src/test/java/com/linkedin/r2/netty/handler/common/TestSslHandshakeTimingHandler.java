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
import io.netty.util.AttributeKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * Tests for {@link SslHandshakeTimingHandler}, specifically verifying that
 * server certificates are accessible to all concurrent callbacks on HTTP/2
 * multiplexed connections.
 */
public class TestSslHandshakeTimingHandler
{
  @Test(timeOut = 10000)
  @SuppressWarnings("unchecked")
  public void testConcurrentCallbacksAllReceiveCerts() throws Exception
  {
    int numCallbacks = 10;
    EmbeddedChannel channel = new EmbeddedChannel();

    Certificate[] mockCerts = new Certificate[] { Mockito.mock(Certificate.class) };
    channel.attr(NettyChannelAttributes.SERVER_CERTIFICATES).set(mockCerts);

    // Create N wrapped callbacks via getSslTimingCallback
    List<RequestContext> contexts = new ArrayList<>();
    List<TransportCallback<Object>> wrappedCallbacks = new ArrayList<>();
    CountDownLatch ready = new CountDownLatch(numCallbacks);
    CountDownLatch go = new CountDownLatch(1);
    AtomicBoolean anyFailed = new AtomicBoolean(false);

    for (int i = 0; i < numCallbacks; i++)
    {
      RequestContext ctx = new RequestContext();
      contexts.add(ctx);

      TransportCallback<Object> inner = response -> {};
      wrappedCallbacks.add(SslHandshakeTimingHandler.getSslTimingCallback(channel, ctx, inner));
    }

    // Fire all callbacks concurrently using a latch barrier
    TransportResponse<Object> mockResponse = Mockito.mock(TransportResponse.class);
    List<Thread> threads = new ArrayList<>();
    for (int i = 0; i < numCallbacks; i++)
    {
      final int idx = i;
      Thread t = new Thread(() -> {
        ready.countDown();
        try
        {
          go.await();
        }
        catch (InterruptedException e)
        {
          anyFailed.set(true);
          return;
        }
        wrappedCallbacks.get(idx).onResponse(mockResponse);
      });
      threads.add(t);
      t.start();
    }

    // Wait for all threads to be ready, then release them simultaneously
    ready.await();
    go.countDown();

    for (Thread t : threads)
    {
      t.join();
    }

    Assert.assertFalse(anyFailed.get(), "A thread was interrupted unexpectedly");

    // All N RequestContexts should have SERVER_CERT populated
    for (int i = 0; i < numCallbacks; i++)
    {
      Certificate[] certs = (Certificate[]) contexts.get(i).getLocalAttr(R2Constants.SERVER_CERT);
      Assert.assertNotNull(certs, "Callback " + i + " should have received certs");
      Assert.assertSame(certs, mockCerts, "Callback " + i + " should have the same cert array");
    }

    channel.finish();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testHandshakeTimingOnlyRecordedOnce()
  {
    EmbeddedChannel channel = new EmbeddedChannel();

    long handshakeDuration = 12345L;
    channel.attr(SslHandshakeTimingHandler.SSL_HANDSHAKE_START_TIME).set(handshakeDuration);

    TransportResponse<Object> mockResponse = Mockito.mock(TransportResponse.class);

    // First callback should capture the handshake timing
    RequestContext ctx1 = new RequestContext();
    TransportCallback<Object> inner1 = response -> {};
    TransportCallback<Object> wrapped1 = SslHandshakeTimingHandler.getSslTimingCallback(channel, ctx1, inner1);
    wrapped1.onResponse(mockResponse);

    // Second callback should NOT have handshake timing (already consumed)
    RequestContext ctx2 = new RequestContext();
    TransportCallback<Object> inner2 = response -> {};
    TransportCallback<Object> wrapped2 = SslHandshakeTimingHandler.getSslTimingCallback(channel, ctx2, inner2);
    wrapped2.onResponse(mockResponse);

    // Verify SSL_HANDSHAKE_START_TIME was cleared after first callback
    Assert.assertNull(channel.attr(SslHandshakeTimingHandler.SSL_HANDSHAKE_START_TIME).get(),
        "Handshake start time should be cleared after first read");

    channel.finish();
  }

  /**
   * Simulates the HTTP/2 case: the child stream channel has no cert attribute,
   * but its parent channel does. The fix should read from the parent.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testChildStreamChannelReadsCertFromParent()
  {
    // Parent channel holds the cert (set by CertificateHandler on the parent pipeline)
    EmbeddedChannel parentChannel = new EmbeddedChannel();
    Certificate[] mockCerts = new Certificate[] { Mockito.mock(Certificate.class) };
    parentChannel.attr(NettyChannelAttributes.SERVER_CERTIFICATES).set(mockCerts);

    // Child stream channel has no cert attribute set -- mock it to return the parent
    Channel childChannel = Mockito.mock(Channel.class);
    Mockito.when(childChannel.parent()).thenReturn(parentChannel);

    // The child channel needs to return a real attribute for SSL_HANDSHAKE_START_TIME
    // (read from the child, not the parent -- this is intentional)
    @SuppressWarnings("rawtypes")
    Attribute mockTimingAttr = Mockito.mock(Attribute.class);
    Mockito.when(mockTimingAttr.getAndSet(null)).thenReturn(null);
    Mockito.when(childChannel.attr(SslHandshakeTimingHandler.SSL_HANDSHAKE_START_TIME))
        .thenReturn(mockTimingAttr);

    RequestContext ctx = new RequestContext();
    TransportCallback<Object> inner = response -> {};
    TransportCallback<Object> wrapped = SslHandshakeTimingHandler.getSslTimingCallback(childChannel, ctx, inner);

    TransportResponse<Object> mockResponse = Mockito.mock(TransportResponse.class);
    wrapped.onResponse(mockResponse);

    Certificate[] certs = (Certificate[]) ctx.getLocalAttr(R2Constants.SERVER_CERT);
    Assert.assertNotNull(certs, "Child channel callback should have received certs from parent");
    Assert.assertSame(certs, mockCerts, "Certs should be the same array from the parent channel");

    parentChannel.finish();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testNoCertSetDoesNotCrash()
  {
    EmbeddedChannel channel = new EmbeddedChannel();
    // Intentionally do NOT set SERVER_CERTIFICATES on the channel

    RequestContext ctx = new RequestContext();
    AtomicBoolean innerCalled = new AtomicBoolean(false);
    TransportCallback<Object> inner = response -> innerCalled.set(true);

    TransportCallback<Object> wrapped = SslHandshakeTimingHandler.getSslTimingCallback(channel, ctx, inner);
    TransportResponse<Object> mockResponse = Mockito.mock(TransportResponse.class);
    wrapped.onResponse(mockResponse);

    // Inner callback should still be invoked
    Assert.assertTrue(innerCalled.get(), "Inner callback should have been invoked");

    // SERVER_CERT should be absent from RequestContext
    Assert.assertNull(ctx.getLocalAttr(R2Constants.SERVER_CERT),
        "SERVER_CERT should be null when no cert is set on channel");

    channel.finish();
  }
}
