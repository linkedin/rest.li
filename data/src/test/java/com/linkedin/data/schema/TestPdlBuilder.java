/*
   Copyright (c) 2020 LinkedIn Corp.

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
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link PdlBuilder}.
 *
 * @author Aman Gupta
 */
public class TestPdlBuilder
{

  @DataProvider
  private static Object[][] propertiesMapProvider()
  {
    DataMap properties1 = new DataMap();
    properties1.put("empty", new DataMap());
    DataMap properties2 = new DataMap();
    properties2.put("validate", properties1);
    return new Object[][]
        {
            {
              properties1,
              "@empty = {}\n"
            },
            {
              properties2,
              "@validate.empty = {}\n"
            }
        //TODO Add test case for multiple properties in a map level once iteration logic is fixed to be deterministic
        };
  }

  @Test(dataProvider = "propertiesMapProvider")
  public void testWriteProperties(Map<String, Object> properties,
                                  String pdlString) throws IOException
  {
    StringWriter writer = new StringWriter();
    PdlBuilder pdlBuilder = (new IndentedPdlBuilder.Provider()).newInstance(writer);
    pdlBuilder.writeProperties(Collections.emptyList(), properties);

    Assert.assertEquals(pdlString, writer.toString());
  }
}
