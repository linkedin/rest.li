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

package com.linkedin.d2.balancer.zkfs;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author Ang Xu
 */
public class ZKFSUtilTest
{
  @DataProvider(name = "clusterPaths")
  public Object[][] createClusterPaths() {
    return new Object[][] {
        {"/d2", "/d2/clusters"},
        {"/d2/", "/d2/clusters"},
        {"/d2//", "/d2/clusters"},
        {"/", "/clusters"},
        {"", "/clusters"}
    };
  }

  @Test (dataProvider = "clusterPaths")
  public void testZKFSUtilClusterPath(String basePath, String clusterPath)
  {
    Assert.assertEquals(ZKFSUtil.clusterPath(basePath), clusterPath);
  }

  @DataProvider(name = "servicePaths")
  public Object[][] createServicePaths() {
    return new Object[][]{
        {"/d2", null, "/d2/services"},
        {"/d2/", null, "/d2/services"},
        {"/d2//", null, "/d2/services"},
        {"/", null, "/services"},
        {"", null, "/services"},
        {"", "test", "/test"},
        // empty servicePath values should use the default path
        {"", "", "/services"},
        {"/d2", "", "/d2/services"},
        {"/d2/", "", "/d2/services"}
    };
  }

  @Test(dataProvider = "servicePaths")
  public void testZKFSUtilServicePath(String basePath, String servicePath, String resultServicePath) {
    Assert.assertEquals(ZKFSUtil.servicePath(basePath, servicePath), resultServicePath);
  }
}
