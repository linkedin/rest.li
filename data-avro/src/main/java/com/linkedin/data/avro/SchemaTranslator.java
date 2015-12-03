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
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.DataSchemaTraverse;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.FixedDataSchema;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.schema.SchemaParserFactory;
import com.linkedin.data.schema.DataSchemaParser;
import com.linkedin.data.schema.UnionDataSchema;
import com.linkedin.data.schema.resolver.DefaultDataSchemaResolver;
import com.linkedin.data.schema.resolver.FileDataSchemaResolver;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.template.DataTemplateUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.data.schema.DataSchemaConstants.NULL_DATA_SCHEMA;
import static com.linkedin.data.schema.UnionDataSchema.avroUnionMemberKey;

/**
 * Translates Avro {@link Schema} to and from Pegasus {@link DataSchema}.
 */
public class SchemaTranslator
{
  private static final Logger log = LoggerFactory.getLogger(SchemaTranslator.class);

  public static final String DATA_PROPERTY = "com.linkedin.data";
  public static final String SCHEMA_PROPERTY = "schema";
  public static final String OPTIONAL_DEFAULT_MODE_PROPERTY = "optionalDefaultMode";
  public static final String AVRO_FILE_EXTENSION = ".avsc";

  private SchemaTranslator()
  {
  }

  /**
   * Translate an Avro {@link Schema} to a {@link DataSchema}.
   * <p>
   * If the translation mode is {@link AvroToDataSchemaTranslationMode#RETURN_EMBEDDED_SCHEMA}
   * and a {@link DataSchema} is embedded in the Avro schema, then return the embedded schema.
   * An embedded schema is present if the Avro {@link Schema} has a "com.linkedin.data" property and the
   * "com.linkedin.data" property contains both "schema" and "optionalDefaultMode" properties.
   * The "schema" property provides the embedded {@link DataSchema}.
   * The "optionalDefaultMode" property provides how optional default values were translated.
   * <p>
   * If the translation mode is {@link AvroToDataSchemaTranslationMode#VERIFY_EMBEDDED_SCHEMA}
   * and a {@link DataSchema} is embedded in the Avro schema, then verify that the embedded schema
   * translates to the input Avro schema. If the translated and embedded schema is the same,
   * then return the embedded schema, else throw {@link IllegalArgumentException}.
   * <p>
   * If the translation mode is {@link com.linkedin.data.avro.AvroToDataSchemaTranslationMode#TRANSLATE}
   * or no embedded {@link DataSchema} is present, then this method
   * translates the provided Avro {@link Schema} to a {@link DataSchema}
   * as described follows:
   * <p>
   * This method translates union with null record fields in Avro {@link Schema}
   * to optional fields in {@link DataSchema}. Record fields
   * whose type is a union with null will be translated to a new type, and the field becomes optional.
   * If the Avro union has two types (one of them is the null type), then the new type of the
   * field is the non-null member type of the union. If the Avro union does not have two types
   * (one of them is the null type) then the new type of the field is a union type with the null type
   * removed from the original union.
   * <p>
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
   * <p>
   * Both the schema and default value translation takes into account that default value
   * representation for Avro unions does not include the member type discriminator and
   * the type of the default value is always the 1st member of the union.
   *
   * @param avroSchemaInJson provides the JSON representation of the Avro {@link Schema}.
   * @param options specifies the {@link AvroToDataSchemaTranslationOptions}.
   * @return the translated {@link DataSchema}.
   * @throws IllegalArgumentException if the Avro {@link Schema} cannot be translated.
   */
  public static DataSchema avroToDataSchema(String avroSchemaInJson, AvroToDataSchemaTranslationOptions options)
    throws IllegalArgumentException
  {
    ValidationOptions validationOptions = SchemaParser.getDefaultSchemaParserValidationOptions();
    validationOptions.setAvroUnionMode(true);

    SchemaParserFactory parserFactory = SchemaParserFactory.instance(validationOptions);

    DataSchemaResolver resolver = getResolver(parserFactory, options);
    DataSchemaParser parser = parserFactory.create(resolver);
    parser.parse(avroSchemaInJson);
    if (parser.hasError())
    {
      throw new IllegalArgumentException(parser.errorMessage());
    }
    assert(parser.topLevelDataSchemas().size() == 1);
    DataSchema dataSchema = parser.topLevelDataSchemas().get(0);
    DataSchema resultDataSchema = null;

    AvroToDataSchemaTranslationMode translationMode = options.getTranslationMode();
    if (translationMode == AvroToDataSchemaTranslationMode.RETURN_EMBEDDED_SCHEMA ||
        translationMode == AvroToDataSchemaTranslationMode.VERIFY_EMBEDDED_SCHEMA)
    {
      // check for embedded schema

      Object dataProperty = dataSchema.getProperties().get(SchemaTranslator.DATA_PROPERTY);
      if (dataProperty != null && dataProperty.getClass() == DataMap.class)
      {
        Object schemaProperty = ((DataMap) dataProperty).get(SchemaTranslator.SCHEMA_PROPERTY);
        if (schemaProperty.getClass() == DataMap.class)
        {
          SchemaParser embeddedSchemaParser = SchemaParserFactory.instance().create(null);
          embeddedSchemaParser.parse(Arrays.asList(schemaProperty));
          if (embeddedSchemaParser.hasError())
          {
            throw new IllegalArgumentException("Embedded schema is invalid\n" + embeddedSchemaParser.errorMessage());
          }
          assert(embeddedSchemaParser.topLevelDataSchemas().size() == 1);
          resultDataSchema = embeddedSchemaParser.topLevelDataSchemas().get(0);

          if (translationMode == AvroToDataSchemaTranslationMode.VERIFY_EMBEDDED_SCHEMA)
          {
            // additional verification to make sure that embedded schema translates to Avro schema
            DataToAvroSchemaTranslationOptions dataToAvdoSchemaOptions = new DataToAvroSchemaTranslationOptions();
            Object optionalDefaultModeProperty = ((DataMap) dataProperty).get(SchemaTranslator.OPTIONAL_DEFAULT_MODE_PROPERTY);
            dataToAvdoSchemaOptions.setOptionalDefaultMode(OptionalDefaultMode.valueOf(optionalDefaultModeProperty.toString()));
            Schema avroSchemaFromEmbedded = dataToAvroSchema(resultDataSchema, dataToAvdoSchemaOptions);
            Schema avroSchemaFromJson = Schema.parse(avroSchemaInJson);
            if (avroSchemaFromEmbedded.equals(avroSchemaFromJson) == false)
            {
              throw new IllegalArgumentException("Embedded schema does not translate to input Avro schema: " + avroSchemaInJson);
            }
          }
        }
      }
    }
    if (resultDataSchema == null)
    {
      // translationMode == TRANSLATE or no embedded schema

      DataSchemaTraverse traverse = new DataSchemaTraverse();
      traverse.traverse(dataSchema, AvroToDataSchemaConvertCallback.INSTANCE);
      // convert default values
      traverse.traverse(dataSchema, DefaultAvroToDataConvertCallback.INSTANCE);
      // make sure it can round-trip
      String dataSchemaJson = dataSchema.toString();
      resultDataSchema = DataTemplateUtil.parseSchema(dataSchemaJson);
    }
    return resultDataSchema;
  }

  /**
   * See {@link #avroToDataSchema(String, AvroToDataSchemaTranslationOptions)}.
   *
   * @param avroSchemaInJson provides the JSON representation of the Avro {@link Schema}.
   * @return the translated {@link DataSchema}.
   * @throws IllegalArgumentException if the Avro {@link Schema} cannot be translated.
   */
  public static DataSchema avroToDataSchema(String avroSchemaInJson) throws IllegalArgumentException
  {
    return avroToDataSchema(avroSchemaInJson, new AvroToDataSchemaTranslationOptions());
  }

  /**
   * See {@link #avroToDataSchema(String, AvroToDataSchemaTranslationOptions)}.
   * <p>
   * When Avro {@link Schema} is parsed from its JSON representation and the resulting
   * Avro {@link Schema} is serialized via {@link #toString()} into JSON again, it does not
   * preserve the custom properties that are in the original JSON representation.
   * Since this method uses the {@link org.apache.avro.Schema#toString()}, this method
   * should not be used together with {@link AvroToDataSchemaTranslationMode#RETURN_EMBEDDED_SCHEMA}
   * or {@link AvroToDataSchemaTranslationMode#VERIFY_EMBEDDED_SCHEMA}. These modes depend
   * on custom properties to provide the embedded schema. If custom properties are not preserved,
   * any embedded schema will not be available to
   * {@link #dataToAvroSchema(com.linkedin.data.schema.DataSchema, DataToAvroSchemaTranslationOptions)}.
   *
   * @param avroSchema provides the Avro {@link Schema}.
   * @param options specifies the {@link AvroToDataSchemaTranslationOptions}.
   * @return the translated {@link DataSchema}.
   * @throws IllegalArgumentException if the Avro {@link Schema} cannot be translated.
   */
  public static DataSchema avroToDataSchema(Schema avroSchema, AvroToDataSchemaTranslationOptions options) throws IllegalArgumentException
  {
    String avroSchemaInJson = avroSchema.toString();
    return avroToDataSchema(avroSchemaInJson, options);
  }

  /**
   * See {@link #avroToDataSchema(Schema, AvroToDataSchemaTranslationOptions)}.
   *
   * @param avroSchema provides the Avro {@link Schema}.
   * @return the translated {@link DataSchema}.
   * @throws IllegalArgumentException if the Avro {@link Schema} cannot be translated.
   */
  public static DataSchema avroToDataSchema(Schema avroSchema) throws IllegalArgumentException
  {
    String avroSchemaInJson = avroSchema.toString();
    return avroToDataSchema(avroSchemaInJson, new AvroToDataSchemaTranslationOptions());
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
    return AvroAdapterFinder.getAvroAdapter().stringToAvroSchema(jsonAvroSchema);
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
    return AvroAdapterFinder.getAvroAdapter().stringToAvroSchema(jsonAvroSchema);
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
   * <p>
   * This method translates optional fields in the {@link DataSchema} to union with null
   * fields in Avro {@link Schema}. Record fields with optional attribute set to true will
   * be translated to a union type that has null member type. If field's type is not
   * a union, then the new union type will be a union of the field's type and the null type.
   * If the field's type is already a union, the new union type contains all the
   * union's member types and the null type.
   * <p>
   * This method also translates or sets the default value for optional fields in
   * the {@link DataSchema}. If the optional field does not have a default value,
   * set the translated default value to null. {@link OptionalDefaultMode}
   * specifies how an optional field with a default value is translated.
   * <p>
   * Both the schema and default value translation takes into account that default value
   * representation for Avro unions does not include the member type discriminator and
   * the type of the default value is always the 1st member type of the union. Schema translation
   * fails by throwing an {@link IllegalArgumentException} if the default value's type
   * is not the same as the 1st member type of the union.
   * <p>
   * If {@link DataToAvroSchemaTranslationOptions#getEmbeddedSchema()} EmbeddedSchema()} is
   * set to {@link EmbedSchemaMode#ROOT_ONLY}, then the input {@link DataSchema} will be embedded in the
   * translated Avro {@link Schema}.
   * The embedded schema will be the value of the "schema" property within the "com.linkedin.data" property.
   * If the input {@link DataSchema} is a typeref, then embedded schema will be that of the
   * actual type referenced.
   *
   * @param dataSchema provides the {@link DataSchema}.
   * @param options specifies the {@link DataToAvroSchemaTranslationOptions}.
   * @return the JSON representation of the Avro {@link Schema}.
   * @throws IllegalArgumentException if the {@link DataSchema} cannot be translated.
   */
  public static String dataToAvroSchemaJson(DataSchema dataSchema, DataToAvroSchemaTranslationOptions options) throws IllegalArgumentException
  {
    // convert default values
    DataSchemaTraverse postOrderTraverse = new DataSchemaTraverse(DataSchemaTraverse.Order.POST_ORDER);
    final DefaultDataToAvroConvertCallback defaultConverter = new DefaultDataToAvroConvertCallback(options);
    postOrderTraverse.traverse(dataSchema, defaultConverter);
    // convert schema
    String schemaJson = SchemaToAvroJsonEncoder.schemaToAvro(dataSchema, defaultConverter.fieldDefaultValueProvider(), options);
    return schemaJson;
  }

  /**
   * Allows caller to specify a file path for schema resolution.
   */
  private static DataSchemaResolver getResolver(SchemaParserFactory parserFactory, AvroToDataSchemaTranslationOptions options)
  {
    String resolverPath = options.getFileResolutionPaths();
    if (resolverPath != null)
    {
      FileDataSchemaResolver resolver = new FileDataSchemaResolver(parserFactory, resolverPath);
      resolver.setExtension(AVRO_FILE_EXTENSION);
      return resolver;
    }
    else
    {
      return new DefaultDataSchemaResolver(parserFactory);
    }
  }

  interface FieldDefaultValueProvider
  {
    Object defaultValue(RecordDataSchema.Field field);
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
    private static class FieldInfo
    {
      private FieldInfo(DataSchema defaultSchema, Object defaultValue)
      {
        _defaultSchema = defaultSchema;
        _defaultValue = defaultValue;
      }

      public String toString()
      {
        return _defaultSchema + " " + _defaultValue;
      }

      final DataSchema _defaultSchema;
      final Object _defaultValue;

      private static FieldInfo NULL_FIELD_INFO = new FieldInfo(DataSchemaConstants.NULL_DATA_SCHEMA, Data.NULL);
    }

    private IdentityHashMap<RecordDataSchema.Field, FieldInfo> _fieldInfos = new IdentityHashMap<RecordDataSchema.Field, FieldInfo>();
    private final DataToAvroSchemaTranslationOptions _options;
    private DataSchema _newDefaultSchema;

    private DefaultDataToAvroConvertCallback(DataToAvroSchemaTranslationOptions options)
    {
      _options = options;
    }

    private FieldDefaultValueProvider fieldDefaultValueProvider()
    {
      FieldDefaultValueProvider defaultValueProvider = new FieldDefaultValueProvider()
      {
        @Override
        public Object defaultValue(RecordDataSchema.Field field)
        {
          DefaultDataToAvroConvertCallback.FieldInfo fieldInfo = _fieldInfos.get(field);
          return fieldInfo == null ? null : fieldInfo._defaultValue;
        }
      };
      return defaultValueProvider;
    }

    protected boolean knownFieldInfo(RecordDataSchema.Field field)
    {
      return _fieldInfos.containsKey(field);
    }

    protected void addFieldInfo(RecordDataSchema.Field field, FieldInfo fieldInfo)
    {
      Object existingValue = _fieldInfos.put(field, fieldInfo);
      assert(existingValue == null);
    }

    @Override
    public void callback(List<String> path, DataSchema schema)
    {
      if (schema.getType() != DataSchema.Type.RECORD)
      {
        return;
      }
      // if schema has avro override, do not translate the record's fields default values
      if (schema.getProperties().get("avro") != null)
      {
        return;
      }
      RecordDataSchema recordSchema = (RecordDataSchema) schema;
      for (RecordDataSchema.Field field : recordSchema.getFields())
      {
        if (knownFieldInfo(field) == false)
        {
          Object defaultData = field.getDefault();
          if (defaultData != null)
          {
            path.add(DataSchemaConstants.DEFAULT_KEY);
            _newDefaultSchema = null;
            Object newDefault = translateField(pathList(path), defaultData, field);
            addFieldInfo(field, new FieldInfo(_newDefaultSchema, newDefault));
            path.remove(path.size() - 1);
          }
          else if (field.getOptional())
          {
            // no default specified and optional
            addFieldInfo(field, FieldInfo.NULL_FIELD_INFO);
          }
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
        if (fieldDataSchema.getDereferencedType() != DataSchema.Type.UNION)
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
              // Figure out field's type is same as the chosen type for the 1st member of the translated field's union.
              // For example, this can occur if the string field is optional and has no default, but a record's default
              // overrides the field's default to a string. This will cause the field's union to be [ "null", "string" ].
              // Since "null" is the first member of the translated union, the record cannot provide a default that
              // is not "null".
              FieldInfo fieldInfo = _fieldInfos.get(field);
              if (fieldInfo != null)
              {
                if (fieldInfo._defaultSchema != fieldDataSchema)
                {
                  throw new IllegalArgumentException(
                    message(path,
                            "cannot translate field because its default value's type is not the same as translated field's first union member's type"));
                }
              }
              fieldDataSchema = field.getType();
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
      _newDefaultSchema = fieldDataSchema;
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
