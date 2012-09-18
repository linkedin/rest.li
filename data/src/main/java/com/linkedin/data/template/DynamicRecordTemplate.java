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


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.RecordDataSchema;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A generic record template that generates a schema on construction, based on a given collection of field definitions.
 *
 * @author Eran Leshem
 */
public class DynamicRecordTemplate extends RecordTemplate
{
  private final Map<FieldDef<?>, RecordDataSchema.Field> _fieldMap;
  private final RecordDataSchema _schema;

  /**
   * Construct a new {@link DynamicRecordTemplate}.
   *
   * @param name provides the name of the record type.
   * @param fieldDefs defines the fields of the record type.
   * @param data provides the underlying data that backs this {@link DynamicRecordTemplate}.
   */
  public DynamicRecordTemplate(String name, Collection<? extends FieldDef<?>> fieldDefs, DataMap data)
  {
    super(data, null);
    _fieldMap = new HashMap<FieldDef<?>, RecordDataSchema.Field>(fieldDefs.size() * 2);

    List<RecordDataSchema.Field> fields = new ArrayList<RecordDataSchema.Field>(fieldDefs.size());
    StringBuilder errorMessageBuilder = new StringBuilder(10);
    for (FieldDef<?> fieldDef: fieldDefs)
    {
      DataSchema paramSchema = DataTemplateUtil.getSchema(fieldDef.getType());
      RecordDataSchema.Field paramField = new RecordDataSchema.Field(paramSchema);
      paramField.setName(fieldDef.getName(), errorMessageBuilder);
      fields.add(paramField);
      _fieldMap.put(fieldDef, paramField);
    }

    _schema = new RecordDataSchema(new Name(name, errorMessageBuilder), RecordDataSchema.RecordType.RECORD);
    _schema.setFields(fields, errorMessageBuilder);
  }

  /**
   * Construct a new empty {@link DynamicRecordTemplate}.
   *
   * @param name provides the name of the record type.
   * @param fieldDefs defines the fields of the record type.
   */
  public DynamicRecordTemplate(String name, Collection<? extends FieldDef<?>> fieldDefs)
  {
    this(name, fieldDefs, new DataMap());
  }

  /**
   * Construct a new {@link DynamicRecordTemplate} initialized with the provided field values.
   *
   * @param name provides the name of the record type.
   * @param fieldDefValues defines the fields of the record type and the value of each field.
   */
  @SuppressWarnings({"unchecked"})
  public DynamicRecordTemplate(String name, Map<? extends FieldDef<?>, Object> fieldDefValues)
  {
    this(name, fieldDefValues.keySet());

    for (Map.Entry<? extends FieldDef<?>, Object> entry: fieldDefValues.entrySet())
    {
      setValue((FieldDef<Object>) entry.getKey(), entry.getValue());
    }
  }

  @Override
  public RecordDataSchema schema()
  {
    return _schema;
  }

  /**
   * Get value of a field.
   *
   * @param fieldDef specifies the field to get the value of.
   * @param <T> provides the expected return type.
   * @return the value of the field, or null if the field is not present, see {@link GetMode#STRICT}.
   */
  @SuppressWarnings({"unchecked"})
  public <T> T getValue(FieldDef<T> fieldDef)
  {
    RecordDataSchema.Field field = _fieldMap.get(fieldDef);
    if (DataTemplate.class.isAssignableFrom(fieldDef.getType()))
    {
      Class<? extends DataTemplate<?>> dataTemplateClass = (Class<DataTemplate<?>>) fieldDef.getType();
      return (T) obtainWrapped(field, dataTemplateClass, GetMode.STRICT);
    }

    return (T) obtainDirect(field, fieldDef.getType(), GetMode.STRICT);
  }

  /**
   * Set the value of a field.
   *
   * @param fieldDef specifies the field to set.
   * @param value provides the value to set.
   * @param <T> provides the type of the value.
   */
  @SuppressWarnings({"unchecked"})
  public final <T> void setValue(FieldDef<T> fieldDef, T value)
  {
    RecordDataSchema.Field field = _fieldMap.get(fieldDef);
    if (DataTemplate.class.isAssignableFrom(fieldDef.getType()))
    {
      putWrapped(field, (Class<DataTemplate<?>>) fieldDef.getType(), (DataTemplate<?>) value);
    }
    else
    {
      putDirect(field, (Class<Object>) fieldDef.getType(), value);
    }
  }
}
