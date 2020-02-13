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
}
