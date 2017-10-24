/*
 * Copyright (c) 2017 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.linkedin.restli.examples;


import com.linkedin.restli.common.RestConstants;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.restli.examples.greetings.server.GreetingUnstructuredDataUtils.MIME_TYPE;
import static com.linkedin.restli.examples.greetings.server.GreetingUnstructuredDataUtils.UNSTRUCTURED_DATA_BYTES;
import static org.testng.Assert.assertEquals;


/**
 * Integration tests for all types of unstructured data resources
 */
public class TestGreetingUnstructuredDataResourcesAsync extends UnstructuredDataResourceTestBase
{
  @DataProvider(name = "goodURLs")
  private static Object[][] goodURLs()
  {
    return new Object[][] {
      { "/greetingCollectionUnstructuredDataAsync/good" },
      { "/greetingCollectionUnstructuredDataPromise/good" },
      { "/greetingCollectionUnstructuredDataTask/good" },
      { "/greetingAssociationUnstructuredDataAsync/src=good&dest=bar" },
      { "/greetingSimpleUnstructuredDataAsync" }
    };
  }

  @Test(dataProvider = "goodURLs")
  public void testGetGood(String resourceURL)
    throws Throwable
  {
    sendGet(resourceURL, (conn) ->
    {
      assertEquals(conn.getResponseCode(), 200);
      assertEquals(conn.getHeaderField(RestConstants.HEADER_CONTENT_TYPE), MIME_TYPE);
      assertUnstructuredDataResponse(conn.getInputStream(), UNSTRUCTURED_DATA_BYTES);
    });
  }
}
