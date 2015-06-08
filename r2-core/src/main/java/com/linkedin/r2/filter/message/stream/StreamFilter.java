package com.linkedin.r2.filter.message.stream;

import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;


import java.util.Map;

/**
 * A filter that processes {@link StreamRequest}s and
 * {@link StreamResponse}s.
 *
 * @author Zhenkai Zhu
 */

public interface StreamFilter
{
  /**
   * Method to be invoked for each {@link StreamRequest} message.
   *
   * @param req the {@link StreamRequest} message.
   * @param requestContext the {@link com.linkedin.r2.message.RequestContext} of the request.
   * @param wireAttrs the wire attributes of the request.
   * @param nextFilter the next filter in the chain.  Concrete implementations should invoke
   *                   {@link com.linkedin.r2.filter.NextFilter#onRequest} to continue the filter chain.
   */

  default void onStreamRequest(StreamRequest req,
                       RequestContext requestContext,
                       Map<String, String> wireAttrs,
                       NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    nextFilter.onRequest(req, requestContext, wireAttrs);
  }

  /**
   * Method to be invoked for each {@link StreamResponse} message.
   *
   * @param res the {@link StreamResponse} message.
   * @param requestContext the {@link RequestContext} of the request.
   * @param wireAttrs the wire attributes of the response.
   * @param nextFilter the next filter in the chain.  Concrete implementations should invoke
   *                   {@link NextFilter#onResponse} to continue the filter chain.
   */
  default void onStreamResponse(StreamResponse res,
                        RequestContext requestContext,
                        Map<String, String> wireAttrs,
                        NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    nextFilter.onResponse(res, requestContext, wireAttrs);
  }

  /**
   * Method to be invoked when an error is encountered.
   *
   * @param ex the {@link Throwable} representation of the error.
   * @param requestContext the {@link RequestContext} of the request.
   * @param wireAttrs the wire attributes of the response (if any).
   * @param nextFilter the next filter in the chain.  Concrete implementations should invoke
   *                   {@link NextFilter#onError} to continue the filter chain.
   */
  default void onStreamError(Throwable ex,
                     RequestContext requestContext,
                     Map<String, String> wireAttrs,
                     NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    nextFilter.onError(ex, requestContext, wireAttrs);
  }
}
