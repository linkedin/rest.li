package com.linkedin.d2.xds;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class XdsChannelFactory
{
  private static final Logger _log = LoggerFactory.getLogger(XdsChannelFactory.class);

  public ManagedChannel createChannel(String xdsServerUri)
  {
    if (xdsServerUri == null || xdsServerUri.isEmpty())
    {
      _log.error("No xDS server address provided");
      return null;
    }

    return ManagedChannelBuilder.forTarget(xdsServerUri)
        // TODO: Will switch to TLS for productionization
        .usePlaintext()
        .keepAliveTime(5, TimeUnit.MINUTES)
        .build();
  }
}
