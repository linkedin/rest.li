package com.linkedin.d2.balancer;

import com.linkedin.d2.discovery.util.D2Utils;
import javax.annotation.Nonnull;
import org.slf4j.Logger;


/**
 * Abstract Factory for creating instance of {@link LoadBalancerWithFacilities}
 */
public abstract class AbstractLoadBalancerWithFacilitiesFactory implements LoadBalancerWithFacilitiesFactory
{
  public static final String LOAD_BALANCER_TYPE_WARNING = "[ACTION REQUIRED] Zookeeper-based D2 Client "
      + "is deprecated (unless talking to a locally-deployed ZK, or for testing EI ZK) and must be migrated to INDIS. "
      + "See instructions at https://iwww.corp.linkedin.com/wiki/cf/display/ENGS/INDIS+Rollout+Issue+Guidelines+for+Java+Apps" +
      "\nFailing to do so will block other apps from stopping ZK announcements and will be escalated for site-up "
      + "stability. Non-INDIS D2 Client will CRASH in OCTOBER 2025.";

  // As standard LinkedIn code set LI-specific properties in d2 clients, this is true when LI code uses raw d2 client
  // builder to create a d2 client.
  protected boolean isLiRawD2Client = false;

  @Override
  public void setIsLiRawD2Client(boolean isRawD2Client)
  {
    isLiRawD2Client = isRawD2Client;
  }

  public void logLoadBalancerTypeWarning(@Nonnull Logger LOG)
  {
    LOG.warn(LOAD_BALANCER_TYPE_WARNING);
  }

  public void logAppProps(@Nonnull Logger LOG)
  {
    LOG.info("LI properties:\n {}", D2Utils.getSystemProperties());
  }
}
