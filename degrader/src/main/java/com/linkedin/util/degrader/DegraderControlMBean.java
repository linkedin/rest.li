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

/**
 * $Id$
 */

package com.linkedin.util.degrader;

import com.linkedin.common.stats.LongStats;
import java.util.Date;

/**
 * @author Swee Lim
 * @version $Rev$
 */

public interface DegraderControlMBean
{
  double getOverrideDropRate();
  void setOverrideDropRate(double overrideDropRate);
  String getName();

  // Current status attributes
  double getCurrentDropRate();
  double getCurrentComputedDropRate();
  long   getCurrentCountTotal();
  long   getCurrentNoOverrideDropCountTotal();
  long   getCurrentDroppedCountTotal();
  Date   getLastNotDroppedTime();
  long   getInterval();
  Date   getIntervalEndTime();
  double getDroppedRate();
  int    getCallCount();
  long   getLatency();
  double getErrorRate();
  long   getOutstandingLatency();
  int    getOutstandingCount();
  default boolean isHigh()
  {
    return false;
  }
  default boolean isLow()
  {
    return false;
  }
  default LongStats getCallTimeStats() { return new LongStats();}

  // Control attributes

  boolean isLogEnabled();
  String getLatencyToUse();
  double getMaxDropRate();
  long getMaxDropDuration();
  double getUpStep();
  double getDownStep();
  int getMinCallCount();
  long getHighLatency();
  long getLowLatency();
  double getHighErrorRate();
  double getLowErrorRate();
  long getHighOutstanding();
  long getLowOutstanding();
  int getMinOutstandingCount();
  default double getInitialDropRate() { return 0.0;}
  default double getLogThreshold() { return 0.0; }
  double getPreemptiveRequestTimeoutRate();

  void setLogEnabled(boolean logEnabled);
  void setLatencyToUse(String latencyToUse);
  void setMaxDropRate(double maxDropRate);
  void setMaxDropDuration(long maxDropDuration);
  void setUpStep(double upStep);
  void setDownStep(double downStep);
  void setMinCallCount(int minCallCount);
  void setHighLatency(long highLatency);
  void setLowLatency(long lowLatency);
  void setHighErrorRate(double highErrorRate);
  void setLowErrorRate(double lowErrorRate);
  void setHighOutstanding(long highOutstanding);
  void setLowOutstanding(long lowOutstanding);
  void setMinOutstandingCount(int minOutstandingCount);
  default void setLogThreshold(double threshold) {};
  void setPreemptiveRequestTimeoutRate(double preemptiveRequestTimeoutRate);

  void reset();
}

