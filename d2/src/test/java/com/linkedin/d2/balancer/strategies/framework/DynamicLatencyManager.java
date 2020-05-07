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

package com.linkedin.d2.balancer.strategies.framework;

import java.net.URI;
import java.util.Map;


/**
 * Create dynamic latency using the QPS and current interval correlation
 */
class DynamicLatencyManager implements LatencyManager
{
  private final Map<URI, LatencyCorrelation> _latencyCalculationMap;

  DynamicLatencyManager(Map<URI, LatencyCorrelation> latencyCalculationMap)
  {
    _latencyCalculationMap = latencyCalculationMap;
  }

  @Override
  public long getLatency(URI uri, int hostRequestCount, int intervalIndex)
  {
    return _latencyCalculationMap.get(uri).getLatency(hostRequestCount, intervalIndex);
  }
}