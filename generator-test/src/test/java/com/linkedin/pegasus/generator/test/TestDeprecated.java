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


import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * Verify that deprecated annotations in "Deprecated.pdsc" are correctly represented as @Deprecated
 * annotations on the generated java data templates.
 */
public class TestDeprecated
{
  @SuppressWarnings("deprecation")
  @Test
  public void testDeprecatedTypes()
  {
    Assert.assertNotNull(Deprecated.class.getAnnotation(java.lang.Deprecated.class));
    Assert.assertNotNull(DeprecatedEnum.class.getAnnotation(java.lang.Deprecated.class));
    Assert.assertNotNull(DeprecatedFixed.class.getAnnotation(java.lang.Deprecated.class));
    Assert.assertNotNull(DeprecatedTyperef.class.getAnnotation(java.lang.Deprecated.class));
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testDeprecatedMethods() throws Exception
  {
    Map<String, Class<?>> fields = new HashMap<String, Class<?>>();
    fields.put("DeprecatedInt", int.class);
    fields.put("Sample", int.class);
    fields.put("SampleTyperef", Double.class);
    fields.put("SampleEnum", DeprecatedEnum.class);
    fields.put("SampleFixed", DeprecatedFixed.class);

    for(Map.Entry<String, Class<?>> field: fields.entrySet())
    {
      String name = field.getKey();
      Class<?> type = field.getValue();
      Assert.assertNotNull(Deprecated.class.getMethod("get" + name).getAnnotation(java.lang.Deprecated.class));
      Assert.assertNotNull(Deprecated.class.getMethod("set" + name, type).getAnnotation(java.lang.Deprecated.class));
      Assert.assertNotNull(Deprecated.class.getMethod("has" + name).getAnnotation(java.lang.Deprecated.class));
      Assert.assertNotNull(Deprecated.class.getMethod("remove" + name).getAnnotation(java.lang.Deprecated.class));
    }
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testDeprecatedEnum() throws Exception
  {
    Assert.assertNotNull(DeprecatedEnum.ONE.getClass().getAnnotation(java.lang.Deprecated.class));
    Assert.assertNotNull(DeprecatedEnum.TWO.getClass().getAnnotation(java.lang.Deprecated.class));
  }
}
