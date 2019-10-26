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
import com.linkedin.restli.common.ContentType;
import com.linkedin.restli.common.RestConstants;
import java.io.IOException;
import java.util.Map;
import javax.activation.MimeTypeParseException;


/**
 * Converter that converts DataMap to byteString and vice versa
 */
public class DataMapConverter
{
  /**
   * Convert from DataMap to ByteString based on the given Content-Type header value
   * @param headers headers of the HTTP request or response
   * @param dataMap data map to convert
   * @return converted ByString encoded based on content-type specified in the headers
   * @throws MimeTypeParseException throws this exception when content type is not parsable.
   * @throws IOException throws this exception when serializing to ByteString failed.
   */
  public static ByteString dataMapToByteString(Map<String, String> headers, DataMap dataMap) throws MimeTypeParseException, IOException
  {
    return ByteString.unsafeWrap(getContentType(headers).getCodec().mapToBytes(dataMap));
  }

  /**
   * Convert from ByteString to DataMap based on the given Content-Type header value
   * @param headers headers of the HTTP request or response
   * @param bytes ByteString to convert
   * @return converted DataMap.  ByteString is decoded based on content-type specified in the headers
   * @throws MimeTypeParseException throws this exception when content type is not parsable.
   * @throws IOException throws this exception when serializing to ByteString failed.
   */
  public static DataMap bytesToDataMap(Map<String, String> headers, ByteString bytes) throws MimeTypeParseException, IOException
  {
    return getContentType(headers).getCodec().readMap(bytes.asInputStream());
  }

  /**
   * Convert from DataMap to ByteString based on the given Content-Type header value
   * @param contentTypeHeaderValue type of ByteString to convert to
   * @param dataMap data map to convert
   * @return converted ByString encoded based on content-type specified in the headers
   * @throws MimeTypeParseException throws this exception when content type is not parsable.
   * @throws IOException throws this exception when serializing to ByteString failed.
   */
  public static ByteString dataMapToByteString(String contentTypeHeaderValue, DataMap dataMap) throws MimeTypeParseException, IOException
  {
    return ByteString.unsafeWrap(getContentType(contentTypeHeaderValue).getCodec().mapToBytes(dataMap));
  }

  /**
   * Convert from ByteString to DataMap based on the given Content-Type header value
   * @param contentTypeHeaderValue type of ByteString to convert from
   * @param bytes ByteString to convert
   * @return converted DataMap.  ByteString is decoded based on content-type specified in the headers
   * @throws MimeTypeParseException throws this exception when content type is not parsable.
   * @throws IOException throws this exception when serializing to ByteString failed.
   */
  public static DataMap bytesToDataMap(String contentTypeHeaderValue, ByteString bytes) throws MimeTypeParseException, IOException
  {
    return getContentType(contentTypeHeaderValue).getCodec().readMap(bytes.asInputStream());
  }

  public static ContentType getContentType(Map<String, String> headers) throws MimeTypeParseException
  {
    return getContentType(headers.get(RestConstants.HEADER_CONTENT_TYPE));
  }

  private static ContentType getContentType(String contentTypeHeaderValue) throws MimeTypeParseException
  {
    // TODO: We should throw an exception instead of using JSON for an unknown content type. This behavior was introduced
    // in commit d149605e4181349b64180bdfe0b4d24a294dc6f6 when this logic is refactored from DataMapUtils.readMapWithExceptions.
    return ContentType.getContentType(contentTypeHeaderValue).orElse(ContentType.JSON);
  }
}
