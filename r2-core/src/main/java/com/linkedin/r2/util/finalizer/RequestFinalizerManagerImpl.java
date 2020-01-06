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
package com.linkedin.r2.util.finalizer;

import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Response;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Simple implementation of {@link RequestFinalizerManager}.
 *
 * A request can only be finalized exactly once after which no additional
 * {@link RequestFinalizer}s can be registered. The finalizers will be
 * executed in the order in which they were registered.
 *
 * @author Chris Zhang
 */
public class RequestFinalizerManagerImpl implements RequestFinalizerManager
{
  private static final Logger LOG = LoggerFactory.getLogger(RequestFinalizerManagerImpl.class);
  private static final AtomicInteger INSTANCE_COUNT = new AtomicInteger();

  private final Request _request;
  private final RequestContext _requestContext;
  private final List<RequestFinalizer> _requestFinalizers;
  private final AtomicBoolean _isFinalized;

  // For debug logging.
  private final int _id;
  private final AtomicInteger _numFinalizations = new AtomicInteger();
  private RuntimeException _firstFinalization;
  private Response _firstResponse;

  public RequestFinalizerManagerImpl(Request request, RequestContext requestContext)
  {
    _request = request;
    _requestContext = requestContext;

    _requestFinalizers = new CopyOnWriteArrayList<>();
    _isFinalized = new AtomicBoolean();

    _id = INSTANCE_COUNT.getAndIncrement();
  }

  @Override
  public boolean registerRequestFinalizer(RequestFinalizer requestFinalizer)
  {
    if (_isFinalized.get())
    {
      return false;
    }
    else
    {
      _requestFinalizers.add(requestFinalizer);
      return true;
    }
  }

  /**
   * Executes registered {@link RequestFinalizer}s.
   *
   * @param response Current response.
   * @param error Will be nonnull when the request is finalized through an error code path.
   *              For example, when an exception occurs when reading/writing to a stream.
   * @return True if RequestFinalizers are run for the first time, else false if they have already been run before.
   */
  public boolean finalizeRequest(Response response, Throwable error)
  {
    if (_isFinalized.compareAndSet(false, true))
    {
      if (LOG.isDebugEnabled())
      {
        _numFinalizations.incrementAndGet();
        _firstFinalization = new RuntimeException("Finalized at time: " + System.currentTimeMillis());
        _firstResponse = response;
      }

      for (RequestFinalizer requestFinalizer: _requestFinalizers)
      {
        try
        {
          requestFinalizer.finalizeRequest(_request, response, _requestContext, error);
        }
        catch (Throwable e)
        {
          LOG.warn("Exception thrown in request finalizer: " + requestFinalizer, e);
        }
      }
      return true;
    }
    else
    {
      if (LOG.isDebugEnabled())
      {
        final int numFinalizations = _numFinalizations.incrementAndGet();

        if (numFinalizations == 2)
        {
          // Log the first finalization since we now know the request will be finalized at least twice.
          LOG.debug(String.format("Request finalized the first time. FinalizerManager ID = %s\nRequest ID = %s\nRequest = %s\nRequestContext ID = %s"
                                    + "\nRequestContext = %s\nResponse ID = %s\nResponse = %s", _id, System.identityHashCode(_request), _request,
                                  System.identityHashCode(_requestContext), _requestContext, System.identityHashCode(_firstResponse), _firstResponse),
                    _firstFinalization);
        }

        LOG.debug(String.format("Request finalized %d times. FinalizerManager ID = %s\nResponse = %s", numFinalizations, _id, response),
                  new RuntimeException());
      }
      return false;
    }
  }
}
