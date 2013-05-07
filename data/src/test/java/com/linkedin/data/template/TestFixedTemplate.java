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

package com.linkedin.data.template;


import com.linkedin.data.ByteString;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.FixedDataSchema;
import org.testng.annotations.Test;

import static org.testng.Assert.*;
import static org.testng.Assert.assertEquals;


/**
 * Unit tests for {@link FixedTemplate}.
 *
 * @author slim
 */
public class TestFixedTemplate
{
  public static class Fixed5 extends FixedTemplate
  {

    private final static FixedDataSchema SCHEMA = ((FixedDataSchema) DataTemplateUtil.parseSchema("{\"type\":\"fixed\",\"name\":\"Fixed5\",\"namespace\":\"com.linkedin.rpc.demo\",\"size\":5}"));

    public Fixed5(ByteString value)
    {
      super(value, SCHEMA);
    }

    public Fixed5(Object data)
    {
      super(data, SCHEMA);
    }

    @Override
    public Fixed5 clone() throws CloneNotSupportedException
    {
      return (Fixed5) super.clone();
    }

    @Override
    public Fixed5 copy() throws CloneNotSupportedException
    {
      return (Fixed5) super.copy();
    }
  }

  @Test
  public void testFixedTemplate()
  {
    String goodObjects[] = {
        "12345",
        "ABCDF"
    };

    ByteString goodByteStrings[] = {
        ByteString.copyAvroString("qwert", false)
    };

    Object badObjects[] = {
        "", "1", "12", "123", "1234", "1234\u0100", "123456",
        1, 2.0f, 3.0, 4L, new DataMap(), new DataList()
    };

    ByteString badByteStrings[]  = {
        ByteString.copyAvroString("", false),
        ByteString.copyAvroString("a", false),
        ByteString.copyAvroString("ab", false),
        ByteString.copyAvroString("abc", false),
        ByteString.copyAvroString("abcd", false),
        ByteString.copyAvroString("abcdef", false)
    };

    Integer lastHashCode = null;
    ByteString lastByteString = null;
    for (String o : goodObjects)
    {
      Exception exc = null;
      Fixed5 fixed = null;
      try
      {
        fixed = new Fixed5(o);
      }
      catch (Exception e)
      {
        exc = e;
      }
      assertNull(exc);

      // equals
      ByteString expectedByteString = ByteString.copyAvroString(o, false);
      assertEquals(fixed.data(), expectedByteString);
      assertTrue(fixed.equals(new Fixed5(expectedByteString)));
      if (lastByteString != null)
      {
        assertFalse(fixed.equals(lastByteString));
      }
      assertFalse(fixed.equals(null));
      assertFalse(fixed.equals(new Object()));

      // hashCode
      int newHashCode = fixed.hashCode();
      if (lastHashCode != null)
      {
        assertTrue(newHashCode != lastHashCode);
      }

      // toString
      assertEquals(expectedByteString.asAvroString(), fixed.toString());

      lastHashCode = newHashCode;
      lastByteString = expectedByteString;

      // clone and copy
      testCopiers(fixed);
    }

    for (ByteString o : goodByteStrings)
    {
      Exception exc = null;
      Fixed5 fixed = null;
      try
      {
        fixed = new Fixed5(o);
      }
      catch (Exception e)
      {
        exc = e;
      }
      assertNull(exc);

      // equals
      assertEquals(fixed.data(), o);
      assertTrue(fixed.equals(new Fixed5(o)));
      if (lastByteString != null)
      {
        assertFalse(fixed.equals(lastByteString));
      }
      assertFalse(fixed.equals(null));
      assertFalse(fixed.equals(new Object()));

      // hashCode
      int newHashCode = fixed.hashCode();
      if (lastHashCode != null)
      {
        assertTrue(newHashCode != lastHashCode);
      }

      // toString
      assertEquals(o.asAvroString(), fixed.toString());

      lastHashCode = newHashCode;
      lastByteString = o;

      // clone and copy
      testCopiers(fixed);
    }

    for (Object o : badObjects)
    {
      Exception exc = null;
      try
      {
        new Fixed5(o);
      }
      catch (Exception e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      assertTrue(exc instanceof TemplateOutputCastException);
    }

    for (ByteString o : badByteStrings)
    {
      Exception exc = null;
      try
      {
        new Fixed5(o);
      }
      catch (Exception e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      assertTrue(exc instanceof TemplateOutputCastException);
    }
  }

  @Test
  public void testWrapping()
      throws InstantiationException, IllegalAccessException
  {
    String input = "12345";

    ByteString input1 = ByteString.copyAvroString(input, false);
    Fixed5 fixed1 = DataTemplateUtil.wrap(input1, Fixed5.class);
    assertSame(input1, fixed1.data());

    ByteString input2 = ByteString.copyAvroString("67890", false);
    Fixed5 fixed2 = DataTemplateUtil.wrap(input2, Fixed5.SCHEMA, Fixed5.class);
    assertSame(input2, fixed2.data());

    Fixed5 fixed3 =  DataTemplateUtil.wrap(input, Fixed5.class);
    assertEquals(fixed1, fixed3);

    Fixed5 fixed4 = DataTemplateUtil.wrap(input, Fixed5.SCHEMA, Fixed5.class);
    assertEquals(fixed3, fixed4);
  }

  private void testCopiers(Fixed5 fixed)
  {
    // clone and copy
    Exception exc = null;
    try
    {
      Fixed5 fixedClone = fixed.clone();
      assertEquals(fixedClone, fixed);
      assertSame(fixedClone.data(), fixed.data());

      Fixed5 fixedCopy = fixed.copy();
      assertEquals(fixedCopy, fixed);
      assertSame(fixedCopy.data(), fixed.data());
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertNull(exc);
  }
}
