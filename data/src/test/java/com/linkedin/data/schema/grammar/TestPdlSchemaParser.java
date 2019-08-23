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
import com.linkedin.data.schema.RecordDataSchema;
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
    String sourcePdl = "namespace com.linkedin.test\n" + "\n" + "@validate.one.two.arrayOne = [\"a\", \"b\"]\n"
        + "@validate.one.two.arrayTwo = [1,2,3,4]\n" + "record RecordDataSchema {}";

    // construct expected data map
    Map<String, Object> expected = new HashMap<>();
    DataMap validate = new DataMap();
    DataMap one = new DataMap();
    DataMap two = new DataMap();
    two.put("arrayOne", new DataList(Arrays.asList("a", "b")));
    two.put("arrayTwo", new DataList(Arrays.asList(1, 2, 3, 4)));
    one.put("two", two);
    validate.put("one", one);
    expected.put("validate", validate);

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

    PdlSchemaParser parser = new PdlSchemaParser(new DefaultDataSchemaResolver());
    parser.parse(getClass().getResourceAsStream("unionWithAliases.pdl"));
    RecordDataSchema mainRecordSchema = (RecordDataSchema) parser.topLevelDataSchemas().get(0);
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

    PdlSchemaParser parser = new PdlSchemaParser(new DefaultDataSchemaResolver());
    parser.parse(getClass().getResourceAsStream("unionWithoutAliases.pdl"));
    RecordDataSchema mainRecordSchema = (RecordDataSchema) parser.topLevelDataSchemas().get(0);
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
}
