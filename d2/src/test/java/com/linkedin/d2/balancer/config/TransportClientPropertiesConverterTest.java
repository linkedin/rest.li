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

import com.linkedin.d2.D2TransportClientProperties;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.poolStrategyType;
import com.linkedin.data.template.StringArray;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Ang Xu
 */
public class TransportClientPropertiesConverterTest
{
  @Test
  public void testTransportClientPropertiesConverter()
  {
    final Integer queryPostThreshold = 8192;
    final Long requestTimeout = 10000l;
    final Long maxResponseSize = 1003300l;
    final Integer poolSize = 200;
    final Integer poolWaiterSize = 32768;
    final Long idleTimeout = 600000l;
    final Long shutdownTimeout = 50000l;
    final List<String> responseCompressionRaw = Arrays.asList("finder:*");
    final List<String> responseContentEncoding = Arrays.asList("gzip", "snappy");
    final List<String> requestContentEncoding = Arrays.asList("lz4", "identity");
    final Boolean useResponseCompression = true;
    final Integer maxHeaderSize = 8192;
    final Integer maxChunkSize = 4096;
    final poolStrategyType poolStrategy = poolStrategyType.LRU;
    final Integer minPoolSize = 5;
    final Integer maxConcurrentConnections = 1000;

    Map<String, Object> transportClientProperties = new HashMap<>();
    transportClientProperties.put(PropertyKeys.HTTP_QUERY_POST_THRESHOLD, queryPostThreshold.toString());
    transportClientProperties.put(PropertyKeys.HTTP_REQUEST_TIMEOUT, requestTimeout.toString());
    transportClientProperties.put(PropertyKeys.HTTP_MAX_RESPONSE_SIZE, maxResponseSize.toString());
    transportClientProperties.put(PropertyKeys.HTTP_POOL_SIZE, poolSize.toString());
    transportClientProperties.put(PropertyKeys.HTTP_POOL_WAITER_SIZE, poolWaiterSize.toString());
    transportClientProperties.put(PropertyKeys.HTTP_IDLE_TIMEOUT, idleTimeout.toString());
    transportClientProperties.put(PropertyKeys.HTTP_SHUTDOWN_TIMEOUT, shutdownTimeout.toString());
    transportClientProperties.put(PropertyKeys.HTTP_RESPONSE_COMPRESSION_OPERATIONS, responseCompressionRaw.stream().collect(
        Collectors.joining(",")));
    transportClientProperties.put(PropertyKeys.HTTP_RESPONSE_CONTENT_ENCODINGS, responseContentEncoding.stream().collect(Collectors.joining(",")));
    transportClientProperties.put(PropertyKeys.HTTP_REQUEST_CONTENT_ENCODINGS, requestContentEncoding.stream().collect(Collectors.joining(",")));
    transportClientProperties.put(PropertyKeys.HTTP_USE_RESPONSE_COMPRESSION, useResponseCompression.toString());
    transportClientProperties.put(PropertyKeys.HTTP_MAX_HEADER_SIZE, maxHeaderSize.toString());
    transportClientProperties.put(PropertyKeys.HTTP_MAX_CHUNK_SIZE, maxChunkSize.toString());
    transportClientProperties.put(PropertyKeys.HTTP_POOL_STRATEGY, poolStrategy.name());
    transportClientProperties.put(PropertyKeys.HTTP_POOL_MIN_SIZE, minPoolSize.toString());
    transportClientProperties.put(PropertyKeys.HTTP_MAX_CONCURRENT_CONNECTIONS, maxConcurrentConnections.toString());

    D2TransportClientProperties d2TransportClientProperties =
        new D2TransportClientProperties()
            .setQueryPostThreshold(queryPostThreshold)
            .setRequestTimeout(requestTimeout)
            .setMaxResponseSize(maxResponseSize)
            .setPoolSize(poolSize)
            .setPoolWaiterSize(poolWaiterSize)
            .setIdleTimeout(idleTimeout)
            .setShutdownTimeout(shutdownTimeout)
            .setResponseCompressionOperations(new StringArray(responseCompressionRaw))
            .setResponseContentEncodings(new StringArray(responseContentEncoding))
            .setRequestContentEncodings(new StringArray(requestContentEncoding))
            .setUseResponseCompression(useResponseCompression)
            .setMaxHeaderSize(maxHeaderSize)
            .setMaxChunkSize(maxChunkSize)
            .setPoolStrategy(poolStrategy)
            .setMinPoolSize(minPoolSize)
            .setMaxConcurrentConnections(maxConcurrentConnections);

    Assert.assertEquals(TransportClientPropertiesConverter.toConfig(transportClientProperties), d2TransportClientProperties);
    Assert.assertEquals(TransportClientPropertiesConverter.toProperties(d2TransportClientProperties), transportClientProperties);
  }
}
