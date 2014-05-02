package com.linkedin.restli.server;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.message.rest.RestResponse;

/**
 * A callback interface which the RestLiServer calls right before invoking the method to handle restli request.
 * This interface allows user code to perform per-request processing while not interfering with the internal logic
 * of RestLiServer.
 *
 * An example application of this interface is to do the call tracking for each request handling for diagnosis.
 *
 * @author Zhenkai Zhu
 */
@Deprecated
public interface InvokeAware
{
  /**
   * Callback to be invoked by RestLiServer after RestLiServer routing and right before RestLiServer handles a request.
   * @param resourceContext The resource context when invocation happens
   * @param methodContext The restli method context when invocation happens
   * @return A callback to be invoked by RestLiServer right after the handling of the request finishes
   */
  public Callback<RestResponse> onInvoke(ResourceContext resourceContext, RestLiMethodContext methodContext);
}
