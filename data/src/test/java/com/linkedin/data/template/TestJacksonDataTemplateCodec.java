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

package com.linkedin.data.template;


import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.ArrayDataSchema;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.testng.annotations.Test;

import static com.linkedin.data.TestUtil.asList;
import static com.linkedin.data.TestUtil.asMap;
import static com.linkedin.data.template.TestRecordAndUnionTemplate.Foo;
import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertTrue;


/**
 * Test {@link JacksonDataTemplateCodec}
 */
public class TestJacksonDataTemplateCodec
{
  public String dataMapToString(DataMap map) throws IOException
  {
    return _jacksonDataTemplateCodec.mapToString(map);
  }

  public String templateToString(DataTemplate<?> template, boolean order) throws IOException
  {
    String resultFromString = _jacksonDataTemplateCodec.dataTemplateToString(template, order);

    StringWriter writer = new StringWriter();
    _jacksonDataTemplateCodec.writeDataTemplate(template, writer, order);
    String resultFromWriter = writer.toString();
    assertEquals(resultFromString, resultFromWriter);

    writer = new StringWriter();
    _jacksonDataTemplateCodec.writeDataTemplate(template.data(), template.schema(), writer, order);
    resultFromWriter = writer.toString();
    assertEquals(resultFromString, resultFromWriter);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    _jacksonDataTemplateCodec.writeDataTemplate(template, out, order);
    byte[] bytesFromOutputStream = out.toByteArray();
    assertTrue(Arrays.equals(bytesFromOutputStream, resultFromString.getBytes(Data.UTF_8_CHARSET)));

    out = new ByteArrayOutputStream();
    _jacksonDataTemplateCodec.writeDataTemplate(template.data(), template.schema(), out, order);
    bytesFromOutputStream = out.toByteArray();
    assertTrue(Arrays.equals(bytesFromOutputStream, resultFromString.getBytes(Data.UTF_8_CHARSET)));

    byte[] bytesFromBytes = _jacksonDataTemplateCodec.dataTemplateToBytes(template, order);
    assertTrue(Arrays.equals(bytesFromBytes, bytesFromOutputStream));

    byte[] bytesFromString = resultFromString.getBytes(Data.UTF_8_CHARSET);
    assertTrue(Arrays.equals(bytesFromString, bytesFromOutputStream));

    if (order == false)
    {
      String s = _jacksonDataTemplateCodec.dataTemplateToString(template);
      assertEquals(s, resultFromString);

      writer = new StringWriter();
      _jacksonDataTemplateCodec.writeDataTemplate(template, writer);
      assertEquals(writer.toString(), resultFromString);

      out = new ByteArrayOutputStream();
      _jacksonDataTemplateCodec.writeDataTemplate(template, out);
      assertTrue(Arrays.equals(out.toByteArray(), bytesFromString));

      byte[] bytes = _jacksonDataTemplateCodec.dataTemplateToBytes(template);
      assertTrue(Arrays.equals(bytes, bytesFromString));
    }

    return resultFromString;
  }

  public DataMap stringToDataMap(String s) throws IOException
  {
    return _jacksonDataTemplateCodec.stringToMap(s);
  }

  @Test
  public void testRecord() throws IOException
  {
    Object inputs[][] = {
      {
        // keys in record schema, number types
        asMap("int", 1, "long", 12L, "float", 3.0f, "double", 2.0),
        "{\"int\":1,\"long\":12,\"double\":2.0,\"float\":3.0}",
        "{\"int\":1,\"long\":12,\"float\":3.0,\"double\":2.0}"
      },
      {
        // keys not in record schema, number types
        asMap("int", 1, "long", 12L, "float", 3.0f, "double", 2.0, "long1", 12L, "double1", 3.0),
        "{\"long1\":12,\"int\":1,\"long\":12,\"double\":2.0,\"double1\":3.0,\"float\":3.0}",
        "{\"int\":1,\"long\":12,\"float\":3.0,\"double\":2.0,\"double1\":3.0,\"long1\":12}"
      },
      {
        // keys in record schema, string, boolean, bytes types
        asMap("string", "aaa", "boolean", true, "bytes", ByteString.copyAvroString("abc", false)),
        "{\"bytes\":\"abc\",\"string\":\"aaa\",\"boolean\":true}",
        "{\"boolean\":true,\"string\":\"aaa\",\"bytes\":\"abc\"}"
      },
      {
        // keys not in record schema, string, boolean, bytes types
        asMap("string", "aaa", "boolean", true, "bytes", ByteString.copyAvroString("abc", false), "string1", "xyz", "bytes1", ByteString.copyAvroString("XYZ", false)),
        "{\"bytes\":\"abc\",\"string1\":\"xyz\",\"string\":\"aaa\",\"bytes1\":\"XYZ\",\"boolean\":true}",
        "{\"boolean\":true,\"string\":\"aaa\",\"bytes\":\"abc\",\"bytes1\":\"XYZ\",\"string1\":\"xyz\"}"
      },
      {
        // record
        asMap("int", 1, "record", new DataMap(asMap("int", 2)), "long1", 12L, "double1", 3.0),
        "{\"record\":{\"int\":2},\"long1\":12,\"int\":1,\"double1\":3.0}",
        "{\"int\":1,\"record\":{\"int\":2},\"double1\":3.0,\"long1\":12}"
      },
      {
        // map
        asMap("int", 1, "map", new DataMap(asMap("a", 1, "b", 2, "c", 3)), "long1", 12L, "double1", 3.0),
        "{\"long1\":12,\"int\":1,\"map\":{\"b\":2,\"c\":3,\"a\":1},\"double1\":3.0}",
        "{\"int\":1,\"map\":{\"a\":1,\"b\":2,\"c\":3},\"double1\":3.0,\"long1\":12}"
      },
      {
        // empty record within union
        asMap("int", 1, "union", new DataMap(asMap("Foo", new DataMap())), "long1", 12L, "double1", 3.0),
        "{\"long1\":12,\"int\":1,\"union\":{\"Foo\":{}},\"double1\":3.0}",
        "{\"int\":1,\"union\":{\"Foo\":{}},\"double1\":3.0,\"long1\":12}"
      },
      {
        // non-empty record within union
        asMap("int", 1, "union", new DataMap(asMap("Foo", new DataMap(asMap("int", 1, "long", 12L, "float", 3.0f, "double", 2.0, "long1", 12L, "double1", 3.0)))), "long1", 12L, "double1", 3.0),
        "{\"long1\":12,\"int\":1,\"union\":{\"Foo\":{\"long1\":12,\"int\":1,\"long\":12,\"double\":2.0,\"double1\":3.0,\"float\":3.0}},\"double1\":3.0}",
        "{\"int\":1,\"union\":{\"Foo\":{\"int\":1,\"long\":12,\"float\":3.0,\"double\":2.0,\"double1\":3.0,\"long1\":12}},\"double1\":3.0,\"long1\":12}"
      },
      {
        // empty record array
        asMap("int", 1, "recordArray", new DataList(), "long1", 12L, "double1", 3.0),
        "{\"long1\":12,\"int\":1,\"double1\":3.0,\"recordArray\":[]}",
        "{\"int\":1,\"recordArray\":[],\"double1\":3.0,\"long1\":12}"
      },
      { // non-empty record array
        asMap("int", 1, "recordArray", new DataList(asList(new DataMap(asMap("int",
                                                                             1,
                                                                             "long",
                                                                             12L,
                                                                             "float",
                                                                             3.0f,
                                                                             "double",
                                                                             2.0,
                                                                             "long1",
                                                                             12L,
                                                                             "double1",
                                                                             3.0)))), "long1", 12L, "double1", 3.0),
        "{\"long1\":12,\"int\":1,\"double1\":3.0,\"recordArray\":[{\"long1\":12,\"int\":1,\"long\":12,\"double\":2.0,\"double1\":3.0,\"float\":3.0}]}",
        "{\"int\":1,\"recordArray\":[{\"int\":1,\"long\":12,\"float\":3.0,\"double\":2.0,\"double1\":3.0,\"long1\":12}],\"double1\":3.0,\"long1\":12}"
      },
      {
        // keys in record schema, string, boolean, bytes types
        asMap("string", Data.NULL, "boolean", true, "bytes", ByteString.copyAvroString("abc", false)),
        "{\"bytes\":\"abc\",\"string\":null,\"boolean\":true}",
        "{\"boolean\":true,\"string\":null,\"bytes\":\"abc\"}"
      }
    };

    for (Object[] row : inputs)
    {
      @SuppressWarnings("unchecked")
      DataMap map = new DataMap((Map<String,Object>) row[0]);
      Foo foo = new Foo(map);
      String raw = dataMapToString(map);
      String noOrder = templateToString(foo, false);
      String order = templateToString(foo, true);
      /* out.println(noOrder);
      out.println(order);
      out.println(); */
      assertEquals(noOrder, raw);
      assertEquals(noOrder, row[1]);
      assertEquals(order, row[2]);

      DataMap noOrderMap = stringToDataMap(noOrder);
      DataMap orderMap = stringToDataMap(order);
      DataMap rawMap = stringToDataMap(raw);
      assertEquals(rawMap, noOrderMap);
      assertEquals(noOrderMap, orderMap);
    }
  }

  public static class FooArray extends WrappingArrayTemplate<Foo>
  {
    public static final ArrayDataSchema SCHEMA = (ArrayDataSchema) Foo.FIELD_arrayRecord.getType();
    public FooArray()
    {
      this(new DataList());
    }
    public FooArray(int capacity)
    {
      this(new DataList(capacity));
    }
    public FooArray(Collection<Foo> c)
    {
      this(new DataList(c.size()));
      addAll(c);
    }
    public FooArray(DataList list)
    {
      super(list, SCHEMA, Foo.class);
    }
  }

  @Test
  public void testArray() throws IOException
  {
    Object inputs[][] = {
      {
        // empty record array
        asList(),
        "[]",
        "[]"
      },
      { // non-empty record array
        asList(new DataMap(asMap("int", 1, "long", 12L, "float", 3.0f, "double", 2.0, "long1", 12L, "double1", 3.0))),
        "[{\"long1\":12,\"int\":1,\"long\":12,\"double\":2.0,\"double1\":3.0,\"float\":3.0}]",
        "[{\"int\":1,\"long\":12,\"float\":3.0,\"double\":2.0,\"double1\":3.0,\"long1\":12}]"
      }
    };

    for (Object[] row : inputs)
    {
      @SuppressWarnings("unchecked")
      DataList list = new DataList((List<Object>) row[0]);
      FooArray fooArray = new FooArray(list);
      String noOrder = templateToString(fooArray, false);
      String order = templateToString(fooArray, true);
      /* out.println(noOrder);
      out.println(order);
      out.println(); */
      assertEquals(noOrder, row[1]);
      assertEquals(order, row[2]);
    }
  }

  @Test
  public void testMap() throws IOException
  {
    Object inputs[][] = {
      {
        // empty record array
        asMap(),
        "{}",
        "{}"
      },
      { // non-empty record array
        asMap("a", "aa", "b", "bb", "c", "cc"),
        "{\"b\":\"bb\",\"c\":\"cc\",\"a\":\"aa\"}",
        "{\"a\":\"aa\",\"b\":\"bb\",\"c\":\"cc\"}"
      }
    };

    for (Object[] row : inputs)
    {
      @SuppressWarnings("unchecked")
      DataMap map = new DataMap((Map<String,Object>) row[0]);
      StringMap stringMap = new StringMap(map);
      String noOrder = templateToString(stringMap, false);
      String order = templateToString(stringMap, true);
      /* out.println(noOrder);
      out.println(order);
      out.println(); */
      assertEquals(noOrder, row[1]);
      assertEquals(order, row[2]);
    }
  }

  private final JacksonDataTemplateCodec _jacksonDataTemplateCodec = new JacksonDataTemplateCodec();
}
