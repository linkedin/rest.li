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

package com.linkedin.data.transform;

import com.linkedin.data.schema.PathSpec;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class Escaper
{

  /**
   * This method replaces all occurrences of "$" with "$$" and if passed
   * parameter is equal by identity i.e. <code>segment == PathSpec.WILDCARD</code>,
   * then return value is "$*".
   *
   * @param segment the segment to be escaped
   * @return the segment, but escaped
   */
  public static String escapePathSegment(String segment)
  {
    //identity comparison - wildcard must be specific instance in PathSpec
    if (segment == PathSpec.WILDCARD)
    {
      return "$*";
    }

    return escape(segment);
  }

  /**
   * This method replaces all occurrences of "$$" with "$" and if passed
   * parameter is equal to "$*", then <code>PathSpec.WILDCARD</code> is returned.
   *
   * @param segment the segment to be unescaped
   * @return the segment, but unescaped
   */
  public static String unescapePathSegment(String segment)
  {
    if (segment.equals("$*"))
    {
      return PathSpec.WILDCARD;
    }

    return unescape(segment);
  }

  /**
   * Returns input string, in which all "$" occurrences are replaced with "$$".
   * This method runs much faster that {@link String#replaceAll(String, String)}.
   * This method is equivalent to <code>s.replaceAll("$", "$$")</code>
   *
   * @param s a String
   * @return s, but escaped
   */
  public static String escape(String s)
  {
    return replaceAll(s, "$", "$$");
  }

  /**
   * Returns input string, in which all "$$" occurrences are replaced with "$".
   * This method runs much faster that {@link String#replaceAll(String, String)}.
   * This method is equivalent to <code>s.replaceAll("$$", "$")</code>
   *
   * @param s a String
   * @return s, but unescaped
   */
  public static String unescape(String s)
  {
    return replaceAll(s, "$$", "$");
  }

  /**
   * Returns input string, in which all occurrences of <code>what</code> are replaced with
   * <code>withWhat</code>.
   * This method runs much faster that {@link String#replaceAll(String, String)}.
   * This method is equivalent to <code>s.replaceAll(what, withWhat)</code>
   *
   * @param s a String
   * @param what a String, replaced with withWhat in s
   * @param withWhat a String, replaces what in s
   * @return s, but with all instances of what replaced with withWhat
   */
  public static String replaceAll(String s, final String what, final String withWhat)
  {
    StringBuilder sb = null;
    int fromIndex = 0;     //index of last found '$' char + 1
    boolean found = false;
    int length = s.length();
    do
    {
      final int index = s.indexOf(what, fromIndex);
      found = index >= 0;
      if (found)
      {
        if (sb == null)
          sb = new StringBuilder(s.length() * 2);

        sb.append(s, fromIndex, index);
        sb.append(withWhat);

        fromIndex = index + what.length();
      }
    } while (found && fromIndex < length);

    if (sb != null)
    {

      if (fromIndex < length)
        sb.append(s, fromIndex, length);

      return sb.toString();
    } else
    {
      return s;
    }
  }
}
