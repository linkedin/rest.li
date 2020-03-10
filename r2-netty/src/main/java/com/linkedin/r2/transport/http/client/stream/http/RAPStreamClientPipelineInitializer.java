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

package com.linkedin.r2.transport.http.client.stream.http;

import com.linkedin.r2.netty.handler.common.SessionResumptionSslHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Netty HTTP/1.1 streaming implementation of {@link ChannelInitializer}
 */
public class RAPStreamClientPipelineInitializer extends ChannelInitializer<NioSocketChannel>
{
  static final Logger LOG = LoggerFactory.getLogger(RAPStreamClientPipelineInitializer.class);

  private final SSLContext _sslContext;
  private final SSLParameters _sslParameters;
  private final int _maxHeaderSize;
  private final int _maxChunkSize;
  private final long _maxResponseSize;
  private final boolean _enableSSLSessionResumption;
  private final int _sslHandShakeTimeout;

  /**
   * Creates new instance.
   * @param sslContext {@link SSLContext} to be used for TLS-enabled channel pipeline.
   * @param sslParameters {@link SSLParameters} to configure {@link javax.net.ssl.SSLEngine}s created
   *          from sslContext. This is somewhat redundant to
   *          SSLContext.getDefaultSSLParameters(), but those turned out to be
   *          exceedingly difficult to configure, so we can't pass all desired
   *          configuration in sslContext.
   */
  RAPStreamClientPipelineInitializer(SSLContext sslContext, SSLParameters sslParameters, int maxHeaderSize,
                                     int maxChunkSize, long maxResponseSize, boolean enableSSLSessionResumption,
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
    _enableSSLSessionResumption = enableSSLSessionResumption;
    _sslHandShakeTimeout = sslHandShakeTimeout;
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

  @Override
  protected void initChannel(NioSocketChannel ch)
  {
    if (_sslContext != null)
    {
      ch.pipeline().addLast(SessionResumptionSslHandler.PIPELINE_SESSION_RESUMPTION_HANDLER,
          new SessionResumptionSslHandler(_sslContext, _sslParameters, _enableSSLSessionResumption, _sslHandShakeTimeout));
    }
    ch.pipeline().addLast("codec", new HttpClientCodec(4096, _maxHeaderSize, _maxChunkSize));
    ch.pipeline().addLast("rapFullRequestEncoder", new RAPStreamFullRequestEncoder());
    ch.pipeline().addLast("rapEncoder", new RAPStreamRequestEncoder());
    ch.pipeline().addLast("rapDecoder", new RAPStreamResponseDecoder(_maxResponseSize));
    // the response handler catches the exceptions thrown by other layers. By consequence no handlers that throw exceptions
    // should be after this one, otherwise the exception won't be caught and managed by R2
    ch.pipeline().addLast("responseHandler", new RAPStreamResponseHandler());
    ch.pipeline().addLast("channelManager", new ChannelPoolStreamHandler());
  }
}
