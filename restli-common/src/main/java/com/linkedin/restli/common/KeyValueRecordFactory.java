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

  private final Class<K> _keyClass;
  private final Class<? extends RecordTemplate> _keyKeyClass;
  private final Class<? extends RecordTemplate> _keyParamsClass;
  private final Map<String, CompoundKey.TypeInfo> _fieldTypes;
  private final Class<V> _valueClass;

  private final KeyType _keyType;

  RecordDataSchema.Field _keyField;
  RecordDataSchema.Field _paramsField;
  RecordDataSchema.Field _valueField;


  public KeyValueRecordFactory(final Class<K> keyClass,
                               final Class<? extends RecordTemplate> keyKeyClass,
                               final Class<? extends RecordTemplate> keyParamsClass,
                               final Map<String, CompoundKey.TypeInfo> fieldTypes,
                               final Class<V> valueClass)
  {
    _keyClass = keyClass;
    _keyKeyClass = keyKeyClass;
    _keyParamsClass = keyParamsClass;
    _valueClass = valueClass;
    _fieldTypes = fieldTypes;

    _keyType = getKeyType(keyClass);

    StringBuilder sb = new StringBuilder(10);

    switch (_keyType)
    {
      case PRIMITIVE:
        _keyField = new RecordDataSchema.Field(DataTemplateUtil.getSchema(_keyClass));
        _keyField.setName(KeyValueRecord.KEY_FIELD_NAME, sb);
        break;
      case COMPLEX:
        _keyField = new RecordDataSchema.Field(DataTemplateUtil.getSchema(_keyKeyClass));
        _keyField.setName(KeyValueRecord.KEY_FIELD_NAME, sb);
        _paramsField = new RecordDataSchema.Field(DataTemplateUtil.getSchema(_keyParamsClass));
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

    _valueField = new RecordDataSchema.Field(DataTemplateUtil.getSchema(_valueClass));
    _valueField.setName(KeyValueRecord.VALUE_FIELD_NAME, sb);
  }

  /**
   * @param keyClass the class of the key
   * @return the type of the {@code keyClass}
   */
  private static KeyType getKeyType(Class<?> keyClass)
  {
    if (CompoundKey.class.isAssignableFrom(keyClass))
    {
      return KeyType.COMPOUND;
    }
    if (keyClass.equals(ComplexResourceKey.class))
    {
      return KeyType.COMPLEX;
    }
    return KeyType.PRIMITIVE;
  }

  /**
   * Build a {@link KeyValueRecord}
   * @param key the key to be stored
   * @param value the value to be stored
   * @return a {@link KeyValueRecord} with the key {@code key} and value {@code value}
   */
  public KeyValueRecord<K, V> create(final K key, final V value)
  {
    final KeyValueRecord<K, V> keyValueRecord = new KeyValueRecord<K, V>();
    switch (_keyType)
    {
      case PRIMITIVE:
        keyValueRecord.setPrimitiveKey(_keyField, key, _keyClass);
        break;
      case COMPLEX:
        keyValueRecord.setComplexKey(_keyField, _paramsField, key, _keyKeyClass, _keyParamsClass);
        break;
      case COMPOUND:
        keyValueRecord.setCompoundKey(_keyField, key, _fieldTypes);
        break;
    }

    keyValueRecord.setValue(_valueField, value, _valueClass);

    return keyValueRecord;
  }

  private static enum KeyType
  {
    PRIMITIVE,
    COMPLEX,
    COMPOUND
  }
}
