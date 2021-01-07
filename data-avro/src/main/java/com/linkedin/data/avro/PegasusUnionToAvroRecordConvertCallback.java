package com.linkedin.data.avro;

import com.google.common.base.CaseFormat;
import com.linkedin.data.Data;
import com.linkedin.data.DataMap;
import com.linkedin.data.message.Message;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaConstants;
import com.linkedin.data.schema.DataSchemaTraverse;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;


/**
 * Implementation of {@link com.linkedin.data.schema.DataSchemaTraverse.Callback} for translating Pegasus fields of
 * type 'union with aliases' into a record.
 *
 * For every invocation of this callback with a record schema, if there exists a field of type union with aliases
 * specified for its members, the field's type will be updated to a record representation of this union. The
 * default value, if exists, will also be updated accordingly. As this callback mutates the schema being traversed,
 * use this with caution.
 *
 * @author Arun Ponniah Sethuramalingam
 */
class PegasusUnionToAvroRecordConvertCallback implements DataSchemaTraverse.Callback {
  private final DataToAvroSchemaTranslationOptions _options;
  private final IdentityHashMap<RecordDataSchema.Field, FieldOverride> _schemaOverrides;

  PegasusUnionToAvroRecordConvertCallback(DataToAvroSchemaTranslationOptions options,
      IdentityHashMap<RecordDataSchema.Field, FieldOverride> schemaOverrides)
  {
    _options = options;
    _schemaOverrides = schemaOverrides;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void callback(List<String> path, DataSchema schema)
  {
    if (schema.getType() != DataSchema.Type.RECORD)
    {
      return;
    }

    // If schema has avro override, do not translate the record's aliased union fields.
    // These are handled in AvroOverrideFactory#createFromDataSchema() while encoding the Avro schema.
    if (schema.getProperties().get("avro") != null)
    {
      return;
    }

    RecordDataSchema recordSchema = (RecordDataSchema) schema;
    for (RecordDataSchema.Field field : recordSchema.getFields())
    {
      DataSchema fieldSchema = field.getType().getDereferencedDataSchema();

      Map<String, Object> propagatedProperties =
          (Map<String, Object>) SchemaToAvroJsonEncoder.produceFieldProperties(field, _options);

      // The conversion from Pegasus union type to an Avro record is performed when the union appears as either the
      // field's direct type or when the field's type is an array or a map whose (nested) elements is of union type.
      // This conversion ignores the default value when specified on an array or map typed field. Since the elements in
      // the default value collection can have conflicting union members, it will be hard to figure out the optionality
      // property of the fields in the generated Avro record.

      DataMap modifiedDefaultValue = modifyFieldDefaultValue(field, path);

      DataSchema modifiedSchema = modifyFieldSchema(recordSchema, field, fieldSchema, modifiedDefaultValue);

      if (modifiedSchema != null)
      {
        overrideUnionFieldSchemaAndDefault(field, modifiedSchema, modifiedDefaultValue, propagatedProperties);
      }
    }
  }

  /**
   * Translates the default value specified on a field. The default value translation only happens for fields whose type
   * is a union that uses aliases for its members. The modified default value will be for the equivalent Avro record that
   * will be generated for this Union type during schema translation.
   *
   * @param field Reference to the union field whose default value is modified
   * @param path The path of the union field whose default value is modified
   * @return An instance of {@link DataMap} which is the modified default value or null if the default value doesn't
   * have to be translated
   */
  private DataMap modifyFieldDefaultValue(RecordDataSchema.Field field, List<String> path)
  {
    DataMap modifiedDefaultValue = null;

    // Find if the field's type is a union that uses aliases for its members
    DataSchema fieldSchema = field.getType().getDereferencedDataSchema();
    boolean unionWithMembersAliased = fieldSchema.getType() == DataSchema.Type.UNION && ((UnionDataSchema) fieldSchema).areMembersAliased();

    // If the field is required or the OptionalDefaultMode.TRANSLATE_DEFAULT is used, propagate the default value to the new record
    boolean propagateDefault = !field.getOptional() || _options.getOptionalDefaultMode() == OptionalDefaultMode.TRANSLATE_DEFAULT;

    Object defaultValue = field.getDefault();
    if (unionWithMembersAliased && propagateDefault && defaultValue != null)
    {
      String key;
      if (defaultValue == Data.NULL) {
        key = DataSchemaConstants.NULL_TYPE;
      } else {
        DataMap unionMap = (DataMap) defaultValue;
        if (unionMap.size() != 1) {
          Message message = new Message(path.toArray(), "union default value $1%s has more than one entry", defaultValue);
          throw new IllegalArgumentException(message.toString());
        }
        Map.Entry<String, Object> entry = unionMap.entrySet().iterator().next();
        key = entry.getKey();
      }

      modifiedDefaultValue = (defaultValue == Data.NULL) ? new DataMap() : new DataMap((DataMap) defaultValue);
      modifiedDefaultValue.put(DataSchemaConstants.DISCRIMINATOR_FIELD, key);
    }

    return modifiedDefaultValue;
  }

  /**
   * Modify the schema for the specified field. The schema modification happens only for fields whose type is either a
   * union that uses aliases for its members or an array or map that has a similar union as its element type (either
   * direct or nested).
   *
   * @param recordSchema An instance of {@link RecordDataSchema} for the record that contains this field
   * @param field An instance of {@link com.linkedin.data.schema.RecordDataSchema.Field} whose schema needs to be translated
   * @param dataSchema An instance of {@link DataSchema} that is being translated. The initial call will have the field's
   *                   schema but when called recursively for arrays and maps, the schema will be of it's elements.
   * @param modifiedDefaultValue An instance of {@link DataMap} which is the modified default value for the field
   * @return An instance of {@link DataSchema} which is the translated schema for the field or null, if the schema
   * doesn't have to be translated
   */
  private DataSchema modifyFieldSchema(RecordDataSchema recordSchema, RecordDataSchema.Field field,
      DataSchema dataSchema, DataMap modifiedDefaultValue)
  {
    DataSchema modifiedSchema = null;
    switch (dataSchema.getType()) {
      case ARRAY:
        DataSchema itemsSchema = ((ArrayDataSchema) dataSchema).getItems().getDereferencedDataSchema();
        // Stop propagating the default value if the field type is an array
        DataSchema modifiedItemsSchema = modifyFieldSchema(recordSchema, field, itemsSchema, null);
        if (modifiedItemsSchema != null)
        {
          modifiedSchema = new ArrayDataSchema(modifiedItemsSchema);
        }
        break;
      case MAP:
        DataSchema valuesSchema = ((MapDataSchema) dataSchema).getValues().getDereferencedDataSchema();
        // Stop propagating the default value if the field type is a map
        DataSchema modifiedValuesSchema = modifyFieldSchema(recordSchema, field, valuesSchema, null);
        if (modifiedValuesSchema != null)
        {
          modifiedSchema = new MapDataSchema(modifiedValuesSchema);
        }
        break;
      case UNION:
        UnionDataSchema unionDataSchema = (UnionDataSchema) dataSchema;
        if (unionDataSchema.areMembersAliased())
        {
          modifiedSchema = buildContainerRecordFromUnion(
              unionDataSchema, field.getName(), recordSchema.getFullName(), modifiedDefaultValue);
        }
        break;
    }

    return modifiedSchema;
  }

  /**
   * Helper method to set an override for the field's schema and default value.
   *
   * @param field Reference to the field whose schema and default value is being overridden
   * @param modifiedSchema The override schema to use for the specified field
   * @param modifiedDefaultValue The override default value to use for the specified field
   * @param propagatedProperties The properties value to use for the specified field
   */
  private void overrideUnionFieldSchemaAndDefault(RecordDataSchema.Field field,
      DataSchema modifiedSchema, Object modifiedDefaultValue, Map<String, Object> propagatedProperties)
  {
    // Stash the field's original type and default value, so that we can use this for reverting them back after
    // the schema translation is complete. This is because we don't want the input schema to have any modifications
    // when the control goes back to the caller.
    FieldOverride fieldSchemaOverride = new FieldOverride(field.getType(), field.getDefault());
    _schemaOverrides.put(field, fieldSchemaOverride);

    field.setType(modifiedSchema);
    field.setDefault(modifiedDefaultValue);
    field.setProperties(propagatedProperties);
  }

  /**
   * Helper method to build a Record schema that represents the passed in Union schema. The new record will contain an
   * optional field for every member in the union with the same type. In addition to these fields, there will be an extra
   * field {@link DataSchemaConstants#DISCRIMINATOR_FIELD} of type enum with all the union member keys as its symbols.
   *
   * @param unionDataSchema Union schema of type {@link UnionDataSchema}
   * @param unionFieldName The name of the union's field. This will be used as the prefix for the new record's name.
   * @param parentRecordFullName The full name of the record that contains this union.
   * @param defaultValue Default value if any available for the union, null is allowed.
   * @return The new generated record schema of type {@link RecordDataSchema}
   */
  private RecordDataSchema buildContainerRecordFromUnion(
      UnionDataSchema unionDataSchema, String unionFieldName, String parentRecordFullName, DataMap defaultValue)
  {
    StringBuilder errorMessageBuilder = new StringBuilder();

    unionFieldName = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, unionFieldName);

    // Use the parent record's full name plus the union field name as the suffix. The parent record's name is included
    // to avoid any potential name conflicts between other similar unions under the same namespace.
    Name recordName = new Name(parentRecordFullName + unionFieldName, errorMessageBuilder);

    RecordDataSchema recordDataSchema = new RecordDataSchema(recordName, RecordDataSchema.RecordType.RECORD);
    List<RecordDataSchema.Field> fields = new ArrayList<>();
    List<String> memberKeys = new ArrayList<>();
    for (UnionDataSchema.Member member: unionDataSchema.getMembers())
    {
      // Add optional fields only for non-null members in the union schema
      if (!DataSchema.Type.NULL.equals(member.getType().getDereferencedType()))
      {
        RecordDataSchema.Field field = new RecordDataSchema.Field(member.getType());
        field.setName(member.getUnionMemberKey(), errorMessageBuilder);
        field.setDoc(member.getDoc());
        field.setDeclaredInline(member.isDeclaredInline());
        field.setOptional(true);
        field.setRecord(recordDataSchema);

        if (defaultValue != null && defaultValue.containsKey(member.getUnionMemberKey()))
        {
          field.setDefault(defaultValue.get(member.getUnionMemberKey()));
        }

        // Add a custom property to identify fields translated from a Pegasus union
        Map<String, Object> properties = new HashMap<>(member.getProperties());
        properties.put(SchemaTranslator.TRANSLATED_UNION_MEMBER_PROPERTY, true);
        field.setProperties(properties);

        fields.add(field);
      }

      memberKeys.add(member.getUnionMemberKey());
    }

    RecordDataSchema.Field discriminatorField = buildDiscriminatorEnumField(
        recordName.getFullName(), memberKeys, errorMessageBuilder);
    discriminatorField.setRecord(recordDataSchema);
    fields.add(discriminatorField);

    recordDataSchema.setFields(fields, errorMessageBuilder);
    recordDataSchema.setProperties(unionDataSchema.getProperties());
    return  recordDataSchema;
  }

  private RecordDataSchema.Field buildDiscriminatorEnumField(
      String parentRecordFullName, List<String> memberKeys, StringBuilder errorMessageBuilder)
  {
    Name enumName = new Name(parentRecordFullName + SchemaTranslator.CONTAINER_RECORD_DISCRIMINATOR_ENUM_SUFFIX, errorMessageBuilder);
    EnumDataSchema enumDataSchema = new EnumDataSchema(enumName);
    enumDataSchema.setSymbols(memberKeys, errorMessageBuilder);

    RecordDataSchema.Field field = new RecordDataSchema.Field(enumDataSchema);
    field.setName(DataSchemaConstants.DISCRIMINATOR_FIELD, errorMessageBuilder);
    field.setDoc("Contains the name of the field that has its value set.");
    field.setDeclaredInline(true);
    field.setOptional(false);

    return field;
  }
}
