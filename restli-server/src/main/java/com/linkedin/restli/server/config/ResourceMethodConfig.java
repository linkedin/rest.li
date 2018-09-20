package com.linkedin.restli.server.config;


import com.linkedin.restli.common.ConfigValue;

/**
 * Interface for rest.li resource method level configuration. Currently this only has method level timeout configuration,
 * but can be extended later for other method level configuration.
 *
 * @author jodzga
 * @author mnchen
 */
public interface ResourceMethodConfig
{
  public ConfigValue<Long> getTimeoutMs();
}
