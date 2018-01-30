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

package com.linkedin.d2.balancer;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.discovery.event.PropertyEventThread.PropertyEventShutdownCallback;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import java.util.concurrent.ExecutionException;


/**
 * The interface for load balancing a request. The main method is to return a client
 * to which the requester can pass the request, given request and some context.
 */
public interface LoadBalancer
{

  /**
   * Given a Request, returns a TransportClient that can handle requests for the Request.
   *
   * @implNote We declare the default implementation to be backward compatible with
   *            classes that didn't implement this method yet. Note that at least one
   *            of the two implementation of getClient (async or sync) should be implemented
   *
   * @param request        A request whose URI is a URL of the format "d2://&gt;servicename&lt;/optional/path".
   * @param requestContext context for this request
   * @param clientCallback A client that can be called to retrieve data for the URN.
   * @throws ServiceUnavailableException If the load balancer can't figure out how to reach a service for the given
   *                                     URN, an ServiceUnavailableException will be thrown.
   */
  default void getClient(Request request, RequestContext requestContext, Callback<TransportClient> clientCallback)
  {
    try
    {
      clientCallback.onSuccess(getClient(request, requestContext));
    }
    catch (ServiceUnavailableException e)
    {
      clientCallback.onError(e);
    }
  }

  /**
   * Given a Service name, returns a TransportClient that can handle requests for the Request.
   *
   * @implNote We declare the default implementation to be backward compatible with
   *            classes that didn't implement this method yet. Note that at least one
   *            of the two implementation of getLoadBalancedServiceProperties (async
   *            or sync) should be implemented
   *
   * @param serviceName    The service name that
   * @param clientCallback A callback that returns the properties of the service if it doesn't throw.
   * @throws ServiceUnavailableException If the load balancer can't figure out how to reach a service for the given
   *                                     URN, an ServiceUnavailableException will be thrown.
   */
  default void getLoadBalancedServiceProperties(String serviceName, Callback<ServiceProperties> clientCallback)
  {
    try
    {
      clientCallback.onSuccess(getLoadBalancedServiceProperties(serviceName));
    }
    catch (ServiceUnavailableException e)
    {
      clientCallback.onError(e);
    }
  }

  void start(Callback<None> callback);

  void shutdown(PropertyEventShutdownCallback shutdown);

  // ################## Methods to deprecate Section ##################

  /**
   * This method is deprecated but kept for backward compatibility.
   * We need a default implementation since every LoadBalancer should implement the
   * asynchronous version of this to fallback (@link {@link #getClient(Request, RequestContext, Callback)})
   * onto thanks to this default implementation.
   * <p>
   * This method will be removed once all the use cases are moved to the async version
   *
   * @implNote The default implementation allows to fallback on the async implementation and therefore delete the
   *            the implementation of this method from inheriting classes
   *
   *@see #getClient(Request, RequestContext, Callback)
   */
  default TransportClient getClient(Request request, RequestContext requestContext) throws ServiceUnavailableException
  {
    FutureCallback<TransportClient> callback = new FutureCallback<>();
    getClient(request, requestContext, callback);
    try
    {
      return callback.get();
    }
    catch (InterruptedException e)
    {
      throw new RuntimeException(e);
    }
    catch (ExecutionException e)
    {
      Throwable throwable = e.getCause();
      if (throwable instanceof ServiceUnavailableException)
      {
        throw (ServiceUnavailableException) throwable;
      }
      throw new RuntimeException(e);
    }
  }

  /**
   * This method is deprecated but kept for backward compatibility.
   * We need a default implementation since every LoadBalancer should implement the
   * asynchronous version of this to fallback (@link {@link #getLoadBalancedServiceProperties(String, Callback)})
   * onto thanks to this default implementation.
   * <p>
   * This method will be removed once all the use cases are moved to the async version
   *
   * @implNote The default implementation allows to fallback on the async implementation and therefore delete the
   *            the implementation of this method from inheriting classes
   *
   * @see #getLoadBalancedServiceProperties(String, Callback)
   */
  default ServiceProperties getLoadBalancedServiceProperties(String serviceName) throws ServiceUnavailableException
  {
    FutureCallback<ServiceProperties> callback = new FutureCallback<>();
    getLoadBalancedServiceProperties(serviceName, callback);
    try
    {
      return callback.get();
    }
    catch (InterruptedException e)
    {
      throw new RuntimeException(e);
    }
    catch (ExecutionException e)
    {
      Throwable throwable = e.getCause();
      if (throwable instanceof ServiceUnavailableException)
      {
        throw (ServiceUnavailableException) throwable;
      }
      throw new RuntimeException(e);
    }
  }
}
