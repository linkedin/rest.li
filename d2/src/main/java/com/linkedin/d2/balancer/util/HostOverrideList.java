/*
   Copyright (c) 2020 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.linkedin.d2.balancer.util;

import com.linkedin.util.ArgumentUtil;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;


/**
 * Stores the list of host overrides for cluster and services. The order of the overrides are stored in the same
 * order as the additions. Checks for the first match and return the overridden {@link URI}.
 */
public class HostOverrideList
{
  private Map<Key, URI> _overrides = new LinkedHashMap<>();

  public void addClusterOverride(String cluster, URI uri) {
    ArgumentUtil.notNull(cluster, "cluster");
    ArgumentUtil.notNull(uri, "uri");
    _overrides.put(new Key(cluster, null), uri);
  }

  public void addServiceOverride(String service, URI uri) {
    ArgumentUtil.notNull(service, "service");
    ArgumentUtil.notNull(uri, "uri");
    _overrides.put(new Key(null, service), uri);
  }

  public void addOverride(URI uri) {
    ArgumentUtil.notNull(uri, "uri");
    _overrides.put(Key.WILDCARD_KEY, uri);
  }

  /**
   * Gets the overridden URI for the given cluster and service.
   * @param cluster Cluster name of the override.
   * @param service Service name of the override.
   * @return The overridden URI for the given cluster and service; {@code null} otherwise.
   */
  public URI getOverride(String cluster, String service)
  {
    for (Map.Entry<Key, URI> override : _overrides.entrySet())
    {
      if (override.getKey().match(cluster, service)) {
        return override.getValue();
      }
    }
    return null;
  }

  /**
   * Key implementation of the override map. Key includes a cluster and a service name. If either cluster or
   * service is {@code null}, then the null cluster or service is treated as a wildcard.
   */
  private static class Key {
    private static final Key WILDCARD_KEY = new Key(null, null);
    private final String _cluster;
    private final String _service;

    public Key(String cluster, String service) {
      _cluster = cluster;
      _service = service;
    }

    /**
     * Checks if the provided cluster and service names match this key.
     * @param cluster Cluster name to check against.
     * @param service Service name to check against.
     * @return {@code True} if provided cluster and service name match the key; {@code false} otherwise.
     */
    public boolean match(String cluster, String service) {
      if (this == WILDCARD_KEY) {
        return true;
      }
      else if (_cluster == null) {
        return Objects.equals(_service, service);
      }
      else if (_service == null) {
        return Objects.equals(_cluster, cluster);
      }
      else {
        return Objects.equals(_cluster, cluster) && Objects.equals(_service, service);
      }
    }
  }
}
