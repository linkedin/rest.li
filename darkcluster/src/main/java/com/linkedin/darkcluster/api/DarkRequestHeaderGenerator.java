package com.linkedin.darkcluster.api;

import java.util.Objects;
import java.util.Optional;


/**
 * Interface for generating header meant to be sent as part of request to dark cluster
 */
public interface DarkRequestHeaderGenerator {
  /**
   * @return Header name / value pair for the given dark cluster.
   *         Can be empty if not applicable for the given dark cluster name.
   */
  Optional<HeaderNameValuePair> get(String darkClusterName);

  class HeaderNameValuePair {
    final String name;
    final String value;

    public HeaderNameValuePair(String name, String value) {
      this.name = name;
      this.value = value;
    }

    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.format("name=%s, value=%s", name, value);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      HeaderNameValuePair that = (HeaderNameValuePair) o;
      return name.equals(that.name)
          && value.equals(that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, value);
    }
  }
}
