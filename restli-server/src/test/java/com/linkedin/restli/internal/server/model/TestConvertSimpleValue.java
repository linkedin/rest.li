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

/**
 * $Id: $
 */

package com.linkedin.restli.internal.server.model;

import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.TemplateOutputCastException;
import com.linkedin.restli.internal.server.util.ArgumentUtils;
import com.linkedin.restli.server.CustomFixedLengthStringRef;
import com.linkedin.restli.server.CustomLongRef;
import com.linkedin.restli.server.CustomStringRef;
import com.linkedin.restli.server.custom.types.CustomFixedLengthString;
import com.linkedin.restli.server.custom.types.CustomLong;
import com.linkedin.restli.server.custom.types.CustomString;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class TestConvertSimpleValue
{
  @Test
  public void testError()
  {
    try
    {
      Class<?> intClass = Integer.class;
      ArgumentUtils.convertSimpleValue("notAnInt", DataTemplateUtil.getSchema(intClass), intClass);
      Assert.fail("expected NumberFormatException");
    }
    catch (NumberFormatException e)
    {
      // expected error
    }

  }

  @Test
  public void testSimpleLong()
  {
    Class<?> longClass = Long.class;
    Object convertedLong = ArgumentUtils.convertSimpleValue("100",
                                                            DataTemplateUtil.getSchema(longClass),
                                                            longClass
    );

    Assert.assertTrue(convertedLong.getClass().equals(longClass));
    Assert.assertEquals(convertedLong, 100L);
  }

  @Test
  public void testSimpleDouble()
  {
    Class<?> doubleClass = Double.class;
    Object convertedLong = ArgumentUtils.convertSimpleValue("100.2",
                                                            DataTemplateUtil.getSchema(doubleClass),
                                                            doubleClass
    );

    Assert.assertTrue(convertedLong.getClass().equals(doubleClass));
    Assert.assertEquals(convertedLong, 100.2);
  }

  @Test
  public void testSimpleInt()
  {
    Class<?> intClass = Integer.class;
    Object convertedInt = ArgumentUtils.convertSimpleValue("100",
                                                           DataTemplateUtil.getSchema(intClass),
                                                           intClass
    );

    Assert.assertTrue(convertedInt.getClass().equals(intClass));
    Assert.assertEquals(convertedInt, 100);

  }

  @Test
  public void testSimpleBoolean()
  {
    Class<?> boolClass = Boolean.class;
    Object convertedBool = ArgumentUtils.convertSimpleValue("true",
                                                           DataTemplateUtil.getSchema(boolClass),
                                                           boolClass
    );

    Assert.assertTrue(convertedBool.getClass().equals(boolClass));
    Assert.assertTrue((Boolean)convertedBool);

  }

  @Test
  public void testConvertCustomLong()
  {
    CustomLong forceRegistration = new CustomLong(0L);

    Class<?> customLongClass = CustomLong.class;
    Object convertedCustomLong = ArgumentUtils.convertSimpleValue("100",
                                                                  DataTemplateUtil.getSchema(CustomLongRef.class),
                                                                  customLongClass
    );

    Assert.assertTrue(convertedCustomLong.getClass().equals(customLongClass));
    CustomLong customLong = (CustomLong) convertedCustomLong;
    Assert.assertTrue(customLong.toLong().equals(Long.valueOf(100)));
  }

  @Test
  public void testConvertCustomString()
  {
    CustomString forceRegistration = new CustomString("");

    Class<?> customStringClass = CustomString.class;
    Object convertedCustomString = ArgumentUtils.convertSimpleValue("aString",
                                                                  DataTemplateUtil.getSchema(CustomStringRef.class),
                                                                  customStringClass
    );

    Assert.assertTrue(convertedCustomString.getClass().equals(customStringClass));
    CustomString customString = (CustomString) convertedCustomString;
    Assert.assertTrue(customString.toString().equals("aString"));

  }

  @Test(expectedExceptions = TemplateOutputCastException.class)
  public void testConvertCustomFixedLengthStringError()
  {
    CustomFixedLengthString forceRegistration = new CustomFixedLengthString("");

    Class<?> customStringClass = CustomFixedLengthString.class;
    ArgumentUtils
        .convertSimpleValue("abcd", DataTemplateUtil.getSchema(CustomFixedLengthStringRef.class), customStringClass);
  }

}
