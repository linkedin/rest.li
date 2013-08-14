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
 * Keys for properties that clients can supply.
 *
 * @author Karan Parikh
 */
public enum AllowedClientPropertyKeys
{
  // allowed Degrader property keys
  DEGRADER_LOG_ENABLED(PropertyKeys.DEGRADER_LOG_ENABLED),
  DEGRADER_LATENCY_TO_USE(PropertyKeys.DEGRADER_LATENCY_TO_USE),
  DEGRADER_UP_STEP(PropertyKeys.DEGRADER_UP_STEP),
  DEGRADER_DOWN_STEP(PropertyKeys.DEGRADER_DOWN_STEP),
  DEGRADER_HIGH_LATENCY(PropertyKeys.DEGRADER_HIGH_LATENCY),
  DEGRADER_LOW_LATENCY(PropertyKeys.DEGRADER_LOW_LATENCY),
  DEGRADER_HIGH_ERROR_RATE(PropertyKeys.DEGRADER_HIGH_ERROR_RATE),
  DEGRADER_LOW_ERROR_RATE(PropertyKeys.DEGRADER_LOW_ERROR_RATE),

  // allowed Transport client property keys
  HTTP_REQUEST_TIMEOUT(PropertyKeys.HTTP_REQUEST_TIMEOUT),
  HTTP_MAX_RESPONSE_SIZE(PropertyKeys.HTTP_MAX_RESPONSE_SIZE),
  HTTP_POOL_SIZE(PropertyKeys.HTTP_POOL_SIZE),
  HTTP_IDLE_TIMEOUT(PropertyKeys.HTTP_IDLE_TIMEOUT),
  HTTP_SHUTDOWN_TIMEOUT(PropertyKeys.HTTP_SHUTDOWN_TIMEOUT),
  HTTP_SSL_CONTEXT(PropertyKeys.HTTP_SSL_CONTEXT),
  HTTP_SSL_PARAMS(PropertyKeys.HTTP_SSL_PARAMS),
  HTTP_RESPONSE_COMPRESSION_OPERATIONS(PropertyKeys.HTTP_RESPONSE_COMPRESSION_OPERATIONS);

  private static final Set<String> _allowedKeys;
  static
  {
    _allowedKeys = new HashSet<String>();
    for (AllowedClientPropertyKeys propertyKey: AllowedClientPropertyKeys.values())
    {
      _allowedKeys.add(propertyKey._keyName);
    }
  }

  private final String _keyName;

  AllowedClientPropertyKeys(String keyName)
  {
    _keyName = keyName;
  }

  public String getKeyName()
  {
    return _keyName;
  }

  public static boolean isAllowedConfigKey(String key)
  {
    return _allowedKeys.contains(key);
  }
}
