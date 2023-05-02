package com.linkedin.r2.transport.common;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.data.ByteString;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;


public class TestAbstractClient {
  public static final String URI = "http://localhost:8080/";
  public static final String RESPONSE_DATA = "This is not empty";
  private static final String CONTENT_LENGTH = "Content-Length";
  private static final String GET_HTTP_METHOD = "GET";
  private static final String HEAD_HTTP_METHOD = "HEAD";

  @Test
  public void testHeaderIsNotOverriddenForHEADRequests() throws Exception {
    ConcreteClient concreteClient = new ConcreteClient();

    // Assert that proper content-length is set with non HEADER requests
    RestRequest restRequest = new RestRequestBuilder(new URI(URI)).setMethod(GET_HTTP_METHOD).build();
    FutureCallback<RestResponse> restResponseCallback = new FutureCallback<>();
    concreteClient.restRequest(restRequest, new RequestContext(), restResponseCallback);
    RestResponse response = restResponseCallback.get(10, TimeUnit.SECONDS);
    Assert.assertNotNull(response);
    Assert.assertTrue(response.getHeaders().containsKey(CONTENT_LENGTH));
    Assert.assertEquals(Integer.parseInt(response.getHeader(CONTENT_LENGTH)), RESPONSE_DATA.length());

    // Assert that existing content-length is not overridden for HEADER requests
    restRequest = new RestRequestBuilder(new URI(URI)).setMethod(HEAD_HTTP_METHOD).build();
    restResponseCallback = new FutureCallback<>();
    concreteClient.restRequest(restRequest, new RequestContext(), restResponseCallback);
    response = restResponseCallback.get(10, TimeUnit.SECONDS);
    Assert.assertNotNull(response);
    Assert.assertFalse(response.getHeaders().containsKey(CONTENT_LENGTH));
  }

  static class ConcreteClient extends AbstractClient {
    @Override
    public void shutdown(Callback<None> callback) {

    }

    @Override
    public void streamRequest(StreamRequest request, RequestContext requestContext, Callback<StreamResponse> callback) {
      StreamResponse response = new StreamResponseBuilder().build(
          EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copy(RESPONSE_DATA.getBytes()))));
      callback.onSuccess(response);
    }
  }
}