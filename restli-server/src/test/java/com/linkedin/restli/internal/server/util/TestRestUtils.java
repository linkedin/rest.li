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

import static org.testng.Assert.fail;

import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.internal.server.ResourceContextImpl;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.server.RestLiServiceException;

import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Nishanth Shankaran
 */

public class TestRestUtils
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
  private static final String PSON_HEADER = "application/x-pson";
  private static final String INVALID_TYPE_HEADER_1 = "foo";
  private static final String INVALID_TYPE_HEADER_2 = "foo, bar, baz";
  private static final String INVALID_TYPES_JSON_HEADER = "foo, bar, baz, application/json";
  private static final String INVALID_TYPES_HTML_HEADER = "foo, bar, baz, text/html";

  @DataProvider(name = "successfulMatch")
  public Object[][] provideSuccessfulMatchData()
  {
    return new Object[][] { { JSON_HEADER, JSON_TYPE }, { PSON_HEADER, PSON_TYPE }, { HTML_HEADER, EMPTY_TYPE },
        { UNKNOWN_TYPE_HEADER, EMPTY_TYPE }, { UNKNOWN_TYPE_HEADER_WITH_INVALID_PARAMS, EMPTY_TYPE },
        { UNKNOWN_TYPE_HEADER_WITH_UNKNOWN_PARAMS, EMPTY_TYPE }, { UNKNOWN_TYPE_HEADER_WITH_VALID_PARAMS, EMPTY_TYPE },
        { UNKNOWN_TYPE_HEADER_JSON, JSON_TYPE }, { UNKNOWN_TYPE_HEADER_WITH_INVALID_PARAMS_JSON, JSON_TYPE },
        { UNKNOWN_TYPE_HEADER_WITH_UNKNOWN_PARAMS_JSON, JSON_TYPE },
        { UNKNOWN_TYPE_HEADER_WITH_VALID_PARAMS_JSON, JSON_TYPE }
    };
  }

  @DataProvider(name = "invalidHeaders")
  public Object[][] provideInvalidHeadersData()
  {
    return new Object[][] { { INVALID_TYPE_HEADER_1 }, { INVALID_TYPE_HEADER_2 }, { INVALID_TYPES_JSON_HEADER },
        { INVALID_TYPES_HTML_HEADER } };
  }

  @Test(dataProvider = "successfulMatch")
  public void testPickBestEncodingWithValidMimeTypes(String header, String result)
  {
    Assert.assertEquals(RestUtils.pickBestEncoding(header), result);
  }

  @Test
  public void testPickBestEncodingWithNoMimeTypes()
  {
    Assert.assertNotEquals(RestUtils.pickBestEncoding(null), EMPTY_TYPE);
  }

  @Test(dataProvider = "invalidHeaders")
  public void testPickBestEncodingWithInvalidHeaders(String header)
  {
    try
    {
      RestUtils.pickBestEncoding(header);
      fail();
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
    Map<String, String> headers = new HashMap<String, String>();
    headers.put("Accept", "text/html");
    ServerResourceContext resourceContext = new ResourceContextImpl();
    try
    {
      RestUtils.validateRequestHeadersAndUpdateResourceContext(headers, resourceContext);
      fail();
    }
    catch (RestLiServiceException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_406_NOT_ACCEPTABLE);
      Assert.assertEquals(e.getMessage(),
                          "None of the types in the request's 'Accept' header are supported. Supported MIME types are: [application/x-pson, application/json]");
      Assert.assertEquals(resourceContext.getResponseMimeType(), null);
    }
  }

  @Test()
  public void testValidateRequestHeadersWithValidAcceptHeaderAndMatch() throws Exception
  {
    Map<String, String> headers = new HashMap<String, String>();
    headers.put("Accept", "application/json");
    ServerResourceContext resourceContext = new ResourceContextImpl();
    RestUtils.validateRequestHeadersAndUpdateResourceContext(headers, resourceContext);
    Assert.assertEquals(resourceContext.getResponseMimeType(), "application/json");
  }
}
