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

/**
 * @author Swee Lim
 * @version $Rev$
 */

import java.util.Date;

public class DegraderControl implements DegraderControlMBean
{
  private final DegraderImpl _degrader;

  public DegraderControl(DegraderImpl degrader)
  {
    _degrader = degrader;
  }

  @Override
  public double getCurrentDropRate()
  {
    return _degrader.getStats().getCurrentDropRate();
  }

  @Override
  public double getCurrentComputedDropRate()
  {
    return _degrader.getStats().getCurrentComputedDropRate();
  }

  @Override
  public long getCurrentCountTotal()
  {
    return _degrader.getStats().getCurrentCountTotal();
  }

  @Override
  public long getCurrentNoOverrideDropCountTotal()
  {
    return _degrader.getStats().getCurrentNoOverrideDropCountTotal();
  }

  @Override
  public long getCurrentDroppedCountTotal()
  {
    return _degrader.getStats().getCurrentDroppedCountTotal();
  }

  @Override
  public Date getLastNotDroppedTime()
  {
    return new Date(_degrader.getStats().getLastNotDroppedTime());
  }

  @Override
  public long getInterval()
  {
    return _degrader.getStats().getInterval();
  }

  @Override
  public Date getIntervalEndTime()
  {
    return new Date(_degrader.getStats().getIntervalEndTime());
  }

  @Override
  public double getDroppedRate()
  {
    return _degrader.getStats().getDroppedRate();
  }

  @Override
  public int getCallCount()
  {
    return _degrader.getStats().getCallCount();
  }

  @Override
  public long getLatency()
  {
    return _degrader.getStats().getLatency();
  }

  @Override
  public double getErrorRate()
  {
    return _degrader.getStats().getErrorRate();
  }

  @Override
  public long getOutstandingLatency()
  {
    return _degrader.getStats().getOutstandingLatency();
  }

  @Override
  public int getOutstandingCount()
  {
    return _degrader.getStats().getOutstandingCount();
  }

  @Override
  public String getName()
  {
    return _degrader.getName();
  }

  @Override
  public boolean isLogEnabled()
  {
    return _degrader.getConfig().isLogEnabled();
  }

  @Override
  public String getLatencyToUse()
  {
    return _degrader.getConfig().getLatencyToUse().toString();
  }

  @Override
  public double getOverrideDropRate()
  {
    return _degrader.getConfig().getOverrideDropRate();
  }

  @Override
  public double getMaxDropRate()
  {
    return _degrader.getConfig().getMaxDropRate();
  }

  @Override
  public long getMaxDropDuration()
  {
    return _degrader.getConfig().getMaxDropDuration();
  }

  @Override
  public double getUpStep()
  {
    return _degrader.getConfig().getUpStep();
  }

  @Override
  public double getDownStep()
  {
    return _degrader.getConfig().getDownStep();
  }

  @Override
  public int getMinCallCount()
  {
    return _degrader.getConfig().getMinCallCount();
  }

  @Override
  public long getHighLatency()
  {
    return _degrader.getConfig().getHighLatency();
  }

  @Override
  public long getLowLatency()
  {
    return _degrader.getConfig().getLowLatency();
  }

  @Override
  public double getHighErrorRate()
  {
    return _degrader.getConfig().getHighErrorRate();
  }

  @Override
  public double getLowErrorRate()
  {
    return _degrader.getConfig().getLowErrorRate();
  }

  @Override
  public long getHighOutstanding()
  {
    return _degrader.getConfig().getHighOutstanding();
  }

  @Override
  public long getLowOutstanding()
  {
    return _degrader.getConfig().getLowOutstanding();
  }

  @Override
  public int getMinOutstandingCount()
  {
    return _degrader.getConfig().getMinOutstandingCount();
  }
  public int getOverrideMinCallCount()
  {
    return _degrader.getConfig().getOverrideMinCallCount();
  }

  @Override
  public void setLogEnabled(boolean logEnabled)
  {
    DegraderImpl.Config config = new DegraderImpl.Config(_degrader.getConfig());
    config.setLogEnabled(logEnabled);
    _degrader.setConfig(config);
  }

  @Override
  public void setLatencyToUse(String latencyToUse)
  {
    DegraderImpl.Config config = new DegraderImpl.Config(_degrader.getConfig());
    config.setLatencyToUse(DegraderImpl.LatencyToUse.valueOf(latencyToUse));
    _degrader.setConfig(config);
  }

  @Override
  public void setOverrideDropRate(double overrideDropRate)
  {
    DegraderImpl.Config config = new DegraderImpl.Config(_degrader.getConfig());
    config.setOverrideDropRate(overrideDropRate);
    _degrader.setConfig(config);
  }

  @Override
  public void setMaxDropRate(double maxDropRate)
  {
    DegraderImpl.Config config = new DegraderImpl.Config(_degrader.getConfig());
    config.setMaxDropRate(maxDropRate);
    _degrader.setConfig(config);
  }

  @Override
  public void setMaxDropDuration(long maxDropDuration)
  {
    DegraderImpl.Config config = new DegraderImpl.Config(_degrader.getConfig());
    config.setMaxDropDuration(maxDropDuration);
    _degrader.setConfig(config);
  }

  @Override
  public void setUpStep(double upStep)
  {
    DegraderImpl.Config config = new DegraderImpl.Config(_degrader.getConfig());
    config.setUpStep(upStep);
    _degrader.setConfig(config);
  }

  @Override
  public void setDownStep(double downStep)
  {
    DegraderImpl.Config config = new DegraderImpl.Config(_degrader.getConfig());
    config.setDownStep(downStep);
    _degrader.setConfig(config);
  }

  @Override
  public void setMinCallCount(int minCallCount)
  {
    DegraderImpl.Config config = new DegraderImpl.Config(_degrader.getConfig());
    config.setMinCallCount(minCallCount);
    _degrader.setConfig(config);
  }

  @Override
  public void setHighLatency(long highLatency)
  {
    DegraderImpl.Config config = new DegraderImpl.Config(_degrader.getConfig());
    config.setHighLatency(highLatency);
    _degrader.setConfig(config);
  }

  @Override
  public void setLowLatency(long lowLatency)
  {
    DegraderImpl.Config config = new DegraderImpl.Config(_degrader.getConfig());
    config.setLowLatency(lowLatency);
    _degrader.setConfig(config);
  }

  @Override
  public void setHighErrorRate(double highErrorRate)
  {
    DegraderImpl.Config config = new DegraderImpl.Config(_degrader.getConfig());
    config.setHighErrorRate(highErrorRate);
    _degrader.setConfig(config);
  }

  @Override
  public void setLowErrorRate(double lowErrorRate)
  {
    DegraderImpl.Config config = new DegraderImpl.Config(_degrader.getConfig());
    config.setLowErrorRate(lowErrorRate);
    _degrader.setConfig(config);
  }

  @Override
  public void setHighOutstanding(long highOutstanding)
  {
    DegraderImpl.Config config = new DegraderImpl.Config(_degrader.getConfig());
    config.setHighOutstanding(highOutstanding);
    _degrader.setConfig(config);
  }

  @Override
  public void setLowOutstanding(long lowOutstanding)
  {
    DegraderImpl.Config config = new DegraderImpl.Config(_degrader.getConfig());
    config.setLowOutstanding(lowOutstanding);
    _degrader.setConfig(config);
  }

  @Override
  public void setMinOutstandingCount(int minOutstandingCount)
  {
    DegraderImpl.Config config = new DegraderImpl.Config(_degrader.getConfig());
    config.setMinOutstandingCount(minOutstandingCount);
    _degrader.setConfig(config);
  }

  public void setOverrideMinCallCount(int overrideMinCallCount)
  {
    DegraderImpl.Config config = new DegraderImpl.Config(_degrader.getConfig());
    config.setOverrideMinCallCount(overrideMinCallCount);
    _degrader.setConfig(config);
  }

  @Override
  public void reset()
  {
    _degrader.reset();
  }
}

