package com.linkedin.r2.filter.transport;


import com.linkedin.common.callback.Callback;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.filter.message.stream.StreamFilter;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.QueryTunnelUtil;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;

import javax.mail.MessagingException;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * @author Zhenkai Zhu
 */
public class ServerQueryTunnelFilter implements StreamFilter,RestFilter
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

  @Override
  public void onStreamRequest(final StreamRequest req,
                            final RequestContext requestContext,
                            final Map<String, String> wireAttrs,
                            final NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    Callback<StreamRequest> callback = new Callback<StreamRequest>()
    {
      @Override
      public void onError(Throwable e)
      {
        if (e instanceof MessagingException || e instanceof URISyntaxException)
        {
          RestResponse errorResponse =
              RestStatus.responseForStatus(RestStatus.BAD_REQUEST, e.toString());
          nextFilter.onResponse(Messages.toStreamResponse(errorResponse), requestContext, wireAttrs);
        }
        else
        {
          nextFilter.onError(e, requestContext, wireAttrs);
        }
      }

      @Override
      public void onSuccess(StreamRequest newReq)
      {
        nextFilter.onRequest(newReq, requestContext, wireAttrs);
      }
    };

    // the entire request would be buffered in memory if decoding is needed
    // this usually is not a problem as request with extremely query parameters is usually get request with no body
    QueryTunnelUtil.decode(req, requestContext, callback);
  }
}