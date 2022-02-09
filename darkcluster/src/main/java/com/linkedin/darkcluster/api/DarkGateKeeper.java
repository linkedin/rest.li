package com.linkedin.darkcluster.api;

import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;


/**
 * Interface that lets users define custom logic to determine if a given request is to be dispatched to dark cluster or not.
 */
@FunctionalInterface
public interface DarkGateKeeper
{
  /**
   * Determine if the request is to be dispatched or not
   * @param request original request
   * @param requestContext original request context
   * @return true if request should be dispatched, false otherwise
   */
  boolean shouldDispatchToDark(RestRequest request, RequestContext requestContext);

  /**
   * Determine if the request is to be dispatched or not given dark cluster name
   * @param request original request
   * @param requestContext original request context
   * @param darkClusterName name of the dark cluster
   * @return true if request should be dispatched, false otherwise
   */
  default boolean shouldDispatchToDark(RestRequest request, RequestContext requestContext, String darkClusterName)
  {
    return shouldDispatchToDark(request, requestContext);
  }
}
