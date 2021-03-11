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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public class DeterministicSubsettingStrategyTest
{
  public static final double DELTA_DIFF = 1e-5;

  @Mock
  private DeterministicSubsettingMetadataProvider _deterministicSubsettingMetadataProvider;

  @Mock
  private LoadBalancerState _state;

  private DeterministicSubsettingStrategy<String> _deterministicSubsettingStrategy;

  private Map<String, Double> constructPointsMap(double[] weights)
  {
    Map<String, Double> pointsMap = new HashMap<>();
    int id = 0;

    for (double weight: weights)
    {
      pointsMap.put("host" + id, weight);
      id += 1;
    }
    return pointsMap;
  }

  @BeforeMethod
  public void setUp()
  {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testCaching()
  {
    Mockito.when(_deterministicSubsettingMetadataProvider.getSubsettingMetadata(_state))
        .thenReturn(new DeterministicSubsettingMetadata(0, 20));
    _deterministicSubsettingStrategy = new DeterministicSubsettingStrategy<>(_deterministicSubsettingMetadataProvider,
        "test", 10, _state);

    double[] weights = new double[100];
    Arrays.fill(weights, 1D);

    assertTrue(_deterministicSubsettingStrategy.isSubsetChanged(0));
    Map<String, Double> pointsMap = constructPointsMap(weights);
    Map<String, Double> weightedSubset1 = _deterministicSubsettingStrategy.getWeightedSubset(pointsMap, 0);

    assertFalse(_deterministicSubsettingStrategy.isSubsetChanged(0));
    pointsMap = constructPointsMap(weights);
    Map<String, Double> weightedSubset2 = _deterministicSubsettingStrategy.getWeightedSubset(pointsMap, 0);

    assertSame(weightedSubset1, weightedSubset2);

    assertTrue(_deterministicSubsettingStrategy.isSubsetChanged(1));
    pointsMap = constructPointsMap(weights);
    Map<String, Double> weightedSubset3 = _deterministicSubsettingStrategy.getWeightedSubset(pointsMap, 1);

    assertEquals(weightedSubset1, weightedSubset3);
    assertNotSame(weightedSubset1, weightedSubset3);

    assertFalse(_deterministicSubsettingStrategy.isSubsetChanged(1));
    pointsMap = constructPointsMap(weights);
    Map<String, Double> weightedSubset4 = _deterministicSubsettingStrategy.getWeightedSubset(pointsMap, 1);

    assertSame(weightedSubset3, weightedSubset4);

    Mockito.when(_deterministicSubsettingMetadataProvider.getSubsettingMetadata(_state))
        .thenReturn(new DeterministicSubsettingMetadata(0, 19));
    assertTrue(_deterministicSubsettingStrategy.isSubsetChanged(1));
  }

  @Test(dataProvider = "uniformWeightData")
  public void testDistributionWithUniformWeight(int clientNum, int hostNum, int minSubsetSize)
  {
    double[] weights = new double[hostNum];
    Arrays.fill(weights, 1D);
    Map<String, Double> pointsMap = constructPointsMap(weights);

    Map<String, Double> distributionMap = new HashMap<>();

    for (int i = 0; i < clientNum; i++)
    {
      Mockito.when(_deterministicSubsettingMetadataProvider.getSubsettingMetadata(_state))
          .thenReturn(new DeterministicSubsettingMetadata(i, clientNum));
      _deterministicSubsettingStrategy = new DeterministicSubsettingStrategy<>(_deterministicSubsettingMetadataProvider,
          "test", minSubsetSize, _state);
      Map<String, Double> weightedSubset = _deterministicSubsettingStrategy.getWeightedSubset(pointsMap, 0);
      assertTrue(weightedSubset.size() >= Math.min(minSubsetSize, hostNum));

      for (Map.Entry<String, Double> entry: weightedSubset.entrySet())
      {
        distributionMap.put(entry.getKey(), distributionMap.getOrDefault(entry.getKey(), 0D) + entry.getValue());
      }
    }

    double host0WeightSum = distributionMap.getOrDefault("host0", 0D);
    for (double weightSum: distributionMap.values())
    {
      assertEquals(weightSum, host0WeightSum, DELTA_DIFF);
    }
  }

  @Test(dataProvider = "differentWeightsData")
  public void testDistributionWithDifferentWeights(int clientNum, double[] weights, int minSubsetSize)
  {
    Map<String, Double> pointsMap = constructPointsMap(weights);
    Map<String, Double> distributionMap = new HashMap<>();
    double minSubsetWeight = minSubsetSize / (double) weights.length;
    double totalHostWeights = Arrays.stream(weights).sum();

    for (int i = 0; i < clientNum; i++)
    {
      Mockito.when(_deterministicSubsettingMetadataProvider.getSubsettingMetadata(_state))
          .thenReturn(new DeterministicSubsettingMetadata(i, clientNum));
      _deterministicSubsettingStrategy = new DeterministicSubsettingStrategy<>(_deterministicSubsettingMetadataProvider,
          "test", minSubsetSize, _state);
      Map<String, Double> weightedSubset = _deterministicSubsettingStrategy.getWeightedSubset(pointsMap, 0);
      double totalWeights = 0;
      for (Map.Entry<String, Double> entry: weightedSubset.entrySet())
      {
        String hostName = entry.getKey();
        double weight = entry.getValue();
        distributionMap.put(hostName,
            distributionMap.getOrDefault(hostName, 0D) + weight * pointsMap.get(hostName));
        totalWeights += weights[Integer.parseInt(hostName.substring("test".length()))] / totalHostWeights * weight;
      }
      assertTrue(totalWeights + DELTA_DIFF >= Math.min(minSubsetWeight, 1D));
    }

    double totalWeights = distributionMap.values().stream().mapToDouble(Double::doubleValue).sum();
    for (Map.Entry<String, Double> entry: distributionMap.entrySet())
    {
      String hostName = entry.getKey();
      double hostWeight = weights[Integer.parseInt(hostName.substring("test".length()))];
      assertEquals(entry.getValue() / totalWeights, hostWeight / totalHostWeights, DELTA_DIFF);
    }
  }

  @DataProvider
  public Object[][] uniformWeightData()
  {
    return new Object[][]
        {
            {5, 0, 10},
            {1, 1, 10},
            {1, 5, 10},
            {5, 1, 10},
            {5, 5, 10},
            {5, 5, 1},
            {3, 6, 2},
            {5, 40, 10},
            {10, 100, 10},
            {7, 47, 13},
            {47, 40, 13},
            {13, 200, 11},
            {83, 359, 23}
        };
  }

  private static double[] generateRandomWeights(int size)
  {
    return new Random().doubles(size, 0D, 1D).toArray();
  }

  @DataProvider
  public Object[][] differentWeightsData()
  {
    return new Object[][]
        {
            {1, new double[]{1.0}, 10},
            {1, new double[]{1.0, 1.0, 1.0, 1.0, 1.0}, 10},
            {1, new double[]{1.0, 0.0, 0.0, 0.0, 0.0}, 10},
            {5, new double[]{1.0, 1.0, 0.0, 0.0, 0.0}, 10},
            {10, generateRandomWeights(100), 10},
            {7, generateRandomWeights(40), 13},
            {47, generateRandomWeights(40), 13},
            {13, generateRandomWeights(200), 11},
            {83, generateRandomWeights(359), 23}
        };
  }
}
