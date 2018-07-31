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

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;


/**
 * This class contains the results for calls to {@link com.linkedin.d2.balancer.URIMapper}
 *
 * It returns a mapping between host and all keys that mapped to that host.
 * It also returns a set of keys that URIMapper is unable to map.
 *
 * @author Alex Jing
 */
public class URIMappingResult<KEY>
{
  private final Map<URI, Set<KEY>> _mappedKeys;
  private final Set<KEY> _unmappedKeys;

  public URIMappingResult(Map<URI, Set<KEY>> mappingResults, Set<KEY> unmappedKeys)
  {
    _mappedKeys = Collections.unmodifiableMap(mappingResults);
    _unmappedKeys = Collections.unmodifiableSet(unmappedKeys);
  }

  public Map<URI, Set<KEY>> getMappedResults()
  {
    return _mappedKeys;
  }

  public Set<KEY> getUnmappedKeys()
  {
    return _unmappedKeys;
  }
}
