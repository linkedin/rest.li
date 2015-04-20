package com.linkedin.r2.filter.transport;

import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.message.rest.RestRequestFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.QueryTunnelUtil;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestStatus;

import javax.mail.MessagingException;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * @author Zhenkai Zhu
 */
public class ServerQueryTunnelFilter implements RestRequestFilter
{
  @Override
  public void onRestRequest(RestRequest req,
                            RequestContext requestContext,
                            Map<String, String> wireAttrs,
                            NextFilter<RestRequest, RestResponse> nextFilter)
  {
    final RestRequest newReq;
    try
    {
      newReq = QueryTunnelUtil.decode(req, requestContext);
    }
    catch (MessagingException ex)
    {
      RestResponse errorResponse =
          RestStatus.responseForStatus(RestStatus.BAD_REQUEST, ex.toString());
      nextFilter.onResponse(errorResponse, requestContext, wireAttrs);
      return;
    }
    catch (URISyntaxException ex)
    {
      RestResponse errorResponse =
          RestStatus.responseForStatus(RestStatus.BAD_REQUEST, ex.toString());
      nextFilter.onResponse(errorResponse, requestContext, wireAttrs);
      return;
    }
    catch (Exception ex)
    {
      nextFilter.onError(ex, requestContext, wireAttrs);
      return;
    }


    nextFilter.onRequest(newReq, requestContext, wireAttrs);
  }
}