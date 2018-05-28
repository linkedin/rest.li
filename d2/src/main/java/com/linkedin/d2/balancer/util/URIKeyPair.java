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

import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.util.ArgumentUtil;
import java.net.URI;
import java.util.Objects;


/**
 * This is the input to {@link com.linkedin.d2.balancer.URIMapper}.
 *
 * The input is {@code KEY}, which is the resource key, and {@code uri}, which is the request uri for d2 request.
 *
 * @param <KEY> the type of the resource key
 *
 * @author Alex Jing
 */
public class URIKeyPair<KEY>
{
  private final KEY _key;
  private final URI _requestUri;

  public URIKeyPair(KEY key, URI uri)
  {
    ArgumentUtil.notNull(key, "key");
    ArgumentUtil.notNull(uri, "uri");
    assert uri.getScheme().equals("d2");

    _key = key;
    _requestUri = uri;
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
            Objects.equals(_requestUri, that._requestUri);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(_key, _requestUri);
  }
}
