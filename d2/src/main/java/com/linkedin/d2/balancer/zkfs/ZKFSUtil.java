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

package com.linkedin.d2.balancer.zkfs;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class ZKFSUtil
{
  public static final String SERVICE_PATH = "services";
  public static final String CLUSTER_PATH = "clusters";
  public static final String URI_PATH = "uris";

  private ZKFSUtil()
  {
  }

  private static String normalizeBasePath(String basePath)
  {
    String normalized = basePath;
    while (normalized.endsWith("/"))
    {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }

  public static String servicePath(String basePath)
  {
    return String.format("%s/%s", normalizeBasePath(basePath), SERVICE_PATH);
  }

  public static String servicePath(String basePath, String servicePath)
  {
    return String.format("%s/%s", normalizeBasePath(basePath), servicePath);
  }

  public static String clusterPath(String basePath)
  {
    return String.format("%s/%s", normalizeBasePath(basePath), CLUSTER_PATH);
  }

  public static String uriPath(String basePath)
  {
    return String.format("%s/%s", normalizeBasePath(basePath), URI_PATH);
  }
}
