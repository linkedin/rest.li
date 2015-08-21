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


import com.linkedin.data.ByteString;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.codec.PsonDataCodec;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.Link;
import com.linkedin.restli.common.LinkArray;
import com.linkedin.restli.common.RestConstants;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.activation.MimeTypeParseException;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;


public class TestDataMapConverter
{
  private static final PsonDataCodec PSON_DATA_CODEC = new PsonDataCodec();
  private static final JacksonDataCodec JACKSON_DATA_CODEC = new JacksonDataCodec();

  @Test
  public void testDataMapToJSONByteString() throws MimeTypeParseException, IOException
  {
    DataMap testDataMap = createTestDataMap();
    byte[] expectedBytes = JACKSON_DATA_CODEC.mapToBytes(testDataMap);
    ByteString byteString = DataMapConverter.dataMapToByteString("application/json", testDataMap);
    Assert.assertEquals(byteString.copyBytes(), expectedBytes);

    Map<String, String> headers = Collections.singletonMap(RestConstants.HEADER_CONTENT_TYPE, "application/json");
    byteString = DataMapConverter.dataMapToByteString(headers, testDataMap);
    Assert.assertEquals(byteString.copyBytes(), expectedBytes);
  }

  @Test
  public void testDataMapToJSONByteStringWithUnsupportedContentType() throws MimeTypeParseException, IOException
  {
    // unsupport content type should fallback to JSON
    DataMap testDataMap = createTestDataMap();
    byte[] expectedBytes = JACKSON_DATA_CODEC.mapToBytes(testDataMap);
    ByteString byteString = DataMapConverter.dataMapToByteString("mysuperkool/xson", testDataMap);
    Assert.assertEquals(byteString.copyBytes(), expectedBytes);

    Map<String, String> headers = Collections.singletonMap(RestConstants.HEADER_CONTENT_TYPE, "mysuperkool/xson");
    byteString = DataMapConverter.dataMapToByteString(headers, testDataMap);
    Assert.assertEquals(byteString.copyBytes(), expectedBytes);
  }

  @Test
  public void testDataMapToPSONByteString() throws MimeTypeParseException, IOException
  {
    DataMap testDataMap = createTestDataMap();
    byte[] expectedBytes = PSON_DATA_CODEC.mapToBytes(testDataMap);
    ByteString byteString = DataMapConverter.dataMapToByteString("application/x-pson", testDataMap);
    Assert.assertEquals(byteString.copyBytes(), expectedBytes);

    Map<String, String> headers = Collections.singletonMap(RestConstants.HEADER_CONTENT_TYPE, "application/x-pson");
    byteString = DataMapConverter.dataMapToByteString(headers, testDataMap);
    Assert.assertEquals(byteString.copyBytes(), expectedBytes);
  }


  @Test
  public void testJSONByteStringToDataMap() throws MimeTypeParseException, IOException
  {
    DataMap expectedDataMap = createTestDataMap();
    ByteString byteString = ByteString.copy(JACKSON_DATA_CODEC.mapToBytes(expectedDataMap));
    DataMap dataMap = DataMapConverter.bytesToDataMap("application/json", byteString);
    Assert.assertEquals(dataMap, expectedDataMap);

    Map<String, String> headers = Collections.singletonMap(RestConstants.HEADER_CONTENT_TYPE, "application/json");
    dataMap = DataMapConverter.bytesToDataMap(headers, byteString);
    Assert.assertEquals(dataMap, expectedDataMap);
  }

  @Test
  public void testJSONByteStringToDataMapWithUnsupportedContentType() throws MimeTypeParseException, IOException
  {
    // unsupport content type should fallback to JSON
    DataMap expectedDataMap = createTestDataMap();
    ByteString byteString = ByteString.copy(JACKSON_DATA_CODEC.mapToBytes(expectedDataMap));
    DataMap dataMap = DataMapConverter.bytesToDataMap("mysuperkool/xson", byteString);
    Assert.assertEquals(dataMap, expectedDataMap);

    Map<String, String> headers = Collections.singletonMap(RestConstants.HEADER_CONTENT_TYPE, "mysuperkool/xson");
    dataMap = DataMapConverter.bytesToDataMap(headers, byteString);
    Assert.assertEquals(dataMap, expectedDataMap);
  }

  @Test
  public void testPSONByteStringToDataMap() throws MimeTypeParseException, IOException
  {
    DataMap expectedDataMap = createTestDataMap();
    ByteString byteString = ByteString.copy(PSON_DATA_CODEC.mapToBytes(expectedDataMap));
    DataMap dataMap = DataMapConverter.bytesToDataMap("application/x-pson", byteString);
    Assert.assertEquals(dataMap, expectedDataMap);

    Map<String, String> headers = Collections.singletonMap(RestConstants.HEADER_CONTENT_TYPE, "application/x-pson");
    dataMap = DataMapConverter.bytesToDataMap(headers, byteString);
    Assert.assertEquals(dataMap, expectedDataMap);
  }

  @Test(expectedExceptions = IOException.class)
  public void testInvalidJSONByteStringToDataMap() throws MimeTypeParseException, IOException
  {
    DataMapConverter.bytesToDataMap("application/json", ByteString.copy("helloWorld".getBytes()));
  }

  @Test(expectedExceptions = IOException.class)
  public void testInvalidPSONByteStringToDataMap() throws MimeTypeParseException, IOException
  {
    DataMapConverter.bytesToDataMap("application/x-pson", ByteString.copy("helloWorld".getBytes()));
  }

  @Test(expectedExceptions = IOException.class)
  public void testEmptyJSONByteStringToDataMap() throws MimeTypeParseException, IOException
  {
    DataMapConverter.bytesToDataMap("application/json", ByteString.copy(new byte[0]));
  }

  @Test(expectedExceptions = IOException.class)
  public void testEmptyPSONByteStringToDataMap() throws MimeTypeParseException, IOException
  {
    DataMapConverter.bytesToDataMap("application/x-pson", ByteString.copy(new byte[0]));
  }

  @Test(expectedExceptions = IOException.class)
  public void testByteStringToDataMapWithInvalidContentType() throws MimeTypeParseException, IOException
  {
    DataMap dataMap = createTestDataMap();
    ByteString byteString = ByteString.copy(JACKSON_DATA_CODEC.mapToBytes(dataMap));
    DataMapConverter.bytesToDataMap("application/x-pson", byteString);
  }

  @Test(expectedExceptions = MimeTypeParseException.class)
  public void testByteStringToDataMapWithNonParsableContentType() throws MimeTypeParseException, IOException
  {
    DataMap dataMap = createTestDataMap();
    ByteString byteString = ByteString.copy(JACKSON_DATA_CODEC.mapToBytes(dataMap));
    DataMapConverter.bytesToDataMap("foo=bar", byteString);
  }

  @Test(expectedExceptions = MimeTypeParseException.class)
  public void testDataMapToByteStringWithNonParsableContentType() throws MimeTypeParseException, IOException
  {
    DataMap dataMap = createTestDataMap();
    DataMapConverter.dataMapToByteString("application::json", dataMap);
  }

  private DataMap createTestDataMap()
  {
    CollectionMetadata someRecord = new CollectionMetadata();
    someRecord.setCount(1);
    someRecord.setStart(0);
    someRecord.setTotal(10);
    LinkArray links = new LinkArray();
    Link link = new Link();
    link.setHref("prevUri");
    link.setRel("prev");
    link.setType("en");
    links.add(link);
    someRecord.setLinks(links);
    return someRecord.data();
  }
}
