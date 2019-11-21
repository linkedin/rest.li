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

import com.linkedin.data.schema.grammar.PdlSchemaParserFactory;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * Tests for {@link SchemaFormatType}.
 *
 * @author Evan Williams
 */
public class TestSchemaFormatType
{
  @Test
  public void testGetSchemaParserFactory()
  {
    Assert.assertSame(SchemaFormatType.PDSC.getSchemaParserFactory(), SchemaParserFactory.instance());
    Assert.assertSame(SchemaFormatType.PDL.getSchemaParserFactory(), PdlSchemaParserFactory.instance());
  }

  @Test
  public void testFromFilename()
  {
    Assert.assertEquals(SchemaFormatType.fromFilename("Foo.pdsc"), SchemaFormatType.PDSC);
    Assert.assertEquals(SchemaFormatType.fromFilename("Bar.pdl"), SchemaFormatType.PDL);
    Assert.assertEquals(SchemaFormatType.fromFilename("Two.dots.pdsc"), SchemaFormatType.PDSC);
    Assert.assertEquals(SchemaFormatType.fromFilename("/some/path/with/Two.dots.pdl"), SchemaFormatType.PDL);
    Assert.assertEquals(SchemaFormatType.fromFilename(".pdl"), SchemaFormatType.PDL);
    Assert.assertNull(SchemaFormatType.fromFilename("Baz.json"));
    Assert.assertNull(SchemaFormatType.fromFilename("Biz"));
    Assert.assertNull(SchemaFormatType.fromFilename("Bop."));
    Assert.assertNull(SchemaFormatType.fromFilename("."));
    Assert.assertNull(SchemaFormatType.fromFilename(""));
    Assert.assertNull(SchemaFormatType.fromFilename(null));
  }

  @Test
  public void testFromFileExtension()
  {
    Assert.assertEquals(SchemaFormatType.fromFileExtension("pdsc"), SchemaFormatType.PDSC);
    Assert.assertEquals(SchemaFormatType.fromFileExtension("pdl"), SchemaFormatType.PDL);
    Assert.assertEquals(SchemaFormatType.fromFileExtension("PdsC"), SchemaFormatType.PDSC);
    Assert.assertEquals(SchemaFormatType.fromFileExtension("PDL"), SchemaFormatType.PDL);
    Assert.assertNull(SchemaFormatType.fromFileExtension("json"));
    Assert.assertNull(SchemaFormatType.fromFileExtension(""));
    Assert.assertNull(SchemaFormatType.fromFileExtension(null));
  }
}
