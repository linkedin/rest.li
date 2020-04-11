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

package com.linkedin.darkcluster.api;

import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;

/**
 * Dummy implementation of DarkClusterStrategy. This can be used in tests safely.
 * requestSent is what this class should return on each invocation of handleRequest. Tests
 * may want to pretend that a strategy was returned, but if this get's used in production,
 * a requestSent value of false is more correct.
 */
public class NoOpDarkClusterStrategy implements DarkClusterStrategy
{
  /**
   *
   */
  private final boolean _requestSent;

  public NoOpDarkClusterStrategy()
  {
    this(false);
  }

  public NoOpDarkClusterStrategy(boolean requestSent)
  {
    _requestSent = requestSent;
  }

  @Override
  public boolean handleRequest(RestRequest originalRequest, RestRequest darkRequest, RequestContext requestContext)
  {
    return _requestSent;
  }
}
