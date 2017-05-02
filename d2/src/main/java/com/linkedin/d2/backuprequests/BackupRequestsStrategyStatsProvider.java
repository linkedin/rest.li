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

/**
 * Provides stats for a {@link BackupRequestsStrategy}. It has two methods: {@link #getStats()} which returns stats
 * gathered from the time when the {@code BackupRequestsStrategy} was created and {@link #getDiffStats()} which
 * returns stats since last call to the {@code getDiffStats()}.
 *
 * @author Jaroslaw Odzga (jodzga@linkedin.com)
 *
 */
public interface BackupRequestsStrategyStatsProvider
{

  /**
   * Returns stats gathered since the last call to this method. If this method has not been called before then it
   * returns stats gathered since {@code BackupRequestsStrategy} was created.
   * @return stats gathered since the last call to this method
   */
  BackupRequestsStrategyStats getDiffStats();

  /**
   * Returns stats gathered from the time when the {@code BackupRequestsStrategy} was created.
   * @return stats gathered from the time when the {@code BackupRequestsStrategy} was created
   */
  BackupRequestsStrategyStats getStats();

}
