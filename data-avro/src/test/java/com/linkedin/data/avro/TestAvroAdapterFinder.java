package com.linkedin.data.avro;


import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;


public class TestAvroAdapterFinder
{
  // Don't constants from AvroAdapterFinder because change of the
  // constant is an incompatible change and we want these tests to catch
  // such incompatible changes.
  private final String CHOOSER_PROPERTY = "com.linkedin.data.avro.AvroAdapterChooser";
  private final String ADAPTER_PROPERTY = "com.linkedin.data.avro.AvroAdapter";

  @BeforeMethod
  private void clearProperties()
  {
    System.clearProperty(CHOOSER_PROPERTY);
    System.clearProperty(ADAPTER_PROPERTY);
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
