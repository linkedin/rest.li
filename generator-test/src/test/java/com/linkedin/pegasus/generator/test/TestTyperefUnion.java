/*
   Copyright (c) 2012 LinkedIn Corp.

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

package com.linkedin.pegasus.generator.test;


import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.HasTyperefInfo;
import com.linkedin.data.template.TestCustom;
import com.linkedin.data.template.TestRecordAndUnionTemplate;
import com.linkedin.data.template.TyperefInfo;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public class TestTyperefUnion
{
  @Test
  public void testTyperefUnion()
  {
    TyperefInfo typerefInfo = DataTemplateUtil.getTyperefInfo(Union.class);
    assertNotNull(typerefInfo);
    TyperefDataSchema typerefDataSchema = typerefInfo.getSchema();

    Union union = new Union();
    assertTrue(union instanceof HasTyperefInfo);

    TyperefInfo typerefInfoFromInstance = union.typerefInfo();
    assertNotNull(typerefInfoFromInstance);
    TyperefDataSchema typerefDataSchemaFromInstance = typerefInfo.getSchema();

    assertSame(typerefDataSchemaFromInstance, typerefDataSchema);
    assertSame(typerefInfoFromInstance, typerefInfo);

    assertEquals(typerefDataSchema.getFullName(), Union.class.getName());
    assertEquals(typerefDataSchema.getRef(), DataTemplateUtil.getSchema(Union.class));
  }

  @Test
  public void testNonTyperefUnion()
  {
    TyperefInfo typerefInfo = DataTemplateUtil.getTyperefInfo(TestRecordAndUnionTemplate.Foo.Union.class);
    assertNull(typerefInfo);

    TestRecordAndUnionTemplate.Foo.Union union = new TestRecordAndUnionTemplate.Foo.Union();
    assertFalse(union instanceof HasTyperefInfo);
  }

  @Test
  public void testRegisterCustomCoercer()
  {
    new UnionTyperef2();
    assertTrue(DataTemplateUtil.hasCoercer(TestCustom.CustomNumber.class));
  }
}
