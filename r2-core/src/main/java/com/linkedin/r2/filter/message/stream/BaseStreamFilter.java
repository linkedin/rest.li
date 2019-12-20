package com.linkedin.r2.filter.message.stream;

import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;

import java.util.Map;

/**
 * A convenient base stream filter class in case the subclass only wants to process request or only wants to process
 * response.
 *
 * Use {@code StreamFilter} instead.
 *
 * @author Zhenkai Zhu
 */
public class BaseStreamFilter implements StreamFilter
{
  @Override
  public void onStreamRequest(StreamRequest req,
                            RequestContext requestContext,
                            Map<String, String> wireAttrs,
                            NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    nextFilter.onRequest(req, requestContext, wireAttrs);
  }

  @Override
  public void onStreamResponse(StreamResponse res,
                             RequestContext requestContext,
                             Map<String, String> wireAttrs,
                             NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    nextFilter.onResponse(res, requestContext, wireAttrs);
  }

  @Override
  public void onStreamError(Throwable ex,
                          RequestContext requestContext,
                          Map<String, String> wireAttrs,
                          NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    nextFilter.onError(ex, requestContext, wireAttrs);
  }
}
