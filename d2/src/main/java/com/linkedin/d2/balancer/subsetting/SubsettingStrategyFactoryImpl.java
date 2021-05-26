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

import com.linkedin.d2.balancer.LoadBalancerState;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class SubsettingStrategyFactoryImpl implements SubsettingStrategyFactory
{
  private final ConcurrentMap<String, ConcurrentMap<Integer, SubsettingStrategy<URI>>> _subsettingStrategyMap;
  private final ConcurrentMap<String, Integer> _minClusterSubsetSizeMap;
  private final DeterministicSubsettingMetadataProvider _deterministicSubsettingMetadataProvider;
  private final LoadBalancerState _state;

  public SubsettingStrategyFactoryImpl(DeterministicSubsettingMetadataProvider deterministicSubsettingMetadataProvider,
      LoadBalancerState state)
  {
    _subsettingStrategyMap = new ConcurrentHashMap<>();
    _minClusterSubsetSizeMap = new ConcurrentHashMap<>();
    _deterministicSubsettingMetadataProvider = deterministicSubsettingMetadataProvider;
    _state = state;
  }

  @Override
  public SubsettingStrategy<URI> get(String serviceName, int minClusterSubsetSize, int partitionId)
  {
    if (minClusterSubsetSize <= 0)
    {
      return null;
    }

    if (_subsettingStrategyMap.containsKey(serviceName))
    {
      Map<Integer, SubsettingStrategy<URI>> strategyMap = _subsettingStrategyMap.get(serviceName);
      if (minClusterSubsetSize == _minClusterSubsetSizeMap.get(serviceName) && strategyMap.containsKey(partitionId))
      {
        return strategyMap.get(partitionId);
      }

      strategyMap.computeIfAbsent(partitionId, id -> new DeterministicSubsettingStrategy<>(
          _deterministicSubsettingMetadataProvider, serviceName, minClusterSubsetSize, _state));
    }
    else
    {
      _subsettingStrategyMap.computeIfAbsent(serviceName, name ->
      {
        ConcurrentMap<Integer, SubsettingStrategy<URI>> strategyMap = new ConcurrentHashMap<>();
        strategyMap.put(partitionId, new DeterministicSubsettingStrategy<>(
            _deterministicSubsettingMetadataProvider, serviceName, minClusterSubsetSize, _state));
        return strategyMap;
      });
    }
    _minClusterSubsetSizeMap.put(serviceName, minClusterSubsetSize);

    return _subsettingStrategyMap.get(serviceName).get(partitionId);
  }
}
