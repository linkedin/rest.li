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


import com.linkedin.data.Data;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaConstants;
import com.linkedin.data.schema.JsonBuilder;
import com.linkedin.data.schema.Named;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.SchemaToJsonEncoder;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.UnionDataSchema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.linkedin.data.avro.SchemaTranslator.AVRO_PREFIX;
import static com.linkedin.data.schema.DataSchemaConstants.DEFAULT_KEY;
import static com.linkedin.data.schema.DataSchemaConstants.TYPE_KEY;

/**
 * Serializes and outputs {@link DataSchema}s in
 * Avro-compliant schema in JSON representation.
 */
class SchemaToAvroJsonEncoder extends SchemaToJsonEncoder
{
  /**
   * Serialize a {@link DataSchema} to an Avro-compliant schema as a JSON encoded string.
   *
   * @param schema is the {@link DataSchema} to build a JSON encoded output for.
   * @param fieldOverridesProvider provides the default values for each of the fields.
   * @param options provides the {@link DataToAvroSchemaTranslationOptions}.
   * @return the Avro-compliant schema as JSON encoded string.
   */
  static String schemaToAvro(DataSchema schema,
                             FieldOverridesProvider fieldOverridesProvider,
                             DataToAvroSchemaTranslationOptions options)
  {
    JsonBuilder builder = null;
    try
    {
      builder = new JsonBuilder(options.getPretty());
      final SchemaToAvroJsonEncoder serializer = new SchemaToAvroJsonEncoder(builder, schema, fieldOverridesProvider, options);
      serializer.encode(schema);
      return builder.result();
    }
    catch (IOException exc)
    {
      throw new IllegalStateException(exc);
    }
    finally
    {
      if (builder != null)
      {
        builder.closeQuietly();
      }
    }
  }

  protected SchemaToAvroJsonEncoder(JsonBuilder builder,
                                    DataSchema rootSchema,
                                    FieldOverridesProvider fieldOverridesProvider,
                                    DataToAvroSchemaTranslationOptions options)
  {
    super(builder);
    _rootSchema = rootSchema;
    _fieldOverridesProvider = fieldOverridesProvider;
    _options = options;
  }

  /**
   * Encode a {@link DataSchema}.
   *
   * Special handling is required for typeref's. All typeref's are
   * de-referenced to the actual type.
   *
   * @param schema to encode.
   * @throws IOException
   */
  @Override
  public void encode(DataSchema schema) throws IOException
  {
    if (encodeCustomAvroSchema(schema) == false)
    {
      super.encode(schema.getDereferencedDataSchema());
    }
  }

  /**
   * Encode a {@link DataSchema}.
   *
   * Special handling is required for typeref's. All typeref's are
   * de-referenced to the actual type.
   *
   * @param schema to encode.
   * @throws IOException
   */
  @Override
  protected void encode(DataSchema schema, boolean originallyInlined) throws IOException
  {
    if (encodeCustomAvroSchema(schema) == false)
    {
      super.encode(schema.getDereferencedDataSchema(), originallyInlined);
    }
  }


  @Override
  protected void encodeProperties(DataSchema schema) throws IOException
  {
    if (_options.getEmbeddedSchema() == EmbedSchemaMode.ROOT_ONLY)
    {
      DataSchema dereferencedSchema = _rootSchema.getDereferencedDataSchema();
      if (schema == dereferencedSchema && schema.getType() != DataSchema.Type.UNION)
      {
        encodePropertiesWithEmbeddedSchema(schema);
        return;
      }
    }
    super.encodeProperties(schema);
  }

  private static final Set<String> RESERVED_DATA_PROPERTIES =
    new HashSet<String>(Arrays.asList(
        SchemaTranslator.AVRO_PREFIX,
        SchemaTranslator.SCHEMA_PROPERTY,
        SchemaTranslator.OPTIONAL_DEFAULT_MODE_PROPERTY,
        SchemaTranslator.TRANSLATED_UNION_MEMBER_PROPERTY));

  private void encodePropertiesWithEmbeddedSchema(DataSchema schema) throws IOException
  {
    Object dataProperty = null;
    for (Map.Entry<String, Object> entry : schema.getProperties().entrySet())
    {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (key.equals(SchemaTranslator.DATA_PROPERTY))
      {
        dataProperty = value;
      }
      else
      {
        _builder.writeFieldName(key);
        _builder.writeData(value);
      }
    }

    _builder.writeFieldName(SchemaTranslator.DATA_PROPERTY);
    _builder.writeStartObject();
    _builder.writeFieldName(SchemaTranslator.SCHEMA_PROPERTY);
    SchemaToJsonEncoder schemaToJsonEncoder = new SchemaToJsonEncoder(_builder);
    schemaToJsonEncoder.encode(schema);
    _builder.writeFieldName(SchemaTranslator.OPTIONAL_DEFAULT_MODE_PROPERTY);
    _builder.writeString(_options.getOptionalDefaultMode().toString());

    if (dataProperty != null && dataProperty.getClass() == DataMap.class)
    {
      for (Map.Entry<String, Object> pegasusEntry : ((DataMap) dataProperty).entrySet())
      {
        String key = pegasusEntry.getKey();
        if (RESERVED_DATA_PROPERTIES.contains(key) == false)
        {
          _builder.writeFieldName(pegasusEntry.getKey());
          _builder.writeData(pegasusEntry.getValue());
        }
      }
    }
    _builder.writeEndObject();
  }

  /**
   * Encode a field's type to an Avro-compliant schema.
   *
   * Special handling is required for optional fields.
   * An optional field is encoded as a union with null.
   * If the optional field is not a union, then union
   * the field's type with null.
   * If the optional field is already a union, then
   * include null as a member type if it is not already
   * part of the union.
   * If the resulting type is a union and resulting field
   * has a default value, then the resulting union's list
   * of member types are encoded such that the type of
   * the translated default value is always the 1st type
   * in this list (see Avro specification for more details.)
   *
   * For required and non-union fields, no special handling is required.
   *
   * @param field providing the type to encode.
   * @throws IOException if there is an error while encoding.
   */
  @Override
  protected void encodeFieldType(RecordDataSchema.Field field) throws IOException
  {
    DataSchema fieldSchema = field.getType();
    UnionDataSchema unionDataSchema =
      (fieldSchema.getDereferencedType() == DataSchema.Type.UNION ?
        (UnionDataSchema) fieldSchema.getDereferencedDataSchema() :
        null);
    _builder.writeFieldName(TYPE_KEY);

    Object defaultValue = field.getDefault();
    boolean optional = field.getOptional() ||
        //If chose to translate default to optional field AND ALSO has defaultValue
        ((defaultValue !=null)
            && _options.getDefaultFieldTranslationMode() == PegasusToAvroDefaultFieldTranslationMode.DO_NOT_TRANSLATE);
    if (!optional && unionDataSchema == null)
    {
      encode(fieldSchema);
    }
    else
    {
      // This branch handles
      // optional fields,
      // or default field needs to be translated as optional
      // or union fields(includes special handling for unions (BOTH optional or default)

      // output will be an union if the field is optional or its type is a union

      // whether to add null to translated union,
      // set to true for optional non-union type or optional union without null member
      boolean addNullMemberType;
      // DataSchema of default value, null if there is no default value.
      DataSchema defaultValueSchema;
      // members of the union (excluding null introduced by optional)
      List<DataSchema> resultMemberTypes;

      if (optional)
      {
        // handle optional field // or if want to translate "required field with default" to Optional field
        boolean isTranslatedUnionMember = (Boolean.TRUE == field.getProperties().get(SchemaTranslator.TRANSLATED_UNION_MEMBER_PROPERTY));
        if (unionDataSchema == null)
        {
          addNullMemberType = true;
          resultMemberTypes = new ArrayList<DataSchema>(1);
          resultMemberTypes.add(fieldSchema);
          defaultValueSchema = (
            defaultValue != null
              && _options.getDefaultFieldTranslationMode() == PegasusToAvroDefaultFieldTranslationMode.TRANSLATE
              && (isTranslatedUnionMember || _options.getOptionalDefaultMode() == OptionalDefaultMode.TRANSLATE_DEFAULT) ?
                fieldSchema : DataSchemaConstants.NULL_DATA_SCHEMA);
        }
        else
        {
          addNullMemberType = unionDataSchema.getTypeByMemberKey(DataSchemaConstants.NULL_TYPE) == null;
          resultMemberTypes = unionDataSchema.getMembers().stream()
              .map(UnionDataSchema.Member::getType)
              .collect(Collectors.toList());
          defaultValueSchema = (
            defaultValue != null
                && _options.getDefaultFieldTranslationMode() == PegasusToAvroDefaultFieldTranslationMode.TRANSLATE
                && _options.getOptionalDefaultMode() == OptionalDefaultMode.TRANSLATE_DEFAULT ?
                unionValueDataSchema(unionDataSchema, defaultValue) :
                DataSchemaConstants.NULL_DATA_SCHEMA);
        }
        assert((_options.getOptionalDefaultMode() != OptionalDefaultMode.TRANSLATE_TO_NULL) ||
              (isTranslatedUnionMember || _options.getOptionalDefaultMode() == OptionalDefaultMode.TRANSLATE_DEFAULT) ||
              (defaultValueSchema == DataSchemaConstants.NULL_DATA_SCHEMA));
      }
      else
      {
        // must be required union, AND didn't choose to be translated as optional
        addNullMemberType = false;
        resultMemberTypes = unionDataSchema.getMembers().stream()
            .map(UnionDataSchema.Member::getType)
            .collect(Collectors.toList());
        defaultValueSchema = unionValueDataSchema(unionDataSchema, defaultValue);
      }

      // encode the member types
      // add null member type if addNullMemberType is present
      _builder.writeStartArray();
      // this variable keeps track of whether null member type has been emitted
      boolean emittedNull = false;
      // if field has a default, defaultValueSchema != null, always encode it 1st, this includes NULL_DATA_SCHEMA
      if (defaultValueSchema != null)
      {
        emittedNull |= (defaultValueSchema.getDereferencedType() == DataSchema.Type.NULL);
        encode(defaultValueSchema);
      }
      for (DataSchema type : resultMemberTypes)
      {
        if (defaultValueSchema == type)
        {
          continue;
        }
        if (type.getDereferencedType() == DataSchema.Type.NULL)
        {
          if (emittedNull)
            continue;
          else
            emittedNull = true;
        }
        encode(type);
      }
      // emit null member type if it is has to be added and has not already been emitted
      if (addNullMemberType && !emittedNull)
      {
        _builder.writeString(DataSchemaConstants.NULL_TYPE);
        emittedNull = true;
      }
      assert(!addNullMemberType || emittedNull);
      _builder.writeEndArray();
    }
  }

  private static DataSchema unionValueDataSchema(UnionDataSchema unionDataSchema, Object value)
  {
    DataSchema schema;
    if (value == null)
    {
      schema = null;
    }
    else if (value == Data.NULL)
    {
      schema = DataSchemaConstants.NULL_DATA_SCHEMA;
    }
    else
    {
      DataMap dataMap = (DataMap) value;
      Map.Entry<String, ?> mapEntry = dataMap.entrySet().iterator().next();
      schema = unionDataSchema.getTypeByMemberKey(mapEntry.getKey());
      assert(schema != null);
    }
    return schema;
  }

  /**
   * Encode a field's default to an Avro-compliant schema.
   *
   * Special handling is required if the translated default value
   * is a union value whose value is not null. In this case,
   * the default value includes the member type discriminator,
   * special handling required to comply with Avro specification
   * requires that (a) the member type to be the 1st member of the
   * union's member type list and (b) the discriminator not be
   * included in the default value.
   *
   * @param field providing the default value to encode.
   * @throws IOException if there is an error while encoding.
   */
  @Override
  protected void encodeFieldDefault(RecordDataSchema.Field field) throws IOException
  {
    FieldOverride defaultValueOverride = _fieldOverridesProvider.getDefaultValueOverride(field);

    // if field is optional, it must have a default value - either Data.NULL or translated value
    assert(!field.getOptional() || (defaultValueOverride != null && defaultValueOverride.getValue() != null));

    boolean isTranslatedUnionMember = (Boolean.TRUE == field.getProperties().get(SchemaTranslator.TRANSLATED_UNION_MEMBER_PROPERTY));

    Object defaultValue = (defaultValueOverride != null) ? defaultValueOverride.getValue() : null;

    if (defaultValue != null || isTranslatedUnionMember) {
      _builder.writeFieldName(DEFAULT_KEY);
      _builder.writeData(defaultValue);
    }
  }

  /**
   * Override to not emit optional flags.
   *
   * @param field providing the optional flag to encode.
   * @throws IOException if there is an error while encoding.
   */
  @Override
  protected void encodeFieldOptional(RecordDataSchema.Field field) throws IOException
  {
    // do nothing.
  }

  /**
   * Override for RecordSchema field's properties encoding in Avro
   *   (1) contains special handling for TypeRef property propagation and
   *   (2) filtered out reserved data properties keyword
   *
   * @param field RecordDataSchema's field
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  @Override
  protected void encodeFieldProperties(RecordDataSchema.Field field) throws IOException
  {
    Stream<Map.Entry<String, Object>> toBeFiltered = field.getProperties().entrySet().stream();

    // If a record field's type is a TypeRef, will need to propagate TypeRef's properties to current record field
    //   and merge with record field's properties.
    if (field.getType().getType() == DataSchema.Type.TYPEREF)
    {
      toBeFiltered = Stream.concat(toBeFiltered,
          ((TyperefDataSchema) field.getType()).getMergedTyperefProperties().entrySet().stream());
    }
    // Property merge rule:
    // For property content inherited from TypeRef that appears to be have same property name as the record field:
    //    if the two property contents are Map type, they will be merged at this level,
    //    otherwise Typeref field property content will be overridden by record field property's content.
    BinaryOperator<Object> propertyMergeLogic = (originalPropertyContent, inheritedPropertyContent) -> {
      if (originalPropertyContent instanceof Map && inheritedPropertyContent instanceof Map)
      {
        Map<String, Object> mergedMap = new DataMap((Map<String, Object>) originalPropertyContent);
        ((Map<String, Object>) inheritedPropertyContent).forEach(mergedMap::putIfAbsent);
        return mergedMap;
      }
      else
      {
        return originalPropertyContent;
      }
    };

    final Map<String, ?> filteredMap = toBeFiltered
        .filter(entry -> !RESERVED_DATA_PROPERTIES.contains(entry.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, propertyMergeLogic));

    _builder.writeProperties(filteredMap);
  }

  /**
   * Encode namespace in the {@link Named}.
   *
   * This method encodes the namespace fields.
   * If the override namespace option is true, the namespace will be prefixed with AVRO_PREFIX.
   *
   * @param schema provides the {@link NamedDataSchema}.
   */
  @Override
  protected String encodeNamespace(Named schema)
  {
    String namespace = schema.getNamespace();
    if (_options.isOverrideNamespace())
    {
      if (!namespace.isEmpty())
      {
        namespace = AVRO_PREFIX + "." + namespace;
      }
      else {
        namespace = AVRO_PREFIX;
      }
    }
    return namespace;
  }

  @Override
  protected void encodeField(RecordDataSchema.Field field) throws IOException
  {
    super.encodeField(field);

    // Reset the field's type and default if there is an override
    FieldOverride schemaOverride = _fieldOverridesProvider.getSchemaOverride(field);
    if (schemaOverride != null) {
      field.setType(schemaOverride.getSchema());
      field.setDefault(schemaOverride.getValue());
    }
  }

  /**
   * Do not encode "include" attribute.
   *
   * The "include" attribute is not encoded.
   * The included fields will be encoded.
   *
   * @return false.
   */
  @Override
  protected boolean isEncodeInclude()
  {
    return false;
  }

  /**
   * Encode custom Avro schema.
   *
   * @param schema the {@link DataSchema} to encode.
   * @return true if there is a custom Avro schema.
   */
  protected boolean encodeCustomAvroSchema(DataSchema schema) throws IOException
  {
    boolean encodedCustomAvroSchema = false;
    AvroOverride avroOverride = _avroOverrideMap.getAvroOverride(schema);
    _avroOverrideFactory.emitExceptionIfThereAreErrors();
    if (avroOverride != null)
    {
      if (avroOverride.getAccessCount() == 1)
      {
        _builder.writeData(avroOverride.getAvroSchemaDataMap());
        encodedCustomAvroSchema = true;
      }
      else
      {
        _builder.writeString(avroOverride.getAvroSchemaFullName());
        encodedCustomAvroSchema = true;
      }
    }
    return encodedCustomAvroSchema;
  }

  private final DataSchema _rootSchema;
  private final FieldOverridesProvider _fieldOverridesProvider;

  private final DataToAvroSchemaTranslationOptions _options;

  private static final MyAvroOverrideFactory _avroOverrideFactory = new MyAvroOverrideFactory();

  private static class MyAvroOverrideFactory extends AvroOverrideFactory
  {
    private StringBuilder _stringBuilder = null;
    private Formatter _formatter = null;

    private MyAvroOverrideFactory()
    {
      setInstantiateCustomDataTranslator(false);
    }

    @Override
    void emitMessage(String format, Object... args)
    {
      if (_stringBuilder == null)
      {
        _stringBuilder = new StringBuilder();
        _formatter = new Formatter(_stringBuilder);
      }
      else
      {
        _stringBuilder.append(", ");
      }
      _formatter.format(format, args);
    }

    private void emitExceptionIfThereAreErrors() throws SchemaTranslationException
    {
      if (_stringBuilder != null)
      {
        StringBuilder sb = _stringBuilder;
        _stringBuilder = null;
        throw new SchemaTranslationException(sb.toString());
      }
    }
  }

  private final AvroOverrideMap _avroOverrideMap = new AvroOverrideMap(_avroOverrideFactory);
}
