/*
   Copyright (c) 2012 LinkedIn Corp.

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

/* $Id$ */
package com.linkedin.r2.transport.http.server;


import com.linkedin.data.ByteString;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.transport.common.WireAttributeHelper;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.r2.transport.http.common.HttpConstants;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Steven Ihde
 * @author Chris Pettitt
 * @author Fatih Emekci
 * @version $Revision$
 */
public abstract class AbstractR2Servlet extends HttpServlet
{
  private static final Logger _log = LoggerFactory.getLogger(AbstractR2Servlet.class);
  private static final long   serialVersionUID = 0L;

  protected abstract HttpDispatcher getDispatcher();

  @Override
  protected void service(final HttpServletRequest req, final HttpServletResponse resp)
          throws ServletException, IOException
  {
    RequestContext requestContext = readRequestContext(req);

    RestRequest restRequest;

    try
    {
      restRequest = readFromServletRequest(req);
    }
    catch (URISyntaxException e)
    {
      writeToServletError(resp, RestStatus.BAD_REQUEST, e.toString());
      return;
    }

    final AtomicReference<TransportResponse<RestResponse>> result =
        new AtomicReference<TransportResponse<RestResponse>>();
    final CountDownLatch latch = new CountDownLatch(1);

    TransportCallback<RestResponse> callback = new TransportCallback<RestResponse>()
    {
      @Override
      public void onResponse(TransportResponse<RestResponse> response)
      {
        result.set(response);
        latch.countDown();
      }
    };

    getDispatcher().handleRequest(restRequest, requestContext, callback);

    try
    {
      latch.await();
    }
    catch (InterruptedException e)
    {
      throw new ServletException("Interrupted!", e);
    }

    writeToServletResponse(result.get(), resp);
  }

  protected void writeToServletResponse(TransportResponse<RestResponse> response,
                                        HttpServletResponse resp)
      throws IOException
  {
    Map<String, String> wireAttrs = response.getWireAttributes();
    for (Map.Entry<String, String> e : WireAttributeHelper.toWireAttributes(wireAttrs)
        .entrySet())
    {
      resp.setHeader(e.getKey(), e.getValue());
    }

    RestResponse restResponse = null;
    if (response.hasError())
    {
      Throwable e = response.getError();
      if (e instanceof RestException)
      {
        restResponse = ((RestException) e).getResponse();
      }
      if (restResponse == null)
      {
        restResponse = RestStatus.responseForError(RestStatus.INTERNAL_SERVER_ERROR, e);
      }
    }
    else
    {
      restResponse = response.getResponse();
    }

    resp.setStatus(restResponse.getStatus());
    Map<String, String> headers = restResponse.getHeaders();
    for (Map.Entry<String, String> e : headers.entrySet())
    {
      // TODO multi-valued headers
      resp.setHeader(e.getKey(), e.getValue());
    }

    for (String cookie : restResponse.getCookies())
    {
      resp.addHeader(HttpConstants.RESPONSE_COOKIE_HEADER_NAME, cookie);
    }

    final ByteString entity = restResponse.getEntity();
    entity.write(resp.getOutputStream());

    resp.getOutputStream().close();
  }

  protected void writeToServletError(HttpServletResponse resp, int statusCode, String message) throws IOException
  {
    RestResponse restResponse =
        RestStatus.responseForStatus(statusCode, message);
    writeToServletResponse(TransportResponseImpl.success(restResponse), resp);
  }

  protected RestRequest readFromServletRequest(HttpServletRequest req) throws IOException,
      ServletException,
      URISyntaxException
  {
    StringBuilder sb = new StringBuilder();
    sb.append(extractPathInfo(req));
    String query = req.getQueryString();
    if (query != null)
    {
      sb.append('?');
      sb.append(query);
    }

    URI uri = new URI(sb.toString());

    RestRequestBuilder rb = new RestRequestBuilder(uri);
    rb.setMethod(req.getMethod());

    for (Enumeration<String> headerNames = req.getHeaderNames(); headerNames.hasMoreElements();)
    {
      String headerName = headerNames.nextElement();
      if (headerName.equalsIgnoreCase(HttpConstants.REQUEST_COOKIE_HEADER_NAME))
      {
        for (Enumeration<String> cookies = req.getHeaders(headerName); cookies.hasMoreElements();)
        {
          rb.addCookie(cookies.nextElement());
        }
      }
      else
      {
        for (Enumeration<String> headerValues = req.getHeaders(headerName); headerValues.hasMoreElements();)
        {
          rb.addHeaderValue(headerName, headerValues.nextElement());
        }
      }
    }

    int length = req.getContentLength();
    if (length >= 0)
    {
      rb.setEntity(ByteString.read(req.getInputStream(), length));
    }
    return rb.build();
  }

  /**
   * Read HTTP-specific properties from the servlet request into the request context. We'll read
   * properties that many clients might be interested in, such as the caller's IP address.
   * @param req The HTTP servlet request
   * @return The request context
   */
  protected RequestContext readRequestContext(HttpServletRequest req)
  {
    RequestContext context = new RequestContext();
    context.putLocalAttr(R2Constants.REMOTE_ADDR, req.getRemoteAddr());
    if (req.isSecure())
    {
      // attribute name documented in ServletRequest API:
      // http://docs.oracle.com/javaee/6/api/javax/servlet/ServletRequest.html#getAttribute%28java.lang.String%29
      Object[] certs = (Object[]) req.getAttribute("javax.servlet.request.X509Certificate");
      if (certs != null && certs.length > 0)
      {
        context.putLocalAttr(R2Constants.CLIENT_CERT, certs[0]);
      }
      context.putLocalAttr(R2Constants.IS_SECURE, true);
    }
    else
    {
      context.putLocalAttr(R2Constants.IS_SECURE, false);
    }
    return context;
  }

  /**
   * Attempts to return a "non decoded" pathInfo by stripping off the contextPath and servletPath parts of the requestURI.
   * As a defensive measure, this method will return the "decoded" pathInfo directly by calling req.getPathInfo() if it is
   * unable to strip off the contextPath or servletPath.
   * @throws ServletException if resulting pathInfo is empty
   */
  protected static String extractPathInfo(HttpServletRequest req) throws ServletException
  {
    // For "http:hostname:8080/contextPath/servletPath/pathInfo" the RequestURI is "/contextPath/servletPath/pathInfo"
    // where the contextPath, servletPath and pathInfo parts all contain their leading slash.

    // stripping contextPath and servletPath this way is not fully compatible with the HTTP spec.  If a
    // request for, say "/%75scp-proxy/reso%75rces" is made (where %75 decodes to 'u')
    // the stripping off of contextPath and servletPath will fail because the requestUri string will
    // include the encoded char but the contextPath and servletPath strings will not.
    String requestUri = req.getRequestURI();
    String contextPath = req.getContextPath();
    StringBuilder builder = new StringBuilder();
    if(contextPath != null)
    {
      builder.append(contextPath);
    }

    String servletPath = req.getServletPath();
    if(servletPath != null)
    {
      builder.append(servletPath);
    }
    String prefix = builder.toString();
    String pathInfo;
    if(prefix.length() == 0)
    {
      pathInfo = requestUri;
    }
    else if(requestUri.startsWith(prefix))
    {
      pathInfo = requestUri.substring(prefix.length());
    }
    else
    {
      _log.warn("Unable to extract 'non decoded' pathInfo, returning 'decoded' pathInfo instead.  This may cause issues processing request URIs containing special characters. requestUri=" + requestUri);
      return req.getPathInfo();
    }

    if(pathInfo.length() == 0)
    {
      // We prefer to keep servlet mapping trivial with R2 and have R2
      // TransportDispatchers make most of the routing decisions based on the 'pathInfo'
      // and query parameters in the URI.
      // If pathInfo is null, it's highly likely that the servlet was mapped to an exact
      // path or to a file extension, making such R2-based services too reliant on the
      // servlet container for routing
      throw new ServletException("R2 servlet should only be mapped via wildcard path mapping e.g. /r2/*. "
          + "Exact path matching (/r2) and file extension mappings (*.r2) are currently not supported");
    }

    return pathInfo;
  }
}
