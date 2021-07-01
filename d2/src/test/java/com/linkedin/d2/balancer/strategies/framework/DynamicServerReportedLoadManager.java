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

import com.linkedin.util.degrader.CallTrackerImpl;
import java.net.URI;
import java.util.Map;


/**
 * Create dynamic server reported load using the QPS and current interval correlation
 */
class DynamicServerReportedLoadManager implements ServerReportedLoadManager
{
  private final Map<URI, ServerReportedLoadCorrelation> _serverLoadScoreCalculationMap;

  DynamicServerReportedLoadManager(Map<URI, ServerReportedLoadCorrelation> serverLoadScoreCalculationMap)
  {
    _serverLoadScoreCalculationMap = serverLoadScoreCalculationMap;
  }

  @Override
  public int getServerReportedLoad(URI uri, int hostRequestCount, int intervalIndex)
  {
    if (_serverLoadScoreCalculationMap.containsKey(uri))
    {
      return _serverLoadScoreCalculationMap.get(uri).getServerReportedLoad(hostRequestCount, intervalIndex);
    }
    return CallTrackerImpl.DEFAULT_SERVER_REPORTED_LOAD;
  }
}