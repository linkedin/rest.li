package com.linkedin.restli.server.config;

import java.util.Map;


/**
 * Interface holder for all rest.li method level configuration, such as method-level timeout, concurrency limit, etc.
 *
 * @author mnchen
 */
public interface RestLiMethodConfig
{
  enum ConfigType
  {
    TIMEOUT("timeoutMs"),
    ALWAYS_PROJECTED_FIELDS("alwaysProjectedFields");

    ConfigType(String configName)
    {
      this._configName = configName;
    }

    String getConfigName()
    {
      return _configName;
    }

    private final String _configName;
  }

  /**
   * method-level timeout,
   */
  Map<String, Long> getTimeoutMsConfig();

  /**
   * Gets whether query parameter validation against its parameter data template is enabled
   */
  boolean shouldValidateQueryParams();

  /**
   * Gets whether resource/path keys validation against its parameter data template is enabled
   */
  boolean shouldValidateResourceKey();

  /**
   * Returns the method level set of fields that should be included when projection is applied. The field set(value) is
   * provided as comma separated string.
   */
  Map<String, String> getAlwaysProjectedFieldsConfig();
}
