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

package com.linkedin.restli.common;


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.WrappingMapTemplate;

import java.util.Arrays;
import java.util.Map;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class BatchRequest<T extends RecordTemplate> extends RecordTemplate
{
  public static final String ENTITIES = "entities";

  private final TypeSpec<T> _valueType;
  private final MapDataSchema _entitiesSchema;
  private final RecordDataSchema.Field _entitiesField;

  private final RecordDataSchema _schema;
  private static final Name _BATCH_REQUEST_NAME = new Name(BatchRequest.class.getSimpleName());

  private static class DynamicRecordMap<R extends RecordTemplate> extends WrappingMapTemplate<R>
  {
    public DynamicRecordMap(DataMap map, MapDataSchema mapSchema, Class<R> valueClass)
    {
      super(map, mapSchema, valueClass);
    }
  }

  /**
   * Initialize a BatchRequest off of the given data and valueClass.
   *
   * @param data a DataMap
   * @param valueClass the class of items that will be returned when this
   *                   request is fulfilled.
   */
  public BatchRequest(DataMap data, Class<T> valueClass)
  {
    this(data, new TypeSpec<>(valueClass));
  }

  private BatchRequest(DataMap data, Class<T> valueClass, int capacity)
  {
    this(data, new TypeSpec<>(valueClass), capacity);
  }

  /**
   * Initialize a BatchRequest off of the given data and valueClass.
   *
   * @param data a DataMap
   * @param valueType the class of items that will be returned when this
   *                   request is fulfilled.
   */
  public BatchRequest(DataMap data, TypeSpec<T> valueType)
  {
    this(data, valueType, 0);
  }

  private BatchRequest(DataMap data, TypeSpec<T> valueType, int capacity)
  {
    super(data, null);
    _valueType = valueType;
    StringBuilder errorMessageBuilder = new StringBuilder(10);
    Name elementSchemaName = new Name(valueType.getType().getSimpleName());
    _entitiesSchema = new MapDataSchema(new RecordDataSchema(elementSchemaName, RecordDataSchema.RecordType.RECORD));
    _entitiesField = new RecordDataSchema.Field(_entitiesSchema);
    _entitiesField.setName(ENTITIES, errorMessageBuilder);

    if (data().get(ENTITIES) == null)
    {
      data().put(ENTITIES, new DataMap(capacity));
    }

    _schema = new RecordDataSchema(_BATCH_REQUEST_NAME, RecordDataSchema.RecordType.RECORD);
    _schema.setFields(Arrays.asList(_entitiesField), errorMessageBuilder);

  }

  @Override
  public RecordDataSchema schema()
  {
    return _schema;
  }

  /**
   * return a mutable map of of keys to entities that can be added to as the
   * batch request is fulfilled.
   *
   * @return a Map from keys to entities
   */
  public Map<String, T> getEntities()
  {
    DataMap value = data().getDataMap(ENTITIES);

    return new DynamicRecordMap<>(value, _entitiesSchema, _valueType.getType());
  }
}
