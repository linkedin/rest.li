package com.linkedin.data.schema;

import com.linkedin.data.DataMap;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestPdlBuilder {

  @DataProvider
  private static Object[][] propertiesMapProvider()
  {
    DataMap properties1 = new DataMap();
    properties1.put("empty", new DataMap());
    DataMap properties2 = new DataMap();
    properties2.put("validate", properties1);
    DataMap properties3 = new DataMap();
    DataMap nestedMap = new DataMap(properties2);
    nestedMap.putAll(properties1);
    properties3.put("nested", nestedMap);
    return new Object[][]{{properties1, "@empty = {}\n"},
        {properties2, "@validate.empty = {}\n"}
        //TODO Add test case for multiple properties in a map level once iteration logic is fixed to be deterministic
    };
  }

  @Test(dataProvider = "propertiesMapProvider")
  public void testPropertiesWriter(Map<String, Object> properties, String pdlString) throws IOException
  {
    StringWriter writer = new StringWriter();
    PdlBuilder pdlBuilder = (new IndentedPdlBuilder.Provider()).newInstance(writer);
    pdlBuilder.writeProperties(Collections.emptyList(), properties);

    Assert.assertEquals(pdlString, writer.toString());
  }
}
