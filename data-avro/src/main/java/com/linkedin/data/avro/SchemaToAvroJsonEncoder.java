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
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.SchemaToJsonEncoder;
import com.linkedin.data.schema.UnionDataSchema;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
   * @param fieldDefaultValues provides the default values for each of the fields.
   * @param options provides the {@link DataToAvroSchemaTranslationOptions}.
   * @return the Avro-compliant schema as JSON encoded string.
   */
  static String schemaToAvro(DataSchema schema,
                             IdentityHashMap<RecordDataSchema.Field, Object> fieldDefaultValues,
                             DataToAvroSchemaTranslationOptions options)
  {
    try
    {
      JsonBuilder builder = new JsonBuilder(options.getPretty());
      SchemaToAvroJsonEncoder serializer = new SchemaToAvroJsonEncoder(builder, schema, fieldDefaultValues, options);
      serializer.encode(schema);
      return builder.result();
    }
    catch (IOException exc)
    {
      throw new IllegalStateException(exc);
    }
  }

  protected SchemaToAvroJsonEncoder(JsonBuilder builder,
                                    DataSchema rootSchema,
                                    IdentityHashMap<RecordDataSchema.Field, Object> fieldDefaultValues,
                                    DataToAvroSchemaTranslationOptions options)
  {
    super(builder);
    _rootSchema = rootSchema;
    _fieldDefaultValues = fieldDefaultValues;
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
    new HashSet<String>(Arrays.asList(SchemaTranslator.SCHEMA_PROPERTY, SchemaTranslator.OPTIONAL_DEFAULT_MODE_PROPERTY));

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
    boolean optional = field.getOptional();
    DataSchema fieldSchema = field.getType();
    UnionDataSchema unionDataSchema =
      (fieldSchema.getDereferencedType() == DataSchema.Type.UNION ?
        (UnionDataSchema) fieldSchema.getDereferencedDataSchema() :
        null);
    _builder.writeFieldName(TYPE_KEY);

    if (optional == false && unionDataSchema == null)
    {
      encode(fieldSchema);
    }
    else
    {
      // special handling for unions
      // output will be an union if the field is optional or its type is a union

      // whether to add null to translated union,
      // set to true for optional non-union type or optional union without null member
      boolean addNullMemberType;
      // DataSchema of default value, null if there is no default value.
      DataSchema defaultValueSchema;
      // members of the union (excluding null introduced by optional)
      List<DataSchema> resultMemberTypes;

      Object defaultValue = field.getDefault();
      if (optional)
      {
        if (unionDataSchema == null)
        {
          addNullMemberType = true;
          resultMemberTypes = new ArrayList<DataSchema>(1);
          resultMemberTypes.add(fieldSchema);
          defaultValueSchema = (
            defaultValue != null && _options.getOptionalDefaultMode() == OptionalDefaultMode.TRANSLATE_DEFAULT ?
              fieldSchema :
              DataSchemaConstants.NULL_DATA_SCHEMA);
        }
        else
        {
          addNullMemberType = unionDataSchema.getType(DataSchemaConstants.NULL_TYPE) == null;
          resultMemberTypes = unionDataSchema.getTypes();
          defaultValueSchema = (
            defaultValue != null && _options.getOptionalDefaultMode() == OptionalDefaultMode.TRANSLATE_DEFAULT ?
              unionValueDataSchema(unionDataSchema, defaultValue) :
              DataSchemaConstants.NULL_DATA_SCHEMA);
        }
        assert(_options.getOptionalDefaultMode() != OptionalDefaultMode.TRANSLATE_TO_NULL ||
               defaultValueSchema == DataSchemaConstants.NULL_DATA_SCHEMA);
      }
      else
      {
        // must be union
        addNullMemberType = false;
        resultMemberTypes = unionDataSchema.getTypes();
        defaultValueSchema = unionValueDataSchema(unionDataSchema, defaultValue);
      }

      // encode the member types
      // add null member type if addNullMemberType is present
      _builder.writeStartArray();
      // this variable keeps track of whether null member type has been emitted
      boolean emittedNull = false;
      // if field has a default, defaultValueSchema != null, always encode it 1st
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
      if (addNullMemberType && emittedNull == false)
      {
        _builder.writeString(DataSchemaConstants.NULL_TYPE);
        emittedNull = true;
      }
      assert(addNullMemberType == false || emittedNull == true);
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
      schema = unionDataSchema.getTypeByName(mapEntry.getKey());
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
    Object defaultValue = _fieldDefaultValues.get(field);

    // if field is optional, it must have a default value - either Data.NULL or translated value
    assert(field.getOptional() == false || defaultValue != null);
    if (defaultValue != null)
    {
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
  private final IdentityHashMap<RecordDataSchema.Field, Object> _fieldDefaultValues;
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

  };

  private final AvroOverrideMap _avroOverrideMap = new AvroOverrideMap(_avroOverrideFactory);
}
