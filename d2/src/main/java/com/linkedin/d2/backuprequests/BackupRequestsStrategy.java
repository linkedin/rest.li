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

import java.util.Optional;

/**
 * This interface defines a backup requests strategy. Instance of a {@code BackupRequestsStrategy}
 * must be used in the following way:
 * <ol>
 * <li>{@link #getTimeUntilBackupRequestNano()} has to be called every time the outgoing request is made. It returns number
 * of nanoseconds until backup request should be considered (see 2).</li>
 * <li>After waiting amount of time returned by {@code getTimeUntilBackupRequestNano()}, if response has not been
 * received yet, {@link #isBackupRequestAllowed()} is called to make the final decision whether to make a backup
 * request.</li>
 * <li>{@code BackupRequestsStrategy} is notified about every response time using
 * {@link #recordCompletion(long)} method.</li>
 * </ol>
 * <p>
 * Implementation of {@code BackupRequestsStrategy} has to be thread safe and can be instantiated multiple times
 * whenever backup requests configuration is changed.
 *
 * @author Jaroslaw Odzga (jodzga@linkedin.com)
 */
public interface BackupRequestsStrategy
{
  /**
   * Returns time to wait before sending a backup requests in nanoseconds.
   * @return time to wait before sending a backup requests in nanoseconds
   */
  Optional<Long> getTimeUntilBackupRequestNano();

  /**
   * Records request's response time. This is an information that feeds backup requests strategy.
   * @param responseTime response time in nanoseconds
   */
  void recordCompletion(long responseTime);

  /**
   * Returns true if backup request is supposed to be made.
   * This method is called when a backup request is about to be made.
   * It should not be called if original request has already completed.
   *
   * @return true if backup request is supposed to be made
   */
  boolean isBackupRequestAllowed();
}
