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

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
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
    DataMap emptyProperty = new DataMap();
    emptyProperty.put("empty", new DataMap());
    DataMap arrayValueProperty = new DataMap();
    arrayValueProperty.put("array", new DataList(Arrays.asList(1, 2, 3)));
    DataMap flattenProperty = new DataMap();
    flattenProperty.put("flatten", arrayValueProperty);
    DataMap multipleProp = new DataMap();
    multipleProp.putAll(emptyProperty);
    multipleProp.putAll(arrayValueProperty);
    DataMap jsonValueProp = new DataMap();
    jsonValueProp.put("nested", multipleProp);
    return new Object[][]
        {
            {
              emptyProperty,
              "@empty = { }\n",
              "@empty={}"
            },
            {
              arrayValueProperty,
              "@`array` = [ 1, 2, 3 ]\n",
              "@`array`=[1,2,3]"
            },
            {
              flattenProperty,
              "@flatten.`array` = [ 1, 2, 3 ]\n",
              "@flatten.`array`=[1,2,3]"
            },
            /* TODO Add test case for multiple properties in a map level once iteration logic is fixed to be deterministic
            {
              jsonValueProp,
              "@nested = {\n  \"array\" : [ 1, 2, 3 ],\n  \"empty\" : { }\n}\n",
              "@nested={\"array\":[1,2,3],\"empty\":{}}"
            }*/
        };
  }

  @Test(dataProvider = "propertiesMapProvider")
  public void testWriteProperties(Map<String, Object> properties,
                                  String indentPdlString,
                                  String compactPdlString) throws IOException
  {
    StringWriter indentWriter = new StringWriter();
    PdlBuilder indentPdlBuilder = (new IndentedPdlBuilder.Provider()).newInstance(indentWriter);
    indentPdlBuilder.writeProperties(Collections.emptyList(), properties);

    StringWriter compactWriter = new StringWriter();
    PdlBuilder compactPdlBuilder = (new CompactPdlBuilder.Provider()).newInstance(compactWriter);
    compactPdlBuilder.writeProperties(Collections.emptyList(), properties);

    Assert.assertEquals(indentPdlString, indentWriter.toString());
    Assert.assertEquals(compactPdlString, compactWriter.toString());
  }
}
