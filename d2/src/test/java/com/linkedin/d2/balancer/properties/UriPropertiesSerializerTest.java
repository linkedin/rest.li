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


import com.linkedin.d2.balancer.util.JacksonUtil;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.d2.discovery.PropertySerializationException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class UriPropertiesSerializerTest
{
  // the old way of constructing uri properties; ideally we would like to keep it as a constructor
  // However, it has the same signature as the new one after type erasure if it is still a constructor
  public static UriProperties getInstanceWithOldArguments(String clusterName, Map<URI, Double> weights)
  {
    Map<URI, Map<Integer, PartitionData>> partitionData = new HashMap<URI, Map<Integer, PartitionData>>();
    for (URI uri : weights.keySet())
    {
      Map<Integer, PartitionData> partitionWeight = new HashMap<Integer, PartitionData>();
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
    Map<URI, Double> uriWeights = new HashMap<URI, Double>();

    UriProperties property = getInstanceWithOldArguments("test", uriWeights);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), property);

    uriWeights.put(URI.create("http://www.google.com"), 1d);
    UriProperties property1 = getInstanceWithOldArguments("test", uriWeights);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property1)), property1);

    uriWeights.put(URI.create("http://www.imdb.com"), 2d);

    UriProperties property2 = getInstanceWithOldArguments("test2", uriWeights);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property2)), property2);

    // test new way of constructing uri property
    final Map<URI, Map<Integer, PartitionData>> partitionDesc = new HashMap<URI, Map<Integer, PartitionData>>();
    property = new UriProperties("test 3", partitionDesc);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), property);
    final Map<Integer, PartitionData> partitions = new HashMap<Integer, PartitionData>();
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
          JacksonUtil.getObjectMapper().readValue(new String(bytes, "UTF-8"), HashMap.class);
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
    Map<URI, Map<Integer, PartitionData>> partitionDesc = new HashMap<URI, Map<Integer, PartitionData>>();
    Map<String, Double> weights = (Map<String, Double>) map.get("weights");
    if (weights != null)
    {
      for(Map.Entry<String, Double> weight: weights.entrySet())
      {
        URI uri = URI.create(weight.getKey());
        Map<Integer, PartitionData> partitionDataMap = new HashMap<Integer, PartitionData>();
        partitionDataMap.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(weight.getValue()));
        partitionDesc.put(uri, partitionDataMap);
      }
    }

    return new UriProperties(clusterName, partitionDesc);
  }

  @Test
  public void testWithApplicationProperties()
      throws PropertySerializationException
  {
    UriPropertiesJsonSerializer jsonSerializer = new UriPropertiesJsonSerializer();
    URI uri = URI.create("https://www.linkedin.com");

    // new constructor

    Map<String, Object> applicationProperties = new HashMap<String, Object>();
    applicationProperties.put("foo", "fooValue");
    applicationProperties.put("bar", "barValue");
    applicationProperties.put("baz", 1);

    Map<URI, Map<Integer, PartitionData>> partitionDesc = new HashMap<URI, Map<Integer, PartitionData>>();
    Map<Integer, PartitionData> partitions = new HashMap<Integer, PartitionData>();
    partitions.put(0, new PartitionData(0.3d));
    partitions.put(1000, new PartitionData(0.3d));

    partitionDesc.put(uri, partitions);

    UriProperties properties = new UriProperties("test", partitionDesc, Collections.singletonMap(uri, applicationProperties));
    UriProperties stored = jsonSerializer.fromBytes(jsonSerializer.toBytes(properties));
    assertEquals(stored, properties);

    // from bytes that were stored using an old constructor

    partitionDesc = new HashMap<URI, Map<Integer, PartitionData>>();
    partitions = new HashMap<Integer, PartitionData>();
    partitions.put(0, new PartitionData(0.3d));
    partitions.put(1000, new PartitionData(0.3d));
    partitionDesc.put(uri, partitions);

    properties = new UriProperties("test", partitionDesc);

    assertEquals(jsonSerializer.fromBytes("{\"clusterName\":\"test\",\"partitionDesc\":{\"https://www.linkedin.com\":{\"0\":{\"weight\":0.3},\"1000\":{\"weight\":0.3}}}}".getBytes()),
                 properties);
  }
}
