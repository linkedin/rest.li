package com.linkedin.restli.client.response;

import com.linkedin.data.ByteString;
import com.linkedin.data.DataMap;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.common.ErrorResponse;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class TestRestliResponseException {

    RestResponse mockRestResponse;
    ErrorResponse mockErrorResponse;
    Response mockDecodeResponse;

    @BeforeMethod
    void setup() {
        mockRestResponse = Mockito.mock(RestResponse.class);
    }

    @Test
    public void testNullErrorAndDecodeResponses() {
        Mockito.when(mockRestResponse.getEntity()).thenReturn(ByteString.copy("test".getBytes()));
        RestLiResponseException responseException = new RestLiResponseException(mockRestResponse, null, null);

        assertNull(responseException.getErrorResponse());
        assertNullErrorResponse(responseException);
        assertNullDecodedResponse(responseException);
    }

    @Test
    public void testErrorResponse() {
        Mockito.when(mockRestResponse.getEntity()).thenReturn(ByteString.copy("test".getBytes()));
        ErrorResponse errorResponse = new ErrorResponse();

        RestLiResponseException responseException = new RestLiResponseException(mockRestResponse, null, errorResponse);

        assertNullErrorResponse(responseException);
    }

    @Test
    public void testValidValues() {
        mockDecodeResponse = Mockito.mock(Response.class);
        mockErrorResponse = Mockito.mock(ErrorResponse.class);
        Mockito.when(mockRestResponse.getEntity()).thenReturn(ByteString.copy("test".getBytes()));
        Mockito.when(mockRestResponse.getStatus()).thenReturn(456);
        DataMap map = new DataMap();
        map.put("serviceErrorCode", 123);
        map.put("errorDetails", new DataMap());
        map.put("exceptionClass", "thisException");
        map.put("stackTrace", "thisStackTrace");
        map.put("message", "this message");
        ErrorResponse errorResponse = new ErrorResponse(map);


        RestLiResponseException responseException = new RestLiResponseException(mockRestResponse, mockDecodeResponse, errorResponse);

        assertEquals(responseException.hasErrorDetails(), true);
        assertEquals(responseException.hasServiceErrorCode(), true);
        assertEquals(responseException.hasServiceExceptionClass(), true);
        assertEquals(responseException.hasServiceErrorMessage(), true);
        assertEquals(responseException.hasServiceErrorStackTrace(), true);

        assertNotNull(responseException.getErrorDetails());
        assertEquals(responseException.getServiceErrorCode(), 123);
        assertEquals(responseException.getServiceExceptionClass(), "thisException");
        assertEquals(responseException.getServiceErrorMessage(), "this message");
        assertEquals(responseException.getServiceErrorStackTrace(), "thisStackTrace");
        assertEquals(responseException.getStatus(), 456);
    }

    private void assertNullErrorResponse(RestLiResponseException responseException) {
        assertNotNull(responseException);

        assertNull(responseException.getErrorSource());
        assertNull(responseException.getErrorDetails());
        assertEquals(responseException.getServiceErrorCode(), 0);
        assertNull(responseException.getServiceExceptionClass());
        assertNull(responseException.getServiceErrorMessage());
        assertNull(responseException.getServiceErrorStackTrace());
        assertEquals(responseException.getStatus(), 0);

        assertEquals(responseException.hasErrorDetails(), false);
        assertEquals(responseException.hasServiceErrorCode(), false);
        assertEquals(responseException.hasServiceExceptionClass(), false);
        assertEquals(responseException.hasServiceErrorMessage(), false);
        assertEquals(responseException.hasServiceErrorStackTrace(), false);
    }

    private void assertNullDecodedResponse(RestLiResponseException responseException) {
        assertNull(responseException.getDecodedResponse());
        assertEquals(responseException.hasDecodedResponse(), false);
    }
}
