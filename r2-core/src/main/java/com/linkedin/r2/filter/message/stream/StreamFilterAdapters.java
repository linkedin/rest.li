package com.linkedin.r2.filter.message.stream;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamException;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;

import java.util.Map;

/**
 * This helper class provides methods to adapt RestFilters to StreamFilters as currently FilterChain would ignore
 * RestFilters.
 *
 * However, using RestFilters would cause the request and/or response being fully buffered in memory, negating the
 * benefits brought by R2 streaming.
 *
 * This class is only supposed to be used during transition period where both rest & stream code path for filter
 * chain exist. Avoid use it if possible.
 *
 * @author Zhenkai Zhu
 */
public class StreamFilterAdapters
{
  private StreamFilterAdapters() {}

  public static StreamFilter adaptRestFilter(RestFilter filter)
  {
    return new StreamFilterAdapter(filter);
  }

  private static class StreamFilterAdapter implements StreamFilter
  {
    RestFilter _filter;

    public StreamFilterAdapter(RestFilter filter)
    {
      _filter = filter;
    }

    @Override
    public   void onStreamRequest(StreamRequest req,
                            final RequestContext requestContext,
                            final Map<String, String> wireAttrs,
                            final NextFilter<StreamRequest, StreamResponse> nextFilter)
    {
      Messages.toRestRequest(req, new Callback<RestRequest>()
      {
        @Override
        public void onError(Throwable e)
        {
          nextFilter.onError(e, requestContext, wireAttrs);
        }

        @Override
        public void onSuccess(RestRequest result)
        {
          _filter.onRestRequest(result, requestContext, wireAttrs,
              new AdaptingNextFilter(nextFilter));
        }
      });
    }

    @Override
    public void onStreamResponse(StreamResponse res,
                                 final RequestContext requestContext,
                                 final Map<String, String> wireAttrs,
                                 final NextFilter<StreamRequest, StreamResponse> nextFilter)
    {
      Messages.toRestResponse(res, new Callback<RestResponse>()
      {
        @Override
        public void onError(Throwable e)
        {
          nextFilter.onError(e, requestContext, wireAttrs);
        }

        @Override
        public void onSuccess(RestResponse result)
        {
          _filter.onRestResponse(result, requestContext, wireAttrs,
              new AdaptingNextFilter(nextFilter));
        }
      });
    }

    @Override
    public void onStreamError(Throwable ex,
                              final RequestContext requestContext,
                              final Map<String, String> wireAttrs,
                              final NextFilter<StreamRequest, StreamResponse> nextFilter)
    {
      if (ex instanceof StreamException)
      {
        Messages.toRestException((StreamException)ex, new Callback<RestException>()
        {
          @Override
          public void onError(Throwable e)
          {
            nextFilter.onError(e, requestContext, wireAttrs);
          }

          @Override
          public void onSuccess(RestException result)
          {
            _filter.onRestError(result, requestContext, wireAttrs,
               new AdaptingNextFilter(nextFilter));
          }
        });
      }
      else
      {
        _filter.onRestError(ex, requestContext, wireAttrs,
            new AdaptingNextFilter(nextFilter));
      }
    }
  }

  private static class AdaptingNextFilter implements NextFilter<RestRequest, RestResponse>
  {
    private final NextFilter<StreamRequest, StreamResponse> _nextFilter;

    AdaptingNextFilter(NextFilter<StreamRequest, StreamResponse> nextFilter)
    {
      _nextFilter = nextFilter;
    }

    public void onRequest(RestRequest req, RequestContext requestContext, Map<String, String> wireAttrs)
    {
        StreamRequest streamRequest = Messages.toStreamRequest(req);
        _nextFilter.onRequest(streamRequest, requestContext, wireAttrs);
    }

    public void onResponse(RestResponse res, RequestContext requestContext, Map<String, String> wireAttrs)
    {
        StreamResponse streamResponse = Messages.toStreamResponse(res);
        _nextFilter.onResponse(streamResponse, requestContext, wireAttrs);
    }

    public void onError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs)
    {
      if (ex instanceof RestException)
      {
        StreamException streamException = Messages.toStreamException((RestException)ex);
        _nextFilter.onError(streamException, requestContext, wireAttrs);
      }
      else
      {
        _nextFilter.onError(ex, requestContext, wireAttrs);
      }
    }
  }
}
