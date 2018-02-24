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

package com.linkedin.data.template;


import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaUtil;
import com.linkedin.data.schema.RecordDataSchema;

import static com.linkedin.data.schema.DataSchemaUtil.*;


/**
 * A dynamic record template field definition.
 *
 * @see DynamicRecordTemplate
 * @author Eran Leshem
 */
public class FieldDef<T>
{
  private final String _name;
  private final Class<T> _type;
  private final DataSchema _dataSchema;
  private final Class<?> _dataClass;
  private final RecordDataSchema.Field _field;

  public FieldDef(String name, Class<T> type)
  {
    this(name, type, DataTemplateUtil.getSchema(type));
  }

  public FieldDef(String name, Class<T> type, DataSchema dataSchema)
  {
    _name = name;
    _type = type;
    _dataSchema = dataSchema;
    /**
     * FieldDefs representing context, pagination, or things relating to synchronization will not
     * have schemas, so dataSchema and thus dataClass can be null.
     */
    _dataClass = getDataClassFromSchema(_dataSchema);

    StringBuilder errorMessageBuilder = new StringBuilder();
    _field = new RecordDataSchema.Field(_dataSchema);
    _field.setName(_name, errorMessageBuilder);
  }

  public String getName()
  {
    return _name;
  }

  public Class<?> getType()
  {
    return _type;
  }

  public DataSchema getDataSchema()
  {
    return _dataSchema;
  }

  public Class<?> getDataClass()
  {
    return _dataClass;
  }

  public RecordDataSchema.Field getField()
  {
    return _field;
  }

  @Override
  public String toString()
  {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("FieldDef{_name='");
    stringBuilder.append(_name);
    stringBuilder.append("\', _type=");
    stringBuilder.append(_type.getName());
    if (_dataSchema != null)
    {
      stringBuilder.append(", _dataSchema=");
      stringBuilder.append(_dataSchema.toString());
    }
    stringBuilder.append("}");
    return stringBuilder.toString();
  }

  @Override
  public boolean equals(Object object)
  {
    if (object instanceof FieldDef)
    {
      @SuppressWarnings("unchecked")
      FieldDef<T> other = (FieldDef<T>) object;

      boolean dataSchemaEquals;
      if (this._dataSchema == null)
      {
        dataSchemaEquals = (other._dataSchema == null);
      }
      else
      {
        dataSchemaEquals = this._dataSchema.equals(other._dataSchema);
      }

      return this._name.equals(other._name) && this._type.equals(other._type) && dataSchemaEquals;
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return 13*_name.hashCode() + 17*_type.hashCode() + 23*(_dataSchema == null? 1 :_dataSchema.hashCode());
  }
}
