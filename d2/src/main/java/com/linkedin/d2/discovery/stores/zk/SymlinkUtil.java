/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.d2.discovery.stores.zk;

/**
 * @author Ang Xu
 * @version $Revision: $
 */
public class SymlinkUtil
{
  public static final char SYMLINK_PREFIX = '$';

  /**
   * return the index to split the first symlink out of {@param path}.
   *
   * If no symlink is found, -1 is returned.
   *
   * Examples:
   *  firstSymlinkIndex("/$symlink/foo/bar") returns 9
   *  firstSymlinkIndex("/foo/bar/$symlink") returns 17
   *  firstSymlinkIndex("/notASymlink") returns -1
   *
   * @param path
   */
  public static int firstSymlinkIndex(String path)
  {
    int fromIndex = 0;
    int index;

    while ((index = path.indexOf('/', fromIndex)) >= 0)
    {
      fromIndex = index + 1;
      if (fromIndex < path.length() && path.charAt(fromIndex) == SYMLINK_PREFIX)
      {
        if (path.indexOf('/', fromIndex) != -1)
          return path.indexOf('/', fromIndex);
        else
          return path.length();
      }
    }
    return -1;
  }

  /**
   * check if a given path contains symlink or not.
   *
   * @param path
   */
  public static boolean containsSymlink(String path)
  {
    return (firstSymlinkIndex(path) < 0) ? false : true;
  }
}
