package com.linkedin.restli.server.config;

import java.util.Map;

class RestLiMethodConfigImpl implements RestLiMethodConfig
{
  private final Map<String, Long> _timeoutMsConfig;
  private boolean _validateQueryParams;

  public RestLiMethodConfigImpl(Map<String, Long> timeoutMsConfig, boolean validateQueryParams)
  {
    _timeoutMsConfig = timeoutMsConfig;
    _validateQueryParams = validateQueryParams;
  }

  @Override
  public Map<String, Long> getTimeoutMsConfig()
  {
    return _timeoutMsConfig;
  }

  @Override
  public boolean shouldValidateQueryParams() {
    return _validateQueryParams;
  }

  public void setValidateQueryParams(boolean validateQueryParams) {
    _validateQueryParams = validateQueryParams;
  }
}
