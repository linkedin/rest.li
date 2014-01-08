package com.linkedin.restli.client;


import com.linkedin.data.DataList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Keren Jin
 */
public class TestAbstractRequestBuilder
{
  @Test
  public void testAddHeaderWithNonNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    Assert.assertSame(builder.addHeader("a", "b"), builder);
    Assert.assertEquals(builder._headers.get("a"), "b");
    builder.addHeader("a", "c");
    Assert.assertEquals(builder._headers.get("a"), "b" + AbstractRequestBuilder.HEADER_DELIMITER + "c");
  }

  @Test
  public void testAddHeaderWithNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.addHeader("a", "b");
    builder.addHeader("a", null);
    builder.addHeader("a", "c");
    Assert.assertEquals(builder._headers.get("a"), "b" + AbstractRequestBuilder.HEADER_DELIMITER + "c");
  }

  @Test
  public void testSetHeaderWithNonNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    Assert.assertSame(builder.setHeader("a", "b"), builder);
    Assert.assertEquals(builder._headers.get("a"), "b");
    builder.setHeader("a", "c");
    Assert.assertEquals(builder._headers.get("a"), "c");
  }

  @Test
  public void testSetHeaderWithNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.setHeader("a", "b");
    builder.setHeader("a", null);
    Assert.assertNull(builder._headers.get("a"));
  }

  @Test
  public void testSetHeadersWithNonNullValue()
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

  @Test
  public void testSetHeadersWithNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();
    final Map<String, String> newHeaders = new HashMap<String, String>();
    newHeaders.put("a", "b");
    newHeaders.put("c", null);

    builder.setHeaders(newHeaders);
    Assert.assertEquals(builder._headers.get("a"), "b");
    Assert.assertNull(builder._headers.get("c"));
  }

  @Test
  public void testSetParamWithNonNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.setParam("a", "b");
    Assert.assertEquals(builder._queryParams.get("a"), "b");
  }

  @Test
  public void testSetParamWithNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.setParam("a", null);
    Assert.assertFalse(builder._queryParams.containsKey("a"));
  }

  @Test
  public void testAddParamWithNonNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.addParam("a", "b");
    Assert.assertEquals(builder._queryParams.get("a"), "b");
  }

  @Test
  public void testAddParamSameKeyMultipleValues()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.addParam("a", "b1");
    Assert.assertEquals(builder._queryParams.get("a"), "b1");
    builder.addParam("a", "b2");
    Assert.assertEquals(builder._queryParams.get("a"), new DataList(Arrays.asList("b1", "b2")));
  }

  @Test
  public void testAddParamWithNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.addParam("a", null);
    Assert.assertFalse(builder._queryParams.containsKey("a"));
  }

  @Test
  public void testSetReqParamWithNonNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.setReqParam("a", "b");
    Assert.assertEquals(builder._queryParams.get("a"), "b");
  }

  @Test
  public void testSetReqParamWithNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    try
    {
      builder.setReqParam("a", null);
      Assert.fail("setReqParam should not allow null values");
    }
    catch (NullPointerException e)
    {

    }
  }

  @Test
  public void testAddReqParamWithNonNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.addReqParam("a", "b");
    Assert.assertEquals(builder._queryParams.get("a"), "b");
  }

  @Test
  public void testAddReqParamSameKeyMultipleValues()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.addReqParam("a", "b1");
    Assert.assertEquals(builder._queryParams.get("a"), "b1");
    builder.addReqParam("a", "b2");
    Assert.assertEquals(builder._queryParams.get("a"), new DataList(Arrays.asList("b1", "b2")));
  }

  @Test
  public void testAddReqParamWithNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    try
    {
      builder.addReqParam("a", null);
      Assert.fail("addReqParam should not allow null values");
    }
    catch (NullPointerException e)
    {

    }
  }

  private static class DummyAbstractRequestBuilder extends AbstractRequestBuilder<Object, Object, Request<Object>>
  {
    public DummyAbstractRequestBuilder()
    {
      super(null, null, RestliRequestOptions.DEFAULT_OPTIONS);
    }

    @Override
    public Request<Object> build()
    {
      return null;
    }
  }
}
