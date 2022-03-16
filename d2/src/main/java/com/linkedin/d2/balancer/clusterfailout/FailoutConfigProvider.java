/*
   Copyright (c) 2022 LinkedIn Corp.

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
package com.linkedin.d2.balancer.clusterfailout;

/**
 * Class responsible for providing failout config for each cluster.
 */
public interface FailoutConfigProvider
{
  /**
   * Gets the failout config for a cluster.
   * @param clusterName The name of the cluster to get failout config for.
   * @return Corresponding failout config if cluster has an associated failed out config.
   */
  FailoutConfig getFailoutConfig(String clusterName);

  /**
   * Optional step for starting the config provider
   */
  default void start() {

  }

  /**
   * Optional step for shutting down the config provider
   */
  default void shutdown() {

  }
}
