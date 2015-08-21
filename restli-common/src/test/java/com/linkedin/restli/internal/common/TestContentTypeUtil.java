/*
   Copyright (c) 2015 LinkedIn Corp.

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


import com.linkedin.restli.internal.common.ContentTypeUtil.ContentType;
import org.testng.Assert;
import org.testng.annotations.Test;
import javax.activation.MimeTypeParseException;


public class TestContentTypeUtil
{
  @Test
  public void testJSONContentType() throws MimeTypeParseException
  {
    ContentType contentType = ContentTypeUtil.getContentType("application/json");
    Assert.assertEquals(contentType, ContentType.JSON);

    ContentType contentTypeWithParameter = ContentTypeUtil.getContentType("application/json; charset=utf-8");
    Assert.assertEquals(contentTypeWithParameter, ContentType.JSON);
  }

  @Test
  public void testPSONContentType() throws MimeTypeParseException
  {
    ContentType contentType = ContentTypeUtil.getContentType("application/x-pson; charset=utf-8");
    Assert.assertEquals(contentType, ContentType.PSON);

    ContentType contentTypeWithParameter = ContentTypeUtil.getContentType("application/x-pson; charset=utf-8");
    Assert.assertEquals(contentTypeWithParameter, ContentType.PSON);
  }

  @Test
  public void testUnknowContentType() throws MimeTypeParseException
  {
    ContentType contentType = ContentTypeUtil.getContentType("foo/bar");
    Assert.assertEquals(contentType, ContentType.JSON);  // default to JSON for unknown content type

    ContentType contentTypeWithParameter = ContentTypeUtil.getContentType("foo/bar; foo=bar");
    Assert.assertEquals(contentTypeWithParameter, ContentType.JSON);  // default to JSON for unknown content type
  }

  @Test
  public void testNullContentType() throws MimeTypeParseException
  {
    ContentType contentType = ContentTypeUtil.getContentType(null);
    Assert.assertEquals(ContentType.JSON, contentType);  // default to JSON for null content-type
  }

  @Test(expectedExceptions = MimeTypeParseException.class)
  public void testNonParsableContentType() throws MimeTypeParseException
  {
    // this should cause parse error
    ContentTypeUtil.getContentType("application=json");
  }
}
