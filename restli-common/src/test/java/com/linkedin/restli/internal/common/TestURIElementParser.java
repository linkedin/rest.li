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

package com.linkedin.restli.internal.common;


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class TestURIElementParser
{
  @DataProvider
  private static Object[][] basicDecodable()
  {
    DataMap simpleMap = new DataMap();
    simpleMap.put("someString", "foo");
    simpleMap.put("anotherString", "bar");

    DataMap withEmptyString = new DataMap();
    withEmptyString.put("empty", "");

    DataMap nestedMap = new DataMap();
    DataMap innerNestedMap = new DataMap();
    innerNestedMap.put("bar", "baz");
    nestedMap.put("foo", innerNestedMap);

    DataList nestedList = new DataList();
    DataList innerNestedList1 = new DataList();
    innerNestedList1.add("a");
    innerNestedList1.add("b");
    DataList innerNestedList2 = new DataList();
    innerNestedList2.add("c");
    innerNestedList2.add("d");
    innerNestedList2.add("e");
    nestedList.add(innerNestedList1);
    nestedList.add(innerNestedList2);

    DataMap complexMap = new DataMap();
    DataMap innerComplexMap = new DataMap();
    innerComplexMap.put("foo", "bar");
    complexMap.put("anObject", innerComplexMap);
    DataList innerComplexList = new DataList();
    innerComplexList.add("1");
    innerComplexList.add("2");
    innerComplexList.add("3");
    innerComplexList.add("4");
    complexMap.put("aList", innerComplexList);
    complexMap.put("aString", "baz");

    return new Object [][] {
      { "(someString:foo,anotherString:bar)", simpleMap },
      { "()", new DataMap() },
      { "List()", new DataList() },
      { "(empty:'')", withEmptyString },
      { "(foo:(bar:baz))", nestedMap },
      { "List(List(a,b),List(c,d,e))", nestedList },
      { "(anObject:(foo:bar),aList:List(1,2,3,4),aString:baz)", complexMap}
    };
  }

  @Test(dataProvider = "basicDecodable")
  public void testBasicDecoding(String decodable, Object expectedObj) throws PathSegment.PathSegmentSyntaxException
  {
    Object actualObj = URIElementParser.parse(decodable);
    Assert.assertEquals(actualObj, expectedObj);
  }

  @DataProvider
  private static Object[][] encoded()
  {
    DataMap externalEncoded = new DataMap();
    DataList externalEncodedList = new DataList();
    externalEncodedList.add("/");
    externalEncodedList.add("=");
    externalEncodedList.add("&");
    externalEncoded.put("this is a key", externalEncodedList);

    DataMap internalEncoded = new DataMap();
    DataList internalEncodedList = new DataList();
    internalEncodedList.add("'");
    internalEncodedList.add("(");
    internalEncodedList.add(")");
    internalEncodedList.add(":");
    internalEncoded.put(",", internalEncodedList);

    return new Object [][] {
      { "(this%20is%20a%20key:List(%2F,%3D,%26))", externalEncoded },
      { "(%2C:List(%27,%28,%29,%3A))", internalEncoded }
    };
  }

  @Test(dataProvider = "encoded")
  public void testEncodedDecoding(String encodedString, Object expectedObj) throws PathSegment.PathSegmentSyntaxException
  {
    Object actualObj = URIElementParser.parse(encodedString);
    Assert.assertEquals(actualObj, expectedObj);
  }

  @DataProvider
  private static Object[][] unicode()
  {
    // create objects
    // test unicode encoding
    DataMap japaneseMap = new DataMap();
    japaneseMap.put("konnichiwa","こんにちは"); // Japanese

    DataMap emojiMap = new DataMap();
    emojiMap.put("smiley","☺"); // Emoji

    DataMap surrogatePairMap = new DataMap();
    surrogatePairMap.put("stickoutTongue", "\uD83D\uDE1B"); // Emoji, but with surrogate pairs

    return new Object[][] {
        {"(konnichiwa:%E3%81%93%E3%82%93%E3%81%AB%E3%81%A1%E3%81%AF)", japaneseMap },
        { "(smiley:%E2%98%BA)", emojiMap},
        { "(stickoutTongue:%F0%9F%98%9B)",surrogatePairMap }
    };
  }

  @Test(dataProvider = "unicode")
  public void testUnicode(String decodable, Object expectedObj) throws PathSegment.PathSegmentSyntaxException
  {
    Object actualObj = URIElementParser.parse(decodable);
    Assert.assertEquals(actualObj, expectedObj);
  }

  @DataProvider
  private static Object[][] undecodables()
  {
    return new Object[][] {
      { "hello:",     "tokens left over after parsing; first excess token: ':' (column 5)" },
      { "(a:b",       "unexpected end of input" },
      { "List(a",     "unexpected end of input" },
      { "List(a,",    "unexpected end of input" },
      { ",hello",     "unexpected token: ',' (column 0) at start of element" },
      { "(,",         "expected string token, found grammar token: ',' (column 1)" },
      { "(a,b)",      "expected ':' but found ',' (column 2)" },
      { "List(a,b,)", "unexpected token: ')' (column 9) at start of element" },
      { "(a:b:c)",    "expected ')' but found ':' (column 4)" },
      { "List(a:b)",  "expected ')' but found ':' (column 6)" },
      { "List(a,,b)", "unexpected token: ',' (column 7) at start of element" },
      { "(:b)",       "expected string token, found grammar token: ':' (column 1)" },
      { "(a:)",       "unexpected token: ')' (column 3) at start of element" },
      { "(a::b)",     "unexpected token: ':' (column 3) at start of element" },
      { "",           "unexpected end of input" },
    };
  }

  @Test(dataProvider = "undecodables")
  public void testUndecodable(String undecoable, String expectedErrorMessage)
  {
    try
    {
      URIElementParser.parse(undecoable);
      Assert.fail();
    }
    catch (PathSegment.PathSegmentSyntaxException e)
    {
      Assert.assertEquals(e.getMessage(), expectedErrorMessage);
    }
  }

  @Test
  public void testParseURIParams() throws PathSegment.PathSegmentSyntaxException
  {
    Map<String, List<String>> queryParams = new HashMap<String, List<String>>();
    queryParams.put("aParam", Collections.singletonList("(someField:someValue,foo:bar,empty:())"));
    queryParams.put("bParam", Collections.singletonList("List(x,y,z)"));

    DataMap expectedQueryParams = new DataMap();
    DataMap aParamMap = new DataMap();
    aParamMap.put("someField", "someValue");
    aParamMap.put("foo", "bar");
    aParamMap.put("empty", new DataMap());
    DataList bParamList = new DataList();
    bParamList.add("x");
    bParamList.add("y");
    bParamList.add("z");
    expectedQueryParams.put("aParam", aParamMap);
    expectedQueryParams.put("bParam", bParamList);

    DataMap actualQueryParams = URIParamUtils.parseUriParams(queryParams);
    Assert.assertEquals(actualQueryParams, expectedQueryParams);
  }
}
