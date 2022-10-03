package com.linkedin.d2.xds;

import io.grpc.ManagedChannel;
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
        .build();
  }
}
