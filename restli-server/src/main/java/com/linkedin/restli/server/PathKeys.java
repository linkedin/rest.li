/*
   Copyright (c) 2012 LinkedIn Corp.

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

package com.linkedin.restli.server;

import java.util.Set;

/**
 * A container for resource keys parsed out of the request providing methods for key type
 * conversion.
 *
 * @author Josh Walker
 * @version $Revision: $
 */

public interface PathKeys
{
  /**
   * Get key value by name.
   *
   * @param key key name
   * @param <T> key value type
   * @return key value
   */
  <T> T get(String key);

  /**
   * Get key value by name as Integer.
   *
   * @param key key name
   * @return key value as Integer
   */
  Integer getAsInt(String key);

  /**
   * Get key value by name as Long.
   *
   * @param key key name
   * @return key value as Integer
   */
  Long getAsLong(String key);

  /**
   * Get key value by name as String.
   *
   * @param key key name
   * @return key value as Integer
   */
  String getAsString(String key);

  /**
   * Get the resource batch keys untyped.
   *
   * @return set of batch key values.
   */
  @Deprecated
  Set<?> getBatchKeys();

  /**
   * Get the resource batch keys typed.
   *
   * @param keyClass batch key class
   * @param <T> key value type
   * @return set of batch key values.
   */
  @Deprecated
  <T> Set<T> getBatchKeys(Class<T> keyClass);

  /**
   * Get the resource batch keys untyped.
   *
   * @return set of batch key values, or null if no batch key is set.
   */
  Set<?> getBatchIds();

  /**
   * Get the resource batch keys typed.
   *
   * @param keyClass batch key class
   * @param <T> key value type
   * @return set of batch key values, or null if no batch key is set.
   */
  <T> Set<T> getBatchIds(Class<T> keyClass);
}
