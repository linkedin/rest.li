package com.linkedin.restli.server.config;

import java.util.Map;

/**
 * Interface holder for all rest.li method level configuration, such as method-level timeout, concurrency limit, etc.
 *
 * @author mnchen
 */
public interface RestLiMethodConfig
{
  /**
   * method-level timeout,
   */
  public Map<String, Long> getTimeoutMsConfig();

  /**
   * Gets whether query parameter validation against its parameter data template is enabled
   */
  boolean shouldValidateQueryParams();

  /**
   * Gets whether resource/path keys validation against its parameter data template is enabled
   */
  boolean shouldValidateResourceKeyParams();
}
