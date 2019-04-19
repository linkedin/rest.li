/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.r2.netty.handler.http2;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpClientUpgradeHandler;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A handler that triggers the clear text upgrade to HTTP/2 upon adding to pipeline by sending
 * an initial HTTP OPTIONS request with connection upgrade headers. Calls to #write and #flush
 * are suspended util the upgrade is complete. Handler removes itself upon upgrade success.
 *
 * Handler listens to upstream {@link HttpClientUpgradeHandler.UpgradeEvent} event for h2c
 * upgrade signals and sets the upgrade promise accordingly.
 *
 * @author Sean Sheng
 * @author Nizar Mankulangara
 */
public class Http2ProtocolUpgradeHandler extends ChannelDuplexHandler
{
  private static final Logger LOG = LoggerFactory.getLogger(Http2ProtocolUpgradeHandler.class);

  private ChannelPromise _upgradePromise;

  public Http2ProtocolUpgradeHandler(ChannelPromise upgradePromise)
  {
    _upgradePromise = upgradePromise;
  }

  /**
   * Configures the pipeline based on the result of the {@link HttpClientUpgradeHandler.UpgradeEvent}.
   * @param ctx Channel handle context.
   * @param event Upgrade event.
   */
  private void configurePipeline(ChannelHandlerContext ctx, HttpClientUpgradeHandler.UpgradeEvent event)
  {
    if (event == HttpClientUpgradeHandler.UpgradeEvent.UPGRADE_SUCCESSFUL)
    {
      ctx.pipeline().remove(this);
      _upgradePromise.setSuccess();
    }
    else if (event == HttpClientUpgradeHandler.UpgradeEvent.UPGRADE_REJECTED)
    {
      _upgradePromise.setFailure(new IllegalStateException("HTTP/2 clear text upgrade failed"));
    }
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx)
  {
    processChannelActive(ctx, LOG, _upgradePromise);
  }

  public static void processChannelActive(ChannelHandlerContext ctx, Logger log, ChannelPromise upgradePromise)
  {
    // For an upgrade request, clients should use an OPTIONS request for path “*” or a HEAD request for “/”.
    // RFC: https://tools.ietf.org/html/rfc7540#section-3.2
    // Implementation detail: https://http2.github.io/faq/#can-i-implement-http2-without-implementing-http11
    final DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.OPTIONS, "*");

    final String hostname;
    if (ctx.channel().remoteAddress() instanceof InetSocketAddress)
    {
      // 1) The documentation of remoteAddress says that it should be down-casted to InetSocketAddress.
      // 2) The getHostString doesn't attempt a reverse lookup
      InetSocketAddress inetAddress = ((InetSocketAddress) ctx.channel().remoteAddress());
      hostname = inetAddress.getHostString() + ":" + inetAddress.getPort();
    }
    else
    {
      // if it is not a InetSocketAddress, it is a DomainSocketAddress, a LocalAddress or a EmbeddedSocketAddress.
      // In the R2 stack it should never happen
      hostname = "localhost";
      log.warn("The remoteAddress is not an InetSocketAddress, therefore it has been used '" + hostname + "'" +
          " for the HOST of the upgrade request", ctx.channel().remoteAddress());
    }

    // The host is required given rfc2616 14.23 also for the upgrade request.
    // Without it, the host the upgrade request fails
    // https://tools.ietf.org/html/rfc2616#section-14.23
    request.headers().add(HttpHeaderNames.HOST, hostname);

    ctx.writeAndFlush(request);

    // Fail the upgrade promise when channel is closed
    ctx.channel().closeFuture().addListener(future -> {
      if (!upgradePromise.isDone())
      {
        upgradePromise.setFailure(new ChannelException("HTTP/2 upgrade did not complete before channel closed"));
      }
    });

    ctx.fireChannelActive();
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
  {
    if (evt instanceof HttpClientUpgradeHandler.UpgradeEvent)
    {
      configurePipeline(ctx, (HttpClientUpgradeHandler.UpgradeEvent) evt);
    }

    ctx.fireUserEventTriggered(evt);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx)
  {
    trySetUpgradeFailure(new ClosedChannelException());
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
  {
    trySetUpgradeFailure(cause);
  }

  private void trySetUpgradeFailure(Throwable cause)
  {
    if (!_upgradePromise.isDone())
    {
      _upgradePromise.setFailure(new IllegalStateException("HTTP/2 clear text upgrade failed", cause));
    }
  }
}
