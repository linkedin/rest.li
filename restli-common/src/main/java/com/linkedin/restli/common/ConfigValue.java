package com.linkedin.restli.common;

import java.util.Objects;
import java.util.Optional;

/**
 * Class representing a config value of any type, for example, timeout, etc. This was inspired by how ParSeqRestClient
 * defines and resolves config values from client side.
 * @param <T> config value type.
 *
 * @author jodzga
 * @author mnchen
 */
public class ConfigValue<T> {

  private final T _value;
  private final String _source;

  public ConfigValue(T value, String source) {
    _value = value;
    _source = source;
  }

  public T getValue() {
    return _value;
  }

  public Optional<String> getSource() {
    return Optional.ofNullable(_source);
  }

  @Override
  public String toString() {
    return "ConfigValue [value=" + _value + ", source=" + _source + "]";
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ConfigValue<?> that = (ConfigValue<?>) o;
    return Objects.equals(_value, that._value) &&
            Objects.equals(_source, that._source);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(_value, _source);
  }
}

