package com.linkedin.data.avro;

import com.linkedin.data.Data;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaConstants;
import com.linkedin.data.schema.DataSchemaTraverse;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import java.util.List;
import java.util.Map;
import org.apache.avro.Schema;


/**
 * Translate values from {@link DataSchema} format to Avro {@link Schema} format.
 *
 * The translated values retains the union member type discriminator for default values.
 * The Avro JSON schema encoder {@link com.linkedin.data.avro.SchemaToAvroJsonEncoder} needs to know
 * the default value type in order to make this type the 1st member type of the
 * Avro union.
 *
 * The output of this translator is a map of fields to translated default values.
 */
class DefaultDataToAvroConvertCallback extends AbstractDefaultDataTranslator implements DataSchemaTraverse.Callback
{
  private final DataToAvroSchemaTranslationOptions _options;
  private final Map<RecordDataSchema.Field, FieldOverride> _defaultValueOverrides;
  private DataSchema _newDefaultSchema;

  DefaultDataToAvroConvertCallback(DataToAvroSchemaTranslationOptions options,
      Map<RecordDataSchema.Field, FieldOverride> defaultValueOverrides)
  {
    _options = options;
    _defaultValueOverrides = defaultValueOverrides;
  }

  @Override
  public void callback(List<String> path, DataSchema schema)
  {
    if (schema.getType() != DataSchema.Type.RECORD)
    {
      return;
    }
    // If schema has avro override, do not translate the record's fields default values
    // These are handled in AvroOverrideFactory#createFromDataSchema() while encoding the Avro schema.
    if (schema.getProperties().get("avro") != null)
    {
      return;
    }
    RecordDataSchema recordSchema = (RecordDataSchema) schema;
    for (RecordDataSchema.Field field : recordSchema.getFields())
    {
      FieldOverride defaultValueOverride = _defaultValueOverrides.get(field);
      if (defaultValueOverride == null)
      {
        Object defaultData = field.getDefault();
        if (defaultData != null)
        {
          if (_options.getDefaultFieldTranslationMode() ==
              PegasusToAvroDefaultFieldTranslationMode.DO_NOT_TRANSLATE)
          {
            //If defaultField translationMode is "DO_NOT_TRANSLATE"
            // set override to NULL as well
            _defaultValueOverrides.put(field, FieldOverride.NULL_DEFAULT_VALUE);
          }
          else
          {
          path.add(field.getName());
          _newDefaultSchema = null;
          Object newDefault = translateField(pathList(path), defaultData, field);
          _defaultValueOverrides.put(field, new FieldOverride(_newDefaultSchema, newDefault));
          path.remove(path.size() - 1);
          }
        }
        else if (field.getOptional())
        {
          // no default specified and optional
          _defaultValueOverrides.put(field, FieldOverride.NULL_DEFAULT_VALUE);
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

    DataSchema memberDataSchema = unionDataSchema.getTypeByMemberKey(key);
    if (memberDataSchema == null)
    {
      throw new IllegalArgumentException(message(path, "union value %1$s has invalid member key %2$s", value, key));
    }
    if (memberDataSchema != unionDataSchema.getMembers().get(0).getType())
    {
      throw new IllegalArgumentException(
          message(path,
              "cannot translate union value %1$s because its type is not the 1st member type of the union %2$s",
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
    boolean isTranslatedUnionMember = Boolean.TRUE == field.getProperties().get(SchemaTranslator.TRANSLATED_UNION_MEMBER_PROPERTY);
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
          // If OptionalDefaultMode.TRANSLATE_TO_NULL is used, we set the default value for the field as null. There
          // is an exception, if this field represents a translated Pegasus union member.
          //
          // When this optional field is translated to Avro, it will be represented as an Union of this field's type
          // and null as its members. The aforementioned exception is required to determine the correct order of member
          // types in the translated Avro union. For more information, see SchemaToAvroJsonEncoder#encodeFieldType()
          // on how the presence of default value for an optional field is used to determine the order in which union
          // member types appear in the translated Avro schema.
          if ((_options.getOptionalDefaultMode() == OptionalDefaultMode.TRANSLATE_TO_NULL) && !isTranslatedUnionMember)
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
            FieldOverride defaultValueOverride = _defaultValueOverrides.get(field);
            if (defaultValueOverride != null)
            {
              if (defaultValueOverride.getSchema() != fieldDataSchema)
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
      assert((_options.getOptionalDefaultMode() != OptionalDefaultMode.TRANSLATE_TO_NULL) ||
          (_options.getOptionalDefaultMode() == OptionalDefaultMode.TRANSLATE_TO_NULL && isTranslatedUnionMember) ||
          (fieldValue == Data.NULL));
    }
    else if (fieldValue == null)
    {
      // If the default specified at parent level doesn't specify a value for the field, use the default specified at
      // field level.
      fieldValue = field.getDefault();
      if (fieldValue == null)
      {
        throw new IllegalArgumentException(
            message(path, "Cannot translate required field without default."));
      }
    }
    Object resultFieldValue = translate(path, fieldValue, fieldDataSchema);
    _newDefaultSchema = fieldDataSchema;
    return resultFieldValue;
  }
}
