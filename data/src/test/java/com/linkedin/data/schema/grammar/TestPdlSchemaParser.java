package com.linkedin.data.schema.grammar;

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import com.linkedin.data.schema.DataSchema;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.testng.annotations.Test;

import static org.junit.Assert.*;


public class TestPdlSchemaParser {

  @Test
  public void testParseNestedProperties() throws IOException {
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
    assertNotNull(encoded);
    TestUtil.assertEquivalent(encoded.getProperties(), expected);
  }
}
