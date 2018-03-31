/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.common.stats;

/**
 * Maintain a collection of values and provide the count, average, standard deviation,
 * minimum, maximum, percentile values for the collection.
 */
public interface LongTracker
{
  /**
   * Adds a {@code long} value to be tracked.
   * @param value Value to track
   */
  void addValue(long value);

  /**
   * Gets the results in the form of {@link LongStats} in the past tracking period.
   * @return {@link LongStats} collected
   */
  LongStats getStats();

  /**
   * Resets the tracking states.
   */
  void reset();
}
