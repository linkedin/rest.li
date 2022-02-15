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

package com.linkedin.data.avro;


import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;


public class TestAvroAdapterFinder
{
  // Don't constants from AvroAdapterFinder because change of the
  // constant is an incompatible change and we want these tests to catch
  // such incompatible changes.
  private final String CHOOSER_PROPERTY = "com.linkedin.data.avro.AvroAdapterChooser";
  private final String ADAPTER_PROPERTY = "com.linkedin.data.avro.AvroAdapter";

  private final static AvroAdapter _avroAdapter = AvroAdapterFinder.getAvroAdapter();

  @BeforeMethod @AfterMethod
  private void clearProperties()
  {
    System.clearProperty(CHOOSER_PROPERTY);
    System.clearProperty(ADAPTER_PROPERTY);
  }

  @Test
  public void testDefaultAvroAdapter()
  {
    assertEquals(_avroAdapter.getClass(), AvroAdapter_1_4.class);
  }

  @Test
  public void testChooserProperty()
  {
    System.setProperty(CHOOSER_PROPERTY, TestAvroAdapterChooser.class.getName());
    AvroAdapter avroAdapter = AvroAdapterFinder.avroAdapter();
    assertEquals(avroAdapter.getClass(), TestAvroAdapterChooser.MyAvroAdapter.class);
  }

  @Test
  public void testAdapterProperty()
  {
    System.setProperty(ADAPTER_PROPERTY, TestAvroAdapter.class.getName());
    AvroAdapter avroAdapter = AvroAdapterFinder.avroAdapter();
    assertEquals(avroAdapter.getClass(), TestAvroAdapter.class);
  }

  @Test
  public void testAdapterHigherPriorityThanChooserProperty()
  {
    System.setProperty(ADAPTER_PROPERTY, TestAvroAdapter.class.getName());
    System.setProperty(CHOOSER_PROPERTY, "xx");
    AvroAdapter avroAdapter = AvroAdapterFinder.avroAdapter();
    assertEquals(avroAdapter.getClass(), TestAvroAdapter.class);
  }

  @Test
  public void testInvalidChooserProperty()
  {
    System.setProperty(CHOOSER_PROPERTY, "xx");
    expectBadAvroAdapter("xx");
  }

  @Test
  public void testInvalidAdapterProperty()
  {
    System.setProperty(ADAPTER_PROPERTY, "yy");
    expectBadAvroAdapter("yy");
  }

  @Test
  public void testValidChooserInvalidAdapterProperty()
  {
    System.setProperty(CHOOSER_PROPERTY, TestAvroAdapterChooser.class.getName());
    System.setProperty(ADAPTER_PROPERTY, "zz");
    expectBadAvroAdapter("zz");
  }

  private void expectBadAvroAdapter(String className)
  {
    try
    {
      AvroAdapterFinder.avroAdapter();
      Assert.fail();
    }
    catch (IllegalStateException e)
    {
      assertEquals(e.getMessage(), "Unable to construct " + className);
      assertEquals(e.getCause().getClass(), ClassNotFoundException.class);
      return;
    }
  }
}
