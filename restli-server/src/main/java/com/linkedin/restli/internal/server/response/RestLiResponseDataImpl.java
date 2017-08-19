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
import com.linkedin.restli.internal.server.ResponseType;

import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import java.net.HttpCookie;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * Concrete implementation of {@link RestLiResponseData}.
 *
 * @param <E> The type of the {@link RestLiResponseEnvelope}.
 *
 * @author gye
 * @author xma
 */
class RestLiResponseDataImpl<E extends RestLiResponseEnvelope> implements RestLiResponseData<E>
{
  private Map<String, String> _headers;
  private List<HttpCookie> _cookies;

  private E _responseEnvelope;

  /**
   * Instantiates a top level response with no exceptions.
   * @param headers of the response.
   * @param cookies of the response.
   */
  RestLiResponseDataImpl(E envelope, Map<String, String> headers, List<HttpCookie> cookies)
  {
    assert envelope != null;

    _headers = new TreeMap<>(headers);
    _cookies = cookies;
    _responseEnvelope = envelope;
  }

  /**
   * Returns whether or not the response is an error. Because of the invariant condition, whether or not the exception
   * is null can be used to indicate an error.
   *
   * @return whether or not the response is an error.
   */
  @Override
  @Deprecated
  public boolean isErrorResponse()
  {
    return _responseEnvelope.getException() != null;
  }

  /**
   * Returns the exception associated with this response. If there is no error, the returned exception will be null.
   *
   * @return the exception associated with this response.
   */
  @Override
  @Deprecated
  public RestLiServiceException getServiceException()
  {
    return _responseEnvelope.getException();
  }

  /**
   * Returns the top level status either from the response or from the exception.
   *
   * @return Top level status of the request.
   */
  @Override
  @Deprecated
  public HttpStatus getStatus()
  {
    return _responseEnvelope.getStatus();
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
  public E getResponseEnvelope()
  {
    return _responseEnvelope;
  }

  @Override
  @Deprecated
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
  @Deprecated
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
  @Deprecated
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
  @Deprecated
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
  @Deprecated
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
  @Deprecated
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
  @Deprecated
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
  @Deprecated
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
  @Deprecated
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
  @Deprecated
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
  @Deprecated
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
  @Deprecated
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
  @Deprecated
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
  @Deprecated
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
  @Deprecated
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
  @Deprecated
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
  @Deprecated
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
  @Deprecated
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
