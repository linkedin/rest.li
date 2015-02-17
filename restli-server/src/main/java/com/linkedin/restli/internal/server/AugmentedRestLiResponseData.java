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

package com.linkedin.restli.internal.server;


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.common.HeaderUtil;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiResponseDataException;
import com.linkedin.restli.server.RestLiServiceException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.lang.Validate;

import static com.linkedin.restli.common.ResourceMethod.ACTION;
import static com.linkedin.restli.common.ResourceMethod.BATCH_CREATE;
import static com.linkedin.restli.common.ResourceMethod.BATCH_DELETE;
import static com.linkedin.restli.common.ResourceMethod.BATCH_GET;
import static com.linkedin.restli.common.ResourceMethod.BATCH_PARTIAL_UPDATE;
import static com.linkedin.restli.common.ResourceMethod.BATCH_UPDATE;
import static com.linkedin.restli.common.ResourceMethod.CREATE;
import static com.linkedin.restli.common.ResourceMethod.DELETE;
import static com.linkedin.restli.common.ResourceMethod.FINDER;
import static com.linkedin.restli.common.ResourceMethod.GET;
import static com.linkedin.restli.common.ResourceMethod.GET_ALL;
import static com.linkedin.restli.common.ResourceMethod.OPTIONS;
import static com.linkedin.restli.common.ResourceMethod.PARTIAL_UPDATE;
import static com.linkedin.restli.common.ResourceMethod.UPDATE;


/**
 * Concrete implementation of {@link RestLiResponseData}.
 *
 * @author nshankar
 *
 */
public class AugmentedRestLiResponseData implements RestLiResponseData
{
  private RecordTemplate _entity;
  private RestLiServiceException _serviceException;
  private List<? extends RecordTemplate> _entities;
  private Map<?, ? extends RecordTemplate> _keyEntityMap;
  private HttpStatus _status;
  private final Map<String, String> _headers;
  private CollectionMetadata _paging;
  private RecordTemplate _collectionResponseCustomMetadata;
  private final ResponseType _responseType;

  private AugmentedRestLiResponseData(ResponseType responseType,
                                      RecordTemplate entity,
                                      RestLiServiceException serviceException,
                                      List<? extends RecordTemplate> entities,
                                      CollectionMetadata paging,
                                      RecordTemplate metadata,
                                      Map<?, ? extends RecordTemplate> keyEntityMap,
                                      HttpStatus status,
                                      Map<String, String> headers)
  {
    _responseType = responseType;
    _entity = entity;
    _serviceException = serviceException;
    _entities = entities;
    _keyEntityMap = keyEntityMap;
    _status = status;
    _headers = headers;
    _paging = paging;
    _collectionResponseCustomMetadata = metadata;
  }

  @Override
  public boolean isEntityResponse()
  {
    return _entity != null;
  }

  @Override
  public boolean isCollectionResponse()
  {
    return _entities != null;
  }

  @Override
  public boolean isBatchResponse()
  {
    return _keyEntityMap != null;
  }

  @Override
  public boolean isErrorResponse()
  {
    return _serviceException != null;
  }

  @Override
  public RecordTemplate getEntityResponse()
  {
    return _entity;
  }

  @Override
  public void setEntityResponse(RecordTemplate entity) throws RestLiResponseDataException
  {
    _responseType.validateResponseData(entity);
    _entity = entity;
    _entities = null;
    _keyEntityMap = null;
    clearServiceException();
  }

  @Override
  public RestLiServiceException getServiceException()
  {
    return _serviceException;
  }

  @Override
  public List<? extends RecordTemplate> getCollectionResponse()
  {
    return _entities;
  }

  @Override
  public Map<?, ? extends RecordTemplate> getBatchResponseMap()
  {
    return _keyEntityMap;
  }

  @Override
  public CollectionMetadata getCollectionResponsePaging()
  {
    return _paging;
  }

  @Override
  public void setCollectionResponsePaging(CollectionMetadata paging) throws RestLiResponseDataException
  {
    if (paging != null && !isCollectionResponse())
    {
      throw new RestLiResponseDataException("This method can be invoked only for collection responses");
    }
    _paging = paging;
  }

  @Override
  public RecordTemplate getCollectionResponseCustomMetadata()
  {
    return _collectionResponseCustomMetadata;
  }

  @Override
  public void setCollectionResponseCustomMetadata(RecordTemplate metadata) throws RestLiResponseDataException
  {
    if (metadata != null && !isCollectionResponse())
    {
      throw new RestLiResponseDataException("This method can be invoked only for collection responses");
    }
    _collectionResponseCustomMetadata = metadata;
  }

  @Override
  public void setCollectionResponse(List<? extends RecordTemplate> responseEntities) throws RestLiResponseDataException
  {
    _responseType.validateResponseData(responseEntities);
    _entities = responseEntities;
    _entity = null;
    _keyEntityMap = null;
    clearServiceException();
  }

  @Override
  public void setBatchKeyResponseMap(Map<?, ? extends RecordTemplate> batchEntityMap) throws RestLiResponseDataException
  {
    _responseType.validateResponseData(batchEntityMap);
    _keyEntityMap = batchEntityMap;
    _entity = null;
    _entities = null;
    clearServiceException();
  }

  private void clearServiceException()
  {
    _serviceException = null;
    _headers.remove(HeaderUtil.getErrorResponseHeaderName(_headers));
  }

  public HttpStatus getStatus()
  {
    return _status;
  }

  public void setStatus(HttpStatus status)
  {
    _status = status;
  }

  public Map<String, String> getHeaders()
  {
    return _headers;
  }

  public static class Builder
  {
    private RecordTemplate _entity;
    private RestLiServiceException _serviceException;
    private List<? extends RecordTemplate> _entities;
    private Map<?, ? extends RecordTemplate> _keyEntityMap;
    private HttpStatus _status;
    private Map<String, String> _headers;
    private CollectionMetadata _collectionResponsePaging;
    private RecordTemplate _collectionMetadata;
    private final ResponseType _responseType;

    public Builder(ResourceMethod methodType)
    {
      _responseType = ResponseType.fromMethodType(methodType);
      _status = HttpStatus.S_200_OK;
      _headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    }

    public Builder status(HttpStatus status)
    {
      _status = status;
      return this;
    }

    public Builder headers(Map<String, String> headers)
    {
      _headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
      _headers.putAll(headers);
      return this;
    }

    public Builder collectionResponsePaging(CollectionMetadata paging)
    {
      try
      {
        _responseType.validatePagingAndCustomMetadata();
      }
      catch (RestLiResponseDataException e)
      {
        throw new IllegalArgumentException(e);
      }
      _collectionResponsePaging = paging;
      return this;
    }

    public Builder collectionCustomMetadata(RecordTemplate metadata)
    {
      try
      {
        _responseType.validatePagingAndCustomMetadata();
      }
      catch (RestLiResponseDataException e)
      {
        throw new IllegalArgumentException(e);
      }
      _collectionMetadata = metadata;
      return this;
    }

    public Builder entity(RecordTemplate entity)
    {
      try
      {
        _responseType.validateResponseData(entity);
      }
      catch (RestLiResponseDataException e)
      {
        throw new IllegalArgumentException(e);
      }
      _entity = entity;
      return this;
    }

    public Builder serviceException(RestLiServiceException serviceException)
    {
      _serviceException = serviceException;
      return this;
    }

    public Builder collectionEntities(List<? extends RecordTemplate> entities)
    {
      try
      {
        _responseType.validateResponseData(entities);
      }
      catch (RestLiResponseDataException e)
      {
        throw new IllegalArgumentException(e);
      }
      _entities = entities;
      return this;
    }

    public Builder batchKeyEntityMap(Map<?, ? extends RecordTemplate> map)
    {
      try
      {
        _responseType.validateResponseData(map);
      }
      catch (RestLiResponseDataException e)
      {
        throw new IllegalArgumentException(e);
      }
      _keyEntityMap = map;
      return this;
    }

    public AugmentedRestLiResponseData build()
    {
      return new AugmentedRestLiResponseData(_responseType, _entity, _serviceException, _entities,
                                             _collectionResponsePaging, _collectionMetadata, _keyEntityMap, _status,
                                             _headers);
    }
  }

  private enum ResponseType
  {
    SIMPLE(RecordTemplate.class, false, GET, ACTION, CREATE),
    GET_COLLECTION(List.class, true, GET_ALL, FINDER),
    CREATE_COLLECTION(List.class, false, BATCH_CREATE),
    BATCH(Map.class, false, BATCH_GET, BATCH_UPDATE, BATCH_PARTIAL_UPDATE, BATCH_DELETE),
    NONE(null, false, PARTIAL_UPDATE, UPDATE, DELETE, OPTIONS);

    private ResponseType(Class<?> clasz, boolean supportsPagingAndCustomMetadata, ResourceMethod... types)
    {
      _methodTypes = Arrays.asList(types);
      _class = clasz;
      _supportsPagingAndCustomMetadata = supportsPagingAndCustomMetadata;
    }

    public static ResponseType fromMethodType(ResourceMethod type)
    {
      for (ResponseType responseType : values())
      {
        if (responseType._methodTypes.contains(type))
        {
          return responseType;
        }
      }
      return NONE;
    }

    public void validatePagingAndCustomMetadata() throws RestLiResponseDataException
    {
      if (!_supportsPagingAndCustomMetadata)
      {
        throw new RestLiResponseDataException(String.format("Paging and/or custom metadata "
            + "is not permitted for %s method types.", _methodTypes));
      }
    }

    public void validateResponseData(Object value) throws RestLiResponseDataException
    {
      if (_class != null)
      {
        Validate.notNull(value);
        if (!_class.isAssignableFrom(value.getClass()))
        {
          throw new RestLiResponseDataException(
                                                String.format("Response data of type %s is not permitted for %s method types.",
                                                              value.getClass().getSimpleName(), _methodTypes));
        }
      }
      else
      {
        throw new RestLiResponseDataException(String.format("No response data is permitted for %s method types",
                                                            _methodTypes));
      }
    }

    private final List<ResourceMethod> _methodTypes;
    private final Class<?> _class;
    private final boolean _supportsPagingAndCustomMetadata;
  }
}
