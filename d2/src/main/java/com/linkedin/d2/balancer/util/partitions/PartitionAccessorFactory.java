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

package com.linkedin.d2.balancer.util.partitions;

import com.linkedin.d2.balancer.properties.CustomizedPartitionProperties;
import com.linkedin.d2.balancer.properties.HashBasedPartitionProperties;
import com.linkedin.d2.balancer.properties.PartitionProperties;
import com.linkedin.d2.balancer.properties.RangeBasedPartitionProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the factory to create {@link PartitionAccessor} for different partition properites
 *
 */
public class PartitionAccessorFactory
{
  private final static Logger _log = LoggerFactory.getLogger(PartitionAccessorFactory.class);
  public static PartitionAccessor getPartitionAccessor(String clusterName,
                                                       PartitionAccessorRegistry availablePartitionAccessorRegistry,
                                                       PartitionProperties properties)
  {
    switch(properties.getPartitionType())
    {
      case RANGE:
        return new RangeBasedPartitionAccessor((RangeBasedPartitionProperties)properties);
      case HASH:
        return new HashBasedPartitionAccessor((HashBasedPartitionProperties)properties);
      case CUSTOM:
        return buildCustomizedPartitionAccessor(clusterName, availablePartitionAccessorRegistry, (CustomizedPartitionProperties) properties);
      case NONE:
        return DefaultPartitionAccessor.getInstance();
      default:
        break;
    }

    throw new IllegalArgumentException("Unsupported partition properties type.");
  }

  /**
   * Create {@Link CustomizedPartitionAccessor}
   *
   * There are several factors that can affect which PartitionAccessor is ultimately generated:
   *
   * 1. If there is no accessor registered, simply use the DefaultPartitionAccessor.

   * 2. If there is no ClassList specified for the given cluster, the first registered accessor will be used.
   *    This is the most common use case, where there should be one PartitionAccessor registered and used.
   *    There is no reason for the client to register more than one (and if it happens, only the first one used).

   * 3. For the purpose of updating/upgrading the accessor, the cluster can provide a ClassList config, which specifies
   *    a prioritized list of PartitionAccessor class names. The first accessor from the list that is registered will be
   *    used.
   *    The primary reason of this config is for the cluster/service to synchronously control which PartitionAccessor to
   *    use, especially during updating phase. Therefore it should be a complete list. If no accessor fall in the list,
   *    the DefaultPartitionAccessor is used, regardless if there is any accessor registered or not.
   *
   *
   * @param clusterName
   * @param registry
   * @param customizedProperties
   * @return Generated PartitionAccessor
   */
  private static PartitionAccessor buildCustomizedPartitionAccessor(String clusterName,
                                                                    PartitionAccessorRegistry registry,
                                                                    CustomizedPartitionProperties customizedProperties)
  {
    List<BasePartitionAccessor> partitionAccessors = registry.getPartitionAccessors(clusterName);

    if (partitionAccessors == null || partitionAccessors.isEmpty())
    {
      // if no partitionAccessor registered for the cluster, simply use the default accessor
      // This can happen when the customized accessor implementation library has not been deployed to the client
      _log.error("No Customized PartitionAccessor defined for cluster " + clusterName + ", fall back to defaultPartitionAccessor");
      return DefaultPartitionAccessor.getInstance();
    }

    List<String> requestedClassList = customizedProperties.getPartitionAccessorList();
    if (requestedClassList == null || requestedClassList.isEmpty())
    {
      // If the no classList is defined, use the first class registered
      BasePartitionAccessor partitionAccessor = partitionAccessors.get(0);
      _log.info("Use customized partitionAccessor for cluster:" + clusterName + ", class: " + partitionAccessor.getClass().getSimpleName()
          + " (out of " + partitionAccessors.size() + ") registration");
      return new CustomizedPartitionAccessor(customizedProperties, partitionAccessor);
    }
    for (String className : requestedClassList)
    {
      for (BasePartitionAccessor accessor : partitionAccessors)
      {
        if (className.equals(accessor.getClass().getSimpleName()))
        {
          _log.info("Use matched partitionAccessor for cluster: " + clusterName + ", class: " + accessor.getClass().getSimpleName());
          return new CustomizedPartitionAccessor(customizedProperties, accessor);
        }
      }
    }
    // fall back to default partition accessor if the customized accessor is not available.
    _log.error("None of the registered PartitionAccessor matches PartitionAccessorList defined for cluster " + clusterName
      + ", fall back to defaultPartitionAccessor");
    return DefaultPartitionAccessor.getInstance();
  }
}
