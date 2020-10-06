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


import com.linkedin.d2.D2QuarantineProperties;
import com.linkedin.d2.D2RelativeStrategyProperties;
import com.linkedin.d2.D2RingProperties;
import com.linkedin.d2.HttpStatusCodeRange;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.r2.transport.http.client.HttpClientFactory;


/**
 * This is the centralized source for keys that are used by Properties Map.
 *
 * @author Oby Sumampouw (osumampouw@linkedin.com)
 */
public class PropertyKeys
{
  //==========================================//
  //new constants
  //NOTE: make sure to change d2-schemas and PropertiesConverters accordingly
  //when you edit this file.
  //used by degrader properties
  public static final String DEGRADER_PROPERTIES = "degraderProperties";
  public static final String DEGRADER_NAME = "degrader.name";
  public static final String DEGRADER_LOG_ENABLED = "degrader.logEnabled";
  public static final String DEGRADER_LATENCY_TO_USE = "degrader.latencyToUse";
  public static final String DEGRADER_OVERRIDE_DROP_DATE = "degrader.overrideDropDate";
  public static final String DEGRADER_INITIAL_DROP_RATE = "degrader.initialDropRate";
  public static final String DEGRADER_MAX_DROP_RATE = "degrader.maxDropRate";
  public static final String DEGRADER_MAX_DROP_DURATION = "degrader.maxDropDuration";
  public static final String DEGRADER_UP_STEP = "degrader.upStep";
  public static final String DEGRADER_DOWN_STEP = "degrader.downStep";
  public static final String DEGRADER_MIN_CALL_COUNT = "degrader.minCallCount";
  public static final String DEGRADER_HIGH_LATENCY = "degrader.highLatency";
  public static final String DEGRADER_LOW_LATENCY = "degrader.lowLatency";
  public static final String DEGRADER_HIGH_ERROR_RATE = "degrader.highErrorRate";
  public static final String DEGRADER_LOW_ERROR_RATE = "degrader.lowErrorRate";
  public static final String DEGRADER_HIGH_OUTSTANDING = "degrader.highOutstanding";
  public static final String DEGRADER_LOW_OUTSTANDING = "degrader.lowOutstanding";
  public static final String DEGRADER_MIN_OUTSTANDING_COUNT = "degrader.minOutstandingCount";
  public static final String DEGRADER_OVERRIDE_MIN_CALL_COUNT = "degrader.overrideMinCallCount";
  public static final String DEGRADER_SLOW_START_THRESHOLD = "degrader.slowStartThreshold";
  public static final String DEGRADER_LOG_THRESHOLD = "degrader.logThreshold";
  public static final String DEGRADER_PREEMPTIVE_REQUEST_TIMEOUT_RATE = "degrader.preemptiveRequestTimeoutRate";

  //used by service properties
  public static final String PATH = "path";
  public static final String SERVICE_NAME = "serviceName";
  public static final String CLOCK = "clock";
  public static final String SERVICES = "services";
  public static final String TRANSPORT_CLIENT_PROPERTIES = "transportClientProperties";
  public static final String PRIORITIZED_SCHEMES = "prioritizedSchemes";
  public static final String BANNED_URIS = "bannedUris";
  public static final String DEFAULT_ROUTING = "defaultRouting";
  public static final String ALLOWED_CLIENT_OVERRIDE_KEYS = "allowedClientOverrideKeys";
  public static final String SERVICE_METADATA_PROPERTIES = "serviceMetadataProperties";
  public static final String RELATIVE_STRATEGY_PROPERTIES = "relativeStrategyProperties";

  //load balancer specific properties
  public static final String LB_STRATEGY_LIST = "loadBalancerStrategyList";
  public static final String LB_STRATEGY_PROPERTIES = "loadBalancerStrategyProperties";

  //load balancer specific properties to replace the old ones
  public static final String HTTP_LB_HASH_METHOD = "http.loadBalancer.hashMethod";
  public static final String HTTP_LB_HASH_CONFIG = "http.loadBalancer.hashConfig";
  public static final String HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS = "http.loadBalancer.updateIntervalMs";
  public static final String HTTP_LB_STRATEGY_PROPERTIES_UPDATE_ONLY_AT_INTERVAL = "http.loadBalancer.updateOnlyAtInterval";
  public static final String HTTP_LB_STRATEGY_PROPERTIES_MAX_CLUSTER_LATENCY_WITHOUT_DEGRADING = "http.loadBalancer.maxClusterLatencyWithoutDegrading";
  public static final String HTTP_LB_STRATEGY_PROPERTIES_DEFAULT_SUCCESSFUL_TRANSMISSION_WEIGHT = "http.loadBalancer.defaultSuccessfulTransmissionWeight";
  public static final String HTTP_LB_STRATEGY_PROPERTIES_POINTS_PER_WEIGHT = "http.loadBalancer.pointsPerWeight";
  public static final String HTTP_LB_LOW_WATER_MARK = "http.loadBalancer.lowWaterMark";
  public static final String HTTP_LB_HIGH_WATER_MARK = "http.loadBalancer.highWaterMark";
  public static final String HTTP_LB_INITIAL_RECOVERY_LEVEL = "http.loadBalancer.initialRecoveryLevel";
  public static final String HTTP_LB_RING_RAMP_FACTOR = "http.loadBalancer.ringRampFactor";
  public static final String HTTP_LB_GLOBAL_STEP_UP = "http.loadBalancer.globalStepUp";
  public static final String HTTP_LB_GLOBAL_STEP_DOWN = "http.loadBalancer.globalStepDown";
  public static final String HTTP_LB_CLUSTER_MIN_CALL_COUNT_HIGH_WATER_MARK = "http.loadBalancer.clusterMinCallCount.highWaterMark";
  public static final String HTTP_LB_CLUSTER_MIN_CALL_COUNT_LOW_WATER_MARK = "http.loadBalancer.clusterMinCallCount.lowWaterMark";
  public static final String HTTP_LB_HASHRING_POINT_CLEANUP_RATE = "http.loadBalancer.hashRingPointCleanupRate";
  public static final String HTTP_LB_CONSISTENT_HASH_ALGORITHM = "http.loadBalancer.consistentHashAlgorithm";
  public static final String HTTP_LB_CONSISTENT_HASH_NUM_PROBES = "http.loadBalancer.consistentHashNumProbes";
  public static final String HTTP_LB_CONSISTENT_HASH_POINTS_PER_HOST = "http.loadBalancer.consistentHashPointsPerHost";
  public static final String HTTP_LB_CONSISTENT_HASH_BOUNDED_LOAD_BALANCING_FACTOR = "http.loadBalancer.consistentHashBoundedLoadBalancingFactor";
  public static final String HTTP_LB_QUARANTINE_MAX_PERCENT = "http.loadBalancer.quarantine.maxPercent";
  public static final String HTTP_LB_QUARANTINE_EXECUTOR_SERVICE = "http.loadBalancer.quarantine.executorService";
  public static final String HTTP_LB_QUARANTINE_METHOD = "http.loadBalancer.quarantine.method";
  public static final String HTTP_LB_ERROR_STATUS_REGEX = "http.loadBalancer.errorStatusRegex";
  public static final String HTTP_LB_LOW_EVENT_EMITTING_INTERVAL = "http.loadBalancer.lowEmittingInterval";
  public static final String HTTP_LB_HIGH_EVENT_EMITTING_INTERVAL = "http.loadBalancer.highEmittingInterval";

  // Relative load balancer specific properties
  public static final String UP_STEP = getFieldName(D2RelativeStrategyProperties.fields().upStep());
  public static final String DOWN_STEP = getFieldName(D2RelativeStrategyProperties.fields().downStep());
  public static final String RELATIVE_LATENCY_HIGH_THRESHOLD_FACTOR = getFieldName(D2RelativeStrategyProperties.fields().relativeLatencyHighThresholdFactor());
  public static final String RELATIVE_LATENCY_LOW_THRESHOLD_FACTOR = getFieldName(D2RelativeStrategyProperties.fields().relativeLatencyLowThresholdFactor());
  public static final String HIGH_ERROR_RATE = getFieldName(D2RelativeStrategyProperties.fields().highErrorRate());
  public static final String LOW_ERROR_RATE = getFieldName(D2RelativeStrategyProperties.fields().lowErrorRate());
  public static final String MIN_CALL_COUNT = getFieldName(D2RelativeStrategyProperties.fields().minCallCount());
  public static final String UPDATE_INTERVAL_MS = getFieldName(D2RelativeStrategyProperties.fields().updateIntervalMs());
  public static final String INITIAL_HEALTH_SCORE = getFieldName(D2RelativeStrategyProperties.fields().initialHealthScore());
  public static final String SLOW_START_THRESHOLD = getFieldName(D2RelativeStrategyProperties.fields().slowStartThreshold());
  public static final String ERROR_STATUS_FILTER = getFieldName(D2RelativeStrategyProperties.fields().errorStatusFilter());
  public static final String ERROR_STATUS_LOWER_BOUND = getFieldName(HttpStatusCodeRange.fields().lowerBound());
  public static final String ERROR_STATUS_UPPER_BOUND = getFieldName(HttpStatusCodeRange.fields().upperBound());
  public static final String EMITTING_INTERVAL_MS = getFieldName(D2RelativeStrategyProperties.fields().emittingIntervalMs());
  public static final String ENABLE_FAST_RECOVERY = getFieldName(D2RelativeStrategyProperties.fields().enableFastRecovery());
  public static final String QUARANTINE_PROPERTIES = getFieldName(D2RelativeStrategyProperties.fields().quarantineProperties());
  public static final String QUARANTINE_MAX_PERCENT = getFieldName(D2QuarantineProperties.fields().quarantineMaxPercent());
  public static final String QUARANTINE_HEALTH_CHECK_METHOD = getFieldName(D2QuarantineProperties.fields().healthCheckMethod());
  public static final String QUARANTINE_HEALTH_CHECK_PATH = getFieldName(D2QuarantineProperties.fields().healthCheckPath());
  public static final String RING_PROPERTIES = getFieldName(D2RelativeStrategyProperties.fields().ringProperties());
  public static final String RING_POINTS_PER_WEIGHT = getFieldName(D2RingProperties.fields().pointsPerWeight());
  public static final String RING_HASH_METHOD = getFieldName(D2RingProperties.fields().hashMethod());
  public static final String RING_HASH_CONFIG = getFieldName(D2RingProperties.fields().hashConfig());
  public static final String RING_HASH_RING_POINT_CLEANUP_RATE = getFieldName(D2RingProperties.fields().hashRingPointCleanupRate());
  public static final String RING_CONSISTENT_HASH_ALGORITHM = getFieldName(D2RingProperties.fields().consistentHashAlgorithm());
  public static final String RING_NUMBER_OF_PROBES = getFieldName(D2RingProperties.fields().numberOfProbes());
  public static final String RING_NUMBER_OF_POINTS_PER_HOST = getFieldName(D2RingProperties.fields().numberOfPointsPerHost());
  public static final String RING_BOUNDED_LOAD_BALANCING_FACTOR = getFieldName(D2RingProperties.fields().boundedLoadBalancingFactor());

  //used by service metadata properties
  public static final String SERVICE_FOLLOW_REDIRECTION_MAX_HOP = "followRedirection.maxHop";

  //used by cluster properties
  public static final String CLUSTER_NAME = "clusterName";
  public static final String PARTITION_PROPERTIES = "partitionProperties";
  public static final String PARTITION_TYPE = "partitionType";
  public static final String KEY_RANGE_START = "keyRangeStart";
  public static final String PARTITION_SIZE = "partitionSize";
  public static final String PARTITION_COUNT = "partitionCount";
  public static final String PARTITION_KEY_REGEX = "partitionKeyRegex";
  public static final String PARTITION_ACCESSOR_LIST = "partitionAccessorList";
  public static final String HASH_ALGORITHM = "hashAlgorithm";
  public static final String CLUSTER_VARIANTS = "clusterVariants";
  public static final String TYPE = "type";
  public static final String CLUSTER_LIST = "clusterList";
  public static final String CLUSTER_VARIANTS_LIST = "clusterVariantsList";
  public static final String FULL_CLUSTER_LIST = "fullClusterList";
  public static final String CLUSTER_PROPERTIES = "properties";
  public static final String SSL_VALIDATION_STRINGS = "sslSessionValidationStrings";
  public static final String DARK_CLUSTER_MAP = "darkClusters";
  public static final String DELEGATED = "delegated";

  //used by transport client creation
  public static final String HTTP_POOL_WAITER_SIZE = HttpClientFactory.HTTP_POOL_WAITER_SIZE;
  public static final String HTTP_REQUEST_TIMEOUT = HttpClientFactory.HTTP_REQUEST_TIMEOUT;
  public static final String HTTP_STREAMING_TIMEOUT = HttpClientFactory.HTTP_STREAMING_TIMEOUT;
  public static final String HTTP_MAX_RESPONSE_SIZE = HttpClientFactory.HTTP_MAX_RESPONSE_SIZE;
  public static final String HTTP_POOL_SIZE = HttpClientFactory.HTTP_POOL_SIZE;
  public static final String HTTP_IDLE_TIMEOUT = HttpClientFactory.HTTP_IDLE_TIMEOUT;
  public static final String HTTP_SSL_IDLE_TIMEOUT = HttpClientFactory.HTTP_SSL_IDLE_TIMEOUT;
  public static final String HTTP_SHUTDOWN_TIMEOUT = HttpClientFactory.HTTP_SHUTDOWN_TIMEOUT;
  public static final String HTTP_GRACEFUL_SHUTDOWN_TIMEOUT = HttpClientFactory.HTTP_GRACEFUL_SHUTDOWN_TIMEOUT;
  public static final String HTTP_SSL_CONTEXT = HttpClientFactory.HTTP_SSL_CONTEXT;
  public static final String HTTP_SSL_PARAMS = HttpClientFactory.HTTP_SSL_PARAMS;
  public static final String HTTP_RESPONSE_COMPRESSION_OPERATIONS = HttpClientFactory.HTTP_RESPONSE_COMPRESSION_OPERATIONS;
  public static final String HTTP_RESPONSE_CONTENT_ENCODINGS = HttpClientFactory.HTTP_RESPONSE_CONTENT_ENCODINGS;
  public static final String HTTP_REQUEST_CONTENT_ENCODINGS = HttpClientFactory.HTTP_REQUEST_CONTENT_ENCODINGS;
  public static final String HTTP_USE_RESPONSE_COMPRESSION = HttpClientFactory.HTTP_USE_RESPONSE_COMPRESSION;
  public static final String HTTP_QUERY_POST_THRESHOLD = HttpClientFactory.HTTP_QUERY_POST_THRESHOLD;
  public static final String HTTP_POOL_STRATEGY = HttpClientFactory.HTTP_POOL_STRATEGY;
  public static final String HTTP_POOL_MIN_SIZE = HttpClientFactory.HTTP_POOL_MIN_SIZE;
  public static final String HTTP_POOL_STATS_NAME_PREFIX = HttpClientFactory.HTTP_POOL_STATS_NAME_PREFIX;
  public static final String HTTP_MAX_HEADER_SIZE = HttpClientFactory.HTTP_MAX_HEADER_SIZE;
  public static final String HTTP_MAX_CHUNK_SIZE = HttpClientFactory.HTTP_MAX_CHUNK_SIZE;
  public static final String HTTP_MAX_CONCURRENT_CONNECTIONS = HttpClientFactory.HTTP_MAX_CONCURRENT_CONNECTIONS;
  public static final String HTTP_TCP_NO_DELAY = HttpClientFactory.HTTP_TCP_NO_DELAY;
  public static final String HTTP_PROTOCOL_VERSION = HttpClientFactory.HTTP_PROTOCOL_VERSION;

  //used for multi colo
  public static final String DEFAULT_COLO = "defaultColo";
  public static final String COLO_VARIANTS = "coloVariants";
  public static final String MASTER_COLO = "masterColo";
  public static final String MASTER_SUFFIX = "Master";
  public static final String HAS_COLO_VARIANTS = "hasColoVariants";
  // IS_MASTER_SERVICE is used internally to identify variants of a service that are master services.
  public static final String IS_MASTER_SERVICE = "isMasterService";
  // service metadata properties
  public static final String IS_DEFAULT_SERVICE = "isDefaultService";
  public static final String ENABLE_SYMLINK = "enableSymlink";
  public static final String DEFAULT_ROUTING_TO_MASTER = "defaultRoutingToMaster";

  //used for backup requests
  public static final String BACKUP_REQUESTS = "backupRequests";
  public static final String MIN_BACKUP_DELAY_MS = "minBackupDelayMs";
  public static final String MAX_BURST = "maxBurst";
  public static final String REQUIRED_HISTORY_LENGTH = "requiredHistoryLength";
  public static final String HISTORY_LENGTH = "historyLength";
  public static final String COST = "cost";
  public static final String PROPERTIES = "properties";
  public static final String STRATEGY = "strategy";
  public static final String OPERATION = "operation";

  // used by uri specific properties
  public static final String DO_NOT_SLOW_START = "doNotSlowStart";

  // used by dark clusters
  public static final String DARK_CLUSTER_MULTIPLIER = "multiplier";
  public static final String DARK_CLUSTER_OUTBOUND_TARGET_RATE = "dispatcherOutboundTargetRate";
  public static final String DARK_CLUSTER_OUTBOUND_MAX_RATE = "dispatcherOutboundMaxRate";
  public static final String DARK_CLUSTER_STRATEGY_LIST = "darkClusterStrategyList";
  public static final String DARK_CLUSTER_TRANSPORT_CLIENT_PROPERTIES = "transportClientProperties";

  // used by ClusterInfoProvider
  public static final String HTTP_SCHEME = "http";
  public static final String HTTPS_SCHEME = "https";

  private static String getFieldName(PathSpec pathSpec)
  {
    if (pathSpec.getPathComponents().size() != 1)
    {
      throw new IllegalArgumentException("Field name can not be converted.");
    }
    return pathSpec.getPathComponents().get(0);
  }
}
