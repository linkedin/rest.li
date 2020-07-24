/*
   Copyright (c) 2020 LinkedIn Corp.

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
 * Create different number of requests in each interval based on user defined correlation formula
 */
class DynamicRequestCountManager implements RequestCountManager
{
  private final RequestCountCorrelation _requestCountCorrelation;

  DynamicRequestCountManager(RequestCountCorrelation requestCountCorrelation)
  {
    _requestCountCorrelation = requestCountCorrelation;
  }

  @Override
  public int getRequestCount(int intervalIndex)
  {
    return _requestCountCorrelation.getRequestCount(intervalIndex);
  }
}
