package com.linkedin.restli.server.config;

import com.linkedin.restli.common.ConfigValue;

import java.util.Objects;

/**
 * Implementation class for {@link ResourceMethodConfig}. When there are more method level configuration introduced
 * in the future, we should consider introducing a ResourceMethodConfigBuilder.
 *
 * @author mnchen
 */
public class ResourceMethodConfigImpl implements ResourceMethodConfig
{
  private final ConfigValue<Long> _timeoutMs;
  private boolean _validateQueryParams;
  private boolean _validateResourceKeyParams;

  public ResourceMethodConfigImpl(ConfigValue<Long> timeoutMs, boolean validateQueryParams, boolean validateResourceKeyParams)
  {
    _timeoutMs = timeoutMs;
    _validateQueryParams = validateQueryParams;
    _validateResourceKeyParams = validateResourceKeyParams;
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
  public boolean shouldValidateResourceKeyParams() {
    return _validateResourceKeyParams;
  }

  @Override
  public String toString() {
    return "ResourceMethodConfigImpl{" +
          "_timeoutMs=" + _timeoutMs +
          ", _validateQueryParams=" + _validateQueryParams +
          ", _validateResourceKeyParams=" + _validateResourceKeyParams +
        "}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResourceMethodConfigImpl that = (ResourceMethodConfigImpl) o;
    return _validateQueryParams == that._validateQueryParams && _validateResourceKeyParams
        == that._validateResourceKeyParams
        && _timeoutMs.equals(that._timeoutMs);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(_timeoutMs, _validateQueryParams, _validateResourceKeyParams);
  }
}
