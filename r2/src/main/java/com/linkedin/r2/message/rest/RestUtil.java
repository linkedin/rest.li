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
package com.linkedin.r2.message.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class RestUtil
{

  private static final Pattern COMMA_PATTERN = Pattern.compile(Pattern.quote(","));

  /**
   * Splits the given header value into a list of values, which are returned. The header is split
   * per the specification in RFC-2616, section 4.2.
   *
   * @param headerValue the header value to split
   *
   * @return the list of values from the header value
   */
  public static List<String> getHeaderValues(String headerValue)
  {
    final String[] elems = COMMA_PATTERN.split(headerValue);
    final List<String> values = new ArrayList<String>();

    // Per RFC 2616, section 2.1, a null list element should not be treated as a value.
    for (String elem : elems)
    {
      elem = elem.trim();
      if (!elem.isEmpty())
      {
        values.add(elem);
      }
    }

    return values;
  }
}
