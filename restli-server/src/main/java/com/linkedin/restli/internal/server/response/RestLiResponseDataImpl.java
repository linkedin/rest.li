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

package com.linkedin.restli.internal.server.response;


import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.common.HeaderUtil;
import com.linkedin.restli.internal.server.ResponseType;

import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.RoutingException;
import java.net.HttpCookie;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * Concrete implementation of {@link RestLiResponseData}.
 *
 * @author gye
 */
public class RestLiResponseDataImpl implements RestLiResponseData
{
  /* Overview of variable invariant:
   *
   * _status is reserved for the status of a response without a thrown exception.
   * _exception contains its own status that should be used whenever an exception is thrown.
   *
   * Because we only need one, only one of {status/exception} may be nonnull.
   *
   * Should maintain the invariant that there are generally two sets of variables, one for exception response and
   * another for regular response. If one group is set, another must be set to null.
   */
  private HttpStatus _status;
  private Map<String, String> _headers;
  private List<HttpCookie> _cookies;
  private RestLiServiceException _exception;

  private RestLiResponseEnvelope _responseEnvelope;

  // Private constructor used to instantiate all shared common objects used.
  private RestLiResponseDataImpl(Map<String, String> headers, List<HttpCookie> cookies)
  {
    _headers = new TreeMap<String, String>(headers);
    _cookies = cookies;
  }

  /**
   * Instantiates a top level response with no exceptions.
   *
   * @param httpStatus Status of the response.
   * @param headers of the response.
   * @param cookies
   */
  public RestLiResponseDataImpl(HttpStatus httpStatus, Map<String, String> headers, List<HttpCookie> cookies)
  {
    this(headers, cookies);
    setStatus(httpStatus);
  }

  /**
   * Instantiates a top level failed response with an exception.
   *
   * @param exception exception thrown.
   * @param headers of the response.
   * @param cookies of the response
   */
  public RestLiResponseDataImpl(RestLiServiceException exception, Map<String, String> headers, List<HttpCookie> cookies)
  {
    this(headers, cookies);
    setServiceException(exception);
  }

  /**
   * Sets the response envelope stored in this response data. This method is package private because only the response
   * data builders should be allowed to set the envelope.
   *
   * @param responseEnvelope the {@link RestLiResponseEnvelope} to set into this data storage.
   */
  void setResponseEnvelope(RestLiResponseEnvelope responseEnvelope)
  {
    _responseEnvelope = responseEnvelope;
    // if this is an error response, the envelope should be cleared. We still want to keep the envelope for the metadata
    // stored within it - only the data should be cleared.
    if (isErrorResponse())
    {
      _responseEnvelope.clearData();
    }
  }

  /**
   * Sets the top level exception of this response.
   * Each inheriting class must maintain invariant unique to its type.
   *
   * This method is for internal use only. If you wish to set an exception in a filter, please do so by either throwing
   * an exception or by completing the future exceptionally. For more information see {@link com.linkedin.restli.server.filter.Filter}.
   *
   * @param throwable to set this response to.
   */
  public void setException(Throwable throwable)
  {
    if (throwable == null)
    {
      throw new UnsupportedOperationException("Null is not permitted in setting an exception.");
    }
    RestLiServiceException restLiServiceException;
    if (throwable instanceof RestLiServiceException)
    {
      restLiServiceException = (RestLiServiceException) throwable;
    }
    else if (throwable instanceof RoutingException)
    {
      RoutingException routingException = (RoutingException) throwable;

      restLiServiceException = new RestLiServiceException(HttpStatus.fromCode(routingException.getStatus()),
                                                          routingException.getMessage(),
                                                          routingException);
    }
    else
    {
      restLiServiceException = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                                          throwable.getMessage(),
                                                          throwable);
    }
    setServiceException(restLiServiceException);
  }

  /**
   * Sets the {@link RestLiServiceException} for this response.
   *
   * @param exception the exception to set for this response.
   */
  private void setServiceException(RestLiServiceException exception)
  {
    if (exception == null)
    {
      throw new UnsupportedOperationException("Null is not permitted in setting an exception.");
    }
    _exception = exception;
    _status = null;
    if (_responseEnvelope != null)
    {
      _responseEnvelope.clearData();
    }
  }

  /**
   * Returns whether or not the response is an error. Because of the invariant condition, whether or not the exception
   * is null can be used to indicate an error.
   *
   * @return whether or not the response is an error.
   */
  @Override
  public boolean isErrorResponse()
  {
    return _exception != null;
  }

  /**
   * Returns the exception associated with this response. If there is no error, the returned exception will be null.
   *
   * @return the exception associated with this response.
   */
  @Override
  public RestLiServiceException getServiceException()
  {
    return _exception;
  }

  /**
   * Returns the top level status either from the response or from the exception.
   *
   * @return Top level status of the request.
   */
  @Override
  public HttpStatus getStatus()
  {
    return _exception != null ? _exception.getStatus() : _status;
  }

  @Override
  public ResponseType getResponseType()
  {
    return _responseEnvelope.getResponseType();
  }

  @Override
  public ResourceMethod getResourceMethod()
  {
    return _responseEnvelope.getResourceMethod();
  }

  /**
   * Sets the status of a response for when there are no exceptions.
   * Does not check if exception is already null, but will instead
   * null the exception to maintain the invariant that there
   * is only one source for getting the status.
   *
   * @param status status to set for this response.
   */
  void setStatus(HttpStatus status)
  {
    if (status == null)
    {
      throw new UnsupportedOperationException("Setting status to null is not permitted for when there are no exceptions.");
    }
    _status = status;
    _exception = null;

    _headers.remove(HeaderUtil.getErrorResponseHeaderName(_headers));
  }

  @Override
  public Map<String, String> getHeaders()
  {
    return _headers;
  }

  @Override
  public List<HttpCookie> getCookies()
  {
    return _cookies;
  }

  @Override
  public RecordResponseEnvelope getRecordResponseEnvelope()
  {
    try
    {
      return (RecordResponseEnvelope) _responseEnvelope;
    }
    catch (ClassCastException e)
    {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public CollectionResponseEnvelope getCollectionResponseEnvelope()
  {
    try
    {
      return (CollectionResponseEnvelope) _responseEnvelope;
    }
    catch (ClassCastException e)
    {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public BatchResponseEnvelope getBatchResponseEnvelope()
  {
    try
    {
      return (BatchResponseEnvelope) _responseEnvelope;
    }
    catch (ClassCastException e)
    {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public EmptyResponseEnvelope getEmptyResponseEnvelope()
  {
    try
    {
      return (EmptyResponseEnvelope) _responseEnvelope;
    }
    catch (ClassCastException e)
    {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public ActionResponseEnvelope getActionResponseEnvelope()
  {
    try
    {
      return (ActionResponseEnvelope) _responseEnvelope;
    }
    catch (ClassCastException e)
    {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public BatchCreateResponseEnvelope getBatchCreateResponseEnvelope()
  {
    try
    {
      return (BatchCreateResponseEnvelope) _responseEnvelope;
    }
    catch (ClassCastException e)
    {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public BatchDeleteResponseEnvelope getBatchDeleteResponseEnvelope()
  {
    try
    {
      return (BatchDeleteResponseEnvelope) _responseEnvelope;
    }
    catch (ClassCastException e)
    {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public BatchGetResponseEnvelope getBatchGetResponseEnvelope()
  {
    try
    {
      return (BatchGetResponseEnvelope) _responseEnvelope;
    }
    catch (ClassCastException e)
    {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public BatchPartialUpdateResponseEnvelope getBatchPartialUpdateResponseEnvelope()
  {
    try
    {
      return (BatchPartialUpdateResponseEnvelope) _responseEnvelope;
    }
    catch (ClassCastException e)
    {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public BatchUpdateResponseEnvelope getBatchUpdateResponseEnvelope()
  {
    try
    {
      return (BatchUpdateResponseEnvelope) _responseEnvelope;
    }
    catch (ClassCastException e)
    {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public CreateResponseEnvelope getCreateResponseEnvelope()
  {
    try
    {
      return (CreateResponseEnvelope) _responseEnvelope;
    }
    catch (ClassCastException e)
    {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public DeleteResponseEnvelope getDeleteResponseEnvelope()
  {
    try
    {
      return (DeleteResponseEnvelope) _responseEnvelope;
    }
    catch (ClassCastException e)
    {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public FinderResponseEnvelope getFinderResponseEnvelope()
  {
    try
    {
      return (FinderResponseEnvelope) _responseEnvelope;
    }
    catch (ClassCastException e)
    {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public GetAllResponseEnvelope getGetAllResponseEnvelope()
  {
    try
    {
      return (GetAllResponseEnvelope) _responseEnvelope;
    }
    catch (ClassCastException e)
    {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public GetResponseEnvelope getGetResponseEnvelope()
  {
    try
    {
      return (GetResponseEnvelope) _responseEnvelope;
    }
    catch (ClassCastException e)
    {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public OptionsResponseEnvelope getOptionsResponseEnvelope()
  {
    try
    {
      return (OptionsResponseEnvelope) _responseEnvelope;
    }
    catch (ClassCastException e)
    {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public PartialUpdateResponseEnvelope getPartialUpdateResponseEnvelope()
  {
    try
    {
      return (PartialUpdateResponseEnvelope) _responseEnvelope;
    }
    catch (ClassCastException e)
    {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public UpdateResponseEnvelope getUpdateResponseEnvelope()
  {
    try
    {
      return (UpdateResponseEnvelope)_responseEnvelope;
    }
    catch (ClassCastException e)
    {
      throw new UnsupportedOperationException();
    }
  }
}
