/*
   Copyright (c) 2018 LinkedIn Corp.

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
import java.util.Objects;
import java.util.Collections;
import java.util.Set;


/**
 * This is the input to {@link com.linkedin.d2.balancer.URIMapper}.
 *
 * The input is {@code KEY}, which is the resource key, and {@code uri}, which is the request uri for d2 request.
 *
 * Alternatively, under custom use case, user can provide a set of partition ids for a given uri. If this is the case,
 * d2 partitioning will be bypassed and the provided partition ids will be used.
 *
 * NOTE: if partitions ids are provided, {@code KEY} is not allowed
 *
 * @param <KEY> the type of the resource key
 *
 * @author Alex Jing
 */
public class URIKeyPair<KEY>
{
  private final KEY _key;
  private final URI _requestUri;
  private final Set<Integer> _overriddenPartitionIds;

  public URIKeyPair(KEY key, URI uri)
  {
    ArgumentUtil.notNull(key, "key");
    ArgumentUtil.notNull(uri, "uri");
    assert uri.getScheme().equals("d2");

    _key = key;
    _requestUri = uri;
    _overriddenPartitionIds = Collections.emptySet();
  }

  public URIKeyPair(URI uri, Set<Integer> overriddenPartitionIds)
  {
    ArgumentUtil.notNull(overriddenPartitionIds, "overridden partition ids");
    ArgumentUtil.notNull(uri, "uri");
    _key = null;
    _requestUri = uri;
    _overriddenPartitionIds = Collections.unmodifiableSet(overriddenPartitionIds);
  }

  public KEY getKey()
  {
    return _key;
  }

  public URI getRequestUri()
  {
    return _requestUri;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    URIKeyPair<?> that = (URIKeyPair<?>) o;
    return Objects.equals(_key, that._key) &&
            Objects.equals(_requestUri, that._requestUri) &&
            Objects.equals(_overriddenPartitionIds, that._overriddenPartitionIds);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(_key, _requestUri, _overriddenPartitionIds);
  }

  public boolean hasOverriddenPartitionIds()
  {
    return !_overriddenPartitionIds.isEmpty();
  }

  public Set<Integer> getOverriddenPartitionIds()
  {
    return _overriddenPartitionIds;
  }
}
