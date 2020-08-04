package com.linkedin.restli.server.config;

import java.util.Collections;
import java.util.Map;


class RestLiMethodConfigImpl implements RestLiMethodConfig
{
  private final Map<String, Long> _timeoutMsConfig;
  private boolean _validateQueryParams;
  private boolean _validateResourceKeys;
  private final Map<String, String> _alwaysProjectedFieldsConfig;

  /**
   * @deprecated Use {@link RestLiMethodConfigBuilder} to build this type.
   */
  @Deprecated
  public RestLiMethodConfigImpl(Map<String, Long> timeoutMsConfig, boolean validateQueryParams,
      boolean validateResourceKeys)
  {
    this(timeoutMsConfig, validateQueryParams, validateResourceKeys, Collections.emptyMap());
  }

  RestLiMethodConfigImpl(Map<String, Long> timeoutMsConfig, boolean validateQueryParams,
      boolean validateResourceKeys, Map<String, String> alwaysProjectedFieldsConfig)
  {
    _timeoutMsConfig = timeoutMsConfig;
    _validateQueryParams = validateQueryParams;
    _validateResourceKeys = validateResourceKeys;
    _alwaysProjectedFieldsConfig = alwaysProjectedFieldsConfig;
  }
  @Override
  public Map<String, Long> getTimeoutMsConfig()
  {
    return _timeoutMsConfig;
  }

  @Override
  public boolean shouldValidateQueryParams()
  {
    return _validateQueryParams;
  }

  @Override
  public boolean shouldValidateResourceKey()
  {
    return _validateResourceKeys;
  }

  @Override
  public Map<String, String> getAlwaysProjectedFieldsConfig()
  {
    return _alwaysProjectedFieldsConfig;
  }
}
