/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.r2.message.timing;

/**
 * Enables {@link TimingKey} prioritization by specifying different levels of importance.
 * This provides the ability to filter keys by different timing levels.
 */
public enum TimingImportance
{
  /**
   * For low priority timings that a user will rarely be interested in. Often these will be
   * very specific timings such as DNS resolution or individual filters.
   */
  LOW (0),

  /**
   * For medium priority timings that a user may be interested in, offered as an extra level
   * between low and high.
   */
  MEDIUM (1),

  /**
   * These timings are of the highest priority and is intended for key measurements such as
   * total infrastructure latency.
   */
  HIGH (2);

  private int _level;

  TimingImportance(int level)
  {
    _level = level;
  }

  public boolean isAtLeast(TimingImportance other)
  {
    return _level >= other._level;
  }
}
