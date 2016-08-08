package com.linkedin.restli.internal.server.util;

import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.filter.FilterRequestContextInternal;
import com.linkedin.restli.internal.server.methods.MethodAdapterRegistry;
import com.linkedin.restli.internal.server.methods.arguments.RestLiArgumentBuilder;
import com.linkedin.restli.internal.server.response.ErrorResponseBuilder;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.RestLiServiceException;


/**
 * Created by gye on 6/27/16.
 *
 * Provides utility functions for the RestLiServer
 */
public class RestLiServerUtils
{

  /**
   * Validates REST headers and updates the RoutingResult with the header
   *
   * @param request
   *          {@link RestRequest}
   * @param method
   *          {@link RoutingResult}
   * @throws RestLiServiceException
   *          {@link RestLiServiceException}
   */
  public static void checkHeadersAndUpdateContext(RestRequest request, RoutingResult method)
      throws RestLiServiceException
  {
    RestUtils.validateRequestHeadersAndUpdateResourceContext(request.getHeaders(),
                                                             (ServerResourceContext)method.getContext());
  }

  /**
   * Builds a RestLiArgument builder
   *
   * @param method
   *          {@link RoutingResult}
   * @param errorResponseBuilder
   *          {@link ErrorResponseBuilder}
   * @return
   */
  public static RestLiArgumentBuilder buildRestLiArgumentBuilder(RoutingResult method,
                                                                 ErrorResponseBuilder errorResponseBuilder)
  {
    ResourceMethodDescriptor resourceMethodDescriptor = method.getResourceMethod();

    RestLiArgumentBuilder adapter = new MethodAdapterRegistry(errorResponseBuilder)
        .getArgumentBuilder(resourceMethodDescriptor.getType());
    if (adapter == null)
    {
      throw new IllegalArgumentException("Unsupported method type: " + resourceMethodDescriptor.getType());
    }
    return adapter;
  }

  /**
   * Updates the filter request context with the request data
   *
   * @param filterContext
   *          {@link FilterRequestContextInternal}
   * @param adapter
   *          {@link RestLiArgumentBuilder}
   * @param request
   *          {@link RestRequest}
   * @param method
   *          {@link RoutingResult}
   */
  public static void updateFilterRequestContext(FilterRequestContextInternal filterContext,
                                                RestLiArgumentBuilder adapter,
                                                RestRequest request,
                                                RoutingResult method)
  {
    RestLiRequestData requestData = adapter.extractRequestData(method, request);
    filterContext.setRequestData(requestData);
  }
}
