/*
   Copyright (c) 2021 LinkedIn Corp.

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

package com.linkedin.d2.balancer.strategies.framework;

import java.net.URI;


/**
 * Defines the latency for a particular host in an interval based on user definition
 */
interface ServerLoadScoreManager
{

  /**
   * Given an interval, calculate the server-reported load score for a host
   *
   * @param uri The uri of the server host
   * @param hostRequestCount The request count the host received in the last interval
   * @param intervalIndex The index of the current interval
   * @return The expected server reported overload score
   */
  int getServerLoadScore(URI uri, int hostRequestCount, int intervalIndex);
}
