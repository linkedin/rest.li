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

/**
 * $Id: $
 */

package com.linkedin.common.util;

import java.util.Map;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class MapUtil
{
  /**
   * @param defaultValue
   *          is returned when the value of the {@code key} is null in the {@code map}.
   *          can be null
   * @return returned type is always {@code valueClass}
   */
  @SuppressWarnings("unchecked")
  public static <K, V> V getWithDefault(final Map<K, Object> map,
                                        final K key,
                                        final V defaultValue,
                                        final Class<V> valueClass)
  {
    final Object actualValue = map.get(key);
    if (actualValue == null)
    {
      return defaultValue;
    }

    if (valueClass.isAssignableFrom(actualValue.getClass()))
    {
      return (V) actualValue;
    }

    if (!(actualValue instanceof String))
    {
      throw new IllegalArgumentException("Unsupported actual value type: "
          + actualValue.getClass());
    }

    final String valStr = (String) actualValue;
    if (valueClass.equals(Double.class))
    {
      return (V) Double.valueOf(Double.parseDouble(valStr));
    }
    if (valueClass.equals(Float.class))
    {
      return (V) Float.valueOf(Float.parseFloat(valStr));
    }
    if (valueClass.equals(Long.class))
    {
      return (V) Long.valueOf(Long.parseLong(valStr));
    }
    if (valueClass.equals(Integer.class))
    {
      return (V) Integer.valueOf(Integer.parseInt(valStr));
    }
    if (valueClass.equals(Boolean.class))
    {
      return (V) Boolean.valueOf(Boolean.parseBoolean(valStr));
    }

    throw new IllegalArgumentException("Unsupported expected value type: " + valueClass);
  }

  /**
   * @param defaultValue
   *          is returned when the value of the {@code key} is null in the {@code map}.
   *          cannot be null
   * @return returned type is always the same as the type of {@code defaultValue}
   */
  @SuppressWarnings("unchecked")
  public static <K, V> V getWithDefault(final Map<K, Object> map,
                                        final K key,
                                        final V defaultValue)
  {
    if (defaultValue == null)
    {
      throw new IllegalArgumentException();
    }

    return getWithDefault(map, key, defaultValue, (Class<V>) defaultValue.getClass());
  }

  private MapUtil()
  {
  }
}
