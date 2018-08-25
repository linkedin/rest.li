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

package com.linkedin.restli.common;

import com.linkedin.data.codec.DataCodec;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.codec.PsonDataCodec;
import com.linkedin.data.codec.JacksonSmileDataCodec;
import com.linkedin.data.codec.entitystream.JacksonStreamDataCodec;
import com.linkedin.data.codec.entitystream.JacksonSmileStreamDataCodec;
import com.linkedin.data.codec.entitystream.StreamDataCodec;
import com.linkedin.r2.filter.R2Constants;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;


/**
 * Rest.Li representation of supported content types. Each content type is associated with a CODEC that will be used
 * to serialize/de-serialize the content.
 *
 * @author Karthik Balasubramanian
 */
public class ContentType
{
  private static final JacksonDataCodec JACKSON_DATA_CODEC = new JacksonDataCodec();
  private static final JacksonStreamDataCodec JACKSON_STREAM_DATA_CODEC = new JacksonStreamDataCodec(R2Constants.DEFAULT_DATA_CHUNK_SIZE);
  private static final PsonDataCodec PSON_DATA_CODEC = new PsonDataCodec();
  private static final JacksonSmileDataCodec SMILE_DATA_CODEC = new JacksonSmileDataCodec();
  private static final JacksonSmileStreamDataCodec SMILE_STREAM_DATA_CODEC = new JacksonSmileStreamDataCodec(R2Constants.DEFAULT_DATA_CHUNK_SIZE);

  public static final ContentType PSON =
      new ContentType(RestConstants.HEADER_VALUE_APPLICATION_PSON, PSON_DATA_CODEC, null);
  public static final ContentType JSON =
      new ContentType(RestConstants.HEADER_VALUE_APPLICATION_JSON, JACKSON_DATA_CODEC, JACKSON_STREAM_DATA_CODEC);
  public static final ContentType SMILE =
      new ContentType(RestConstants.HEADER_VALUE_APPLICATION_SMILE, SMILE_DATA_CODEC, SMILE_STREAM_DATA_CODEC);
  // Content type to be used only as an accept type.
  public static final ContentType ACCEPT_TYPE_ANY =
      new ContentType(RestConstants.HEADER_VALUE_ACCEPT_ANY, JACKSON_DATA_CODEC, null);

  private static final Map<String, ContentType> SUPPORTED_TYPES = new ConcurrentHashMap<>();
  static
  {
    // Include content types supported by Rest.Li by default.
    SUPPORTED_TYPES.put(PSON.getHeaderKey(), PSON);
    SUPPORTED_TYPES.put(JSON.getHeaderKey(), JSON);
    SUPPORTED_TYPES.put(SMILE.getHeaderKey(), SMILE);
  }

  /**
   * Helper method to create a custom content type and also register it as a supported type.
   * @param headerKey Content-Type header value to associate this content type with.
   * @param codec Codec to use for this content type.
   *
   * @return A ContentType representing this custom type that can be use with restli framework.
   */
  public static ContentType createContentType(String headerKey, DataCodec codec)
  {
    return createContentType(headerKey, codec, null);
  }

  /**
   * Helper method to create a custom content type and also register it as a supported type.
   * @param headerKey Content-Type header value to associate this content type with.
   * @param codec Codec to use for this content type.
   * @param streamCodec A {@link StreamDataCodec} to use for this content type.
   *
   * @return A ContentType representing this custom type that can be use with restli framework.
   */
  public static ContentType createContentType(String headerKey, DataCodec codec, StreamDataCodec streamCodec)
  {
    assert headerKey != null : "Header key for custom content type cannot be null";
    assert codec != null : "Codec for custom content type cannot be null";
    ContentType customType = new ContentType(headerKey, codec, streamCodec);
    SUPPORTED_TYPES.put(headerKey.toLowerCase(), customType);
    return customType;
  }

  /**
   * Get content type based on the given mime type
   * @param contentTypeHeaderValue value of Content-Type header.
   * @return type of content Restli supports. Can be null if the Content-Type header does not match any of the supported
   * content types.
   *
   * @throws MimeTypeParseException thrown when content type is not parsable.
   */
  public static Optional<ContentType> getContentType(String contentTypeHeaderValue) throws MimeTypeParseException
  {
    if (contentTypeHeaderValue == null)
    {
      return Optional.of(JSON);
    }
    MimeType parsedMimeType = new MimeType(contentTypeHeaderValue);
    return Optional.ofNullable(SUPPORTED_TYPES.get(parsedMimeType.getBaseType().toLowerCase()));
  }

  private final String _headerKey;
  private final DataCodec _codec;
  private final StreamDataCodec _streamCodec;

  /** Constructable only through {@link ContentType#createContentType(String, DataCodec)} */
  private ContentType(String headerKey, DataCodec codec, StreamDataCodec streamCodec)
  {
    _headerKey = headerKey;
    _codec = codec;
    _streamCodec = streamCodec;
  }

  public String getHeaderKey()
  {
    return _headerKey;
  }

  public DataCodec getCodec()
  {
    return _codec;
  }

  public StreamDataCodec getStreamCodec()
  {
    return _streamCodec;
  }

  @Override
  public String toString()
  {
    return _headerKey;
  }
}
