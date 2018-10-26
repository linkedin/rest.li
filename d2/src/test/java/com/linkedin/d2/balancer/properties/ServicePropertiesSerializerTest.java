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


import com.linkedin.d2.discovery.PropertySerializationException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
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
    assertEquals(serializer.fromBytes(serializer.toBytes(property)), property);

    property = new ServiceProperties("servicename2", "clustername2", "/path2", Arrays.asList("strategy2"),
                                     Collections.<String,Object>singletonMap("foo", "bar"));
    assertEquals(serializer.fromBytes(serializer.toBytes(property)), property);

    Map<String, Object> arbitraryProperties = new HashMap<String, Object>();
    arbitraryProperties.put("foo", "bar");
    property = new ServiceProperties(TEST_SERVICE_NAME,
      TEST_CLUSTER_NAME,
                                     "/service",
                                     Arrays.asList("strategyName"),
                                     arbitraryProperties,
                                     arbitraryProperties,
                                     Collections.<String, String>emptyMap(),
                                     Collections.<String>emptyList(),
                                     Collections.<URI>emptySet(),
                                     arbitraryProperties);
    assertEquals(serializer.fromBytes(serializer.toBytes(property)), property);
  }

  @Test(groups = { "small", "back-end" }, enabled = false)
  public void testBadConfigInServiceProperties() throws PropertySerializationException
  {
    ServicePropertiesJsonSerializer serializer = new ServicePropertiesJsonSerializer();
    Map<String, String> badDegraderConfig = Collections.singletonMap(PropertyKeys.DEGRADER_INITIAL_DROP_RATE, "0.1");

    Map<String, Object> arbitraryProperties = new HashMap<String, Object>();
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

    assertEquals(serializer.fromBytes(serializer.toBytes(badServiceProp)), goodServiceProp);
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
}
