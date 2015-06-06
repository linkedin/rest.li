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

package com.linkedin.restli.server.multiplexer;


import com.linkedin.common.callback.Callback;
import com.linkedin.data.ByteString;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.GetMode;
import com.linkedin.data.template.SetMode;
import com.linkedin.data.template.StringArray;
import com.linkedin.data.template.StringMap;
import com.linkedin.parseq.Engine;
import com.linkedin.parseq.Task;
import com.linkedin.parseq.Tasks;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.restli.common.HttpMethod;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.multiplexer.IndividualRequest;
import com.linkedin.restli.common.multiplexer.IndividualResponse;
import com.linkedin.restli.common.multiplexer.IndividualResponseArray;
import com.linkedin.restli.common.multiplexer.MultiplexedRequestContent;
import com.linkedin.restli.common.multiplexer.MultiplexedResponseContent;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import com.linkedin.restli.server.RestLiServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;


/**
 * Multiplexer implementation.
 *
 * @author Dmitriy Yefremov
 */
public class MultiplexedRequestHandlerImpl implements MultiplexedRequestHandler
{
  private static final String MUX_URI_PATH = "/mux";

  private final Logger _log = LoggerFactory.getLogger(MultiplexedRequestHandlerImpl.class);
  private final RestRequestHandler _requestHandler;
  private final Engine _engine;

  /**
   * @param requestHandler the handler that will take care of individual requests
   * @param engine         ParSeq engine to run request handling on
   */
  public MultiplexedRequestHandlerImpl(RestRequestHandler requestHandler, Engine engine)
  {
    _requestHandler = requestHandler;
    _engine = engine;
  }

  @Override
  public boolean isMultiplexedRequest(RestRequest request)
  {
    // we don't check the method here because we want to return 405 if it anything but POST
    return MUX_URI_PATH.equals(request.getURI().getPath());
  }

  @Override
  public void handleRequest(RestRequest request, RequestContext requestContext, final Callback<RestResponse> callback)
  {
    if (HttpMethod.POST != HttpMethod.valueOf(request.getMethod()))
    {
      _log.error("POST is expected, but " + request.getMethod() + " received");
      callback.onError(new RestLiServiceException(HttpStatus.S_405_METHOD_NOT_ALLOWED));
      return;
    }
    List<IndividualRequest> individualRequests;
    try
    {
      individualRequests = extractIndividualRequests(request);
    }
    catch (Exception e)
    {
      _log.error("Invalid multiplexed request", e);
      callback.onError(new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST));
      return;
    }
    // prepare the list of individual responses to be collected
    final List<IndividualResponse> individualResponses = new ArrayList<IndividualResponse>(individualRequests.size());
    // all tasks are Void and side effect based, that will be useful when we add streaming
    Task<Void> requestProcessingTask = createParallelRequestsTask(requestContext, individualRequests, individualResponses);
    Task<Void> responseAggregationTask = Tasks.action("send aggregated response", new Runnable()
    {
      @Override
      public void run()
      {
        RestResponse aggregatedResponse = aggregateResponses(individualResponses);
        callback.onSuccess(aggregatedResponse);
      }
    });
    _engine.run(Tasks.seq(requestProcessingTask, responseAggregationTask));
  }

  /**
   * Extracts individual requests from the given REST request.
   *
   * @return a non-empty list of individual requests
   */
  private List<IndividualRequest> extractIndividualRequests(RestRequest restRequest)
  {
    validateHeaders(restRequest);
    DataMap data = DataMapUtils.readMap(restRequest);
    MultiplexedRequestContent multiplexedRequestContent = DataTemplateUtil.wrap(data, MultiplexedRequestContent.class);
    List<IndividualRequest> individualRequests = multiplexedRequestContent.getRequests();
    if (individualRequests.isEmpty())
    {
      throw new IllegalArgumentException("No individual requests to process");
    }
    return individualRequests;
  }

  private void validateHeaders(RestRequest request)
  {
    String contentType = request.getHeader(RestConstants.HEADER_CONTENT_TYPE);
    boolean valid = contentType != null && RestConstants.HEADER_VALUE_APPLICATION_JSON.equals(contentType);
    if (!valid)
    {
      throw new IllegalArgumentException("Invalid headers");
    }
  }

  private Task<Void> createParallelRequestsTask(RequestContext requestContext,
                                                List<IndividualRequest> individualRequests,
                                                List<IndividualResponse> individualResponses)
  {
    List<Task<Void>> tasks = new ArrayList<Task<Void>>(individualRequests.size());
    for (IndividualRequest individualRequest : individualRequests)
    {
      // create a task for the current request
      Task<Void> individualRequestTask = createRequestHandlingTask(requestContext, individualRequest, individualResponses);
      List<IndividualRequest> dependentRequests = individualRequest.getDependentRequests();
      if (dependentRequests.isEmpty())
      {
        tasks.add(individualRequestTask);
      }
      else
      {
        // recursively process dependent requests
        Task<Void> dependentRequestsTask = createParallelRequestsTask(requestContext, dependentRequests, individualResponses);
        // tasks for dependant requests are executed after the current request's task
        tasks.add(Tasks.seq(individualRequestTask, dependentRequestsTask));
      }
    }
    return toVoid(Tasks.par(tasks));
  }

  private Task<Void> createRequestHandlingTask(RequestContext requestContext,
                                               final IndividualRequest individualRequest,
                                               final List<IndividualResponse> individualResponses)
  {
    RestRequest individualRestRequest = createSyntheticRequest(individualRequest);
    final RequestHandlingTask responseTask = new RequestHandlingTask(_requestHandler, individualRestRequest, requestContext);
    Task<Void> addResponseTask = Tasks.action("add response", new Runnable()
    {
      @Override
      public void run()
      {
        IndividualResponse individualResponse = toIndividualResponse(individualRequest.getId(), responseTask.get());
        individualResponses.add(individualResponse);
      }
    });
    return Tasks.seq(responseTask, addResponseTask);
  }

  private RestRequest createSyntheticRequest(IndividualRequest individualRequest)
  {
    URI uri = URI.create(individualRequest.getRelativeUrl());
    RestRequestBuilder builder = new RestRequestBuilder(uri);
    builder.setMethod(individualRequest.getMethod());
    builder.setHeaders(individualRequest.getHeaders());
    builder.setCookies(individualRequest.getCookies());
    builder.setEntity(getEntity(individualRequest));
    return builder.build();
  }

  private ByteString getEntity(IndividualRequest individualRequest)
  {
    ByteString body = individualRequest.getBody(GetMode.NULL);
    if (body == null)
    {
      return ByteString.empty();
    }
    else
    {
      return body;
    }
  }

  private IndividualResponse toIndividualResponse(int id, RestResponse restResponse)
  {
    IndividualResponse individualResponse = new IndividualResponse();
    individualResponse.setId(id);
    individualResponse.setStatus(restResponse.getStatus());
    individualResponse.setHeaders(new StringMap(restResponse.getHeaders()));
    individualResponse.setCookies(new StringArray(restResponse.getCookies()));
    individualResponse.setBody(getBody(restResponse), SetMode.IGNORE_NULL);
    return individualResponse;
  }

  private ByteString getBody(RestResponse restResponse)
  {
    ByteString data = restResponse.getEntity();
    if (data.isEmpty())
    {
      return null;
    }
    else
    {
      return data;
    }
  }

  private RestResponse aggregateResponses(List<IndividualResponse> responses)
  {
    MultiplexedResponseContent aggregatedResponseContent = new MultiplexedResponseContent();
    aggregatedResponseContent.setResponses(new IndividualResponseArray(responses));
    byte[] aggregatedResponseData = DataMapUtils.mapToBytes(aggregatedResponseContent.data());
    return new RestResponseBuilder()
        .setStatus(HttpStatus.S_200_OK.getCode())
        .setEntity(aggregatedResponseData)
        .build();
  }

  /**
   * Converts a Task<List<Void>> into a Task<Void>. That is a hack to make the type system happy.
   * This method adds an unneeded empty task to the execution plan.
   */
  private Task<Void> toVoid(Task<List<Void>> task)
  {
    Task<Void> doNothingTask = Tasks.action("do nothing", new Runnable()
    {
      @Override
      public void run()
      {
        // seriously, nothing
      }
    });
    return Tasks.seq(task, doNothingTask);
  }
}
