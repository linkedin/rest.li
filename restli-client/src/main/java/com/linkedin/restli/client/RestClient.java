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
import com.linkedin.data.codec.PsonDataCodec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.restli.client.uribuilders.RestliUriBuilderUtil;
import com.linkedin.restli.common.HttpMethod;
import com.linkedin.restli.common.OperationNameGenerator;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.client.ExceptionUtil;
import com.linkedin.restli.internal.client.RequestBodyTransformer;
import com.linkedin.restli.internal.client.ResponseFutureImpl;
import com.linkedin.restli.internal.client.RestResponseDecoder;
import com.linkedin.restli.internal.common.AllProtocolVersions;

import javax.mail.internet.ParseException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
  private static final JacksonDataCodec  JACKSON_DATA_CODEC = new JacksonDataCodec();
  private static final PsonDataCodec     PSON_DATA_CODEC    = new PsonDataCodec();
  private static final List<AcceptType>  DEFAULT_ACCEPT_TYPES = Collections.emptyList();
  private static final ContentType DEFAULT_CONTENT_TYPE = ContentType.JSON;

  private final Client _client;
  private final String _uriPrefix;
  private final List<AcceptType> _acceptTypes;
  private final ContentType _contentType;

  public RestClient(Client client, String uriPrefix)
  {
    this(client, uriPrefix, DEFAULT_CONTENT_TYPE, DEFAULT_ACCEPT_TYPES);
  }

  public RestClient(Client client, String uriPrefix, List<AcceptType> acceptTypes)
  {
    this(client, uriPrefix, DEFAULT_CONTENT_TYPE, acceptTypes);
  }

  public RestClient(Client client, String uriPrefix, ContentType contentType, List<AcceptType> acceptTypes)
  {
    _client = client;
    _uriPrefix = (uriPrefix == null) ? null : uriPrefix.trim();
    _acceptTypes = acceptTypes;
    _contentType = contentType;
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
   * Sends a type-bound REST request, returning a future.
   *
   *
   * @param request to send
   * @param requestContext context for the request
   * @param errorHandlingBehavior error handling behavior
   * @return response future
   */
  public <T> ResponseFuture<T> sendRequest(Request<T> request,
                                           RequestContext requestContext,
                                           ErrorHandlingBehavior errorHandlingBehavior)
  {
    FutureCallback<Response<T>> callback = new FutureCallback<Response<T>>();
    sendRequest(request, requestContext, callback);
    return new ResponseFutureImpl<T>(callback, errorHandlingBehavior);
  }

  /**
   * Sends a type-bound REST request, returning a future.
   *
   *
   * @param requestBuilder to invoke {@link com.linkedin.restli.client.RequestBuilder#build()} on to obtain the request
   *                       to send.
   * @param requestContext context for the request
   * @return response future
   */
  public <T> ResponseFuture<T> sendRequest(RequestBuilder<? extends Request<T>> requestBuilder,
                                           RequestContext requestContext)
  {
    return sendRequest(requestBuilder.build(), requestContext);
  }

  /**
   * Sends a type-bound REST request, returning a future.
   *
   *
   * @param requestBuilder to invoke {@link com.linkedin.restli.client.RequestBuilder#build()} on to obtain the request
   *                       to send.
   * @param requestContext context for the request
   * @param errorHandlingBehavior error handling behavior
   * @return response future
   */
  public <T> ResponseFuture<T> sendRequest(RequestBuilder<? extends Request<T>> requestBuilder,
                                           RequestContext requestContext,
                                           ErrorHandlingBehavior errorHandlingBehavior)
  {
    return sendRequest(requestBuilder.build(), requestContext, errorHandlingBehavior);
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
    sendRestRequest(request, requestContext, new RestLiCallbackAdapter<T>(request.getResponseDecoder(), callback));
  }

/**
   * Sends a type-bound REST request using a {@link CallbackAdapter}.
   *
   * @param request to send
   * @param requestContext context for the request
   * @param callback to call on request completion
   */
  @SuppressWarnings("deprecation")
  public <T> void sendRestRequest(final Request<T> request,
                                  RequestContext requestContext,
                                  Callback<RestResponse> callback)
  {
    RecordTemplate input = request.getInputRecord();
    ProtocolVersion protocolVersion;

    URI requestUri;
    boolean hasPrefix;

    if (request.hasUri())
    {
      // if someone has manually crafted a request with a URI we want to use that. This if check will be removed when
      // we remove the getUri() method. In this case hasPrefix is false because the old constructor assumed no prefix
      // and prepended a prefix in this class. In this case we use the baseline protocol version.
      protocolVersion = AllProtocolVersions.BASELINE_PROTOCOL_VERSION;
      requestUri = request.getUri();
      hasPrefix = false;
    }
    else
    {
      protocolVersion = getProtocolVersionForService(request);
      requestUri = RestliUriBuilderUtil.createUriBuilder(request, _uriPrefix, protocolVersion).build();
      hasPrefix = true;
    }

    sendRequestImpl(requestContext,
                    requestUri,
                    hasPrefix,
                    request.getMethod(),
                    input != null ? RequestBodyTransformer.transform(request, protocolVersion) : null,
                    request.getHeaders(),
                    request.getMethodName(),
                    protocolVersion,
                    callback);
  }

  /**
   * @param request
   */
  private ProtocolVersion getProtocolVersionForService(final Request<?> request)
  {
    try
    {
      return getProtocolVersion(AllProtocolVersions.BASELINE_PROTOCOL_VERSION,
                                AllProtocolVersions.LATEST_PROTOCOL_VERSION,
                                AllProtocolVersions.NEXT_PROTOCOL_VERSION,
                                getAnnouncedVersion(_client.getMetadata(new URI(_uriPrefix + request.getServiceName()))),
                                request.getRequestOptions().getProtocolVersionOption());
    }
    catch (URISyntaxException e)
    {
      throw new RuntimeException("Failed to create a valid URI to fetch properties for!");
    }
  }

  /**
   * @param properties The server properties
   * @return the announced protocol version based on percentage
   */
  /*package private*/ static ProtocolVersion getAnnouncedVersion(Map<String, Object> properties)
  {
    if(properties == null)
    {
      throw new RuntimeException("No valid properties found!");
    }
    Object potentialAnnouncedVersion = properties.get(RestConstants.RESTLI_PROTOCOL_VERSION_PROPERTY);
    // if the server doesn't announce a protocol version we assume it is running the baseline version
    if(potentialAnnouncedVersion == null)
    {
      return AllProtocolVersions.BASELINE_PROTOCOL_VERSION;
    }
    Object potentialAnnouncedVersionPercentage = properties.get(RestConstants.RESTLI_PROTOCOL_VERSION_PERCENTAGE_PROPERTY);
    // if the server doesn't announce a protocol version percentage we assume it is running the announced version
    if(potentialAnnouncedVersionPercentage == null)
    {
      return new ProtocolVersion(potentialAnnouncedVersion.toString());
    }
    try
    {
      int announceVersionPercentage = Integer.parseInt(potentialAnnouncedVersionPercentage.toString());
      return (announceVersionPercentage < 0 || new Random().nextInt(100) + 1 <= announceVersionPercentage) ?
          new ProtocolVersion(potentialAnnouncedVersion.toString()) : AllProtocolVersions.BASELINE_PROTOCOL_VERSION;
    }
    catch(NumberFormatException e)
    {
      // if the server announces a incorrect protocol version percentage we assume it is running the announced version
      return new ProtocolVersion(potentialAnnouncedVersion.toString());
    }
  }

  /**
   *
   * @param baselineProtocolVersion baseline version on the client
   * @param latestVersion latest version on the client
   * @param nextVersion the next version on the client
   * @param announcedVersion version announced by the service
   * @param versionOption options present on the request
   * @return the {@link ProtocolVersion} that should be used to build the request
   */
  /*package private*/static ProtocolVersion getProtocolVersion(ProtocolVersion baselineProtocolVersion,
                                                               ProtocolVersion latestVersion,
                                                               ProtocolVersion nextVersion,
                                                               ProtocolVersion announcedVersion,
                                                               ProtocolVersionOption versionOption)
  {
    if (versionOption == null)
    {
      throw new IllegalArgumentException("versionOptions cannot be null!");
    }
    switch (versionOption)
    {
      case FORCE_USE_NEXT:
        return nextVersion;
      case FORCE_USE_LATEST:
        return latestVersion;
      case USE_LATEST_IF_AVAILABLE:
        if (announcedVersion.compareTo(baselineProtocolVersion) == -1)
        {
          // throw an exception as the announced version is less than the default version
          throw new RuntimeException("Announced version is less than the default version!" +
            "Announced version: " + announcedVersion + ", default version: " + baselineProtocolVersion);
        }
        else if (announcedVersion.compareTo(baselineProtocolVersion) == 0)
        {
          // server is running the default version
          return baselineProtocolVersion;
        }
        else if (announcedVersion.compareTo(latestVersion) == -1)
        {
          // use the server announced version if it is less than the latest version
          return announcedVersion;
        }
        // server is either running the latest version or something newer. Use the latest version in this case.
        return latestVersion;
      default:
        return baselineProtocolVersion;
    }
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

  private void addEntityAndContentTypeHeaders(RestRequestBuilder builder, DataMap dataMap)
    throws IOException
  {
    if (dataMap != null)
    {
      String header = builder.getHeader(RestConstants.HEADER_CONTENT_TYPE);

      ContentType type;
      if(header == null)
      {
        type = _contentType;
        builder.setHeader(RestConstants.HEADER_CONTENT_TYPE, type.getHeaderKey());
      }
      else
      {
        javax.mail.internet.ContentType contentType;
        try
        {
          contentType = new javax.mail.internet.ContentType(header);
        }
        catch (ParseException e)
        {
          throw new IllegalStateException("Unable to parse Content-Type: " + header);
        }

        if (contentType.getBaseType().equalsIgnoreCase(RestConstants.HEADER_VALUE_APPLICATION_JSON))
        {
          type = ContentType.JSON;
        }
        else if (contentType.getBaseType().equalsIgnoreCase(RestConstants.HEADER_VALUE_APPLICATION_PSON))
        {
          type = ContentType.PSON;
        }
        else
        {
          throw new IllegalStateException("Unknown Content-Type: " + contentType.toString());
        }
      }

      switch (type)
      {
        case PSON:
          builder.setEntity(PSON_DATA_CODEC.mapToBytes(dataMap));
          break;
        case JSON:
          builder.setEntity(JACKSON_DATA_CODEC.mapToBytes(dataMap));
          break;
        default:
          throw new IllegalStateException("Unknown ContentType:" + type);
      }
    }

  }

  /**
   * Sends a type-bound REST request using a callback.
   *
   * @param requestBuilder to invoke {@link com.linkedin.restli.client.RequestBuilder#build()} on to obtain the request
   *                       to send.
   * @param requestContext context for the request
   * @param callback to call on request completion. In the event of an error, the callback
   *                 will receive a {@link com.linkedin.r2.RemoteInvocationException}. If a valid
   *                 error response was received from the remote server, the callback will receive
   *                 a {@link RestLiResponseException} containing the error details.
   */
  public <T> void sendRequest(final RequestBuilder<? extends Request<T>> requestBuilder,
                              RequestContext requestContext,
                              Callback<Response<T>> callback)
  {
    sendRequest(requestBuilder.build(), requestContext, callback);
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
   * Sends a type-bound REST request, returning a future
   * @param request to send
   * @param errorHandlingBehavior error handling behavior
   * @return response future
   */
  public <T> ResponseFuture<T> sendRequest(Request<T> request, ErrorHandlingBehavior errorHandlingBehavior)
  {
    return sendRequest(request, new RequestContext(), errorHandlingBehavior);
  }

  /**
   * Sends a type-bound REST request, returning a future
   *
   * @param requestBuilder to invoke {@link com.linkedin.restli.client.RequestBuilder#build()} on to obtain the request
   *                       to send.
   * @return response future
   */
  public <T> ResponseFuture<T> sendRequest(RequestBuilder<? extends Request<T>> requestBuilder)
  {
    return sendRequest(requestBuilder.build(), new RequestContext());
  }

  /**
   * Sends a type-bound REST request, returning a future
   *
   * @param requestBuilder to invoke {@link com.linkedin.restli.client.RequestBuilder#build()} on to obtain the request
   *                       to send.
   * @param errorHandlingBehavior error handling behavior
   * @return response future
   */
  public <T> ResponseFuture<T> sendRequest(RequestBuilder<? extends Request<T>> requestBuilder,
                                           ErrorHandlingBehavior errorHandlingBehavior)
  {
    return sendRequest(requestBuilder.build(), new RequestContext(), errorHandlingBehavior);
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
  public <T> void sendRequest(final Request<T> request, Callback<Response<T>> callback)
  {
    sendRequest(request, new RequestContext(), callback);
  }

  /**
   * Sends a type-bound REST request using a callback.
   *
   * @param requestBuilder to invoke {@link com.linkedin.restli.client.RequestBuilder#build()} on to obtain the request
   *                       to send.
   * @param callback to call on request completion. In the event of an error, the callback
   *                 will receive a {@link com.linkedin.r2.RemoteInvocationException}. If a valid
   *                 error response was received from the remote server, the callback will receive
   *                 a {@link RestLiResponseException} containing the error details.
   */
  public <T> void sendRequest(final RequestBuilder<? extends Request<T>> requestBuilder, Callback<Response<T>> callback)
  {
    sendRequest(requestBuilder.build(), new RequestContext(), callback);
  }

  /**
   * Sends an untyped REST request using a callback.
   *
   * @param requestContext context for the request
   * @param uri for resource
   * @param hasPrefix
   * @param method to perform
   * @param dataMap request body entity
   * @param protocolVersion the version of the Rest.li protocol used to build this request
   * @param callback to call on request completion. In the event of an error, the callback
   *                 will receive a {@link com.linkedin.r2.RemoteInvocationException}. If a valid
   *                 error response was received from the remote server, the callback will receive
   *                 a {@link com.linkedin.r2.message.rest.RestException} containing the error details.
   */
  private void sendRequestImpl(RequestContext requestContext,
                               URI uri,
                               boolean hasPrefix,
                               ResourceMethod method,
                               DataMap dataMap,
                               Map<String, String> headers,
                               String methodName,
                               ProtocolVersion protocolVersion,
                               Callback<RestResponse> callback)
  {
    try
    {
      RestRequest request = buildRequest(uri, hasPrefix, method, dataMap, headers, protocolVersion);
      String operation = OperationNameGenerator.generate(method, methodName);
      requestContext.putLocalAttr(R2Constants.OPERATION, operation);
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
  private RestRequest buildRequest(URI uri,
                                   boolean hasPrefix,
                                   ResourceMethod method,
                                   DataMap dataMap,
                                   Map<String, String> headers,
                                   ProtocolVersion protocolVersion) throws Exception
  {
    if (!hasPrefix)
    {
      try
      {
        uri = new URI(_uriPrefix + uri.toString());
      }
      catch (URISyntaxException e)
      {
        throw new IllegalArgumentException(e);
      }
    }

    RestRequestBuilder requestBuilder = new RestRequestBuilder(uri).setMethod(
            method.getHttpMethod().toString());

    requestBuilder.setHeaders(headers);
    addAcceptHeaders(requestBuilder);
    addEntityAndContentTypeHeaders(requestBuilder, dataMap);
    addProtocolVersionHeader(requestBuilder, protocolVersion);

    if (method.getHttpMethod() == HttpMethod.POST)
    {
      requestBuilder.setHeader(RestConstants.HEADER_RESTLI_REQUEST_METHOD, method.toString());
    }

    return requestBuilder.build();
  }

  /**
   * Adds the protocol version of Rest.li used to build the request to the headers for this request
   * @param builder
   * @param protocolVersion
   */
  private void addProtocolVersionHeader(RestRequestBuilder builder, ProtocolVersion protocolVersion)
  {
    builder.setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString());
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

  public static enum ContentType
  {
    PSON(RestConstants.HEADER_VALUE_APPLICATION_PSON),
    JSON(RestConstants.HEADER_VALUE_APPLICATION_JSON);

    private String _headerKey;

    private ContentType(String headerKey)
    {
      _headerKey = headerKey;
    }

    public String getHeaderKey()
    {
      return _headerKey;
    }
  }

  private static class RestLiCallbackAdapter<T> extends CallbackAdapter<Response<T>, RestResponse>
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
      return ExceptionUtil.exceptionForThrowable(error, _decoder);
    }
  }
}
