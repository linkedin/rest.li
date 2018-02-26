/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.client;


import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.client.test.TestRecord;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.RestConstants;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import java.util.Set;
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
    Assert.assertEquals(builder.getHeader("a"), "b");
    builder.addHeader("a", "c");
    Assert.assertEquals(builder.getHeader("a"), "b" + AbstractRequestBuilder.HEADER_DELIMITER + "c");
  }

  @Test
  public void testAddHeaderWithNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.addHeader("a", "b");
    builder.addHeader("a", null);
    builder.addHeader("a", "c");
    Assert.assertEquals(builder.getHeader("a"), "b" + AbstractRequestBuilder.HEADER_DELIMITER + "c");
  }

  @Test
  public void testSetHeaderWithNonNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    Assert.assertSame(builder.setHeader("a", "b"), builder);
    Assert.assertEquals(builder.getHeader("a"), "b");
    builder.setHeader("a", "c");
    Assert.assertEquals(builder.getHeader("a"), "c");
  }

  @Test
  public void testSetHeaderWithNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.setHeader("a", "b");
    builder.setHeader("a", null);
    Assert.assertNull(builder.getHeader("a"));
  }

  @Test
  public void testSetHeadersWithNonNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.setHeader("a", "b");
    builder.setHeader("c", "d");
    Assert.assertEquals(builder.getHeader("a"), "b");
    Assert.assertEquals(builder.getHeader("c"), "d");

    final Map<String, String> newHeaders = new HashMap<String, String>();
    newHeaders.put("c", "e");

    builder.setHeaders(newHeaders);
    Assert.assertNull(builder.getHeader("a"));
    Assert.assertEquals(builder.getHeader("c"), "e");

    newHeaders.put("c", "f");
    Assert.assertEquals(builder.getHeader("c"), "e");
  }

  @Test
  public void testSetHeadersWithNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();
    final Map<String, String> newHeaders = new HashMap<String, String>();
    newHeaders.put("a", "b");
    newHeaders.put("c", null);

    builder.setHeaders(newHeaders);
    Assert.assertEquals(builder.getHeader("a"), "b");
    Assert.assertNull(builder.getHeader("c"));
  }

  @Test
  public void testAddCookieWithNonNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();
    List<HttpCookie> cookies = new ArrayList<HttpCookie>(Arrays.asList(new HttpCookie("X", "1"),
                                                                       new HttpCookie("Y", "2"),
                                                                       new HttpCookie("Z", "3")));

    Assert.assertSame(builder.addCookie(new HttpCookie("X", "1")), builder);
    Assert.assertSame(builder.addCookie(new HttpCookie("Y", "2")), builder);
    Assert.assertSame(builder.addCookie(new HttpCookie("Z", "3")), builder);
    Assert.assertEquals(builder.getCookies(), cookies);
  }

  @Test
  public void testAddCookieWithNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    Assert.assertSame(builder.addCookie(new HttpCookie("X", "1")), builder);
    Assert.assertSame(builder.addCookie(null), builder);
    Assert.assertSame(builder.addCookie(new HttpCookie("Z", "3")), builder);

    List<HttpCookie> cookies = new ArrayList<HttpCookie>(Arrays.asList(new HttpCookie("X", "1"),
                                                                       new HttpCookie("Z", "3")));

    Assert.assertEquals(builder.getCookies(), cookies);
  }

  @Test
  public void testSetCookiesWithNonNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();
    List<HttpCookie> cookies = new ArrayList<HttpCookie>(Arrays.asList(new HttpCookie("X", "1"),
                                                                       new HttpCookie("Y", "2"),
                                                                       new HttpCookie("Z", "3")));

    Assert.assertSame(builder.setCookies(cookies), builder);
    Assert.assertEquals(builder.getCookies(), cookies);
  }

  @Test
  public void testSetCookiesWithNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();
    List<HttpCookie> cookies = new ArrayList<HttpCookie>(Arrays.asList(new HttpCookie("X", "1"),
                                                                       null,
                                                                       new HttpCookie("Z", "3")));
   // Null element will not be passed
    Assert.assertSame(builder.setCookies(cookies), builder);
    List<HttpCookie> resultCookies = new ArrayList<HttpCookie>(Arrays.asList(new HttpCookie("X", "1"),
                                                                       new HttpCookie("Z", "3")));
    Assert.assertEquals(builder.getCookies(), resultCookies);
  }

  @Test
  public void testClearCookie()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();
    List<HttpCookie> cookies = new ArrayList<HttpCookie>(Arrays.asList(new HttpCookie("X", "1"),
                                                                       new HttpCookie("Y", "2"),
                                                                       new HttpCookie("Z", "3")));

    Assert.assertSame(builder.setCookies(cookies), builder);
    Assert.assertSame(builder.clearCookies(), builder);
    Assert.assertEquals(builder.getCookies(), Collections.emptyList());
  }

  @Test
  public void testSetParamWithNonNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.setParam("a", "b");
    Map<String, Object> queryParams = builder.buildReadOnlyQueryParameters();
    Assert.assertEquals(queryParams.get("a"), "b");
  }

  @Test
  public void testSetParamWithNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.setParam("a", null);
    Map<String, Object> queryParams = builder.buildReadOnlyQueryParameters();
    Assert.assertFalse(queryParams.containsKey("a"));
  }

  @Test
  public void testAddParamWithNonNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.addParam("a", "b");
    Map<String, Object> queryParams = builder.buildReadOnlyQueryParameters();

    Assert.assertEquals(queryParams.get("a"), Arrays.asList("b"));
  }

  @Test(dataProvider = "testQueryParam")
  public void testAddParamSameKeyMultipleValues(Object value1, Object value2, Object value3)
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.addParam("a", value1);
    Assert.assertEquals(builder.getParam("a"), Arrays.asList(value1));
    builder.addParam("a", value2);
    Assert.assertEquals(builder.getParam("a"), Arrays.asList(value1, value2));
  }

  @Test
  public void testAddParamWithNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.addParam("a", null);
    Assert.assertFalse(builder.hasParam("a"));
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

    Assert.assertEquals(builder.getParam("a"), Arrays.asList(value1, value2, value3));
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

    Assert.assertEquals(builder.getParam("a"), Arrays.asList(value1, value2, value3));
  }

  @Test(dataProvider = "testQueryParam")
  public void testAddThenSetParam(Object value1, Object value2, Object value3)
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.addParam("a", value1).addParam("a", value2);
    builder.setParam("a", value3);

    Assert.assertEquals(builder.getParam("a"), value3);
  }

  @Test
  public void testSetReqParamWithNonNullValue()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.setReqParam("a", "b");

    Assert.assertEquals(builder.getParam("a"), "b");
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

    Assert.assertEquals(builder.getParam("a"), Arrays.asList("b"));
  }

  @Test
  public void testAddReqParamSameKeyMultipleValues()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.addReqParam("a", "b1");
    Assert.assertEquals(builder.getParam("a"), Arrays.asList("b1"));
    builder.addReqParam("a", "b2");
    Assert.assertEquals(builder.getParam("a"), Arrays.asList("b1", "b2"));
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
  @SuppressWarnings("unchecked")
  public void testProjectionFields()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    PathSpec pathSpec1 = new PathSpec("firstField");
    PathSpec pathSpec23 = new PathSpec("secondField", PathSpec.WILDCARD, "thirdField");
    builder.addFields(pathSpec1, pathSpec23);
    Assert.assertTrue(builder.getParam(RestConstants.FIELDS_PARAM) instanceof Set);
    final Set<PathSpec> fieldsPathSpecs = (Set<PathSpec>) builder.getParam(RestConstants.FIELDS_PARAM);
    Assert.assertEquals(fieldsPathSpecs, new HashSet<>(Arrays.asList(pathSpec1, pathSpec23)), "The path spec(s) should match!") ;

    PathSpec pathSpec4 = new PathSpec(PathSpec.WILDCARD, "fourthField");
    PathSpec pathSpec5 = new PathSpec("fifthField");
    builder.addMetadataFields(pathSpec4, pathSpec5);
    Assert.assertTrue(builder.getParam(RestConstants.METADATA_FIELDS_PARAM) instanceof Set);
    final Set<PathSpec> metadataFieldsPathSpecs = (Set<PathSpec>)  builder.getParam(RestConstants.METADATA_FIELDS_PARAM);
    Assert.assertEquals(metadataFieldsPathSpecs, new HashSet<>(Arrays.asList(pathSpec4, pathSpec5)), "The path spec(s) should match!") ;

    PathSpec pathSpec6 = new PathSpec("sixthField", PathSpec.WILDCARD);
    PathSpec pathSpec7 = new PathSpec("seventhField");
    builder.addPagingFields(pathSpec6, pathSpec7);
    Assert.assertTrue(builder.getParam(RestConstants.PAGING_FIELDS_PARAM) instanceof Set);
    final Set<PathSpec> pagingFieldsPathSpecs = (Set<PathSpec>) builder.getParam(RestConstants.PAGING_FIELDS_PARAM);
    Assert.assertEquals(pagingFieldsPathSpecs, new HashSet<>(Arrays.asList(pathSpec6, pathSpec7)), "The path spec(s) should match!") ;

    Assert.assertEquals(builder.buildReadOnlyQueryParameters().size(), 3,
                        "We should have 3 query parameters, one for each projection type");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testParametersAreReadOnly()
  {
    final AbstractRequestBuilder<?, ?, ?> builder = new DummyAbstractRequestBuilder();

    TestRecord testRecord = new TestRecord();

    builder.setParam("abc", testRecord);
    Map<String, Object> parameters = builder.buildReadOnlyQueryParameters();
    Assert.assertNotSame(parameters.get("abc"), testRecord);
    Assert.assertTrue(((RecordTemplate) parameters.get("abc")).data().isMadeReadOnly());

    testRecord.data().makeReadOnly();
    parameters = builder.buildReadOnlyQueryParameters();
    Assert.assertSame(parameters.get("abc"), testRecord);

    TestRecord testRecord2 = new TestRecord();

    builder.addParam("abc2", testRecord2);
    parameters = builder.buildReadOnlyQueryParameters();
    List<Object> collectionParam = (List<Object>) parameters.get("abc2");
    Assert.assertNotSame(collectionParam.get(0), testRecord2);
    Assert.assertTrue(((RecordTemplate)collectionParam.get(0)).data().isMadeReadOnly());

    testRecord2.data().makeReadOnly();
    parameters = builder.buildReadOnlyQueryParameters();
    collectionParam = (List<Object>) parameters.get("abc2");
    Assert.assertSame(collectionParam.get(0), testRecord2);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testKeysAreReadOnly()
  {
    final AbstractRequestBuilder<Object, ?, ?> builder = new DummyAbstractRequestBuilder();

    TestRecord testRecord = new TestRecord();
    TestRecord testRecord2 = new TestRecord();
    ComplexResourceKey<TestRecord, TestRecord> originalKey =
        new ComplexResourceKey<TestRecord, TestRecord>(testRecord, testRecord2);

    builder.addKey(originalKey);
    Map<String, Object> parameters = builder.buildReadOnlyQueryParameters();
    Object key = ((Set<Object>)parameters.get(RestConstants.QUERY_BATCH_IDS_PARAM)).iterator().next();
    Assert.assertNotSame(key, originalKey);
    Assert.assertTrue(((ComplexResourceKey<TestRecord, TestRecord>)key).isReadOnly());

    try
    {
      parameters.put("xyz", "abc");
      Assert.fail("The generated parameters should be read-only.");
    }
    catch (Exception e)
    {

    }

    originalKey.makeReadOnly();
    parameters = builder.buildReadOnlyQueryParameters();
    key = ((Set<Object>)parameters.get(RestConstants.QUERY_BATCH_IDS_PARAM)).iterator().next();
    Assert.assertSame(key, originalKey);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testPathKeysAreReadOnly()
  {
    final AbstractRequestBuilder<Object, ?, ?> builder = new DummyAbstractRequestBuilder();

    TestRecord testRecord = new TestRecord();
    TestRecord testRecord2 = new TestRecord();
    ComplexResourceKey<TestRecord, TestRecord> originalKey =
        new ComplexResourceKey<TestRecord, TestRecord>(testRecord, testRecord2);

    builder.pathKey("abc", originalKey);
    Map<String, Object> pathKeys = builder.buildReadOnlyPathKeys();
    Object key = pathKeys.get("abc");
    Assert.assertNotSame(key, originalKey);
    Assert.assertTrue(((ComplexResourceKey<TestRecord, TestRecord>)key).isReadOnly());

    try
    {
      pathKeys.put("xyz", "abc");
      Assert.fail("The generated path keys should be read-only.");
    }
    catch (Exception e)
    {

    }

    originalKey.makeReadOnly();
    pathKeys = builder.buildReadOnlyPathKeys();
    key = pathKeys.get("abc");
    Assert.assertSame(key, originalKey);
  }

  @Test
  public void testAssocKeysAreReadOnly()
  {
    final AbstractRequestBuilder<Object, ?, ?> builder = new DummyAbstractRequestBuilder();

    builder.addAssocKey("abc", 5);
    builder.addAssocKey("abc2", 6);

    CompoundKey compoundKey = builder.buildReadOnlyAssocKey();
    Assert.assertTrue(compoundKey.isReadOnly());
  }

  @Test
  public void testHeadersAreReadOnly()
  {
    final AbstractRequestBuilder<Object, ?, ?> builder = new DummyAbstractRequestBuilder();
    builder.setHeader("abc", "abc");

    Map<String, String> headers = builder.buildReadOnlyHeaders();

    try
    {
      headers.put("xyz", "abc");
      Assert.fail("The generated headers should be read-only.");
    }
    catch (Exception e)
    {

    }

    builder.setHeader("abc", "def");

    Assert.assertEquals(headers.get("abc"), "abc");
  }

  @Test
  public void testCookiesAreReadOnly()
  {
    final AbstractRequestBuilder<Object, ?, ?> builder = new DummyAbstractRequestBuilder();
    builder.setCookies(Arrays.asList(new HttpCookie("a", "b"), new HttpCookie("c", "d")));
    List<HttpCookie> cookies = builder.buildReadOnlyCookies();

    try
    {
      cookies.add(new HttpCookie("ac", "bb"));
      Assert.fail("The generated cookies should be read-only.");
    }
    catch (Exception e)
    {

    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testProjectionFieldsAreReadOnly()
  {
    final AbstractRequestBuilder<Object, ?, ?> builder = new DummyAbstractRequestBuilder();
    PathSpec field = new PathSpec("abc");
    PathSpec[] originalFields = new PathSpec[] { field };
    builder.addFields(originalFields);
    builder.addMetadataFields(originalFields);
    builder.addPagingFields(originalFields);

    Map<String, Object> parameters = builder.buildReadOnlyQueryParameters();
    Set<Object> fields = (Set<Object>) parameters.get(RestConstants.FIELDS_PARAM);
    Set<Object> metadataFields = (Set<Object>) parameters.get(RestConstants.METADATA_FIELDS_PARAM);
    Set<Object> pagingFields = (Set<Object>) parameters.get(RestConstants.PAGING_FIELDS_PARAM);

    PathSpec field2 = new PathSpec("def");
    originalFields[0] = field2;

    Assert.assertTrue(fields.contains(field));
    Assert.assertFalse(fields.contains(field2));

    try
    {
      fields.add("xyz");
      Assert.fail("The generated fields should be read-only.");
    }
    catch (Exception e)
    {

    }

    Assert.assertTrue(metadataFields.contains(field));
    Assert.assertFalse(metadataFields.contains(field2));

    try
    {
      metadataFields.add("xyz");
      Assert.fail("The generated metadata fields should be read-only.");
    }
    catch (Exception e)
    {

    }

    Assert.assertTrue(pagingFields.contains(field));
    Assert.assertFalse(pagingFields.contains(field2));

    try
    {
      pagingFields.add("xyz");
      Assert.fail("The generated paging fields should be read-only.");
    }
    catch (Exception e)
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
