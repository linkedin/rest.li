/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.data.schema.grammar;

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.FixedDataSchema;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import com.linkedin.data.schema.resolver.DefaultDataSchemaResolver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * Tests for {@link PdlSchemaParser}.
 */
public class TestPdlSchemaParser
{
  @Test
  public void testParseNestedProperties() throws IOException
  {
    String sourcePdl = "namespace com.linkedin.test\n"
        + "\n"
        + "@validate.one.two.arrayOne = [\"a\", \"b\"]\n"
        + "@validate.one.two.arrayTwo = [1,2,3,4]\n"
        + "@validate.`com.linkedin.CustomValidator`.low = 5\n"
        + "@validate.`com.linkedin.namespace.CustomValidator`.low = 15\n"
        + "@validate.`com.linkedin.namespace.CustomValidator`.`union`.low.`record` = \"Date\"\n"
        + "@`com.linkedin.CustomValidator` = \"abc\"\n"
        + "@pathProp.`/*`.`*/.$` = false\n"
        + "@`/*.*/.$`\n"
        + "@grammarChars.`foo[type=a.b.c].bar` = \"grammarChars\"\n"
        + "record RecordDataSchema {}";

    // construct expected data map
    Map<String, Object> expected = new HashMap<>();
    DataMap validate = new DataMap();
    // @validate.one.two.arrayOne = ["a", "b"]"
    // @validate.one.two.arrayTwo = [1,2,3,4]"
    DataMap one = new DataMap();
    DataMap two = new DataMap();
    two.put("arrayOne", new DataList(Arrays.asList("a", "b")));
    two.put("arrayTwo", new DataList(Arrays.asList(1, 2, 3, 4)));
    one.put("two", two);
    validate.put("one", one);

    // @validate.`com.linkedin.CustomValidator`.low = 5"
    DataMap customValidator = new DataMap();
    customValidator.put("low", 5);
    validate.put("com.linkedin.CustomValidator", customValidator);

    // @validate.`com.linkedin.namespace.CustomValidator`.low = 15"
    // @validate.`com.linkedin.namespace.CustomValidator`.`union`.low.`record` = "Date"
    DataMap customValidator2 = new DataMap();
    customValidator2.put("low", 15);
    DataMap unionMap = new DataMap();
    customValidator2.put("union", unionMap);
    DataMap lowMap = new DataMap();
    unionMap.put("low", lowMap);
    lowMap.put("record", "Date");
    validate.put("com.linkedin.namespace.CustomValidator", customValidator2);
    expected.put("validate", validate);

    // @`com.linkedin.CustomValidator` = "abc"
    expected.put("com.linkedin.CustomValidator", "abc");

    // @pathProp.`/*`.`*/.$` = false
    DataMap propertyWithPath = new DataMap();
    DataMap propertyWithSpecialChars = new DataMap();
    propertyWithPath.put("/*", propertyWithSpecialChars);
    propertyWithSpecialChars.put("*/.$", false);
    expected.put("pathProp", propertyWithPath);

    // @`/*.*/.$`
    expected.put("/*.*/.$", true);

    // "@grammarChars.`foo[type=a.b.c].bar` = "grammarChars"
    DataMap grammarChars = new DataMap();
    grammarChars.put("foo[type=a.b.c].bar", "grammarChars");
    expected.put("grammarChars", grammarChars);

    DataSchema encoded = TestUtil.dataSchemaFromPdlString(sourcePdl);
    Assert.assertNotNull(encoded);
    TestUtil.assertEquivalent(encoded.getProperties(), expected);
  }

  @Test
  public void testUnionDataSchemaWithAliases()
  {
    List<String> membersInDeclaredOrder = new ArrayList<>(Arrays.asList("null", "member", "article", "school",
        "organization", "company", "jobs", "courses", "fingerprint", "audio", "video"));
    Set<String> inlinedMembers = new HashSet<>(Arrays.asList("organization", "jobs", "courses", "fingerprint", "audio"));

    RecordDataSchema mainRecordSchema = (RecordDataSchema) parsePdlSchema("unionWithAliases.pdl");
    RecordDataSchema.Field resourceField = mainRecordSchema.getField("resource");

    UnionDataSchema resourceSchema = (UnionDataSchema) resourceField.getType();

    Assert.assertTrue(resourceSchema.areMembersAliased());
    Assert.assertEquals(resourceSchema.getMembers().size(), membersInDeclaredOrder.size());

    int index = 0;
    for (UnionDataSchema.Member member: resourceSchema.getMembers())
    {
      Assert.assertFalse(member.hasError());

      boolean isNonNullMember = (member.getType().getDereferencedType() != DataSchema.Type.NULL);

      // Only non-null members should be aliased
      Assert.assertEquals(member.hasAlias(), isNonNullMember);

      String memberKey = member.getUnionMemberKey();
      DataSchema type = member.getType();

      // Verify the member's getUnionMemberKey() is same as the member alias (for non null members)
      Assert.assertEquals(memberKey, isNonNullMember ? member.getAlias() : type.getUnionMemberKey());

      // Verify the order is maintained as declared in the union definition
      Assert.assertEquals(memberKey, membersInDeclaredOrder.get(index));

      // Verify the inlined member definition is captured correctly
      Assert.assertEquals(member.isDeclaredInline(), inlinedMembers.contains(memberKey));

      // Verify the type, doc and other properties
      Assert.assertEquals(type, resourceSchema.getTypeByMemberKey(memberKey));
      Assert.assertEquals(member.getDoc(), isNonNullMember ? memberKey + " doc" : "");
      Assert.assertEquals(member.getProperties().containsKey("inlined"), member.isDeclaredInline());

      index++;
    }
  }

  @Test
  public void testUnionDataSchemaWithoutAliases()
  {
    List<String> membersInDeclaredOrder = new ArrayList<>(Arrays.asList("null", "int",
        "com.linkedin.data.schema.grammar.AuxRecord", "com.linkedin.data.schema.grammar.Organization", "array", "map",
        "com.linkedin.data.schema.grammar.MD5", "string"));
    Set<String> inlinedMembers = new HashSet<>(Arrays.asList("com.linkedin.data.schema.grammar.Organization", "array",
        "map", "com.linkedin.data.schema.grammar.MD5", "string"));

    RecordDataSchema mainRecordSchema = (RecordDataSchema) parsePdlSchema("unionWithoutAliases.pdl");
    RecordDataSchema.Field resourceField = mainRecordSchema.getField("resource");

    UnionDataSchema resourceSchema = (UnionDataSchema) resourceField.getType();

    Assert.assertFalse(resourceSchema.areMembersAliased());
    Assert.assertEquals(resourceSchema.getMembers().size(), membersInDeclaredOrder.size());

    int index = 0;
    for (UnionDataSchema.Member member: resourceSchema.getMembers())
    {
      Assert.assertFalse(member.hasError());

      Assert.assertFalse(member.hasAlias());
      Assert.assertNull(member.getAlias());

      String memberKey = member.getUnionMemberKey();
      DataSchema type = member.getType();

      // Verify the member's getUnionMemberKey() is same as the member type's getUnionMemberKey()
      Assert.assertEquals(memberKey, type.getUnionMemberKey());

      // Verify the order is maintained as declared in the union definition
      Assert.assertEquals(memberKey, membersInDeclaredOrder.get(index));

      // Verify the type, doc and other properties are empty
      Assert.assertEquals(type, resourceSchema.getTypeByMemberKey(memberKey));
      Assert.assertTrue(member.getDoc().isEmpty());
      Assert.assertTrue(member.getProperties().isEmpty());

      // Verify the inlined member definition is captured correctly
      Assert.assertEquals(member.isDeclaredInline(), inlinedMembers.contains(memberKey));

      index++;
    }
  }

  /**
   * Ensures that a {@link NamedDataSchema} can have aliases defined, and that those aliases can be used to reference
   * the schema.
   */
  @Test
  public void testNamedDataSchemaWithAliases()
  {
    RecordDataSchema mainRecordSchema = (RecordDataSchema) parsePdlSchema("namedWithAliases.pdl");

    // Test that all the aliases have the correct full name
    assertAliasesEqual(mainRecordSchema.getField("recordField"),
        "com.linkedin.data.schema.grammar.RecordAlias",
        "com.linkedin.data.schema.grammar.RecordAlias2");
    assertAliasesEqual(mainRecordSchema.getField("typerefField"),
        "com.linkedin.data.schema.grammar.TyperefAlias");
    assertAliasesEqual(mainRecordSchema.getField("fixedField"),
        "com.linkedin.data.schema.grammar.FixedAlias");
    assertAliasesEqual(mainRecordSchema.getField("enumField"),
        "com.linkedin.data.schema.grammar.EnumAlias",
        "org.example.OverriddenEnumAlias");

    // Test that the aliases are bound to the correct schemas
    RecordDataSchema.Field refsField = mainRecordSchema.getField("references");
    Assert.assertNotNull(refsField);
    RecordDataSchema refsRecord = (RecordDataSchema) refsField.getType();

    assertFieldTypesEqual(refsRecord, mainRecordSchema, "recordField");
    assertFieldTypesEqual(refsRecord, mainRecordSchema, "typerefField");
    assertFieldTypesEqual(refsRecord, mainRecordSchema, "fixedField");
    assertFieldTypesEqual(refsRecord, mainRecordSchema, "enumField");

  }

  @Test
  public void testComplexTypeWithProperties()
  {
    RecordDataSchema mainRecordSchema = (RecordDataSchema) parsePdlSchema("ComplexTypeWithProperties.pdl");
    RecordDataSchema.Field arrayField = mainRecordSchema.getField("arrayField");
    Assert.assertNotNull(arrayField);
    Assert.assertFalse(arrayField.getType().getProperties().isEmpty());
    DataMap expectedProperty  = new DataMap();
    DataMap validate = new DataMap();
    validate.put("minSize", 1);
    expectedProperty.put("validate", validate);
    Assert.assertTrue(arrayField.getProperties().isEmpty());
    Assert.assertEquals(arrayField.getType().getProperties(), expectedProperty);

    RecordDataSchema.Field mapField = mainRecordSchema.getField("mapField");
    Assert.assertNotNull(mapField);
    Assert.assertTrue(mapField.getProperties().isEmpty());
    Assert.assertFalse(mapField.getType().getProperties().isEmpty());
    Assert.assertEquals(mapField.getType().getProperties(), expectedProperty);

    RecordDataSchema.Field unionField = mainRecordSchema.getField("unionField");
    Assert.assertNotNull(unionField);
    Assert.assertTrue(unionField.getProperties().isEmpty());
    Assert.assertFalse(unionField.getType().getProperties().isEmpty());
    validate.clear();
    validate.put("minValue", 0);
    Assert.assertEquals(unionField.getType().getProperties(), expectedProperty);
  }

  @Test
  public void testRecordParserLocations()
  {
    PdlSchemaParser parser = new PdlSchemaParser(new DefaultDataSchemaResolver(), true);
    parser.parse(getClass().getResourceAsStream("TestRecordForParserContextLocations.pdl"));
    List<DataSchema> topLevelSchemas = parser.topLevelDataSchemas();

    Assert.assertEquals(topLevelSchemas.size(), 1, "Expected 1 top-level schema to be parsed.");

    RecordDataSchema mainRecordSchema = (RecordDataSchema) topLevelSchemas.get(0);
    Map<Object, PdlSchemaParser.ParseLocation> locations = parser.getParseLocations();
    checkParseLocationForRecord(locations, mainRecordSchema);

    // Checks for namespace locations.
    // Top-level namespace
    PdlSchemaParser.ParseLocation topNamespaceLoc = locations.get(mainRecordSchema.getNamespace());
    assertLocation(topNamespaceLoc, 1, 1, 1, 42,
        "namespace: " + mainRecordSchema.getNamespace());
    // Inline namespace
    String inlineNamespace = ((RecordDataSchema) mainRecordSchema.getField("inlineNamespacedField").getType())
        .getNamespace();
    PdlSchemaParser.ParseLocation inlineNamespaceLoc = locations.get(inlineNamespace);
    assertLocation(inlineNamespaceLoc, 48, 5, 48, 38,
        "namespace: " + inlineNamespace);

  }

  @Test
  public void testEnumParserLocations()
  {
    PdlSchemaParser parser = new PdlSchemaParser(new DefaultDataSchemaResolver(), true);
    parser.parse(getClass().getResourceAsStream("TestEnumForParserContextLocations.pdl"));
    List<DataSchema> topLevelSchemas = parser.topLevelDataSchemas();

    Assert.assertEquals(topLevelSchemas.size(), 1, "Expected 1 top-level schema to be parsed.");

    EnumDataSchema topSchema = (EnumDataSchema) topLevelSchemas.get(0);
    Map<Object, PdlSchemaParser.ParseLocation> locations = parser.getParseLocations();
    checkParseLocationForEnum(locations, topSchema);
  }

  @Test
  public void testTyperefParserLocations()
  {
    PdlSchemaParser parser = new PdlSchemaParser(new DefaultDataSchemaResolver(), true);
    parser.parse(getClass().getResourceAsStream("TestTyperefForParserContextLocations.pdl"));
    List<DataSchema> topLevelSchemas = parser.topLevelDataSchemas();

    Assert.assertEquals(topLevelSchemas.size(), 1, "Expected 1 top-level schema to be parsed.");

    TyperefDataSchema topSchema = (TyperefDataSchema) topLevelSchemas.get(0);
    Map<Object, PdlSchemaParser.ParseLocation> locations = parser.getParseLocations();
    checkParseLocationForTyperef(locations, topSchema);
  }

  @Test
  public void testFixedParserLocations()
  {
    PdlSchemaParser parser = new PdlSchemaParser(new DefaultDataSchemaResolver(), true);
    parser.parse(getClass().getResourceAsStream("TestFixedForParserContextLocations.pdl"));
    List<DataSchema> topLevelSchemas = parser.topLevelDataSchemas();

    Assert.assertEquals(topLevelSchemas.size(), 1, "Expected 1 top-level schema to be parsed.");

    FixedDataSchema topSchema = (FixedDataSchema) topLevelSchemas.get(0);
    Map<Object, PdlSchemaParser.ParseLocation> locations = parser.getParseLocations();
    checkParseLocationForFixed(locations, topSchema);
  }

  private void checkParseLocationForRecord(
      Map<Object, PdlSchemaParser.ParseLocation> locations, RecordDataSchema recordSchema)
  {
    checkParseLocation(locations, recordSchema, (DataMap) recordSchema.getProperties().get("location"),
        recordSchema.getName());

    // Check all fields
    for (RecordDataSchema.Field field : recordSchema.getFields())
    {
      checkParseLocation(locations, field, (DataMap) field.getProperties().get("location"),
          recordSchema.getName() + ":" + field.getName());
      if (field.isDeclaredInline())
      {
        checkParseLocationOfInlineSchema(locations, field.getType(), field.getName());
      }
    }
  }

  private void checkParseLocationForTyperef(
      Map<Object, PdlSchemaParser.ParseLocation> locations, TyperefDataSchema typerefSchema)
  {
    checkParseLocation(locations, typerefSchema, (DataMap) typerefSchema.getProperties().get("location"),
        typerefSchema.getName());

    // Check de-referenced schema
    checkParseLocationOfInlineSchema(locations, typerefSchema.getDereferencedDataSchema(), typerefSchema.getName());
  }

  private void checkParseLocationForFixed(
      Map<Object, PdlSchemaParser.ParseLocation> locations, FixedDataSchema fixedSchema)
  {
    checkParseLocation(locations, fixedSchema, (DataMap) fixedSchema.getProperties().get("location"),
        fixedSchema.getName());
  }

  private void checkParseLocationOfInlineSchema(Map<Object, PdlSchemaParser.ParseLocation> locations,
      DataSchema schema, String context)
  {
    if (schema instanceof NamedDataSchema)
    {
      NamedDataSchema namedSchema = (NamedDataSchema) schema;
      if (namedSchema instanceof RecordDataSchema)
      {
        checkParseLocationForRecord(locations, (RecordDataSchema) namedSchema);
      }
      else if (namedSchema instanceof EnumDataSchema)
      {
        checkParseLocationForEnum(locations, (EnumDataSchema) namedSchema);
      }
      else if (namedSchema instanceof TyperefDataSchema)
      {
        checkParseLocationForTyperef(locations, (TyperefDataSchema) namedSchema);
      }
      else if (namedSchema instanceof FixedDataSchema)
      {
        checkParseLocationForFixed(locations, (FixedDataSchema) namedSchema);
      }
    }
    else if (schema instanceof UnionDataSchema)
    {
      checkParseLocationForUnion(locations, (UnionDataSchema) schema, context);
    }

  }
  private void checkParseLocationForUnion(
      Map<Object, PdlSchemaParser.ParseLocation> locations, UnionDataSchema unionSchema,
      String fieldName)
  {
    checkParseLocation(locations, unionSchema, (DataMap) unionSchema.getProperties().get("location"),
        fieldName);

    // Check all fields
    for (UnionDataSchema.Member member : unionSchema.getMembers())
    {
      DataMap expected = unionSchema.areMembersAliased() ? (DataMap) member.getProperties().get("location")
          : ((DataMap) unionSchema.getProperties().get("location")).getDataMap(member.getUnionMemberKey());
      checkParseLocation(locations, member, expected, fieldName + ":" + member.getUnionMemberKey());
      if (member.isDeclaredInline())
      {
        checkParseLocationOfInlineSchema(locations, member.getType(), fieldName);
      }
    }
  }

  private void checkParseLocationForEnum(
      Map<Object, PdlSchemaParser.ParseLocation> locations, EnumDataSchema enumSchema)
  {
    checkParseLocation(locations, enumSchema, (DataMap) enumSchema.getProperties().get("location"),
        enumSchema.getName());

    // Check all symbols
    for (String symbol : enumSchema.getSymbols())
    {
      checkParseLocation(locations, symbol, (DataMap) enumSchema.getSymbolProperties(symbol).get("location"),
          enumSchema.getName() + ":" + symbol);
    }
  }

  private void checkParseLocation(Map<Object, PdlSchemaParser.ParseLocation> locations, Object schemaNode,
      DataMap expected, String context)
  {
    PdlSchemaParser.ParseLocation location = locations.get(schemaNode);
    assertLocation(location, expected.getInteger("startLine"), expected.getInteger("startCol"),
        expected.getInteger("endLine"), expected.getInteger("endCol"), context);
  }

  private void assertLocation(PdlSchemaParser.ParseLocation location, int startLine, int startCol, int endLine,
      int endCol, String context)
  {
    Assert.assertNotNull(location);
    Assert.assertEquals(location.getStartLine(), startLine, context + " startLine");
    Assert.assertEquals(location.getStartColumn(), startCol, context + " startCol");
    Assert.assertEquals(location.getEndLine(), endLine, context + " endLine");
    Assert.assertEquals(location.getEndColumn(), endCol, context + " endCol");

  }

  /**
   * Asserts that the aliases of some field's type are equivalent to the given strings.
   * @param field field whose type has aliases
   * @param expectedFullAliasNames expected aliases (full names)
   */
  private void assertAliasesEqual(RecordDataSchema.Field field, String ... expectedFullAliasNames)
  {
    Assert.assertNotNull(field);
    List<String> actualFullAliasNames = ((NamedDataSchema) field.getType()).getAliases()
        .stream()
        .map(Name::getFullName)
        .collect(Collectors.toList());

    Set<String> expectedFullAliasNameSet = new HashSet<>(Arrays.asList(expectedFullAliasNames));
    Assert.assertEquals(actualFullAliasNames.size(), expectedFullAliasNameSet.size());
    Assert.assertTrue(expectedFullAliasNameSet.containsAll(actualFullAliasNames),
        String.format("Incorrect aliases for field \"%s\". Expected aliases %s. Found aliases %s.",
            field.getName(), expectedFullAliasNameSet, actualFullAliasNames));
  }

  /**
   * Asserts that for two schemas A and B, the field "fieldName" for each is of the same type.
   * @param schemaA schema A
   * @param schemaB schema B
   * @param fieldName field name to check on each schema
   */
  private void assertFieldTypesEqual(RecordDataSchema schemaA, RecordDataSchema schemaB, String fieldName)
  {
    RecordDataSchema.Field fieldA = schemaA.getField(fieldName);
    Assert.assertNotNull(fieldA);

    RecordDataSchema.Field fieldB = schemaB.getField(fieldName);
    Assert.assertNotNull(fieldB);

    Assert.assertEquals(fieldA.getType(), fieldB.getType(), "Expected the type of both fields to be the same.");
  }

  /**
   * Parses a .pdl file found at a given filename in the resource directory for this class.
   * @param filename file name pointing to a .pdl file
   * @return parsed data schema
   */
  private DataSchema parsePdlSchema(String filename)
  {
    PdlSchemaParser parser = new PdlSchemaParser(new DefaultDataSchemaResolver());
    parser.parse(getClass().getResourceAsStream(filename));
    List<DataSchema> topLevelSchemas = parser.topLevelDataSchemas();

    Assert.assertEquals(topLevelSchemas.size(), 1, "Expected 1 top-level schema to be parsed.");

    return topLevelSchemas.get(0);
  }
}
