/*
   Copyright (c) 2013 LinkedIn Corp.

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

package com.linkedin.pegasus.generator.override;


import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.schema.FixedDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.FixedTemplate;
import com.linkedin.data.template.TestDataTemplateUtil;
import org.testng.annotations.Test;

import java.lang.reflect.Constructor;

import static org.testng.Assert.*;


/**
 * @author Min Chen
 */
public class TestFixed
{
  private <T extends FixedTemplate> void testFixed(Class<T> fixedClass)
  {
    try
    {
      // check for ByteString constructor
      Constructor<T> byteStringConstructor = fixedClass.getConstructor(ByteString.class);

      // check for Object constructor
      Constructor<T> objectConstructor = fixedClass.getConstructor(Object.class);

      // has embedded FixedDataSchema
      FixedDataSchema schema = (FixedDataSchema) DataTemplateUtil.getSchema(fixedClass);

      // get size of fixed
      int size = schema.getSize();

      // create input value
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < size; i++)
      {
        sb.append((char) ('a' + i % 26));
      }
      String stringValue = sb.toString();
      ByteString byteStringValue = ByteString.copy(stringValue.getBytes(Data.UTF_8_CHARSET));

      // Object ctor, value is String
      T fixed = objectConstructor.newInstance(stringValue);
      assertEquals(fixed.data(), byteStringValue);
      assertSame(fixed.data(), fixed.bytes());

      // Object ctor, value is ByteString
      fixed = objectConstructor.newInstance(byteStringValue);
      assertSame(fixed.data(), byteStringValue);
      assertSame(fixed.data(), fixed.bytes());

      // ByteString ctor
      fixed = byteStringConstructor.newInstance(byteStringValue);
      assertSame(fixed.data(), byteStringValue);
      assertSame(fixed.data(), fixed.bytes());

      // schema()
      assertSame(fixed.schema(), schema);

      // toString()
      assertEquals(fixed.toString(), byteStringValue.toString());

      // check for clone and copy override with correct return type
      TestDataTemplateUtil.assertCloneAndCopyReturnType(fixedClass);

      // test clone
      FixedTemplate fixedClone = fixed.clone();
      assertSame(fixedClone.getClass(), fixed.getClass());
      assertSame(fixedClone.bytes(), fixed.bytes());

      // test copy
      FixedTemplate fixedCopy = fixed.clone();
      assertSame(fixedCopy.getClass(), fixed.getClass());
      assertSame(fixedCopy.bytes(), fixed.bytes());
    }
    catch (Exception exc)
    {
      fail("Unexpected exception", exc);
    }
  }

  @Test
  public void testFixed()
  {
    testFixed(Fixed16.class);
    testFixed(FixedMD5.class);
    testFixed(FixedInUnion.class);
  }
}
