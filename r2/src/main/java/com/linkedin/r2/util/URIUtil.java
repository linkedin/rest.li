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
package com.linkedin.r2.util;

import java.util.regex.Pattern;

import com.linkedin.util.ArgumentUtil;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class URIUtil
{
  private static final char PATH_SEP = '/';

  private static final Pattern PATH_SEP_PATTERN = Pattern.compile(Pattern.quote(String.valueOf(PATH_SEP)));

  /**
   * Returns an array of each path segment, using the definition as defined in RFC 2396.
   * If present, a leading path separator is removed.
   *
   * @param path the path to tokenize
   * @return an array of the path segments in the given path
   */
  public static String[] tokenizePath(String path)
  {
    ArgumentUtil.notNull(path, "uri");
    if (!path.isEmpty() && path.charAt(0) == PATH_SEP)
    {
      path = path.substring(1);
    }
    return PATH_SEP_PATTERN.split(path);
  }
}
