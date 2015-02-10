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

package com.linkedin.restli.internal.server.model;


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.data.template.AbstractArrayTemplate;
import com.linkedin.data.template.AbstractMapTemplate;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.FieldDef;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.TemplateOutputCastException;
import com.linkedin.data.template.UnionTemplate;
import com.linkedin.restli.internal.common.ValueConverter;
import com.linkedin.restli.server.ResourceConfigException;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.QueryParam;

import java.io.IOException;


/**
 * Descriptor for a rest.li parameter. Applicable to both {@link QueryParam} (in Finders)
 * and {@link ActionParam} (in Actions)
 *
 * @author dellamag
 */
public class Parameter<T> extends FieldDef<T>
{
  public enum ParamType
  {
    QUERY,                  // @QueryParam
    RESOURCE_KEY,           // normal resource keys
    @Deprecated
    KEY,                    // @AssocKey
    ASSOC_KEY_PARAM,        // @AssocKeyParam
    POST,                   // POST-based parameters such as @ActionParam or CREATE
    /**
     * @deprecated Use PAGING_CONTEXT_PARAM instead
     */
    @Deprecated
    CONTEXT,                      // @PagingContext
    PAGING_CONTEXT_PARAM,         // @PagingContextParam
    CALLBACK,                     // @CallbackParam
    @Deprecated
    PARSEQ_CONTEXT,               // @ParSeqContext
    PARSEQ_CONTEXT_PARAM,         // @ParSeqContextParam
    BATCH,
    @Deprecated
    PROJECTION,                   // @Projection
    PROJECTION_PARAM,             // @ProjectionParam
    @Deprecated
    PATH_KEYS,                    // @Keys
    PATH_KEYS_PARAM,              // @PathKeysParam
    @Deprecated
    RESOURCE_CONTEXT,             // @ResourceContextParam
    RESOURCE_CONTEXT_PARAM,       // @ResourceContextParam
    HEADER,                       // @HeaderParam
    METADATA_PROJECTION_PARAM,    // @MetadataProjectionParam
    PAGING_PROJECTION_PARAM,      // @PagingProjectionParam
    VALIDATOR_PARAM               // @ValidatorParam
  }

  private final boolean _optional;
  private final Object _defaultValue;
  private final Object _defaultValueData;

  private final ParamType _paramType;

  private final boolean _custom;

  private final boolean _isArray; // true if the parameter is an array
  private final Class<?> _itemType; // array item type or null

  private final AnnotationSet _annotations;
  private final DataMap _customAnnotations;

  public Parameter(final String name,
                   final Class<T> type,
                   final DataSchema dataSchema,
                   final boolean optional,
                   final Object defaultValueData,
                   final ParamType paramType,
                   final boolean custom,
                   final AnnotationSet annotations)
  {
    super(name, type, dataSchema);

    _optional = optional;
    _defaultValue = null;
    _defaultValueData = defaultValueData;
    _paramType = paramType;
    _custom = custom;

    _isArray = getType().isArray();
    _itemType = getType().getComponentType();
    _annotations = annotations;
    _customAnnotations = ResourceModelAnnotation.getAnnotationsMap(annotations.getAll());
  }

  public boolean isOptional()
  {
    return _optional;
  }

  public boolean hasDefaultValue()
  {
    return _defaultValueData != null;
  }

  public Object getDefaultValue()
  {
    if (_defaultValueData == null)
    {
      return null;
    }

    final Object result;
    if (_defaultValueData instanceof String)
    {
      final String defaultValueString = (String) _defaultValueData;
      try
      {
        if (getType().isArray())
        {
          final DataList valueAsDataList = _codec.stringToList(defaultValueString);
          result = DataTemplateUtil.convertDataListToArray(valueAsDataList, getItemType());
        }
        else if (DataTemplate.class.isAssignableFrom(getType()))
        {
          final Object input;
          if (AbstractArrayTemplate.class.isAssignableFrom(getType()))
          {
            input = _codec.stringToList(defaultValueString);
          }
          else if (AbstractMapTemplate.class.isAssignableFrom(getType()) ||
            UnionTemplate.class.isAssignableFrom(getType()) ||
            RecordTemplate.class.isAssignableFrom(getType()))
          {
            input = _codec.stringToMap(defaultValueString);
          }
          else
          {
            input = defaultValueString;
          }

          result = DataTemplateUtil.wrap(input, getType().asSubclass(DataTemplate.class));
          validate((DataTemplate<?>) result, getType());
        }
        else
        {
          result = ValueConverter.coerceString(defaultValueString, getType());
        }
      }
      catch (TemplateOutputCastException e)
      {
        throw new ResourceConfigException(e.getMessage(), e);
      }
      catch (IllegalArgumentException e)
      {
        throw new ResourceConfigException("Default value for parameter of type \""
                                              + getType().getName() + "\" is not supported: " + e.getMessage(), e);
      }
      catch (IOException e)
      {
        throw new ResourceConfigException("Default value for parameter of type \""
                                              + getType().getName() + "\" is not supported: " + e.getMessage(), e);
      }
    }
    else
    {
      result = _defaultValueData;
    }

    return result;
  }

  public Object getDefaultValueData()
  {
    return _defaultValueData;
  }

  public ParamType getParamType()
  {
    return _paramType;
  }

  public boolean isCustom()
  {
    return _custom;
  }

  public boolean isArray()
  {
    return _isArray;
  }

  public Class<?> getItemType()
  {
    return _itemType;
  }

  public AnnotationSet getAnnotations()
  {
    return _annotations;
  }

  public DataMap getCustomAnnotationData()
  {
    return _customAnnotations;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder(super.toString());
    sb.append(", defaultValue=")
      .append(_defaultValue)
      .append(", isOptional=")
      .append(_optional)
      .append(", paramType=")
      .append(_paramType);
    return sb.toString();
  }

  private static void validate(DataTemplate<?> data, Class<?> clazz)
  {
    final ValidationResult valResult = ValidateDataAgainstSchema.validate(data.data(),
                                                                          data.schema(),
                                                                          _defaultValOptions);
    if (!valResult.isValid())
    {
      throw new IllegalArgumentException("Coercing String \"" + data.data() + "\" to type " + clazz.getName() + " failed due to schema validation: " + valResult.getMessages());
    }
  }

  private static final JacksonDataCodec _codec = new JacksonDataCodec();
  private static final ValidationOptions _defaultValOptions = new ValidationOptions();
}
