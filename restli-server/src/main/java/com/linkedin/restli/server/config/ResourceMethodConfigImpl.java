package com.linkedin.restli.server.config;

import com.linkedin.restli.common.ConfigValue;

import java.util.Objects;
import java.util.Set;


/**
 * Implementation class for {@link ResourceMethodConfig}. When there are more method level configuration introduced
 * in the future, we should consider introducing a ResourceMethodConfigBuilder.
 *
 * @author mnchen
 */
public class ResourceMethodConfigImpl implements ResourceMethodConfig
{
  private final ConfigValue<Long> _timeoutMs;
  private final ConfigValue<Set<String>> _alwaysProjectedFields;
  private boolean _validateQueryParams;
  private boolean _validateResourceKeys;

  public static final ResourceMethodConfig DEFAULT_CONFIG = new ResourceMethodConfigImpl(null, false, false, null);

  @Deprecated
  public ResourceMethodConfigImpl(ConfigValue<Long> timeoutMs, boolean validateQueryParams, boolean validateResourceKeys)
  {
    this(timeoutMs, validateQueryParams, validateResourceKeys, null);
  }

  ResourceMethodConfigImpl(ConfigValue<Long> timeoutMs, boolean validateQueryParams, boolean validateResourceKeys,
      ConfigValue<Set<String>> alwaysProjectedFields)
  {
    _timeoutMs = timeoutMs;
    _validateQueryParams = validateQueryParams;
    _validateResourceKeys = validateResourceKeys;
    _alwaysProjectedFields = alwaysProjectedFields;
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
  public boolean shouldValidateResourceKeys() {
    return _validateResourceKeys;
  }

  @Override
  public ConfigValue<Set<String>> getAlwaysProjectedFields()
  {
    return _alwaysProjectedFields;
  }

  @Override
  public String toString()
  {
    return "ResourceMethodConfigImpl{" +
          "_timeoutMs=" + _timeoutMs +
          ", _validateQueryParams=" + _validateQueryParams +
          ", _validateResourceKeys=" + _validateResourceKeys +
        "}";
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
    {
      return true;
    }
    if (o == null || getClass() != o.getClass())
    {
      return false;
    }
    ResourceMethodConfigImpl that = (ResourceMethodConfigImpl) o;
    return _validateQueryParams == that._validateQueryParams && _validateResourceKeys
        == that._validateResourceKeys
        && _timeoutMs.equals(that._timeoutMs);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(_timeoutMs, _validateQueryParams, _validateResourceKeys);
  }

  public void setValidateQueryParams(boolean validateQueryParams)
  {
    _validateQueryParams = validateQueryParams;
  }
}
