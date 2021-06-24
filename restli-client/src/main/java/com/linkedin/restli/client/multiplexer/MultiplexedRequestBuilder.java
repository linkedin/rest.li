/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.client.multiplexer;


import com.linkedin.common.callback.Callback;
import com.linkedin.data.template.JacksonDataTemplateCodec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.SetMode;
import com.linkedin.data.template.StringMap;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestLiCallbackAdapter;
import com.linkedin.restli.client.RestLiEncodingException;
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.client.uribuilders.RestliUriBuilderUtil;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.multiplexer.IndividualBody;
import com.linkedin.restli.common.multiplexer.IndividualRequest;
import com.linkedin.restli.common.multiplexer.IndividualRequestMap;
import com.linkedin.restli.common.multiplexer.MultiplexedRequestContent;
import com.linkedin.restli.internal.client.RequestBodyTransformer;
import com.linkedin.restli.internal.common.AllProtocolVersions;

import java.net.HttpCookie;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Provides convenient ways to build multiplexed requests.
 *
 * @author Dmitriy Yefremov
 */
public class MultiplexedRequestBuilder
{
  private final List<RequestWithCallback<?>> _requestsWithCallbacks = new ArrayList<>();
  private final boolean _isParallel;
  private RestliRequestOptions _requestOptions = RestliRequestOptions.DEFAULT_MULTIPLEXER_OPTIONS;
  /**
   * Creates a builder for a multiplexed request containing parallel individual requests.
   *
   * @return a new builder instance
   */
  public static MultiplexedRequestBuilder createParallelRequest()
  {
    return new MultiplexedRequestBuilder(true);
  }

  /**
   * Creates a builder for a multiplexed request containing sequential individual requests.
   *
   * @return a new builder instance
   */
  public static MultiplexedRequestBuilder createSequentialRequest()
  {
    return new MultiplexedRequestBuilder(false);
  }

  /**
   * Private constructor.
   *
   * @param isParallel defines whether requests are going to be parallel or sequential
   */
  private MultiplexedRequestBuilder(boolean isParallel)
  {
    _isParallel = isParallel;
  }

  /**
   * Adds a request to the builder. In case of sequential execution individual requests will be executed in the order in
   * which this method is called. For parallel execution the order does not matter.
   *
   * @param callback will be called when the response for the given request is available (no long running code should be
   *                 in the callback because it will block other callbacks from being notified)
   */
  public <T> MultiplexedRequestBuilder addRequest(Request<T> request, Callback<Response<T>> callback)
  {
    _requestsWithCallbacks.add(new RequestWithCallback<>(request, callback));
    return this;
  }

  /**
   * Sets the request options to use for this multiplexed request.
   * @param requestOptions Request options to configure the multiplexed request. Allows customizing content and accept
   *                       types.
   */
  public MultiplexedRequestBuilder setRequestOptions(RestliRequestOptions requestOptions)
  {
    _requestOptions = requestOptions;
    return this;
  }

  /**
   * Builds a multiplexed request from the current state of the builder.
   *
   * @return the request
   * @throws IllegalStateException   if there were no requests added
   * @throws RestLiEncodingException if there is an error encoding individual requests
   */
  public MultiplexedRequest build() throws RestLiEncodingException
  {
    if (_requestsWithCallbacks.isEmpty())
    {
      throw new IllegalStateException("No requests provided for multiplexing");
    }
    if (_isParallel)
    {
      return buildParallel();
    }
    else
    {
      return buildSequential();
    }
  }

  private MultiplexedRequest buildParallel() throws RestLiEncodingException
  {
    Map<Integer, Callback<RestResponse>> callbacks = new HashMap<>(_requestsWithCallbacks.size());
    IndividualRequestMap individualRequests = new IndividualRequestMap(_requestsWithCallbacks.size());
    // Dependent requests map is always empty
    IndividualRequestMap dependentRequests = new IndividualRequestMap();
    for (int i = 0; i < _requestsWithCallbacks.size(); i++)
    {
      RequestWithCallback<?> requestWithCallback = _requestsWithCallbacks.get(i);
      IndividualRequest individualRequest = toIndividualRequest(requestWithCallback.getRequest(), dependentRequests);
      individualRequests.put(Integer.toString(i), individualRequest);
      callbacks.put(i, wrapCallback(requestWithCallback));
    }
    return toMultiplexedRequest(individualRequests, callbacks, _requestOptions);
  }

  private MultiplexedRequest buildSequential() throws RestLiEncodingException
  {
    Map<Integer, Callback<RestResponse>> callbacks = new HashMap<>(_requestsWithCallbacks.size());
    // Dependent requests - requests which are dependent on the current request (executed after the current request)
    IndividualRequestMap dependentRequests = new IndividualRequestMap();
    // We start with the last request in the list and proceed backwards because sequential ordering is built using reverse dependencies
    for (int i = _requestsWithCallbacks.size() - 1; i >= 0; i--)
    {
      RequestWithCallback<?> requestWithCallback = _requestsWithCallbacks.get(i);
      IndividualRequest individualRequest = toIndividualRequest(requestWithCallback.getRequest(), dependentRequests);
      dependentRequests = new IndividualRequestMap();
      dependentRequests.put(Integer.toString(i), individualRequest);
      callbacks.put(i, wrapCallback(requestWithCallback));
    }
    return toMultiplexedRequest(dependentRequests, callbacks, _requestOptions);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static Callback<RestResponse> wrapCallback(RequestWithCallback<?> requestWithCallback)
  {
    return new RestLiCallbackAdapter(requestWithCallback.getRequest().getResponseDecoder(),
                                     requestWithCallback.getCallback());
  }

  private static IndividualRequest toIndividualRequest(Request<?> request, IndividualRequestMap dependantRequests) throws RestLiEncodingException
  {
    //TODO: Hardcoding RESTLI_PROTOCOL_2_0_0 for now. We need to refactor this code later to get protocol version using the mechanism similar to
    // RestClient.getProtocolVersionForService()
    ProtocolVersion protocolVersion = AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion();
    String relativeUrl = getRelativeUrl(request, protocolVersion);
    IndividualRequest individualRequest = new IndividualRequest();
    individualRequest.setRelativeUrl(relativeUrl);
    individualRequest.setMethod(request.getMethod().getHttpMethod().name());
    individualRequest.setHeaders(new StringMap(request.getHeaders()));
    List<HttpCookie> cookies = request.getCookies();
    if (cookies != null && !cookies.isEmpty())
    {
      throw new IllegalArgumentException(String.format("Cookies for individual request '%s' MUST be added at the envelope request level", relativeUrl));
    }
    individualRequest.setBody(getBody(request, protocolVersion), SetMode.IGNORE_NULL);
    individualRequest.setDependentRequests(dependantRequests);
    return individualRequest;
  }

  private static String getRelativeUrl(Request<?> request, ProtocolVersion protocolVersion)
  {
    URI requestUri = RestliUriBuilderUtil.createUriBuilder(request, "", protocolVersion).build();
    return requestUri.toString();
  }

  /**
   * Tries to extract the body of the given request and serialize it. If there is no body returns null.
   */
  private static IndividualBody getBody(Request<?> request, ProtocolVersion protocolVersion) throws RestLiEncodingException
  {
    RecordTemplate record = request.getInputRecord();
    if (record == null)
    {
      return null;
    }
    else
    {
      return new IndividualBody(RequestBodyTransformer.transform(request, protocolVersion));
    }
  }

  private static MultiplexedRequest toMultiplexedRequest(IndividualRequestMap individualRequests,
      Map<Integer, Callback<RestResponse>> callbacks, RestliRequestOptions requestOptions)
  {
    MultiplexedRequestContent multiplexedRequestContent = new MultiplexedRequestContent();
    multiplexedRequestContent.setRequests(individualRequests);
    return new MultiplexedRequest(multiplexedRequestContent, callbacks, requestOptions);
  }
}
