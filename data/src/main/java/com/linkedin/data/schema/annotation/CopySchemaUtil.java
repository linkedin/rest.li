package com.linkedin.data.schema.annotation;

import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaConstants;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.UnionDataSchema;


/**
 * Util for making data schema copies
 */
class CopySchemaUtil
{
  /**
   * Create a skeleton schema from the given schema
   * For example, if the given schema is a {@link RecordDataSchema}, the skeletonSchema will be an empty {@link RecordDataSchema} with no fields,
   * but with its doc, alias and properties copied.
   *
   * @param schema input schema to be copied
   * @return
   * @throws CloneNotSupportedException
   */
  static DataSchema buildSkeletonSchema(DataSchema schema) throws CloneNotSupportedException
  {
    switch (schema.getType())
    {
      case RECORD:
        RecordDataSchema newRecordSchema = new RecordDataSchema(new Name(((RecordDataSchema) schema).getFullName()),
                                         RecordDataSchema.RecordType.RECORD);
        RecordDataSchema originalRecordSchema = (RecordDataSchema) schema;
        if (originalRecordSchema.getAliases() != null)
        {
          newRecordSchema.setAliases(originalRecordSchema.getAliases());
        }
        if (originalRecordSchema.getDoc() != null)
        {
          newRecordSchema.setDoc(originalRecordSchema.getDoc());
        }
        if (originalRecordSchema.getProperties() != null)
        {
          newRecordSchema.setProperties(originalRecordSchema.getProperties());
        }
        return newRecordSchema;
      case UNION:
        UnionDataSchema newUnionDataSchema = new UnionDataSchema();
        UnionDataSchema unionDataSchema = (UnionDataSchema) schema;
        if (unionDataSchema.getProperties() != null)
        {
          newUnionDataSchema.setProperties(unionDataSchema.getProperties());
        }
        return newUnionDataSchema;
      case TYPEREF:
        TyperefDataSchema originalTypeRefSchema = (TyperefDataSchema) schema;
        TyperefDataSchema newTypeRefSchema = new TyperefDataSchema(new Name(originalTypeRefSchema.getFullName()));
        if (originalTypeRefSchema.getProperties() != null)
        {
          newTypeRefSchema.setProperties(originalTypeRefSchema.getProperties());
        }
        if (originalTypeRefSchema.getDoc() != null)
        {
          newTypeRefSchema.setDoc(originalTypeRefSchema.getDoc());
        }
        if (originalTypeRefSchema.getAliases() != null)
        {
          newTypeRefSchema.setAliases(originalTypeRefSchema.getAliases());
        }
        return newTypeRefSchema;
      case ARRAY:
        ArrayDataSchema originalArrayDataSchema = (ArrayDataSchema) schema;
        //Set null item types for this skeleton
        ArrayDataSchema newArrayDataSchema = new ArrayDataSchema(DataSchemaConstants.NULL_DATA_SCHEMA);
        if (originalArrayDataSchema.getProperties() != null)
        {
          newArrayDataSchema.setProperties(originalArrayDataSchema.getProperties());
        }
        return newArrayDataSchema;
      case MAP:
        MapDataSchema originalMapDataSchema = (MapDataSchema) schema;
        //Set null value types for this skeleton
        MapDataSchema newMapDataSchema = new MapDataSchema(DataSchemaConstants.NULL_DATA_SCHEMA);
        if (originalMapDataSchema.getProperties() != null)
        {
          newMapDataSchema.setProperties(originalMapDataSchema.getProperties());
        }
        return newMapDataSchema;
      case FIXED:
      case ENUM:
      default:
        // Primitive types, FIXED, ENUM: using schema's clone method
        return schema.clone();
    }
  }

  /**
   * Copy a {@link RecordDataSchema.Field} given original field object and return a new {@link RecordDataSchema.Field} object.
   *
   * @param originalField the field to be copied
   * @param fieldSchemaToReplace the field's schema that this field should contain
   * @return a copy of the originalField
   */
  static RecordDataSchema.Field copyField(RecordDataSchema.Field originalField, DataSchema fieldSchemaToReplace)
  {
    RecordDataSchema.Field newField = new RecordDataSchema.Field(fieldSchemaToReplace);
    if (originalField.getAliases() != null)
    {
      newField.setAliases(originalField.getAliases(), new StringBuilder());
    }
    if (originalField.getDefault() != null)
    {
      newField.setDefault(originalField.getDefault());
    }
    if (originalField.getDoc() != null)
    {
      newField.setDoc(originalField.getDoc());
    }
    if (originalField.getName() != null)
    {
      newField.setName(originalField.getName(), new StringBuilder());
    }
    if (originalField.getOrder() != null)
    {
      newField.setOrder(originalField.getOrder());
    }
    if (originalField.getProperties() != null)
    {
      newField.setProperties(originalField.getProperties());
    }
    newField.setOptional(originalField.getOptional());
    return newField;
  }

  /**
   * Copy a {@link UnionDataSchema.Member} given an original value and return a new {@link UnionDataSchema.Member} value.
   *
   * @param member the member object to be copied
   * @param newSkeletonSchema the dataSchema that this member object should contain
   * @return a new copy of the member object
   */
  static UnionDataSchema.Member copyUnionMember(UnionDataSchema.Member member, DataSchema newSkeletonSchema)
  {
    UnionDataSchema.Member newMember = new UnionDataSchema.Member(newSkeletonSchema);
    if (member.hasAlias())
    {
      newMember.setAlias(member.getAlias(), new StringBuilder());
    }
    newMember.setDeclaredInline(member.isDeclaredInline());
    newMember.setDoc(member.getDoc());
    newMember.setProperties(member.getProperties());
    return newMember;
  }
}
