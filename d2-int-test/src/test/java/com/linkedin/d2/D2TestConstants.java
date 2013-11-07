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

package com.linkedin.d2;

interface D2TestConstants
{
  public static final String ECHO_SERVER_HOST     = "127.0.0.1";
  public static final double DISABLED_NODE_WEIGHT = -999.0;
  public static final double             D2_DELAY = 1.0;
  public static final double             D2_RANGE = 1.0;
  public static final int                  FAILED = -111;

  public static final int QUORUM_SIZE             = 7;
  public static final int TTL_CLUSTERS            = 4;
  public static final int TIMEOUT                 = 3000;
  public static final int ECHO_SERVER_PORT1_1     = 2331;
  public static final int ECHO_SERVER_PORT1_2     = 2341;
  public static final int ECHO_SERVER_PORT2_1     = 2332;
  public static final int ECHO_SERVER_PORT2_2     = 2342;
  public static final int ECHO_SERVER_PORT3       = 2333;
  public static final int ECHO_SERVER_PORT4       = 2334;
  public static final int ECHO_SERVER_PORT5       = 2335;

  public static final String D2_CONFIG_DATA =
    "{" +
      "\"clusterDefaults\": {\"properties\":{\"requestTimeout\":\"10000\"}}," +
      "\"serviceDefaults\": { \"prioritizedSchemes\":[\"http\"], \"loadBalancerStrategyProperties\":{\"maxClusterLatencyWithoutDegrading\":\"500\", \"updateIntervalsMs\":\"5000\", \"defaultSuccessfulTransmissionWeight\":\"1.0\", \"pointsPerWeight\":\"100\"}, \"loadBalancerStrategyList\":[\"degraderV3\"]}," +
      "\"serviceVariants\":{}," +
      "\"extraClusterServiceConfigurations\":{}," +
      "\"clusterServiceConfigurations\":" +
      "{" +
        "\"cluster-1\":{\"services\":{\"service-1_1\":{\"path\":\"/service-1_1\"}, \"service-1_2\":{\"path\":\"/service-1_2\"}, \"service-1_3\":{\"path\":\"/service-1_3\"}}}," +
        "\"cluster-2\":{\"services\":{\"service-2_1\":{\"path\":\"/service-2_1\"}, \"service-2_2\":{\"path\":\"/service-2_2\"}, \"service-2_3\":{\"path\":\"/service-2_3\"}}}," +
        "\"cluster-3\":{\"services\":{\"service-3_1\":{\"path\":\"/service-3_1\"}, \"service-3_2\":{\"path\":\"/service-3_2\"}, \"service-3_3\":{\"path\":\"/service-3_3\"}}}," +
        "\"cluster-4\":{\"services\":{\"service-4_11\":{\"path\":\"/service-4_11\"}, \"service-4_12\":{\"path\":\"/service-4_12\"}, \"service-4_13\":{\"path\":\"/service-4_13\"}}, \"partitionProperties\":{\"partitionSize\":\"10\", \"keyRangeStart\":\"0\", \"partitionKeyRegex\":\"service-4_(\\\\d+)\", \"partitionType\":\"RANGE\", \"partitionCount\":\"10\"}}" +
      "}" +
    "}";

  public static final String D2_CONFIG_DEFAULT_PARTITION_DATA =
    "{" +
      "\"clusterDefaults\": { \"properties\":{\"requestTimeout\":\"10000\"}}," +
      "\"serviceDefaults\": {\"prioritizedSchemes\":[\"http\"], \"loadBalancerStrategyProperties\":{\"maxClusterLatencyWithoutDegrading\":\"500\", \"updateIntervalsMs\":\"5000\", \"defaultSuccessfulTransmissionWeight\":\"1.0\", \"pointsPerWeight\":\"100\"}, \"loadBalancerStrategyList\":[\"degraderV3\"]}," +
      "\"serviceVariants\":{}," +
      "\"extraClusterServiceConfigurations\":{}," +
      "\"clusterServiceConfigurations\":" +
      "{" +
        "\"cluster-1\":{\"services\":{\"service-1_1\":{\"path\":\"/service-1_1\"}, \"service-1_2\":{\"path\":\"/service-1_2\"}, \"service-1_3\":{\"path\":\"/service-1_3\"}}}," +
        "\"cluster-2\":{\"services\":{\"service-2_1\":{\"path\":\"/service-2_1\"}, \"service-2_2\":{\"path\":\"/service-2_2\"}, \"service-2_3\":{\"path\":\"/service-2_3\"}}}," +
        "\"cluster-3\":{\"services\":{\"service-3_1\":{\"path\":\"/service-3_1\"}, \"service-3_2\":{\"path\":\"/service-3_2\"}, \"service-3_3\":{\"path\":\"/service-3_3\"}}}" +
      "}" +
    "}";

  public static final String D2_CONFIG_CUSTOM_PARTITION_DATA =
    "{" +
      "\"clusterDefaults\": { \"properties\":{\"requestTimeout\":\"10000\"}}," +
      "\"serviceDefaults\": { \"prioritizedSchemes\":[\"http\"], \"loadBalancerStrategyProperties\":{\"maxClusterLatencyWithoutDegrading\":\"500\", \"updateIntervalsMs\":\"5000\", \"defaultSuccessfulTransmissionWeight\":\"1.0\", \"pointsPerWeight\":\"100\"}, \"loadBalancerStrategyList\":[\"degraderV3\"]}," +
      "\"serviceVariants\":{}," +
      "\"extraClusterServiceConfigurations\":{}," +
      "\"clusterServiceConfigurations\":" +
      "{" +
        "\"cluster-4\":{\"services\":{\"service-4_11\":{\"path\":\"/service-4_11\"}, \"service-4_12\":{\"path\":\"/service-4_12\"}, \"service-4_13\":{\"path\":\"/service-4_13\"}}, \"partitionProperties\":{\"partitionSize\":\"10\", \"keyRangeStart\":\"0\", \"partitionKeyRegex\":\"service-4_(\\\\d+)\", \"partitionType\":\"RANGE\", \"partitionCount\":\"10\"}}" +
      "}" +
    "}";

  public static final String D2_CONFIG_CUSTOM_PARTITION_DATA2 =
    "{" +
    "\"clusterDefaults\": {\"properties\":{\"requestTimeout\":\"10000\"}}," +
    "\"serviceDefaults\": { \"prioritizedSchemes\":[\"http\"], \"loadBalancerStrategyProperties\":{\"maxClusterLatencyWithoutDegrading\":\"500\", \"updateIntervalsMs\":\"5000\", \"defaultSuccessfulTransmissionWeight\":\"1.0\", \"pointsPerWeight\":\"100\"}, \"loadBalancerStrategyList\":[\"degraderV3\"]}," +
    "\"serviceVariants\":{}," +
    "\"extraClusterServiceConfigurations\":" +
    "{" +
      "\"cluster-4\":{\"services\":{\"service-4_11\":{\"path\":\"/service-4_11\"}, \"service-4_12\":{\"path\":\"/service-4_12\"}, \"service-4_13\":{\"path\":\"/service-4_13\"}}, \"partitionProperties\":{\"partitionSize\":\"10\", \"keyRangeStart\":\"0\", \"partitionKeyRegex\":\"service-4_(\\\\d+)\", \"partitionType\":\"RANGE\", \"partitionCount\":\"10\"}}" +
    "}," +
    "\"clusterServiceConfigurations\":{}" +
  "}";
}
