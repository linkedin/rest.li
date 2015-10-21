package com.linkedin.r2.filter.message.rest;

import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;

import java.util.Map;

/**
 * A convenient base rest filter class in case the subclass only wants to process request or only wants to process
 * response.
 *
 * @auther Zhenkai Zhu
 */

public class BaseRestFilter implements RestFilter
{
  @Override
  public void onRestRequest(RestRequest req,
                     RequestContext requestContext,
                     Map<String, String> wireAttrs,
                     NextFilter<RestRequest, RestResponse> nextFilter)
  {
    nextFilter.onRequest(req, requestContext, wireAttrs);
  }

  @Override
  public void onRestResponse(RestResponse res,
                      RequestContext requestContext,
                      Map<String, String> wireAttrs,
                      NextFilter<RestRequest, RestResponse> nextFilter)
  {
    nextFilter.onResponse(res, requestContext, wireAttrs);
  }

  @Override
  public void onRestError(Throwable ex,
                   RequestContext requestContext,
                   Map<String, String> wireAttrs,
                   NextFilter<RestRequest, RestResponse> nextFilter)
  {
    nextFilter.onError(ex, requestContext, wireAttrs);
  }
}
