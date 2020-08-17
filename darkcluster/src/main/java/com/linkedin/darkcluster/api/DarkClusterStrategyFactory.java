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
   * get retrieves the {@link DarkClusterStrategy} corresponding to the darkClusterName.
   * @param darkClusterName darkClusterName to look up
   * @return {@link DarkClusterStrategy}
   */
  DarkClusterStrategy get(String darkClusterName);

  /**
   * Do any actions necessary to start the DarkClusterStrategyFactory.
   */
  void start();

  /**
   * Do any actions necessary to stop the DarkClusterStrategyFactory.
   */
  void shutdown();
}
