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

package com.linkedin.data.schema;

import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.testng.annotations.Test;

import static org.junit.Assert.*;


public class TestSchemaToPdlEncoder
{

  @Test
  public void testEncodeRecordWithEmptyDataMapInProperty() throws IOException
  {
    RecordDataSchema source =
        new RecordDataSchema(new Name("com.linkedin.test.RecordDataSchema"), RecordDataSchema.RecordType.RECORD);
    Map<String, Object> properties = new HashMap<>();
    properties.put("empty", new DataMap());
    source.setProperties(properties);

    // schema to pdl
    StringWriter writer = new StringWriter();
    SchemaToPdlEncoder encoder = new SchemaToPdlEncoder(writer);
    encoder.setTypeReferenceFormat(SchemaToPdlEncoder.TypeReferenceFormat.PRESERVE);
    encoder.encode(source);

    DataSchema encoded = TestUtil.dataSchemaFromPdlString(writer.toString());
    assertTrue(encoded instanceof RecordDataSchema);
    assertEquals(source.getProperties(), encoded.getProperties());
    assertEquals(source, encoded);
  }

  @Test
  public void testEncodeSortsNestedPropertyMap() throws IOException
  {
    String inputSchema = String.join("\n",
        "@nested = {",
        "  \"c\" : [ \"z\", \"y\" ],",
        "  \"b\" : \"b\",",
        "  \"a\" : \"a\"",
        "}",
        "record A {}");

    DataSchema schema = TestUtil.dataSchemaFromPdlString(inputSchema);

    String indentedSchema = SchemaToPdlEncoder.schemaToPdl(schema, SchemaToPdlEncoder.EncodingStyle.INDENTED);

    assertEquals(String.join("\n",
        "@nested = {",
        "  \"a\" : \"a\",",
        "  \"b\" : \"b\",",
        "  \"c\" : [ \"z\", \"y\" ]",
        "}",
        "record A {}"), indentedSchema);

    String compactSchema = SchemaToPdlEncoder.schemaToPdl(schema, SchemaToPdlEncoder.EncodingStyle.COMPACT);

    assertEquals("@nested={\"a\":\"a\",\"b\":\"b\",\"c\":[\"z\",\"y\"]}record A{}", compactSchema);
  }

  @Test
  public void testEncodeSortsMultiLevelNestedPropertyMap() throws IOException
  {
    String inputSchema = String.join("\n",
        "@nested = {",
        "  \"b\" : \"b\",",
        "  \"a\" : {",
        "    \"d\" : \"d\",",
        "    \"c\" : \"c\"",
        "  }",
        "}",
        "record A {}");

    DataSchema schema = TestUtil.dataSchemaFromPdlString(inputSchema);

    String indentedSchema = SchemaToPdlEncoder.schemaToPdl(schema, SchemaToPdlEncoder.EncodingStyle.INDENTED);

    assertEquals(String.join("\n",
        "@nested = {",
        "  \"a\" : {",
        "    \"c\" : \"c\",",
        "    \"d\" : \"d\"",
        "  },",
        "  \"b\" : \"b\"",
        "}",
        "record A {}"), indentedSchema);

    String compactSchema = SchemaToPdlEncoder.schemaToPdl(schema, SchemaToPdlEncoder.EncodingStyle.COMPACT);

    assertEquals("@nested={\"a\":{\"c\":\"c\",\"d\":\"d\"},\"b\":\"b\"}record A{}", compactSchema);
  }

  @Test
  public void testEncodeDefaultValueFieldsInSchemaOrder() throws IOException
  {
    String inputSchema = String.join("\n",
        "record A {",
        "",
        "  b: record B {",
        "    b1: string",
        "",
        "    c: record C {",
        "      c2: int",
        "      c1: boolean",
        "",
        "      c3: array[string]",
        "    }",
        "    b2: double",
        "  } = {",
        "    \"b1\" : \"hello\",",
        "    \"b2\" : 0.05,",
        "    \"c\" : {",
        "      \"c1\" : true,",
        "      \"c2\" : 100,",
        "      \"c3\" : [ \"one\", \"two\" ]",
        "    }",
        "  }",
        "}");

    DataSchema schema = TestUtil.dataSchemaFromPdlString(inputSchema);

    String indentedSchema = SchemaToPdlEncoder.schemaToPdl(schema, SchemaToPdlEncoder.EncodingStyle.INDENTED);

    assertEquals(String.join("\n",
        "record A {",
        "",
        "  b: record B {",
        "    b1: string",
        "",
        "    c: record C {",
        "      c2: int",
        "      c1: boolean",
        "",
        "      c3: array[string]",
        "    }",
        "    b2: double",
        "  } = {",
        "    \"b1\" : \"hello\",",
        "    \"c\" : {",
        "      \"c2\" : 100,",
        "      \"c1\" : true,",
        "      \"c3\" : [ \"one\", \"two\" ]",
        "    },",
        "    \"b2\" : 0.05",
        "  }",
        "}"), indentedSchema);

    String compactSchema = SchemaToPdlEncoder.schemaToPdl(schema, SchemaToPdlEncoder.EncodingStyle.COMPACT);

    assertEquals(Stream.of(
        "record A{",
        "  b:record B{",
        "    b1:string,",
        "    c:record C{",
        "    c2:int,",
        "    c1:boolean,",
        "    c3:array[string]",
        "  }",
        "  b2:double",
        "  }={",
        "    \"b1\":\"hello\",",
        "    \"c\":{",
        "      \"c2\":100,",
        "      \"c1\":true,",
        "      \"c3\":[\"one\",\"two\"]",
        "    },",
        "    \"b2\":0.05",
        "  }",
        "}")
        .map(String::trim)
        .collect(Collectors.joining()), compactSchema);
  }
}
