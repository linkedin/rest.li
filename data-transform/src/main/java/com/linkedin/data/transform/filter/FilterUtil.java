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
 * $id$
 */
package com.linkedin.data.transform.filter;

import com.linkedin.data.DataMap;

/**
 * This class contains constants and helper methods used in data filtering.
 *
 * @author jodzga
 *
 */
public class FilterUtil
{

  /**
   * Returns Integer value from {@link DataMap} for given key. If DataMap does not contains value associated with given key,
   * then defaultValue is returned. If value associated with given key in DataMap is not if type Integer, then
   * null is returned.
   *
   * @param data the DataMap
   * @param key the key
   * @param defaultValue the value to return if key is not in the DataMap
   * @return Integer value for a given key or default value or null if value
   * is of incorrect type
   */
  public static Integer getIntegerWithDefaultValue(final DataMap data, final String key, Integer defaultValue)
  {
    final Object o = data.get(key);
    if (o != null)
    {
      if (o instanceof Integer)
        return (Integer)o;
      else
        return null;
    } else
      return defaultValue;
  }

  /**
   * This method answers the question if given mask has all fields
   * marked with positive mask as if it were merged with mask = 1.
   * <p>
   * Mask was merged with 1 if any of the following is true:
   * <li>mask equals <code>Integer(1)</code></li>
   * <li>mask if of type <code>DataMap</code> and contains wildcard,
   * which is marked as merged with 1 (recursion)</code></li>
   *
   * @param mask the mask to check
   * @return true if mask was merged with 1
   */
  public static boolean isMarkedAsMergedWith1(Object mask)
  {
    return (mask != null && (mask.equals(FilterConstants.POSITIVE) ||
        ((mask.getClass() == DataMap.class) && isMarkedAsMergedWith1(((DataMap) mask).get(FilterConstants.WILDCARD)))));
  }


}
