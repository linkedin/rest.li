/*
   Copyright (c) 2013 LinkedIn Corp.

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

package com.linkedin.restli.common;


import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import java.util.Map;


/**
 *
 * @author kparikh
 */
public class KeyValueRecordFactory<K, V extends RecordTemplate>
{
  // A CompoundKey is being stored as a DataMap in the KeyValueRecord.
  private static final String COMPOUND_KEY_SCHEMA_STRING = "{\n" +
      "  \"type\": \"record\",\n" +
      "  \"name\": \"CompoundKeySchema\",\n" +
      "  \"namespace\": \"com.linkedin.restli.common\",\n" +
      "  \"fields\": []\n" +
      "}";
  static final DataSchema COMPOUND_KEY_SCHEMA = DataTemplateUtil.parseSchema(COMPOUND_KEY_SCHEMA_STRING);

  private final TypeSpec<K> _keyType;
  private final ComplexKeySpec<?, ?> _complexKeyType;
  private final Map<String, CompoundKey.TypeInfo> _fieldTypes;
  private final TypeSpec<V> _valueType;

  private final ResourceKeyType _resourceKeyType;

  RecordDataSchema.Field _keyField;
  RecordDataSchema.Field _paramsField;
  RecordDataSchema.Field _valueField;

  public KeyValueRecordFactory(final Class<K> keyClass,
                               final Class<? extends RecordTemplate> keyKeyClass,
                               final Class<? extends RecordTemplate> keyParamsClass,
                               final Map<String, CompoundKey.TypeInfo> fieldTypes,
                               final Class<V> valueClass)
  {
    this(TypeSpec.forClassMaybeNull(keyClass),
         ComplexKeySpec.forClassesMaybeNull(keyKeyClass, keyParamsClass),
         fieldTypes,
         TypeSpec.forClassMaybeNull(valueClass));
  }

  public KeyValueRecordFactory(final TypeSpec<K> keyType,
                               final ComplexKeySpec<?, ?> complexKeyType,
                               final Map<String, CompoundKey.TypeInfo> fieldTypes,
                               final TypeSpec<V> valueType)
  {
    _keyType = keyType;
    _complexKeyType = complexKeyType;
    _valueType = valueType;
    _fieldTypes = fieldTypes;
    _resourceKeyType = getResourceKeyType(_keyType.getType());

    StringBuilder sb = new StringBuilder(10);

    switch (_resourceKeyType)
    {
      case PRIMITIVE:
        _keyField = new RecordDataSchema.Field(_keyType.getSchema());
        _keyField.setName(KeyValueRecord.KEY_FIELD_NAME, sb);
        break;
      case COMPLEX:
        _keyField = new RecordDataSchema.Field(_complexKeyType.getKeyType().getSchema());
        _keyField.setName(KeyValueRecord.KEY_FIELD_NAME, sb);
        _paramsField = new RecordDataSchema.Field(_complexKeyType.getParamsType().getSchema());
        _paramsField.setName(KeyValueRecord.PARAMS_FIELD_NAME, sb);
        break;
      case COMPOUND:
        if (_fieldTypes == null)
        {
          throw new IllegalArgumentException("Must specify field types for a CompoundKey!");
        }
        _keyField = new RecordDataSchema.Field(COMPOUND_KEY_SCHEMA);
        _keyField.setName(KeyValueRecord.KEY_FIELD_NAME, sb);
        break;
    }

    _valueField = new RecordDataSchema.Field(_valueType.getSchema());
    _valueField.setName(KeyValueRecord.VALUE_FIELD_NAME, sb);
  }

  private ResourceKeyType getResourceKeyType(Class<?> type)
  {
    if (CompoundKey.class.isAssignableFrom(type))
    {
      return ResourceKeyType.COMPOUND;
    }
    else if (type.equals(ComplexResourceKey.class))
    {
      return ResourceKeyType.COMPLEX;
    }
    else
    {
      return ResourceKeyType.PRIMITIVE;
    }
  }

  /**
   * Build a {@link KeyValueRecord}
   * @param key the key to be stored
   * @param value the value to be stored
   * @return a {@link KeyValueRecord} with the key {@code key} and value {@code value}
   */
  public KeyValueRecord<K, V> create(final K key, final V value)
  {
    final KeyValueRecord<K, V> keyValueRecord = new KeyValueRecord<>();

    switch (_resourceKeyType)
    {
      case PRIMITIVE:
        keyValueRecord.setPrimitiveKey(_keyField, key, _keyType);
        break;
      case COMPLEX:
        keyValueRecord.setComplexKey(_keyField, _paramsField, key, _complexKeyType);
        break;
      case COMPOUND:
        keyValueRecord.setCompoundKey(_keyField, key, _fieldTypes);
        break;
    }

    keyValueRecord.setValue(_valueField, value, _valueType.getType());

    return keyValueRecord;
  }

  private static enum ResourceKeyType
  {
    PRIMITIVE,
    COMPLEX,
    COMPOUND
  }
}
