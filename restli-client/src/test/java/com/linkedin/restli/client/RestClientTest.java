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
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.common.util.None;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.client.EntityResponseDecoder;
import junit.framework.Assert;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class RestClientTest
{
  @Test
  public void testEmptyErrorResponse()
  {
    RestResponse response = new RestResponseBuilder().setStatus(200).build();
    RestLiResponseException e = new RestLiResponseException(response, new ErrorResponse());

    Assert.assertNull(e.getServiceErrorMessage());
    Assert.assertNull(e.getErrorDetails());
    Assert.assertNull(e.getErrorSource());
    Assert.assertFalse(e.hasServiceErrorCode());
    Assert.assertNull(e.getServiceErrorStackTrace());
    Assert.assertNull(e.getServiceExceptionClass());
  }

  @Test
  public void testShutdown()
  {
    Client client = EasyMock.createMock(Client.class);

    @SuppressWarnings("unchecked")
    Callback<None> callback = EasyMock.createMock(Callback.class);
    Capture<Callback<None>> callbackCapture = new Capture<Callback<None>>();

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

  @Test
  public void testRestLiResponseExceptionFuture() throws RemoteInvocationException, TimeoutException, IOException
  {
    final String ERR_KEY = "someErr";
    final String ERR_VALUE = "WHOOPS!";
    final String ERR_MSG = "whoops2";
    final int HTTP_CODE = 400;
    final int APP_CODE = 666;

    RestClient client = mockClient(ERR_KEY, ERR_VALUE, ERR_MSG, HTTP_CODE, APP_CODE);
    Request<EmptyRecord> req = mockRequest(EmptyRecord.class);

    try
    {
      client.sendRequest(req).getResponse(30, TimeUnit.SECONDS);
      Assert.fail("Should have thrown");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(HTTP_CODE, e.getStatus());
      Assert.assertEquals(ERR_VALUE, e.getErrorDetails().get(ERR_KEY));
      Assert.assertEquals(APP_CODE, e.getServiceErrorCode());
      Assert.assertEquals(ERR_MSG, e.getServiceErrorMessage());
    }
  }

  @Test
  public void testRestLiResponseExceptionFutureStandard()
          throws TimeoutException, IOException, ExecutionException, InterruptedException
  {
    final String ERR_KEY = "someErr";
    final String ERR_VALUE = "WHOOPS!";
    final String ERR_MSG = "whoops2";
    final int HTTP_CODE = 400;
    final int APP_CODE = 666;

    RestClient client = mockClient(ERR_KEY, ERR_VALUE, ERR_MSG, HTTP_CODE, APP_CODE);
    Request<EmptyRecord> req = mockRequest(EmptyRecord.class);

    try
    {
      client.sendRequest(req).get(30, TimeUnit.SECONDS);
      Assert.fail("Should have thrown");
    }
    catch (ExecutionException e)
    {
      Throwable cause = e.getCause();
      Assert.assertEquals(RestLiResponseException.class, cause.getClass());
      RestLiResponseException rlre = (RestLiResponseException)cause;
      Assert.assertEquals(HTTP_CODE, rlre.getStatus());
      Assert.assertEquals(ERR_VALUE, rlre.getErrorDetails().get(ERR_KEY));
      Assert.assertEquals(APP_CODE, rlre.getServiceErrorCode());
      Assert.assertEquals(ERR_MSG, rlre.getServiceErrorMessage());
    }
  }

  @Test
  public void testRestLiResponseExceptionCallback()
          throws ExecutionException, TimeoutException, InterruptedException
  {
    final String ERR_KEY = "someErr";
    final String ERR_VALUE = "WHOOPS!";
    final String ERR_MSG = "whoops2";
    final int HTTP_CODE = 400;
    final int APP_CODE = 666;

    RestClient client = mockClient(ERR_KEY, ERR_VALUE, ERR_MSG, HTTP_CODE, APP_CODE);
    Request<EmptyRecord> req = mockRequest(EmptyRecord.class);

    FutureCallback<Response<EmptyRecord>> callback = new FutureCallback<Response<EmptyRecord>>();
    try
    {
      client.sendRequest(req, callback);
      callback.get(30, TimeUnit.SECONDS);
      Assert.fail("Should have thrown");
    }
    catch (ExecutionException e)
    {
      Throwable cause = e.getCause();
      Assert.assertTrue("Expected RestLiResponseException not " + cause.getClass().getName(), cause instanceof RestLiResponseException);
      RestLiResponseException rlre = (RestLiResponseException)cause;
      Assert.assertEquals(HTTP_CODE, rlre.getStatus());
      Assert.assertEquals(ERR_VALUE, rlre.getErrorDetails().get(ERR_KEY));
      Assert.assertEquals(APP_CODE, rlre.getServiceErrorCode());
      Assert.assertEquals(ERR_MSG, rlre.getServiceErrorMessage());

    }
  }

  @Test
  public void testRestLiResponseExceptionCallbackOld()
          throws ExecutionException, TimeoutException, InterruptedException, RestLiDecodingException
  {
    final String ERR_KEY = "someErr";
    final String ERR_VALUE = "WHOOPS!";
    final String ERR_MSG = "whoops2";
    final int HTTP_CODE = 400;
    final int APP_CODE = 666;

    RestClient client = mockClient(ERR_KEY, ERR_VALUE, ERR_MSG, HTTP_CODE, APP_CODE);
    Request<EmptyRecord> req = mockRequest(EmptyRecord.class);

    FutureCallback<Response<EmptyRecord>> callback = new FutureCallback<Response<EmptyRecord>>();
    try
    {
      client.sendRequest(req, callback);
      callback.get(30, TimeUnit.SECONDS);
      Assert.fail("Should have thrown");
    }
    catch (ExecutionException e)
    {
      Throwable cause = e.getCause();
      Assert.assertTrue("Expected RestException not " + cause.getClass().getName(), cause instanceof RestException);
      RestException re = (RestException)cause;
      RestResponse r = re.getResponse();

      ErrorResponse er = new EntityResponseDecoder<ErrorResponse>(ErrorResponse.class).decodeResponse(r).getEntity();

      Assert.assertEquals(HTTP_CODE, r.getStatus());
      Assert.assertEquals(ERR_VALUE, er.getErrorDetails().get(ERR_KEY));
      Assert.assertEquals(APP_CODE, er.getServiceErrorCode());
      Assert.assertEquals(ERR_MSG, er.getMessage());

    }
  }

  private <T extends RecordTemplate> Request<T> mockRequest(Class<T> clazz)
  {
    return new GetRequest<T>(URI.create(""), Collections.<String,String>emptyMap(), clazz, URI.create(""), "foo", Collections.<PathSpec>emptySet(), null);
  }

  private RestClient mockClient(String ERR_KEY, String ERR_VALUE, String ERR_MSG, int HTTP_CODE,
                                int APP_CODE)
  {
    ErrorResponse er = new ErrorResponse();

    DataMap errMap = new DataMap();
    errMap.put(ERR_KEY, ERR_VALUE);
    er.setErrorDetails(errMap);
    er.setStatus(HTTP_CODE);
    er.setMessage(ERR_MSG);
    er.setServiceErrorCode(APP_CODE);

    byte[] mapBytes;
    try
    {
      mapBytes = new JacksonDataCodec().mapToBytes(er.data());
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }

    Map<String,String> headers = new HashMap<String,String>();
    headers.put(RestConstants.HEADER_LINKEDIN_TYPE, er.getClass().getName());
    headers.put(RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE, RestConstants.HEADER_VALUE_ERROR_APPLICATION);

    return new RestClient(new MockClient(HTTP_CODE, headers, mapBytes), "http://localhost");
  }

}
