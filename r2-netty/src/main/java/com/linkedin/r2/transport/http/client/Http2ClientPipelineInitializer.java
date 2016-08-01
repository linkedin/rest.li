/*
   Copyright (c) 2016 LinkedIn Corp.

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

/**
 * $Id: $
 */

package com.linkedin.r2.transport.http.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.util.AttributeKey;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Initializes Netty HTTP/2 streaming pipeline implementation of {@link io.netty.channel.ChannelInitializer}
 */
class Http2ClientPipelineInitializer extends ChannelInitializer<NioSocketChannel>
{
  private static final Logger LOG = LoggerFactory.getLogger(Http2ClientPipelineInitializer.class);

  private final SSLContext _sslContext;
  private final SSLParameters _sslParameters;
  private final ScheduledExecutorService _scheduler;
  private final int _maxHeaderSize;
  private final int _maxChunkSize;
  private final long _maxResponseSize;
  private final long _streamingTimeout;

  public static final AttributeKey<Http2Connection> HTTP2_CONNECTION_ATTR_KEY
      = AttributeKey.valueOf("Http2Connection");
  public static final AttributeKey<Http2Connection.PropertyKey> CALLBACK_ATTR_KEY
      = AttributeKey.valueOf("Callback");
  public static final AttributeKey<Http2Connection.PropertyKey> CHANNEL_POOL_HANDLE_ATTR_KEY
      = AttributeKey.valueOf("Handle");

  public Http2ClientPipelineInitializer(SSLContext sslContext, SSLParameters sslParameters,
      ScheduledExecutorService scheduler, int maxHeaderSize, int maxChunkSize, long maxResponseSize,
      long streamingTimeout)
  {
    // Check if requested parameters are present in the supported params of the context.
    // Log warning for those not present. Throw an exception if none present.
    if (sslParameters != null)
    {
      if (sslContext == null)
      {
        throw new IllegalArgumentException("SSLParameters passed with no SSLContext");
      }

      SSLParameters supportedSSLParameters = sslContext.getSupportedSSLParameters();

      if (sslParameters.getCipherSuites() != null)
      {
        checkContained(supportedSSLParameters.getCipherSuites(),
            sslParameters.getCipherSuites(),
            "cipher suite");
      }

      if (sslParameters.getProtocols() != null)
      {
        checkContained(supportedSSLParameters.getProtocols(),
            sslParameters.getProtocols(),
            "protocol");
      }
    }
    _sslContext = sslContext;
    _sslParameters = sslParameters;
    _scheduler = scheduler;
    _maxHeaderSize = maxHeaderSize;
    _maxChunkSize = maxChunkSize;
    _maxResponseSize = maxResponseSize;
    _streamingTimeout = streamingTimeout;
  }

  @Override
  protected void initChannel(NioSocketChannel channel) throws Exception
  {
    Http2Connection connection = new DefaultHttp2Connection(false /* not server */);
    channel.attr(HTTP2_CONNECTION_ATTR_KEY).set(connection);
    channel.attr(CALLBACK_ATTR_KEY).set(connection.newKey());
    channel.attr(CHANNEL_POOL_HANDLE_ATTR_KEY).set(connection.newKey());

    Http2InitializerHandler initializerHandler = new Http2InitializerHandler(_maxHeaderSize, _maxChunkSize,
        _maxResponseSize, _streamingTimeout, _scheduler, connection, _sslContext, _sslParameters);
    channel.pipeline().addLast("initializerHandler", initializerHandler);
  }

  /**
   * Checks if an array is completely or partially contained in another. Logs warnings
   * for one array values not contained in the other. Throws IllegalArgumentException if
   * none are.
   *
   * @param containingArray array to contain another.
   * @param containedArray array to be contained in another.
   * @param valueName - name of the value type to be included in log warning or
   *          exception.
   */
  private void checkContained(String[] containingArray,
      String[] containedArray,
      String valueName)
  {
    Set<String> containingSet = new HashSet<>(Arrays.asList(containingArray));
    Set<String> containedSet = new HashSet<>(Arrays.asList(containedArray));

    boolean changed = containedSet.removeAll(containingSet);
    if (!changed)
    {
      throw new IllegalArgumentException("None of the requested " + valueName
          + "s: " + containedSet + " are found in SSLContext");
    }

    if (!containedSet.isEmpty())
    {
      for (String paramValue : containedSet)
      {
        LOG.warn("{} {} requested but not found in SSLContext", valueName, paramValue);
      }
    }
  }
}
