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

package com.linkedin.d2.balancer.util.canary;

import com.linkedin.d2.D2CanaryDistributionStrategy;
import com.linkedin.d2.PercentageStrategyProperties;
import com.linkedin.d2.StrategyType;
import com.linkedin.d2.TargetApplicationsStrategyProperties;
import com.linkedin.d2.TargetHostsStrategyProperties;
import com.linkedin.data.template.StringArray;
import java.util.Arrays;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.lang.Math.abs;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;


/**
 * Test behavior of {@link BasicCanaryDistributionProviderImpl}
 */
public class TestBasicCanaryDistributionProviderImpl
{
  private static final class CanaryDistributionProviderImplFixture
  {
    CanaryDistributionProviderImplFixture()
    {
    }

    BasicCanaryDistributionProviderImpl getSpiedImpl() {
      return getSpiedImpl("dummyService", "dummyHost", 0);
    }

    BasicCanaryDistributionProviderImpl getSpiedImpl(int hashResult) {
      return getSpiedImpl("dummyService", "dummyHost", hashResult);
    }

    BasicCanaryDistributionProviderImpl getSpiedImpl(String serviceName, String hostName) {
      return getSpiedImpl(serviceName, hostName, 0);
    }

    BasicCanaryDistributionProviderImpl getSpiedImpl(String serviceName, String hostName, int hashResult) {
      BasicCanaryDistributionProviderImpl impl = spy(new BasicCanaryDistributionProviderImpl(serviceName, hostName));
      when(impl.getHashResult()).thenReturn(hashResult);
      return impl;
    }
  }

  /**
   * Provide objects for testing percentage strategy normal cases
   * @return a list of objects with structure: {
   *  D2 canary distribution strategy,
   *  An integer for mocking hashing result,
   *  Expected canary distribution result
   * }
   */
  @DataProvider(name = "getNormalCasesForPercentageStrategy")
  public Object[][] getNormalCasesForPercentageStrategy()
  {
    PercentageStrategyProperties percentageProperties = new PercentageStrategyProperties().setScope(0.27);
    D2CanaryDistributionStrategy percentageStrategy =
        new D2CanaryDistributionStrategy().setStrategy(StrategyType.PERCENTAGE).setPercentageStrategyProperties(percentageProperties);

    return new Object[][]{
        {percentageStrategy,
            0,  // 0 falls into scope(0.27 => 27)
            CanaryDistributionProvider.Distribution.CANARY},
        {percentageStrategy,
            6, // 6 falls into scope
            CanaryDistributionProvider.Distribution.CANARY},
        {percentageStrategy,
            27, // 27 falls into scope
            CanaryDistributionProvider.Distribution.CANARY},
        {percentageStrategy,
            111,  // 111 % 100 = 11, falls into scope
            CanaryDistributionProvider.Distribution.CANARY},
        {percentageStrategy,
            30, // 30 is out of scope
            CanaryDistributionProvider.Distribution.STABLE},
        {new D2CanaryDistributionStrategy().setStrategy(StrategyType.PERCENTAGE)
            .setPercentageStrategyProperties(new PercentageStrategyProperties().setScope(0)), // scope 0 means no canary
            0,
            CanaryDistributionProvider.Distribution.STABLE}};
  }

  @Test(dataProvider = "getNormalCasesForPercentageStrategy")
  public void testNormalCasesForPercentageStrategy(D2CanaryDistributionStrategy strategy, int hashResult,
      CanaryDistributionProvider.Distribution expected)
  {
    CanaryDistributionProviderImplFixture fixture = new CanaryDistributionProviderImplFixture();
    Assert.assertEquals(fixture.getSpiedImpl(hashResult).distribute(strategy), expected,
        "Testing percentage strategy: " + strategy + ", with hash result: " + hashResult + ", should return: "
            + expected.name());
  }

  @Test
  public void testNormalCasesForHostsStrategy()
  {
    TargetHostsStrategyProperties hostsProperties =
        new TargetHostsStrategyProperties().setTargetHosts(new StringArray(Arrays.asList("hostA", "hostB")));
    D2CanaryDistributionStrategy targetHostsStrategy =
        new D2CanaryDistributionStrategy().setStrategy(StrategyType.TARGET_HOSTS)
            .setTargetHostsStrategyProperties(hostsProperties);

    CanaryDistributionProviderImplFixture fixture = new CanaryDistributionProviderImplFixture();

    Assert.assertEquals(fixture.getSpiedImpl(null, "hostA").distribute(targetHostsStrategy), // in target list
        CanaryDistributionProvider.Distribution.CANARY, "Host in target list should return canary.");

    Assert.assertEquals(fixture.getSpiedImpl(null, "hostC").distribute(targetHostsStrategy), // NOT in target list
        CanaryDistributionProvider.Distribution.STABLE, "Host not in target list should return stable.");
  }

  @Test
  public void testNormalCasesForApplicationsStrategy()
  {
    TargetApplicationsStrategyProperties appsProperties =
        new TargetApplicationsStrategyProperties().setTargetApplications(new StringArray(Arrays.asList("appA", "appB")))
            .setScope(0.4);
    D2CanaryDistributionStrategy targetAppsStrategy =
        new D2CanaryDistributionStrategy().setStrategy(StrategyType.TARGET_APPLICATIONS)
            .setTargetApplicationsStrategyProperties(appsProperties);

    CanaryDistributionProviderImplFixture fixture = new CanaryDistributionProviderImplFixture();

    // NOT in target list
    Assert.assertEquals(fixture.getSpiedImpl("appC", null).distribute(targetAppsStrategy),
        CanaryDistributionProvider.Distribution.STABLE, "App not in target list should return stable.");

    // in scope and target list
    Assert.assertEquals(fixture.getSpiedImpl("appA", null, 38).distribute(targetAppsStrategy),
        CanaryDistributionProvider.Distribution.CANARY,
        "App in target list and hash result in canary scope should return canary.");

    // not in scope and in target list
    Assert.assertEquals(fixture.getSpiedImpl("appA", null, 50).distribute(targetAppsStrategy),
        CanaryDistributionProvider.Distribution.STABLE,
        "App in target list but hash result not in canary scope should return stable.");
  }

  /**
   * Provide objects for testing edge cases
   * @return a list of objects with structure: {
   *  D2 canary distribution strategy
   * }
   */
  @DataProvider(name = "getEdgeCaseStrategies")
  public Object[][] getEdgeCaseStrategies()
  {
    return new Object[][]{
        // percentage strategy missing properties
        {new D2CanaryDistributionStrategy().setStrategy(StrategyType.PERCENTAGE)},
        // target hosts strategy missing properties
        {new D2CanaryDistributionStrategy().setStrategy(StrategyType.TARGET_HOSTS)},
        // target apps strategy missing properties
        {new D2CanaryDistributionStrategy().setStrategy(StrategyType.TARGET_APPLICATIONS)},
        // strategy with disabled type
        {new D2CanaryDistributionStrategy().setStrategy(StrategyType.DISABLED)},
        // strategy with unknown type
        {new D2CanaryDistributionStrategy().setStrategy(StrategyType.$UNKNOWN)}};
  }

  @Test(dataProvider = "getEdgeCaseStrategies")
  public void testEdgeCases(D2CanaryDistributionStrategy strategy)
  {
    Assert.assertEquals(new CanaryDistributionProviderImplFixture().getSpiedImpl().distribute(strategy),
        CanaryDistributionProvider.Distribution.STABLE, "Invalid strategies should return stable");
  }

  // Verify uniform distribution of the String built-in hashCode method, which theoretically should be guaranteed as it's used
  // in Map with String keys. But we explicitly test it to verify its behavior.
  @Test
  public void testHashDistribution()
  {
    // verify hash results are almost uniformly distributed
    int bucketCount = 100; // since ramp is 100% based, we test with 100 buckets
    int attemptCount = 1000;
    // count how many attempts are skewed
    // (e.g: one bucket gets 13 will cause another bucket gets 7, the attempts skewed in this pair of buckets is ((13 - 10) + (10 - 7)) / 2 = 3
    int skewedCount = 0;
    int[] hits = new int[bucketCount]; // to count hits in each bucket 0 ~ 99.
    String serviceName = "test-service";
    String hostName = "lor1-app";
    int bucketIdx;
    BasicCanaryDistributionProviderImpl impl = spy(new BasicCanaryDistributionProviderImpl(serviceName, hostName));
    for (int i = 0; i < attemptCount; i++) {
      when(impl.getHashKey()).thenReturn(serviceName + hostName + i);
      bucketIdx = impl.getHashResult() % bucketCount; // hash "test-service"+"lor1-appXXX"
      hits[bucketIdx]++;
    }

    // compare each bucket hits with the average hit, aggregate skewed attempts
    int avgHits = attemptCount / bucketCount;
    int offsetBound = 4;
    int offset;
    for (int i = 0; i < bucketCount; i++)
    {
      offset = abs(hits[i] - avgHits);
      skewedCount += offset;
      // verify no single bucket goes beyond the offset bound.
      Assert.assertTrue(offset < offsetBound,
          "hits[" + i + "]" + hits[i] + " should not bias from avgHits " + avgHits + " by greater or equal than " + offsetBound);
    }
    skewedCount /= 2; // divide by 2 since we counted the skewed attempts twice in a pair of buckets
    // skewed attempts should take less than 7% in total attempts (actual skewed rate is ~6.2%).
    double skewedRate = 0.07;
    Assert.assertTrue(skewedCount / (double) attemptCount < skewedRate,
        "Skewed attempts " + skewedCount + " out of total attempts " + attemptCount + " should be less than " + skewedRate);
  }
}
