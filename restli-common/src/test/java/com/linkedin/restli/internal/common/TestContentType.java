/*
   Copyright (c) 2017 LinkedIn Corp.

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


import com.linkedin.restli.common.ContentType;
import org.testng.Assert;
import org.testng.annotations.Test;
import javax.activation.MimeTypeParseException;


public class TestContentType
{
  @Test
  public void testJSONContentType() throws MimeTypeParseException
  {
    ContentType contentType = ContentType.getContentType("application/json").get();
    Assert.assertEquals(contentType, ContentType.JSON);

    ContentType contentTypeWithParameter = ContentType.getContentType("application/json; charset=utf-8").get();
    Assert.assertEquals(contentTypeWithParameter, ContentType.JSON);
  }

  @Test
  public void testPSONContentType() throws MimeTypeParseException
  {
    ContentType contentType = ContentType.getContentType("application/x-pson; charset=utf-8").get();
    Assert.assertEquals(contentType, ContentType.PSON);

    ContentType contentTypeWithParameter = ContentType.getContentType("application/x-pson; charset=utf-8").get();
    Assert.assertEquals(contentTypeWithParameter, ContentType.PSON);
  }

  @Test
  public void testUnknowContentType() throws MimeTypeParseException
  {
    // Return Optional.empty for unknown types
    Assert.assertFalse(ContentType.getContentType("foo/bar").isPresent());

    Assert.assertFalse(ContentType.getContentType("foo/bar; foo=bar").isPresent());
  }

  @Test
  public void testNullContentType() throws MimeTypeParseException
  {
    ContentType contentType = ContentType.getContentType(null).get();
    Assert.assertEquals(ContentType.JSON, contentType);  // default to JSON for null content-type
  }

  @Test(expectedExceptions = MimeTypeParseException.class)
  public void testNonParsableContentType() throws MimeTypeParseException
  {
    // this should cause parse error
    ContentType.getContentType("application=json");
  }
}
