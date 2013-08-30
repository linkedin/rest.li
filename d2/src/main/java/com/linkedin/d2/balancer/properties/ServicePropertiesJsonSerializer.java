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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.linkedin.d2.discovery.PropertyBuilder;
import com.linkedin.d2.discovery.PropertySerializationException;
import java.util.Set;
import org.codehaus.jackson.map.ObjectMapper;

import com.linkedin.d2.discovery.PropertySerializer;

public class ServicePropertiesJsonSerializer implements
    PropertySerializer<ServiceProperties>, PropertyBuilder<ServiceProperties>
{
  private final ObjectMapper _mapper;

  public static void main(String[] args) throws UnsupportedEncodingException,
      URISyntaxException, PropertySerializationException
  {
    String serviceName = "testService";
    String clusterName = "testCluster";
    String path = "/foo/bar";
    String loadBalancerStrategyName = "degrader";

    ServiceProperties property =
        new ServiceProperties(serviceName, clusterName, path, loadBalancerStrategyName);

    ServicePropertiesJsonSerializer serializer = new ServicePropertiesJsonSerializer();
    System.err.println(new String(serializer.toBytes(property), "UTF-8"));
    System.err.println(serializer.fromBytes(serializer.toBytes(property)));
  }

  public ServicePropertiesJsonSerializer()
  {
    _mapper = new ObjectMapper();
  }

  @Override
  public byte[] toBytes(ServiceProperties property)
  {
    try
    {
      return _mapper.writeValueAsString(property).getBytes("UTF-8");
    }
    catch (Exception e)
    {
      // TODO log
      e.printStackTrace();
    }

    return null;
  }

  @Override
  public ServiceProperties fromBytes(byte[] bytes) throws PropertySerializationException
  {
    try
    {
      @SuppressWarnings("unchecked")
      Map<String, Object> untyped =
          _mapper.readValue(new String(bytes, "UTF-8"), Map.class);

      return fromMap(untyped);

    }
    catch (Exception e)
    {
      throw new PropertySerializationException(e);
    }
  }

  // Need to work around a compiler bug that doesn't obey the SuppressWarnings("unchecked")
  @SuppressWarnings("unchecked")
  private static <T> T mapGet(Map<String, Object> map, String key)
  {
    return (T) map.get(key);
  }

  public ServiceProperties fromMap(Map<String,Object> map)
  {
    Map<String,Object> loadBalancerStrategyProperties = mapGet(map,PropertyKeys.LB_STRATEGY_PROPERTIES);
    if (loadBalancerStrategyProperties == null)
    {
      loadBalancerStrategyProperties = Collections.emptyMap();
    }
    List<String> loadBalancerStrategyList = mapGet(map, PropertyKeys.LB_STRATEGY_LIST);
    if (loadBalancerStrategyList == null)
    {
      loadBalancerStrategyList = Collections.emptyList();
    }
    Map<String, Object> transportClientProperties = mapGet(map, PropertyKeys.TRANSPORT_CLIENT_PROPERTIES);
    if (transportClientProperties == null)
    {
      transportClientProperties = Collections.emptyMap();
    }
    Map<String, String> degraderProperties = mapGet(map, PropertyKeys.DEGRADER_PROPERTIES);
    if (degraderProperties == null)
    {
      degraderProperties = Collections.emptyMap();
    }
    List<URI> bannedList = mapGet(map, PropertyKeys.BANNED_URIS);
    if (bannedList == null)
    {
      bannedList = Collections.emptyList();
    }
    Set<URI> banned = new HashSet<URI>(bannedList);
    List<String> prioritizedSchemes = mapGet(map,PropertyKeys.PRIORITIZED_SCHEMES);

    Map<String, Object> metadataProperties = new HashMap<String,Object>();
    String isDefaultService = mapGet(map, PropertyKeys.IS_DEFAULT_SERVICE);
    if (isDefaultService != null && "true".equalsIgnoreCase(isDefaultService))
    {
      metadataProperties.put(PropertyKeys.IS_DEFAULT_SERVICE, isDefaultService);
    }

    return new ServiceProperties((String) map.get(PropertyKeys.SERVICE_NAME),
                                 (String) map.get(PropertyKeys.CLUSTER_NAME),
                                 (String) map.get(PropertyKeys.PATH),
                                 (String) map.get(PropertyKeys.LB_STRATEGY_NAME),
                                 loadBalancerStrategyList,
                                 loadBalancerStrategyProperties,
                                 transportClientProperties,
                                 degraderProperties,
                                 prioritizedSchemes,
                                 banned,
                                 metadataProperties);

  }
}
