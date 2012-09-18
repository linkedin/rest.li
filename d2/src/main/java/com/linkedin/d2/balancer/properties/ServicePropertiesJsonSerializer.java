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
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.linkedin.d2.discovery.PropertyBuilder;
import com.linkedin.d2.discovery.PropertySerializationException;
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

  public ServiceProperties fromMap(Map<String,Object> map)
  {
    @SuppressWarnings("unchecked")
    Map<String,Object> loadBalancerStrategyProperties = (Map<String,Object>)map.get("loadBalancerStrategyProperties");
    if (loadBalancerStrategyProperties == null)
    {
      loadBalancerStrategyProperties = Collections.emptyMap();
    }
    @SuppressWarnings("unchecked")
    List<String> loadBalancerStrategyList = (List<String>) map.get("loadBalancerStrategyList");
    if (loadBalancerStrategyList == null)
    {
      loadBalancerStrategyList = Collections.emptyList();
    }

    return new ServiceProperties((String) map.get("serviceName"),
                                 (String) map.get("clusterName"),
                                 (String) map.get("path"),
                                 (String) map.get("loadBalancerStrategyName"),
                                 loadBalancerStrategyList,
                                 loadBalancerStrategyProperties);

  }
}
