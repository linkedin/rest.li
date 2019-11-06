/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.server;

import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.util.finalizer.RequestFinalizerDispatcher;


/**
 * Creates instances of {@link HttpDispatcher}.
 *
 * @author Chris Zhang
 */
public class HttpDispatcherFactory
{

  private HttpDispatcherFactory()
  {
    // Can't be instantiated.
  }

  /**
   * Creates an instance {@link HttpDispatcher} with the given {@link TransportDispatcher}.
   *
   * @param transportDispatcher Given TransportDispatcher.
   * @return HttpDispatcher.
   */
  @SuppressWarnings("deprecation")
  public static HttpDispatcher create(TransportDispatcher transportDispatcher)
  {
    return new HttpDispatcher(new RequestFinalizerDispatcher(transportDispatcher));
  }
}
