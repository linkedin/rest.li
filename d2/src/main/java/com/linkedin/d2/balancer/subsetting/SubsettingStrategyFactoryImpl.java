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
