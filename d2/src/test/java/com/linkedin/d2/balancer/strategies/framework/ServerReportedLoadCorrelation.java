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

/**
 * Define the correlation between server reported load, call count and time
 */
public interface ServerReportedLoadCorrelation
{
  /**
   * Given the requests per interval, calculate the server-reported load
   * @param requestsPerInterval the number of requests received in the interval
   * @param intervalIndex the index of the current interval since the test initialization
   * @return Expected server reported load
   */
  int getServerReportedLoad(int requestsPerInterval, int intervalIndex);
}
