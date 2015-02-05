/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.data.template;


/**
 * Coercer keys from primary to alternative formats and vice versa.
 *
 * @author Moira Tagle
 * @param <T> alternativeKey type
 * @param <K> primary key type
 */
public interface KeyCoercer<T, K>
{
  /**
   * Coerce an alternative key to a primary key.
   *
   * @param object the alternative key.
   * @return the key in primary key format.
   * @throws InvalidAlternativeKeyException
   */
  K coerceToKey(T object) throws InvalidAlternativeKeyException;

  /**
   * Coerce a primary key to an alternative key.
   *
   * @param object the primary key.
   * @return the key in alternative key format.
   */
  T coerceFromKey(K object);
}
