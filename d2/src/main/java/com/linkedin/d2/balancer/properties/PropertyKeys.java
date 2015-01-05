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
  //used by degrader properties
  public static final String DEGRADER_PROPERTIES = "degraderProperties";
  public static final String DEGRADER_NAME = "degrader.name";
  public static final String DEGRADER_LOG_ENABLED = "degrader.logEnabled";
  public static final String DEGRADER_LATENCY_TO_USE = "degrader.latencyToUse";
  public static final String DEGRADER_OVERRIDE_DROP_DATE = "degrader.overrideDropDate";
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

  //used by service properties
  public static final String PATH = "path";
  public static final String SERVICE_NAME = "serviceName";
  public static final String CLOCK = "clock";
  public static final String SERVICES = "services";
  public static final String TRANSPORT_CLIENT_PROPERTIES = "transportClientProperties";
  public static final String PRIORITIZED_SCHEMES = "prioritizedSchemes";
  public static final String BANNED_URIS = "bannedUri";
  public static final String DEFAULT_ROUTING = "defaultRouting";
  public static final String ALLOWED_CLIENT_OVERRIDE_KEYS = "allowedClientOverrideKeys";
  public static final String SERVICE_METADATA_PROPERTIES = "serviceMetadataProperties";

  //load balancer specific properties
  public static final String LB_STRATEGY_LIST = "loadBalancerStrategyList";
  public static final String LB_STRATEGY_PROPERTIES = "loadBalancerStrategyProperties";

  //load balancer specific properties to replace the old ones
  public static final String HTTP_LB_HASH_METHOD = "http.loadBalancer.hashMethod";
  public static final String HTTP_LB_HASH_CONFIG = "http.loadBalancer.hashConfig";
  public static final String HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS = "http.loadBalancer.updateIntervalMs";
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
  public static final String HASH_ALGORITHM = "hashAlgorithm";
  public static final String CLUSTER_VARIANTS = "clusterVariants";
  public static final String TYPE = "type";
  public static final String CLUSTER_LIST = "clusterList";
  public static final String CLUSTER_VARIANTS_LIST = "clusterVariantsList";
  public static final String FULL_CLUSTER_LIST = "fullClusterList";

  //used by transport client creation
  public static final String HTTP_POOL_WAITER_SIZE = HttpClientFactory.HTTP_POOL_WAITER_SIZE;
  public static final String HTTP_REQUEST_TIMEOUT = HttpClientFactory.HTTP_REQUEST_TIMEOUT;
  public static final String HTTP_MAX_RESPONSE_SIZE = HttpClientFactory.HTTP_MAX_RESPONSE_SIZE;
  public static final String HTTP_POOL_SIZE = HttpClientFactory.HTTP_POOL_SIZE;
  public static final String HTTP_IDLE_TIMEOUT = HttpClientFactory.HTTP_IDLE_TIMEOUT;
  public static final String HTTP_SHUTDOWN_TIMEOUT = HttpClientFactory.HTTP_SHUTDOWN_TIMEOUT;
  public static final String HTTP_SSL_CONTEXT = HttpClientFactory.HTTP_SSL_CONTEXT;
  public static final String HTTP_SSL_PARAMS = HttpClientFactory.HTTP_SSL_PARAMS;
  public static final String HTTP_RESPONSE_COMPRESSION_OPERATIONS = HttpClientFactory.HTTP_RESPONSE_COMPRESSION_OPERATIONS;
  public static final String HTTP_REQUEST_CONTENT_ENCODINGS = HttpClientFactory.HTTP_REQUEST_CONTENT_ENCODINGS;
  public static final String HTTP_USE_RESPONSE_COMPRESSION = HttpClientFactory.HTTP_USE_RESPONSE_COMPRESSION;
  public static final String HTTP_QUERY_POST_THRESHOLD = HttpClientFactory.HTTP_QUERY_POST_THRESHOLD;
  public static final String HTTP_POOL_STRATEGY = HttpClientFactory.HTTP_POOL_STRATEGY;
  public static final String HTTP_POOL_MIN_SIZE = HttpClientFactory.HTTP_POOL_MIN_SIZE;

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

}
