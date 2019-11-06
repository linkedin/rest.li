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
import java.util.concurrent.atomic.AtomicReference;
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

  private final Request _request;
  private final RequestContext _requestContext;
  private final List<RequestFinalizer> _requestFinalizers;
  private final AtomicBoolean _isFinalized;

  public RequestFinalizerManagerImpl(Request request, RequestContext requestContext)
  {
    _request = request;
    _requestContext = requestContext;

    _requestFinalizers = new CopyOnWriteArrayList<>();
    _isFinalized = new AtomicBoolean();
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
    return false;
  }
}
