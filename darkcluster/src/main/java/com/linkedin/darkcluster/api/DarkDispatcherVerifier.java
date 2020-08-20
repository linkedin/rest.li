package com.linkedin.darkcluster.api;

import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;


/**
 * Interface that lets users define custom logic to determine if a given request is to be dispatched to dark cluster or not.
 */
@FunctionalInterface
public interface DarkDispatcherVerifier
{
  /**
   * Determine if the request is to be dispatched or not
   * @param request original request
   * @param requestContext original request context
   * @return true if request should be dispatched, false otherwise
   */
  boolean shouldDispatchToDark(RestRequest request, RequestContext requestContext);
}
