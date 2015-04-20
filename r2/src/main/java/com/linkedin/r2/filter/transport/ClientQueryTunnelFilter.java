package com.linkedin.r2.filter.transport;

import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.message.rest.RestRequestFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.QueryTunnelUtil;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;

import java.util.Map;

/**
 * @author Zhenkai Zhu
 */
public class ClientQueryTunnelFilter implements RestRequestFilter
{
  private final int _queryPostThreshold;

  public ClientQueryTunnelFilter(int queryPostThreshold)
  {
    _queryPostThreshold = queryPostThreshold;
  }

  @Override
  public void onRestRequest(RestRequest req,
                     RequestContext requestContext,
                     Map<String, String> wireAttrs,
                     NextFilter<RestRequest, RestResponse> nextFilter)
  {
    final RestRequest newReq;
    try
    {
      newReq = QueryTunnelUtil.encode(req, requestContext, _queryPostThreshold);
    }
    catch (Exception e)
    {
      nextFilter.onError(e, requestContext, wireAttrs);
      return;
    }
    nextFilter.onRequest(newReq, requestContext, wireAttrs);
  }
}
