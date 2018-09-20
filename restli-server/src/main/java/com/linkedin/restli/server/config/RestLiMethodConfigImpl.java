package com.linkedin.restli.server.config;

import java.util.Map;

class RestLiMethodConfigImpl implements RestLiMethodConfig
{
  private final Map<String, Long> _timeoutMsConfig;

  public RestLiMethodConfigImpl(Map<String, Long> timeoutMsConfig)
  {
    _timeoutMsConfig = timeoutMsConfig;
  }

  @Override
  public Map<String, Long> getTimeoutMsConfig()
  {
    return _timeoutMsConfig;
  }
}
