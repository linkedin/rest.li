/*
   Copyright (c) 2013 LinkedIn Corp.

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

package com.linkedin.d2.balancer.properties;

import java.util.HashSet;
import java.util.Set;

/**
 * All config keys that clients can provide which are NOT specified in the set of keys published by the service that a
 * client can override. These keys MUST be a member of {@link AllowedClientPropertyKeys}
 *
 * @author Karan Parikh
 */
public enum AllowedClientOnlyPropertyKeys
{
  HTTP_RESPONSE_COMPRESSION_OPERATIONS(PropertyKeys.HTTP_RESPONSE_COMPRESSION_OPERATIONS);

  private final String _keyName;
  private static final Set<String> _allowedClientOnlyPropertyKeys;
  static
  {
    _allowedClientOnlyPropertyKeys = new HashSet<String>();
    for (AllowedClientOnlyPropertyKeys key: AllowedClientOnlyPropertyKeys.values())
    {
      _allowedClientOnlyPropertyKeys.add(key._keyName);
    }
  }

  AllowedClientOnlyPropertyKeys(String keyName)
  {
    _keyName = keyName;
  }

  public static boolean isAllowedClientOnlyPropertyKey(String key)
  {
    return _allowedClientOnlyPropertyKeys.contains(key);
  }
}
