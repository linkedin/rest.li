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

public class AsyncPoolStats {
  private final int _totalCreated;
  private final int _totalDestroyed;
  private final int _totalCreateErrors;
  private final int _totalDestroyErrors;
  private final int _totalBadDestroyed;
  private final int _totalTimedOut;

  private final int _checkedOut;
  private final int _maxPoolSize;
  private final int _minPoolSize;
  private final int _poolSize;

  private final int _sampleMaxCheckedOut;
  private final int _sampleMaxPoolSize;

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

      int checkedOut,
      int maxPoolSize,
      int minPoolSize,
      int poolSize,

      int sampleMaxCheckedOut,
      int sampleMaxPoolSize
  )
  {
    _totalCreated = totalCreated;
    _totalDestroyed = totalDestroyed;
    _totalCreateErrors = totalCreateErrors;
    _totalDestroyErrors = totalDestroyErrors;
    _totalBadDestroyed = totalBadDestroyed;
    _totalTimedOut = totalTimedOut;

    _checkedOut = checkedOut;
    _maxPoolSize = maxPoolSize;
    _minPoolSize = minPoolSize;
    _poolSize = poolSize;

    _sampleMaxCheckedOut = sampleMaxCheckedOut;
    _sampleMaxPoolSize = sampleMaxPoolSize;
  }

  /**
   * Get the total number of pool objects created between
   * the starting of the AsyncPool and the call to getStats().
   * Does not include create errors.
   * @return The total number of pool objects created
   */
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
  public int getTotalDestroyed()
  {
    return _totalDestroyed;
  }

  /**
   * Get the total number of lifecycle create errors between
   * the starting of the AsyncPool and the call to getStats().
   * @return The total number of create errors
   */
  public int getTotalCreateErrors()
  {
    return _totalCreateErrors;
  }

  /**
   * Get the total number of lifecycle destroy errors between
   * the starting of the AsyncPool and the call to getStats().
   * @return The total number of destroy errors
   */
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
  public int getTotalBadDestroyed()
  {
    return _totalBadDestroyed;
  }

  /**
   * Get the total number of timed out pool objects between the
   * starting of the AsyncPool and the call to getStats().
   * @return The total number of timed out objects
   */
  public int getTotalTimedOut()
  {
    return _totalTimedOut;
  }

  /**
   * Get the number of pool objects checked out at the time of
   * the call to getStats().
   * @return The number of checked out pool objects
   */
  public int getCheckedOut()
  {
    return _checkedOut;
  }

  /**
   * Get the configured maximum pool size.
   * @return The maximum pool size
   */
  public int getMaxPoolSize()
  {
    return _maxPoolSize;
  }

  /**
   * Get the configured minimum pool size.
   * @return The minimum pool size
   */
  public int getMinPoolSize()
  {
    return _minPoolSize;
  }

  /**
   * Get the pool size at the time of the call to getStats().
   * @return The pool size
   */
  public int getPoolSize()
  {
    return _poolSize;
  }

  /**
   * Get the maximum number of checked out objects. Reset
   * after each call to getStats().
   * @return The maximum number of checked out objects
   */
  public int getSampleMaxCheckedOut()
  {
    return _sampleMaxCheckedOut;
  }

  /**
   * Get the maximum pool size. Reset after each call to
   * getStats().
   * @return The maximum pool size
   */
  public int getSampleMaxPoolSize()
  {
    return _sampleMaxPoolSize;
  }
}
