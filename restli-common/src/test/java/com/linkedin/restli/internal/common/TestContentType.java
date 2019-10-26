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


import com.linkedin.data.codec.symbol.InMemorySymbolTable;
import com.linkedin.data.codec.symbol.SymbolTable;
import com.linkedin.data.codec.symbol.SymbolTableProvider;
import com.linkedin.data.codec.symbol.SymbolTableProviderHolder;
import com.linkedin.restli.common.ContentType;
import com.linkedin.restli.common.RestConstants;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import javax.activation.MimeTypeParseException;


public class TestContentType
{
  private static final String TEST_REQUEST_SYMBOL_TABLE_NAME = "HahaRequest";
  private static final String TEST_RESPONSE_SYMBOL_TABLE_NAME = "HahaResponse";
  private static final URI TEST_URI = URI.create("https://www.linkedin.com");

  @BeforeClass
  public void setupSymbolTableProvider()
  {
    SymbolTableProviderHolder.INSTANCE.setSymbolTableProvider(new SymbolTableProvider() {
      @Override
      public SymbolTable getSymbolTable(String symbolTableName) {
        return null;
      }

      @Override
      public SymbolTable getRequestSymbolTable(URI requestUri) {
        return TEST_URI == requestUri ? new InMemorySymbolTable(TEST_REQUEST_SYMBOL_TABLE_NAME, Collections.emptyList()) : null;
      }

      @Override
      public SymbolTable getResponseSymbolTable(URI requestUri, Map<String, String> requestHeaders) {
        return TEST_URI == requestUri ? new InMemorySymbolTable(TEST_RESPONSE_SYMBOL_TABLE_NAME, Collections.emptyList()) : null;
      }
    });
  }

  @AfterClass
  public void tearDownSymbolTableProvider()
  {
    SymbolTableProviderHolder.INSTANCE.setSymbolTableProvider(new SymbolTableProvider() {});
  }

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
  public void testProtobufContentType() throws MimeTypeParseException
  {
    ContentType contentType = ContentType.getContentType("application/x-protobuf; charset=utf-8").get();
    Assert.assertEquals(contentType, ContentType.PROTOBUF);

    ContentType contentTypeWithParameter = ContentType.getContentType("application/x-protobuf; charset=utf-8").get();
    Assert.assertEquals(contentTypeWithParameter, ContentType.PROTOBUF);
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

  @Test
  public void testGetRequestNullContentType() throws MimeTypeParseException
  {
    ContentType contentType =
        ContentType.getRequestContentType(null, TEST_URI).get();
    Assert.assertEquals(ContentType.JSON, contentType);
  }

  @Test
  public void testGetRequestJSONContentType() throws MimeTypeParseException
  {
    ContentType contentType =
        ContentType.getRequestContentType(RestConstants.HEADER_VALUE_APPLICATION_JSON, TEST_URI).get();
    Assert.assertEquals(ContentType.JSON, contentType);
  }

  @Test
  public void testGetRequestProtobufContentType() throws MimeTypeParseException
  {
    ContentType contentType =
        ContentType.getRequestContentType(RestConstants.HEADER_VALUE_APPLICATION_PROTOBUF, TEST_URI).get();
    Assert.assertEquals("application/x-protobuf; symbol-table=HahaRequest", contentType.getHeaderKey());
  }

  @Test
  public void testGetResponseNullContentType() throws MimeTypeParseException
  {
    ContentType contentType =
        ContentType.getResponseContentType(null, TEST_URI, Collections.emptyMap()).get();
    Assert.assertEquals(ContentType.JSON, contentType);
  }

  @Test
  public void testGetResponseJSONContentType() throws MimeTypeParseException
  {
    ContentType contentType =
        ContentType.getResponseContentType(RestConstants.HEADER_VALUE_APPLICATION_JSON, TEST_URI, Collections.emptyMap()).get();
    Assert.assertEquals(ContentType.JSON, contentType);
  }

  @Test
  public void testGetResponseProtobufContentType() throws MimeTypeParseException
  {
    ContentType contentType =
        ContentType.getResponseContentType(RestConstants.HEADER_VALUE_APPLICATION_PROTOBUF, TEST_URI, Collections.emptyMap()).get();
    Assert.assertEquals("application/x-protobuf; symbol-table=HahaResponse", contentType.getHeaderKey());
  }
}
