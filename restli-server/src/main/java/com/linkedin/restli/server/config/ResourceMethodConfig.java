package com.linkedin.restli.server.config;


import com.linkedin.restli.common.ConfigValue;
import java.util.Set;


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
  ConfigValue<Long> getTimeoutMs();

  /**
   * Config for whether this method will need to validate query parameters.
   */
  boolean shouldValidateQueryParams();

  /**
   * Config for whether this method will need to validate path/resource keys.
   */
  boolean shouldValidateResourceKeys();

  /**
   * Returns the method level list of fields that should be included when projection is applied.
   */
  ConfigValue<Set<String>> getAlwaysProjectedFields();
}
