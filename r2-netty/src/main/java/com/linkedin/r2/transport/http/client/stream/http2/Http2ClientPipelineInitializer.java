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

package com.linkedin.r2.transport.http.client.stream.http2;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpClientUpgradeHandler;
import io.netty.handler.codec.http.HttpScheme;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.Http2ClientUpgradeCodec;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.util.AttributeKey;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
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
  private final int _maxHeaderSize;
  private final int _maxChunkSize;
  private final long _maxResponseSize;
  private final long _gracefulShutdownTimeout;

  private static final int MAX_CLIENT_UPGRADE_CONTENT_LENGTH = Integer.MAX_VALUE;
  private static final int MAX_INITIAL_LINE_LENGTH = 4096;
  private static final boolean IS_CLIENT = true;

  public static final AttributeKey<Http2Connection> HTTP2_CONNECTION_ATTR_KEY
      = AttributeKey.valueOf("Http2Connection");
  public static final AttributeKey<Http2Connection.PropertyKey> CALLBACK_ATTR_KEY
      = AttributeKey.valueOf("Callback");
  public static final AttributeKey<Http2Connection.PropertyKey> CHANNEL_POOL_HANDLE_ATTR_KEY
      = AttributeKey.valueOf("Handle");
  private final boolean _enableSSLSessionResumption;
  private final int _sslHandShakeTimeout;

  public Http2ClientPipelineInitializer(SSLContext sslContext, SSLParameters sslParameters,
                                        int maxHeaderSize, int maxChunkSize, long maxResponseSize,
                                        long gracefulShutdownTimeout, boolean enableSSLSessionResumption,
                                        int sslHandShakeTimeout)
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
    _maxHeaderSize = maxHeaderSize;
    _maxChunkSize = maxChunkSize;
    _maxResponseSize = maxResponseSize;
    _gracefulShutdownTimeout = gracefulShutdownTimeout;
    _enableSSLSessionResumption = enableSSLSessionResumption;
    _sslHandShakeTimeout = sslHandShakeTimeout;
  }

  @Override
  protected void initChannel(NioSocketChannel channel) throws Exception
  {
    Http2Connection connection = new DefaultHttp2Connection(false /* not server */);
    channel.attr(HTTP2_CONNECTION_ATTR_KEY).set(connection);
    channel.attr(CALLBACK_ATTR_KEY).set(connection.newKey());
    channel.attr(CHANNEL_POOL_HANDLE_ATTR_KEY).set(connection.newKey());

    if (_sslParameters == null)
    {
      // clear text
      configureHttpPipeline(channel, connection);
    }
    else
    {
      // TLS
      configureHttpsPipeline(channel, connection);
    }
  }


  /**
   * Sets up HTTP/2 over TCP through protocol upgrade (h2c) pipeline
   */
  private void configureHttpPipeline(Channel channel, Http2Connection connection) throws Exception
  {
    Http2StreamCodec http2Codec = new Http2StreamCodecBuilder()
      .connection(connection)
      .maxContentLength(_maxResponseSize)
      .gracefulShutdownTimeoutMillis(_gracefulShutdownTimeout)
      .build();
    HttpClientCodec sourceCodec = new HttpClientCodec(MAX_INITIAL_LINE_LENGTH, _maxHeaderSize, _maxChunkSize);
    Http2ClientUpgradeCodec upgradeCodec = new Http2ClientUpgradeCodec(http2Codec);
    HttpClientUpgradeHandler upgradeHandler = new HttpClientUpgradeHandler(
      sourceCodec, upgradeCodec, MAX_CLIENT_UPGRADE_CONTENT_LENGTH);
    Http2SchemeHandler schemeHandler = new Http2SchemeHandler(HttpScheme.HTTP.toString());

    Http2UpgradeHandler upgradeRequestHandler = new Http2UpgradeHandler();
    Http2StreamResponseHandler responseHandler = new Http2StreamResponseHandler();

    channel.pipeline().addLast("sourceCodec", sourceCodec);
    channel.pipeline().addLast("upgradeHandler", upgradeHandler);
    channel.pipeline().addLast("upgradeRequestHandler", upgradeRequestHandler);
    channel.pipeline().addLast("schemeHandler", schemeHandler);
    channel.pipeline().addLast("responseHandler", responseHandler);

  }

  /**
   * Sets up HTTP/2 over TLS through ALPN (h2) pipeline
   */
  @SuppressWarnings("deprecation")
  private void configureHttpsPipeline(NioSocketChannel ctx, Http2Connection connection) throws Exception
  {
    JdkSslContext context = new JdkSslContext(
      _sslContext,
      IS_CLIENT,
      Arrays.asList(_sslParameters.getCipherSuites()),
      IdentityCipherSuiteFilter.INSTANCE,
      // We should not use the non deprecated version to avoid breaking forward compatibility
      // until we dont have a shadowed version of Netty
      new ApplicationProtocolConfig(
        ApplicationProtocolConfig.Protocol.ALPN,
        ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
        ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
        ApplicationProtocolNames.HTTP_2,
        ApplicationProtocolNames.HTTP_1_1),
      _sslParameters.getNeedClientAuth() ? ClientAuth.REQUIRE : ClientAuth.OPTIONAL);

    Http2StreamCodec http2Codec = new Http2StreamCodecBuilder()
      .connection(connection)
      .maxContentLength(_maxResponseSize)
      .gracefulShutdownTimeoutMillis(_gracefulShutdownTimeout)
      .build();

    Http2AlpnHandler alpnHandler = new Http2AlpnHandler(context, http2Codec, _enableSSLSessionResumption, _sslHandShakeTimeout);
    Http2SchemeHandler schemeHandler = new Http2SchemeHandler(HttpScheme.HTTPS.toString());
    Http2StreamResponseHandler responseHandler = new Http2StreamResponseHandler();

    ctx.pipeline().addLast(Http2AlpnHandler.PIPELINE_ALPN_HANDLER, alpnHandler);
    ctx.pipeline().addLast("schemeHandler", schemeHandler);
    ctx.pipeline().addLast("responseHandler", responseHandler);

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
