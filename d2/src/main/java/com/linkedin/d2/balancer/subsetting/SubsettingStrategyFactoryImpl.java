/*
   Copyright (c) 2021 LinkedIn Corp.

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

package com.linkedin.d2.balancer.subsetting;

import com.linkedin.d2.balancer.properties.ServiceProperties;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class SubsettingStrategyFactoryImpl implements SubsettingStrategyFactory
{
  private final Map<String, Map<Integer, SubsettingStrategy<URI>>> _subsettingStrategyMap;
  private final DeterministicSubsettingMetadataProvider _deterministicSubsettingMetadataProvider;

  public SubsettingStrategyFactoryImpl(DeterministicSubsettingMetadataProvider deterministicSubsettingMetadataProvider)
  {
    _subsettingStrategyMap = new ConcurrentHashMap<>();
    _deterministicSubsettingMetadataProvider = deterministicSubsettingMetadataProvider;
  }

  @Override
  public SubsettingStrategy<URI> get(String serviceName, ServiceProperties serviceProperties, int partitionId)
  {
    int minClusterSubsetSize = serviceProperties.getMinClusterSubsetSize();

    if (minClusterSubsetSize > 0)
    {
      if (_subsettingStrategyMap.containsKey(serviceName))
      {
        Map<Integer, SubsettingStrategy<URI>> strategyMap = _subsettingStrategyMap.get(serviceName);
        if (strategyMap.containsKey(partitionId))
        {
          return strategyMap.get(partitionId);
        }
        else
        {
          strategyMap.put(partitionId, new DeterministicSubsettingStrategy<>(_deterministicSubsettingMetadataProvider, serviceName, minClusterSubsetSize));
        }
      }
      else
      {
        Map<Integer, SubsettingStrategy<URI>> strategyMap = new ConcurrentHashMap<>();
        strategyMap.put(partitionId, new DeterministicSubsettingStrategy<>(_deterministicSubsettingMetadataProvider, serviceName, minClusterSubsetSize));
        _subsettingStrategyMap.put(serviceName, strategyMap);
      }
    }

    return null;
  }
}
