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

package com.linkedin.d2.discovery.util;

import java.util.concurrent.atomic.AtomicLong;

public class Stats
{
  private final long                         _intervalMs;
  private AtomicLong                         _count;
  private volatile LoadBalancerStatsSnapshot _snapshot;

  public Stats(long intervalMs)
  {
    _intervalMs = intervalMs;
    _count = new AtomicLong();
    _snapshot = new LoadBalancerStatsSnapshot(System.currentTimeMillis(), 0);
  }

  public void inc()
  {
    snap();

    _count.incrementAndGet();
  }

  public long getCount()
  {
    snap();

    return _snapshot.getCount();
  }

  public void snap()
  {
    synchronized (_snapshot)
    {
      // TODO make this testable by allowing a clock to be passed in
      if (System.currentTimeMillis() - _snapshot.getLastUpdateMs() >= _intervalMs)
      {
        _snapshot =
            new LoadBalancerStatsSnapshot(System.currentTimeMillis(), _count.get());

        _count.set(0);
      }
    }
  }

  public class LoadBalancerStatsSnapshot
  {
    private long _lastUpdateMs;
    private long _count;

    public LoadBalancerStatsSnapshot(long lastUpdateMs, long count)
    {
      _lastUpdateMs = lastUpdateMs;
      _count = count;
    }

    public long getCount()
    {
      return _count;
    }

    public long getLastUpdateMs()
    {
      return _lastUpdateMs;
    }
  }
}
