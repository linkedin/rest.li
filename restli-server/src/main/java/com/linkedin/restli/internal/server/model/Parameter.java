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

import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.template.FieldDef;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.QueryParam;

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
    QUERY,
    KEY,
    POST,
    CONTEXT,
    CALLBACK,
    PARSEQ_CONTEXT,
    BATCH
  }

  private final boolean _optional;
  private final T _defaultValue;

  private final ParamType _paramType;
  private final TyperefDataSchema _typerefSchema;

  private final boolean _custom;

  private final boolean _isArray; // true if the parameter is an array
  private final Class<?> _itemType; // array item type or null

  public Parameter(final String name,
                   final Class<T> type,
                   final TyperefDataSchema typerefSchema,
                   final boolean optional,
                   final T defaultValue,
                   final ParamType paramType,
                   final boolean custom)
  {
    super(name, type);

    _optional = optional;
    _defaultValue = defaultValue;
    _paramType = paramType;
    _typerefSchema = typerefSchema;
    _custom = custom;

    _isArray = getType().isArray();
    _itemType = getType().getComponentType();
  }

  public boolean isOptional()
  {
    return _optional;
  }

  public boolean hasDefaultValue()
  {
    return _defaultValue != null;
  }

  public Object getDefaultValue()
  {
    return _defaultValue;
  }

  public ParamType getParamType()
  {
    return _paramType;
  }

  public TyperefDataSchema getTyperefSchema()
  {
    return _typerefSchema;
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
    if (_typerefSchema != null)
    {
      sb.append(", typerefSchema=").append(_typerefSchema.getFullName());
    }
    return sb.toString();
  }
}
