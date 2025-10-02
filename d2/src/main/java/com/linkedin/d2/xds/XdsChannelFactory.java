/*
   Copyright (c) 2023 LinkedIn Corp.

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

package com.linkedin.d2.xds;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.internal.GrpcUtil;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.channel.ChannelOption;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;


public class XdsChannelFactory
{
  private static final long DEFAULT_KEEPALIVE_TIME_MINS = 5L; // Default keep alive time for the xDS channel in minutes.
  // Default connection timeout.
  private static final long DEFAULT_CONNECTION_TIMEOUT_MS = 30_000L;

  private static final Logger _log = LoggerFactory.getLogger(XdsChannelFactory.class);

  private final SslContext _sslContext;
  private final String _xdsServerUri;
  @Nullable
  private final String _defaultLoadBalancingPolicy;
  @Nullable
  private final Map<String, ?> _loadBalancingPolicyConfig;
  private final long _keepAliveTimeMins;
  private final long _connectionTimeoutMs;

  /**
   * Invokes alternative constructor with {@code defaultLoadBalancingPolicy} and {@code loadBalancingPolicyConfig} as
   * {@code null}.
   */
  public XdsChannelFactory(SslContext sslContext, String xdsServerUri)
  {
    this(sslContext, xdsServerUri, null, null);
  }

  /**
   * Invokes alternative constructor with {@code loadBalancingPolicyConfig} as {@code null}.
   */
  public XdsChannelFactory(SslContext sslContext, String xdsServerUri, @Nullable String defaultLoadBalancingPolicy)
  {
    this(sslContext, xdsServerUri, defaultLoadBalancingPolicy, null);
  }

  public XdsChannelFactory(
      @Nullable SslContext sslContext,
      String xdsServerUri,
      @Nullable String defaultLoadBalancingPolicy,
      @Nullable Map<String, ?> loadBalancingPolicyConfig)
  {
    this(sslContext, xdsServerUri, defaultLoadBalancingPolicy, loadBalancingPolicyConfig, null, null);
  }

  @Deprecated
  public XdsChannelFactory(
      @Nullable SslContext sslContext,
      String xdsServerUri,
      @Nullable String defaultLoadBalancingPolicy,
      @Nullable Map<String, ?> loadBalancingPolicyConfig,
      @Nullable Long keepAliveTimeMins)
  {
    this(
        sslContext,
        xdsServerUri,
        defaultLoadBalancingPolicy,
        loadBalancingPolicyConfig,
        keepAliveTimeMins,
        null
    );
  }

  /**
   * @param sslContext                 The sslContext to use. If {@code null}, SSL will not be used when connecting to
   *                                   the xDS server.
   * @param xdsServerUri               The address of the xDS server. Can be an IP address or a domain with multiple
   *                                   underlying A/AAAA records.
   * @param defaultLoadBalancingPolicy If provided, changes the default load balancing policy on the builder to the
   *                                   given policy (see
   *                                   {@link ManagedChannelBuilder#defaultLoadBalancingPolicy(String)}).
   * @param loadBalancingPolicyConfig  Can only be provided if {@code defaultLoadBalancingPolicy} is provided. Will be
   *                                   provided to {@link ManagedChannelBuilder#defaultServiceConfig(Map)}})
   *                                   after being wrapped in a "loadBalancingConfig" JSON context that corresponds
   *                                   to the load balancing policy name provided by {@code defaultLoadBalancingPolicy}.
   * @param keepAliveTimeMins          Time in minutes to keep the xDS channel alive without read activity, will send a
   *                                   keepalive ping to the server, if the time passed. If {@code null} or less than 0,
   *                                   defaults to {@link #DEFAULT_KEEPALIVE_TIME_MINS}.
   * @param connectionTimeoutMs        If the client cannot successfully establish the connection to an xDS server
   *                                   within this timeout (in millis), it will look for another server.
   * @see <a href="https://daniel.haxx.se/blog/2012/01/03/getaddrinfo-with-round-robin-dns-and-happy-eyeballs/"/>
   * Details on IPv6 routing.
   */
  public XdsChannelFactory(
      @Nullable SslContext sslContext,
      String xdsServerUri,
      @Nullable String defaultLoadBalancingPolicy,
      @Nullable Map<String, ?> loadBalancingPolicyConfig,
      @Nullable Long keepAliveTimeMins,
      @Nullable Long connectionTimeoutMs
  )
  {
    _sslContext = sslContext;
    _xdsServerUri = xdsServerUri;
    if (defaultLoadBalancingPolicy == null && loadBalancingPolicyConfig != null)
    {
      _log.warn("loadBalancingPolicyConfig ignored because defaultLoadBalancingPolicy was not provided.");
    }
    _defaultLoadBalancingPolicy = defaultLoadBalancingPolicy;
    _loadBalancingPolicyConfig = loadBalancingPolicyConfig;
    _keepAliveTimeMins = (keepAliveTimeMins != null && keepAliveTimeMins > 0)
        ? keepAliveTimeMins
        : DEFAULT_KEEPALIVE_TIME_MINS;
    _connectionTimeoutMs = (connectionTimeoutMs != null && connectionTimeoutMs > 0)
        ? connectionTimeoutMs
        : DEFAULT_CONNECTION_TIMEOUT_MS;
    _log.info("Creating xDS channel with server URI: {}, SSL enabled: {}, load balancing policy: {}, "
            + "load balancing policy config: {}, keep alive time: {} mins, connection timeout: {} ms",
        _xdsServerUri, (_sslContext != null), _defaultLoadBalancingPolicy, _loadBalancingPolicyConfig,
        _keepAliveTimeMins, _connectionTimeoutMs);
  }

  public ManagedChannel createChannel()
  {
    if (_xdsServerUri == null || _xdsServerUri.isEmpty())
    {
      _log.error("No xDS server address provided");
      return null;
    }

    NettyChannelBuilder builder = NettyChannelBuilder.forTarget(_xdsServerUri);
    if (_defaultLoadBalancingPolicy != null)
    {
      _log.info("Applying custom load balancing policy for xDS channel: {}", _defaultLoadBalancingPolicy);
      builder = builder.defaultLoadBalancingPolicy(_defaultLoadBalancingPolicy);

      if (_loadBalancingPolicyConfig != null)
      {
        _log.info("Applying custom load balancing config for xDS channel: {}", _loadBalancingPolicyConfig);
        builder = builder
            .defaultServiceConfig(
                singletonMap("loadBalancingConfig",
                    singletonList(singletonMap(_defaultLoadBalancingPolicy, _loadBalancingPolicyConfig))));
      }
    }

    if (_sslContext != null)
    {
      builder.sslContext(_sslContext);
    }
    else
    {
      builder.usePlaintext();
    }


    return builder.keepAliveTime(_keepAliveTimeMins, TimeUnit.MINUTES) // Keep alive time for the xDS channel.
        // No proxy wanted here; the default proxy detector can mistakenly detect forwarded ports as proxies.
        .proxyDetector(GrpcUtil.NOOP_PROXY_DETECTOR)
        .withOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) _connectionTimeoutMs)
        .build();
  }
}
