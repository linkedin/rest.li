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

package com.linkedin.pegasus.generator.spec;

import com.linkedin.data.schema.SchemaFormatType;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Tests for {@link ClassTemplateSpec}.
 *
 * @author Evan Williams
 */
public class TestClassTemplateSpec
{
  @DataProvider(name = "getSourceFileFormatData")
  private Object[][] provideGetSourceFileFormatData()
  {
    return new Object[][]
        {
            { new String[] { "Foo.pdl", null, null }, SchemaFormatType.PDL },
            { new String[] { "/some/path/to/Two.dots.pdl", "dummy", null }, SchemaFormatType.PDL },
            { new String[] { "Foo.pdsc" }, SchemaFormatType.PDSC },
            { new String[] { "Foo.pdsc", null, "Bar.pdl" }, SchemaFormatType.PDSC },
            { new String[] { "Foo.restspec.json", null, null }, null },
            { new String[] { "Foo.restspec.json", "Foo.pdsc", "Bar.pdl" }, null },
            { new String[] { null, null, null, null, "IgnoreMe.pdsc" }, null }
        };
  }

  /**
   * Tests {@link ClassTemplateSpec#getSourceFileFormat()}.
   *
   * Ensures that nested class template specs correctly calculate their source format type as that of their oldest
   * parent, where the oldest parent's source format type is calculated using the file extension of its location.
   * Constructs a nested chain of class template specs and verifies for each that its calculated source format type
   * is expected.
   *
   * @param locations location of each class template spec to construct (first = outermost, last = innermost).
   * @param expected expected source file format for each class template spec.
   */
  @Test(dataProvider = "getSourceFileFormatData")
  public void testGetSourceFileFormat(String[] locations, SchemaFormatType expected)
  {
    ClassTemplateSpec enclosingClassTemplateSpec = null;
    for (String location : locations)
    {
      ClassTemplateSpec classTemplateSpec = new ClassTemplateSpec();
      classTemplateSpec.setLocation(location);
      classTemplateSpec.setEnclosingClass(enclosingClassTemplateSpec);

      Assert.assertEquals(classTemplateSpec.getSourceFileFormat(), expected);
      enclosingClassTemplateSpec = classTemplateSpec;
    }
  }
}
