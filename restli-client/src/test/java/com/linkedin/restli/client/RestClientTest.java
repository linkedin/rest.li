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

/**
 * $Id: $
 */

package com.linkedin.restli.client;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.restli.common.ContentType;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.ErrorDetails;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceSpecImpl;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.client.EntityResponseDecoder;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.DataMapConverter;
import com.linkedin.restli.internal.common.TestConstants;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpCookie;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.activation.MimeTypeParseException;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class RestClientTest
{
  private static final RequestContext DEFAULT_REQUEST_CONTEXT = new RequestContext();
  static
  {
    DEFAULT_REQUEST_CONTEXT.putLocalAttr("__attr1", "1");
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testEmptyErrorResponse()
  {
    RestResponse response = new RestResponseBuilder().setStatus(200).build();
    RestLiResponseException e = new RestLiResponseException(response, null, new ErrorResponse());

    Assert.assertNull(e.getServiceErrorMessage());
    Assert.assertNull(e.getErrorDetails());
    Assert.assertNull(e.getErrorDetailsRecord());
    Assert.assertNull(e.getErrorSource());
    Assert.assertFalse(e.hasServiceErrorCode());
    Assert.assertNull(e.getServiceErrorStackTrace());
    Assert.assertNull(e.getServiceExceptionClass());
    Assert.assertNull(e.getCode());
    Assert.assertNull(e.getDocUrl());
    Assert.assertNull(e.getRequestId());
    Assert.assertNull(e.getErrorDetailType());
  }

  @Test
  public void testShutdown()
  {
    Client client = EasyMock.createMock(Client.class);

    @SuppressWarnings("unchecked")
    Callback<None> callback = EasyMock.createMock(Callback.class);
    Capture<Callback<None>> callbackCapture = EasyMock.newCapture();

    // Underlying client's shutdown should be invoked with correct callback
    client.shutdown(EasyMock.capture(callbackCapture));
    EasyMock.replay(client);

    // No methods should be invoked on the callback
    EasyMock.replay(callback);

    RestClient restClient = new RestClient(client, "d2://");
    restClient.shutdown(callback);

    EasyMock.verify(client);
    EasyMock.verify(callback);

    EasyMock.reset(callback);

    None none = None.none();
    callback.onSuccess(none);
    EasyMock.replay(callback);

    Callback<None> captured = callbackCapture.getValue();
    captured.onSuccess(none);

    EasyMock.verify(callback);

  }

  private enum SendRequestOption
  {
    REQUEST_NO_CONTEXT(false, false),
    REQUEST_WITH_CONTEXT(false, true),
    REQUESTBUILDER_NO_CONTEXT(true, false),
    REQUESTBUILDER_WITH_CONTEXT(true, true);

    private SendRequestOption(boolean requestBuilder, boolean context)
    {
      _requestBuilder = requestBuilder;
      _context = context;
    }

    private final boolean _requestBuilder;
    private final boolean _context;
  }

  private enum GetResponseOption
  {
    GET,
    GET_RESPONSE,
    GET_RESPONSE_EXPLICIT_NO_THROW,
    GET_RESPONSE_EXPLICIT_THROW,
    GET_RESPONSE_ENTITY,
    GET_RESPONSE_ENTITY_EXPLICIT_NO_THROW,
    GET_RESPONSE_ENTITY_EXPLICIT_THROW,
  }

  private enum TimeoutOption
  {
    NO_TIMEOUT(null, null),
    THIRTY_SECONDS(30L, TimeUnit.SECONDS);

    private TimeoutOption(Long l, TimeUnit timeUnit)
    {
      _l = l;
      _timeUnit = timeUnit;
    }

    private final Long _l;
    private final TimeUnit _timeUnit;
  }

  private enum ContentTypeOption
  {
    JSON(ContentType.JSON),
    LICOR_TEXT(ContentType.LICOR_TEXT),
    LICOR_BINARY(ContentType.LICOR_BINARY),
    PROTOBUF(ContentType.PROTOBUF),
    PROTOBUF2(ContentType.PROTOBUF2),
    PSON(ContentType.PSON),
    SMILE(ContentType.SMILE);

    ContentTypeOption(ContentType contentType)
    {
      _contentType = contentType;
    }

    private final ContentType _contentType;
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "sendRequestOptions")
  private Object[][] sendRequestOptions()
  {
    Object[][] result = new Object[SendRequestOption.values().length *
                                   TimeoutOption.values().length *
                                   ContentTypeOption.values().length *
                                   2][];
    int i = 0;
    for (SendRequestOption sendRequestOption : SendRequestOption.values())
    {
      for (TimeoutOption timeoutOption : TimeoutOption.values())
      {
        for (ContentTypeOption contentTypeOption : ContentTypeOption.values())
        {
          result[i++] = new Object[] {
              sendRequestOption,
              timeoutOption,
              ProtocolVersionOption.USE_LATEST_IF_AVAILABLE,
              AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
              RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE,
              contentTypeOption._contentType
          };
          result[i++] = new Object[] {
              sendRequestOption,
              timeoutOption,
              ProtocolVersionOption.FORCE_USE_NEXT,
              AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
              RestConstants.HEADER_RESTLI_ERROR_RESPONSE,
              contentTypeOption._contentType
          };
        }
      }
    }
    return result;
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "sendRequestAndGetResponseOptions")
  private Object[][] sendRequestAndGetResponseOptions()
  {
    Object[][] result = new Object[SendRequestOption.values().length *
                                   GetResponseOption.values().length *
                                   TimeoutOption.values().length *
                                   ContentTypeOption.values().length *
                                   2][];
    int i = 0;
    for (SendRequestOption sendRequestOption : SendRequestOption.values())
    {
      for (GetResponseOption getResponseOption : GetResponseOption.values() )
      {
        for (TimeoutOption timeoutOption : TimeoutOption.values())
        {
          for (ContentTypeOption contentTypeOption : ContentTypeOption.values())
          {
            result[i++] = new Object[]{
                sendRequestOption,
                getResponseOption,
                timeoutOption,
                ProtocolVersionOption.USE_LATEST_IF_AVAILABLE,
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE,
                contentTypeOption._contentType};
            result[i++] =
                new Object[]{
                    sendRequestOption,
                    getResponseOption,
                    timeoutOption,
                    ProtocolVersionOption.FORCE_USE_NEXT,
                    AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                    RestConstants.HEADER_RESTLI_ERROR_RESPONSE,
                    contentTypeOption._contentType};
          }
        }
      }
    }
    return result;
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "sendRequestAndNoThrowGetResponseOptions")
  private Object[][] sendRequestAndNoThrowGetResponseOptions()
  {
    Object[][] result = new Object[SendRequestOption.values().length *
                                   TimeoutOption.values().length *
                                   ContentTypeOption.values().length *
                                   4][];
    int i = 0;
    for (SendRequestOption sendRequestOption : SendRequestOption.values())
    {
      for (TimeoutOption timeoutOption : TimeoutOption.values())
      {
        for (ContentTypeOption contentTypeOption : ContentTypeOption.values())
        {
          result[i++] = new Object[] {
              sendRequestOption,
              GetResponseOption.GET_RESPONSE_ENTITY_EXPLICIT_NO_THROW,
              timeoutOption,
              ProtocolVersionOption.USE_LATEST_IF_AVAILABLE,
              AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
              RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE,
              contentTypeOption._contentType
          };
          result[i++] = new Object[] {
              sendRequestOption,
              GetResponseOption.GET_RESPONSE_ENTITY_EXPLICIT_NO_THROW,
              timeoutOption,
              ProtocolVersionOption.FORCE_USE_NEXT,
              AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
              RestConstants.HEADER_RESTLI_ERROR_RESPONSE,
              contentTypeOption._contentType
          };
          result[i++] = new Object[] {
              sendRequestOption,
              GetResponseOption.GET_RESPONSE_EXPLICIT_NO_THROW,
              timeoutOption,
              ProtocolVersionOption.USE_LATEST_IF_AVAILABLE,
              AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
              RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE,
              contentTypeOption._contentType
          };
          result[i++] = new Object[] {
              sendRequestOption,
              GetResponseOption.GET_RESPONSE_EXPLICIT_NO_THROW,
              timeoutOption,
              ProtocolVersionOption.FORCE_USE_NEXT,
              AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
              RestConstants.HEADER_RESTLI_ERROR_RESPONSE,
              contentTypeOption._contentType
          };
        }
      }
    }

    return result;
  }

  @SuppressWarnings("deprecation")
  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "sendRequestAndGetResponseOptions")
  public void testRestLiResponseFuture(SendRequestOption sendRequestOption,
                                       GetResponseOption getResponseOption,
                                       TimeoutOption timeoutOption,
                                       ProtocolVersionOption versionOption,
                                       ProtocolVersion protocolVersion,
                                       String errorResponseHeaderName,
                                       ContentType contentType)
    throws ExecutionException, RemoteInvocationException,
           TimeoutException, InterruptedException, IOException
  {
    final String ERR_KEY = "someErr";
    final String ERR_VALUE = "WHOOPS!";
    final String ERR_MSG = "whoops2";
    final int HTTP_CODE = 200;
    final int APP_CODE = 666;
    final String CODE = "INVALID_INPUT";
    final String DOC_URL = "https://example.com/errors/invalid-input";
    final String REQUEST_ID = "abc123";

    RestClient client = mockClient(ERR_KEY, ERR_VALUE, ERR_MSG, HTTP_CODE, APP_CODE, CODE, DOC_URL, REQUEST_ID,
        protocolVersion, errorResponseHeaderName);
    Request<ErrorResponse> request = mockRequest(ErrorResponse.class, versionOption, contentType);
    RequestBuilder<Request<ErrorResponse>> requestBuilder = mockRequestBuilder(request);

    ResponseFuture<ErrorResponse> future = sendRequest(sendRequestOption,
                                                       determineErrorHandlingBehavior(getResponseOption),
                                                       client,
                                                       request,
                                                       requestBuilder);
    Response<ErrorResponse> response = getOkResponse(getResponseOption, future, timeoutOption);
    ErrorResponse e = response.getEntity();

    Assert.assertNull(response.getError());
    Assert.assertFalse(response.hasError());
    Assert.assertEquals(HTTP_CODE, response.getStatus());
    Assert.assertEquals(ERR_VALUE, e.getErrorDetails().data().getString(ERR_KEY));
    Assert.assertEquals(APP_CODE, e.getServiceErrorCode().intValue());
    Assert.assertEquals(ERR_MSG, e.getMessage());
    Assert.assertEquals(CODE, e.getCode());
    Assert.assertEquals(DOC_URL, e.getDocUrl());
    Assert.assertEquals(REQUEST_ID, e.getRequestId());
    Assert.assertEquals(EmptyRecord.class.getCanonicalName(), e.getErrorDetailType());
  }

  @SuppressWarnings("deprecation")
  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "sendRequestAndGetResponseOptions")
  public void testRestLiResponseExceptionFuture(SendRequestOption sendRequestOption,
                                                GetResponseOption getResponseOption,
                                                TimeoutOption timeoutOption,
                                                ProtocolVersionOption versionOption,
                                                ProtocolVersion protocolVersion,
                                                String errorResponseHeaderName,
                                                ContentType contentType)
    throws RemoteInvocationException, TimeoutException, InterruptedException, IOException
  {
    final String ERR_KEY = "someErr";
    final String ERR_VALUE = "WHOOPS!";
    final String ERR_MSG = "whoops2";
    final int HTTP_CODE = 400;
    final int APP_CODE = 666;
    final String CODE = "INVALID_INPUT";
    final String DOC_URL = "https://example.com/errors/invalid-input";
    final String REQUEST_ID = "abc123";

    RestClient client = mockClient(ERR_KEY, ERR_VALUE, ERR_MSG, HTTP_CODE, APP_CODE, CODE, DOC_URL, REQUEST_ID,
        protocolVersion, errorResponseHeaderName);
    Request<EmptyRecord> request = mockRequest(EmptyRecord.class, versionOption, contentType);
    RequestBuilder<Request<EmptyRecord>> requestBuilder = mockRequestBuilder(request);

    ResponseFuture<EmptyRecord> future = sendRequest(sendRequestOption,
                                                     determineErrorHandlingBehavior(getResponseOption),
                                                     client,
                                                     request,
                                                     requestBuilder);
    RestLiResponseException e = getErrorResponse(getResponseOption, future, timeoutOption);

    if (getResponseOption == GetResponseOption.GET_RESPONSE_ENTITY_EXPLICIT_NO_THROW)
    {
      Assert.assertNull(e);
    }
    else
    {
      Assert.assertEquals(HTTP_CODE, e.getStatus());
      Assert.assertEquals(ERR_VALUE, e.getErrorDetails().get(ERR_KEY));
      Assert.assertEquals(APP_CODE, e.getServiceErrorCode());
      Assert.assertEquals(ERR_MSG, e.getServiceErrorMessage());
      Assert.assertEquals(CODE, e.getCode());
      Assert.assertEquals(DOC_URL, e.getDocUrl());
      Assert.assertEquals(REQUEST_ID, e.getRequestId());
      Assert.assertEquals(EmptyRecord.class.getCanonicalName(), e.getErrorDetailType());
      Assert.assertNotNull(e.getErrorDetailsRecord());
      Assert.assertTrue(e.getErrorDetailsRecord() instanceof EmptyRecord);
    }
  }

  @SuppressWarnings("deprecation")
  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "sendRequestAndNoThrowGetResponseOptions")
  public void testRestLiResponseExceptionFutureNoThrow(SendRequestOption sendRequestOption,
                                                       GetResponseOption getResponseOption,
                                                       TimeoutOption timeoutOption,
                                                       ProtocolVersionOption versionOption,
                                                       ProtocolVersion protocolVersion,
                                                       String errorResponseHeaderName,
                                                       ContentType contentType)
      throws RemoteInvocationException, ExecutionException, TimeoutException, InterruptedException, IOException
  {
    final String ERR_KEY = "someErr";
    final String ERR_VALUE = "WHOOPS!";
    final String ERR_MSG = "whoops2";
    final int HTTP_CODE = 400;
    final int APP_CODE = 666;
    final String CODE = "INVALID_INPUT";
    final String DOC_URL = "https://example.com/errors/invalid-input";
    final String REQUEST_ID = "abc123";

    RestClient client = mockClient(ERR_KEY, ERR_VALUE, ERR_MSG, HTTP_CODE, APP_CODE, CODE, DOC_URL, REQUEST_ID,
            protocolVersion, errorResponseHeaderName);
    Request<EmptyRecord> request = mockRequest(EmptyRecord.class, versionOption, contentType);
    RequestBuilder<Request<EmptyRecord>> requestBuilder = mockRequestBuilder(request);

    ResponseFuture<EmptyRecord> future = sendRequest(sendRequestOption,
                                                     determineErrorHandlingBehavior(getResponseOption),
                                                     client,
                                                     request,
                                                     requestBuilder);

    Response<EmptyRecord> response = getOkResponse(getResponseOption, future, timeoutOption);
    Assert.assertTrue(response.hasError());
    RestLiResponseException e = response.getError();

    Assert.assertNotNull(e);
    Assert.assertEquals(HTTP_CODE, e.getStatus());
    Assert.assertEquals(ERR_VALUE, e.getErrorDetails().get(ERR_KEY));
    Assert.assertEquals(APP_CODE, e.getServiceErrorCode());
    Assert.assertEquals(ERR_MSG, e.getServiceErrorMessage());
    Assert.assertEquals(CODE, e.getCode());
    Assert.assertEquals(DOC_URL, e.getDocUrl());
    Assert.assertEquals(REQUEST_ID, e.getRequestId());
    Assert.assertEquals(EmptyRecord.class.getCanonicalName(), e.getErrorDetailType());
    Assert.assertNotNull(e.getErrorDetailsRecord());
    Assert.assertTrue(e.getErrorDetailsRecord() instanceof EmptyRecord);
  }

  @SuppressWarnings("deprecation")
  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "sendRequestOptions")
  public void testRestLiResponseExceptionCallback(SendRequestOption option,
                                                  TimeoutOption timeoutOption,
                                                  ProtocolVersionOption versionOption,
                                                  ProtocolVersion protocolVersion,
                                                  String errorResponseHeaderName,
                                                  ContentType contentType)
          throws ExecutionException, TimeoutException, InterruptedException, RestLiDecodingException
  {
    final String ERR_KEY = "someErr";
    final String ERR_VALUE = "WHOOPS!";
    final String ERR_MSG = "whoops2";
    final int HTTP_CODE = 400;
    final int APP_CODE = 666;
    final String CODE = "INVALID_INPUT";
    final String DOC_URL = "https://example.com/errors/invalid-input";
    final String REQUEST_ID = "abc123";

    RestClient client = mockClient(ERR_KEY, ERR_VALUE, ERR_MSG, HTTP_CODE, APP_CODE, CODE, DOC_URL, REQUEST_ID,
        protocolVersion, errorResponseHeaderName);
    Request<EmptyRecord> request = mockRequest(EmptyRecord.class, versionOption, contentType);
    RequestBuilder<Request<EmptyRecord>> requestBuilder = mockRequestBuilder(request);

    FutureCallback<Response<EmptyRecord>> callback = new FutureCallback<Response<EmptyRecord>>();
    try
    {
      sendRequest(option, client, request, requestBuilder, callback);
      Long l = timeoutOption._l;
      TimeUnit timeUnit = timeoutOption._timeUnit;
      Response<EmptyRecord> response = l == null ? callback.get() : callback.get(l, timeUnit);
      Assert.fail("Should have thrown");
    }
    catch (ExecutionException e)
    {
      // New

      Throwable cause = e.getCause();
      Assert.assertTrue(cause instanceof RestLiResponseException, "Expected RestLiResponseException not " + cause.getClass().getName());
      RestLiResponseException rlre = (RestLiResponseException)cause;
      Assert.assertEquals(HTTP_CODE, rlre.getStatus());
      Assert.assertEquals(ERR_VALUE, rlre.getErrorDetails().get(ERR_KEY));
      Assert.assertEquals(APP_CODE, rlre.getServiceErrorCode());
      Assert.assertEquals(ERR_MSG, rlre.getServiceErrorMessage());
      Assert.assertEquals(CODE, rlre.getCode());
      Assert.assertEquals(DOC_URL, rlre.getDocUrl());
      Assert.assertEquals(REQUEST_ID, rlre.getRequestId());
      Assert.assertEquals(EmptyRecord.class.getCanonicalName(), rlre.getErrorDetailType());
      Assert.assertNotNull(rlre.getErrorDetailsRecord());
      Assert.assertTrue(rlre.getErrorDetailsRecord() instanceof EmptyRecord);

      // Old

      Assert.assertTrue(cause instanceof RestException, "Expected RestException not " + cause.getClass().getName());
      RestException re = (RestException)cause;
      RestResponse r = re.getResponse();

      ErrorResponse er = new EntityResponseDecoder<ErrorResponse>(ErrorResponse.class).decodeResponse(r).getEntity();

      Assert.assertEquals(HTTP_CODE, r.getStatus());
      Assert.assertEquals(ERR_VALUE, er.getErrorDetails().data().getString(ERR_KEY));
      Assert.assertEquals(APP_CODE, er.getServiceErrorCode().intValue());
      Assert.assertEquals(ERR_MSG, er.getMessage());
    }
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "sendRequestOptions")
  public void testRestLiRemoteInvocationException(SendRequestOption option,
                                                  TimeoutOption timeoutOption,
                                                  ProtocolVersionOption versionOption,
                                                  ProtocolVersion protocolVersion,
                                                  String errorResponseHeaderName,
                                                  ContentType contentType)
      throws ExecutionException, TimeoutException, InterruptedException, RestLiDecodingException
  {
    final int HTTP_CODE = 404;
    final String ERR_MSG = "WHOOPS!";

    RestClient client = mockClient(HTTP_CODE, ERR_MSG, protocolVersion);
    Request<EmptyRecord> request = mockRequest(EmptyRecord.class, versionOption, contentType);
    RequestBuilder<Request<EmptyRecord>> requestBuilder = mockRequestBuilder(request);

    FutureCallback<Response<EmptyRecord>> callback = new FutureCallback<Response<EmptyRecord>>();
    try
    {
      sendRequest(option, client, request, requestBuilder, callback);
      Long l = timeoutOption._l;
      TimeUnit timeUnit = timeoutOption._timeUnit;
      Response<EmptyRecord> response = l == null ? callback.get() : callback.get(l, timeUnit);
      Assert.fail("Should have thrown");
    }
    catch (ExecutionException e)
    {
      Throwable cause = e.getCause();
      Assert.assertTrue(cause instanceof RemoteInvocationException,
                        "Expected RemoteInvocationException not " + cause.getClass().getName());
      RemoteInvocationException rlre = (RemoteInvocationException)cause;
      Assert.assertTrue(rlre.getMessage().startsWith("Received error " + HTTP_CODE + " from server"));
      Throwable rlCause = rlre.getCause();
      Assert.assertTrue(rlCause instanceof RestException, "Expected RestException not " + rlCause.getClass().getName());
      RestException rle = (RestException) rlCause;
      Assert.assertEquals(ERR_MSG, rle.getResponse().getEntity().asString("UTF-8"));
      Assert.assertEquals(HTTP_CODE, rle.getResponse().getStatus());
    }
  }

  private ErrorHandlingBehavior determineErrorHandlingBehavior(GetResponseOption getResponseOption)
  {
    switch (getResponseOption)
    {
      case GET:
      case GET_RESPONSE:
      case GET_RESPONSE_ENTITY:
        return null;
      case GET_RESPONSE_EXPLICIT_NO_THROW:
      case GET_RESPONSE_ENTITY_EXPLICIT_NO_THROW:
        return ErrorHandlingBehavior.TREAT_SERVER_ERROR_AS_SUCCESS;
      case GET_RESPONSE_EXPLICIT_THROW:
      case GET_RESPONSE_ENTITY_EXPLICIT_THROW:
        return ErrorHandlingBehavior.FAIL_ON_ERROR;
      default:
        throw new IllegalStateException();
    }
  }

  private <T extends RecordTemplate> ResponseFuture<T> sendRequest(SendRequestOption option,
                                                                   ErrorHandlingBehavior errorHandlingBehavior,
                                                                   RestClient client,
                                                                   Request<T> request,
                                                                   RequestBuilder<Request<T>> requestBuilder)
  {
    switch (option)
    {
      case REQUEST_NO_CONTEXT:
        if (errorHandlingBehavior == null)
        {
          return client.sendRequest(request);
        }
        else
        {
          return client.sendRequest(request, errorHandlingBehavior);
        }
      case REQUEST_WITH_CONTEXT:
        if (errorHandlingBehavior == null)
        {
          return client.sendRequest(request, DEFAULT_REQUEST_CONTEXT);
        }
        else
        {
          return client.sendRequest(request, DEFAULT_REQUEST_CONTEXT, errorHandlingBehavior);
        }
      case REQUESTBUILDER_NO_CONTEXT:
        if (errorHandlingBehavior == null)
        {
          return client.sendRequest(requestBuilder);
        }
        else
        {
          return client.sendRequest(requestBuilder, errorHandlingBehavior);
        }
      case REQUESTBUILDER_WITH_CONTEXT:
        if (errorHandlingBehavior == null)
        {
          return client.sendRequest(requestBuilder, DEFAULT_REQUEST_CONTEXT);
        }
        else
        {
          return client.sendRequest(requestBuilder, DEFAULT_REQUEST_CONTEXT, errorHandlingBehavior);
        }
      default:
        throw new IllegalStateException();
    }
  }

  private <T extends RecordTemplate> void sendRequest(SendRequestOption option,
                                                      RestClient client,
                                                      Request<T> request,
                                                      RequestBuilder<Request<T>> requestBuilder,
                                                      Callback<Response<T>> callback)
  {
    switch (option)
    {
      case REQUEST_NO_CONTEXT:
        client.sendRequest(request, callback);
        break;
      case REQUEST_WITH_CONTEXT:
        client.sendRequest(request, DEFAULT_REQUEST_CONTEXT, callback);
        break;
      case REQUESTBUILDER_NO_CONTEXT:
        client.sendRequest(requestBuilder, callback);
        break;
      case REQUESTBUILDER_WITH_CONTEXT:
        client.sendRequest(requestBuilder, DEFAULT_REQUEST_CONTEXT, callback);
        break;
      default:
        throw new IllegalStateException();
    }
  }

  private <T extends RecordTemplate> Response<T> getOkResponse(GetResponseOption option,
                                                               ResponseFuture<T> future,
                                                               TimeoutOption timeoutOption)
    throws ExecutionException, InterruptedException, TimeoutException, RemoteInvocationException
  {
    Response<T> result = null;
    T entity;
    Long l = timeoutOption._l;
    TimeUnit timeUnit = timeoutOption._timeUnit;
    switch (option)
    {
      case GET:
        result = l == null ? future.get() : future.get(l, timeUnit);
        break;
      case GET_RESPONSE:
      case GET_RESPONSE_EXPLICIT_NO_THROW:
      case GET_RESPONSE_EXPLICIT_THROW:
        result = l == null ? future.getResponse() : future.getResponse(l, timeUnit);
        break;
      case GET_RESPONSE_ENTITY:
      case GET_RESPONSE_ENTITY_EXPLICIT_NO_THROW:
      case GET_RESPONSE_ENTITY_EXPLICIT_THROW:
        entity = l == null ? future.getResponseEntity() : future.getResponseEntity(l, timeUnit);
        result = future.getResponse();
        Assert.assertSame(entity, result.getEntity());
        break;
      default:
        throw new IllegalStateException();
    }
    return result;
  }

  private <T extends RecordTemplate> RestLiResponseException getErrorResponse(GetResponseOption option,
                                                                              ResponseFuture<T> future,
                                                                              TimeoutOption timeoutOption)
    throws InterruptedException, TimeoutException, RemoteInvocationException
  {
    Response<T> response = null;
    T entity;
    RestLiResponseException result = null;
    Long l = timeoutOption._l;
    TimeUnit timeUnit = timeoutOption._timeUnit;
    switch (option)
    {
      case GET:
        try
        {
          response = l == null ? future.get() : future.get(l, timeUnit);
          Assert.fail("Should have thrown");
        }
        catch (ExecutionException e)
        {
          Throwable cause = e.getCause();
          Assert.assertTrue(cause instanceof RestException, "Expected RestLiResponseException not " + cause.getClass().getName());
          result = (RestLiResponseException) cause;
        }
        break;
      case GET_RESPONSE:
      case GET_RESPONSE_EXPLICIT_THROW:
        try
        {
          response = l == null ? future.getResponse() : future.getResponse(l, timeUnit);
          Assert.fail("Should have thrown");
        }
        catch (RestLiResponseException e)
        {
          result = e;
        }
        break;
      case GET_RESPONSE_EXPLICIT_NO_THROW:
        response = l == null ? future.getResponse() : future.getResponse(l, timeUnit);
        result = response.getError();
        break;
      case GET_RESPONSE_ENTITY:
      case GET_RESPONSE_ENTITY_EXPLICIT_THROW:
        try
        {
          entity = l == null ? future.getResponseEntity() : future.getResponseEntity(l, timeUnit);
          Assert.fail("Should have thrown");
        }
        catch (RestLiResponseException e)
        {
          result = e;
        }
        break;
      case GET_RESPONSE_ENTITY_EXPLICIT_NO_THROW:
        entity = l == null ? future.getResponseEntity() : future.getResponseEntity(l, timeUnit);
        break;
      default:
        throw new IllegalStateException();
    }
    return result;
  }

  private <T extends RecordTemplate> RequestBuilder<Request<T>> mockRequestBuilder(final Request<T> request)
  {
    return new RequestBuilder<Request<T>>()
    {
      @Override
      public Request<T> build()
      {
        return request;
      }
    };
  }

  private <T extends RecordTemplate> Request<T> mockRequest(Class<T> clazz,
      ProtocolVersionOption versionOption, ContentType contentType)
  {
    RestliRequestOptions restliRequestOptions = new RestliRequestOptionsBuilder()
        .setProtocolVersionOption(versionOption)
        .setContentType(contentType)
        .setAcceptTypes(Collections.singletonList(contentType))
        .build();

    return new GetRequest<T>(Collections.<String, String> emptyMap(),
                             Collections.<HttpCookie>emptyList(),
                             clazz,
                             null,
                             new DataMap(),
                             Collections.<String, Class<?>>emptyMap(),
                             new ResourceSpecImpl(),
                             "/foo",
                             Collections.<String, Object>emptyMap(),
                             restliRequestOptions);
  }

  private static class MyMockClient extends MockClient
  {
    private RequestContext _requestContext;

    private MyMockClient(int httpCode, Map<String, String> headers, byte[] bytes)
    {
      super(httpCode, headers, bytes);
    }

    @Override
    public void restRequest(RestRequest request, RequestContext requestContext,
                            Callback<RestResponse> callback)
    {
      Assert.assertNotNull(requestContext);
      _requestContext = requestContext;
      super.restRequest(request, requestContext, callback);
    }

    @Override
    protected Map<String, String> headers()
    {
      Map<String, String> headers = new HashMap<String, String>(super.headers());
      for (Map.Entry<String, Object> attr : _requestContext.getLocalAttrs().entrySet())
      {
        if (!attr.getKey().startsWith("__attr"))
        {
          continue;
        }
        headers.put(attr.getKey(), attr.getValue().toString());
      }
      return headers;
    }
  }

  @SuppressWarnings("deprecation")
  private RestClient mockClient(String errKey,
                                String errValue,
                                String errMsg,
                                int httpCode,
                                int appCode,
                                String code,
                                String docUrl,
                                String requestId,
                                ProtocolVersion protocolVersion,
                                String errorResponseHeaderName)
  {
    ErrorResponse er = new ErrorResponse();

    DataMap errMap = new DataMap();
    errMap.put(errKey, errValue);
    er.setErrorDetails(new ErrorDetails(errMap));
    er.setErrorDetailType(EmptyRecord.class.getCanonicalName());
    er.setStatus(httpCode);
    er.setMessage(errMsg);
    er.setServiceErrorCode(appCode);
    er.setCode(code);
    er.setDocUrl(docUrl);
    er.setRequestId(requestId);

    Map<String,String> headers = new HashMap<String,String>();
    headers.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString());
    headers.put(errorResponseHeaderName, RestConstants.HEADER_VALUE_ERROR);

    byte[] mapBytes;
    try
    {
      mapBytes = DataMapConverter.getContentType(headers).getCodec().mapToBytes(er.data());
    }
    catch (IOException | MimeTypeParseException e)
    {
      throw new RuntimeException(e);
    }

    return new RestClient(new MyMockClient(httpCode, headers, mapBytes), "http://localhost");
  }

  private RestClient mockClient(int httpCode, String errDetails, ProtocolVersion protocolVersion)
  {
    byte[] mapBytes;
    try
    {
      mapBytes = errDetails.getBytes("UTF8");
    }
    catch (UnsupportedEncodingException e)
    {
      throw new RuntimeException(e);
    }

    Map<String,String> headers = new HashMap<String,String>();
    headers.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString());

    return new RestClient(new MyMockClient(httpCode, headers, mapBytes), "http://localhost");
  }
}
