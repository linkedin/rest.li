package com.linkedin.r2.message;

import com.linkedin.common.callback.Callback;
import com.linkedin.data.ByteString;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.stream.StreamException;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.FullEntityReader;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.r2.transport.http.common.HttpConstants;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


/**
 * A helper class that holds static convenience methods for conversion between rest messages and stream messages
 *
 * @author Zhenkai Zhu
 */
public class Messages
{
  private Messages() {}

  /**
   * Converts a StreamRequest to RestRequest
   * @param streamRequest the stream request to be converted
   * @param callback the callback to be invoked when the rest request is constructed
   */
  public static void toRestRequest(StreamRequest streamRequest, final Callback<RestRequest> callback)
  {
    final RestRequestBuilder builder = new RestRequestBuilder(streamRequest);
    Callback<ByteString> assemblyCallback = new Callback<ByteString>()
    {
      @Override
      public void onError(Throwable e)
      {
        callback.onError(e);
      }

      @Override
      public void onSuccess(ByteString result)
      {
        RestRequest restRequest = builder.setEntity(result).build();
        callback.onSuccess(restRequest);
      }
    };
    streamRequest.getEntityStream().setReader(new FullEntityReader(assemblyCallback));
  }

  public static CompletionStage<RestRequest> toRestRequest(StreamRequest streamRequest)
  {
    CompletableFuture<RestRequest> completable = new CompletableFuture<>();
    final RestRequestBuilder builder = new RestRequestBuilder(streamRequest);
    streamRequest.getEntityStream().setReader(new FullEntityReader(new Callback<ByteString>()
    {
      @Override
      public void onError(Throwable e)
      {
        completable.completeExceptionally(e);
      }

      @Override
      public void onSuccess(ByteString result)
      {
        RestRequest restRequest = builder.setEntity(result).build();
        completable.complete(restRequest);
      }
    }));

    return completable;
  }

  /**
   * Converts a StreamResponse to RestResponse
   * @param streamResponse the stream request to be converted
   * @param callback the callback to be invoked when the rest response is constructed
   */
  public static void toRestResponse(StreamResponse streamResponse, final Callback<RestResponse> callback)
  {
    toRestResponse(streamResponse, callback, false);
  }

  /**
   * Converts a StreamResponse to RestResponse
   * @param streamResponse the stream request to be converted
   * @param callback the callback to be invoked when the rest response is constructed
   * @param addContentLengthHeader whether the rest response should have content-length header
   */
  public static void toRestResponse(StreamResponse streamResponse, final Callback<RestResponse> callback, final boolean addContentLengthHeader)
  {
    final RestResponseBuilder builder = new RestResponseBuilder(streamResponse);
    Callback<ByteString> assemblyCallback = new Callback<ByteString>()
    {
      @Override
      public void onError(Throwable e)
      {
        callback.onError(e);
      }

      @Override
      public void onSuccess(ByteString result)
      {
        if (addContentLengthHeader)
        {
          builder.setHeader(HttpConstants.CONTENT_LENGTH, String.valueOf(result.length()));
        }
        RestResponse restResponse = builder.setEntity(result).build();
        callback.onSuccess(restResponse);
      }
    };
    streamResponse.getEntityStream().setReader(new FullEntityReader(assemblyCallback));
  }

  /**
   * Create a StreamRequest based on the RestRequest
   * @param restRequest the rest request
   * @return the StreamRequest that's created based on rest request
   */
  public static StreamRequest toStreamRequest(RestRequest restRequest)
  {
    StreamRequestBuilder builder = new StreamRequestBuilder(restRequest);
    return builder.build(EntityStreams.newEntityStream(new ByteStringWriter(restRequest.getEntity())));
  }

  /**
   * Create a StreamResponse based on the RestResponse
   * @param restResponse the rest response
   * @return the StreamResponse that's created based on rest response
   */
  public static StreamResponse toStreamResponse(RestResponse restResponse)
  {
    StreamResponseBuilder builder = new StreamResponseBuilder(restResponse);
    return builder.build(EntityStreams.newEntityStream(new ByteStringWriter(restResponse.getEntity())));
  }

  /**
   * Converts a StreamException to RestException
   * @param streamException the stream exception to be converted
   * @param callback the callback to be invoked when the rest exception is constructed
   */
  public static void toRestException(final StreamException streamException, final Callback<RestException> callback)
  {
    toRestException(streamException, callback, false);
  }

  /**
   * Converts a StreamException to RestException
   * @param streamException the stream exception to be converted
   * @param callback the callback to be invoked when the rest exception is constructed
   * @param addContentLengthHeader whether to add content length header for the rest response
   */
  public static void toRestException(final StreamException streamException, final Callback<RestException> callback, final boolean addContentLengthHeader)
  {
    toRestResponse(streamException.getResponse(), new Callback<RestResponse>()
    {
      @Override
      public void onError(Throwable e)
      {
        callback.onError(e);
      }

      @Override
      public void onSuccess(RestResponse result)
      {
        callback.onSuccess(new RestException(result, streamException.getMessage(), streamException.getCause()));
      }
    }, addContentLengthHeader);
  }

  /**
   * Create a StreamException based on the RestException
   * @param restException the rest Exception
   * @return the StreamException that's created based on rest exception
   */
  public static StreamException toStreamException(final RestException restException)
  {
    return new StreamException(toStreamResponse(restException.getResponse()), restException.getMessage(), restException.getCause());
  }

  /**
   * Creates a Callback of StreamResponse based on a Callback of RestResponse.
   * If the throwable in the error case is StreamException, it will be converted to RestException.
   *
   * @param callback the callback of rest response
   * @return callback of stream response
   */
  public static Callback<StreamResponse> toStreamCallback(final Callback<RestResponse> callback)
  {
    return toStreamCallback(callback, false);
  }

  /**
   * Creates a Callback of StreamResponse based on a Callback of RestResponse.
   * If the throwable in the error case is StreamException, it will be converted to RestException.
   *
   * @param callback the callback of rest response
   * @param addContentLengthHeader whether to add content-length header for the rest response
   * @return callback of stream response
   */
  public static Callback<StreamResponse> toStreamCallback(final Callback<RestResponse> callback, final boolean addContentLengthHeader)
  {
    return new Callback<StreamResponse>()
    {
      @Override
      public void onError(Throwable originalException)
      {
        if (originalException instanceof StreamException)
        {
          toRestException((StreamException)originalException, new Callback<RestException>()
          {
            @Override
            public void onError(Throwable e)
            {
              callback.onError(e);
            }

            @Override
            public void onSuccess(RestException restException)
            {
              callback.onError(restException);
            }
          }, addContentLengthHeader);
        }
        else
        {
          callback.onError(originalException);
        }
      }

      @Override
      public void onSuccess(StreamResponse streamResponse)
      {
        toRestResponse(streamResponse, new Callback<RestResponse>()
        {
          @Override
          public void onError(Throwable e)
          {
            callback.onError(e);
          }

          @Override
          public void onSuccess(RestResponse restResponse)
          {
            callback.onSuccess(restResponse);
          }
        }, addContentLengthHeader);
      }
    };
  }

  /**
   * Creates a Callback of RestResponse based on a Callback of StreamResponse.
   * If the throwable in the error case is RestException, it will be converted to StreamException.
   *
   * @param callback the callback of stream response
   * @return callback of rest response
   */
  public static Callback<RestResponse> toRestCallback(final Callback<StreamResponse> callback)
  {
    return new Callback<RestResponse>()
    {
      @Override
      public void onError(Throwable e)
      {
        if (e instanceof RestException)
        {
          callback.onError(toStreamException((RestException) e));
        }
        else
        {
          callback.onError(e);
        }
      }

      @Override
      public void onSuccess(RestResponse result)
      {
        callback.onSuccess(toStreamResponse(result));
      }
    };
  }

  /**
   * Creates a {@link TransportCallback} of {@link StreamResponse} based on a TransportCallback of {@link RestResponse}
   *
   * @param callback the callback of rest response
   * @return callback of stream response
   */
  public static TransportCallback<StreamResponse> toStreamTransportCallback(final TransportCallback<RestResponse> callback)
  {
    return response -> {
      if (response.hasError())
      {
        Throwable throwable = response.getError();
        if (throwable instanceof StreamException)
        {
          toRestException((StreamException)throwable, new Callback<RestException>()
          {
            @Override
            public void onError(Throwable e)
            {
              callback.onResponse(TransportResponseImpl.error(e, response.getWireAttributes()));
            }

            @Override
            public void onSuccess(RestException restException)
            {
              callback.onResponse(TransportResponseImpl.error(restException, response.getWireAttributes()));
            }
          });
        }
        else
        {
          callback.onResponse(TransportResponseImpl.error(throwable, response.getWireAttributes()));
        }
      }
      else
      {
        toRestResponse(response.getResponse(), new Callback<RestResponse>()
        {
          @Override
          public void onError(Throwable e)
          {
            callback.onResponse(TransportResponseImpl.error(e, response.getWireAttributes()));
          }

          @Override
          public void onSuccess(RestResponse result)
          {
            callback.onResponse(TransportResponseImpl.success(result, response.getWireAttributes()));
          }
        });
      }
    };
  }

  /**
   * Creates a {@link TransportCallback} of {@link RestResponse} based on a TransportCallback of {@link StreamResponse}
   *
   * @param callback the callback of stream response
   * @return callback of rest response
   */
  public static TransportCallback<RestResponse> toRestTransportCallback(final TransportCallback<StreamResponse> callback)
  {
    return response -> {
      if (response.hasError())
      {
        Throwable throwable = response.getError();
        if (throwable instanceof RestException)
        {
          callback.onResponse(TransportResponseImpl.error(
              toStreamException((RestException)throwable), response.getWireAttributes()));
        }
        else
        {
          callback.onResponse(TransportResponseImpl.error(throwable, response.getWireAttributes()));
        }
      }
      else
      {
        callback.onResponse(TransportResponseImpl.success(
            toStreamResponse(response.getResponse()), response.getWireAttributes()));
      }
    };
  }
}
