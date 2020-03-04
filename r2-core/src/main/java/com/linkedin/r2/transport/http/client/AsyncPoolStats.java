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
 * $Id: $
 */

package com.linkedin.r2.transport.http.client;

/**
 * This class contains various statistics from a single AsyncPool.
 * Can be retrieved from an AsyncPool with a call to getStats().
 *
 * @author Douglas Chen
 * @version $Revision: $
 */

public class AsyncPoolStats implements PoolStats
{
  private final int _totalCreated;
  private final int _totalDestroyed;
  private final int _totalCreateErrors;
  private final int _totalDestroyErrors;
  private final int _totalBadDestroyed;
  private final int _totalTimedOut;
  private final int _totalWaiterTimedOut;
  private final int _totalCreationIgnored;

  private final int _checkedOut;
  private final int _maxPoolSize;
  private final int _minPoolSize;
  private final int _poolSize;

  private final int _sampleMaxCheckedOut;
  private final int _sampleMaxPoolSize;
  private final long _sampleMaxWaitTime;

  private final int _idleCount;
  private final double _waitTimeAvg;
  private final long _waitTime50Pct;
  private final long _waitTime95Pct;
  private final long _waitTime99Pct;
  private final LifecycleStats _lifecycleStats;

  /**
   * This class should be instantiated through a call to
   * getStats() on an AsyncPool.
   */
  public AsyncPoolStats(
      int totalCreated,
      int totalDestroyed,
      int totalCreateErrors,
      int totalDestroyErrors,
      int totalBadDestroyed,
      int totalTimedOut,
      int totalWaiterTimedOut,
      int totalCreationsIgnored,

      int checkedOut,
      int maxPoolSize,
      int minPoolSize,
      int poolSize,

      int sampleMaxCheckedOut,
      int sampleMaxPoolSize,
      long sampleMaxWaitTime,

      int idleCount,
      double waitTimeAvg,
      long waitTime50Pct,
      long waitTime95Pct,
      long waitTime99Pct,
      LifecycleStats lifecycleStats
  )
  {
    _totalCreated = totalCreated;
    _totalDestroyed = totalDestroyed;
    _totalCreateErrors = totalCreateErrors;
    _totalDestroyErrors = totalDestroyErrors;
    _totalBadDestroyed = totalBadDestroyed;
    _totalTimedOut = totalTimedOut;
    _totalCreationIgnored = totalCreationsIgnored;
    _totalWaiterTimedOut = totalWaiterTimedOut;

    _checkedOut = checkedOut;
    _maxPoolSize = maxPoolSize;
    _minPoolSize = minPoolSize;
    _poolSize = poolSize;

    _sampleMaxCheckedOut = sampleMaxCheckedOut;
    _sampleMaxPoolSize = sampleMaxPoolSize;
    _sampleMaxWaitTime = sampleMaxWaitTime;

    _idleCount = idleCount;
    _waitTimeAvg = waitTimeAvg;
    _waitTime50Pct = waitTime50Pct;
    _waitTime95Pct = waitTime95Pct;
    _waitTime99Pct = waitTime99Pct;
    _lifecycleStats = lifecycleStats;
  }

  /**
   * Get the total number of pool objects created between
   * the starting of the AsyncPool and the call to getStats().
   * Does not include create errors.
   * @return The total number of pool objects created
   */
  @Override
  public int getTotalCreated()
  {
    return _totalCreated;
  }

  /**
   * Get the total number of pool objects destroyed between
   * the starting of the AsyncPool and the call to getStats().
   * Includes lifecycle validation failures, disposes,
   * and timed-out objects, but does not include destroy errors.
   * @return The total number of pool objects destroyed
   */
  @Override
  public int getTotalDestroyed()
  {
    return _totalDestroyed;
  }

  /**
   * Get the total number of lifecycle create errors between
   * the starting of the AsyncPool and the call to getStats().
   * @return The total number of create errors
   */
  @Override
  public int getTotalCreateErrors()
  {
    return _totalCreateErrors;
  }

  /**
   * Get the total number of lifecycle destroy errors between
   * the starting of the AsyncPool and the call to getStats().
   * @return The total number of destroy errors
   */
  @Override
  public int getTotalDestroyErrors()
  {
    return _totalDestroyErrors;
  }

  /**
   * Get the total number of pool objects destroyed (or failed to
   * to destroy because of an error) because of disposes or failed
   * lifecycle validations between the starting of the AsyncPool
   * and the call to getStats().
   * @return The total number of destroyed "bad" objects
   */
  @Override
  public int getTotalBadDestroyed()
  {
    return _totalBadDestroyed;
  }

  /**
   * Get the total number of timed out pool objects between the
   * starting of the AsyncPool and the call to getStats().
   * @return The total number of timed out objects
   */
  @Override
  public int getTotalTimedOut()
  {
    return _totalTimedOut;
  }

  /**
   * Get the total number of timed out pool waiters between the
   * starting of the Pool and the call to getStats().
   * @return The total number of timed out objects
   */
  @Override
  public int getTotalWaiterTimedOut()
  {
    return _totalWaiterTimedOut;
  }
  /**
   * Get the total number of times the object creation ignored between the
   * starting of the AsyncPool and the call to getStats().
   * @return The total number of times the object creation ignored
   */
  @Override
  public int getTotalCreationIgnored()
  {
    return _totalCreationIgnored;
  }

  /**
   * Get the number of pool objects checked out at the time of
   * the call to getStats().
   * @return The number of checked out pool objects
   */
  @Override
  public int getCheckedOut()
  {
    return _checkedOut;
  }

  /**
   * Get the configured maximum pool size.
   * @return The maximum pool size
   */
  @Override
  public int getMaxPoolSize()
  {
    return _maxPoolSize;
  }

  /**
   * Get the configured minimum pool size.
   * @return The minimum pool size
   */
  @Override
  public int getMinPoolSize()
  {
    return _minPoolSize;
  }

  /**
   * Get the pool size at the time of the call to getStats().
   * @return The pool size
   */
  @Override
  public int getPoolSize()
  {
    return _poolSize;
  }

  /**
   * Get the maximum number of checked out objects. Reset
   * after each call to getStats().
   * @return The maximum number of checked out objects
   */
  @Override
  public int getSampleMaxCheckedOut()
  {
    return _sampleMaxCheckedOut;
  }

  /**
   * Get the maximum pool size. Reset after each call to
   * getStats().
   * @return The maximum pool size
   */
  @Override
  public int getSampleMaxPoolSize()
  {
    return _sampleMaxPoolSize;
  }

  @Override
  public long getSampleMaxWaitTime()
  {
    return _sampleMaxWaitTime;
  }

  /**
   * Get the number of objects that are idle(not checked out)
   * in the pool.
   * @return The number of idle objects
   */
  @Override
  public int getIdleCount()
  {
    return _idleCount;
  }

  /**
   * Get the average wait time to get a pooled object.
   * @return The average wait time.
   */
  @Override
  public double getWaitTimeAvg()
  {
    return _waitTimeAvg;
  }

  /**
   * Get the 50 percentage wait time to get a pooled object.
   * @return 50 percentage wait time.
   */
  @Override
  public long getWaitTime50Pct()
  {
    return _waitTime50Pct;
  }

  /**
   * Get the 95 percentage wait time to get a pooled object.
   * @return 95 percentage wait time.
   */
  @Override
  public long getWaitTime95Pct()
  {
    return _waitTime95Pct;
  }

  /**
   * Get the 99 percentage wait time to get a pooled object.
   * @return 99 percentage wait time.
   */
  @Override
  public long getWaitTime99Pct()
  {
    return _waitTime99Pct;
  }

  /**
   * Get stats collected from {@link AsyncPool.Lifecycle}
   * @return Lifecycle stats
   */
  @Override
  public LifecycleStats getLifecycleStats()
  {
    return _lifecycleStats;
  }

  @Override
  public String toString()
  {
    return "\ntotalCreated: " + _totalCreated +
        "\ntotalDestroyed: " + _totalDestroyed +
        "\ntotalCreateErrors: " + _totalCreateErrors +
        "\ntotalDestroyErrors: " + _totalDestroyErrors +
        "\ntotalBadDestroyed: " + _totalBadDestroyed +
        "\ntotalTimeOut: " + _totalTimedOut +
        "\ntotalWaiterTimedOut: " + _totalWaiterTimedOut +
        "\ncheckedOut: " + _totalTimedOut +
        "\nmaxPoolSize: " + _maxPoolSize +
        "\npoolSize: " + _poolSize +
        "\nsampleMaxCheckedOut: " + _sampleMaxCheckedOut +
        "\nsampleMaxPoolSize: " + _sampleMaxPoolSize +
        "\nidleCount: " + _idleCount +
        "\nwaitTimeAvg: " + _waitTimeAvg +
        "\nwaitTime50Pct: " + _waitTime50Pct +
        "\nwaitTime95Pct: " + _waitTime95Pct +
        "\nwaitTime99Pct: " + _waitTime99Pct +
        (_lifecycleStats != null ? _lifecycleStats.toString() : "");
  }
}
