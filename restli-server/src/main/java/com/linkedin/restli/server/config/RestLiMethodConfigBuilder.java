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
    }
  }

  public RestLiMethodConfig build()
  {
    return new RestLiMethodConfigImpl(_timeoutMsConfig);
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
}
