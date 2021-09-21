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

package com.linkedin.restli.internal.server.util;


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.LongMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.transform.filter.request.MaskOperation;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.pegasus.generator.test.NestedArrayRefRecord;
import com.linkedin.pegasus.generator.test.RecordBar;
import com.linkedin.pegasus.generator.test.RecordBarArray;
import com.linkedin.pegasus.generator.test.RecordBarArrayArray;
import com.linkedin.pegasus.generator.test.RecordBarMap;
import com.linkedin.pegasus.generator.test.TyperefTest;
import com.linkedin.pegasus.generator.test.UnionTest;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.restli.common.ContentType;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.ResourceContextImpl;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.server.LinkedListNode;
import com.linkedin.restli.server.RestLiServiceException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author Nishanth Shankaran
 */
public class TestRestUtils
{
  private static final String JSON_TYPE = "application/json";
  private static final String JSON_TYPE_WITH_VALID_PARAMS = "application/json; foo=bar";
  private static final String JSON_TYPE_WITH_Q_PARAM = "application/json; q=.9";
  private static final String PSON_TYPE = "application/x-pson";
  private static final String EMPTY_TYPE = "";
  private static final String HTML_HEADER = "text/html";
  private static final String UNKNOWN_TYPE_HEADER = "foo/bar";
  private static final String UNKNOWN_TYPE_HEADER_WITH_INVALID_PARAMS = "foo/bar; baz";
  private static final String UNKNOWN_TYPE_HEADER_WITH_UNKNOWN_PARAMS = "foo/bar; baz=bark";
  private static final String UNKNOWN_TYPE_HEADER_WITH_VALID_PARAMS = "foo/bar; level=1";
  private static final String UNKNOWN_TYPE_HEADER_JSON = "foo/bar, application/json";
  private static final String UNKNOWN_TYPE_HEADER_WITH_INVALID_PARAMS_JSON = "foo/bar; baz, application/json";
  private static final String UNKNOWN_TYPE_HEADER_WITH_UNKNOWN_PARAMS_JSON = "foo/bar; baz=bark, application/json";
  private static final String UNKNOWN_TYPE_HEADER_WITH_VALID_PARAMS_JSON = "foo/bar; level=1, application/json";
  private static final String PSON_TYPE_HEADER_WITH_VALID_PARAMS_JSON = "application/x-pson; level=1, application/json";
  private static final String JSON_HEADER = "application/json";
  private static final String PSON_HEADER = "application/x-pson";
  private static final String INVALID_TYPE_HEADER_1 = "foo";
  private static final String INVALID_TYPE_HEADER_2 = "foo, bar, baz";
  private static final String INVALID_TYPES_JSON_HEADER = "foo, bar, baz, application/json";
  private static final String INVALID_TYPES_HTML_HEADER = "foo, bar, baz, text/html";
  private static final String MULTIPART_MIME_RELATED_TYPE = "multipart/related";

  @DataProvider(name = "successfulMatch")
  public Object[][] provideSuccessfulMatchData()
  {
    return new Object[][]
    {
        { JSON_HEADER, JSON_TYPE },
        { PSON_HEADER, PSON_TYPE },
        { JSON_TYPE_WITH_VALID_PARAMS, JSON_TYPE_WITH_VALID_PARAMS },
        { JSON_TYPE_WITH_Q_PARAM, JSON_TYPE },
        { HTML_HEADER, EMPTY_TYPE },
        { UNKNOWN_TYPE_HEADER, EMPTY_TYPE },
        { UNKNOWN_TYPE_HEADER_WITH_INVALID_PARAMS, EMPTY_TYPE },
        { UNKNOWN_TYPE_HEADER_WITH_UNKNOWN_PARAMS, EMPTY_TYPE },
        { UNKNOWN_TYPE_HEADER_WITH_VALID_PARAMS, EMPTY_TYPE },
        { UNKNOWN_TYPE_HEADER_JSON, JSON_TYPE },
        { UNKNOWN_TYPE_HEADER_WITH_INVALID_PARAMS_JSON, JSON_TYPE },
        { UNKNOWN_TYPE_HEADER_WITH_UNKNOWN_PARAMS_JSON, JSON_TYPE },
        { UNKNOWN_TYPE_HEADER_WITH_VALID_PARAMS_JSON, JSON_TYPE },
        { MULTIPART_MIME_RELATED_TYPE, JSON_TYPE},
        { PSON_TYPE_HEADER_WITH_VALID_PARAMS_JSON, PSON_TYPE }
    };
  }

  @DataProvider(name = "invalidHeaders")
  public Object[][] provideInvalidHeadersData()
  {
    return new Object[][]
    {
        { INVALID_TYPE_HEADER_1 },
        { INVALID_TYPE_HEADER_2 },
        { INVALID_TYPES_JSON_HEADER },
        { INVALID_TYPES_HTML_HEADER }
    };
  }

  @Test(dataProvider = "successfulMatch")
  public void testPickBestEncodingWithValidMimeTypes(String header, String result)
  {
    Assert.assertEquals(RestUtils.pickBestEncoding(header, Collections.emptySet()), result);
  }

  @Test
  public void testPickBestEncodingWithSupportedMimeTypes()
  {
    Assert.assertEquals(RestUtils.pickBestEncoding(PSON_TYPE_HEADER_WITH_VALID_PARAMS_JSON, Arrays.asList(JSON_HEADER),Collections.emptySet()), JSON_HEADER);
    Assert.assertEquals(RestUtils.pickBestEncoding(PSON_TYPE_HEADER_WITH_VALID_PARAMS_JSON, Collections.EMPTY_LIST,Collections.emptySet()), PSON_HEADER);
  }

  @Test
  public void testPickBestEncodingWithNoMimeTypes()
  {
    Assert.assertNotEquals(RestUtils.pickBestEncoding(null, Collections.emptySet()), EMPTY_TYPE);
  }

  @Test(dataProvider = "invalidHeaders")
  public void testPickBestEncodingWithInvalidHeaders(String header)
  {
    try
    {
      RestUtils.pickBestEncoding(header, Collections.emptySet());
      Assert.fail();
    }
    catch (RestLiServiceException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_400_BAD_REQUEST);
      Assert.assertTrue(e.getMessage().matches("Encountered invalid MIME type '\\w*' in accept header."));
    }
  }

  @Test()
  public void testValidateRequestHeadersWithValidAcceptHeaderAndNoMatch() throws Exception
  {
    Map<String, String> headers = new HashMap<>();
    headers.put("Accept", "text/html");
    ServerResourceContext resourceContext = new ResourceContextImpl();
    try
    {
      RestUtils.validateRequestHeadersAndUpdateResourceContext(headers, Collections.emptySet(), resourceContext);
      Assert.fail();
    }
    catch (RestLiServiceException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_406_NOT_ACCEPTABLE);
      Assert.assertEquals(e.getMessage(),
                          "None of the types in the request's 'Accept' header are supported. " +
                          "Supported MIME types are: " + RestConstants.SUPPORTED_MIME_TYPES + "[]");
      Assert.assertEquals(resourceContext.getResponseMimeType(), null);
    }
  }

  @Test()
  public void testValidateRequestHeadersWithValidAcceptHeaderAndMatch() throws Exception
  {
    Map<String, String> headers = new HashMap<>();
    headers.put("Accept", "application/json");
    ServerResourceContext resourceContext = new ResourceContextImpl();
    RestUtils.validateRequestHeadersAndUpdateResourceContext(headers, Collections.emptySet(), resourceContext);
    Assert.assertEquals(resourceContext.getResponseMimeType(), "application/json");
  }

  @Test()
  public void testValidateRequestHeadersForInProcessRequest() throws Exception
  {
    Map<String, String> headers = new AbstractMap<String, String>()
    {
      @Override
      public Set<Entry<String, String>> entrySet() {
        throw new IllegalStateException("Didn't expect headers to be accessed.");
      }
    };
    RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(ServerResourceContext.CONTEXT_IN_PROCESS_RESOLUTION_KEY, true);
    ServerResourceContext resourceContext = new ResourceContextImpl();
    RestUtils.validateRequestHeadersAndUpdateResourceContext(headers, Collections.emptySet(), resourceContext,
        requestContext);
    Assert.assertEquals(resourceContext.getResponseMimeType(), ContentType.JSON.getHeaderKey());
  }

  @Test
  public void testTrimmerWithPrimitivesRecordsUnionsMix() throws CloneNotSupportedException
  {
    TyperefTest recordTemplate = new TyperefTest();
    recordTemplate.setBoolean(true);

    RecordBar foo = new RecordBar();
    foo.setLocation("foo");
    recordTemplate.setBar1(foo);

    TyperefTest.Union5 union = new TyperefTest.Union5();
    union.setIntRef(5);
    recordTemplate.setUnion5(union);

    RecordTemplate expected = recordTemplate.copy();

    // Introduce bad elements
    recordTemplate.getBar1().data().put("troublemaker", "foo");
    ((DataMap)recordTemplate.getUnion5().data()).put("troublemaker", "foo");
    recordTemplate.data().put("foo", "bar");

    DataList list = new DataList();
    list.add(1);
    DataMap map = new DataMap();
    map.put("foo", 666);
    recordTemplate.data().put("keyFoo", list);
    recordTemplate.data().put("keyBar", map);

    // Pre filtering
    Assert.assertEquals(recordTemplate.data().size(), 6);
    Assert.assertEquals(recordTemplate.getBar1().data().size(), 2);

    RestUtils.trimRecordTemplate(recordTemplate, false);
    // Post filtering
    Assert.assertEquals(recordTemplate, expected);
  }

  @Test
  public void testRecord() throws CloneNotSupportedException
  {
    RecordBar bar = new RecordBar();
    bar.setLocation("mountain view");
    RecordBar expected = bar.copy();

    // Introduce bad elements
    bar.data().put("SF", "CA");

    Assert.assertEquals(bar.data().size(), 2);
    RestUtils.trimRecordTemplate(bar, false);
    Assert.assertEquals(bar, expected);
  }

  @Test
  public void testRefTrim() throws CloneNotSupportedException
  {
    TyperefTest test = new TyperefTest();
    test.setImportRef(1.0);
    test.setImportRef2(2.0);

    RecordTemplate expected = test.copy();

    // Introduce bad elements
    test.data().put("troublemaker", "boo");
    Assert.assertEquals(test.data().size(), 3);
    RestUtils.trimRecordTemplate(test, false);
    Assert.assertEquals(test, expected);
  }

  @Test
  public void testMapTrim() throws CloneNotSupportedException
  {
    TyperefTest test = new TyperefTest();

    RecordBarMap map = new RecordBarMap();
    RecordBar recordBar = new RecordBar();
    recordBar.setLocation("foo");
    map.put("map", recordBar);
    test.setBarRefMap(map);

    TyperefTest expected = test.copy();
    test.getBarRefMap().get("map").data().put("troublemaker", "data filth");

    Assert.assertEquals(recordBar.data().size(), 2);
    Assert.assertEquals(test.getBarRefMap().get("map").data().size(), 2);
    RestUtils.trimRecordTemplate(test, false);
    Assert.assertEquals(expected, test);
  }

  @Test
  public void testArrayTrim() throws CloneNotSupportedException
  {
    TyperefTest test = new TyperefTest();
    RecordBarArray array = new RecordBarArray();

    RecordBar recordBar = new RecordBar();
    recordBar.setLocation("mountain view");
    array.add(recordBar);

    RecordBar recordBar2 = new RecordBar();
    recordBar2.setLocation("palo alto");
    array.add(recordBar2);

    test.setRecordArray(array);

    // Generate expected copy.
    TyperefTest expected = test.copy();

    // Introduce bad elements.
    test.getRecordArray().get(0).data().put("troublemaker", "foo");
    test.getRecordArray().get(0).data().put("troublemaker2", "foo");
    test.getRecordArray().get(1).data().put("troublemaker", "foo");
    test.getRecordArray().get(1).data().put("troublemaker2", "foo");

    Assert.assertEquals(test.getRecordArray().get(0).data().size(), 3);
    Assert.assertEquals(test.getRecordArray().get(1).data().size(), 3);
    RestUtils.trimRecordTemplate(test, false);
    Assert.assertEquals(test, expected);
  }

  @Test
  public void testRecordRefArrayTrim() throws CloneNotSupportedException
  {
    TyperefTest test = new TyperefTest();

    RecordBarArrayArray recordBarArrayArray = new RecordBarArrayArray();

    RecordBarArray recordBarArray = new RecordBarArray();
    RecordBar recordBar = new RecordBar();
    recordBar.setLocation("mountain view");
    recordBarArray.add(recordBar);

    RecordBar recordBar2 = new RecordBar();
    recordBar2.setLocation("palo alto");
    recordBarArray.add(recordBar2);

    recordBarArrayArray.add(recordBarArray);

    test.setRecordRefArray(recordBarArrayArray);

    // Generate expected copy.
    TyperefTest expected = test.copy();

    // Introduce bad elements.
    test.getRecordRefArray().get(0).get(0).data().put("evil", "bar");
    test.getRecordRefArray().get(0).get(0).data().put("evil2", "bar");
    test.getRecordRefArray().get(0).get(1).data().put("evil", "foo");
    test.getRecordRefArray().get(0).get(1).data().put("evil2", "foo");

    Assert.assertEquals(test.getRecordRefArray().get(0).get(0).data().size(), 3);
    Assert.assertEquals(test.getRecordRefArray().get(0).get(1).data().size(), 3);

    RestUtils.trimRecordTemplate(test, false);

    Assert.assertEquals(test, expected);
  }

  @Test
  public void testNestedArrayRefRecord() throws CloneNotSupportedException
  {
    TyperefTest test = new TyperefTest();

    NestedArrayRefRecord nestedArrayRefRecord = new NestedArrayRefRecord();

    RecordBarArray recordBarArray = new RecordBarArray();
    RecordBar recordBar = new RecordBar();
    recordBar.setLocation("mountain view");
    recordBarArray.add(recordBar);

    RecordBar recordBar2 = new RecordBar();
    recordBar2.setLocation("palo alto");
    recordBarArray.add(recordBar2);

    RecordBarArrayArray recordBarArrayArray = new RecordBarArrayArray();
    recordBarArrayArray.add(recordBarArray);

    nestedArrayRefRecord.setNestedRecordRefArray(recordBarArrayArray);

    test.setNestedArrayRefRecord(nestedArrayRefRecord);

    // Generate expected copy.
    TyperefTest expected = test.copy();

    // Introduce bad elements.
    test.getNestedArrayRefRecord().getNestedRecordRefArray().get(0).get(0).data().put("evil", "bar");
    test.getNestedArrayRefRecord().getNestedRecordRefArray().get(0).get(0).data().put("evil2", "bar");
    test.getNestedArrayRefRecord().getNestedRecordRefArray().get(0).get(1).data().put("evil", "foo");
    test.getNestedArrayRefRecord().getNestedRecordRefArray().get(0).get(1).data().put("evil2", "foo");

    Assert.assertEquals(test.getNestedArrayRefRecord().getNestedRecordRefArray().get(0).get(0).data().size(), 3);
    Assert.assertEquals(test.getNestedArrayRefRecord().getNestedRecordRefArray().get(0).get(1).data().size(), 3);

    RestUtils.trimRecordTemplate(test, false);

    Assert.assertEquals(test, expected);
  }

  @Test
  public void testOverrideMask() throws CloneNotSupportedException
  {
    RecordBar bar = new RecordBar();
    bar.setLocation("mountain view");
    bar.data().put("SF", "CA");
    RecordBar expected = bar.clone();

    MaskTree maskTree = new MaskTree();
    maskTree.addOperation(new PathSpec("SF"), MaskOperation.POSITIVE_MASK_OP);

    RestUtils.trimRecordTemplate(bar, maskTree, false);
    Assert.assertEquals(bar, expected);
  }

  @Test
  public void testOverrideMaskNestedRecord() throws CloneNotSupportedException
  {
    LinkedListNode node1 = new LinkedListNode();
    node1.setIntField(1);

    LinkedListNode node2 = new LinkedListNode();
    node2.setIntField(2);
    node1.setNext(node2);
    node2.data().put("keep me", "foo");

    MaskTree maskTree = new MaskTree();
    maskTree.addOperation(new PathSpec("next", "keep me"), MaskOperation.POSITIVE_MASK_OP);
    maskTree.addOperation(new PathSpec("next", "intField"), MaskOperation.POSITIVE_MASK_OP);

    LinkedListNode expected = node2.clone();
    RestUtils.trimRecordTemplate(node1, maskTree, false);
    Assert.assertEquals(node1.getNext(), expected);
  }

  @Test
  public void testOverrideMaskNestedWithMap() throws CloneNotSupportedException
  {
    TyperefTest test = new TyperefTest();
    RecordBar bar = new RecordBar();
    bar.setLocation("foo");
    bar.data().put("bar", "keep me");
    RecordBar expected = bar.clone();

    test.setBarRefMap(new RecordBarMap());
    test.getBarRefMap().put("foo", bar);

    MaskTree maskTree = new MaskTree();
    maskTree.addOperation(new PathSpec("barRefMap", PathSpec.WILDCARD, "location"), MaskOperation.POSITIVE_MASK_OP);
    maskTree.addOperation(new PathSpec("barRefMap", PathSpec.WILDCARD, "bar"), MaskOperation.POSITIVE_MASK_OP);

    RestUtils.trimRecordTemplate(test, maskTree, false);
    Assert.assertEquals(test.getBarRefMap().get("foo"), expected);
  }

  @Test
  public void testRecursiveBasic() throws CloneNotSupportedException
  {
    LinkedListNode node1 = new LinkedListNode();
    node1.setIntField(1);

    LinkedListNode node2 = new LinkedListNode();
    node2.setIntField(2);
    node1.setNext(node2);

    RecordTemplate expected = node1.copy();

    // Introduce bad elements.
    node1.data().put("foo", "bar");
    node2.data().put("foo", "bar");

    Assert.assertEquals(node1.getIntField().intValue(), 1);
    Assert.assertEquals(node1.getNext(), node2);
    Assert.assertEquals(node1.data().size(), 3);

    Assert.assertEquals(node2.getIntField().intValue(), 2);
    Assert.assertEquals(node2.getNext(), null);
    Assert.assertEquals(node2.data().size(), 2);

    RestUtils.trimRecordTemplate(node1, false);
    Assert.assertEquals(node1, expected);
  }

  @Test
  public void testUnionDataMap() throws CloneNotSupportedException
  {
    UnionTest foo = new UnionTest();
    foo.setUnionEmpty(new UnionTest.UnionEmpty());

    UnionTest expected = foo.copy();
    ((DataMap) foo.getUnionEmpty().data()).put("foo", "bar");

    RestUtils.trimRecordTemplate(foo, false);
    Assert.assertEquals(foo, expected);

    // Primitive case
    foo = new UnionTest();
    UnionTest.UnionWithNull bar = new UnionTest.UnionWithNull();
    bar.setBoolean(true);
    foo.setUnionWithNull(bar);
    expected = foo.copy();

    ((DataMap)foo.getUnionWithNull().data()).put("foo", "bar");
    Assert.assertEquals(((DataMap) foo.getUnionWithNull().data()).size(), 2);
    RestUtils.trimRecordTemplate(foo, false);
    Assert.assertEquals(foo, expected);

    // Complex case
    foo = new UnionTest();
    bar = new UnionTest.UnionWithNull();
    bar.setMap(new LongMap());
    foo.setUnionWithNull(bar);
    expected = foo.copy();
    expected.getUnionWithNull().getMap().put("foo", 1L);

    foo.getUnionWithNull().getMap().data().put("foo", 1L);
    foo.data().put("foo", "bar");
    Assert.assertEquals(((DataMap) foo.getUnionWithNull().data()).size(), 1);
    RestUtils.trimRecordTemplate(foo, false);
    Assert.assertEquals(foo, expected);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testFailFast()
  {
    RecordBar bar = new RecordBar();
    bar.setLocation("mountain view");
    bar.data().put("SF", "CA");

    RestUtils.trimRecordTemplate(bar, true);
  }

  @Test (expectedExceptions = UnsupportedOperationException.class)
  public void testReadOnly()
  {
    RecordBar bar = new RecordBar();
    bar.setLocation("mountain view");
    bar.data().put("SF", "CA");
    bar.data().makeReadOnly();

    RestUtils.trimRecordTemplate(bar, false);
  }
}
