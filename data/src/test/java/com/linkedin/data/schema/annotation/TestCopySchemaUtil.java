package com.linkedin.data.schema.annotation;

import com.linkedin.data.TestUtil;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaConstants;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.FixedDataSchema;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.PrimitiveDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import java.util.Objects;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestCopySchemaUtil
{
  public String fooSchemaText =
      "{" +
      "  \"name\" : \"Foo\"," +
      "  \"type\" : \"record\"," +
      "  \"fields\" : [" +
      "    { \"name\" : \"intField\", \"type\" : \"int\", \"optional\" : true }," +
      "    { \"name\" : \"stringField\", \"type\" : \"string\", \"optional\" : true }," +
      "    { \"name\" : \"arrayField\", \"type\" : { \"type\" : \"array\", \"items\" : \"Foo\" }, \"optional\" : true }," +
      "    { \"name\" : \"fixedField\", \"type\" : { \"type\" : \"fixed\", \"name\":\"namedFixed\", \"size\" : 16 }, \"optional\" : true }," +
      "    { \"name\" : \"mapField\", \"type\" : { \"type\" : \"map\", \"values\" : \"Foo\" }, \"optional\" : true }," +
      "    { \"name\" : \"enumField\", \"type\" : {\"name\":\"namedEnum\", \"type\":\"enum\", \"symbols\": [ \"SYMBOL1\", \"SYMBOL2\", \"SYMBOL3\" ] }, \"optional\" : true }," +
      "    { \"name\" : \"unionField\", \"type\" : [ \"int\", \"string\" ], \"optional\" : true }," +
      "    { \"name\" : \"typeRefField\", \"type\" : {\"name\":\"namedTypeRef\", \"type\": \"typeref\", \"ref\": \"int\"}, \"optional\" : true }," +
      "    { \"name\" : \"unionWithAliasesField\", \"type\" : [" +
      "        {" +
      "          \"type\" : \"string\"," +
      "          \"alias\" : \"stringFieldInUnionWithAliases\"" +
      "        }," +
      "        {" +
      "          \"type\": {" +
      "            \"type\" : \"array\"," +
      "            \"items\" : \"string\"" +
      "          }," +
      "          \"alias\" : \"arrayOfStringInUnionWithAliases\"" +
      "        }" +
      "     ], \"optional\" : true }" +
      "  ]" +
      "}";

  @Test
  public void testCopyField() throws Exception
  {
    RecordDataSchema fooSchema = (RecordDataSchema) TestUtil.dataSchemaFromString(fooSchemaText);
    RecordDataSchema.Field field = fooSchema.getField("intField");
    // Use old field to do the exact copy
    RecordDataSchema.Field newField = CopySchemaUtil.copyField(field, field.getType());
    newField.setRecord(field.getRecord());
    // Copy result should appear to be the same
    Assert.assertEquals(field, newField);
  }

  @Test
  public void testCopyUnionMember() throws Exception
  {
    RecordDataSchema fooSchema = (RecordDataSchema) TestUtil.dataSchemaFromString(fooSchemaText);
    UnionDataSchema unionDataSchema = (UnionDataSchema) fooSchema.getField("unionField").getType();
    UnionDataSchema.Member firstMember = unionDataSchema.getMembers().get(0);
    UnionDataSchema.Member newMember = CopySchemaUtil.copyUnionMember(firstMember, firstMember.getType());
    Assert.assertEquals(firstMember, newMember);
  }

  @Test
  public void testBuildSkeletonSchema() throws Exception
  {
    DataSchema oldSchema = null;
    RecordDataSchema fooSchema = (RecordDataSchema) TestUtil.dataSchemaFromString(fooSchemaText);
    // Test Record
    RecordDataSchema newRecordSchema = (RecordDataSchema) CopySchemaUtil.buildSkeletonSchema(fooSchema);
    assert((newRecordSchema.getFields().size() == 0) && Objects.equals(newRecordSchema.getDoc(), fooSchema.getDoc())
           && Objects.equals(newRecordSchema.getProperties(), fooSchema.getProperties())
           && Objects.equals(newRecordSchema.getAliases(), fooSchema.getAliases()));
    // Test TypeRef
    oldSchema = fooSchema.getField("typeRefField").getType();
    TyperefDataSchema newTypeRefDataSchema = (TyperefDataSchema) CopySchemaUtil.buildSkeletonSchema(oldSchema);
    assert( Objects.equals(newTypeRefDataSchema.getDoc(), ((TyperefDataSchema) oldSchema).getDoc())
           && Objects.equals(newTypeRefDataSchema.getProperties(), oldSchema.getProperties())
           && Objects.equals(newTypeRefDataSchema.getAliases(), ((TyperefDataSchema)oldSchema).getAliases()));
    // Test Union
    oldSchema = fooSchema.getField("unionField").getType();
    UnionDataSchema newUnionDataSchema = (UnionDataSchema) CopySchemaUtil.buildSkeletonSchema(oldSchema);
    assert(newUnionDataSchema.getMembers().size() == 0 && Objects.equals(newUnionDataSchema.getProperties(), oldSchema.getProperties()));
    // Test map
    oldSchema = fooSchema.getField("mapField").getType();
    MapDataSchema mapDataSchema = (MapDataSchema) CopySchemaUtil.buildSkeletonSchema(oldSchema);
    assert (Objects.equals(mapDataSchema.getProperties(), oldSchema.getProperties()) &&
            Objects.equals(mapDataSchema.getValues(), DataSchemaConstants.NULL_DATA_SCHEMA));
    // Test array
    oldSchema = fooSchema.getField("arrayField").getType();
    ArrayDataSchema arrayDataSchema = (ArrayDataSchema) CopySchemaUtil.buildSkeletonSchema(oldSchema);
    assert (Objects.equals(arrayDataSchema.getProperties(), oldSchema.getProperties()) &&
            Objects.equals(arrayDataSchema.getItems(), DataSchemaConstants.NULL_DATA_SCHEMA));
    // Test ENUM
    oldSchema = fooSchema.getField("enumField").getType();
    EnumDataSchema enumDataSchema = (EnumDataSchema) CopySchemaUtil.buildSkeletonSchema(oldSchema);
    Assert.assertEquals(enumDataSchema, oldSchema);
    // Test FIXED
    oldSchema = fooSchema.getField("fixedField").getType();
    FixedDataSchema fixedDataSchema = (FixedDataSchema) CopySchemaUtil.buildSkeletonSchema(oldSchema);
    Assert.assertEquals(fixedDataSchema, oldSchema);
    // Test primitive
    oldSchema = fooSchema.getField("intField").getType();
    PrimitiveDataSchema primitiveDataSchema = (PrimitiveDataSchema) CopySchemaUtil.buildSkeletonSchema(oldSchema);
    Assert.assertEquals(primitiveDataSchema, oldSchema);

  }

}
