/*
   Copyright (c) 2017 LinkedIn Corp.

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


package com.linkedin.d2.balancer.util.partitions;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * PartitionAccessorRegistry keeps track of the BasePartitonAccessor implementation for customized partition
 */

public class PartitionAccessorRegistryImpl implements PartitionAccessorRegistry
{
  private static final Logger _log = LoggerFactory.getLogger(PartitionAccessorRegistryImpl.class.getName());

  private final Map<String, List<BasePartitionAccessor>> _partitionAccessors = new ConcurrentHashMap<>();

  @Override
  public void register(String clusterName, BasePartitionAccessor accessor)
  {
    List<BasePartitionAccessor> accessors = _partitionAccessors.computeIfAbsent(clusterName,
      k -> Collections.synchronizedList(new ArrayList<>()));
    accessors.add(accessor);
    _log.info("Register partitionAccessor for cluster: {} class: {} (total {})",
        new Object[]{ clusterName, accessor.getClass().getSimpleName(), accessors.size() });
  }

  @Override
  public List<BasePartitionAccessor> getPartitionAccessors(String clusterName)
  {
    return _partitionAccessors.get(clusterName);
  }
}
