package com.linkedin.restli.server;

import com.linkedin.common.callback.Callback;
import com.linkedin.data.ByteString;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.r2.transport.common.StreamRequestHandler;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import com.linkedin.restli.common.HttpStatus;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings("deprecation")
public class TestDelegatingTransportDispatcher
{
  static final String URI_PATH = "testRequestHandler";
  static final String REQUEST_TYPE_HEADER = "REQUEST_TYPE";
  static final String REST_REQUEST = "REST";
  static final String STREAM_REQUEST = "STREAMING";
  static final String ERROR_NOT_REST = "This server cannot handle non-rest requests";
  static final String ERROR_NOT_STREAM = "This server cannot handle non-stream requests";

  boolean hasError = false;
  String errorMessage = null;

  RestRequest getTestRestRequest() throws Exception
  {
    return new RestRequestBuilder(new URI(URI_PATH)).setHeader(REQUEST_TYPE_HEADER, REST_REQUEST).build();
  }

  StreamRequest getTestStreamRequest() throws Exception
  {
    return new StreamRequestBuilder(new URI(URI_PATH)).setHeader(REQUEST_TYPE_HEADER, STREAM_REQUEST).build(EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copyString("", Charset.defaultCharset()))));
  }

  class RestAndStreamRequestHandler implements RestRequestHandler, StreamRequestHandler
  {
    public RestAndStreamRequestHandler()
    {
    }

    @Override
    public void handleRequest(StreamRequest request, RequestContext requestContext, Callback<StreamResponse> callback)
    {
      if (!request.getHeader(REQUEST_TYPE_HEADER).equals(STREAM_REQUEST))
      {
        callback.onError(Messages.toStreamException(RestException.forError(HttpStatus.S_406_NOT_ACCEPTABLE.getCode(), ERROR_NOT_STREAM)));
      }
      else
      {
        callback.onSuccess(new StreamResponse()
        {
          @Override
          public StreamResponseBuilder builder()
          {
            return null;
          }

          @Override
          public int getStatus()
          {
            return HttpStatus.S_200_OK.getCode();
          }

          @Override
          public EntityStream getEntityStream()
          {
            return null;
          }

          @Override
          public String getHeader(String name)
          {
            return null;
          }

          @Override
          public List<String> getHeaderValues(String name)
          {
            return null;
          }

          @Override
          public List<String> getCookies()
          {
            return null;
          }

          @Override
          public Map<String, String> getHeaders()
          {
            return null;
          }
        });
      }
    }

    @Override
    public void handleRequest(RestRequest request, RequestContext requestContext, Callback<RestResponse> callback)
    {
      if (!request.getHeader(REQUEST_TYPE_HEADER).equals(REST_REQUEST))
      {
        callback.onError(Messages.toStreamException(RestException.forError(HttpStatus.S_406_NOT_ACCEPTABLE.getCode(), ERROR_NOT_REST)));
      }
      else
      {
        callback.onSuccess(new RestResponse()
        {
          @Override
          public RestResponseBuilder builder()
          {
            return null;
          }

          @Override
          public int getStatus()
          {
            return HttpStatus.S_200_OK.getCode();
          }

          @Override
          public ByteString getEntity()
          {
            return null;
          }

          @Override
          public String getHeader(String name)
          {
            return null;
          }

          @Override
          public List<String> getHeaderValues(String name)
          {
            return null;
          }

          @Override
          public List<String> getCookies()
          {
            return null;
          }

          @Override
          public Map<String, String> getHeaders()
          {
            return null;
          }
        });
      }
    }
  }

  private TransportCallback<RestResponse> getRestCallback()
  {
    return new TransportCallback<RestResponse>()
    {
      @Override
      public void onResponse(TransportResponse<RestResponse> response)
      {
        if (response.hasError())
        {
          hasError = true;
          errorMessage = response.getError().getMessage();
        }
      }
    };
  }

  private TransportCallback<StreamResponse> getStreamCallback()
  {
    return new TransportCallback<StreamResponse>()
    {
      @Override
      public void onResponse(TransportResponse<StreamResponse> response)
      {
        if (response.hasError())
        {
          hasError = true;
          errorMessage = response.getError().getMessage();
        }
      }
    };
  }

  @BeforeMethod
  protected void setUp()
  {
    hasError = false;
    errorMessage = null;
  }

  @Test()
  public void testDispatcherWithAdapter() throws Exception
  {
    DelegatingTransportDispatcher dispatcher = new DelegatingTransportDispatcher(new RestAndStreamRequestHandler());

    dispatcher.handleRestRequest(getTestRestRequest(), null, null, getRestCallback());
    Assert.assertFalse(hasError);

    setUp();
    dispatcher.handleStreamRequest(getTestStreamRequest(), null, null, getStreamCallback());
    Assert.assertTrue(hasError);
    Assert.assertEquals(errorMessage, ERROR_NOT_REST);
  }

  @Test()
  public void testDispatcherWithoutAdapter() throws Exception
  {
    RestAndStreamRequestHandler requestHandler = new RestAndStreamRequestHandler();
    DelegatingTransportDispatcher dispatcher = new DelegatingTransportDispatcher(requestHandler, requestHandler);

    dispatcher.handleRestRequest(getTestRestRequest(), null, null, getRestCallback());
    Assert.assertFalse(hasError);

    setUp();
    dispatcher.handleStreamRequest(getTestStreamRequest(), null, null, getStreamCallback());
    Assert.assertFalse(hasError);
  }
}
