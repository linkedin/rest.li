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

/* $Id$ */
package com.linkedin.r2.transport.common.bridge.server;


import com.linkedin.r2.transport.common.RestRequestHandler;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Builder for {@link TransportDispatcher} instances.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public class TransportDispatcherBuilder
{
  private final Map<URI, RestRequestHandler> _restHandlers = new HashMap<URI, RestRequestHandler>();

  /**
   * Add a {@link RestRequestHandler} at the specified URI.
   *
   * @param uri the URI at which the handler is bound.
   * @param handler the handler to bind to the specified URI.
   * @return the current Builder object (fluent interface pattern).
   */
  public TransportDispatcherBuilder addRestHandler(URI uri, RestRequestHandler handler)
  {
    _restHandlers.put(uri, handler);
    return this;
  }

  /**
   * Remove any {@link RestRequestHandler} bound to the specified URI.
   *
   * @param uri the URI for which the handler should be removed.
   * @return the {@link RestRequestHandler} which was removed, or null if no handler
   *         exists.
   */
  public RestRequestHandler removeRestHandler(URI uri)
  {
    return _restHandlers.remove(uri);
  }

  /**
   * Reset the state of this builder to its initial state.
   *
   * @return the current builder object (fluent interface pattern).
   */
  public TransportDispatcherBuilder reset()
  {
    _restHandlers.clear();
    return this;
  }

  /**
   * Build a new {@link TransportDispatcher} using the settings of this builder.
   *
   * @return a new {@link TransportDispatcher} using the settings of this builder.
   */
  public TransportDispatcher build()
  {
    return new TransportDispatcherImpl(copy(_restHandlers));
  }

  private <T> Map<URI, T> copy(Map<URI, T> handlers)
  {
    return new HashMap<URI, T>(handlers);
  }
}
