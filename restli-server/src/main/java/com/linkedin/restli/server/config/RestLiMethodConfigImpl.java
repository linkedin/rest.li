package com.linkedin.restli.server.config;

import java.util.Map;

class RestLiMethodConfigImpl implements RestLiMethodConfig
{
  private final Map<String, Long> _timeoutMsConfig;
  private boolean _validateQueryParams;
  private boolean _validateResourceKeys;

  public RestLiMethodConfigImpl(Map<String, Long> timeoutMsConfig, boolean validateQueryParams,
      boolean validateResourceKeys) {
    _timeoutMsConfig = timeoutMsConfig;
    _validateQueryParams = validateQueryParams;
    _validateResourceKeys = validateResourceKeys;
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
}
