/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.r2.disruptor;

/**
 * Types of disrupt that R2 can induce to a request and response
 *
 * @author Sean Sheng
 * @version $Revision$
 */
public enum DisruptMode
{
  /**
   * Artificial delay added that can potentially cause a request to timeout.
   */
  DELAY,

  /**
   * No response is returned from the service. Request will timeout.
   */
  TIMEOUT,

  /**
   * Simulated status code and exceptions thrown to the client.
   */
  ERROR,

  /**
   * If the round trip takes less time than the specified delay, a delay will be added.
   */
  MINIMUM_DELAY
}
