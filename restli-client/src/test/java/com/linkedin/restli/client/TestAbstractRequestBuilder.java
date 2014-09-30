package com.linkedin.restli.client;


import com.linkedin.data.schema.PathSpec;
import com.linkedin.restli.common.RestConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
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
    Assert.assertEquals(builder._queryParams.get("a"), Arrays.asList("b"));
  }

  @Test(dataProvider = "testQueryParam")
  public void testAddParamSameKeyMultipleValues(Object value1, Object value2, Object value3)
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.addParam("a", value1);
    Assert.assertEquals(builder._queryParams.get("a"), Arrays.asList(value1));
    builder.addParam("a", value2);
    Assert.assertEquals(builder._queryParams.get("a"), Arrays.asList(value1, value2));
  }

  @Test
  public void testAddParamWithNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.addParam("a", null);
    Assert.assertFalse(builder._queryParams.containsKey("a"));
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testSetNonIterableThenAddParam()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.setParam("a", "b");
    builder.addParam("a", "c");
  }

  @Test(dataProvider = "testQueryParam")
  public void testSetIterableThenAddParam(final Object value1, final Object value2, Object value3)
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    final Iterable<Object> iter = new Iterable<Object>()
    {
      private final Collection<Object> _coll = Arrays.asList(value1, value2);
      @Override
      public Iterator<Object> iterator()
      {
        return _coll.iterator();
      }
    };
    builder.setParam("a", iter);
    builder.addParam("a", value3);
    Assert.assertEquals(builder._queryParams.get("a"), Arrays.asList(value1, value2, value3));
  }

  @Test(dataProvider = "testQueryParam")
  public void testSetCollectionThenAddParam(Object value1, Object value2, Object value3)
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    // AbstractList returned by Arrays.asList() does not support add()
    // need to wrap it with ArrayList
    final Collection<Object> testData = new ArrayList<Object>(Arrays.asList(value1, value2));
    builder.setParam("a", testData);
    builder.addParam("a", value3);
    Assert.assertEquals(builder._queryParams.get("a"), Arrays.asList(value1, value2, value3));
  }

  @Test(dataProvider = "testQueryParam")
  public void testAddThenSetParam(Object value1, Object value2, Object value3)
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.addParam("a", value1).addParam("a", value2);
    builder.setParam("a", value3);

    Assert.assertEquals(builder._queryParams.get("a"), value3);
  }

  @Test
  public void testSetReqParamWithNonNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.setReqParam("a", "b");
    Assert.assertEquals(builder._queryParams.get("a"), "b");
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testSetReqParamWithNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();
    builder.setReqParam("a", null);
  }

  @Test
  public void testAddReqParamWithNonNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.addReqParam("a", "b");
    Assert.assertEquals(builder._queryParams.get("a"), Arrays.asList("b"));
  }

  @Test
  public void testAddReqParamSameKeyMultipleValues()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.addReqParam("a", "b1");
    Assert.assertEquals(builder._queryParams.get("a"), Arrays.asList("b1"));
    builder.addReqParam("a", "b2");
    Assert.assertEquals(builder._queryParams.get("a"), Arrays.asList("b1", "b2"));
  }

  @DataProvider(name = "testQueryParam")
  public static Object[][] testQueryParamDataProvider()
  {
    final Object value3 = new ArrayList<String>(Arrays.asList("x", "y"));
    return new Object[][] {
      { "a", "b", "z" },
      { "a", "b", value3 },
      { new String[] { "a", "b" }, new String[] { "c", "d" }, "z" },
      { new String[] { "a", "b" }, new String[] { "c", "d" }, value3 },
      { new ArrayList<String>(Arrays.asList("a", "b")), new ArrayList<String>(Arrays.asList("c", "d")), "z" },
      { new ArrayList<String>(Arrays.asList("a", "b")), new ArrayList<String>(Arrays.asList("c", "d")), value3 }
    };
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

  @Test
  public void testProjectionFields()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.addFields(new PathSpec("firstField"), new PathSpec("secondField", PathSpec.WILDCARD, "thirdField"));
    Assert.assertTrue(builder._queryParams.get(RestConstants.FIELDS_PARAM) instanceof PathSpec[]);
    final PathSpec[] fieldsPathSpecs = (PathSpec[])builder._queryParams.get(RestConstants.FIELDS_PARAM);
    Assert.assertEquals(fieldsPathSpecs[0].toString(), "/firstField", "The path spec(s) should match!");
    Assert.assertEquals(fieldsPathSpecs[1].toString(), "/secondField/*/thirdField", "The path spec(s) should match!");

    builder.addMetadataFields(new PathSpec(PathSpec.WILDCARD, "fourthField"), new PathSpec("fifthField"));
    Assert.assertTrue(builder._queryParams.get(RestConstants.METADATA_FIELDS_PARAM) instanceof PathSpec[]);
    final PathSpec[] metadataFieldsPathSpecs = (PathSpec[])builder._queryParams.get(RestConstants.METADATA_FIELDS_PARAM);
    Assert.assertEquals(metadataFieldsPathSpecs[0].toString(), "/*/fourthField", "The path spec(s) should match!");
    Assert.assertEquals(metadataFieldsPathSpecs[1].toString(), "/fifthField", "The path spec(s) should match!");

    builder.addPagingFields(new PathSpec("sixthField", PathSpec.WILDCARD), new PathSpec("seventhField"),
        new PathSpec(PathSpec.WILDCARD));
    Assert.assertTrue(builder._queryParams.get(RestConstants.PAGING_FIELDS_PARAM) instanceof PathSpec[]);
    final PathSpec[] pagingFieldsPathSpecs = (PathSpec[])builder._queryParams.get(RestConstants.PAGING_FIELDS_PARAM);
    Assert.assertEquals(pagingFieldsPathSpecs[0].toString(), "/sixthField/*", "The path spec(s) should match!");
    Assert.assertEquals(pagingFieldsPathSpecs[1].toString(), "/seventhField", "The path spec(s) should match!");

    Assert.assertEquals(builder._queryParams.size(), 3, "We should have 3 query parameters, one for each projection type");
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
