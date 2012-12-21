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

package com.linkedin.restli.client;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.CallbackAdapter;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.restli.common.HttpMethod;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.client.ExceptionUtil;
import com.linkedin.restli.internal.client.ResponseFutureImpl;
import com.linkedin.restli.internal.client.RestResponseDecoder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Subset of Jersey's REST client, omitting things we probably won't use for internal API calls +
 * enforcing the use of Model (stenciled) response entities. We feature:
 *
 * <ul>
 * <li>Generic client interface (domain-specific hooks are via URIBuilders and Models)
 * <li>All request/response entities _must_ be 'Representation', which encapsultes a DataTemplate and hyperlinks
 * <li>Batch mode for all REST operations. This is inherently unRESTful, as you are supposed
 *     to operate on a single, named resource, NOT an arbitrary, ad-hoc collection of resources.
 *     For example, this probably breaks cacheability in proxies, as clients can specify an arbitrary
 *     set of IDs in the URI, in any order. Caching that specific batched response is pretty much useless,
 *     as no other client will probably look up those same IDs. Same for invalidating resources on POST/PUT.
 *     If we can add batch-aware proxies, this may work.
 *     In any case, we need batching to reduce latency, chattiness and framing overhead for calls made
 *     within and across our datacenters.
 *     Semantics of batch operations (atomic/non-atomic, etc) are specified by the server.
 * <li>Async invocation
 * <li>Client can choose to deal with the 'Response' (including status, headers) or just get the
 *     response entity (Model) directly (assuming response was status 200)
 * <li>TODO Exceptions at this layer? We clearly can't declare checked exceptions on THIS interface
 * </ul>
 *
 * <h2>Features NOT ported from Jersey client</h2>
 *
 * <ul>
 * <li>No support for String URIs
 * <li>Clients may define accept MIME types, the expected response entity type, and their preference order.
 *     By default clients will send no accept headers and receive responses in and expect the server to
 *     send responses in application/json format when there is no accept header.
 *  <li>TODO Do we need to support Accept-Language header for i18n profile use case?
 *  <li>No cookies
 *  <li>No (convenient) synchronous invocation mode (can just call Future.get())
 * </ul>
 *
 * <h2>Features in Jersey we should consider</h2>
 *
 * <ul>
 * <li>Standard a way to fetch 'links' from response entities.
 *     This would at least open the door for HATEOAS-style REST if we choose to use it in certain places (e.g.
 *     collection navigation, secondary actions (like, comment), etc)
 * </ul>
 *
 * @author dellamag
 * @author Eran Leshem
 */
public class RestClient
{
  private static final JacksonDataCodec JACKSON_DATA_CODEC = new JacksonDataCodec();
  private static final List<AcceptType> DEFAULT_ACCEPT_TYPES = Collections.emptyList();

  private final Client _client;
  private final String _uriPrefix;
  private final List<AcceptType> _acceptTypes;

  public RestClient(Client client, String uriPrefix)
  {
    this(client, uriPrefix, DEFAULT_ACCEPT_TYPES);
  }

  public RestClient(Client client, String uriPrefix, List<AcceptType> acceptTypes)
  {
    _client = client;
    _uriPrefix = uriPrefix;
    _acceptTypes = acceptTypes;
  }

  /**
   * Shuts down the underlying {@link Client} which this RestClient wraps.
   * @param callback
   */
  public void shutdown(Callback<None> callback)
  {
    _client.shutdown(callback);
  }

  /**
   * Sends a type-bound REST request, returning a future.
   *
   *
   * @param request to send
   * @param requestContext context for the request
   * @return response future
   */
  public <T> ResponseFuture<T> sendRequest(Request<T> request,
                                                                  RequestContext requestContext)
  {
    FutureCallback<Response<T>> callback = new FutureCallback<Response<T>>();
    sendRequest(request, requestContext, callback);
    return new ResponseFutureImpl<T>(callback);
  }

  /**
   * Sends a type-bound REST request using a callback.
   *
   * @param request to send
   * @param requestContext context for the request
   * @param callback to call on request completion. In the event of an error, the callback
   *                 will receive a {@link com.linkedin.r2.RemoteInvocationException}. If a valid
   *                 error response was received from the remote server, the callback will receive
   *                 a {@link RestLiResponseException} containing the error details.
   */
  public <T> void sendRequest(final Request<T> request,
                                                     RequestContext requestContext,
                                                     Callback<Response<T>> callback)
  {
    RecordTemplate input = request.getInput();
    RestLiCallbackAdapter<T> adapter = new RestLiCallbackAdapter<T>(request.getResponseDecoder(), callback);
    sendRequestImpl(requestContext, request.getUri(), request.getMethod(),
                    input != null ? input.data() : null, request.getHeaders(), adapter);
  }

  private void addAcceptHeaders(RestRequestBuilder builder)
  {
    if(!_acceptTypes.isEmpty() && builder.getHeader(RestConstants.HEADER_ACCEPT) == null)
    {
      builder.setHeader(RestConstants.HEADER_ACCEPT, createAcceptHeader());
    }
  }

  private String createAcceptHeader()
  {
    if (_acceptTypes.size() == 1)
    {
      return _acceptTypes.get(0).getHeaderKey();
    }

    // general case
    StringBuilder acceptHeader = new StringBuilder();
    double currQ = 1.0;
    Iterator<AcceptType> iterator = _acceptTypes.iterator();
    while(iterator.hasNext())
    {
      acceptHeader.append(iterator.next().getHeaderKey());
      acceptHeader.append(";q=");
      acceptHeader.append(currQ);
      currQ -= .1;
      if (iterator.hasNext())
        acceptHeader.append(",");
    }

    return acceptHeader.toString();
  }

  /**
   * Sends a type-bound REST request, returning a future
   * @param request to send
   * @return response future
   */
  public <T> ResponseFuture<T> sendRequest(Request<T> request)
  {
    return sendRequest(request, new RequestContext());
  }

  /**
   * Sends a type-bound REST request using a callback.
   *
   * @param request to send
   * @param callback to call on request completion. In the event of an error, the callback
   *                 will receive a {@link com.linkedin.r2.RemoteInvocationException}. If a valid
   *                 error response was received from the remote server, the callback will receive
   *                 a {@link RestLiResponseException} containing the error details.
   */
  public <T> void sendRequest(final Request<T> request,
                                                     Callback<Response<T>> callback)
  {
    sendRequest(request, new RequestContext(), callback);
  }

  /**
   * Sends an untyped REST request using a callback.
   *
   * @param requestContext context for the request
   * @param uri for resource
   * @param method to perform
   * @param dataMap request body entity
   * @param callback to call on request completion. In the event of an error, the callback
   *                 will receive a {@link com.linkedin.r2.RemoteInvocationException}. If a valid
   *                 error response was received from the remote server, the callback will receive
   *                 a {@link com.linkedin.r2.message.rest.RestException} containing the error details.
   */
  private <T> void sendRequestImpl(RequestContext requestContext,
                                  URI uri,
                                  ResourceMethod method,
                                  DataMap dataMap,
                                  Map<String, String> headers,
                                  RestLiCallbackAdapter<T> callback)
  {
    try
    {
      RestRequest request = buildRequest(uri, method, dataMap, headers);
      _client.restRequest(request, requestContext, callback);
    }
    catch (Exception e)
    {
      // No need to wrap the exception; RestLiCallbackAdapter.onError() will take care of that
      callback.onError(e);
    }
  }

  // This throws Exception to remind the caller to deal with arbitrary exceptions including RuntimeException
  // in a way appropriate for the public method that was originally invoked.
  private RestRequest buildRequest(URI uri, ResourceMethod method, DataMap dataMap, Map<String, String> headers) throws Exception
  {
    try
    {
      uri = new URI(_uriPrefix + uri.toString());
    }
    catch (URISyntaxException e)
    {
      throw new IllegalArgumentException(e);
    }

    RestRequestBuilder requestBuilder = new RestRequestBuilder(uri).setMethod(
            method.getHttpMethod().toString());

    requestBuilder.setHeaders(headers);
    addAcceptHeaders(requestBuilder);

    if (method.getHttpMethod() == HttpMethod.POST)
    {
      requestBuilder.setHeader(RestConstants.HEADER_RESTLI_REQUEST_METHOD, method.toString());
    }

    if (dataMap != null)
    {
      requestBuilder.setEntity(JACKSON_DATA_CODEC.mapToBytes(dataMap))
                    .setHeader(RestConstants.HEADER_CONTENT_TYPE,
                               RestConstants.HEADER_VALUE_APPLICATION_JSON);
    }

    return requestBuilder.build();
  }

  public static enum AcceptType
  {
    PSON(RestConstants.HEADER_VALUE_APPLICATION_PSON),
    JSON(RestConstants.HEADER_VALUE_APPLICATION_JSON),
    ANY(RestConstants.HEADER_VALUE_ACCEPT_ANY);

    private String _headerKey;

    private AcceptType(String headerKey)
    {
      _headerKey = headerKey;
    }

    public String getHeaderKey()
    {
      return _headerKey;
    }
  }

  private static class RestLiCallbackAdapter<T> extends CallbackAdapter<Response<T>,RestResponse>
  {
    private final RestResponseDecoder<T> _decoder;

    private RestLiCallbackAdapter(RestResponseDecoder<T> decoder, Callback<Response<T>> callback)
    {
      super(callback);
      _decoder = decoder;
    }

    @Override
    protected Response<T> convertResponse(RestResponse response) throws Exception
    {
      return _decoder.decodeResponse(response);
    }

    @Override
    protected Throwable convertError(Throwable error)
    {
      return ExceptionUtil.exceptionForThrowable(error, false);
    }
  }
}
