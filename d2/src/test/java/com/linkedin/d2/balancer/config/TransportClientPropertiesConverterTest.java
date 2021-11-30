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
import com.linkedin.d2.HttpProtocolVersionType;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.poolStrategyType;
import com.linkedin.data.template.StringArray;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;


/**
 * @author Ang Xu
 */
public class TransportClientPropertiesConverterTest
{
  @Test
  public void testTransportClientPropertiesConverter()
  {
    final Integer queryPostThreshold = 8192;
    final Long requestTimeout = 10000L;
    final Long maxResponseSize = 1003300L;
    final Integer poolSize = 200;
    final Integer poolWaiterSize = 32768;
    final Long idleTimeout = 600000L;
    final Long sslIdleTimeout = 900000L;
    final Long shutdownTimeout = 50000L;
    final Long gracefulShutdownTimeout = 30000L;
    final StringArray responseCompressionRaw = new StringArray("finder:*");
    final StringArray responseContentEncoding = new StringArray("gzip", "snappy");
    final StringArray requestContentEncoding = new StringArray("lz4", "identity");
    final Boolean useResponseCompression = true;
    final Integer maxHeaderSize = 8192;
    final Integer maxChunkSize = 4096;
    final poolStrategyType poolStrategy = poolStrategyType.LRU;
    final Integer minPoolSize = 5;
    final String poolStatsNamePrefix = "poolStats";
    final Integer maxConcurrentConnections = 1000;
    final Boolean tcpNoDelay = true;
    final HttpProtocolVersionType protocolVersion = HttpProtocolVersionType.HTTP_1_1;
    final StringArray allowedClientOverrideKeys = new StringArray(PropertyKeys.HTTP_REQUEST_TIMEOUT,
        PropertyKeys.HTTP_QUERY_POST_THRESHOLD);
    final Double maxClientRequestRetryRatio = 0.2;

    Map<String, Object> transportClientProperties = new HashMap<>();
    transportClientProperties.put(PropertyKeys.HTTP_QUERY_POST_THRESHOLD, queryPostThreshold.toString());
    transportClientProperties.put(PropertyKeys.HTTP_REQUEST_TIMEOUT, requestTimeout.toString());
    transportClientProperties.put(PropertyKeys.HTTP_MAX_RESPONSE_SIZE, maxResponseSize.toString());
    transportClientProperties.put(PropertyKeys.HTTP_POOL_SIZE, poolSize.toString());
    transportClientProperties.put(PropertyKeys.HTTP_POOL_WAITER_SIZE, poolWaiterSize.toString());
    transportClientProperties.put(PropertyKeys.HTTP_IDLE_TIMEOUT, idleTimeout.toString());
    transportClientProperties.put(PropertyKeys.HTTP_SSL_IDLE_TIMEOUT, sslIdleTimeout.toString());
    transportClientProperties.put(PropertyKeys.HTTP_SHUTDOWN_TIMEOUT, shutdownTimeout.toString());
    transportClientProperties.put(PropertyKeys.HTTP_GRACEFUL_SHUTDOWN_TIMEOUT, gracefulShutdownTimeout.toString());
    transportClientProperties.put(PropertyKeys.HTTP_RESPONSE_COMPRESSION_OPERATIONS,
        String.join(",", responseCompressionRaw));
    transportClientProperties.put(PropertyKeys.HTTP_RESPONSE_CONTENT_ENCODINGS,
        String.join(",", responseContentEncoding));
    transportClientProperties.put(PropertyKeys.HTTP_REQUEST_CONTENT_ENCODINGS,
        String.join(",", requestContentEncoding));
    transportClientProperties.put(PropertyKeys.HTTP_USE_RESPONSE_COMPRESSION, useResponseCompression.toString());
    transportClientProperties.put(PropertyKeys.HTTP_MAX_HEADER_SIZE, maxHeaderSize.toString());
    transportClientProperties.put(PropertyKeys.HTTP_MAX_CHUNK_SIZE, maxChunkSize.toString());
    transportClientProperties.put(PropertyKeys.HTTP_POOL_STRATEGY, poolStrategy.name());
    transportClientProperties.put(PropertyKeys.HTTP_POOL_MIN_SIZE, minPoolSize.toString());
    transportClientProperties.put(PropertyKeys.HTTP_POOL_STATS_NAME_PREFIX, poolStatsNamePrefix);
    transportClientProperties.put(PropertyKeys.HTTP_MAX_CONCURRENT_CONNECTIONS, maxConcurrentConnections.toString());
    transportClientProperties.put(PropertyKeys.HTTP_TCP_NO_DELAY, tcpNoDelay.toString());
    transportClientProperties.put(PropertyKeys.HTTP_PROTOCOL_VERSION, protocolVersion.name());
    transportClientProperties.put(PropertyKeys.ALLOWED_CLIENT_OVERRIDE_KEYS,
        String.join(",", allowedClientOverrideKeys));
    transportClientProperties.put(PropertyKeys.HTTP_MAX_CLIENT_REQUEST_RETRY_RATIO, maxClientRequestRetryRatio.toString());

    D2TransportClientProperties d2TransportClientProperties =
        new D2TransportClientProperties()
            .setQueryPostThreshold(queryPostThreshold)
            .setRequestTimeout(requestTimeout)
            .setMaxResponseSize(maxResponseSize)
            .setPoolSize(poolSize)
            .setPoolWaiterSize(poolWaiterSize)
            .setIdleTimeout(idleTimeout)
            .setSslIdleTimeout(sslIdleTimeout)
            .setShutdownTimeout(shutdownTimeout)
            .setGracefulShutdownTimeout(gracefulShutdownTimeout)
            .setResponseCompressionOperations(responseCompressionRaw)
            .setResponseContentEncodings(responseContentEncoding)
            .setRequestContentEncodings(requestContentEncoding)
            .setUseResponseCompression(useResponseCompression)
            .setMaxHeaderSize(maxHeaderSize)
            .setMaxChunkSize(maxChunkSize)
            .setPoolStrategy(poolStrategy)
            .setMinPoolSize(minPoolSize)
            .setPoolStatsNamePrefix(poolStatsNamePrefix)
            .setMaxConcurrentConnections(maxConcurrentConnections)
            .setProtocolVersion(protocolVersion)
            .setTcpNoDelay(tcpNoDelay)
            .setAllowedClientOverrideKeys(allowedClientOverrideKeys)
            .setMaxClientRequestRetryRatio(maxClientRequestRetryRatio);

    Assert.assertEquals(TransportClientPropertiesConverter.toConfig(transportClientProperties), d2TransportClientProperties);
    Assert.assertEquals(TransportClientPropertiesConverter.toProperties(d2TransportClientProperties), transportClientProperties);
  }
}
