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

package com.linked.restli.docgen.test;


import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.restli.docgen.DefaultDocumentationRequestHandler;
import java.net.URI;
import java.net.URISyntaxException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author kparikh
 */
public class TestDefaultDocumentationRequestHandler
{
  private static final DefaultDocumentationRequestHandler DEFAULT_HANDLER = new DefaultDocumentationRequestHandler();
  private static final String BASE_URI = "http://localhost:123";

  private static RestRequest buildRestRequest(String uri)
      throws URISyntaxException
  {
    return new RestRequestBuilder(new URI(BASE_URI + uri)).build();
  }

  @DataProvider(name = "data")
  public Object[][] dataProvider()
      throws URISyntaxException
  {
    return new Object[][]
      {
          { buildRestRequest("/restli/docs/rest/foo"), true},
          { buildRestRequest("/restli/docs/rest/bar"), true},
          { buildRestRequest("/restli/rest/bar"), false},
          { buildRestRequest("/docs/rest/bar"), false},
          { buildRestRequest("/docs/restli/rest/bar"), false},
          { buildRestRequest("/restliFoo/docs/rest/foo"), false},
          { buildRestRequest("/fooRestli/docs/rest/foo"), false},
          { buildRestRequest("/foo/docs/rest/foo"), false},
          { buildRestRequest("/restli/docsrest/foo"), false},
          { buildRestRequest("/restli/Docs/rest/foo"), false},
          { buildRestRequest("/foo/restli/docs/rest/bar"), false}
      };
  }

  @Test(dataProvider = "data")
  public void testIsDocumentationRequest(RestRequest restRequest, boolean expectedIsDocumentationRequest)
  {
    Assert.assertEquals(DEFAULT_HANDLER.isDocumentationRequest(restRequest), expectedIsDocumentationRequest);
  }
}
