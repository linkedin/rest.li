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

/**
 * Interface for determining if request should be dropped to shed load.
 */
public interface Degrader
{
  /**
   * Returns the name of the degrader.
   * @return the name of the degrader.
   */
  String getName();

  /**
   * Determine if a request should be dropped to reduce load.
   *
   * This depends on a code with a decimal value from 0 (inclusive) to 1.0 (exclusive).
   * This code should have a uniform distribution. Otherwise, load shedding behavior
   * will not be linear.
   *
   * This code is compared with the desired drop rate which is also a value from 0 (inclusive)
   * to 1 (exclusive). The desired drop rate provided by the implementation class.
   * If the code's value is less than the desired drop rate, then this method will
   * return true indicating that this request should be dropped.
   *
   * The primary use for code is to allow the same user request to be dropped throughout
   * the call tree.
   *
   * @param code a value from 0 (inclusive) to 1 (exclusive).
   * @return whether the request should be dropped to reduce load.
   */
  boolean checkDrop(double code);

  /**
   * Same as checkDrop but uses internally generated code, usually a random number.
   *
   * @return whether the request should be dropped to reduce load.
   */
  boolean checkDrop();

  /**
   * Determines if a request should be timed out earlier in order to release resources acquired
   * by both the underlying transport client and application logic in order to make the client more
   * durable in the event of downstream latency and timeout.
   *
   * @return {@code true} if request should be timeout early; {@code false} otherwise.
   */
  boolean checkPreemptiveTimeout();
}
