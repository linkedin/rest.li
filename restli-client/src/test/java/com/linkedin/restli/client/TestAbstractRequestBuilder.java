package com.linkedin.restli.client;


import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;


/**
 * @author Keren Jin
 */
public class TestAbstractRequestBuilder
{
  @Test
  public void testHeader()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    Assert.assertSame(builder.header("a", "b"), builder);
    Assert.assertEquals(builder._headers.get("a"), "b");
    builder.header("a", "c");
    Assert.assertEquals(builder._headers.get("a"), "c");
  }

  @Test
  public void testAddHeader()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.addHeader("a", "b");
    Assert.assertEquals(builder._headers.get("a"), "b");
    builder.addHeader("a", "c");
    Assert.assertEquals(builder._headers.get("a"), "b" + AbstractRequestBuilder.HEADER_DELIMITER + "c");
  }

  @Test
  public void testSetHeader()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.setHeader("a", "b");
    Assert.assertEquals(builder._headers.get("a"), "b");
    builder.setHeader("a", "c");
    Assert.assertEquals(builder._headers.get("a"), "c");
  }

  @Test
  public void testSetHeaders()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.setHeader("a", "b");
    builder.setHeader("c", "d");
    Assert.assertEquals(builder._headers.get("a"), "b");
    Assert.assertEquals(builder._headers.get("c"), "d");

    final Map<String, String> newHeaders = new HashMap<String, String>();
    newHeaders.put("c", "e");

    builder.setHeaders(newHeaders);
    Assert.assertNull(builder._headers.get("a"));
    Assert.assertEquals(builder._headers.get("c"), "e");

    newHeaders.put("c", "f");
    Assert.assertEquals(builder._headers.get("c"), "e");
  }

  private static class DummyAbstractRequestBuilder extends AbstractRequestBuilder<Object, Object, Request<Object>>
  {
    public DummyAbstractRequestBuilder()
    {
      super(null, null);
    }

    @Override
    public Request<Object> build()
    {
      return null;
    }
  }
}
