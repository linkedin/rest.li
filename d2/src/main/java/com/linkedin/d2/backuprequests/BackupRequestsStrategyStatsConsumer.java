/*
   Copyright (c) 2017 LinkedIn Corp.

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
package com.linkedin.d2.backuprequests;

import org.HdrHistogram.AbstractHistogram;

/**
 * Allows consumption of BackupRequestsStats. Consumer will be called every time the new stats provider is added or
 * removed. For example, when backup requests strategy gets re-created (e.g. its configuration is changed) consumer
 * receives sequence of calls: {@link #removeStatsProvider(String, String, BackupRequestsStrategyStatsProvider)},
 * {@link #addStatsProvider(String, String, BackupRequestsStrategyStatsProvider)}.
 * <p>
 * All calls to the consumer are made sequentially.
 *
 * @author Jaroslaw Odzga (jodzga@linkedin.com)
 *
 */
public interface BackupRequestsStrategyStatsConsumer
{

  /**
   * This method is called when new instance of {@link BackupRequestsStrategyStatsProvider} is created.
   * <p>
   * Implementation of this method has to be thread safe.
   * @param service
   * @param operation
   * @param statsProvider
   */
  void addStatsProvider(String service, String operation, BackupRequestsStrategyStatsProvider statsProvider);

  /**
   * This method is called when an instance of {@link BackupRequestsStrategyStatsProvider} is removed.
   * <p>
   * Implementation of this method has to be thread safe.
   * @param service
   * @param operation
   * @param statsProvider
   */
  void removeStatsProvider(String service, String operation, BackupRequestsStrategyStatsProvider statsProvider);

  /**
   * This method is called when there is a new latency histogram available. Histogram contains only information
   * about requests that happened since last time this method was called. This method is called at least once per
   * minute but it may be called more often when there is high QPS.
   * <p>
   * This method can not cache reference to the histogram because it will be reused for future recording. More
   * specifically very soon after this method returns {@code histogram.reset()} will be called.
   * <p>
   * Implementation of this method does not have to be thread safe.
   * @param service
   * @param operation
   * @param histogram
   * @param withBackup
   */
  void latencyUpdate(String service, String operation, AbstractHistogram histogram, boolean withBackup);
}
