/*
   Copyright (c) 2022 LinkedIn Corp.

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
package com.linkedin.d2.balancer.properties;

import com.linkedin.util.ArgumentUtil;
import java.util.Map;
import java.util.List;


import java.util.stream.Collectors;
import java.util.Collections;

/**
 * Configuration for a service's failout properties. These properties are used to control
 * the flow of traffic between datacenters.
 */
public class FailoutProperties
{
  private final List<Map<String, Object>> _failoutRedirectConfigs;
  private final List<Map<String, Object>> _failoutBucketConfigs;

  public FailoutProperties(List<Map<String, Object>> failoutRedirectConfigs,
      List<Map<String, Object>> failoutBucketConfigs)
  {
    ArgumentUtil.notNull(failoutBucketConfigs, "bucketConfigs");
    ArgumentUtil.notNull(failoutRedirectConfigs, "redirectConfigs");
    _failoutBucketConfigs = failoutBucketConfigs;
    _failoutRedirectConfigs = failoutRedirectConfigs;
  }

  public List<Map<String, Object>> getFailoutRedirectConfigs()
  {
    return _failoutRedirectConfigs;
  }

  public List<Map<String, Object>> getFailoutBucketConfigs()
  {
    return _failoutBucketConfigs;
  }

  @Override
  public String toString()
  {
    return "FailoutProperties [_failoutRedirectConfigs=" + _failoutRedirectConfigs
        + ", _failoutBucketConfigs=" + _failoutBucketConfigs
        + "]";
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + _failoutRedirectConfigs.hashCode();
    result = prime * result + _failoutBucketConfigs.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;

    FailoutProperties other = (FailoutProperties) obj;
    return _failoutRedirectConfigs.equals(other.getFailoutRedirectConfigs()) &&
          _failoutBucketConfigs.equals(other.getFailoutBucketConfigs());
  }
}
