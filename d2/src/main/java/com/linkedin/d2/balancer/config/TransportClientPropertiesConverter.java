/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.d2.balancer.config;

import com.linkedin.common.util.MapUtil;
import com.linkedin.d2.D2TransportClientProperties;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.HttpProtocolVersionType;
import com.linkedin.d2.poolStrategyType;
import com.linkedin.data.template.StringArray;
import com.linkedin.r2.util.ConfigValueExtractor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.linkedin.d2.balancer.properties.util.PropertyUtil.coerce;


/**
 * This class converts {@link D2TransportClientProperties} into a map from String to Object
 * that can be stored in zookeeper and vice versa.
 * @author Ang Xu
 */
public class TransportClientPropertiesConverter
{
  public static Map<String, Object> toProperties(D2TransportClientProperties config)
  {
    if (config == null)
    {
      return Collections.emptyMap();
    }

    Map<String, Object> prop = new HashMap<>();
    if (config.hasQueryPostThreshold())
    {
      prop.put(PropertyKeys.HTTP_QUERY_POST_THRESHOLD, config.getQueryPostThreshold().toString());
    }
    if (config.hasRequestTimeout())
    {
      prop.put(PropertyKeys.HTTP_REQUEST_TIMEOUT, config.getRequestTimeout().toString());
    }
    if (config.hasMaxResponseSize())
    {
      prop.put(PropertyKeys.HTTP_MAX_RESPONSE_SIZE, config.getMaxResponseSize().toString());
    }
    if (config.hasPoolSize())
    {
      prop.put(PropertyKeys.HTTP_POOL_SIZE, config.getPoolSize().toString());
    }
    if (config.hasPoolWaiterSize())
    {
      prop.put(PropertyKeys.HTTP_POOL_WAITER_SIZE, config.getPoolWaiterSize().toString());
    }
    if (config.hasIdleTimeout())
    {
      prop.put(PropertyKeys.HTTP_IDLE_TIMEOUT, config.getIdleTimeout().toString());
    }
    if (config.hasShutdownTimeout())
    {
      prop.put(PropertyKeys.HTTP_SHUTDOWN_TIMEOUT, config.getShutdownTimeout().toString());
    }
    if (config.hasResponseCompressionOperations())
    {
      prop.put(PropertyKeys.HTTP_RESPONSE_COMPRESSION_OPERATIONS,
          config.getResponseCompressionOperations().stream().collect(Collectors.joining(",")));
    }
    if (config.hasResponseContentEncodings())
    {
      prop.put(PropertyKeys.HTTP_RESPONSE_CONTENT_ENCODINGS,
          config.getResponseContentEncodings().stream().collect(Collectors.joining(",")));
    }
    if (config.hasRequestContentEncodings())
    {
      prop.put(PropertyKeys.HTTP_REQUEST_CONTENT_ENCODINGS,
          config.getRequestContentEncodings().stream().collect(Collectors.joining(",")));
    }
    if (config.hasUseResponseCompression())
    {
      prop.put(PropertyKeys.HTTP_USE_RESPONSE_COMPRESSION, config.isUseResponseCompression().toString());
    }
    if (config.hasMaxHeaderSize()) {
      prop.put(PropertyKeys.HTTP_MAX_HEADER_SIZE, config.getMaxHeaderSize().toString());
    }
    if (config.hasMaxChunkSize())
    {
      prop.put(PropertyKeys.HTTP_MAX_CHUNK_SIZE, config.getMaxChunkSize().toString());
    }
    if (config.hasPoolStrategy())
    {
      prop.put(PropertyKeys.HTTP_POOL_STRATEGY, config.getPoolStrategy().name());
    }
    if (config.hasMinPoolSize())
    {
      prop.put(PropertyKeys.HTTP_POOL_MIN_SIZE, config.getMinPoolSize().toString());
    }
    if (config.hasMaxConcurrentConnections())
    {
      prop.put(PropertyKeys.HTTP_MAX_CONCURRENT_CONNECTIONS, config.getMaxConcurrentConnections().toString());
    }
    if (config.hasProtocolVersion())
    {
      prop.put(PropertyKeys.HTTP_PROTOCOL_VERSION, config.getProtocolVersion().name());
    }
    if (!config.getAllowedClientOverrideKeys().isEmpty())
    {
      prop.put(PropertyKeys.ALLOWED_CLIENT_OVERRIDE_KEYS,
          config.getAllowedClientOverrideKeys().stream().collect(Collectors.joining(",")));
    }
    return prop;
  }

  public static D2TransportClientProperties toConfig(Map<String, Object> properties)
  {
    D2TransportClientProperties config = new D2TransportClientProperties();
    if (properties.containsKey(PropertyKeys.HTTP_QUERY_POST_THRESHOLD))
    {
      config.setQueryPostThreshold(coerce(properties.get(PropertyKeys.HTTP_QUERY_POST_THRESHOLD),
          Integer.class));
    }
    if (properties.containsKey(PropertyKeys.HTTP_REQUEST_TIMEOUT))
    {
      config.setRequestTimeout(coerce(properties.get(PropertyKeys.HTTP_REQUEST_TIMEOUT), Long.class));
    }
    if (properties.containsKey(PropertyKeys.HTTP_MAX_RESPONSE_SIZE))
    {
      config.setMaxResponseSize(coerce(properties.get(PropertyKeys.HTTP_MAX_RESPONSE_SIZE),
          Integer.class));
    }
    if (properties.containsKey(PropertyKeys.HTTP_POOL_SIZE))
    {
      config.setPoolSize(coerce(properties.get(PropertyKeys.HTTP_POOL_SIZE), Integer.class));
    }
    if (properties.containsKey(PropertyKeys.HTTP_POOL_WAITER_SIZE))
    {
      config.setPoolWaiterSize(coerce(properties.get(PropertyKeys.HTTP_POOL_WAITER_SIZE),
          Integer.class));
    }
    if (properties.containsKey(PropertyKeys.HTTP_IDLE_TIMEOUT))
    {
      config.setIdleTimeout(coerce(properties.get(PropertyKeys.HTTP_IDLE_TIMEOUT),
          Long.class));
    }
    if (properties.containsKey(PropertyKeys.HTTP_SHUTDOWN_TIMEOUT))
    {
      config.setShutdownTimeout(coerce(properties.get(PropertyKeys.HTTP_SHUTDOWN_TIMEOUT),
          Long.class));
    }
    if (properties.containsKey(PropertyKeys.HTTP_RESPONSE_COMPRESSION_OPERATIONS))
    {
      config.setResponseCompressionOperations(new StringArray(
          ConfigValueExtractor.buildList(properties.get(PropertyKeys.HTTP_RESPONSE_COMPRESSION_OPERATIONS), ",")
      ));
    }
    if (properties.containsKey(PropertyKeys.HTTP_RESPONSE_CONTENT_ENCODINGS))
    {
      config.setResponseContentEncodings(new StringArray(
          ConfigValueExtractor.buildList(properties.get(PropertyKeys.HTTP_RESPONSE_CONTENT_ENCODINGS), ",")
      ));
    }
    if (properties.containsKey(PropertyKeys.HTTP_REQUEST_CONTENT_ENCODINGS))
    {
      config.setRequestContentEncodings(new StringArray(
          ConfigValueExtractor.buildList(properties.get(PropertyKeys.HTTP_REQUEST_CONTENT_ENCODINGS), ",")
      ));
    }
    if (properties.containsKey(PropertyKeys.HTTP_USE_RESPONSE_COMPRESSION))
    {
      config.setUseResponseCompression(MapUtil.getWithDefault(properties, PropertyKeys.HTTP_USE_RESPONSE_COMPRESSION,
          Boolean.TRUE));
    }
    if (properties.containsKey(PropertyKeys.HTTP_MAX_HEADER_SIZE))
    {
      config.setMaxHeaderSize(coerce(properties.get(PropertyKeys.HTTP_MAX_HEADER_SIZE), Integer.class));
    }
    if (properties.containsKey(PropertyKeys.HTTP_MAX_CHUNK_SIZE))
    {
      config.setMaxChunkSize(coerce(properties.get(PropertyKeys.HTTP_MAX_CHUNK_SIZE), Integer.class));
    }
    if (properties.containsKey(PropertyKeys.HTTP_POOL_STRATEGY))
    {
      config.setPoolStrategy(poolStrategyType.valueOf((String)properties.get(PropertyKeys.HTTP_POOL_STRATEGY)));
    }
    if (properties.containsKey(PropertyKeys.HTTP_POOL_MIN_SIZE))
    {
      config.setMinPoolSize(coerce(properties.get(PropertyKeys.HTTP_POOL_MIN_SIZE), Integer.class));
    }
    if (properties.containsKey(PropertyKeys.HTTP_MAX_CONCURRENT_CONNECTIONS))
    {
      config.setMaxConcurrentConnections(coerce(properties.get(PropertyKeys.HTTP_MAX_CONCURRENT_CONNECTIONS), Integer.class));
    }
    if (properties.containsKey(PropertyKeys.HTTP_PROTOCOL_VERSION))
    {
      config.setProtocolVersion(
          HttpProtocolVersionType.valueOf((String) properties.get(PropertyKeys.HTTP_PROTOCOL_VERSION)));
    }
    if (properties.containsKey(PropertyKeys.ALLOWED_CLIENT_OVERRIDE_KEYS))
    {
      config.setAllowedClientOverrideKeys(new StringArray(
          ConfigValueExtractor.buildList(properties.get(PropertyKeys.ALLOWED_CLIENT_OVERRIDE_KEYS), ",")
      ));
    }
    return config;
  }
}
