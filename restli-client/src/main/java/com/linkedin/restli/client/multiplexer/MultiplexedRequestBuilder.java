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
import com.linkedin.data.ByteString;
import com.linkedin.data.template.JacksonDataTemplateCodec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.SetMode;
import com.linkedin.data.template.StringMap;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestLiCallbackAdapter;
import com.linkedin.restli.client.RestLiEncodingException;
import com.linkedin.restli.client.uribuilders.RestliUriBuilderUtil;
import com.linkedin.restli.common.multiplexer.IndividualRequest;
import com.linkedin.restli.common.multiplexer.IndividualRequestArray;
import com.linkedin.restli.common.multiplexer.MultiplexedRequestContent;
import com.linkedin.restli.internal.common.AllProtocolVersions;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
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
  private static final JacksonDataTemplateCodec TEMPLATE_CODEC = new JacksonDataTemplateCodec();

  private final List<RequestWithCallback<?>> _requestsWithCallbacks = new ArrayList<RequestWithCallback<?>>();
  private final boolean _isParallel;

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
    _requestsWithCallbacks.add(new RequestWithCallback<T>(request, callback));
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
    Map<Integer, Callback<RestResponse>> callbacks = new HashMap<Integer, Callback<RestResponse>>(_requestsWithCallbacks.size());
    List<IndividualRequest> individualRequests = new ArrayList<IndividualRequest>(_requestsWithCallbacks.size());
    // Dependant requests list is always empty
    List<IndividualRequest> dependantRequests = Collections.emptyList();
    for (int i = 0; i < _requestsWithCallbacks.size(); i++)
    {
      RequestWithCallback<?> requestWithCallback = _requestsWithCallbacks.get(i);
      IndividualRequest individualRequest = toIndividualRequest(i, requestWithCallback.getRequest(), dependantRequests);
      individualRequests.add(individualRequest);
      callbacks.put(i, wrapCallback(requestWithCallback));
    }
    return toMultiplexedRequest(individualRequests, callbacks);
  }

  private MultiplexedRequest buildSequential() throws RestLiEncodingException
  {
    Map<Integer, Callback<RestResponse>> callbacks = new HashMap<Integer, Callback<RestResponse>>(_requestsWithCallbacks.size());
    // Dependent requests - requests which are dependent on the current request (executed after the current request)
    List<IndividualRequest> dependantRequests = Collections.emptyList();
    // We start with the last request in the list and proceed backwards because sequential ordering is built using reverse dependencies
    for (int i = _requestsWithCallbacks.size() - 1; i >= 0; i--)
    {
      RequestWithCallback<?> requestWithCallback = _requestsWithCallbacks.get(i);
      IndividualRequest individualRequest = toIndividualRequest(i, requestWithCallback.getRequest(), dependantRequests);
      dependantRequests = Collections.singletonList(individualRequest);
      callbacks.put(i, wrapCallback(requestWithCallback));
    }
    return toMultiplexedRequest(dependantRequests, callbacks);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Callback<RestResponse> wrapCallback(RequestWithCallback<?> requestWithCallback)
  {
    return new RestLiCallbackAdapter(requestWithCallback.getRequest().getResponseDecoder(),
                                     requestWithCallback.getCallback());
  }

  private IndividualRequest toIndividualRequest(int id, Request<?> request, List<IndividualRequest> dependantRequests) throws RestLiEncodingException
  {
    IndividualRequest individualRequest = new IndividualRequest();
    individualRequest.setId(id);
    individualRequest.setRelativeUrl(getRelativeUrl(request));
    individualRequest.setMethod(request.getMethod().getHttpMethod().name());
    individualRequest.setHeaders(new StringMap(request.getHeaders()));
    individualRequest.setBody(getBody(request), SetMode.IGNORE_NULL);
    individualRequest.setDependentRequests(new IndividualRequestArray(dependantRequests));
    return individualRequest;
  }

  private String getRelativeUrl(Request<?> request)
  {
    URI requestUri = RestliUriBuilderUtil.createUriBuilder(request, "", AllProtocolVersions.LATEST_PROTOCOL_VERSION).build();
    return requestUri.toString();
  }

  /**
   * Tries to extract the body of the given request and serialize it. If there is no body returns null.
   */
  private ByteString getBody(Request<?> request) throws RestLiEncodingException
  {
    RecordTemplate record = request.getInputRecord();
    if (record == null)
    {
      return null;
    }
    else
    {
      try
      {
        byte[] bytes = TEMPLATE_CODEC.dataTemplateToBytes(record, true);
        return ByteString.copy(bytes);
      }
      catch (IOException e)
      {
        throw new RestLiEncodingException("Can't serialize a data map", e);
      }
    }
  }

  private MultiplexedRequest toMultiplexedRequest(List<IndividualRequest> individualRequests, Map<Integer, Callback<RestResponse>> callbacks)
  {
    MultiplexedRequestContent multiplexedRequestContent = new MultiplexedRequestContent();
    multiplexedRequestContent.setRequests(new IndividualRequestArray(individualRequests));
    return new MultiplexedRequest(multiplexedRequestContent, callbacks);
  }
}
