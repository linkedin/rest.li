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

/* $Id$ */
package com.linkedin.r2.transport.common;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;


/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class WireAttributeHelper
{
  private static final String WIRE_ATTR_PREFIX = "X-LI-R2-W-";

  /**
   * Creates a new instance of wire attributes implementation.
   * @return A new instance of wire attributes
   */
  public static Map<String, String> newWireAttributes()
  {
    return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  }

  /**
   * Removes the wire attributes from the specified map of message attributes (headers)
   * and returns a new instance of case insensitive map of wire attributes with prefix removed.
   *
   * @param map the map containing wire attributes to be removed.
   * @return a new instance of case insensitive map of the wire attributes from the input map,
   *         with any key prefixes removed.
   */
  public static Map<String, String> removeWireAttributes(Map<String, String> map)
  {
    final Map<String, String> wireAttrs = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    for (Iterator<Map.Entry<String, String>> it = map.entrySet().iterator(); it.hasNext();)
    {
      final Map.Entry<String, String> entry = it.next();
      final String key = entry.getKey();
      if (key.regionMatches(true, 0, WIRE_ATTR_PREFIX, 0, WIRE_ATTR_PREFIX.length()))
      {
        final String value = entry.getValue();
        final String newKey = key.substring(WIRE_ATTR_PREFIX.length());
        wireAttrs.put(newKey, value);
        it.remove();
      }
    }

    return wireAttrs;
  }

  /**
   * Convert the specified map of wire attributes to a new instance of case insensitive map of wire
   * attributes of message attribute format (by adding a namespace prefix).
   *
   * @param attrs wire attributes to be converted.
   * @return a new instance case insensitive map of message attributes constructed from specified
   *         wire attributes.
   */
  public static Map<String, String> toWireAttributes(Map<String, String> attrs)
  {
    final Map<String, String> wireAttrs = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    for (Map.Entry<String, String> entry : attrs.entrySet())
    {
      final String key = WIRE_ATTR_PREFIX + entry.getKey();
      final String value = entry.getValue();
      wireAttrs.put(key, value);
    }

    return wireAttrs;
  }
}
