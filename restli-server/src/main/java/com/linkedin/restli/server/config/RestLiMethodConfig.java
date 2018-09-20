package com.linkedin.restli.server.config;

import java.util.Map;

/**
 * Interface holder for all rest.li method level configuration, such as method-level timeout, concurrency limit, etc.
 *
 * @author mnchen
 */
public interface RestLiMethodConfig
{
  public Map<String, Long> getTimeoutMsConfig();
}
