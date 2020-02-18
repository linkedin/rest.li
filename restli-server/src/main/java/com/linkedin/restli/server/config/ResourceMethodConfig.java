package com.linkedin.restli.server.config;


import com.linkedin.restli.common.ConfigValue;

/**
 * Interface for rest.li resource method level configuration.
 *
 * @author jodzga
 * @author mnchen
 */
public interface ResourceMethodConfig
{
  /**
   * method level timeout configuration,
   *
   */
  public ConfigValue<Long> getTimeoutMs();

  /**
   * Config for whether this method will need to validate query parameters.
   */
  boolean shouldValidateQueryParams();

  /**
   * Config for whether this method will need to validate path/resource keys.
   */
  boolean shouldValidateResourceKeyParams();
}
