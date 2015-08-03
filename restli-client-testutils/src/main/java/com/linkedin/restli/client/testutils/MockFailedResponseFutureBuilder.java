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

package com.linkedin.restli.client.testutils;


import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.restli.client.ErrorHandlingBehavior;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.client.ResponseFutureImpl;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.CookieUtil;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


/**
 * Builder class for constructing a {@link ResponseFuture} that represents a failed request.
 *
 * We can use this builder to create {@link ResponseFuture}s representing one of two kinds of failed requests:
 *
 * <ol>
 *  <li>No information was returned by the server. A {@link ResponseFuture} representing this case can be built using
 *      the {@link #setErrorResponse(com.linkedin.restli.common.ErrorResponse)} method.</li>
 *  <li>The server returned partial results. A {@link ResponseFuture} representing this case can be built using the
 *      {@link #setEntity(com.linkedin.data.template.RecordTemplate)} method.</li>
 * </ol>
 *
 * <br></br>
 *
 * In the documentation below the phrase "{@link ResponseFuture} accessor methods" refers to
 * {@link com.linkedin.restli.client.ResponseFuture#getResponse()},
 *      {@link ResponseFuture#getResponse(long, java.util.concurrent.TimeUnit)},
 *      {@link com.linkedin.restli.client.ResponseFuture#getResponseEntity()}, or
 *      {@link ResponseFuture#getResponseEntity(long, java.util.concurrent.TimeUnit)}.<br></br><br></br>
 *
 *
 * If we are trying to represent the case where no information was returned from the server (i.e case (1) above):
 *
 * <ul>
 *  <li>if {@link ErrorHandlingBehavior#FAIL_ON_ERROR} is being used then a {@link RestLiResponseException} is thrown
 *      when any {@link ResponseFuture} accessor method is called. The {@link ErrorResponse}
 *      used to construct the {@link ResponseFuture} is used to populate this {@link RestLiResponseException}.</li>
 *
 *  <li>if {@link ErrorHandlingBehavior#TREAT_SERVER_ERROR_AS_SUCCESS} is being used then no
 *      exception is thrown when one of the {@link ResponseFuture} accessor methods is called. Instead, a
 *      {@link Response} is returned which returns a {@link RestLiResponseException} constructed from
 *      the {@link ErrorResponse} when {@link com.linkedin.restli.client.Response#getError()} is called. The returned
 *      {@link Response} has a {@code null} entity.</li>
 * </ul>

 * <br></br>
 *
 * If we are trying to represent the case where the server returns partial results (i.e. case (2) above):
 *
 * <ul>
 *   <li>if {@link ErrorHandlingBehavior#FAIL_ON_ERROR} is being used then a {@link RestLiResponseException} is thrown
 *       when any {@link ResponseFuture} accessor method is called. Calling
 *       {@link com.linkedin.restli.client.RestLiResponseException#getDecodedResponse()} on this exception will return
 *       a {@link Response} that has its entity as the object that was used in
 *       {@link #setEntity(com.linkedin.data.template.RecordTemplate)}. The other fields on this {@link Response}
 *       correspond to setters invoked in this class. E.g. calling {@link com.linkedin.restli.client.Response#getStatus()}
 *       will return the HTTP status code that was set in {@link #setStatus(int)}</li>
 *
 *   <li>if {@link ErrorHandlingBehavior#TREAT_SERVER_ERROR_AS_SUCCESS} is being used then no exception is thrown
 *       and a {@link Response} object is returned that has its that entity as the object that was used in
 *       {@link #setEntity(com.linkedin.data.template.RecordTemplate)}. The other fields on this {@link Response}
 *       correspond to setters invoked in this class. E.g. calling {@link com.linkedin.restli.client.Response#getStatus()}
 *       will return the HTTP status code that was set in {@link #setStatus(int)}</li>
 * </ul>
 *
 * <br></br>
 *
 * If neither {@link #setEntity(com.linkedin.data.template.RecordTemplate)} or
 * {@link #setErrorResponse(com.linkedin.restli.common.ErrorResponse)} is called, then we assume case (1) is desired.
 * An {@link ErrorResponse} will be constructed in this case using the status set via {@link #setStatus(int)} or
 * {@link #DEFAULT_HTTP_STATUS} if that method was not called.
 *
 * @author kparikh
 *
 * @param <K> key type of the mocked response
 * @param <V> entity type of the mocked response
 */
public class MockFailedResponseFutureBuilder<K, V extends RecordTemplate> extends MockAbstractResponseFutureBuilder<K, V>
{
  private ErrorResponse _errorResponse;
  private ErrorHandlingBehavior _errorHandlingBehavior;

  private static final int DEFAULT_HTTP_STATUS = 500;
  private static final JacksonDataCodec CODEC = new JacksonDataCodec();

  @Override
  protected int getStatus()
  {
    int status = super.getStatus();
    return (status == 0) ? DEFAULT_HTTP_STATUS : status;
  }

  /**
   * Sets the entity. Please see the Javadoc for {@link MockFailedResponseFutureBuilder} to
   * see how this entity is used. In short, this is the entity of the partial response.
   *
   * An {@link IllegalStateException} is thrown if this method is called after
   * {@link #setErrorResponse(com.linkedin.restli.common.ErrorResponse)} is called.
   *
   * @param entity the entity
   * @return
   */
  @Override
  public MockFailedResponseFutureBuilder<K, V> setEntity(V entity)
  {
    if (_errorResponse != null)
    {
      throw new IllegalStateException("Cannot set both errorResponse and entity!");
    }
    super.setEntity(entity);
    return this;
  }

  /**
   * Sets the HTTP status code for the {@link Response} in the case of a partial result.
   * An {@link IllegalArgumentException} is thrown if the status lies in the range [200, 300)
   *
   * @param status the HTTP status
   * @return
   */
  @Override
  public MockFailedResponseFutureBuilder<K, V> setStatus(int status)
  {
    if (status >= 200 && status < 300)
    {
      throw new IllegalArgumentException("Status must be a non 2xx HTTP status code!");
    }
    super.setStatus(status);
    return this;
  }

  /**
   * Set the headers.
   *
   * <ul>
   *  <li>In the case of a partial result, these headers are set on the {@link Response}.</li>
   *  <li>In the case of complete failure, these headers are set on the {@link RestResponse} which can be accessed via
   *      {@link com.linkedin.restli.client.RestLiResponseException#getResponse()} and then calling
   *      {@link com.linkedin.r2.message.rest.RestResponse#getHeaders()}</li>
   * </ul>
   *
   * @param headers the headers to set
   * @return
   */
  @Override
  public MockFailedResponseFutureBuilder<K, V> setHeaders(Map<String, String> headers)
  {
    super.setHeaders(headers);
    return this;
  }

  @Override
  public MockFailedResponseFutureBuilder<K, V> setCookies(List<HttpCookie> cookies)
  {
    super.setCookies(cookies);
    return this;
  }

  /**
   * Sets the {@link ProtocolVersion}
   *
   * @param protocolVersion the {@link ProtocolVersion} we want to set
   * @return
   */
  @Override
  public MockFailedResponseFutureBuilder<K, V> setProtocolVersion(ProtocolVersion protocolVersion)
  {
    super.setProtocolVersion(protocolVersion);
    return this;
  }

  /**
   * Sets the {@link ErrorResponse}.
   *
   * Please see the Javadoc for {@link MockFailedResponseFutureBuilder} to
   * see how this {@link ErrorResponse} is used. In short, this is used to create a {@link RestLiResponseException}.
   *
   * If {@code errorResponse} does not have a status code then {@link #DEFAULT_HTTP_STATUS} will be used.
   *
   * @return
   */
  public MockFailedResponseFutureBuilder<K, V> setErrorResponse(ErrorResponse errorResponse)
  {
    if (getEntity() != null)
    {
      throw new IllegalStateException("Cannot set both errorResponse and entity!");
    }
    _errorResponse =  errorResponse;
    return this;
  }

  /**
   * Set how server errors are treated. Please see {@link ErrorHandlingBehavior} for more details.
   *
   * @param errorHandlingBehavior the {@link ErrorHandlingBehavior} we want to set.
   * @return
   */
  public MockFailedResponseFutureBuilder<K, V> setErrorHandlingBehavior(ErrorHandlingBehavior errorHandlingBehavior)
  {
    _errorHandlingBehavior = errorHandlingBehavior;
    return this;
  }

  /**
   * Builds the {@link ResponseFuture}
   * @return
   */
  @Override
  public ResponseFuture<V> build()
  {
    if (_errorResponse == null && getEntity() == null)
    {
      // Create an ErrorResponse from the status, or use the DEFAULT_HTTP_STATUS to build one
      _errorResponse = new ErrorResponse();
      _errorResponse.setStatus(getStatus());
    }

    _errorHandlingBehavior = (_errorHandlingBehavior == null) ?
        ErrorHandlingBehavior.FAIL_ON_ERROR : _errorHandlingBehavior;

    ProtocolVersion protocolVersion = (getProtocolVersion() == null) ? AllProtocolVersions.BASELINE_PROTOCOL_VERSION :
        getProtocolVersion();

    if (_errorResponse != null)
    {
      return buildWithErrorResponse(protocolVersion);
    }

    // _entity has been set
    return buildWithEntity();
  }

  private ResponseFuture<V> buildWithErrorResponse(ProtocolVersion protocolVersion)
  {
    int status = (_errorResponse.hasStatus()) ? _errorResponse.getStatus() : DEFAULT_HTTP_STATUS;
    byte[] entity = mapToBytes(_errorResponse.data());

    // The header indicating that this RestResponse is an ErrorResponse depends on the version of the Rest.li
    // protocol being used.
    String errorHeaderName;
    if (protocolVersion.equals(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion()))
    {
      errorHeaderName = RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE;
    }
    else
    {
      errorHeaderName = RestConstants.HEADER_RESTLI_ERROR_RESPONSE;
    }

    Map<String, String> headers = new HashMap<String, String>();
    if (getHeaders() != null)
    {
      headers.putAll(getHeaders());
    }
    headers.put(errorHeaderName, "true");
    headers.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString());
    List<HttpCookie> cookies = getCookies() == null ? Collections.<HttpCookie>emptyList() : getCookies();

    RestResponse restResponse = new RestResponseBuilder()
        .setEntity(entity)
        .setStatus(status)
        .setHeaders(Collections.unmodifiableMap(headers))
        .setCookies(Collections.unmodifiableList(CookieUtil.encodeCookies(cookies)))
        .build();

    // create a RestLiResponseException and wrap it in an ExecutionException that will be thrown by the ResponseFuture
    RestLiResponseException restLiResponseException = new RestLiResponseException(restResponse, null, _errorResponse);
    ExecutionException executionException = new ExecutionException(restLiResponseException);

    Future<Response<V>> responseFuture = buildFuture(null, executionException);
    return new ResponseFutureImpl<V>(responseFuture, _errorHandlingBehavior);
  }

  private ResponseFuture<V> buildWithEntity()
  {
    int status = getStatus();
    byte[] entity = mapToBytes(getEntity().data());

    Response<V> decodedResponse = new MockResponseBuilder<K, V>()
        .setEntity(getEntity())
        .setStatus(status)
        .setHeaders(getHeaders())
        .setCookies(getCookies())
        .setProtocolVersion(getProtocolVersion())
        .build();

    RestResponse restResponse = new RestResponseBuilder()
        .setEntity(entity)
        .setStatus(status)
        .setHeaders(decodedResponse.getHeaders())
        .setCookies(CookieUtil.encodeCookies(decodedResponse.getCookies()))
        .build();

    RestLiResponseException restLiResponseException = new RestLiResponseException(restResponse,
                                                                                  decodedResponse,
                                                                                  new ErrorResponse());
    ExecutionException executionException = new ExecutionException(restLiResponseException);

    Future<Response<V>> responseFuture = buildFuture(null, executionException);
    return new ResponseFutureImpl<V>(responseFuture, _errorHandlingBehavior);
  }

  private static byte[] mapToBytes(DataMap dataMap)
  {
    try
    {
      return CODEC.mapToBytes(dataMap);
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }
}
