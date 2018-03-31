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
 * Implementation of {@link LongTracker} that does not do any tracking for performance reasons.
 */
public class NoopLongTracker implements LongTracker
{
  private static final LongStats DEFAULT_STATS = new LongStats(0, 0.0D, 0.0D, 0L, 0L, 0L, 0L, 0L, 0L);
  private static final NoopLongTracker DEFAULT_INSTANCE = new NoopLongTracker();

  private NoopLongTracker()
  {
  }

  /**
   * Gets the default instance of {@link NoopLongTracker}. Since the implementation is stateless and non-blocking,
   * the instance can be shared.
   * @return the default shared instance of {@link NoopLongTracker}
   */
  public static NoopLongTracker instance()
  {
    return DEFAULT_INSTANCE;
  }

  @Override
  public void addValue(long value)
  {
  }

  @Override
  public LongStats getStats()
  {
    return DEFAULT_STATS;
  }

  @Override
  public void reset()
  {
  }
}
