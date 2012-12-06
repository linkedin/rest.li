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
import java.util.List;
import java.util.Map;


/**
 * A generic record template that generates a schema on construction, based on a given collection of field definitions.
 *
 * @author Eran Leshem
 */
public class DynamicRecordTemplate extends RecordTemplate
{

  /**
   * Construct a new {@link DynamicRecordTemplate}.
   *
   * @param name provides the name of the record type.
   * @param fieldDefs defines the fields of the record type.
   * @param data provides the underlying data that backs this {@link DynamicRecordTemplate}.
   * @deprecated {@link RecordDataSchema} should be computed in builders and passed, rather than creating
   * it on the fly.
   */
  @Deprecated
  public DynamicRecordTemplate(String name, Collection<? extends FieldDef<?>> fieldDefs, DataMap data)
  {
    super(data, DynamicRecordMetadata.buildSchema(name, fieldDefs));
  }

  /**
   * Construct a new {@link DynamicRecordTemplate}.
   *
   * @param data provides the underlying data that backs this {@link DynamicRecordTemplate}.
   * @param schema the schema for the {@link DynamicRecordTemplate}.
   */
  public DynamicRecordTemplate(DataMap data, RecordDataSchema schema)
  {
    super(data, schema);
  }

  /**
   * Construct a new empty {@link DynamicRecordTemplate}.
   *
   * @param name provides the name of the record type.
   * @param fieldDefs defines the fields of the record type.
   * @deprecated {@link RecordDataSchema} should be computed in builders and passed, rather than creating
   * it on the fly.
   */
  @Deprecated
  public DynamicRecordTemplate(String name, Collection<? extends FieldDef<?>> fieldDefs)
  {
    this(name, fieldDefs, new DataMap());
  }

  /**
   * Construct a new {@link DynamicRecordTemplate}
   *
   * @param schema the schema for the {@link DynamicRecordTemplate}.
   */
  public DynamicRecordTemplate(RecordDataSchema schema)
  {
    super(new DataMap(), schema);
  }

  /**
   * Construct a new {@link DynamicRecordTemplate} initialized with the provided field values.
   *
   * @param name provides the name of the record type.
   * @param fieldDefValues defines the fields of the record type and the value of each field.
   * @deprecated {@link RecordDataSchema} should be computed in builders and passed, rather than creating
   * it on the fly.
   */
  @Deprecated
  @SuppressWarnings({"unchecked"})
  public DynamicRecordTemplate(String name, Map<? extends FieldDef<?>, Object> fieldDefValues)
  {
    this(name, fieldDefValues.keySet());

    for (Map.Entry<? extends FieldDef<?>, Object> entry: fieldDefValues.entrySet())
    {
      setValue((FieldDef<Object>) entry.getKey(), entry.getValue());
    }
  }

  /**
   * Construct a new {@link DynamicRecordTemplate} initialized with the provided field values.
   *
   * @param schema provides the schema for the record type.
   * @param fieldDefValues defines the fields of the record type.
   */
  @SuppressWarnings({"unchecked"})
  public DynamicRecordTemplate(RecordDataSchema schema, Map<FieldDef<?>, Object> fieldDefValues)
  {
    this(schema);

    for (Map.Entry<? extends FieldDef<?>, Object> entry: fieldDefValues.entrySet())
    {
      setValue((FieldDef<Object>) entry.getKey(), entry.getValue());
    }

  }

  /**
   * Get value of a field.
   *
   * @param fieldDef specifies the field to get the value of.
   * @param <T> provides the expected return type.
   * @return the value of the field, or null if the field is not present, see {@link GetMode#STRICT}.
   * @throws IllegalArgumentException if the given field is not a field of this {@link DynamicRecordTemplate}
   */
  @SuppressWarnings({"unchecked"})
  public <T> T getValue(FieldDef<T> fieldDef)
  {
    RecordDataSchema.Field field = fieldDef.getField();

    if (!fieldDefInRecord(fieldDef))
    {
      throw new IllegalArgumentException("Field " + fieldDef.getName() + " is not a field belonging to the schema of this DynamicRecordTemplate.");
    }

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
   * @throws IllegalArgumentException if the given field is not a field of this {@link DynamicRecordTemplate}
   */
  @SuppressWarnings({"unchecked"})
  public final <T> void setValue(FieldDef<T> fieldDef, T value)
  {
    RecordDataSchema.Field field = fieldDef.getField();

    if (!fieldDefInRecord(fieldDef))
    {
      throw new IllegalArgumentException("Field " + fieldDef.getName() + " is not a field belonging to the schema of this DynamicRecordTemplate.");
    }


    if (DataTemplate.class.isAssignableFrom(fieldDef.getType()))
    {
      putWrapped(field, (Class<DataTemplate<?>>) fieldDef.getType(), (DataTemplate<?>) value);
    }
    else
    {
      Class<?> dataClass = fieldDef.getDataClass();
      putDirect(field, (Class<Object>) fieldDef.getType(), dataClass, value, SetMode.DISALLOW_NULL);
    }
  }

  /**
   * Check if this {@link FieldDef} belongs to the {@link RecordDataSchema} in this
   * {@link DynamicRecordTemplate}.
   *
   * If this fails, it is because the FieldDef passed in was not the same fieldDef used to create
   * the RecordDataSchema.
   *
   * @param fieldDef provides the {@link FieldDef} to check.
   * @return true if the provided fieldDef belongs to the schema of this {@link DynamicRecordTemplate}, false otherwise.
   */
  private boolean fieldDefInRecord(FieldDef<?> fieldDef)
  {
    return fieldDef.getField().getRecord() == this.schema();
  }
}
