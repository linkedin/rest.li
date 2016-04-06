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


import com.linkedin.restli.server.InvalidMimeTypeException;

import java.util.Arrays;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author Nishanth Shankaran
 */
public class TestMIMEParse
{
  private static final String JSON_TYPE = "application/json";
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
  private static final String JSON_HEADER = "application/json";
  private static final String JSON_PSON_HTML_HEADER = "application/json, application/x-pson, text/html";
  private static final String INVALID_TYPE_HEADER_1 = "foo";
  private static final String INVALID_TYPE_HEADER_2 = "foo, bar, baz";
  private static final String INVALID_TYPES_JSON_HEADER = "foo, bar, baz, application/json";
  private static final String INVALID_TYPES_HTML_HEADER = "foo, bar, baz, text/html";

  @DataProvider(name = "successfulMatch")
  public Object[][] provideSuccessfulMatchData()
  {
    return new Object[][]
    {
        { Arrays.asList(new String[] { JSON_TYPE }), JSON_HEADER, JSON_TYPE },
        { Arrays.asList(new String[] { PSON_TYPE }), JSON_HEADER, EMPTY_TYPE },
        { Arrays.asList(new String[] { JSON_TYPE, PSON_TYPE }), JSON_HEADER, JSON_TYPE },
        { Arrays.asList(new String[] { JSON_TYPE, PSON_TYPE }), HTML_HEADER, EMPTY_TYPE },
        { Arrays.asList(new String[] { JSON_TYPE, PSON_TYPE }), UNKNOWN_TYPE_HEADER, EMPTY_TYPE },
        { Arrays.asList(new String[] { JSON_TYPE, PSON_TYPE }), UNKNOWN_TYPE_HEADER_WITH_INVALID_PARAMS, EMPTY_TYPE },
        { Arrays.asList(new String[] { JSON_TYPE, PSON_TYPE }), UNKNOWN_TYPE_HEADER_WITH_UNKNOWN_PARAMS, EMPTY_TYPE },
        { Arrays.asList(new String[] { JSON_TYPE, PSON_TYPE }), UNKNOWN_TYPE_HEADER_WITH_VALID_PARAMS, EMPTY_TYPE },
        { Arrays.asList(new String[] { JSON_TYPE, PSON_TYPE }), UNKNOWN_TYPE_HEADER_JSON, JSON_TYPE },
        { Arrays.asList(new String[] { JSON_TYPE, PSON_TYPE }), UNKNOWN_TYPE_HEADER_WITH_INVALID_PARAMS_JSON, JSON_TYPE },
        { Arrays.asList(new String[] { JSON_TYPE, PSON_TYPE }), UNKNOWN_TYPE_HEADER_WITH_UNKNOWN_PARAMS_JSON, JSON_TYPE },
        { Arrays.asList(new String[] { JSON_TYPE, PSON_TYPE }), UNKNOWN_TYPE_HEADER_WITH_VALID_PARAMS_JSON, JSON_TYPE }
    };
  }

  @DataProvider(name = "invalidHeaders")
  public Object[][] provideInvalidHeadersData()
  {
    return new Object[][]
    {
        { Arrays.asList(new String[] { JSON_TYPE, PSON_TYPE }), INVALID_TYPE_HEADER_1 },
        { Arrays.asList(new String[] { JSON_TYPE, PSON_TYPE }), INVALID_TYPE_HEADER_2 },
        { Arrays.asList(new String[] { JSON_TYPE, PSON_TYPE }), INVALID_TYPES_JSON_HEADER },
        { Arrays.asList(new String[] { JSON_TYPE, PSON_TYPE }), INVALID_TYPES_HTML_HEADER }
    };
  }

  @Test(dataProvider = "successfulMatch")
  public void testBestMatchForSuccessfullMatch(List<String> supportedTypes, String header, String result)
  {
    Assert.assertEquals(MIMEParse.bestMatch(supportedTypes, header), result);
  }

  @Test
  public void testBestMatchForForNonNullMatch()
  {
    Assert.assertNotEquals(MIMEParse.bestMatch(Arrays.asList(new String[] { JSON_TYPE, PSON_TYPE }),
                                               JSON_PSON_HTML_HEADER), EMPTY_TYPE);
  }

  @Test(dataProvider = "invalidHeaders", expectedExceptions = InvalidMimeTypeException.class)
  public void testBestMatchForInvalidHeaders(List<String> supportedTypes, String header)
  {
    MIMEParse.bestMatch(supportedTypes, header);
  }

  @DataProvider(name = "sampleValidAcceptHeaders")
  public Object[][] sampleAcceptHeaders()
  {
    return new Object[][]
    {
        { "multipart/related;q=1.0,application/x-pson;q=0.9,application/json;q=0.8", Arrays.asList("multipart/related",
                                                                                                   "application/x-pson",
                                                                                                   "application/json") },
        { "application/x-pson;q=1.0,multipart/related;q=0.9,*/*;q=0.8", Arrays.asList("application/x-pson",
                                                                                      "multipart/related",
                                                                                      "*/*") },
        { "application/json;q=1.0,application/x-pson;q=0.9,*/*;q=0.8,multipart/related;q=0.7", Arrays.asList("application/json",
                                                                                                             "application/x-pson",
                                                                                                             "*/*",
                                                                                                             "multipart/related") },
        { "application/x-pson,multipart/related", Arrays.asList("application/x-pson", "multipart/related") },
        { "multipart/related", Arrays.asList("multipart/related") }
    };
  }

  @Test(dataProvider = "sampleValidAcceptHeaders")
  public void testParseAcceptTypes(String header, List<String> supportedTypes)
  {
    Assert.assertEquals(MIMEParse.parseAcceptType(header), supportedTypes);
  }

  @DataProvider(name = "sampleInvalidAcceptHeaders")
  public Object[][] sampleInvalidAcceptHeaders()
  {
    return new Object[][]
    {
        { INVALID_TYPE_HEADER_1 },
        { INVALID_TYPE_HEADER_2 },
        { INVALID_TYPES_JSON_HEADER },
        { INVALID_TYPES_HTML_HEADER }
    };
  }

  @Test(dataProvider = "sampleInvalidAcceptHeaders", expectedExceptions = InvalidMimeTypeException.class)
  public void testParseAcceptInvalidTypes(String header)
  {
    MIMEParse.parseAcceptType(header);
  }
}