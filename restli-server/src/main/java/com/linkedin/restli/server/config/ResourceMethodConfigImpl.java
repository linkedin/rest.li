package com.linkedin.restli.server.config;

import com.linkedin.restli.common.ConfigValue;

import java.util.Objects;

/**
 * Implementation class for {@link ResourceMethodConfig}. When there are more method level configuration introduced
 * in the future, we should consider introducing a ResourceMethodConfigBuilder.
 *
 * @author mnchen
 */
class ResourceMethodConfigImpl implements ResourceMethodConfig
{
  private final ConfigValue<Long> _timeoutMs;
  private boolean _validateQueryParams;

  ResourceMethodConfigImpl(ConfigValue<Long> timeoutMs, boolean validateQueryParams)
  {
    _timeoutMs = timeoutMs;
    _validateQueryParams = validateQueryParams;
  }

  public ConfigValue<Long> getTimeoutMs()
  {
    return _timeoutMs;
  }

  @Override
  public boolean shouldValidateQueryParams() {
    return _validateQueryParams;
  }

  @Override
  public String toString()
  {
    return "ResourceMethodConfigImpl{" +
            "_timeoutMs=" + _timeoutMs +
            '}';
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ResourceMethodConfigImpl that = (ResourceMethodConfigImpl) o;
    return Objects.equals(_timeoutMs, that._timeoutMs);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(_timeoutMs);
  }
}
