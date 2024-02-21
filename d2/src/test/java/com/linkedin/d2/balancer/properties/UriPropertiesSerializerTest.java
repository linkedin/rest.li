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


import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.linkedin.d2.balancer.util.JacksonUtil;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.d2.discovery.PropertySerializationException;

import indis.XdsD2;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class UriPropertiesSerializerTest
{
  private static final URI TEST_URI = URI.create("https://www.linkedin.com");
  private static final String CLUSTER_NAME = "test";
  private static final Map<URI, Map<Integer, PartitionData>> PARTITION_DESC = new HashMap<>();
  private static final UriProperties URI_PROP;

  static {
    Map<Integer, PartitionData> partitions = new HashMap<>();
    partitions.put(0, new PartitionData(0.3d));
    partitions.put(1000, new PartitionData(0.3d));
    PARTITION_DESC.put(TEST_URI, partitions);

    URI_PROP = new UriProperties("test", PARTITION_DESC,
        Collections.emptyMap(), 0);
  }

  // the old way of constructing uri properties; ideally we would like to keep it as a constructor
  // However, it has the same signature as the new one after type erasure if it is still a constructor
  public static UriProperties getInstanceWithOldArguments(String clusterName, Map<URI, Double> weights)
  {
    Map<URI, Map<Integer, PartitionData>> partitionData = new HashMap<>();
    for (URI uri : weights.keySet())
    {
      Map<Integer, PartitionData> partitionWeight = new HashMap<>();
      partitionWeight.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(weights.get(uri)));
      partitionData.put(uri,partitionWeight);
    }
    return new UriProperties(clusterName, partitionData);
  }

  @Test(groups = { "small", "back-end" })
  public void testUriPropertiesSerializer() throws URISyntaxException,
          PropertySerializationException
  {
    UriPropertiesJsonSerializer jsonSerializer = new UriPropertiesJsonSerializer();
    Map<URI, Double> uriWeights = new HashMap<>();

    UriProperties property = getInstanceWithOldArguments("test", uriWeights);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), property);

    uriWeights.put(URI.create("http://www.google.com"), 1d);
    UriProperties property1 = getInstanceWithOldArguments("test", uriWeights);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property1)), property1);

    uriWeights.put(URI.create("http://www.imdb.com"), 2d);

    UriProperties property2 = getInstanceWithOldArguments("test2", uriWeights);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property2)), property2);

    // test new way of constructing uri property
    final Map<URI, Map<Integer, PartitionData>> partitionDesc = new HashMap<>();
    property = new UriProperties("test 3", partitionDesc);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), property);
    final Map<Integer, PartitionData> partitions = new HashMap<>();
    partitions.put(0, new PartitionData(0.3d));
    partitions.put(700, new PartitionData(0.3d));
    partitions.put(1200, new PartitionData(0.4d));


    partitionDesc.put(URI.create("http://www.google.com"), partitions);
    partitionDesc.put(URI.create("http://www.imdb.com"), partitions);
    property = new UriProperties("test 3", partitionDesc);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), property);

    // test compatibility with old UriProperties bytes: client can understand uris published by old servers
    String oldUriJson = "{\"clusterName\": \"test4\", \"weights\":{\"http://www.google.com\": 1.0, \"http://www.imdb.com\": 2.0}}";
    UriProperties fromOldBytes = jsonSerializer.fromBytes(oldUriJson.getBytes());
    UriProperties createdNew = getInstanceWithOldArguments("test4", uriWeights);
    assertEquals(fromOldBytes, createdNew);

    // test the we include old uri format in the serialization result for unpartitioned services
    // servers publish uris that can be understood by old clients (if it is unpartitioned services)
    byte[] bytesIncludingWeights = jsonSerializer.toBytes(createdNew);
    UriProperties result = fromOldFormatBytes(bytesIncludingWeights);
    assertEquals(createdNew, result);
  }

  public UriProperties fromOldFormatBytes(byte[] bytes) throws PropertySerializationException
  {
    try
    {
      @SuppressWarnings("unchecked")
      Map<String, Object> untyped =
          JacksonUtil.getObjectMapper().readValue(new String(bytes, StandardCharsets.UTF_8), HashMap.class);
      return fromOldFormatMap(untyped);
    }
    catch (Exception e)
    {
      throw new PropertySerializationException(e);
    }
  }

  // this method only uses the weights map
  // if this method succeeds, old clients would have no problem processing the weights map
  @SuppressWarnings("unchecked")
  public UriProperties fromOldFormatMap(Map<String, Object> map)
  {
    String clusterName = (String)map.get("clusterName");
    Map<URI, Map<Integer, PartitionData>> partitionDesc = new HashMap<>();
    Map<String, Double> weights = (Map<String, Double>) map.get("weights");
    if (weights != null)
    {
      for(Map.Entry<String, Double> weight: weights.entrySet())
      {
        URI uri = URI.create(weight.getKey());
        Map<Integer, PartitionData> partitionDataMap = new HashMap<>();
        partitionDataMap.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(weight.getValue()));
        partitionDesc.put(uri, partitionDataMap);
      }
    }

    return new UriProperties(clusterName, partitionDesc);
  }

  @Test
  public void testWithApplicationPropertiesAndVersion()
      throws PropertySerializationException
  {
    UriPropertiesJsonSerializer jsonSerializer = new UriPropertiesJsonSerializer();

    // new constructor
    Map<String, Object> applicationProperties = new HashMap<>();
    applicationProperties.put("foo", "fooValue");
    applicationProperties.put("bar", "barValue");
    applicationProperties.put("baz", 1);

    UriProperties properties = new UriProperties("test", PARTITION_DESC,
        Collections.singletonMap(TEST_URI, applicationProperties), 0);

    UriProperties stored = jsonSerializer.fromBytes(jsonSerializer.toBytes(properties));
    assertEquals(stored, properties);

    // from bytes that were stored using an old constructor
    assertEquals(jsonSerializer.fromBytes(("{\"clusterName\":\"test\",\"partitionDesc\":{\"https://www.linkedin.com\""
            + ":{\"0\":{\"weight\":0.3},\"1000\":{\"weight\":0.3}}}}").getBytes()), URI_PROP);
  }

  @Test
  public void testFromProto() throws PropertySerializationException {
    UriPropertiesJsonSerializer jsonSerializer = new UriPropertiesJsonSerializer();

    XdsD2.D2URI xdsUri = XdsD2.D2URI.newBuilder()
        .setVersion(0)
        .setUri(TEST_URI.toString())
        .setClusterName(CLUSTER_NAME)
        .putPartitionDesc(0, 0.3d)
        .putPartitionDesc(1000, 0.3d)
        .build();
    UriProperties actual = jsonSerializer.fromProto(xdsUri);
    assertEquals(actual, URI_PROP);
    assertEquals(actual.toString(), URI_PROP.toString()); // string is also the same so that FS data is the same

    UriProperties expected = new UriProperties("test", PARTITION_DESC,
        Collections.singletonMap(TEST_URI,
            Collections.singletonMap("foo", "fooValue")), 1);
    xdsUri = XdsD2.D2URI.newBuilder()
        .setVersion(1)
        .setUri(TEST_URI.toString())
        .setClusterName(CLUSTER_NAME)
        .putPartitionDesc(0, 0.3d)
        .putPartitionDesc(1000, 0.3d)
        .setUriSpecificProperties(Struct.newBuilder()
            .putFields("foo", Value.newBuilder().setStringValue("fooValue").build())
            .build())
        .build();
    actual = jsonSerializer.fromProto(xdsUri);
    assertEquals(actual, expected);
    assertEquals(actual.toString(), expected.toString()); // string is also the same so that FS data is the same
  }
}
