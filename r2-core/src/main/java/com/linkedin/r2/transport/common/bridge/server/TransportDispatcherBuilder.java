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
import com.linkedin.r2.transport.common.StreamRequestHandler;
import com.linkedin.r2.transport.common.StreamRequestHandlerAdapter;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Zhenkai Zhu
 */
public class TransportDispatcherBuilder
{
  private final Map<URI, StreamRequestHandler> _streamHandlers;
  private final Map<URI, RestRequestHandler> _restHandlers;
  private final Map<URI, StreamRequestHandler> _adaptedHandlers;
  private final boolean _restOverStream;

  public TransportDispatcherBuilder()
  {
    /**
     *  mostly for testing, so set restOverStream to true regardless of {@link com.linkedin.r2.filter.R2Constants.DEFAULT_REST_OVER_STREAM}
     */
    this(true);
  }

  public TransportDispatcherBuilder(boolean restOverStream)
  {
    this(new HashMap<URI, RestRequestHandler>(), new HashMap<URI, StreamRequestHandler>(), restOverStream);
  }

  public TransportDispatcherBuilder(Map<URI, RestRequestHandler> restHandlers, Map<URI, StreamRequestHandler> streamHandlers, boolean restOverStream)
  {
    _restHandlers = new HashMap<URI, RestRequestHandler>(restHandlers);
    _streamHandlers = new HashMap<URI, StreamRequestHandler>(streamHandlers);
    _adaptedHandlers = new HashMap<URI, StreamRequestHandler>();
    _restOverStream = restOverStream;
  }

  public TransportDispatcherBuilder addRestHandler(URI uri, RestRequestHandler handler)
  {
    _restHandlers.put(uri, handler);
    if (_restOverStream)
    {
      _adaptedHandlers.put(uri, new StreamRequestHandlerAdapter(handler));
    }
    return this;
  }

  public RestRequestHandler removeRestHandler(URI uri)
  {
    RestRequestHandler handler = _restHandlers.remove(uri);
    if (_restOverStream)
    {
      _adaptedHandlers.remove(uri);
    }
    return handler;
  }

  public TransportDispatcherBuilder addStreamHandler(URI uri, StreamRequestHandler handler)
  {
    _streamHandlers.put(uri, handler);
    return this;
  }

  public StreamRequestHandler removeStreamHandler(URI uri)
  {
    return _streamHandlers.remove(uri);
  }


  public TransportDispatcherBuilder reset()
  {
    _restHandlers.clear();
    _adaptedHandlers.clear();
    _streamHandlers.clear();
    return this;
  }

  public TransportDispatcher build()
  {
    Map<URI, StreamRequestHandler> mergedStreamHandlers = new HashMap<URI, StreamRequestHandler>(_adaptedHandlers);
    mergedStreamHandlers.putAll(_streamHandlers);
    return new TransportDispatcherImpl(_restHandlers, mergedStreamHandlers);
  }

}
