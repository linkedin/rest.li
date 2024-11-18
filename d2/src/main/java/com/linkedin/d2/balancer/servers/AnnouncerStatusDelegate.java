package com.linkedin.d2.balancer.servers;

import java.net.URI;


public interface AnnouncerStatusDelegate
{
  /**
   * @return true if the markup intent has been sent.
   */
  boolean isMarkUpIntentSent();

  /**
   * @return true if the dark warmup mark up intent has been sent.
   */
  boolean isDarkWarmupMarkUpIntentSent();

  /**
   * @return the name of the regular cluster that the announcer manages.
   */
  String getCluster();

  /**
   * @return the name of the warmup cluster that the announcer manages.
   */
  String getWarmupCluster();

  /**
   * @return the uri that the announcer manages.
   */
  URI getURI();
}
