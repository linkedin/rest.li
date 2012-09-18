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


import java.net.URI;
import java.util.Date;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;


public class TestCoercerClassInitialization
{
  public static class StaticBlockInitializerCoercer implements DirectCoercer<Date>
  {
    static
    {
      Custom.registerCoercer(new StaticBlockInitializerCoercer(), Date.class);
    }

    @Override
    public Object coerceInput(Date object)
      throws ClassCastException
    {
      return object.getTime();
    }

    @Override
    public Date coerceOutput(Object object)
      throws TemplateOutputCastException
    {
      return new Date(((Number) object).longValue());
    }
  }

  public static class StaticVariableInitializerCoercer implements DirectCoercer<URI>
  {
    private static final boolean REGISTER_COERCER = Custom.registerCoercer(new StaticVariableInitializerCoercer(), URI.class);

    private StaticVariableInitializerCoercer()
    {
    }

    @Override
    public Object coerceInput(URI object)
      throws ClassCastException
    {
      return object.toString();
    }

    @Override
    public URI coerceOutput(Object object)
      throws TemplateOutputCastException
    {
      return URI.create((String) object);
    }
  }

  @BeforeSuite
  public void testInitialization()
  {
    assertFalse(DataTemplateUtil.hasCoercer(Date.class));
    Custom.initializeCoercerClass(StaticBlockInitializerCoercer.class);
    assertTrue(DataTemplateUtil.hasCoercer(Date.class));

    assertFalse(DataTemplateUtil.hasCoercer(URI.class));
    Custom.initializeCoercerClass(StaticVariableInitializerCoercer.class);
    assertTrue(DataTemplateUtil.hasCoercer(URI.class));
  }

  @Test
  public void testStaticBlockInitializerCoercer()
  {
    Date now = new Date();
    Long nowLong = (Long) DataTemplateUtil.coerceInput(now, Date.class, Long.class);
    assertEquals(now.getTime(), nowLong.longValue());
    Date nowOutput = DataTemplateUtil.coerceOutput(nowLong, Date.class);
    assertEquals(now, nowOutput);
  }

  @Test
  public void testStaticVariableInitializerCoercer()
  {
    String testUriString = "http://www.linkedin.com";

    URI uri = URI.create(testUriString);
    String uriString = (String) DataTemplateUtil.coerceInput(uri, URI.class, String.class);
    assertEquals(uri, URI.create(uriString));
    URI uriOutput = DataTemplateUtil.coerceOutput(uriString, URI.class);
    assertEquals(uri, uriOutput);
  }
}
