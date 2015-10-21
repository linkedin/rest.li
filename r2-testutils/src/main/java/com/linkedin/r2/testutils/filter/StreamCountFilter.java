package com.linkedin.r2.testutils.filter;

import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.message.stream.StreamFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;

import java.util.Map;

/**
 * @auther Zhenkai Zhu
 */

public class StreamCountFilter implements StreamFilter
{
  private int _streamReqCount;
  private int _streamResCount;
  private int _streamErrCount;

  public int getStreamReqCount()
  {
    return _streamReqCount;
  }

  public int getStreamResCount()
  {
    return _streamResCount;
  }

  public int getStreamErrCount()
  {
    return _streamErrCount;
  }

  public void reset()
  {
    _streamReqCount = _streamResCount = _streamErrCount = 0;
  }

  @Override
  public void onStreamRequest(StreamRequest req, RequestContext requestContext,
                            Map<String, String> wireAttrs,
                            NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    _streamReqCount++;
    nextFilter.onRequest(req, requestContext, wireAttrs);
  }

  @Override
  public void onStreamResponse(StreamResponse res, RequestContext requestContext,
                             Map<String, String> wireAttrs,
                             NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    _streamResCount++;
    nextFilter.onResponse(res, requestContext, wireAttrs);
  }

  @Override
  public void onStreamError(Throwable ex, RequestContext requestContext,
                          Map<String, String> wireAttrs,
                          NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    _streamErrCount++;
    nextFilter.onError(ex, requestContext, wireAttrs);
  }
}
