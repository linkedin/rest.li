/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.restli.internal.server.filter;


import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.common.attachments.RestLiAttachmentReader;
import com.linkedin.restli.internal.server.RestLiCallback;
import com.linkedin.restli.internal.server.RestLiMethodInvoker;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.methods.arguments.RestLiArgumentBuilder;
import com.linkedin.restli.internal.server.response.PartialRestResponse;
import com.linkedin.restli.internal.server.response.RestLiResponseHandler;
import com.linkedin.restli.server.RequestExecutionCallback;
import com.linkedin.restli.server.RequestExecutionReport;
import com.linkedin.restli.server.RequestExecutionReportBuilder;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.RestLiResponseAttachments;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;


/**
 * Concrete implementation of {@link FilterChainCallback}.
 *
 * @author gye
 */
public class FilterChainCallbackImpl implements FilterChainCallback
{
  private RoutingResult _method;
  private RestLiMethodInvoker _methodInvoker;
  private RestLiArgumentBuilder _restLiArgumentBuilder;
  private RequestExecutionReportBuilder _requestExecutionReportBuilder;
  private RestLiAttachmentReader _requestAttachmentReader;
  private RestLiResponseHandler _responseHandler;
  private RequestExecutionCallback<RestResponse> _wrappedCallback;

  public FilterChainCallbackImpl(RoutingResult method,
                                 RestLiMethodInvoker methodInvoker,
                                 RestLiArgumentBuilder adapter,
                                 RequestExecutionReportBuilder requestExecutionReportBuilder,
                                 RestLiAttachmentReader requestAttachmentReader,
                                 RestLiResponseHandler responseHandler,
                                 RequestExecutionCallback<RestResponse> wrappedCallback)
  {
    _method = method;
    _methodInvoker = methodInvoker;
    _restLiArgumentBuilder = adapter;
    _requestExecutionReportBuilder = requestExecutionReportBuilder;
    _requestAttachmentReader = requestAttachmentReader;
    _responseHandler = responseHandler;
    _wrappedCallback = wrappedCallback;
  }

  @Override
  public void onRequestSuccess(final RestLiRequestData requestData, final RestLiCallback<Object> restLiCallback)
  {
    _methodInvoker.invoke(requestData, _method, _restLiArgumentBuilder, restLiCallback,
                          _requestExecutionReportBuilder);
  }

  @Override
  public void onResponseSuccess(final RestLiResponseData responseData,
                                final RestLiResponseAttachments responseAttachments)
  {
    final PartialRestResponse response = _responseHandler.buildPartialResponse(_method, responseData);
    _wrappedCallback.onSuccess(_responseHandler.buildResponse(_method, response), getRequestExecutionReport(),
                               responseAttachments);
  }

  @Override
  public void onError(Throwable th, final RestLiResponseData responseData,
                      final RestLiResponseAttachments responseAttachments)
  {
    RestLiServiceException e = responseData.getServiceException();
    final PartialRestResponse response = _responseHandler.buildPartialResponse(_method, responseData);

    _wrappedCallback.onError(_responseHandler.buildRestException(e, response), getRequestExecutionReport(),
                             _requestAttachmentReader, responseAttachments);
  }

  private RequestExecutionReport getRequestExecutionReport() {
    return _requestExecutionReportBuilder == null ? null : _requestExecutionReportBuilder.build();
  }
}
