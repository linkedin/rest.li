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
import io.grpc.internal.GrpcUtil;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class XdsChannelFactory
{
  private static final Logger _log = LoggerFactory.getLogger(XdsChannelFactory.class);

  private final SslContext _sslContext;
  private final String _xdsServerUri;

  public XdsChannelFactory(SslContext sslContext, String xdsServerUri) {
    _sslContext = sslContext;
    _xdsServerUri = xdsServerUri;
  }

  public ManagedChannel createChannel()
  {
    if (_xdsServerUri == null || _xdsServerUri.isEmpty())
    {
      _log.error("No xDS server address provided");
      return null;
    }

    NettyChannelBuilder builder = NettyChannelBuilder.forTarget(_xdsServerUri);

    if (_sslContext != null) {
      builder.sslContext(_sslContext);
    } else {
      builder.usePlaintext();
    }

    return builder.keepAliveTime(5, TimeUnit.MINUTES)
        .proxyDetector(GrpcUtil.NOOP_PROXY_DETECTOR)
        .build();
  }
}
