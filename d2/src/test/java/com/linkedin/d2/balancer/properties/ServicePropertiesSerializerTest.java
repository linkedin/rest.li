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

package com.linkedin.d2.balancer.properties;


import com.linkedin.d2.ConsistentHashAlgorithm;
import com.linkedin.d2.D2QuarantineProperties;
import com.linkedin.d2.D2RelativeStrategyProperties;
import com.linkedin.d2.D2RingProperties;
import com.linkedin.d2.HttpMethod;
import com.linkedin.d2.HttpStatusCodeRange;
import com.linkedin.d2.HttpStatusCodeRangeArray;
import com.linkedin.d2.balancer.config.RelativeStrategyPropertiesConverter;
import com.linkedin.d2.discovery.PropertySerializationException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class ServicePropertiesSerializerTest
{

  public static final String TEST_SERVICE_NAME = "serviceName";
  public static final String TEST_CLUSTER_NAME = "clusterName";

  public static void main(String[] args) throws URISyntaxException, PropertySerializationException
  {
    new ServicePropertiesSerializerTest().testServicePropertiesSerializer();
  }

  @Test(groups = { "small", "back-end" })
  public void testServicePropertiesSerializer() throws URISyntaxException,
          PropertySerializationException
  {
    ServicePropertiesJsonSerializer serializer = new ServicePropertiesJsonSerializer();

    ServiceProperties property =
      new ServiceProperties(TEST_SERVICE_NAME, TEST_CLUSTER_NAME, "/foo", Arrays.asList("rr"));
    ServiceStoreProperties storeProperties = new ServiceStoreProperties(property, null, null);
    // service properties will be serialized then deserialized as service store properties
    assertEquals(serializer.fromBytes(serializer.toBytes(property)), storeProperties);
    // service store properties will be serialized then deserialized as service store properties
    assertEquals(serializer.fromBytes(serializer.toBytes(storeProperties)), storeProperties);


    property = new ServiceProperties("servicename2", "clustername2", "/path2", Arrays.asList("strategy2"),
                                     Collections.<String,Object>singletonMap("foo", "bar"));
    assertEquals(serializer.fromBytes(serializer.toBytes(property)), new ServiceStoreProperties(property, null, null));

    Map<String, Object> arbitraryProperties = new HashMap<>();
    arbitraryProperties.put("foo", "bar");
    property = new ServiceStoreProperties(TEST_SERVICE_NAME,
      TEST_CLUSTER_NAME,
                                     "/service",
                                     Arrays.asList("strategyName"),
                                     arbitraryProperties,
                                     arbitraryProperties,
                                     Collections.<String, String>emptyMap(),
                                     Collections.<String>emptyList(),
                                     Collections.<URI>emptySet(),
                                     arbitraryProperties);
    assertEquals(serializer.fromBytes(serializer.toBytes(property)), new ServiceStoreProperties(property, null, null));
  }

  @DataProvider(name = "distributionStrategies")
  public Object[][] getDistributionStrategies() {
    Map<String, Object> percentageProperties = new HashMap<>();
    percentageProperties.put("scope", 0.1);

    Map<String, Object> targetHostsProperties = new HashMap<>();
    targetHostsProperties.put("targetHosts", Arrays.asList("hostA", "hostB"));

    Map<String, Object> targetApplicationsProperties = new HashMap<>();
    targetApplicationsProperties.put("targetApplications", Arrays.asList("appA", "appB"));
    targetApplicationsProperties.put("scope", 0.1);

    return new Object[][] {
        {new CanaryDistributionStrategy("percentage", percentageProperties, Collections.emptyMap(), Collections.emptyMap())},
        {new CanaryDistributionStrategy("targetHosts", Collections.emptyMap(), targetHostsProperties, Collections.emptyMap())},
        {new CanaryDistributionStrategy("targetApplications", Collections.emptyMap(), Collections.emptyMap(), targetApplicationsProperties)}
    };
  }

  @Test(dataProvider = "distributionStrategies")
  public void testServicePropertiesWithCanary(CanaryDistributionStrategy distributionStrategy) throws PropertySerializationException
  {
    ServicePropertiesJsonSerializer serializer = new ServicePropertiesJsonSerializer();

    // canary configs has a different cluster name (migrating the service) and adds relative strategy properties
    ServiceProperties canaryProperty = new ServiceProperties("servicename2", "clustername3",
        "/path2", Arrays.asList("rr"), new HashMap<>(),
        null, null, Arrays.asList("HTTPS"), Collections.emptySet(),
        Collections.emptyMap(), Collections.emptyList(), RelativeStrategyPropertiesConverter.toMap(createRelativeStrategyProperties()));

    ServiceStoreProperties property = new ServiceStoreProperties("servicename2",
        "clustername2", "/path2", Arrays.asList("strategy2"), canaryProperty, distributionStrategy);

    assertEquals(serializer.fromBytes(serializer.toBytes(property)), property);
  }

  @Test
  public void testServicePropertiesWithCanaryEdgeCases()  throws PropertySerializationException
  {
    ServicePropertiesJsonSerializer serializer = new ServicePropertiesJsonSerializer();

    ServiceProperties property = new ServiceProperties(TEST_SERVICE_NAME, TEST_CLUSTER_NAME, "/foo", Arrays.asList("rr"));
    ServiceStoreProperties expected = new ServiceStoreProperties(property, null, null);
    // having canary configs but missing distribution strategy will not be taken in.
    ServiceStoreProperties inputProperty = new ServiceStoreProperties(property, property, null);
    assertEquals(serializer.fromBytes(serializer.toBytes(inputProperty)), expected);

    // having distribution strategy but missing canary configs will not be taken in.
    inputProperty = new ServiceStoreProperties(property, null, new CanaryDistributionStrategy("percentage",
        Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()));
    assertEquals(serializer.fromBytes(serializer.toBytes(inputProperty)), expected);
  }

  @Test
  public void testServicePropertiesSerializerWithRelativeStrategy() throws PropertySerializationException
  {
    ServicePropertiesJsonSerializer serializer = new ServicePropertiesJsonSerializer();

    D2RelativeStrategyProperties relativeStrategyProperties = createRelativeStrategyProperties();
    ServiceProperties property =
        new ServiceProperties(TEST_SERVICE_NAME, TEST_CLUSTER_NAME, "/foo", Arrays.asList("rr"), new HashMap<>(),
            null, null, Arrays.asList("HTTPS"), Collections.emptySet(),
            Collections.emptyMap(), Collections.emptyList(), RelativeStrategyPropertiesConverter.toMap(relativeStrategyProperties));
    assertEquals(serializer.fromBytes(serializer.toBytes(property)), new ServiceStoreProperties(property, null, null));
  }

  @Test(groups = { "small", "back-end" }, enabled = false)
  public void testBadConfigInServiceProperties() throws PropertySerializationException
  {
    ServicePropertiesJsonSerializer serializer = new ServicePropertiesJsonSerializer();
    Map<String, String> badDegraderConfig = Collections.singletonMap(PropertyKeys.DEGRADER_INITIAL_DROP_RATE, "0.1");

    Map<String, Object> arbitraryProperties = new HashMap<>();
    arbitraryProperties.put("foo", "bar");
    ServiceProperties badServiceProp = new ServiceProperties(TEST_SERVICE_NAME,
      TEST_CLUSTER_NAME,
        "/service",
        Arrays.asList("strategyName"),
        arbitraryProperties,
        arbitraryProperties,
        badDegraderConfig,
        Collections.<String>emptyList(),
        Collections.<URI>emptySet(),
        arbitraryProperties);

    ServiceProperties goodServiceProp = new ServiceProperties(TEST_SERVICE_NAME,
      TEST_CLUSTER_NAME,
        "/service",
        Arrays.asList("strategyName"),
        arbitraryProperties,
        arbitraryProperties,
        Collections.<String, String>emptyMap(),
        Collections.<String>emptyList(),
        Collections.<URI>emptySet(),
        arbitraryProperties);

    assertEquals(serializer.fromBytes(serializer.toBytes(badServiceProp)), new ServiceStoreProperties(goodServiceProp, null, null));
  }

  @Test
  public void testServicePropertiesClientOverride() throws PropertySerializationException
  {
    Map<String, Object> transportPropertiesClientSide = new HashMap<>();
    transportPropertiesClientSide.put(PropertyKeys.ALLOWED_CLIENT_OVERRIDE_KEYS, "http.requestTimeout, http.useResponseCompression, http.responseContentEncodings");
    transportPropertiesClientSide.put(PropertyKeys.HTTP_REQUEST_TIMEOUT, "10000");
    transportPropertiesClientSide.put(PropertyKeys.HTTP_USE_RESPONSE_COMPRESSION, true);
    transportPropertiesClientSide.put(PropertyKeys.HTTP_RESPONSE_CONTENT_ENCODINGS, "1000");
    transportPropertiesClientSide.put(PropertyKeys.HTTP_IDLE_TIMEOUT, "100000");
    ServicePropertiesJsonSerializer serializerWithClientProperties = new ServicePropertiesJsonSerializer(Collections.singletonMap(TEST_SERVICE_NAME, transportPropertiesClientSide));

    Map<String, Object> transportPropertiesServerSide = new HashMap<>();
    transportPropertiesServerSide.put(PropertyKeys.ALLOWED_CLIENT_OVERRIDE_KEYS, "http.requestTimeout, http.useResponseCompression, http.responseContentEncodings");
    transportPropertiesServerSide.put(PropertyKeys.HTTP_REQUEST_TIMEOUT, "5000");
    transportPropertiesServerSide.put(PropertyKeys.HTTP_USE_RESPONSE_COMPRESSION, false);
    transportPropertiesServerSide.put(PropertyKeys.HTTP_RESPONSE_CONTENT_ENCODINGS, "5000");


    ServiceProperties servicePropertiesServerSide =
      new ServiceProperties(TEST_SERVICE_NAME, TEST_CLUSTER_NAME, "/foo",
        Arrays.asList("strategyName"), Collections.emptyMap(),
        transportPropertiesServerSide, Collections.emptyMap(),
        Collections.emptyList(), Collections.emptySet());
    ServiceProperties servicePropertiesWithClientCfg = serializerWithClientProperties.fromBytes(serializerWithClientProperties.toBytes(servicePropertiesServerSide));

    boolean atLeastOneConfigFromCfg2 = false;
    boolean atLeastOneConfigFromZk = false;
    for (Map.Entry<String, Object> compiledProperty : servicePropertiesWithClientCfg.getTransportClientProperties().entrySet())
    {
      if (AllowedClientPropertyKeys.isAllowedConfigKey(compiledProperty.getKey()))
      {
        atLeastOneConfigFromCfg2 = true;
        Assert.assertEquals(compiledProperty.getValue(), transportPropertiesClientSide.get(compiledProperty.getKey()));
      }
      else
      {
        atLeastOneConfigFromZk = true;
        Assert.assertEquals(compiledProperty.getValue(), transportPropertiesServerSide.get(compiledProperty.getKey()));
      }
    }

    Assert.assertTrue(atLeastOneConfigFromCfg2);
    Assert.assertTrue(atLeastOneConfigFromZk);

    Map<String, Object> transportProperties = servicePropertiesWithClientCfg.getTransportClientProperties();
    Assert.assertTrue(transportProperties != null && transportProperties.containsKey(PropertyKeys.ALLOWED_CLIENT_OVERRIDE_KEYS));
  }

  private D2RelativeStrategyProperties createRelativeStrategyProperties()
  {
    double upStep = 0.2;
    double downStep = 0.1;
    double relativeLatencyHighThresholdFactor = 1.5;
    double relativeLatencyLowThresholdFactor = 1.2;
    double highErrorRate = 0.2;
    double lowErrorRate = 0.1;
    int minCallCount = 1000;
    long updateIntervalMs = 5000;
    double initialHealthScore = 0.0;
    double slowStartThreshold = 0.32;
    HttpStatusCodeRangeArray
        errorStatusRange = new HttpStatusCodeRangeArray(new HttpStatusCodeRange().setLowerBound(500).setUpperBound(599));
    double quarantineMaxPercent = 0.1;
    HttpMethod quarantineMethod = HttpMethod.OPTIONS;
    String healthCheckPath = "";
    ConsistentHashAlgorithm consistentHashAlgorithm = ConsistentHashAlgorithm.POINT_BASED;

    D2QuarantineProperties quarantineProperties = new D2QuarantineProperties()
        .setQuarantineMaxPercent(quarantineMaxPercent)
        .setHealthCheckMethod(quarantineMethod)
        .setHealthCheckPath(healthCheckPath);

    D2RingProperties ringProperties = new D2RingProperties()
        .setConsistentHashAlgorithm(consistentHashAlgorithm);

    return new D2RelativeStrategyProperties()
        .setQuarantineProperties(quarantineProperties)
        .setRingProperties(ringProperties)
        .setUpStep(upStep)
        .setDownStep(downStep)
        .setRelativeLatencyHighThresholdFactor(relativeLatencyHighThresholdFactor)
        .setRelativeLatencyLowThresholdFactor(relativeLatencyLowThresholdFactor)
        .setHighErrorRate(highErrorRate)
        .setLowErrorRate(lowErrorRate)
        .setMinCallCount(minCallCount)
        .setUpdateIntervalMs(updateIntervalMs)
        .setInitialHealthScore(initialHealthScore)
        .setSlowStartThreshold(slowStartThreshold)
        .setErrorStatusFilter(errorStatusRange);
  }
}
