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

package com.linkedin.data.avro;


import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.message.Message;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaConstants;
import com.linkedin.data.schema.DataSchemaTraverse;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.FixedDataSchema;
import com.linkedin.data.schema.JsonBuilder;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.schema.SchemaParserFactory;
import com.linkedin.data.schema.UnionDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.apache.avro.Schema;

import static com.linkedin.data.schema.DataSchemaConstants.NULL_DATA_SCHEMA;
import static com.linkedin.data.schema.UnionDataSchema.avroUnionMemberKey;

/**
 * Translates Avro {@link Schema} to and from Pegasus {@link DataSchema}.
 */
public class SchemaTranslator
{
  private SchemaTranslator()
  {
  }

  /**
   * Translate an Avro {@link Schema} to a {@link DataSchema}.
   *
   * This method translates union with null record fields in Avro {@link Schema}
   * to optional fields in {@link DataSchema}. Record fields
   * whose type is a union with null will be translated to a new type, and the field becomes optional.
   * If the Avro union has two types (one of them is the null type), then the new type of the
   * field is the non-null member type of the union. If the Avro union does not have two types
   * (one of them is the null type) then the new type of the field is a union type with the null type
   * removed from the original union.
   *
   * This method also translates default values. If the field's type is a union with null
   * and has a default value, then this method also translates the default value of the field
   * to comply with the new type of the field. If the default value is null,
   * then remove the default value. If new type is not a union and the default value
   * is of the non-null member type, then assign the default value to the
   * non-null value within the union value (i.e. the value of the only entry within the
   * JSON object.) If the new type is a union and the default value is of the
   * non-null member type, then assign the default value to a JSON object
   * containing a single entry with the key being the member type discriminator of
   * the first union member and the value being the actual member value.
   *
   * Both the schema and default value translation takes into account that default value
   * representation for Avro unions does not include the member type discriminator and
   * the type of the default value is always the 1st member of the union.
   *
   * @param avroSchema provides the Avro {@link Schema}.
   * @return the translated {@link DataSchema}.
   * @throws IllegalArgumentException if the Avro {@link Schema} cannot be translated.
   */
  public static DataSchema avroToDataSchema(Schema avroSchema) throws IllegalArgumentException
  {
    String avroJson = avroSchema.toString();
    SchemaParser parser = SchemaParserFactory.instance().create(null);
    parser.getValidationOptions().setAvroUnionMode(true);
    parser.parse(new ByteArrayInputStream(avroJson.getBytes()));
    if (parser.hasError())
    {
      throw new IllegalArgumentException(parser.errorMessage());
    }
    assert(parser.topLevelDataSchemas().size() == 1);
    DataSchema dataSchema = parser.topLevelDataSchemas().get(0);
    DataSchemaTraverse traverse = new DataSchemaTraverse();
    traverse.traverse(dataSchema, AvroToDataSchemaConvertCallback.INSTANCE);
    // convert default values
    traverse.traverse(dataSchema, DefaultAvroToDataConvertCallback.INSTANCE);
    // make sure it can round-trip
    String dataSchemaJson = dataSchema.toString();
    dataSchema = DataTemplateUtil.parseSchema(dataSchemaJson);
    return dataSchema;
  }

  /**
   * Translate from a {@link DataSchema} to an Avro {@link Schema}
   *
   * @see #dataToAvroSchemaJson(com.linkedin.data.schema.DataSchema, DataToAvroSchemaTranslationOptions)
   *
   * @param dataSchema provides the {@link DataSchema}.
   * @return the Avro {@link Schema}.
   */
  public static Schema dataToAvroSchema(DataSchema dataSchema)
  {
    String jsonAvroSchema = dataToAvroSchemaJson(dataSchema, new DataToAvroSchemaTranslationOptions());
    // Avro Schema parser does not validate default values !!!
    return Schema.parse(jsonAvroSchema);
  }

  /**
   * Translate from a {@link DataSchema} to an Avro {@link Schema}
   *
   * @see #dataToAvroSchemaJson(com.linkedin.data.schema.DataSchema, DataToAvroSchemaTranslationOptions)
   *
   * @param dataSchema provides the {@link DataSchema}.
   * @param options provides the {@link DataToAvroSchemaTranslationOptions}.
   * @return the Avro {@link Schema}.
   */
  public static Schema dataToAvroSchema(DataSchema dataSchema, DataToAvroSchemaTranslationOptions options)
  {
    String jsonAvroSchema = dataToAvroSchemaJson(dataSchema, options);
    // Avro Schema parser does not validate default values !!!
    return Schema.parse(jsonAvroSchema);
  }

  /**
   * Translate from a {@link DataSchema} to an Avro {@link Schema}
   *
   * @see #dataToAvroSchemaJson(com.linkedin.data.schema.DataSchema, DataToAvroSchemaTranslationOptions)
   *
   * @param dataSchema provides the {@link DataSchema}.
   * @return the JSON representation of the Avro {@link Schema}.
   */
  public static String dataToAvroSchemaJson(DataSchema dataSchema)
  {
    return dataToAvroSchemaJson(dataSchema, new DataToAvroSchemaTranslationOptions());
  }

  /**
   * Translate from a {@link DataSchema} to an Avro {@link Schema}
   *
   * This method is deprecated, use {@link #dataToAvroSchemaJson(com.linkedin.data.schema.DataSchema, DataToAvroSchemaTranslationOptions)}
   * instead.
   *
   * @param dataSchema provides the {@link DataSchema}.
   * @param pretty provides how the JSON output will be formatted.
   * @return the JSON representation of the Avro {@link Schema}.
   */
  @Deprecated
  public static String dataToAvroSchemaJson(DataSchema dataSchema, JsonBuilder.Pretty pretty)
  {
    return dataToAvroSchemaJson(dataSchema, new DataToAvroSchemaTranslationOptions(pretty));
  }

  /**
   * Translate from a {@link DataSchema} to an Avro {@link Schema}
   *
   * This method translates optional fields in the {@link DataSchema} to union with null
   * fields in Avro {@link Schema}. Record fields with optional attribute set to true will
   * be translated to a union type that has null member type. If field's type is not
   * a union, then the new union type will be a union of the field's type and the null type.
   * If the field's type is already a union, the new union type contains all the
   * union's member types and the null type.
   *
   * This method also translates or sets the default value for optional fields in
   * the {@link DataSchema}. If the optional field does not have a default value,
   * set the translated default value to null. {@link OptionalDefaultMode}
   * specifies how an optional field with a default value is translated.
   *
   * Both the schema and default value translation takes into account that default value
   * representation for Avro unions does not include the member type discriminator and
   * the type of the default value is always the 1st member type of the union. Schema translation
   * fails by throwing an {@link IllegalArgumentException} if the default value's type
   * is not the same as the 1st member type of the union.
   *
   * @param dataSchema provides the {@link DataSchema}.
   * @param options specifies the {@link DataToAvroSchemaTranslationOptions}.
   * @return the JSON representation of the Avro {@link Schema}.
   * @throws IllegalArgumentException if the {@link DataSchema} cannot be translated.
   */
  public static String dataToAvroSchemaJson(DataSchema dataSchema, DataToAvroSchemaTranslationOptions options) throws IllegalArgumentException
  {
    // convert default values
    DataSchemaTraverse traverse = new DataSchemaTraverse();
    DefaultDataToAvroConvertCallback defaultConverter = new DefaultDataToAvroConvertCallback(options);
    traverse.traverse(dataSchema, defaultConverter);
    // convert schema
    String schemaJson = SchemaToAvroJsonEncoder.schemaToAvro(dataSchema, defaultConverter._fieldDefaultValues, options);
    return schemaJson;
  }

  private abstract static class AbstractDefaultDataTranslator
  {
    protected abstract Object translateField(List<Object> path, Object fieldValue, RecordDataSchema.Field field);
    protected abstract Object translateUnion(List<Object> path, Object value, UnionDataSchema unionDataSchema);

    protected Object translate(List<Object> path, Object value, DataSchema dataSchema)
    {
      dataSchema = dataSchema.getDereferencedDataSchema();
      DataSchema.Type type = dataSchema.getType();
      Object result;
      switch (type)
      {
        case NULL:
          if (value != Data.NULL)
          {
            throw new IllegalArgumentException(message(path, "value must be null for null schema"));
          }
          result = value;
          break;
        case BOOLEAN:
          result = ((Boolean) value).booleanValue();
          break;
        case INT:
          result = ((Number) value).intValue();
          break;
        case LONG:
          result = ((Number) value).longValue();
          break;
        case FLOAT:
          result = ((Number) value).floatValue();
          break;
        case DOUBLE:
          result = ((Number) value).doubleValue();
          break;
        case STRING:
          result = (String) value;
          break;
        case BYTES:
          Class<?> clazz = value.getClass();
          if (clazz != String.class && clazz != ByteString.class)
          {
            throw new IllegalArgumentException(message(path, "bytes value %1$s is not a String or ByteString", value));
          }
          result = value;
          break;
        case ENUM:
          String enumValue = (String) value;
          EnumDataSchema enumDataSchema = (EnumDataSchema) dataSchema;
          if (enumDataSchema.getSymbols().contains(enumValue) == false)
          {
            throw new IllegalArgumentException(message(path, "enum value %1$s not one of %2$s", value, enumDataSchema.getSymbols()));
          }
          result = value;
          break;
        case FIXED:
          clazz = value.getClass();
          ByteString byteString;
          if (clazz == String.class)
          {
            byteString = ByteString.copyAvroString((String) value, true);
          }
          else if (clazz == ByteString.class)
          {
            byteString = (ByteString) value;
          }
          else
          {
            throw new IllegalArgumentException(message(path, "fixed value %1$s is not a String or ByteString", value));
          }
          FixedDataSchema fixedDataSchema = (FixedDataSchema) dataSchema;
          if (fixedDataSchema.getSize() != byteString.length())
          {
            throw new IllegalArgumentException(message(path,
                                                       "ByteString size %1$d != FixedDataSchema size %2$d",
                                                       byteString.length(),
                                                       fixedDataSchema.getSize()));
          }
          result = byteString;
          break;
        case MAP:
          DataMap map = (DataMap) value;
          DataSchema valueDataSchema = ((MapDataSchema) dataSchema).getValues();
          Map<String, Object> resultMap = new DataMap(map.size() * 2);
          for (Map.Entry<String, Object> entry : map.entrySet())
          {
            String key = entry.getKey();
            path.add(key);
            Object entryAvroValue = translate(path, entry.getValue(), valueDataSchema);
            path.remove(path.size() - 1);
            resultMap.put(key, entryAvroValue);
          }
          result = resultMap;
          break;
        case ARRAY:
          DataList list = (DataList) value;
          DataList resultList = new DataList(list.size());
          DataSchema elementDataSchema = ((ArrayDataSchema) dataSchema).getItems();
          for (int i = 0; i < list.size(); i++)
          {
            path.add(i);
            Object entryAvroValue = translate(path, list.get(i), elementDataSchema);
            path.remove(path.size() - 1);
            resultList.add(entryAvroValue);
          }
          result = resultList;
          break;
        case RECORD:
          DataMap recordMap = (DataMap) value;
          RecordDataSchema recordDataSchema = (RecordDataSchema) dataSchema;
          DataMap resultRecordMap = new DataMap(recordDataSchema.getFields().size() * 2);
          for (RecordDataSchema.Field field : recordDataSchema.getFields())
          {
            String fieldName = field.getName();
            Object fieldValue = recordMap.get(fieldName);
            path.add(fieldName);
            Object resultFieldValue = translateField(path, fieldValue, field);
            path.remove(path.size() - 1);
            if (resultFieldValue != null)
            {
              resultRecordMap.put(fieldName, resultFieldValue);
            }
          }
          result = resultRecordMap;
          break;
        case UNION:
          result = translateUnion(path, value, (UnionDataSchema) dataSchema);
          break;
        default:
          throw new IllegalStateException(message(path, "schema type unknown %1$s", type));
      }
      return result;
    }
  }

  /**
   * Translate values from {@link DataSchema} format to Avro {@link Schema} format.
   *
   * The translated values retains the union member type discriminator for default values.
   * The Avro JSON schema encoder {@link SchemaToAvroJsonEncoder} needs to know
   * the default value type in order to make this type the 1st member type of the
   * Avro union.
   *
   * The output of this translator is a map of fields to translated default values.
   */
  private static class DefaultDataToAvroConvertCallback extends AbstractDefaultDataTranslator implements DataSchemaTraverse.Callback
  {
    private IdentityHashMap<RecordDataSchema.Field, Object> _fieldDefaultValues = new IdentityHashMap<RecordDataSchema.Field, Object>();
    private final DataToAvroSchemaTranslationOptions _options;

    private DefaultDataToAvroConvertCallback(DataToAvroSchemaTranslationOptions options)
    {
      _options = options;
    }

    protected void addDefaultValue(RecordDataSchema.Field field, Object value)
    {
      Object existingValue = _fieldDefaultValues.put(field, value);
      assert(existingValue == null);
    }

    @Override
    public void callback(List<String> path, DataSchema schema)
    {
      if (schema.getType() != DataSchema.Type.RECORD)
      {
        return;
      }
      RecordDataSchema recordSchema = (RecordDataSchema) schema;
      for (RecordDataSchema.Field field : recordSchema.getFields())
      {
        Object defaultData = field.getDefault();
        if (defaultData != null)
        {
          path.add(DataSchemaConstants.DEFAULT_KEY);
          Object newDefault = translateField(pathList(path), defaultData, field);
          addDefaultValue(field, newDefault);
          path.remove(path.size() - 1);
        }
        else if (field.getOptional())
        {
          // no default specified and optional
          addDefaultValue(field, Data.NULL);
        }
      }
    }

    @Override
    protected Object translateUnion(List<Object> path, Object value, UnionDataSchema unionDataSchema)
    {
      String key;
      Object memberValue;
      if (value == Data.NULL)
      {
        key = DataSchemaConstants.NULL_TYPE;
        memberValue = Data.NULL;
      }
      else
      {
        DataMap unionMap = (DataMap) value;
        if (unionMap.size() != 1)
        {
          throw new IllegalArgumentException(message(path, "union value $1%s has more than one entry", value));
        }
        Map.Entry<String, Object> entry = unionMap.entrySet().iterator().next();
        key = entry.getKey();
        memberValue = entry.getValue();
      }
      DataSchema memberDataSchema = unionDataSchema.getType(key);
      if (memberDataSchema == null)
      {
        throw new IllegalArgumentException(message(path, "union value %1$s has invalid member key %2$s", value, key));
      }
      if (memberDataSchema != unionDataSchema.getTypes().get(0))
      {
        throw new IllegalArgumentException(
          message(path,
                  "cannot translate union value %1$s because it's type is not the 1st member type of the union %2$s",
                  value, unionDataSchema));
      }
      path.add(key);
      Object resultMemberValue = translate(path, memberValue, memberDataSchema);
      path.remove(path.size() - 1);
      return resultMemberValue;
    }

    @Override
    protected Object translateField(List<Object> path, Object fieldValue, RecordDataSchema.Field field)
    {
      DataSchema fieldDataSchema = field.getType();
      boolean isOptional = field.getOptional();
      if (isOptional)
      {
        if (fieldDataSchema.getType() != DataSchema.Type.UNION)
        {
          if (fieldValue == null)
          {
            if (_options.getOptionalDefaultMode() != OptionalDefaultMode.TRANSLATE_TO_NULL &&
                field.getDefault() != null)
            {
              throw new IllegalArgumentException(
                message(path,
                        "cannot translate absent optional field (to have null value) because this field is optional and has a default value"));
            }
            fieldValue = Data.NULL;
            fieldDataSchema = DataSchemaConstants.NULL_DATA_SCHEMA;
          }
          else
          {
            if (_options.getOptionalDefaultMode() == OptionalDefaultMode.TRANSLATE_TO_NULL)
            {
              fieldValue = Data.NULL;
              fieldDataSchema = DataSchemaConstants.NULL_DATA_SCHEMA;
            }
            else
            {
              // Avro schema should be union with 2 types: null and the field's type
              // default value is field's type
            }
          }
        }
        else
        {
          // already a union
          if (fieldValue == null)
          {
            // field is not present
            if (_options.getOptionalDefaultMode() != OptionalDefaultMode.TRANSLATE_TO_NULL)
            {
              Object fieldDefault = field.getDefault();
              if (fieldDefault != null || fieldDefault != Data.NULL)
              {
                throw new IllegalArgumentException(
                  message(path,
                          "cannot translate absent optional field (to have null value) or field with non-null union value because this field is optional and has a non-null default value"));
              }
            }
            fieldValue = Data.NULL;
            fieldDataSchema = DataSchemaConstants.NULL_DATA_SCHEMA;
          }
          else
          {
            // field has value
            if (_options.getOptionalDefaultMode() == OptionalDefaultMode.TRANSLATE_TO_NULL)
            {
              fieldValue = Data.NULL;
              fieldDataSchema = DataSchemaConstants.NULL_DATA_SCHEMA;
            }
          }
        }
        assert(_options.getOptionalDefaultMode() != OptionalDefaultMode.TRANSLATE_TO_NULL ||
               fieldValue == Data.NULL);
      }
      Object resultFieldValue = translate(path, fieldValue, fieldDataSchema);
      return resultFieldValue;
    }
  }

  private static class DefaultAvroToDataConvertCallback extends AbstractDefaultDataTranslator implements DataSchemaTraverse.Callback
  {
    private static final DefaultAvroToDataConvertCallback INSTANCE = new DefaultAvroToDataConvertCallback();

    private DefaultAvroToDataConvertCallback()
    {
    }

    @Override
    public void callback(List<String> path, DataSchema schema)
    {
      if (schema.getType() != DataSchema.Type.RECORD)
      {
        return;
      }
      RecordDataSchema recordSchema = (RecordDataSchema) schema;
      for (RecordDataSchema.Field field : recordSchema.getFields())
      {
        Object defaultData = field.getDefault();
        if (defaultData != null)
        {
          path.add(DataSchemaConstants.DEFAULT_KEY);
          Object newDefault = translateField(pathList(path), defaultData, field);
          path.remove(path.size() - 1);
          field.setDefault(newDefault);
        }
      }
    }

    private static final DataMap unionDefaultValue(DataSchema schema, Object value)
    {
      DataMap dataMap = new DataMap(2);
      dataMap.put(avroUnionMemberKey(schema), value);
      return dataMap;
    }

    @Override
    protected Object translateUnion(List<Object> path, Object value, UnionDataSchema unionDataSchema)
    {
      Object result;
      if (value == Data.NULL)
      {
        result = value;
      }
      else
      {
        // member type is always the 1st member of the union.
        DataSchema memberSchema = unionDataSchema.getTypes().get(0);
        result = unionDefaultValue(memberSchema, value);
        path.add(avroUnionMemberKey(memberSchema));
        translate(path, value, memberSchema);
        path.remove(path.size() - 1);
      }
      return result;
    }

    @Override
    protected Object translateField(List<Object> path, Object fieldValue, RecordDataSchema.Field field)
    {
      DataSchema fieldDataSchema = field.getType();
      boolean isOptional = field.getOptional();
      Object result;
      if (isOptional && fieldValue == Data.NULL)
      {
        // for optional fields,
        // null union members have been removed from translated union schema
        // default value of null should also be removed, make it so that there is no default

        result = null;
      }
      else
      {
        result = translate(path, fieldValue, fieldDataSchema);
      }

      return result;
    }
  }

  private static class AvroToDataSchemaConvertCallback implements DataSchemaTraverse.Callback
  {
    private static final AvroToDataSchemaConvertCallback INSTANCE = new AvroToDataSchemaConvertCallback();

    private AvroToDataSchemaConvertCallback()
    {
    }

    @Override
    public void callback(List<String> path, DataSchema schema)
    {
      if (schema.getType() != DataSchema.Type.RECORD)
      {
        return;
      }
      RecordDataSchema recordSchema = (RecordDataSchema) schema;
      for (RecordDataSchema.Field field : recordSchema.getFields())
      {
        DataSchema fieldSchema = field.getType();
        // check if union
        boolean isUnion = fieldSchema.getDereferencedType() == DataSchema.Type.UNION;
        field.setOptional(false);
        if (isUnion) {
          UnionDataSchema unionSchema = (UnionDataSchema) fieldSchema;
          int nullIndex= unionSchema.index(NULL_DATA_SCHEMA.getUnionMemberKey());
          // check if union with null
          if (nullIndex != -1)
          {
            List<DataSchema> types = unionSchema.getTypes();
            if (types.size() == 2)
            {
              DataSchema newFieldSchema = unionSchema.getTypes().get((nullIndex + 1) % 2);
              field.setType(newFieldSchema);
            }
            else
            {
              ArrayList<DataSchema> newTypes = new ArrayList<DataSchema>(types);
              newTypes.remove(nullIndex);
              StringBuilder errorMessages = null; // not expecting errors
              unionSchema.setTypes(newTypes, errorMessages);
            }
            // set to optional
            field.setOptional(true);
          }
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  static final private List<Object> pathList(List<String> path)
  {
    return (List<Object>) ((List) path);
  }

  static final private String message(List<?> path, String format, Object... args)
  {
    Message message = new Message(path.toArray(), format, args);
    return message.toString();
  }
}
