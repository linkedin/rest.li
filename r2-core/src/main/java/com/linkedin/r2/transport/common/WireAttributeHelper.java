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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class WireAttributeHelper
{
  private static final String WIRE_ATTR_PREFIX = "X-LI-R2-W-";

  /**
   * Remove the wire attributes from the specified map of message attributes (headers).
   *
   * @param map the map containing wire attributes to be removed.
   * @return the wire attributes from the input map, with any key prefixes removed.
   */
  public static Map<String, String> removeWireAttributes(Map<String, String> map)
  {
    final Map<String, String> wireAttrs = new HashMap<String, String>();

    for (Iterator<Map.Entry<String, String>> it = map.entrySet().iterator(); it.hasNext();)
    {
      final Map.Entry<String, String> entry = it.next();
      final String key = entry.getKey();
      if (key.startsWith(WIRE_ATTR_PREFIX))
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
   * Convert the specified map of wire attributes to message attribute format (by adding a
   * namespacing prefix).
   *
   * @param attrs wire attributes to be converted.
   * @return map of message attributes constructed from specified wire attributes.
   */
  public static Map<String, String> toWireAttributes(Map<String, String> attrs)
  {
    final Map<String, String> wireAttrs = new HashMap<String, String>();

    for (Map.Entry<String, String> entry : attrs.entrySet())
    {
      final String key = WIRE_ATTR_PREFIX + entry.getKey();
      final String value = entry.getValue();
      wireAttrs.put(key, value);
    }

    return wireAttrs;
  }
}
