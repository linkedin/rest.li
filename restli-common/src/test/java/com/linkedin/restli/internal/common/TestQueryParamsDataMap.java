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

package com.linkedin.restli.internal.common;

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.transform.filter.request.MaskCreator;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.jersey.api.uri.UriComponent;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.PathSegment.PathSegmentSyntaxException;
import com.linkedin.restli.internal.common.URLEscaper.Escaping;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestQueryParamsDataMap
{
  @Test
  public void testParseDataMapKeys() throws Exception {
    String testQS = "ids[0].params.versionTag=tag1&ids[0].params.authToken=tok1&ids[0].memberID=1&ids[0].groupID=2&" +
                    "ids[1].params.versionTag=tag2&ids[1].params.authToken=tok2&ids[1].memberID=2&ids[1].groupID=2&" +
                    "q=someFinder";
    /*
     * Resulting DataMap:
     *
     * {    q=someFinder,
     *      ids=[
     *          {groupID=2,
     *           params={
     *                  versionTag=tag2,
     *                  authToken=tok2
     *           },
     *           memberID=2
     *           },
     *
     *           {groupID=2,
     *           params={
     *                  versionTag=tag1,
     *                  authToken=tok1
     *                  },
     *           memberID=1
     *           }
     *       ]
     * }
     */
    DataMap queryParamDataMap = queryParamsDataMap(testQS);

    Assert.assertNotNull(queryParamDataMap);
    Assert.assertEquals("someFinder", queryParamDataMap.get("q"));

    DataList ids = queryParamDataMap.getDataList("ids");

    DataMap ids0 = ids.getDataMap(0);
    Assert.assertEquals(ids0.get("memberID"), "1");
    Assert.assertEquals(ids0.get("groupID"), "2");
    DataMap params = ids0.getDataMap("params");
    Assert.assertEquals(params.get("versionTag"), "tag1");
    Assert.assertEquals(params.get("authToken"), "tok1");

    ids0 = ids.getDataMap(1);
    Assert.assertEquals(ids0.get("memberID"), "2");
    Assert.assertEquals(ids0.get("groupID"), "2");
    params = ids0.getDataMap("params");
    Assert.assertEquals(params.get("versionTag"), "tag2");
    Assert.assertEquals(params.get("authToken"), "tok2");
  }


  /**
   * Test nested list and map cases
   */
  @Test
  public void testNestedCases() throws Exception
  {
    String testQS = "ids[0].keys[0].part1=part001&ids[0].keys[0].part2=part002&" +
                    "ids[0].keys[1].part1=part011&ids[0].keys[1].part2=part012&" +
                    "ids[1].keys[0].part1=part101&ids[1].keys[0].part2=part102&" +
                    "ids[1].keys[1].part1=part111&ids[1].keys[1].part2=part112&";
    DataMap queryParamsDataMap = queryParamsDataMap(testQS);
    Object ids = queryParamsDataMap.get("ids");
    Assert.assertTrue(ids instanceof DataList);
    DataList idsList = (DataList)ids;
    Object id = idsList.get(0);
    Assert.assertTrue(id instanceof DataMap);
    DataMap idDataMap = (DataMap)id;
    Object keys = idDataMap.get("keys");
    Assert.assertTrue(keys instanceof DataList);
    DataList keysList = (DataList)keys;

    Object parts = keysList.get(0);
    Assert.assertTrue(parts instanceof DataMap);
    DataMap partsDataMap = (DataMap)parts;
    Assert.assertEquals("part001", partsDataMap.get("part1"));
    Assert.assertEquals("part002", partsDataMap.get("part2"));

    parts = keysList.get(1);
    Assert.assertTrue(parts instanceof DataMap);
    partsDataMap = (DataMap)parts;
    Assert.assertEquals("part011", partsDataMap.get("part1"));
    Assert.assertEquals("part012", partsDataMap.get("part2"));

    id = idsList.get(1);
    Assert.assertTrue(id instanceof DataMap);
    idDataMap = (DataMap)id;
    keys = idDataMap.get("keys");
    Assert.assertTrue(keys instanceof DataList);
    keysList = (DataList)keys;

    parts = keysList.get(0);
    Assert.assertTrue(parts instanceof DataMap);
    partsDataMap = (DataMap)parts;
    Assert.assertEquals("part101", partsDataMap.get("part1"));
    Assert.assertEquals("part102", partsDataMap.get("part2"));

    parts = keysList.get(1);
    Assert.assertTrue(parts instanceof DataMap);
    partsDataMap = (DataMap)parts;
    Assert.assertEquals("part111", partsDataMap.get("part1"));
    Assert.assertEquals("part112", partsDataMap.get("part2"));
  }

  /**
   * Test that the legacy simple (no multi-parts, no indices) keys still work
   * as expected, resulting in a list of values keyed on the parameter name.
   *
   * @throws Exception
   */
  @Test
  public void testMultipleSimpleKeys() throws Exception {
    String testQS = "ids=1&ids=2&ids=bla&q=someFinder";

    DataMap queryParamDataMap = queryParamsDataMap(testQS);
    Assert.assertEquals("someFinder", queryParamDataMap.get("q"));

    Object idsObj = queryParamDataMap.get("ids");
    Assert.assertTrue(idsObj instanceof DataList);
    DataList ids = (DataList)idsObj;
    Assert.assertEquals(ids.get(0), "1");
    Assert.assertEquals(ids.get(1), "2");
    Assert.assertEquals(ids.get(2), "bla");
  }

  /**
   * Test that if they key names are numeric, such as in ids.1=1, they are interpreted
   * as the keys in the resulting DataMap
   * @throws Exception
   */
  @Test
  public void testNumericKeyNames() throws Exception {
    String testQS = "ids.1=1&ids.2=2&ids.3=3";

    DataMap queryParamDataMap = queryParamsDataMap(testQS);
    Object idsObj = queryParamDataMap.get("ids");
    Assert.assertTrue(idsObj instanceof DataMap);
    DataMap ids = (DataMap)idsObj;
    Assert.assertEquals(ids.get("1"), "1");
    Assert.assertEquals(ids.get("2"), "2");
    Assert.assertEquals(ids.get("3"), "3");
  }

  /**
   * Test that key indices, such as ids[1]=1, are interpreted as the ordinal numbers
   * in the resulting DataList
   * @throws Exception
   */
  @Test
  public void testNumericKeyIndices() throws Exception {
    String testQS = "ids[0]=0&ids[1]=1&ids[2]=2";

    DataMap queryParamDataMap = queryParamsDataMap(testQS);
    Object idsObj = queryParamDataMap.get("ids");
    Assert.assertTrue(idsObj instanceof DataList);
    DataList ids = (DataList)idsObj;
    Assert.assertEquals(ids.get(0), "0");
    Assert.assertEquals(ids.get(1), "1");
    Assert.assertEquals(ids.get(2), "2");
  }

  /**
   * Test that if there are "holes" in the index sequence of the parameters,
   * they are collapsed in the resulting list, preserving the order of the
   * parameters
   *
   * @throws Exception
   */
  @Test
  public void testNumericKeyIndicesWithHoles() throws Exception
  {
    DataMap queryParamDataMap = queryParamsDataMap("ids[4]=1&ids[1]=0&ids[8]=2");
    Object idsObj = queryParamDataMap.get("ids");
    Assert.assertTrue(idsObj instanceof DataList);
    DataList ids = (DataList) idsObj;
    Assert.assertEquals(ids.get(0), "0");
    Assert.assertEquals(ids.get(1), "1");
    Assert.assertEquals(ids.get(2), "2");
  }

  @Test
  public void testPathSegment() throws Exception {
    PathSegment pathSegment = PathSegment.parse("abc[123]");
    Assert.assertEquals(pathSegment.getName(), "abc");
    Assert.assertEquals(pathSegment.getIndex().intValue(), 123);
  }

  @Test
  public void testShortPathSegment() throws Exception {
    PathSegment pathSegment = PathSegment.parse("a[1]");
    Assert.assertEquals(pathSegment.getName(), "a");
    Assert.assertEquals(pathSegment.getIndex().intValue(), 1);
  }

  @Test
  public void testPathSegmentMalformed() throws Exception {
    String[] malformedPathSegments = {"abc[123a]", "[123]", "abc[-123]", "abc[]"};

    for (String pathSegmentString : malformedPathSegments)
    {
      try
      {
        PathSegment.parse(pathSegmentString);
        Assert.fail("Failed to throw PathSegmentSyntaxException for: " + pathSegmentString);
      }
      catch (PathSegmentSyntaxException e)
      {
        // success
      }
    }
  }

  /**
   * Test that reserved characters '[', ']', '.' in the parameter names are properly
   * processed when encoded
   *
   * @throws Exception
   */
  @Test
  public void testPathSegmentEncoded() throws Exception
  {
    String paramString =
        new StringBuilder(PathSegment.CODEC.encode("a[.]")).append('[')
                                                           .append(12)
                                                           .append(']')
                                                           .append('=')
                                                           .append("[.]")
                                                           .toString();
    DataMap queryParamDataMap = queryParamsDataMap(paramString);
    Object param = queryParamDataMap.get("a[.]");
    Assert.assertTrue(param instanceof DataList);
    Assert.assertEquals("[.]", ((DataList)param).get(0));
  }

  @Test
  /**
   * Test query string representation of a DataMap representing a compound key
   */
  public void testCompoundKeyDataMapQueryString() throws Exception
  {
    DataMap dataMap = new DataMap();
    dataMap.put("memberID", 2);
    dataMap.put("groupID", 1);

    Map<String, List<String>> result = QueryParamsDataMap.queryString(dataMap);
    Assert.assertEquals("2", result.get("memberID").get(0));
    Assert.assertEquals("1", result.get("groupID").get(0));
  }

  private DataMap queryParamsDataMap(String queryString) throws PathSegmentSyntaxException
  {
    Map<String, List<String>> queryParameters =
        UriComponent.decodeQuery(URI.create("http://api.linkedin.com?" + queryString),
                                 true);

    return QueryParamsDataMap.parseDataMapKeys(queryParameters);
  }

  @Test
  public void testKeysWithSpecialChars() throws Exception
  {
    DataMap nested = new DataMap();
    nested.put("messy[key1", "value1");
    nested.put("messykey2]", "value2");
    nested.put("messy~", "value3");
    DataMap dataMap = new DataMap();
    dataMap.put("com.linkedin.groups.Group", nested);
    Map<String, List<String>> queryString = QueryParamsDataMap.queryString(dataMap);
    Assert.assertEquals(queryString.get("com~2Elinkedin~2Egroups~2EGroup.messy~5Bkey1").get(0), "value1");
    Assert.assertEquals(queryString.get("com~2Elinkedin~2Egroups~2EGroup.messykey2~5D").get(0), "value2");
    Assert.assertEquals(queryString.get("com~2Elinkedin~2Egroups~2EGroup.messy~7E").get(0), "value3");

    DataMap resultingDataMap = queryParamsDataMap("com~2Elinkedin~2Egroups~2EGroup.messy~5Bkey1=newValue1&com~2Elinkedin~2Egroups~2EGroup.messykey2~5D=newValue2&com~2Elinkedin~2Egroups~2EGroup.messy~7E=newValue3");
    DataMap group = resultingDataMap.getDataMap("com.linkedin.groups.Group");
    Assert.assertEquals(group.get("messy[key1"), "newValue1");
    Assert.assertEquals(group.get("messykey2]"), "newValue2");
    Assert.assertEquals(group.get("messy~"), "newValue3");
  }

  @Test
  public void testSimpleKeys() throws PathSegmentSyntaxException
  {
    String testQS = "ids=1&ids=2&ids=3";
    DataMap queryParamDataMap = queryParamsDataMap(testQS);
    String queryString =
        QueryParamsDataMap.dataMapToQueryString(queryParamDataMap, Escaping.NO_ESCAPING);
    Assert.assertEquals(queryString, testQS);
  }

  /**
   * Creates MaskTrees and extracts the subsequent DataMap to verify that processProjections can correctly
   * convert it correctly into a Map that can later be constructed and encoded into an URI
   */
  @Test
  public void testProcessProjections()
  {
    //Construct a MaskTree from a series of PathSpecs. Extract the subsequent Datamap representation.
    final MaskTree rootObjectsMask = MaskCreator
        .createPositiveMask(new PathSpec("foo", PathSpec.WILDCARD, "bar"));

    final MaskTree metadataMask = MaskCreator
        .createPositiveMask(new PathSpec("foo", "bar"), new PathSpec("bar", "baz"), new PathSpec("qux"));

    final MaskTree pagingMask = MaskCreator
        .createPositiveMask(new PathSpec("total"), new PathSpec("count"), new PathSpec("links", PathSpec.WILDCARD, "rel"));

    final DataMap resultMap = new DataMap(4); //For each type of projection, plus one query string parameter
    resultMap.put(RestConstants.FIELDS_PARAM, rootObjectsMask.getDataMap());
    resultMap.put(RestConstants.METADATA_FIELDS_PARAM, metadataMask.getDataMap());
    resultMap.put(RestConstants.PAGING_FIELDS_PARAM, pagingMask.getDataMap());
    resultMap.put("someQueryString", "someValue");

    final Map<String, List<String>> processedProjections = new LinkedHashMap<String, List<String>>();
    final DataMap processedDataMap = QueryParamsDataMap.processProjections(resultMap, processedProjections);
    Assert.assertTrue(processedDataMap.size() == 1, "Processed datamap should only have one item left!");

    final Map<String, Set<String>> expectedProcessedProjections = new LinkedHashMap<String, Set<String>>();
    //"{fields=[foo:($*:(bar))], metadataFields=[foo:(bar),bar:(baz),qux], pagingFields=[total,count,links:($*:(rel))]}"
    expectedProcessedProjections.put(RestConstants.FIELDS_PARAM, Collections.singleton("foo:($*:(bar))"));
    expectedProcessedProjections.put(RestConstants.METADATA_FIELDS_PARAM,
        new HashSet<String>(Arrays.asList("foo:(bar)", "bar:(baz)", "qux")));
    expectedProcessedProjections.put(RestConstants.PAGING_FIELDS_PARAM,
        new HashSet<String>(Arrays.asList("total", "count", "links:($*:(rel))")));

    Assert.assertEquals(processedProjections.size(), expectedProcessedProjections.size(), "We must have the correct number of" +
        " expected projections!");
    for (final Map.Entry<String, List<String>> entry : processedProjections.entrySet())
    {
      //Acceptable because these are always comma delimited
      final Set<String> actualProjectionValueSet = new HashSet<String>(Arrays.asList(entry.getValue().get(0).split(",")));
      Assert.assertEquals(actualProjectionValueSet, expectedProcessedProjections.get(entry.getKey()), "The individual projection " +
          "for " + entry.getKey() + " does not match what is expected!");
    }
  }
}
