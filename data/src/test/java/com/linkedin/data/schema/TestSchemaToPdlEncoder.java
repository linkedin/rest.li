package com.linkedin.data.schema;

import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import org.testng.annotations.Test;

import static org.junit.Assert.*;


public class TestSchemaToPdlEncoder {

  @Test
  public void testEncodeRecordWithEmptyDatamapInProperty() throws IOException {
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
