/*
   Copyright (c) 2013 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.linkedin.r2.filter.logging;


import com.linkedin.common.callback.Callback;
import com.linkedin.data.ByteString;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.filter.message.stream.StreamFilter;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.Response;

import java.net.URI;
import java.util.Iterator;
import java.util.Map;

import com.linkedin.r2.message.stream.StreamException;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter for basic logging all requests, responses and exceptions in a rest.li server or client.
 *
 * For requests and responses:  Method, URI, headers and entity length are logged.
 * For exceptions: Exception class name and message are logged.
 *
 * @author Chris Pettitt
 * @author Joe Betz
 */
public class SimpleLoggingFilter implements StreamFilter, RestFilter
{
  // _log is not static because we need to be able to set it during test.
  private final Logger _log;

  private static final String OPERATION = R2Constants.OPERATION;

  private static final String REQUEST_URI = "com.linkedin.r2.requestURI";
  private static final String REQUEST_METHOD = "com.linkedin.r2.requestMethod";

  public SimpleLoggingFilter()
  {
    this(LoggerFactory.getLogger(SimpleLoggingFilter.class));
  }

  public SimpleLoggingFilter(Logger log)
  {
    _log = log;
  }

  @Override
  public void onRestRequest(RestRequest req, RequestContext requestContext, Map<String, String> wireAttrs,
                              NextFilter<RestRequest, RestResponse> nextFilter)
  {
    trace("onRestRequest", req, wireAttrs, requestContext);
    requestContext.putLocalAttr(REQUEST_URI, req.getURI());
    requestContext.putLocalAttr(REQUEST_METHOD, req.getMethod());
    nextFilter.onRequest(req, requestContext, wireAttrs);
  }

  @Override
  public void onRestResponse(RestResponse res, RequestContext requestContext, Map<String, String> wireAttrs,
                               NextFilter<RestRequest, RestResponse> nextFilter)
  {
    trace("onRestResponse", res, wireAttrs, requestContext);
    nextFilter.onResponse(res, requestContext, wireAttrs);
  }

  @Override
  public void onRestError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs,
                            NextFilter<RestRequest, RestResponse> nextFilter)
  {
    warn("onRestError", ex, wireAttrs, requestContext);
    nextFilter.onError(ex, requestContext, wireAttrs);
  }

  @Override
  public void onStreamRequest(StreamRequest req, RequestContext requestContext, Map<String, String> wireAttrs,
                            NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    trace("onStreamRequest", req, wireAttrs, requestContext);
    requestContext.putLocalAttr(REQUEST_URI, req.getURI());
    requestContext.putLocalAttr(REQUEST_METHOD, req.getMethod());
    nextFilter.onRequest(req, requestContext, wireAttrs);
  }

  @Override
  public void onStreamResponse(StreamResponse res, RequestContext requestContext, Map<String, String> wireAttrs,
                             NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    trace("onStreamResponse", res, wireAttrs, requestContext);
    nextFilter.onResponse(res, requestContext, wireAttrs);
  }

  @Override
  public void onStreamError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs,
                          NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    warn("onStreamError", ex, wireAttrs, requestContext);
    nextFilter.onError(ex, requestContext, wireAttrs);
  }

  private void trace(final String method, final RestRequest request,
                     final Map<String, String> wireAttrs, final RequestContext requestContext)
  {
    _log.debug(buildLogMessage(method, "request", formatRequest(request, request.getEntity().length()), wireAttrs, requestContext));
  }

  private void trace(final String method, final RestResponse response, final Map<String, String> wireAttrs,
                     final RequestContext requestContext)
  {
    final URI requestUri = (URI)requestContext.getLocalAttr(REQUEST_URI);
    final String requestMethod = (String)requestContext.getLocalAttr(REQUEST_METHOD);
    _log.debug(buildLogMessage(method, "response", formatResponse(response, response.getEntity().length(), requestUri, requestMethod), wireAttrs, requestContext));
  }

  private void trace(final String method, final StreamRequest request,
                     final Map<String, String> wireAttrs, final RequestContext requestContext)
  {
    Callback<Integer> callback = new Callback<Integer>()
    {
      @Override
      public void onError(Throwable e)
      {
        _log.warn("Cannot get the length of the request", e);
      }

      @Override
      public void onSuccess(Integer result)
      {
        _log.debug(buildLogMessage(method, "request", formatRequest(request, result), wireAttrs, requestContext));
      }
    };
    request.getEntityStream().addObserver(new LengthObserver(callback));
  }

  private void trace(final String method, final StreamResponse response, final Map<String, String> wireAttrs,
                     final RequestContext requestContext)
  {
    final URI requestUri = (URI)requestContext.getLocalAttr(REQUEST_URI);
    final String requestMethod = (String)requestContext.getLocalAttr(REQUEST_METHOD);

    Callback<Integer> callback = new Callback<Integer>()
    {
      @Override
      public void onError(Throwable e)
      {
        _log.warn("Cannot get the length of the response", e);
      }

      @Override
      public void onSuccess(Integer result)
      {
        _log.debug(buildLogMessage(method, "response", formatResponse(response, result, requestUri, requestMethod), wireAttrs, requestContext));
      }
    };

    response.getEntityStream().addObserver(new LengthObserver(callback));
  }

  private void warn(String method, Throwable ex, Map<String, String> wireAttrs, RequestContext requestContext)
  {
    if (!ignoreLog(ex))
    {
      if (logFullException(ex))
      {
        _log.warn(buildLogMessage(method, "ex", buildErrorMessage(ex), wireAttrs, requestContext), ex);
      }
      else
      {
        _log.warn(buildLogMessage(method, "ex", buildErrorMessage(ex), wireAttrs, requestContext));
      }
    }
  }

  private boolean ignoreLog(Throwable ex)
  {
    if (ex instanceof StreamException)
    {
      StreamException streamException = (StreamException)ex;
      return streamException.getResponse() != null;
    }

    if (ex instanceof RestException)
    {
      RestException restException = (RestException)ex;
      return restException.getResponse() != null;
    }

    return false;
  }

  private boolean logFullException(Throwable ex)
  {
    if (ex instanceof StreamException)
    {
      StreamException streamException = (StreamException)ex;
      return streamException.getResponse() != null;
    }

    if (ex instanceof RestException)
    {
      RestException restException = (RestException)ex;
      return restException.getResponse() != null;
    }

    return true;
  }

  private String buildLogMessage(String method, String type, String obj,
                                 Map<String, String> wireAttrs, RequestContext requestContext)
  {
    String operationName = (String)requestContext.getLocalAttr(OPERATION);

    StringBuilder builder = new StringBuilder();
    builder.append("[").append(method).append("] ");
    builder.append(type).append(": ");
    if(operationName != null)
    {
      builder.append("(" + operationName + ") ");
    }
    builder.append(obj);

    if(wireAttrs.size() > 0)
    {
      builder.append(" wire: " + wireAttrs);
    }
    return builder.toString();
  }

  private String buildErrorMessage(Throwable ex)
  {
    final String exMsg = ex.getMessage() != null ? ex.getMessage() : "";
    final int eol = exMsg.indexOf('\n');
    final String msg = eol != -1 ? exMsg.substring(0, eol) : exMsg;
    return ex.getClass().getName() + " (" + msg + ")";
  }

  public String formatRequest(Request request, int reqLen)
  {
    StringBuilder builder = new StringBuilder();
    builder.append("\"").append(request.getMethod());
    builder.append(" ").append(formatRequestURI(request)).append("\"");
    builder.append(" headers=[").append(formatHeaders(request.getHeaders())).append("]");
    builder.append(" entityLength=").append(reqLen);
    return builder.toString();
  }

  public String formatResponse(Response response, int resLen, URI requestUri, String requestMethod)
  {
    StringBuilder builder = new StringBuilder();
    builder.append("\"").append(requestMethod);
    if(requestUri != null)
    {
      builder.append(" ").append(extractURI(requestUri.toString()));
    }
    builder.append(" ").append(response.getStatus()).append("\"");
    builder.append(" headers=[").append(formatHeaders(response.getHeaders())).append("]");

    builder.append(" entityLength=").append(resLen);
    return builder.toString();
  }

  public String formatRequestURI(Request request)
  {
    // We want the ascii representation without the query string
    String uriText = request.getURI().toASCIIString();
    return extractURI(uriText);
  }

  private String extractURI(String uriText)
  {
    int queryStringIndex = uriText.lastIndexOf('?');

    return (queryStringIndex >= 0) ? uriText.substring(0, queryStringIndex) : uriText;
  }

  protected String formatHeaders(Map<String, String> headers)
  {
    StringBuilder builder = new StringBuilder();

    Iterator<Map.Entry<String,String>> it = headers.entrySet().iterator();
    while(it.hasNext())
    {
      Map.Entry<String, String> entry = it.next();
      builder.append(entry.getKey()).append("=").append(entry.getValue());
      if(it.hasNext())
      {
        builder.append(",");
      }
    }
    return builder.toString();
  }

  private static class LengthObserver implements Observer
  {
    private int _len = 0;
    private final Callback<Integer> _callback;

    LengthObserver(Callback<Integer> callback)
    {
      _callback = callback;
    }

    @Override
    public void onDataAvailable(ByteString data)
    {
      _len += data.length();
    }

    @Override
    public void onError(Throwable e)
    {
      _callback.onError(e);
    }

    @Override
    public void onDone()
    {
      _callback.onSuccess(_len);
    }
  }
}
