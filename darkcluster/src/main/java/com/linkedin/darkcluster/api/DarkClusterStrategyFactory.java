/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.darkcluster.api;

import com.linkedin.d2.DarkClusterConfig;

/**
 * The DarkClusterStrategyFactory is responsible for creating and maintaining the strategies needed for dark clusters. This involves refreshing
 * when darkClusterConfig changes are detected. This hides the lifecycle and maintenance of {@link DarkClusterStrategy} from users.
 */
public interface DarkClusterStrategyFactory
{
  /**
   * getOrCreate retrieves the {@link DarkClusterStrategy} corresponding to the darkClusterName. If it doesn't exist, create it
   * and return the new strategy. The darkClusterConfig is passed in so that it can be easily stored if needed.
   * @param darkClusterName darkClusterName to look up
   * @param darkClusterConfig darkClusterConfig to store, if needed.
   * @return {@link DarkClusterStrategy}
   */
  DarkClusterStrategy getOrCreate(String darkClusterName, DarkClusterConfig darkClusterConfig);

  /**
   * Do any actions necessary to start the DarkClusterStrategyFactory.
   */
  void start();

  /**
   * Do any actions necessary to stop the DarkClusterStrategyFactory.
   */
  void shutdown();
}
