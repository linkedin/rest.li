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
import com.linkedin.d2.discovery.PropertyBuilder;
import com.linkedin.d2.discovery.PropertySerializationException;
import com.linkedin.d2.discovery.PropertySerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClusterPropertiesJsonSerializer implements
    PropertySerializer<ClusterProperties>, PropertyBuilder<ClusterProperties>
{
  private static final Logger _log = LoggerFactory.getLogger(ClusterPropertiesJsonSerializer.class);
  private final ObjectMapper _mapper;

  public ClusterPropertiesJsonSerializer()
  {
    _mapper = new ObjectMapper();
  }

  @Override
  public byte[] toBytes(ClusterProperties property)
  {
    try
    {
      return _mapper.writeValueAsString(property).getBytes("UTF-8");
    }
    catch (Exception e)
    {
      _log.error("Failed to write property to bytes: ", e);
    }

    return null;
  }

  @Override
  public ClusterProperties fromBytes(byte[] bytes) throws PropertySerializationException
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
  public ClusterProperties fromMap(Map<String, Object> map)
  {
    @SuppressWarnings("unchecked")
    List<URI> bannedList = (List<URI>)map.get("banned");
    if (bannedList == null)
    {
      bannedList = Collections.emptyList();
    }
    Set<URI> banned = new HashSet<URI>(bannedList);

    String clusterName = PropertyUtil.checkAndGetValue(map, "clusterName", String.class, "ClusterProperties");
    @SuppressWarnings("unchecked")
    List<String> prioritizedSchemes = (List<String>) map.get("prioritizedSchemes");
    @SuppressWarnings("unchecked")
    Map<String, String> properties = (Map<String, String>) map.get("properties");
    @SuppressWarnings("unchecked")
    Map<String, Object> partitionPropertiesMap = (Map<String, Object>)map.get("partitionProperties");
    PartitionProperties partitionProperties;
    String scope = "cluster: " + clusterName;
    if (partitionPropertiesMap != null)
    {
      PartitionProperties.PartitionType partitionType =
          PropertyUtil.checkAndGetValue(partitionPropertiesMap, "partitionType", PartitionProperties.PartitionType.class, scope);
      switch (partitionType)
      {
        case RANGE:
        {
          long keyRangeStart =
              PropertyUtil.checkAndGetValue(partitionPropertiesMap, "keyRangeStart", Number.class, scope).longValue();
          long partitionSize =
              PropertyUtil.checkAndGetValue(partitionPropertiesMap, "partitionSize", Number.class, scope).longValue();
          int partitionCount =
              PropertyUtil.checkAndGetValue(partitionPropertiesMap, "partitionCount", Number.class, scope).intValue();
          String partitionKeyRegex =
              PropertyUtil.checkAndGetValue(partitionPropertiesMap, "partitionKeyRegex", String.class, scope);
          partitionProperties =
              new RangeBasedPartitionProperties(partitionKeyRegex, keyRangeStart, partitionSize, partitionCount);

          break;
        }
        case HASH:
        {
          int partitionCount =
              PropertyUtil.checkAndGetValue(partitionPropertiesMap, "partitionCount", Number.class, scope).intValue();
          String partitionKeyRegex =
              PropertyUtil.checkAndGetValue(partitionPropertiesMap, "partitionKeyRegex", String.class, scope);
          HashBasedPartitionProperties.HashAlgorithm algorithm =
              PropertyUtil.checkAndGetValue(partitionPropertiesMap, "hashAlgorithm", HashBasedPartitionProperties.HashAlgorithm.class, scope);
          partitionProperties =
              new HashBasedPartitionProperties(partitionKeyRegex, partitionCount, algorithm);
          break;
        }
        case NONE:
          partitionProperties = NullPartitionProperties.getInstance();
          break;
        default:
          throw new IllegalArgumentException("In " + scope + ": Unsupported partitionType: " + partitionType);
      }
    }
    else
    {
      partitionProperties = NullPartitionProperties.getInstance();
    }

    return new ClusterProperties(clusterName, prioritizedSchemes, properties, banned, partitionProperties);
  }
}
