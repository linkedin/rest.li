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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.r2.transport.common.RpcRequestHandler;

/**
 * Builder for {@link TransportDispatcher} instances.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public class TransportDispatcherBuilder
{
  private final Map<URI, RpcRequestHandler> _rpcHandlers = new HashMap<URI, RpcRequestHandler>();
  private final Map<URI, RestRequestHandler> _restHandlers = new HashMap<URI, RestRequestHandler>();

  /**
   * Add an {@link RpcRequestHandler} for the specified URI.
   *
   * @deprecated R2 RPC is not supported. Please use REST instead.
   * @param uri the URI at which to bind the handler.
   * @param handler the handler to be bound at the specified URI.
   * @return the current Builder object, for fluent interface chaining.
   */
  @Deprecated
  public TransportDispatcherBuilder addRpcHandler(URI uri, RpcRequestHandler handler)
  {
    _rpcHandlers.put(uri, handler);
    return this;
  }

  /**
   * Add bindings for multiple RPC RequestHandlers that have a common dispatch prefix.
   *
   * The input arguments are a common URI prefix and a map of suffixes to RPC
   * RequestHandlers. The dispatch key for each entry in the map will be specified URI
   * prefix followed by the suffix provided by key the entry. If the URI prefix does not
   * end with a "/", a "/" will be inserted to separate the URI prefix from suffix.
   *
   * @deprecated R2 RPC is not supported. Please use REST instead.
   * @param uriPrefix provides the common URI prefix.
   * @param map whose keys provide the dispatch key suffixes and values provide the RPC
   *          RequestHandlers associated with the dispatch keys created from the specified
   *          URI prefix and map's keys.
   * @return this.
   */
  @Deprecated
  public TransportDispatcherBuilder addRpcHandler(URI uriPrefix,
                                                  Map<String, RpcRequestHandler> map)
  {
    String uriPrefixStr = uriPrefix.toString();
    if (!uriPrefixStr.endsWith("/"))
    {
      uriPrefixStr += "/";
    }
    for (Map.Entry<String, RpcRequestHandler> entry : map.entrySet())
    {
      URI handlerUri = URI.create(uriPrefixStr + entry.getKey());
      _rpcHandlers.put(handlerUri, entry.getValue());
    }
    return this;
  }

  /**
   * Remove the handler at the specified URI.
   *
   * @deprecated R2 RPC is not supported. Please use REST instead.
   * @param uri the URI for which the handler should be removed.
   * @return the {@link RpcRequestHandler} which was removed, or null if no handler
   *         exists.
   */
  @Deprecated
  public RpcRequestHandler removeRpcHandler(URI uri)
  {
    return _rpcHandlers.remove(uri);
  }

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
    _rpcHandlers.clear();
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
    return new TransportDispatcherImpl(copy(_rpcHandlers), copy(_restHandlers));
  }

  private <T> Map<URI, T> copy(Map<URI, T> handlers)
  {
    return new HashMap<URI, T>(handlers);
  }
}
