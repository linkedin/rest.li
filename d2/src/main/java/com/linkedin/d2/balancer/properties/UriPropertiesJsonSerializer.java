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

import com.linkedin.d2.balancer.properties.util.PropertyUtil;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.d2.discovery.PropertyBuilder;
import com.linkedin.d2.discovery.PropertySerializationException;
import com.linkedin.d2.discovery.PropertySerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class UriPropertiesJsonSerializer implements PropertySerializer<UriProperties>, PropertyBuilder<UriProperties>
{
  private final ObjectMapper _mapper;
  private static final Logger _log = LoggerFactory.getLogger(UriPropertiesJsonSerializer.class);

  public UriPropertiesJsonSerializer()
  {
    _mapper = new ObjectMapper();
  }

  @Override
  public byte[] toBytes(UriProperties property)
  {
    try
    {
      UriProperties propertyToSerialize;
      final Map<URI, Map<Integer, PartitionData>> partitionDesc = property.getPartitionDesc();
      final Map<URI, Double> weights = new HashMap<URI, Double>(partitionDesc.size() * 2);
      boolean isPartitioned = false;
      for (Map.Entry<URI, Map<Integer, PartitionData>> entry : partitionDesc.entrySet())
      {
        final Map<Integer, PartitionData> partitionDataMap = entry.getValue();
        // this is UriProperty for partitioned cluster
        if (partitionDataMap.size() > 1 || (!partitionDataMap.containsKey(DefaultPartitionAccessor.DEFAULT_PARTITION_ID)))
        {
          isPartitioned = true;
          break;
        }
        weights.put(entry.getKey(), partitionDataMap.get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight());
      }
      // for patitioned uri, only write new format
      if (isPartitioned)
      {
        propertyToSerialize = property;
      }
      // for UriProperty of potentially unpartitioned cluster, we also provide "weights" map in serialized format
      // so that old D2 client can understand the property
      // Added here a getter method getWeights() to UriProperty so that ObjectMapper can do its job
      else
      {
        propertyToSerialize = new UriProperties(property.getClusterName(), partitionDesc)
        {
          public Map<URI, Double> getWeights()
          {
            return weights;
          }
        };
      }
      return _mapper.writeValueAsString(propertyToSerialize).getBytes("UTF-8");
    }
    catch (Exception e)
    {
      // TODO log
      e.printStackTrace();
    }

    return null;
  }

  @Override
  public UriProperties fromBytes(byte[] bytes) throws PropertySerializationException
  {
    try
    {
      @SuppressWarnings("unchecked")
      Map<String, Object> untyped =
          _mapper.readValue(new String(bytes, "UTF-8"), HashMap.class);
      return fromMap(untyped);
    }
    catch (Exception e)
    {
      throw new PropertySerializationException(e);
    }
  }

  @Override
  public UriProperties fromMap(Map<String, Object> map)
  {
    String clusterName = PropertyUtil.checkAndGetValue(map, "clusterName", String.class, "UriProperties");

    Map<URI, Map<Integer, PartitionData>> partitionDesc =
        new HashMap<URI, Map<Integer, PartitionData>>();

    @SuppressWarnings("unchecked")
    Map<String, Map<String, Object>> descMap = (Map<String, Map<String, Object>>)map.get("partitionDesc");

    if (descMap != null)
    {
      for (Map.Entry<String, Map<String, Object>> entry : descMap.entrySet())
      {
        URI uri = URI.create(entry.getKey());
        Map<String, Object> partitionMap = entry.getValue();
        Map<Integer, PartitionData> partitionDataMap = new HashMap<Integer, PartitionData>(partitionMap.size()* 2);
        for (Map.Entry<String, Object> partitionEntry : partitionMap.entrySet())
        {
          @SuppressWarnings("unchecked")
          Map<String, Object> partitionEntryValue = (Map<String, Object>)partitionEntry.getValue();
          partitionDataMap.put(PropertyUtil.parseInt("partitionId", partitionEntry.getKey()),
              new PartitionData(PropertyUtil.checkAndGetValue(partitionEntryValue, "weight", Number.class, clusterName).doubleValue()));
        }
        partitionDesc.put(uri, partitionDataMap);
      }
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> weights = (Map<String, Object>) map.get("weights");
    Map<URI, Map<Integer, PartitionData>> partitionDescFromWeights = new HashMap<URI, Map<Integer, PartitionData>>();
    if (weights != null)
    {
      for(Map.Entry<String, Object> weightEntry: weights.entrySet())
      {
        String uriStr = weightEntry.getKey();
        URI uri = URI.create(uriStr);
        Map<Integer, PartitionData> partitionDataMap = new HashMap<Integer, PartitionData>(2);
        partitionDataMap.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
            // may be not a proper use of checkAndGetValue and uriStr is not the proper name for the value
            new PartitionData(PropertyUtil.checkAndGetValue(weights, uriStr, Number.class, clusterName).doubleValue()));
        partitionDescFromWeights.put(uri, partitionDataMap);
      }
    }

    // if both partitionDesc and weights exist, check consistency
    if (!partitionDesc.isEmpty() && !partitionDescFromWeights.isEmpty())
    {
      if (!partitionDesc.equals(partitionDescFromWeights))
      {
        _log.error("Inconsistency detected between partitionDesc and weights", partitionDesc, weights);
      }
    }

    // always trust partitionDesc over weights
    if (partitionDesc.isEmpty())
    {
      partitionDesc = partitionDescFromWeights;
    }

    return new UriProperties(clusterName, partitionDesc);
  }
}
