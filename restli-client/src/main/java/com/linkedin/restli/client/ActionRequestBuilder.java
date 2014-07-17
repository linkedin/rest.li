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

import com.linkedin.data.DataList;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.data.template.DynamicRecordTemplate;
import com.linkedin.data.template.FieldDef;
import com.linkedin.restli.common.ActionResponse;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.internal.client.ActionResponseDecoder;
import com.linkedin.util.ArgumentUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class ActionRequestBuilder<K, V> extends AbstractRequestBuilder<K, V, ActionRequest<V>>
{
  private final TypeSpec<V>              _elementType;
  private K                              _id;
  private String                         _name;
  private final Map<FieldDef<?>, Object> _actionParams = new HashMap<FieldDef<?>, Object>();

  public ActionRequestBuilder(String baseUriTemplate, Class<V> elementClass, ResourceSpec resourceSpec, RestliRequestOptions requestOptions)
  {
    this(baseUriTemplate, new TypeSpec<V>(elementClass), resourceSpec, requestOptions);
  }

  public ActionRequestBuilder(String baseUriTemplate, TypeSpec<V> elementType, ResourceSpec resourceSpec, RestliRequestOptions requestOptions)
  {
    super(baseUriTemplate, resourceSpec, requestOptions);
    _elementType = elementType;
  }

  public ActionRequestBuilder<K, V> name(String name)
  {
    _name = name;
    setParam(RestConstants.ACTION_PARAM, _name);
    return this;
  }

  public ActionRequestBuilder<K, V> id(K id)
  {
    _id = id;
    return this;
  }

  public ActionRequestBuilder<K, V> setParam(FieldDef<?> key, Object value)
  {
    _actionParams.put(key, value);
    return this;
  }

  public ActionRequestBuilder<K, V> setReqParam(FieldDef<?> key, Object value)
  {
    ArgumentUtil.notNull(value, "value");
    return setParam(key, value);
  }

  public ActionRequestBuilder<K, V> addParam(FieldDef<?> key, Object value)
  {
    if (value != null)
    {
      final Object existingData = _actionParams.get(key);
      if (existingData == null)
      {
        return setParam(key, value);
      }
      else
      {
        final DataList newList;
        if (existingData instanceof DataList)
        {
          newList = ((DataList) existingData);
          newList.add(value);
        }
        else
        {
          newList = new DataList(Arrays.asList(existingData, value));
        }
        _actionParams.put(key, newList);
      }
    }

    return this;
  }

  public ActionRequestBuilder<K, V> addReqParam(FieldDef<?> key, Object value)
  {
    ArgumentUtil.notNull(value, "value");
    return addParam(key, value);
  }

  @Override
  public ActionRequestBuilder<K, V> setHeader(String key, String value)
  {
    super.setHeader(key, value);
    return this;
  }

  @Override
  public ActionRequestBuilder<K, V> setHeaders(Map<String, String> headers)
  {
    super.setHeaders(headers);
    return this;
  }

  @Override
  public ActionRequestBuilder<K, V> addHeader(String key, String value)
  {
    super.addHeader(key, value);
    return this;
  }

  @Override
  public ActionRequestBuilder<K, V> pathKey(String name, Object value)
  {
    super.pathKey(name, value);
    return this;
  }

  @SuppressWarnings("unchecked")
  @Override
  public ActionRequest<V> build()
  {
    if (_name == null)
    {
      throw new IllegalStateException("name required to build action request");
    }

    RecordDataSchema requestDataSchema;
    RecordDataSchema actionResponseDataSchema;
    FieldDef<V> responseFieldDef;

    if (_resourceSpec.getRequestMetadata(_name) == null) // old builder code in use
    {
      requestDataSchema = DynamicRecordMetadata.buildSchema(_name, _actionParams.keySet());

      Collection<FieldDef<?>> responseFieldDefCollection;
      if (_elementType.getType() == Void.class)
      {
        responseFieldDef = null;
        responseFieldDefCollection = Collections.emptyList();
      }
      else
      {
        responseFieldDef = new FieldDef<V>(ActionResponse.VALUE_NAME, _elementType.getType(), _elementType.getSchema());
        responseFieldDefCollection = Collections.<FieldDef<?>>singleton(responseFieldDef);
      }
      actionResponseDataSchema = DynamicRecordMetadata.buildSchema(_name,responseFieldDefCollection);
    }
    else
    {
      requestDataSchema =  _resourceSpec.getRequestMetadata(_name).getRecordDataSchema();
      actionResponseDataSchema = _resourceSpec.getActionResponseMetadata(_name).getRecordDataSchema();
      responseFieldDef = (FieldDef<V>)_resourceSpec.getActionResponseMetadata(_name).getFieldDef(ActionResponse.VALUE_NAME);
    }

    @SuppressWarnings("unchecked")
    ActionResponseDecoder<V> actionResponseDecoder =
        new ActionResponseDecoder<V>(responseFieldDef, actionResponseDataSchema);
    DynamicRecordTemplate inputParameters =
        new DynamicRecordTemplate(requestDataSchema, buildReadOnlyActionParameters());
    inputParameters.data().setReadOnly();
    return new ActionRequest<V>(inputParameters,
                                buildReadOnlyHeaders(),
                                actionResponseDecoder,
                                _resourceSpec,
                                buildReadOnlyQueryParameters(),
                                _name,
                                getBaseUriTemplate(),
                                buildReadOnlyPathKeys(),
                                getRequestOptions(),
                                buildReadOnlyId());

  }

  private Map<FieldDef<?>, Object> buildReadOnlyActionParameters()
  {
    try
    {
      Map<FieldDef<?>, Object> readOnlyParameters = new HashMap<FieldDef<?>, Object>(_actionParams.size());

      for (Map.Entry<FieldDef<?>, Object> originalParameterEntry : _actionParams.entrySet())
      {
        readOnlyParameters.put(
            originalParameterEntry.getKey(),
            getReadOnlyActionParameter(originalParameterEntry.getValue()));
      }

      return readOnlyParameters;
    }
    catch (CloneNotSupportedException cloneException)
    {
      throw new IllegalArgumentException("Action parameters cannot be cloned", cloneException);
    }
  }

  private Object getReadOnlyActionParameter(Object original) throws CloneNotSupportedException
  {
    Object result = original;

    if (original.getClass().isArray())
    {
      //Since the array shell will be replace with a data list internally we do not need to
      // make the collection immutable.
      Object[] items = (Object[]) original;
      Object[] readOnlyItems = new Object[items.length];

      for (int i = 0; i < items.length; i++)
      {
        readOnlyItems[i] = getReadOnlyActionParameter(items[i]);
      }

      result = readOnlyItems;
    }
    else if (DataTemplate.class.isAssignableFrom(original.getClass()))
    {
      result = getReadOnlyOrCopyDataTemplate((DataTemplate) original);
    }

    return result;
  }

  private K buildReadOnlyId()
  {
    try
    {
      return getReadOnlyOrCopyKey(_id);
    }
    catch (CloneNotSupportedException cloneException)
    {
      throw new IllegalArgumentException("Key cannot be copied.", cloneException);
    }
  }
}
