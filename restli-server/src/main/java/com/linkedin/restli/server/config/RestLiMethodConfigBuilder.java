package com.linkedin.restli.server.config;

import java.util.HashMap;
import java.util.Map;


/**
 * A builder to build {@link RestLiMethodConfig}. This is created for extensibility.
 *
 * @author mnchen
 */
public class RestLiMethodConfigBuilder
{
  private final Map<String, Long> _timeoutMsConfig = new HashMap<>();
  private final Map<String, String> _alwaysProjectedFieldsConfig = new HashMap<>();
  // Whether to validate parameter in the query parameters.
  private boolean shouldValidateQueryParams = false;
  private boolean shouldValidateResourceKeys = false;

  public RestLiMethodConfigBuilder()
  {
  }

  public RestLiMethodConfigBuilder(RestLiMethodConfig config)
  {
    addConfig(config);
  }

  public void addConfig(RestLiMethodConfig config)
  {
    if (config != null)
    {
      addTimeoutMsConfigMap(config.getTimeoutMsConfig());
      withShouldValidateQueryParams(config.shouldValidateQueryParams());
      withShouldValidateResourceKeys(config.shouldValidateResourceKey());
      addAlwaysProjectedFieldsMap(config.getAlwaysProjectedFieldsConfig());
    }
  }

  public RestLiMethodConfig build()
  {
    return new RestLiMethodConfigImpl(_timeoutMsConfig, shouldValidateQueryParams, shouldValidateResourceKeys,
        _alwaysProjectedFieldsConfig);
  }

  public RestLiMethodConfigBuilder withShouldValidateQueryParams(boolean shouldValidateQueryParams)
  {
    this.shouldValidateQueryParams = shouldValidateQueryParams;
    return this;
  }

  public RestLiMethodConfigBuilder withShouldValidateResourceKeys(boolean shouldValidateResourceKeys)
  {
    this.shouldValidateResourceKeys = shouldValidateResourceKeys;
    return this;
  }

  public RestLiMethodConfigBuilder addTimeoutMsConfigMap(Map<String, Long> config)
  {
    _timeoutMsConfig.putAll(config);
    return this;
  }

  public RestLiMethodConfigBuilder addTimeoutMs(String key, long value)
  {
    _timeoutMsConfig.put(key, value);
    return this;
  }

  public RestLiMethodConfigBuilder clearTimeoutMs()
  {
    _timeoutMsConfig.clear();
    return this;
  }

  public RestLiMethodConfigBuilder addAlwaysProjectedFieldsMap(Map<String, String> config)
  {
    _alwaysProjectedFieldsConfig.putAll(config);
    return this;
  }

  public RestLiMethodConfigBuilder addAlwaysProjectedFields(String key, String value)
  {
    _alwaysProjectedFieldsConfig.put(key, value);
    return this;
  }

  public RestLiMethodConfigBuilder clearAlwaysProjectedFields()
  {
    _alwaysProjectedFieldsConfig.clear();
    return this;
  }
}
