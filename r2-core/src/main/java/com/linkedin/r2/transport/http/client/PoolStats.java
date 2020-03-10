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
 * @author Ang Xu
 * @version $Revision: $
 */
public interface PoolStats
{
  /**
   * Get the total number of pool objects created between
   * the starting of the Pool and the call to getStats().
   * Does not include create errors.
   * @return The total number of pool objects created
   */
  int getTotalCreated();

  /**
   * Get the total number of pool objects destroyed between
   * the starting of the Pool and the call to getStats().
   * Includes lifecycle validation failures, disposes,
   * and timed-out objects, but does not include destroy errors.
   * @return The total number of pool objects destroyed
   */
  int getTotalDestroyed();

  /**
   * Get the total number of lifecycle create errors between
   * the starting of the Pool and the call to getStats().
   * @return The total number of create errors
   */
  int getTotalCreateErrors();

  /**
   * Get the total number of lifecycle destroy errors between
   * the starting of the Pool and the call to getStats().
   * @return The total number of destroy errors
   */
  int getTotalDestroyErrors();

  /**
   * Get the total number of pool objects destroyed (or failed to
   * to destroy because of an error) because of disposes or failed
   * lifecycle validations between the starting of the Pool
   * and the call to getStats().
   * @return The total number of destroyed "bad" objects
   */
  int getTotalBadDestroyed();

  /**
   * Get the total number of timed out pool objects between the
   * starting of the Pool and the call to getStats().
   * @return The total number of timed out objects
   */
  int getTotalTimedOut();

  /**
   * Get the total number of timed out pool waiters between the
   * starting of the Pool and the call to getStats().
   * @return The total number of timed out objects
   */
  int getTotalWaiterTimedOut();

  /**
   * Get the total number of times the creation of objects ignored between the
   * starting of the Pool and the call to getStats().
   * @return The total number of times the object creation ignored
   */
  int getTotalCreationIgnored();

  /**
   * Get the number of pool objects checked out at the time of
   * the call to getStats().
   * @return The number of checked out pool objects
   */
  int getCheckedOut();

  /**
   * Get the configured maximum pool size.
   * @return The maximum pool size
   */
  int getMaxPoolSize();

  /**
   * Get the configured minimum pool size.
   * @return The minimum pool size
   */
  int getMinPoolSize();

  /**
   * Get the pool size at the time of the call to getStats().
   * @return The pool size
   */
  int getPoolSize();

  /**
   * Get the maximum number of checked out objects. Reset
   * after each call to getStats().
   * @return The maximum number of checked out objects
   */
  int getSampleMaxCheckedOut();

  /**
   * Get the maximum pool size. Reset after each call to
   * getStats().
   * @return The maximum pool size
   */
  int getSampleMaxPoolSize();

  /**
   * Get the maximum waiting time of pool requests
   * @return The maximum wait time
   */
  default long getSampleMaxWaitTime()
  {
    return 0;
  }

  /**
   * Get the number of objects that are idle(not checked out)
   * in the pool.
   * @return The number of idle objects
   */
  int getIdleCount();

  /**
   * Get the average wait time to get a pooled object.
   * @return The average wait time.
   */
  double getWaitTimeAvg();

  /**
   * Get the 50 percentage wait time to get a pooled object.
   * @return 50 percentage wait time.
   */
  long getWaitTime50Pct();

  /**
   * Get the 95 percentage wait time to get a pooled object.
   * @return 95 percentage wait time.
   */
  long getWaitTime95Pct();

  /**
   * Get the 99 percentage wait time to get a pooled object.
   * @return 99 percentage wait time.
   */
  long getWaitTime99Pct();

  /**
   * Get stats collected from {@link AsyncPool.Lifecycle}
   * @return Lifecycle stats
   */
  LifecycleStats getLifecycleStats();

  /**
   * Return a string which represents the pool stats
   */
  String toString();

  public interface LifecycleStats
  {
    /**
     * Get the average time to create an object.
     * @return The average create time.
     */
    double getCreateTimeAvg();

    /**
     * Get the 50 percentage time to create an object.
     * @return 50 percentage create time.
     */
    long getCreateTime50Pct();

    /**
     * Get the 95 percentage time to create an object.
     * @return 95 percentage create time.
     */
    long getCreateTime95Pct();

    /**
     * Get the 99 percentage time to create an object.
     * @return 99 percentage create time.
     */
    long getCreateTime99Pct();
  }
}
