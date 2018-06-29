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
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.jersey.api.uri.UriComponent;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.test.MyComplexKey;

import java.net.URI;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class TestUriParamUtils
{
  private static final URLEscaper.Escaping NO_ESCAPING = URLEscaper.Escaping.NO_ESCAPING;
  private static final URLEscaper.Escaping URL_ESCAPING = URLEscaper.Escaping.URL_ESCAPING;

  @DataProvider
  private static Object[][] maps()
  {
    DataMap simpleMap = new DataMap();
    simpleMap.put("a", "b");

    DataMap longerMap = new DataMap();
    longerMap.put("a", "b");
    longerMap.put("c", "d");

    DataMap withEmptyStrings = new DataMap();
    withEmptyStrings.put("", "");

    DataMap nestedMap1 = new DataMap();
    nestedMap1.put("c", simpleMap);

    DataMap nestedMap2 = new DataMap();
    DataList list = new DataList();
    list.add(1);
    list.add(2);
    nestedMap2.put("empty", new DataMap());
    nestedMap2.put("list", list);
    nestedMap2.put("long", longerMap);

    return new Object [][] {
      { simpleMap, "(a:b)" },
      { longerMap, "(a:b,c:d)" },
      { new DataMap(), "()" },
      { withEmptyStrings, "('':'')" },
      { nestedMap1, "(c:(a:b))" },
      { nestedMap2, "(empty:(),list:List(1,2),long:(a:b,c:d))" }
    };
  }

  @Test(dataProvider = "maps")
  public void testStringMapElement(DataMap map, String expectedElement)
  {
    String stringSimpleMap = URIParamUtils.encodeElement(map, NO_ESCAPING, null);
    Assert.assertEquals(stringSimpleMap, expectedElement);
  }

  @DataProvider
  private static Object[][] lists()
  {
    DataList simpleList = new DataList();
    simpleList.add("a");
    simpleList.add("b");

    DataList longerList = new DataList();
    longerList.add(1);
    longerList.add(2);
    longerList.add(3);
    longerList.add(4);
    longerList.add(5);

    DataList withEmptyString = new DataList();
    withEmptyString.add("");

    DataList nestedList1 = new DataList();
    nestedList1.add(simpleList);
    nestedList1.add(longerList);

    DataList nestedList2 = new DataList();
    DataMap map = new DataMap();
    map.put("a", "b");
    nestedList2.add(simpleList);
    nestedList2.add(map);

    return new Object [][] {
      { simpleList, "List(a,b)" },
      { longerList, "List(1,2,3,4,5)" },
      { new DataList(), "List()" },
      { withEmptyString, "List('')" },
      { nestedList1, "List(List(a,b),List(1,2,3,4,5))" },
      { nestedList2, "List(List(a,b),(a:b))" }
    };
  }

  @Test(dataProvider = "lists")
  public void testStringListElement(DataList list, String expectedElement)
  {
    String stringSimpleList = URIParamUtils.encodeElement(list, NO_ESCAPING, null);
    Assert.assertEquals(stringSimpleList, expectedElement);
  }

  @DataProvider
  public Object[][] encoding()
  {
    // create objects
    // test internal encoding
    // forbidden characters: '(),:
    DataMap internalEncodingMap = new DataMap();
    DataList internalEncodingList = new DataList();
    internalEncodingList.add("'");
    internalEncodingList.add("(");
    internalEncodingList.add(")");
    internalEncodingList.add(":");
    internalEncodingMap.put(",", internalEncodingList);

    // test external encoding
    DataMap externalEncodingMap = new DataMap();
    DataList externalEncodingList = new DataList();
    externalEncodingList.add("/");
    externalEncodingList.add("=");
    externalEncodingList.add("&");
    externalEncodingMap.put("this is a key", externalEncodingList);

    // test %
    DataMap percentEncodingMap = new DataMap();
    percentEncodingMap.put("%25", "%");

    return new Object[][] {
      { internalEncodingMap, "(%2C:List(%27,%28,%29,%3A))", "(%2C:List(%27,%28,%29,%3A))", "(%2C:List(%27,%28,%29,%3A))" },
      { externalEncodingMap, "(this is a key:List(/,=,&))", "(this%20is%20a%20key:List(%2F,=,&))", "(this%20is%20a%20key:List(/,%3D,%26))" },
      { percentEncodingMap, "(%2525:%25)", "(%2525:%25)", "(%2525:%25)" }
    };
  }

  @Test(dataProvider = "encoding")
  public void testEncoding(Object obj, String expectedNoEsc, String expectedPathSegEsc, String expectedQueryParamEsc)
  {
    String actualNoEsc = URIParamUtils.encodeElement(obj, NO_ESCAPING, null);
    Assert.assertEquals(actualNoEsc, expectedNoEsc);
    String actualPathSegEsc = URIParamUtils.encodeElement(obj, URL_ESCAPING,
                                                          UriComponent.Type.PATH_SEGMENT);
    Assert.assertEquals(actualPathSegEsc, expectedPathSegEsc);
    String actualQueryParamEsc = URIParamUtils.encodeElement(obj, URL_ESCAPING,
                                                             UriComponent.Type.QUERY_PARAM);
    Assert.assertEquals(actualQueryParamEsc, expectedQueryParamEsc);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "complexKey")
  public Object[][] complexKey()
  {
    return new Object[][]
      {
        { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "$params.a=anotherStringVal&$params.b=4&a=stringVal&b=3",
          "a=stringVal&b=3" },
        { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "($params:(a:anotherStringVal,b:4),a:stringVal,b:3)",
          "(a:stringVal,b:3)" }
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "complexKey")
  public void testComplexKeyToString(ProtocolVersion version, String full, String notFull)
  {
    MyComplexKey myComplexKey1 = new MyComplexKey();
    myComplexKey1.setA("stringVal");
    myComplexKey1.setB(3);
    MyComplexKey myComplexKey2 = new MyComplexKey();
    myComplexKey2.setA("anotherStringVal");
    myComplexKey2.setB(4);
    ComplexResourceKey<MyComplexKey, MyComplexKey> complexKey =
      new ComplexResourceKey<MyComplexKey, MyComplexKey>(myComplexKey1, myComplexKey2);
    String complexKeyString = URIParamUtils.keyToString(complexKey, NO_ESCAPING, null, true, version);
    Assert.assertEquals(complexKeyString, full);

    // not full
    String complexKeyStringNotFull = URIParamUtils.keyToString(complexKey, NO_ESCAPING, null, false, version);
    Assert.assertEquals(complexKeyStringNotFull, notFull);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "compoundKey")
  public Object[][] compoundKey()
  {
    return new Object[][]
      {
        { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "key1=stringVal&key2=5&key3=VALUE_1" },
        { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "(key1:stringVal,key2:5,key3:VALUE_1)" }
      };
  }

  public enum TestEnum
  {
    VALUE_1,
    VALUE_2,
    $UNKNOWN;
    private final static EnumDataSchema SCHEMA = ((EnumDataSchema) DataTemplateUtil.parseSchema(
        "{\"type\":\"enum\",\"name\":\"TestEnum\",\"namespace\":\"com.linkedin.restli.internal.common\",\"symbols\":[\"VALUE_1\",\"VALUE_2\"]}"));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "compoundKey")
  public void testCompoundKeyToString(ProtocolVersion version, String expected)
  {
    CompoundKey compoundKey = new CompoundKey();
    compoundKey.append("key1", "stringVal");
    compoundKey.append("key2", 5);
    compoundKey.append("key3", TestEnum.VALUE_1);
    String compoundKeyString = URIParamUtils.keyToString(compoundKey, NO_ESCAPING, null, true, version);
    Assert.assertEquals(compoundKeyString, expected);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "longKey")
  public Object[][] longKey()
  {
    return new Object[][]
      {
        { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "6" },
        { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "6" }
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "longKey")
  public void testLongKeyToString(ProtocolVersion version, String expected)
  {
    Long longKey = 6L;
    String longKeyString = URIParamUtils.keyToString(longKey, NO_ESCAPING, null, true, version);
    Assert.assertEquals(longKeyString, expected);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "stringKey")
  public Object[][] stringKey()
  {
    return new Object[][]
      {
        { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "key" },
        { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "key" }
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "stringKey")
  public void testStringKeyToString(ProtocolVersion version, String expected)
  {
    String stringKey = "key";
    String stringKeyString = URIParamUtils.keyToString(stringKey, NO_ESCAPING, null, true, version);
    Assert.assertEquals(stringKeyString, expected);
  }

  @Test
  public void addSortedParams()
  {
    DataMap queryParams = new DataMap();
    DataMap aParamMap = new DataMap();
    aParamMap.put("someField", "someValue");
    aParamMap.put("foo", "bar");
    aParamMap.put("empty", new DataMap());
    DataList bParamList = new DataList();
    bParamList.add("x");
    bParamList.add("y");
    bParamList.add("z");
    queryParams.put("aParam", aParamMap);
    queryParams.put("bParam", bParamList);

    UriBuilder uriBuilder = new UriBuilder();
    URIParamUtils.addSortedParams(uriBuilder, queryParams);
    String query = uriBuilder.build().getQuery();

    Assert.assertEquals(query, "aParam=(empty:(),foo:bar,someField:someValue)&bParam=List(x,y,z)");
  }

  @Test
  public void replaceQueryParam()
  {
    DataMap queryParams = new DataMap();
    queryParams.put("bq", "batch_finder");
    queryParams.put("page", "1");
    queryParams.put("count", "10");



    DataMap criteria1 = new DataMap();
    criteria1.put("criteria1_fieldA", "valueA");
    criteria1.put("criteria1_fieldB", "valueB");
    criteria1.put("criteria1_fieldC", "valueC");
    DataMap criteria2 = new DataMap();
    criteria2.put("criteria2_fieldA", "valueA");
    criteria2.put("criteria2_fieldB", "valueB");
    criteria2.put("criteria2_fieldC", "valueC");

    DataList paramList = new DataList();
    paramList.add(criteria1);
    paramList.add(criteria2);
    queryParams.put("criteria", paramList);

    UriBuilder uriBuilder = new UriBuilder();
    URIParamUtils.addSortedParams(uriBuilder, queryParams);
    URI uri = uriBuilder.build();


    DataList newParamList = new DataList();
    newParamList.add(criteria1);

    URI replacedURIV1 = URIParamUtils.replaceQueryParam(uri,
                                             "criteria",
                                                        newParamList,
                                                        queryParams,
                                                        AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion());


    URI replacedURIV2 = URIParamUtils.replaceQueryParam(uri,
                                            "criteria",
                                                        newParamList,
                                                        queryParams,
                                                        AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion());



    String expectedURI = "bq=batch_finder&count=10&criteria=List((criteria1_fieldA:valueA,criteria1_fieldB:valueB,"
        + "criteria1_fieldC:valueC),(criteria2_fieldA:valueA,criteria2_fieldB:valueB,criteria2_fieldC:valueC))&page=1";
    String expectedNewURIV2 = "bq=batch_finder&count=10&criteria=List((criteria1_fieldA:valueA,criteria1_fieldB:valueB,"
        + "criteria1_fieldC:valueC))&page=1";
    String expectedNewURIV1 = "bq=batch_finder&count=10&criteria[0].criteria1_fieldA=valueA"
        + "&criteria[0].criteria1_fieldB=valueB&criteria[0].criteria1_fieldC=valueC&page=1";


    Assert.assertEquals(uri.getQuery(), expectedURI);
    Assert.assertEquals(replacedURIV2.getQuery(), expectedNewURIV2);
    Assert.assertEquals(replacedURIV1.getQuery(), expectedNewURIV1);
  }


  @Test
  public void testProjectionMask()
  {
    DataMap queryParams = new DataMap();

    DataMap fields = new DataMap();
    fields.put("name", 1);

    DataMap friends = new DataMap();
    friends.put("$start", 1);
    friends.put("$count", 2);
    fields.put("friends", friends);

    queryParams.put("fields", fields);

    DataMap paramMap = new DataMap();
    paramMap.put("foo", "bar");
    paramMap.put("empty", new DataMap());

    queryParams.put("aParam", paramMap);

    DataList paramList = new DataList();
    paramList.add("x");
    paramList.add("y");
    paramList.add("z");

    queryParams.put("bParam", paramList);

    UriBuilder uriBuilder = new UriBuilder();
    URIParamUtils.addSortedParams(uriBuilder, queryParams);
    URI uri = uriBuilder.build();

    String query = uri.getQuery();
    Assert.assertEquals(query, "aParam=(empty:(),foo:bar)&bParam=List(x,y,z)&fields=name,friends:($start:1,$count:2)");

    String rawQuery = uri.getRawQuery();
    Assert.assertEquals(rawQuery, "aParam=(empty:(),foo:bar)&bParam=List(x,y,z)&fields=name,friends:($start:1,$count:2)");
  }

  @Test
  public void testExtractionWithTemplateVariables()
  {
    final String[] components1 = URIParamUtils.extractPathComponentsFromUriTemplate("foo");
    Assert.assertEquals(components1.length, 1);
    Assert.assertEquals(components1[0], "foo");

    final String[] components2 = URIParamUtils.extractPathComponentsFromUriTemplate("foo/{keys}/bar");
    Assert.assertEquals(components2.length, 2);
    Assert.assertEquals(components2[0], "foo");
    Assert.assertEquals(components2[1], "bar");

    final String[] components3 = URIParamUtils.extractPathComponentsFromUriTemplate("foo/{keys1}/bar/{keys2}/baz");
    Assert.assertEquals(components3.length, 3);
    Assert.assertEquals(components3[0], "foo");
    Assert.assertEquals(components3[1], "bar");
    Assert.assertEquals(components3[2], "baz");

    final String[] components4 = URIParamUtils.extractPathComponentsFromUriTemplate("foo/{keys1}/{keys2}/bar");
    Assert.assertEquals(components4.length, 2);
    Assert.assertEquals(components4[0], "foo");
    Assert.assertEquals(components4[1], "bar");
  }

  @Test
  public void testExtractionWithoutTemplateVariables()
  {
    final String[] components = URIParamUtils.extractPathComponentsFromUriTemplate("foo/bar");
    Assert.assertEquals(components.length, 2);
    Assert.assertEquals(components[0], "foo");
    Assert.assertEquals(components[1], "bar");
  }

  @Test
  public void testExtractionWithSlashes()
  {
    final String[] components1 = URIParamUtils.extractPathComponentsFromUriTemplate("/foo");
    Assert.assertEquals(components1.length, 1);
    Assert.assertEquals(components1[0], "foo");

    final String[] components2 = URIParamUtils.extractPathComponentsFromUriTemplate("foo/");
    Assert.assertEquals(components2.length, 1);
    Assert.assertEquals(components2[0], "foo");
  }

  @DataProvider
  public Object[][] unicode()
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
        { japaneseMap, "(konnichiwa:こんにちは)", "(konnichiwa:%E3%81%93%E3%82%93%E3%81%AB%E3%81%A1%E3%81%AF)", "(konnichiwa:%E3%81%93%E3%82%93%E3%81%AB%E3%81%A1%E3%81%AF)" },
        { emojiMap, "(smiley:☺)", "(smiley:%E2%98%BA)", "(smiley:%E2%98%BA)"},
        { surrogatePairMap, "(stickoutTongue:\uD83D\uDE1B)", "(stickoutTongue:%F0%9F%98%9B)","(stickoutTongue:%F0%9F%98%9B)" }
    };
  }

  @Test(dataProvider = "unicode")
  public void testUnicode(Object obj, String expectedNoEsc, String expectedPathSegEsc, String expectedQueryParamEsc)
  {
    String actualNoEsc = URIParamUtils.encodeElement(obj, NO_ESCAPING, null);
    Assert.assertEquals(actualNoEsc, expectedNoEsc);
    String actualPathSegEsc = URIParamUtils.encodeElement(obj, URL_ESCAPING,
        UriComponent.Type.PATH_SEGMENT);
    Assert.assertEquals(actualPathSegEsc, expectedPathSegEsc);
    String actualQueryParamEsc = URIParamUtils.encodeElement(obj, URL_ESCAPING,
        UriComponent.Type.QUERY_PARAM);
    Assert.assertEquals(actualQueryParamEsc, expectedQueryParamEsc);
  }
}
