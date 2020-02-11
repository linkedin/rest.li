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
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.junit.Assert.*;


public class TestSchemaToPdlEncoder
{

  @DataProvider
  private static Object[][] recordSchemaProvider()
  {
    DataMap properties1 = new DataMap();
    properties1.put("empty", new DataMap());
    DataMap properties2 = new DataMap();
    properties2.put("validate", properties1);
    DataMap properties3 = new DataMap();
    DataMap nestedMap = new DataMap(properties2);
    nestedMap.putAll(properties1);
    properties3.put("nested", nestedMap);
    return new Object[][]{{properties1, "namespace com.linkedin.test\n" + "\n" + "@empty = {}\nrecord RecordDataSchema {}"},
        {properties2, "namespace com.linkedin.test\n" + "\n" + "@validate.empty = {}\n" + "@empty = {}\nrecord RecordDataSchema {}"},
        {properties3, "namespace com.linkedin.test\n" + "\n" + "@nested = {\"validate\":{\"empty\":{}},\"empty\":{}}\n"
            + "@empty = {}\n" + "record RecordDataSchema {}"}};
  }

  @Test(dataProvider = "recordSchemaProvider")
  public void testEncodeRecordWithEmptyDataMapInProperty(Map<String, Object> properties, String pdlString) throws IOException
  {
    RecordDataSchema source =
        new RecordDataSchema(new Name("com.linkedin.test.RecordDataSchema"), RecordDataSchema.RecordType.RECORD);
    properties.put("empty", new DataMap());
    source.setProperties(properties);

    // schema to pdl
    StringWriter writer = new StringWriter();
    SchemaToPdlEncoder encoder = new SchemaToPdlEncoder(writer);
    encoder.setTypeReferenceFormat(SchemaToPdlEncoder.TypeReferenceFormat.PRESERVE);
    encoder.encode(source);

    Assert.assertEquals(pdlString, writer.toString());
    DataSchema encoded = TestUtil.dataSchemaFromPdlString(writer.toString());
    assertTrue(encoded instanceof RecordDataSchema);
    assertEquals(source.getProperties(), encoded.getProperties());
    assertEquals(source, encoded);
  }
}
