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

import junit.framework.Assert;
import org.testng.annotations.Test;

/**
 * @author Ang Xu
 * @version $Revision: $
 */
public class SymlinkUtilTest
{
  private final String path1 = "/$symlink";
  private final String path2 = "/foo/bar/$symlink";
  private final String path3 = "/$symlink/foo/bar";
  private final String path4 = "/foo/bar";
  private final String path5 = "/";

  @Test
  public void testContainsSymlink()
  {
    Assert.assertTrue(SymlinkUtil.containsSymlink(path1));
    Assert.assertTrue(SymlinkUtil.containsSymlink(path2));
    Assert.assertTrue(SymlinkUtil.containsSymlink(path3));
    Assert.assertFalse(SymlinkUtil.containsSymlink(path4));
    Assert.assertFalse(SymlinkUtil.containsSymlink(path5));
  }

  @Test
  public void testFirstSymlinkIndex()
  {
    Assert.assertEquals(SymlinkUtil.firstSymlinkIndex(path1), path1.length());
    Assert.assertEquals(SymlinkUtil.firstSymlinkIndex(path2), path2.length());
    Assert.assertEquals(SymlinkUtil.firstSymlinkIndex(path3), path1.length());
    Assert.assertEquals(SymlinkUtil.firstSymlinkIndex(path4), -1);
    Assert.assertEquals(SymlinkUtil.firstSymlinkIndex(path5), -1);
  }
}
