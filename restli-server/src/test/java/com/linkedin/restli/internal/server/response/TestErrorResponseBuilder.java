/*
   Copyright (c) 2014 LinkedIn Corp.

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


package com.linkedin.restli.internal.server.response;


import com.linkedin.data.DataMap;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.HeaderUtil;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.ErrorResponseFormat;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;

import com.linkedin.restli.server.TestRecord;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author kparikh
 */
public class TestErrorResponseBuilder
{
  @DataProvider(name = "testData")
  public Object[][] dataProvider()
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion() },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion() }
        };
  }

  @Test(dataProvider = "testData")
  public void testBuilder(ProtocolVersion protocolVersion)
  {
    Map<String, String> headers = ResponseBuilderUtil.getHeaders();
    Map<String, String> expectedHeaders = new HashMap<>(headers);
    expectedHeaders.put(HeaderUtil.getErrorResponseHeaderName(protocolVersion), RestConstants.HEADER_VALUE_ERROR);

    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor();
    RoutingResult routingResult = new RoutingResult(null, mockDescriptor);

    RuntimeException runtimeException = new RuntimeException("Internal server error!");
    RestLiServiceException serviceException = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                                                         runtimeException);

    ErrorResponseBuilder errorResponseBuilder = new ErrorResponseBuilder();
    RestLiResponseData<?> responseData = errorResponseBuilder.buildRestLiResponseData(routingResult,
                                                                                      serviceException,
                                                                                      headers,
                                                                                      Collections.emptyList());
    RestLiResponse restResponse = errorResponseBuilder.buildResponse(responseData);

    EasyMock.verify(mockDescriptor);
    ErrorResponse errorResponse = (ErrorResponse) restResponse.getEntity();
    Assert.assertEquals(errorResponse.getStatus(), Integer.valueOf(500));
    Assert.assertTrue(errorResponse.getMessage().contains(runtimeException.getMessage()));
  }

  private ResourceMethodDescriptor getMockResourceMethodDescriptor()
  {
    ResourceMethodDescriptor mockDescriptor = EasyMock.createMock(ResourceMethodDescriptor.class);
    EasyMock.expect(mockDescriptor.getMethodType()).andReturn(ResourceMethod.GET);
    EasyMock.replay(mockDescriptor);
    return mockDescriptor;
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testExceptionClass()
  {
    ErrorResponseBuilder builder = new ErrorResponseBuilder();
    RestLiServiceException exception = new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, "foobar", new IllegalStateException("foo"));
    exception.setServiceErrorCode(123);
    exception.setOverridingFormat(ErrorResponseFormat.MESSAGE_AND_SERVICECODE_AND_EXCEPTIONCLASS);

    ErrorResponse errorResponse = builder.buildErrorResponse(exception);
    Assert.assertFalse(errorResponse.hasErrorDetails());
    Assert.assertTrue(errorResponse.hasExceptionClass());
    Assert.assertTrue(errorResponse.hasStatus());
    Assert.assertTrue(errorResponse.hasMessage());
    Assert.assertTrue(errorResponse.hasServiceErrorCode());
    Assert.assertFalse(errorResponse.hasStackTrace());
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testOverride()
  {
    RestLiServiceException exception = new RestLiServiceException(HttpStatus.S_200_OK, "Some message", new IllegalStateException("Some other message"));
    exception.setServiceErrorCode(123);
    exception.setCode("INVALID_SOMETHING");
    exception.setDocUrl("www.documentation.com");
    exception.setRequestId("id123");
    exception.setErrorDetails(new DataMap());
    ErrorResponseBuilder builder = new ErrorResponseBuilder(ErrorResponseFormat.FULL);

    ErrorResponse errorResponse = builder.buildErrorResponse(exception);
    Assert.assertTrue(errorResponse.hasErrorDetails());
    Assert.assertTrue(errorResponse.hasExceptionClass());
    Assert.assertTrue(errorResponse.hasStatus());
    Assert.assertTrue(errorResponse.hasMessage());
    Assert.assertTrue(errorResponse.hasCode());
    Assert.assertTrue(errorResponse.hasServiceErrorCode());
    Assert.assertTrue(errorResponse.hasStackTrace());
    Assert.assertTrue(errorResponse.hasDocUrl());
    Assert.assertTrue(errorResponse.hasRequestId());

    exception.setOverridingFormat(ErrorResponseFormat.MESSAGE_AND_SERVICECODE);
    errorResponse = builder.buildErrorResponse(exception);
    Assert.assertFalse(errorResponse.hasErrorDetails());
    Assert.assertFalse(errorResponse.hasExceptionClass());
    Assert.assertTrue(errorResponse.hasStatus());
    Assert.assertTrue(errorResponse.hasMessage());
    Assert.assertTrue(errorResponse.hasCode());
    Assert.assertTrue(errorResponse.hasServiceErrorCode());
    Assert.assertFalse(errorResponse.hasStackTrace());
    Assert.assertFalse(errorResponse.hasDocUrl());
    Assert.assertFalse(errorResponse.hasRequestId());
  }


  @Test
  public void testErrorDetailsFromDataMap()
  {
    RestLiServiceException exception = new RestLiServiceException(HttpStatus.S_200_OK, "Some message", new IllegalStateException("Some other message"));
    exception.setCode("INVALID_SOMETHING");
    exception.setDocUrl("www.documentation.com");
    exception.setRequestId("id123");
    exception.setErrorDetails((DataMap)null);
    Assert.assertFalse(exception.hasErrorDetails());

    ErrorResponseBuilder builder = new ErrorResponseBuilder(ErrorResponseFormat.FULL);

    ErrorResponse errorResponse = builder.buildErrorResponse(exception);
    Assert.assertFalse(errorResponse.hasErrorDetails());
    Assert.assertTrue(errorResponse.hasExceptionClass());
    Assert.assertTrue(errorResponse.hasStatus());
    Assert.assertTrue(errorResponse.hasMessage());
    Assert.assertTrue(errorResponse.hasCode());
    Assert.assertTrue(errorResponse.hasStackTrace());
    Assert.assertTrue(errorResponse.hasDocUrl());
    Assert.assertTrue(errorResponse.hasRequestId());

    exception.setOverridingFormat(ErrorResponseFormat.MESSAGE_AND_SERVICECODE);
    errorResponse = builder.buildErrorResponse(exception);
    Assert.assertFalse(errorResponse.hasErrorDetails());
    Assert.assertFalse(errorResponse.hasExceptionClass());
    Assert.assertTrue(errorResponse.hasStatus());
    Assert.assertTrue(errorResponse.hasMessage());
    Assert.assertTrue(errorResponse.hasCode());
    Assert.assertFalse(errorResponse.hasStackTrace());
    Assert.assertFalse(errorResponse.hasDocUrl());
    Assert.assertFalse(errorResponse.hasRequestId());
  }

  @Test
  public void testNullStatus()
  {
    RestLiServiceException exception = new RestLiServiceException((HttpStatus) null);
    ErrorResponseBuilder builder = new ErrorResponseBuilder(ErrorResponseFormat.FULL);

    ErrorResponse errorResponse = builder.buildErrorResponse(exception);
    Assert.assertFalse(errorResponse.hasStatus());
  }
}