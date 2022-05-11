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


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.collections.CheckedUtil;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaUtil;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.TyperefDataSchema;

import java.lang.reflect.Array;
import java.util.Arrays;
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
   * @param data provides the underlying data that backs this {@link DynamicRecordTemplate}.
   * @param schema the schema for the {@link DynamicRecordTemplate}.
   */
  public DynamicRecordTemplate(DataMap data, RecordDataSchema schema)
  {
    super(data, schema);
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
    if (fieldDef == null){
      return null;
    }
    RecordDataSchema.Field field = fieldDef.getField();

    if (!fieldDefInRecord(fieldDef))
    {
      throw new IllegalArgumentException("Field " + fieldDef.getName() + " is not a field belonging to the schema of this DynamicRecordTemplate.");
    }

    if (fieldDef.getType().isArray())
    {
      return obtainArray(field, fieldDef);
    }
    else if (DataTemplate.class.isAssignableFrom(fieldDef.getType()))
    {
      return (T) obtainWrapped(field, (Class<DataTemplate<?>>) fieldDef.getType(), GetMode.STRICT);
    }
    else
    {
      return (T) obtainDirect(field, fieldDef.getType(), GetMode.STRICT);
    }
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
    if (fieldDef == null){
      return;
    }
    RecordDataSchema.Field field = fieldDef.getField();

    if (!fieldDefInRecord(fieldDef))
    {
      throw new IllegalArgumentException("Field " +
          fieldDef.getName() +
          " is not a field belonging to the schema of this DynamicRecordTemplate.");
    }

    if (fieldDef.getType().isArray())
    {
      putArray(field, fieldDef, value);
    }
    else if (DataTemplate.class.isAssignableFrom(fieldDef.getType()))
    {
      unsafePutWrapped(field, (Class<DataTemplate<?>>) fieldDef.getType(), (DataTemplate<?>)value, SetMode.DISALLOW_NULL);
    }
    else
    {
      unsafePutDirect(field,
          (Class<Object>) fieldDef.getType(),
          fieldDef.getDataClass(),
          value,
          fieldDef.getField().getOptional()? SetMode.IGNORE_NULL : SetMode.DISALLOW_NULL);
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

  /**
   * Obtains an array value with items wrapped appropriately.
   * @param field specifies the field to get the value of.
   * @param fieldDef specifies the field definition to get the value of.
   * @param <T> provides the type of the value.
   * @return the value of the field, or null if the field is not present.
   */
  @SuppressWarnings({"unchecked"})
  private <T> T obtainArray(RecordDataSchema.Field field, FieldDef<T> fieldDef)
  {
    final Class<?> itemType = fieldDef.getType().getComponentType();
    final DataList itemsList = obtainDirect(field, DataList.class, GetMode.STRICT);
    final Object convertedValue = Array.newInstance(itemType, itemsList.size());
    boolean isDataTemplate = DataTemplate.class.isAssignableFrom(itemType);
    int j = 0;

    for (Object item: itemsList)
    {
      Object itemsElem = null;
      if (isDataTemplate)
      {
        itemsElem = DataTemplateUtil.wrap(item, itemType.asSubclass(DataTemplate.class));
      }
      else
      {
        itemsElem = DataTemplateUtil.coerceOutput(item, itemType);
      }

      Array.set(convertedValue, j++, itemsElem);
    }

    return (T) convertedValue;
  }

  /**
   * Puts an array field value by doing the necessary unwrapping at the items level.
   * @param field specifies the field to put the value for.
   * @param fieldDef specifies the field definition to put the value for.
   * @param value provides the value to put for the specified field.
   * @param <T> provides the type of the value.
   */
  @SuppressWarnings({"unchecked"})
  private <T> void putArray(RecordDataSchema.Field field, FieldDef<T> fieldDef, T value)
  {
    Class<?> itemType = null;
    ArrayDataSchema arrayDataSchema = null;
    if (fieldDef.getDataSchema() instanceof ArrayDataSchema)
    {
      arrayDataSchema = (ArrayDataSchema)fieldDef.getDataSchema();
      DataSchema itemSchema = arrayDataSchema.getItems();

      if (itemSchema instanceof TyperefDataSchema)
      {
        itemType = DataSchemaUtil.dataSchemaTypeToPrimitiveDataSchemaClass(
            itemSchema.getDereferencedType());
      }
      else
      {
        itemType = fieldDef.getType().getComponentType();
      }
    }
    else
    {
      throw new IllegalArgumentException(
          "Field " + fieldDef.getName() +
              " does not have an array schema; although the data is an array.");
    }

    boolean isDataTemplate = DataTemplate.class.isAssignableFrom(itemType);
    List<Object> items;

    if (value instanceof DataList)
    {
      items = (List<Object>) value;
    }
    else
    {
      items = Arrays.asList((Object[]) value);
    }

    DataList data = new DataList(items.size());

    for (Object item: items)
    {
      if (isDataTemplate)
      {
        Object itemData;

        if (item instanceof DataMap)
        {
          itemData = item;
        }
        else
        {
          itemData = ((DataTemplate<?>) item).data();
        }

        data.add(itemData);
      }
      else
      {
        data.add(
            DataTemplateUtil.coerceInput(item,
                (Class<Object>)item.getClass(),
                itemType.isEnum() ? String.class : itemType));
      }
    }

    CheckedUtil.putWithoutChecking(_map, field.getName(), data);
  }
}
