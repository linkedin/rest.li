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

package com.linkedin.d2.balancer.config;

import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.util.PropertyUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a helper class to convert the user config to the internal data structure D2 uses.
 * The user config is a plain map, and we need to convert it first before
 * D2 announcer can use it in announcing
 */
public class PartitionDataFactory
{
  public static Map<Integer, PartitionData> createPartitionDataMap(Map<String, Object> sourceMap)
  {
    Map<Integer, PartitionData> map = new HashMap<Integer, PartitionData>();
    if (sourceMap != null)
    {
      for (Map.Entry<String, Object> entry : sourceMap.entrySet())
      {
        @SuppressWarnings("unchecked")
        Map<String, Object> partitionDataMap = (Map<String, Object>) entry.getValue();
        String weightStr = PropertyUtil.checkAndGetValue(partitionDataMap, "weight", String.class, "URI weight");
        PartitionData data = new PartitionData(PropertyUtil.parseDouble("weight", weightStr));
        map.put(PropertyUtil.parseInt("partitionId", entry.getKey()), data);
      }
    }
    return map;
  }
}
