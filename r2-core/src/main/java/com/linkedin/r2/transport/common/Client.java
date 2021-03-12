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
package com.linkedin.r2.transport.common;


import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * @author Chris Pettitt
 * @author Zhenkai Zhu
 * @version $Revision$
 */
public interface Client
{
  /**
   * Asynchronously issues the given request and returns a {@link Future} that can be used to wait
   * for the response.
   *
   *
   * @param request the request to issue
   * @return a future to wait for the response
   */
  Future<RestResponse> restRequest(RestRequest request);

  /**
   * Asynchronously issues the given request and returns a {@link Future} that can be used to wait
   * for the response.
   *
   *
   * @param request the request to issue
   * @param requestContext context for the request
   * @return a future to wait for the response
   */
  Future<RestResponse> restRequest(RestRequest request, RequestContext requestContext);

  /**
   * Asynchronously issues the given request. The given callback is invoked when the response is
   * received. This event driven approach is typically more complicated to use and is appropriate
   * for building other abstractions, such as a DAG based resolver.
   *
   * @param request the request to issue
   * @param callback the callback to invoke with the response
   */
  void restRequest(RestRequest request, Callback<RestResponse> callback);

  /**
   * Asynchronously issues the given request. The given callback is invoked when the response is
   * received. This event driven approach is typically more complicated to use and is appropriate
   * for building other abstractions, such as a DAG based resolver.
   *
   * @param request the request to issue
   * @param requestContext context for the request
   * @param callback the callback to invoke with the response
   */
  void restRequest(RestRequest request, RequestContext requestContext,
                   Callback<RestResponse> callback);

  /**
   * Asynchronously issues the given request. The given callback is invoked when the response is
   * received.
   *
   * Any implementation that wants to support bidirectional streaming MUST override this method.
   *
   * @param request the request to issue
   * @param callback the callback to invoke with the response
   */
  default void streamRequest(StreamRequest request, Callback<StreamResponse> callback)
  {
    throw new UnsupportedOperationException("Please use an implementation that supports streaming.");
  }

  /**
   * Asynchronously issues the given request. The given callback is invoked when the response is
   * received.
   *
   * Any implementation that wants to support bidirectional streaming MUST override this method.
   *
   * @param request the request to issue
   * @param requestContext context for the request
   * @param callback the callback to invoke with the response
   */
  default void streamRequest(StreamRequest request, RequestContext requestContext, Callback<StreamResponse> callback)
  {
    throw new UnsupportedOperationException("Please use an implementation that supports streaming.");
  }

  /**
   * Asynchronously issues the given request. The given callback is invoked when the response is
   * received.
   *
   * Any implementation that wants to support response-only streaming MUST override this method.
   *
   * @param request the request to issue
   * @param callback the callback to invoke with the response
   */
  default void restRequestStreamResponse(RestRequest request, Callback<StreamResponse> callback)
  {
    throw new UnsupportedOperationException("Please use an implementation that supports response-only streaming.");
  }

  /**
   * Asynchronously issues the given request. The given callback is invoked when the response is
   * received.
   *
   * Any implementation that wants to support response-only streaming MUST override this method.
   *
   * @param request the request to issue
   * @param requestContext context for the request
   * @param callback the callback to invoke with the response
   */
  default void restRequestStreamResponse(RestRequest request, RequestContext requestContext, Callback<StreamResponse> callback)
  {
    throw new UnsupportedOperationException("Please use an implementation that supports response-only streaming.");
  }

  /**
   * Initiates asynchronous shutdown of the client. This method should block minimally, if at all.
   *
   * @param callback a callback to invoke when the shutdown is complete
   */
  void shutdown(Callback<None> callback);

  /**
   * Returns metadata about the server running at {@code uri}.
   *
   * This metadata could be the data returned from the server by making an HTTP OPTIONS request to it, metadata about
   * the {@code uri} stored in a static config file, metadata about the {@code uri} stored in a key-value store etc.
   *
   * @implNote We declare the default implementation to be backward compatible with
   *            classes that didn't implement this method yet. Note that at least one
   *            of the two implementation of getMetadata (async
   *            or sync) should be implemented
   *
   * THE MAP RETURNED FROM THIS METHOD MUST NOT BE NULL!
   *
   * The callback must be guaranteed to return after a certain time
   *
   * @param uri the URI to get metadata for
   */
  default void getMetadata(URI uri, Callback<Map<String, Object>> callback)
  {
    callback.onSuccess(getMetadata(uri));
  }

  // ################## Methods to deprecate Section ##################

  /**
   * This method is deprecated but kept for backward compatibility.
   * We need a default implementation since every Client should implement the
   * asynchronous version of this to fallback {@link #getMetadata(URI, Callback)}
   * <p>
   * This method will be removed once all the use cases are moved to the async version
   *
   * @implNote The default implementation allows to fallback on the async implementation and therefore delete the
   * the implementation of this method from inheriting classes
   *
   * @deprecated use #getMetadata(uri, callback) instead
   * @return metadata for the URI
   */
  @Deprecated
  default Map<String, Object> getMetadata(URI uri){
    FutureCallback<Map<String, Object>> callback = new FutureCallback<>();
    getMetadata(uri, callback);
    try
    {
      // this call is guaranteed to return in a time bounded manner
      return callback.get();
    }
    catch (Exception e)
    {
      return Collections.emptyMap();
    }
  }
}
