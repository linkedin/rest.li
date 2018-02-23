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


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.DataSchemaTraverse;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.schema.SchemaParserFactory;
import com.linkedin.data.schema.PegasusSchemaParser;
import com.linkedin.data.schema.resolver.DefaultDataSchemaResolver;
import com.linkedin.data.schema.resolver.FileDataSchemaResolver;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.template.DataTemplateUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
  public static final String AVRO_PREFIX = "avro";

  public static final String CONTAINER_RECORD_DISCRIMINATOR_ENUM_SUFFIX = "Discriminator";
  public static final String TRANSLATED_UNION_MEMBER_PROPERTY = "translatedUnionMember";

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
    PegasusSchemaParser parser = parserFactory.create(resolver);
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
            if (!avroSchemaFromEmbedded.equals(avroSchemaFromJson))
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
    // Before the actual schema translation, we perform some pre-processing mainly to deal with default values and pegasus
    // unions with aliases.
    DataSchemaTraverse schemaTraverser = new DataSchemaTraverse();

    // Build callbacks for the schema traverser. We convert any Pegasus 'union with aliases' into Pegasus records during
    // PRE_ORDER and convert all the default values in the schema during POST_ORDER. The aforementioned order should be
    // maintained, as we want the Pegasus unions translated before converting the default values.
    Map<DataSchemaTraverse.Order, DataSchemaTraverse.Callback> callbacks = new HashMap<>();

    IdentityHashMap<RecordDataSchema.Field, FieldOverride> schemaOverrides = new IdentityHashMap<>();
    callbacks.put(DataSchemaTraverse.Order.PRE_ORDER, new PegasusUnionToAvroRecordConvertCallback(options, schemaOverrides));

    IdentityHashMap<RecordDataSchema.Field, FieldOverride> defaultValueOverrides = new IdentityHashMap<>();
    callbacks.put(DataSchemaTraverse.Order.POST_ORDER, new DefaultDataToAvroConvertCallback(options, defaultValueOverrides));

    schemaTraverser.traverse(dataSchema, callbacks);

    // convert schema
    FieldOverridesProvider fieldOverridesProvider = new FieldOverridesBuilder()
        .schemaOverrides(schemaOverrides)
        .defaultValueOverrides(defaultValueOverrides)
        .build();
    String schemaJson = SchemaToAvroJsonEncoder.schemaToAvro(dataSchema, fieldOverridesProvider, options);

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
}
