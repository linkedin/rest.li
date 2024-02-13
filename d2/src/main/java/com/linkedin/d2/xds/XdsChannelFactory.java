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

import com.google.gson.Gson;
import io.grpc.ManagedChannel;
import io.grpc.internal.GrpcUtil;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class XdsChannelFactory
{
  private static final Logger _log = LoggerFactory.getLogger(XdsChannelFactory.class);


  @SuppressWarnings("unchecked")
  private static final Map<String, ?> DEFAULT_SHUFFLED_PICK_FIRST_CONFIG = new Gson().fromJson(
      "{ \"loadBalancingConfig\": " +
          "    [ { \"pick_first\": { \"shuffleAddressList\": true } } ]" +
          "}",
      Map.class);

  private final SslContext _sslContext;
  private final String _xdsServerUri;
  @Nullable
  private final String _defaultLoadBalancingPolicy;

  /**
   * Invokes alternative constructor with {@code defaultLoadBalancingPolicy} as {@code null}.
   */
  public XdsChannelFactory(SslContext sslContext, String xdsServerUri)
  {
    this(sslContext, xdsServerUri, null);
  }

  /**
   * @param sslContext                 The sslContext to use. If {@code null}, SSL will not be used when connecting to
   *                                   the xDS server.
   * @param xdsServerUri               The address of the xDS server. Can be an IP address or a domain with multiple
   *                                   underlying A/AAAA records.
   * @param defaultLoadBalancingPolicy If provided, changes the default load balancing policy on the builder to the
   *                                   given policy (see
   *                                   {@link io.grpc.ManagedChannelBuilder#defaultLoadBalancingPolicy(String)}).
   *                                   Otherwise, by default it uses the "pick_first" policy, with the
   *                                   "shuffleAddressList" config set to {@code true}. This will make the managed
   *                                   channel always pick a random xDS server address.
   * @see <a href="https://daniel.haxx.se/blog/2012/01/03/getaddrinfo-with-round-robin-dns-and-happy-eyeballs/"/>
   * Details on IPv6 routing.
   */
  public XdsChannelFactory(
      @Nullable SslContext sslContext,
      String xdsServerUri,
      @Nullable String defaultLoadBalancingPolicy)
  {
    _sslContext = sslContext;
    _xdsServerUri = xdsServerUri;
    _defaultLoadBalancingPolicy = defaultLoadBalancingPolicy;
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
    }
    else
    {
      _log.info("Using \"pick_first\" policy with shuffleAddressList=true for xDS channel");
      builder = builder
          .defaultLoadBalancingPolicy("pick_first")
          .defaultServiceConfig(DEFAULT_SHUFFLED_PICK_FIRST_CONFIG);
    }

    if (_sslContext != null)
    {
      builder.sslContext(_sslContext);
    }
    else
    {
      builder.usePlaintext();
    }


    return builder.keepAliveTime(5, TimeUnit.MINUTES)
        // No proxy wanted here; the default proxy detector can mistakenly detect forwarded ports as proxies.
        .proxyDetector(GrpcUtil.NOOP_PROXY_DETECTOR)
        .build();
  }
}
