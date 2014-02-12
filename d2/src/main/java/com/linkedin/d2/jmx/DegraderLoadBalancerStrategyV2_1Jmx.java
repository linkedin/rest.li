package com.linkedin.d2.jmx;


import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyV2;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyV2_1;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DegraderLoadBalancerStrategyV2_1Jmx implements DegraderLoadBalancerStrategyV2_1JmxMBean
{
  private final DegraderLoadBalancerStrategyV2_1 _strategy;

  public DegraderLoadBalancerStrategyV2_1Jmx(DegraderLoadBalancerStrategyV2_1 strategy)
  {
    _strategy = strategy;
  }

  @Override
  public double getOverrideClusterDropRate()
  {
    @SuppressWarnings("deprecation")
    double rate = _strategy.getCurrentOverrideDropRate();
    return rate;
  }

  @Override
  public String toString()
  {
    return "DegraderLoadBalancerStrategyV2_1Jmx [_strategy=" + _strategy + "]";
  }

  @Override
  public int getTotalPointsInHashRing()
  {
    Map<URI, Integer> uris = _strategy.getState().getPointsMap();
    int total = 0;
    for (Map.Entry<URI, Integer> entry : uris.entrySet())
    {
      total += entry.getValue();
    }
    return total;
  }

  @Override
  public String getPointsMap()
  {
    return _strategy.getState().getPointsMap().toString();
  }

  @Override
  public String getUnhealthyClientsPoints()
  {
    int pointsPerWeight = _strategy.getConfig().getPointsPerWeight();
    List<String> result = new ArrayList<String>();
    for (Map.Entry<URI, Integer> entry : _strategy.getState().getPointsMap().entrySet())
    {
      if (entry.getValue() < pointsPerWeight)
      {
        result.add(entry.getKey().toString() + ":" + entry.getValue() + "/" + pointsPerWeight);
      }
    }
    return result.toString();
  }
}
