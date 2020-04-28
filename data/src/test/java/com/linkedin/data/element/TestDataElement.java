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

package com.linkedin.data.element;


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaConstants;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.StringDataSchema;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.data.TestUtil.dataMapFromString;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;


public class TestDataElement
{
  @Test
  public void testRootName()
  {
    assertEquals(DataElement.ROOT_NAME, new String());
    assertNotSame(DataElement.ROOT_NAME, new String());
  }

  public String fooSchemaText =
      "{" +
      "  \"name\" : \"Foo\"," +
      "  \"type\" : \"record\"," +
      "  \"fields\" : [" +
      "    { \"name\" : \"int\", \"type\" : \"int\", \"optional\" : true }," +
      "    { \"name\" : \"string\", \"type\" : \"string\", \"optional\" : true }," +
      "    { \"name\" : \"array\", \"type\" : { \"type\" : \"array\", \"items\" : \"Foo\" }, \"optional\" : true }," +
      "    { \"name\" : \"mapField\", \"type\" : { \"type\" : \"map\", \"values\" : \"string\" }, \"optional\" : true }," +
      "    { \"name\" : \"enumField\", \"type\" : {\"name\":\"namedEnum\", \"type\":\"enum\", \"symbols\": [ \"SYMBOL1\", \"SYMBOL2\", \"SYMBOL3\" ] }, \"optional\" : true }," +
      "    { \"name\" : \"unionField\", \"type\" : [ \"int\", \"string\" ], \"optional\" : true }," +
      "    { \"name\" : \"typeRefField\", \"type\" : {\"name\":\"namedTypeRef\", \"type\": \"typeref\", \"ref\": \"int\"}, \"optional\" : true }," +
      "    { \"name\" : \"typeRefFieldToMap\", \"type\" : { \"type\" : \"typeref\", \"name\" : \"namedTypeRefToMap\", \"ref\" : { \"type\" : \"map\", \"values\" : \"string\" } }, \"optional\" : true}," +
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
      "}" ;

  public static interface DataElementFactory
  {
    DataElement create(Object value, Object name, DataSchema schema, DataElement parent);
  }

  @DataProvider(name="dataElementFactories")
  public Object[][] dataElementFactories()
  {
    Object[][] factories =
      {
        {
          new DataElementFactory()
          {
            @Override
            public DataElement create(Object value, Object name, DataSchema schema, DataElement parent)
            {
              return new SimpleDataElement(value, name, schema, parent);
            }
          }
        },
        {
          new DataElementFactory()
          {
            @Override
            public DataElement create(Object value, Object name, DataSchema schema, DataElement parent)
            {
              return new MutableDataElement(value, name, schema, parent);
            }
          }
        }
      };

    return factories;
  }

  @Test(dataProvider="dataElementFactories")
  public void testDataElement(DataElementFactory factory) throws IOException
  {
    RecordDataSchema fooSchema = (RecordDataSchema) TestUtil.dataSchemaFromString(fooSchemaText);
    ArrayDataSchema arraySchema = (ArrayDataSchema) fooSchema.getField("array").getType();

    String fooText =
        " {" +
        "   \"int\" : 34," +
        "   \"string\" : \"abc\"," +
        "   \"array\" : [" +
        "     { \"int\" : 56 }," +
        "     { \"string\" : \"xyz\" }," +
        "     { \"array\" : [" +
        "       { \"int\" : 78 }" +
        "     ] }" +
        "   ]," +
        "   \"enumField\": \"SYMBOL1\"," +
        "   \"unionField\": {" +
        "       \"string\":\"unionString\"" +
        "   }," +
        "   \"mapField\": {" +
        "    \"key1\":\"value1\"," +
        "    \"key2\":\"value2\"" +
        "   }," +
        "   \"typeRefField\": \"42\"," +
        "   \"typeRefFieldToMap\": {" +
        "    \"key1\":\"value1\"," +
        "    \"key2\":\"value2\"" +
        "   }," +
        "   \"unionWithAliasesField\" : {" +
        "       \"stringFieldInUnionWithAliases\" : \"stringInUnionWithAlias\"" +
        "   }" +
        " }" ;


    DataMap foo = TestUtil.dataMapFromString(fooText);

    DataElement root = factory.create(foo, DataElement.ROOT_NAME, fooSchema, null);
    DataElement int1 = factory.create(foo.get("int"), "int", fooSchema.getField("int").getType(), root);
    DataElement string1 = factory.create(foo.get("string"), "string", fooSchema.getField("string").getType(), root);
    DataElement array1 = factory.create(foo.get("array"), "array", fooSchema.getField("array").getType(), root);

    MapDataSchema mapDataSchema = (MapDataSchema) fooSchema.getField("mapField").getType();
    StringDataSchema stringDataSchema = (StringDataSchema) mapDataSchema.getValues();
    DataElement mapFieldElement = factory.create(foo.get("mapField"), "mapField", mapDataSchema, root);
    DataElement mapValueInMapField = factory.create(mapFieldElement.getChild("key1"), "key1", stringDataSchema, mapFieldElement);

    EnumDataSchema enumDataSchema = (EnumDataSchema) fooSchema.getField("enumField").getType();
    DataElement enumField = factory.create(foo.get("enumField"), "enumField", enumDataSchema, root);

    DataElement unionField = factory.create(foo.get("unionField"), "unionField", fooSchema.getField("unionField").getType(), root);
    UnionDataSchema unionFieldSchema = (UnionDataSchema) fooSchema.getField("unionField").getType();
    DataElement unionFieldString = factory.create(unionField.getChild("string"), "string", unionFieldSchema.getTypeByMemberKey("string"), unionField);

    TyperefDataSchema typerefDataSchema = (TyperefDataSchema) fooSchema.getField("typeRefField").getType();
    DataElement typeRefField = factory.create(foo.get("typeRefField"), "typeRefField", typerefDataSchema, root);

    TyperefDataSchema typeRefToMapDataSchema = (TyperefDataSchema) fooSchema.getField("typeRefFieldToMap").getType();
    DataElement typeRefToMapField = factory.create(foo.get("typeRefFieldToMap"), "typeRefFieldToMap", typeRefToMapDataSchema, root);
    DataElement mapValueInTypeReffedMapField = factory.create(typeRefToMapField.getChild("key1"), "key1", stringDataSchema, typeRefToMapField);

    DataElement unionWithAliasesField = factory.create(foo.get("unionWithAliasesField"), "unionWithAliasesField", fooSchema.getField("unionWithAliasesField").getType(), root);
    UnionDataSchema unionWithAliasesSchema = (UnionDataSchema) fooSchema.getField("unionWithAliasesField").getType();
    DataElement stringFieldInUnionWithAliases = factory.create(unionWithAliasesField.getChild("stringFieldInUnionWithAliases"),
                                                               "stringFieldInUnionWithAliases",
                                                               unionWithAliasesSchema.getTypeByMemberKey("stringFieldInUnionWithAliases"),
                                                               unionWithAliasesField);


    DataElement foo20 = factory.create(array1.getChild(0), 0, arraySchema.getItems(), array1);
    DataElement foo21 = factory.create(array1.getChild(1), 1, arraySchema.getItems(), array1);
    DataElement foo22 = factory.create(array1.getChild(2), 2, arraySchema.getItems(), array1);

    DataElement int20 = factory.create(foo20.getChild("int"), "int", fooSchema.getField("int").getType(), foo20);
    DataElement string21 = factory.create(foo21.getChild("string"), "string", fooSchema.getField("string").getType(), foo21);
    DataElement array22 = factory.create(foo22.getChild("array"), "array", fooSchema.getField("array").getType(), foo22);

    DataElement foo30 = factory.create(array22.getChild(0), 0, arraySchema.getItems(), array22);
    DataElement int30 = factory.create(foo30.getChild("int"), "int", fooSchema.getField("int").getType(), foo30);

    // test path

    Object[][] testPathInput =
      {
        {
          root,
          foo,
          fooSchema,
          new Object[] {},
          ""
        },
        {
          int1,
          foo.get("int"),
          DataSchemaConstants.INTEGER_DATA_SCHEMA,
          new Object[] { "int" },
          "/int"
        },
        {
          string1,
          foo.get("string"),
          DataSchemaConstants.STRING_DATA_SCHEMA,
          new Object[] { "string" },
          "/string"
        },
        {
          array1,
          foo.get("array"),
          arraySchema,
          new Object[] { "array" },
          "/array"
        },
        {
            mapFieldElement,
            foo.get("mapField"),
            mapDataSchema,
            new Object[] { "mapField" },
            "/mapField"
        },
        {
            mapValueInMapField,
            ((DataMap)foo.get("mapField")).get("key1"),
            stringDataSchema,
            new Object[] { "mapField", "key1" },
            "/mapField/*"
        },
        {
            mapValueInTypeReffedMapField,
            ((DataMap)foo.get("typeRefFieldToMap")).get("key1"),
            stringDataSchema,
            new Object[] { "typeRefFieldToMap", "key1" },
            "/typeRefFieldToMap/*"
        },
        {
            enumField,
            foo.get("enumField"),
            enumDataSchema,
            new Object[] { "enumField" },
            "/enumField"
        },
        {
            unionFieldString,
            ((DataMap) foo.get("unionField")).get("string"),
            DataSchemaConstants.STRING_DATA_SCHEMA,
            new Object[] { "unionField", "string" },
            "/unionField/string"
        },
        {
            typeRefField,
            foo.get("typeRefField"),
            typerefDataSchema,
            new Object[] { "typeRefField"},
            "/typeRefField"
        },
        {
            stringFieldInUnionWithAliases,
            ((DataMap) foo.get("unionWithAliasesField")).get("stringFieldInUnionWithAliases"),
            DataSchemaConstants.STRING_DATA_SCHEMA,
            new Object[] { "unionWithAliasesField", "stringFieldInUnionWithAliases"},
            "/unionWithAliasesField/stringFieldInUnionWithAliases"
        },
        {
        foo20,
        ((DataList) foo.get("array")).get(0),
        fooSchema,
        new Object[] { "array", 0 },
        "/array/*"
        },
        {
          foo21,
          ((DataList) foo.get("array")).get(1),
          fooSchema,
          new Object[] { "array", 1 },
          "/array/*"
        },
        {
          foo22,
          ((DataList) foo.get("array")).get(2),
          fooSchema,
          new Object[] { "array", 2 },
          "/array/*"
        },
        {
          int20,
          ((DataMap) ((DataList) foo.get("array")).get(0)).get("int"),
          DataSchemaConstants.INTEGER_DATA_SCHEMA,
          new Object[] { "array", 0, "int" },
          "/array/*/int"
        },
        {
          string21,
          ((DataMap) ((DataList) foo.get("array")).get(1)).get("string"),
          DataSchemaConstants.STRING_DATA_SCHEMA,
          new Object[] { "array", 1, "string" },
          "/array/*/string"
        },
        {
          array22,
          ((DataMap) ((DataList) foo.get("array")).get(2)).get("array"),
          arraySchema,
          new Object[] { "array", 2, "array" },
          "/array/*/array"
        },
        {
          foo30,
          ((DataList) ((DataMap) ((DataList) foo.get("array")).get(2)).get("array")).get(0),
          fooSchema,
          new Object[] { "array", 2, "array", 0 },
          "/array/*/array/*"
        },
        {
          int30,
          ((DataMap) ((DataList) ((DataMap) ((DataList) foo.get("array")).get(2)).get("array")).get(0)).get("int"),
          DataSchemaConstants.INTEGER_DATA_SCHEMA,
          new Object[] { "array", 2, "array", 0, "int" },
          "/array/*/array/*/int"
        }
      };

    ArrayList<Object> pathAsList = new ArrayList<Object>();
    for (Object[] row : testPathInput)
    {
      DataElement element = (DataElement) row[0];

      // test value
      Object expectedValue = row[1];
      assertSame(expectedValue, element.getValue());

      // test schema
      DataSchema expectedSchema = (DataSchema) row[2];
      assertSame(expectedSchema, element.getSchema());

      // test name
      Object[] expectedPath = (Object[]) row[3];
      Object expectedName = expectedPath.length == 0 ? DataElement.ROOT_NAME : expectedPath[expectedPath.length - 1];
      assertEquals(expectedName, element.getName());

      // test path
      Object[] path = element.path();
      element.pathAsList(pathAsList);
      StringBuilder builder = new StringBuilder();
      StringBuilder builder2 = new StringBuilder();
      assertEquals(expectedPath.length, path.length);
      assertEquals(expectedPath.length, pathAsList.size());
      for (int i = 0; i < expectedPath.length; i++)
      {
        assertEquals(path[i], expectedPath[i]);
        assertEquals(pathAsList.get(i), expectedPath[i]);
        builder.append('*').append(expectedPath[i]);
        builder2.append(DataElement.SEPARATOR).append(expectedPath[i]);
      }
      assertEquals(builder.toString(), element.pathAsString('*'));
      assertEquals(builder2.toString(), element.pathAsString());

      // test copyChain
      DataElement copy = element.copyChain();
      assertElementChainEquals(copy, element.copyChain(), null);

      // test DataElementUtil.element
      DataElement elementFromUtil = DataElementUtil.element(root, path);
      assertElementChainEquals(elementFromUtil, element, root);
      elementFromUtil = DataElementUtil.element(root, pathAsList);
      assertElementChainEquals(elementFromUtil, element, root);
      elementFromUtil = DataElementUtil.element(root, element.pathAsString());
      assertElementChainEquals(elementFromUtil, element, root);
      elementFromUtil = DataElementUtil.element(root, element.pathAsString('*'), '*');
      assertElementChainEquals(elementFromUtil, element, root);
      elementFromUtil = DataElementUtil.element(root.getValue(), root.getSchema(), path);
      assertElementChainEquals(elementFromUtil, element, root);
      elementFromUtil = DataElementUtil.element(root.getValue(), root.getSchema(), pathAsList);
      assertElementChainEquals(elementFromUtil, element, root);
      elementFromUtil = DataElementUtil.element(root.getValue(), root.getSchema(), element.pathAsString());
      assertElementChainEquals(elementFromUtil, element, root);
      elementFromUtil = DataElementUtil.element(root.getValue(), root.getSchema(), element.pathAsString('*'), '*');
      assertElementChainEquals(elementFromUtil, element, root);

      // test pathSpec
      String pathSpecString = (String) row[4];
      assertEquals(element.getSchemaPathSpec().toString(), pathSpecString);

    }
  }

  public void assertElementChainEquals(DataElement e1, DataElement e2, DataElement common)
  {
    while (e1 != null)
    {
      if (common != null && e1 == e2 && e1 == common)
      {
        return;
      }
      assertNotSame(e2, e1);
      assertTrue(e2 != null);
      assertSame(e2.getValue(), e1.getValue());
      assertSame(e2.getSchema(), e1.getSchema());
      assertEquals(e2.getName(), e1.getName());
      if (e2.getParent() == null)
      {
        assertSame(e2.getParent(), e1.getParent());
      }
      e1 = e1.getParent();
      e2 = e2.getParent();
    }
  }

  @Test
  public void testDataElementUtilElement() throws IOException
  {
    Object[][] input =
      {
        {
          "{ \"a\" : { \"b\" : 3 } }",
          new String[] { "/a", "/a/b" },
          new String[] { "/b", "/a/x", "/a/b/x" },
        },
        {
          "{ \"a\" : [ 0, \"abc\", { \"b\" : 3 } ] }",
          new String[] { "/a", "/a/0", "/a/1", "/a/2", "/a/2/b" },
          new String[] { "/a/b", "/a/0/x", "/a/1/x", "/a/2/x", "/a/2/b/x" }
        },
      };

    for (Object[] row : input)
    {
      DataMap map = dataMapFromString((String) row[0]);
      for (String goodPath : (String[]) row[1])
      {
        DataElement result = DataElementUtil.element(map, null, goodPath);
        assertNotSame(result, null);
        assertEquals(goodPath, result.pathAsString());
      }
      for (String badPath : (String[]) row[2])
      {
        DataElement result = DataElementUtil.element(map, null, badPath);
        assertSame(result, null);
      }
    }
  }

  @Test
  public void testPathToList()
  {
    Object[][] input =
      {
        {
          "",
          Collections.emptyList()
        },
        {
          "/a/b/c",
          TestUtil.asList("a", "b", "c")
        },
        {
          "/a",
          TestUtil.asList("a")
        },
        {
          "/a/b",
          TestUtil.asList("a", "b")
        }
      };

    for (Object[] row : input)
    {
      String path = ((String) row[0]);
      List<Object> list = DataElementUtil.pathToList(path, '/');
      assertEquals(list, row[1]);
      String path2 = path.replaceAll("/", "*");
      List<Object> list2 = DataElementUtil.pathToList(path2, '*');
      assertEquals(list2, row[1]);
    }
  }

  @Test
  public void testPathToListWithBadInput()
  {
    Object [][] input =
      {
        {
          "/",
          "Path component starting at index 0 of \"/\" is empty",
        },
        {
          "/a/",
          "Path component starting at index 2 of \"/a/\" is empty"
        },
        {
          "/a//b",
          "Path component starting at index 2 of \"/a//b\" is empty"
        },
        {
          "a/",
          "\"/\" expected at index 0 of \"a/\""
        }
      };

    for (Object[] row : input)
    {
      String path = ((String) row[0]);
      String expected = ((String) row[1]);
      Exception exc;
      try
      {
        exc = null;
        List<Object> list = DataElementUtil.pathToList(path, '/');
      }
      catch (Exception e)
      {
        exc = e;
      }
      assertTrue(exc instanceof IllegalArgumentException);
      assertEquals(exc.getMessage(), expected);

      String path2 = path.replaceAll("/", "*");
      try
      {
        exc = null;
        List<Object> list2 = DataElementUtil.pathToList(path2, '*');
      }
      catch (Exception e)
      {
        exc = e;
      }
      assertTrue(exc instanceof IllegalArgumentException);
      assertEquals(exc.getMessage(), expected.replace("/", "*"));
    }
  }
}
