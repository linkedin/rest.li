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

import com.linkedin.data.ByteString;
import com.linkedin.data.DataList;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.data.template.AbstractArrayTemplate;
import com.linkedin.data.template.AbstractMapTemplate;
import com.linkedin.data.template.BooleanArray;
import com.linkedin.data.template.BytesArray;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.DirectArrayTemplate;
import com.linkedin.data.template.DoubleArray;
import com.linkedin.data.template.FieldDef;
import com.linkedin.data.template.FixedTemplate;
import com.linkedin.data.template.FloatArray;
import com.linkedin.data.template.IntegerArray;
import com.linkedin.data.template.LongArray;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.StringArray;
import com.linkedin.data.template.UnionTemplate;
import com.linkedin.data.template.WrappingArrayTemplate;
import com.linkedin.restli.internal.common.ValueConverter;
import com.linkedin.restli.server.ResourceConfigException;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.QueryParam;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;


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
    QUERY,          // @QueryParam
    KEY,            // @AssocKey
    POST,           // POST-based parameters such as @ActionParam or CREATE
    CONTEXT,        // @Context
    CALLBACK,       // @CallbackParam
    PARSEQ_CONTEXT, // @ParSeqContext
    BATCH
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

  @SuppressWarnings({"unchecked", "rawtypes"})
  public Object getDefaultValue()
  {
    if (_defaultValueData == null)
    {
      return null;
    }

    final Object result;
    if (_defaultValueData instanceof String)
    {
      final String _defaultValueString = (String) _defaultValueData;
      try
      {
        if (getType().isArray())
        {
          final DataList valueAsDataList = _codec.stringToList(_defaultValueString);
          final AbstractArrayTemplate<?> arrayTemplate;
          if (DataTemplate.class.isAssignableFrom(_itemType))
          {
            final ArrayDataSchema arraySchema = new ArrayDataSchema(DataTemplateUtil.getSchema(_itemType));
            arrayTemplate = new DynamicWrappedArray(valueAsDataList, arraySchema, _itemType);
          }
          else
          {
            final Class<? extends DirectArrayTemplate<?>> directArrayTemplateType = _componentTypeToDirectArrayTemplate.get(_itemType);
            arrayTemplate = DataTemplateUtil.wrap(valueAsDataList, directArrayTemplateType);
          }

          validate(arrayTemplate, getType());

          final Object resultArray = Array.newInstance(_itemType, arrayTemplate.size());
          for (int i = 0; i < arrayTemplate.size(); ++i)
          {
            Array.set(resultArray, i, arrayTemplate.get(i));
          }
          result = resultArray;
        }
        else if (FixedTemplate.class.isAssignableFrom(getType()))
        {
          result = DataTemplateUtil.wrap(_defaultValueString, getType().asSubclass(FixedTemplate.class));
          validate((FixedTemplate) result, getType());
        }
        else if (AbstractArrayTemplate.class.isAssignableFrom(getType()))
        {
          final DataList valueAsDataList = _codec.stringToList(_defaultValueString);
          result = DataTemplateUtil.wrap(valueAsDataList, getType().asSubclass(AbstractArrayTemplate.class));
          validate((AbstractArrayTemplate) result, getType());
        }
        else if (AbstractMapTemplate.class.isAssignableFrom(getType()) ||
            UnionTemplate.class.isAssignableFrom(getType()) ||
            RecordTemplate.class.isAssignableFrom(getType()))
        {
          final DataMap valueAsDataMap = _codec.stringToMap(_defaultValueString);
          result = DataTemplateUtil.wrap(valueAsDataMap, getType().asSubclass(DataTemplate.class));
          validate((DataTemplate) result, getType());
        }
        else
        {
          result = ValueConverter.coerceString(_defaultValueString, getType());
        }
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
    StringBuilder sb = new StringBuilder(super.toString());
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

  private static class  DynamicWrappedArray<T extends DataTemplate<?>> extends WrappingArrayTemplate<T>
  {
    @SuppressWarnings("unchecked")
    private DynamicWrappedArray(DataList list, ArrayDataSchema schema, Class<T> elementClass)
    {
      super(list, schema, elementClass);
    }
  }

  private static final JacksonDataCodec _codec = new JacksonDataCodec();
  private static final ValidationOptions _defaultValOptions = new ValidationOptions();
  private static final Map<Class<?>, Class<? extends DirectArrayTemplate<?>>> _componentTypeToDirectArrayTemplate =
      new HashMap<Class<?>, Class<? extends DirectArrayTemplate<?>>>();
  static
  {
    _componentTypeToDirectArrayTemplate.put(boolean.class, BooleanArray.class);
    _componentTypeToDirectArrayTemplate.put(Boolean.class, BooleanArray.class);
    _componentTypeToDirectArrayTemplate.put(int.class, IntegerArray.class);
    _componentTypeToDirectArrayTemplate.put(Integer.class, IntegerArray.class);
    _componentTypeToDirectArrayTemplate.put(long.class, LongArray.class);
    _componentTypeToDirectArrayTemplate.put(Long.class, LongArray.class);
    _componentTypeToDirectArrayTemplate.put(float.class, FloatArray.class);
    _componentTypeToDirectArrayTemplate.put(Float.class, FloatArray.class);
    _componentTypeToDirectArrayTemplate.put(double.class, DoubleArray.class);
    _componentTypeToDirectArrayTemplate.put(Double.class, DoubleArray.class);
    _componentTypeToDirectArrayTemplate.put(String.class, StringArray.class);
    _componentTypeToDirectArrayTemplate.put(ByteString.class, BytesArray.class);
  }
}
