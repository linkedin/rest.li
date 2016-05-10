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


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaUtil;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.GetMode;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.SetMode;
import com.linkedin.util.CustomTypeUtil;

import java.util.Map;

/**
 * Represents an entity in the form of a key-value pair where the key can be a primitive, a {@link CompoundKey}, or a
 * {@link ComplexResourceKey} and the value is a {@link RecordTemplate}
 *
 * @author kparikh
 */
public class KeyValueRecord<K, V extends RecordTemplate> extends RecordTemplate
{
  public static final String KEY_FIELD_NAME = "key";
  public static final String VALUE_FIELD_NAME = "value";
  public static final String PARAMS_FIELD_NAME = "$params";

  /**
   * Needed by {@link DataTemplateUtil#templateConstructor(Class)}
   * @param dataMap provides the contents of the key
   */
  public KeyValueRecord(DataMap dataMap)
  {
    super(dataMap, null);
  }

  /*package private*/KeyValueRecord()
  {
    super(new DataMap(), null);
  }

  /**
   * Sets a primitive key. If the key is a typeref the typeref is followed and the primitive value is stored.
   * @param keyField key field
   * @param key the primitive key to set
   * @param keyType the type of the key
   */
  @SuppressWarnings("unchecked")
  void setPrimitiveKey(RecordDataSchema.Field keyField, K key, TypeSpec<K> keyType)
  {
    DataSchema keySchema = keyType.getSchema();
    if (keySchema.isPrimitive())
    {
      putDirect(keyField, keyType.getType(), keyType.getType(), key, SetMode.IGNORE_NULL);
      return;
    }
    switch (keySchema.getType())
    {
      case TYPEREF:
        TyperefDataSchema typerefDataSchema = (TyperefDataSchema)keySchema;
        DataSchema.Type dereferencedType = keySchema.getDereferencedType();
        Class<?> javaClassForSchema = CustomTypeUtil.getJavaCustomTypeClassFromSchema(typerefDataSchema);
        if (javaClassForSchema == null)
        {
          // typeref to a primitive. In this case the keyClass is a primitive, and so is the key.
          putDirect(keyField, keyType.getType(), keyType.getType(), key, SetMode.IGNORE_NULL);
        }
        else
        {
          // typeref to a custom type. In this case the keyClass is the typeref class, but the class of the key
          // is the custom class.
          Class<?> keyDereferencedClass = DataSchemaUtil.dataSchemaTypeToPrimitiveDataSchemaClass(dereferencedType);
          putDirect(keyField, (Class<Object>) javaClassForSchema, keyDereferencedClass, key, SetMode.IGNORE_NULL);
        }
        break;
      case ENUM:
        putDirect(keyField, keyType.getType(), String.class, key, SetMode.IGNORE_NULL);
        break;
      default:
        throw new IllegalArgumentException("key is not a primitive, typeref, or an enum!");
    }
  }

  /**
   * Sets a {@link ComplexResourceKey}
   * @param keyField key field
   * @param paramsField params field
   * @param key the complex key to set
   * @param complexKeyType the type of the {@link ComplexResourceKey}
   * @param <KK> type of keyKeyClass
   * @param <KP> type of keyParamsClass
   */
  <KK extends RecordTemplate, KP extends RecordTemplate> void setComplexKey(RecordDataSchema.Field keyField,
                                                                            RecordDataSchema.Field paramsField,
                                                                            K key,
                                                                            ComplexKeySpec<KK, KP> complexKeyType)
  {
    if (!(key instanceof ComplexResourceKey))
    {
      throw new IllegalArgumentException("Key must be a ComplexResourceKey!");
    }
    @SuppressWarnings("unchecked")
    ComplexResourceKey<KK, KP> complexResourceKey = (ComplexResourceKey<KK, KP>)key;
    KK keyKey = complexResourceKey.getKey();
    KP keyParams = complexResourceKey.getParams();

    putWrapped(keyField, complexKeyType.getKeyType().getType(), keyKey, SetMode.IGNORE_NULL);
    putWrapped(paramsField, complexKeyType.getParamsType().getType(), keyParams, SetMode.IGNORE_NULL);
  }

  /**
   * Sets a {@link CompoundKey} key.
   * The "key" field in this object will hold a {@link DataMap}. The keys in this {@code DataMap} will be the key names
   * from the {@code compoundKey}. The values will be the values for these keys.
   *
   * @param keyField compoundKey field
   * @param key the key to set.
   */
  void setCompoundKey(RecordDataSchema.Field keyField, K key, Map<String, CompoundKey.TypeInfo> fieldTypes)
  {
    if (!(key instanceof CompoundKey))
    {
      throw new IllegalArgumentException("Key must be a CompoundKey!");
    }
    CompoundKey compoundKey = (CompoundKey)key;
    DataMap compoundKeyData = compoundKey.toDataMap(fieldTypes);
    putDirect(keyField, DataMap.class, DataMap.class, compoundKeyData, SetMode.IGNORE_NULL);
  }

  /**
   * Sets the value to be stored
   * @param valueField value field
   * @param value the value to set
   * @param valueClass the class of the {@code value}
   */
  public void setValue(RecordDataSchema.Field valueField, V value, Class<V> valueClass)
  {
    putWrapped(valueField, valueClass, value, SetMode.IGNORE_NULL);
  }

  /**
   * Get the stored primitive key
   * @param keyClass class of the key
   * @return the stored key as an object of {@code keyClass}
   */
  public K getPrimitiveKey(Class<K> keyClass)
  {
    return getPrimitiveKey(TypeSpec.forClassMaybeNull(keyClass));
  }

  @SuppressWarnings("unchecked")
  public K getPrimitiveKey(TypeSpec<K> keyType)
  {
    StringBuilder sb = new StringBuilder(10);

    DataSchema keySchema = keyType.getSchema();

    RecordDataSchema.Field keyField = new RecordDataSchema.Field(keySchema);
    keyField.setName(KEY_FIELD_NAME, sb);

    if (keySchema.isPrimitive() || keySchema.getType() == DataSchema.Type.ENUM)
    {
      return obtainDirect(keyField, keyType.getType(), GetMode.DEFAULT);
    }
    else if (keySchema.getType() == DataSchema.Type.TYPEREF)
    {
      TyperefDataSchema typerefDataSchema = (TyperefDataSchema)keySchema;
      Class<?> javaClass = CustomTypeUtil.getJavaCustomTypeClassFromSchema(typerefDataSchema);
      if (javaClass == null)
      {
        // typeref to a primitive. keyClass is a primitive
        return obtainDirect(keyField, keyType.getType(), GetMode.DEFAULT);
      }
      else
      {
        // typeref to a custom type. javaClass is the custom type that the typeref refers to.
        return (K) obtainDirect(keyField, javaClass, GetMode.DEFAULT);
      }
    }
    else
    {
      throw new IllegalArgumentException("key is not a primitive, typeref, or an enum!");
    }
  }

  /**
   * Get the stored {@link ComplexResourceKey}
   * @param keyKeyClass the class of the key in the {@link ComplexResourceKey}
   * @param keyParamsClass the class of the params in the {@link ComplexResourceKey}
   * @param <KK> type of keyKeyClass
   * @param <KP> type of keyParamsClass
   * @return a key
   */
  public <KK extends RecordTemplate,
      KP extends RecordTemplate> ComplexResourceKey<KK, KP> getComplexKey(Class<KK> keyKeyClass,
                                                                          Class<KP> keyParamsClass)
  {
    return getComplexKey(ComplexKeySpec.forClassesMaybeNull(keyKeyClass, keyParamsClass));
  }

  /**
   * Get the stored {@link ComplexResourceKey}
   * @param complexKeyType the type of the {@link ComplexResourceKey}
   * @param <KK> type of keyKeyClass
   * @param <KP> type of keyParamsClass
   * @return a key
   */
  public <KK extends RecordTemplate,
      KP extends RecordTemplate> ComplexResourceKey<KK, KP> getComplexKey(ComplexKeySpec<KK, KP> complexKeyType)
  {
    StringBuilder sb = new StringBuilder(10);

    RecordDataSchema.Field keyField = new RecordDataSchema.Field(complexKeyType.getKeyType().getSchema());
    keyField.setName(KEY_FIELD_NAME, sb);
    RecordDataSchema.Field paramsField = new RecordDataSchema.Field(complexKeyType.getParamsType().getSchema());
    paramsField.setName(PARAMS_FIELD_NAME, sb);

    KK keyKey = obtainWrapped(keyField, complexKeyType.getKeyType().getType(), GetMode.DEFAULT);
    KP keyParams = obtainWrapped(paramsField, complexKeyType.getParamsType().getType(), GetMode.DEFAULT);

    return new ComplexResourceKey<KK, KP>(keyKey, keyParams);
  }

  /**
   * Gets the stored {@link CompoundKey}
   * @param fieldTypes mapping of key name to {@link CompoundKey.TypeInfo} for that key
   * @return the stored {@link CompoundKey}
   */
  public CompoundKey getCompoundKey(Map<String, CompoundKey.TypeInfo> fieldTypes)
  {
    StringBuilder sb = new StringBuilder(10);
    RecordDataSchema.Field keyField = new RecordDataSchema.Field(KeyValueRecordFactory.COMPOUND_KEY_SCHEMA);
    keyField.setName(KEY_FIELD_NAME, sb);

    DataMap compoundKeyData = obtainDirect(keyField, DataMap.class, GetMode.DEFAULT);

    if (compoundKeyData.size() != fieldTypes.size())
    {
      throw new IllegalArgumentException("Number of keys must be the same! Number of keys stored in the KeyValueRecord" +
                                             "is: " + compoundKeyData.size() + ". Number of keys in fieldTypes is: " + fieldTypes.size());
    }

    return CompoundKey.fromValues(compoundKeyData, fieldTypes);
  }

  public V getValue(Class<V> valueClass)
  {
    return getValue(new TypeSpec<V>(valueClass));
  }

  /**
   * Get the stored value
   * @param valueTypeSpec the expected class of the stored value
   * @return the stored value as an object of {@code valueClass}
   */
  public V getValue(TypeSpec<V> valueTypeSpec)
  {
    StringBuilder sb = new StringBuilder(10);

    RecordDataSchema.Field valueField = new RecordDataSchema.Field(valueTypeSpec.getSchema());
    valueField.setName(VALUE_FIELD_NAME, sb);

    return obtainWrapped(valueField, valueTypeSpec.getType(), GetMode.DEFAULT);
  }
}
