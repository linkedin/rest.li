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
import com.linkedin.data.codec.JacksonLICORDataCodec;
import com.linkedin.data.codec.JacksonSmileDataCodec;
import com.linkedin.data.codec.ProtobufDataCodec;
import com.linkedin.data.codec.PsonDataCodec;
import com.linkedin.data.codec.entitystream.JacksonLICORStreamDataCodec;
import com.linkedin.data.codec.entitystream.JacksonSmileStreamDataCodec;
import com.linkedin.data.codec.entitystream.JacksonStreamDataCodec;
import com.linkedin.data.codec.entitystream.StreamDataCodec;
import com.linkedin.r2.filter.R2Constants;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Rest.Li representation of supported content types. Each content type is associated with a CODEC that will be used
 * to serialize/de-serialize the content.
 *
 * @author Karthik Balasubramanian
 */
public class ContentType
{
  private static final Logger LOG = LoggerFactory.getLogger(ContentType.class);
  private static final JacksonDataCodec JACKSON_DATA_CODEC = new JacksonDataCodec();
  private static final JacksonStreamDataCodec JACKSON_STREAM_DATA_CODEC = new JacksonStreamDataCodec(R2Constants.DEFAULT_DATA_CHUNK_SIZE);
  private static final JacksonLICORDataCodec LICOR_TEXT_DATA_CODEC = new JacksonLICORDataCodec(false);
  private static final JacksonLICORStreamDataCodec
      LICOR_TEXT_STREAM_DATA_CODEC = new JacksonLICORStreamDataCodec(R2Constants.DEFAULT_DATA_CHUNK_SIZE, false);
  private static final JacksonLICORDataCodec LICOR_BINARY_DATA_CODEC = new JacksonLICORDataCodec(true);
  private static final JacksonLICORStreamDataCodec
      LICOR_BINARY_STREAM_DATA_CODEC = new JacksonLICORStreamDataCodec(R2Constants.DEFAULT_DATA_CHUNK_SIZE, true);
  private static final ProtobufDataCodec PROTOBUF_DATA_CODEC = new ProtobufDataCodec();
  private static final PsonDataCodec PSON_DATA_CODEC = new PsonDataCodec();
  private static final JacksonSmileDataCodec SMILE_DATA_CODEC = new JacksonSmileDataCodec();
  private static final JacksonSmileStreamDataCodec SMILE_STREAM_DATA_CODEC = new JacksonSmileStreamDataCodec(R2Constants.DEFAULT_DATA_CHUNK_SIZE);

  public static final ContentType PSON =
      new ContentType(RestConstants.HEADER_VALUE_APPLICATION_PSON, PSON_DATA_CODEC, null);
  public static final ContentType JSON =
      new ContentType(RestConstants.HEADER_VALUE_APPLICATION_JSON, JACKSON_DATA_CODEC, JACKSON_STREAM_DATA_CODEC);
  public static final ContentType LICOR_TEXT =
      new ContentType(RestConstants.HEADER_VALUE_APPLICATION_LICOR_TEXT, LICOR_TEXT_DATA_CODEC,
          LICOR_TEXT_STREAM_DATA_CODEC);
  public static final ContentType LICOR_BINARY =
      new ContentType(RestConstants.HEADER_VALUE_APPLICATION_LICOR_BINARY, LICOR_BINARY_DATA_CODEC,
          LICOR_BINARY_STREAM_DATA_CODEC);
  public static final ContentType SMILE =
      new ContentType(RestConstants.HEADER_VALUE_APPLICATION_SMILE, SMILE_DATA_CODEC, SMILE_STREAM_DATA_CODEC);
  public static final ContentType PROTOBUF =
      new ContentType(RestConstants.HEADER_VALUE_APPLICATION_PROTOBUF, PROTOBUF_DATA_CODEC, null);
  // Content type to be used only as an accept type.
  public static final ContentType ACCEPT_TYPE_ANY =
      new ContentType(RestConstants.HEADER_VALUE_ACCEPT_ANY, JACKSON_DATA_CODEC, null);

  private static final Map<String, ContentTypeProvider> SUPPORTED_TYPE_PROVIDERS = new ConcurrentHashMap<>();
  static
  {
    // Include content types supported by Rest.li by default.
    SUPPORTED_TYPE_PROVIDERS.put(JSON.getHeaderKey(), (rawMimeType, mimeType) -> JSON);
    SUPPORTED_TYPE_PROVIDERS.put(PSON.getHeaderKey(), (rawMimeType, mimeType) -> PSON);
    SUPPORTED_TYPE_PROVIDERS.put(SMILE.getHeaderKey(), (rawMimeType, mimeType) -> SMILE);
    SUPPORTED_TYPE_PROVIDERS.put(PROTOBUF.getHeaderKey(),
        new SymbolTableBasedContentTypeProvider(PROTOBUF,
            (rawMimeType, symbolTable) -> new ContentType(rawMimeType, new ProtobufDataCodec(symbolTable), null)));
    SUPPORTED_TYPE_PROVIDERS.put(LICOR_TEXT.getHeaderKey(),
        new SymbolTableBasedContentTypeProvider(LICOR_TEXT,
            (rawMimeType, symbolTable) -> new ContentType(rawMimeType,
                new JacksonLICORDataCodec(false, symbolTable),
                new JacksonLICORStreamDataCodec(R2Constants.DEFAULT_DATA_CHUNK_SIZE, false, symbolTable))));
    SUPPORTED_TYPE_PROVIDERS.put(LICOR_BINARY.getHeaderKey(),
        new SymbolTableBasedContentTypeProvider(LICOR_BINARY,
            (rawMimeType, symbolTable) -> new ContentType(rawMimeType,
                new JacksonLICORDataCodec(true, symbolTable),
                new JacksonLICORStreamDataCodec(R2Constants.DEFAULT_DATA_CHUNK_SIZE, true, symbolTable))));
  }

  /**
   * Helper method to create a custom content type and also register it as a supported type.
   * @param headerKey Content-Type header value to associate this content type with.
   * @param codec Codec to use for this content type.
   *
   * @return The created content type
   */
  public static ContentType createContentType(String headerKey, DataCodec codec)
  {
    return createContentType(headerKey, codec, null);
  }

  /**
   * Helper method to create a custom content type and also register it as a supported type.
   * @param headerKey Content-Type header base mime type value to associate this content type with.
   * @param codec Codec to use for this content type.
   * @param streamCodec An optional {@link StreamDataCodec} to use for this content type.
   *
   * @return The created content type
   */
  public static ContentType createContentType(String headerKey, DataCodec codec, StreamDataCodec streamCodec)
  {
    final ContentType contentType = getContentType(headerKey, codec, streamCodec);
    SUPPORTED_TYPE_PROVIDERS.put(headerKey, (rawMimeType, mimeType) -> contentType);
    return contentType;
  }

  /**
   * @deprecated Use {@link #createContentType(String, DataCodec, StreamDataCodec)} instead, as header-based codec
   *             providers are no longer used.
   */
  @Deprecated
  public static ContentType createContentType(String headerKey, DataCodec codec, StreamDataCodec streamCodec,
      com.linkedin.data.codec.HeaderBasedCodecProvider headerBasedCodecProvider)
  {
    return createContentType(headerKey, codec, streamCodec);
  }

  /**
   * Helper method to create a custom content type and also register it as a supported type.
   * @param headerKey Content-Type header base mime type value to associate this content type with.
   * @param provider A {@link ContentTypeProvider} to provide the actual content type.
   */
  public static void createContentType(String headerKey, ContentTypeProvider provider)
  {
    assert headerKey != null : "Header key for custom content type cannot be null";
    assert provider != null : "Provider for custom content type cannot be null";
    SUPPORTED_TYPE_PROVIDERS.put(headerKey.toLowerCase(), provider);
  }

  /**
   * Helper method to create a custom content type without registering it as a supported type.
   * @param headerKey Content-Type header base mime type value to associate this content type with.
   * @param codec Codec to use for this content type.
   * @param streamCodec An optional {@link StreamDataCodec} to use for this content type.
   *
   * @return The created content type
   */
  public static ContentType getContentType(String headerKey, DataCodec codec, StreamDataCodec streamCodec)
  {
    assert headerKey != null : "Header key for custom content type cannot be null";
    assert codec != null : "Codec for custom content type cannot be null";
    return new ContentType(headerKey, codec, streamCodec);
  }

  /**
   * Get content type based on the given mime type. This is to be used when decoding request/response bodies.
   *
   * @param contentTypeHeaderValue value of Content-Type header.
   * @return type of content Rest.li supports. Can be empty if the Content-Type header does not match any of the supported
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
    MimeType parsedMimeType = parseMimeType(contentTypeHeaderValue);
    ContentTypeProvider provider = SUPPORTED_TYPE_PROVIDERS.get(parsedMimeType.getBaseType().toLowerCase());
    if (provider == null)
    {
      return Optional.empty();
    }
    return Optional.of(provider.getContentType(contentTypeHeaderValue, parsedMimeType));
  }

  /**
   * Get content type to use for encoding the request body.
   *
   * @param rawMimeType Raw value of the mime type.
   * @param requestUri The request URI
   * @return type of content Rest.li supports. Can be empty if the mime type does not match any of the supported
   * content types.
   *
   * @throws MimeTypeParseException thrown when mime type is not parsable.
   */
  public static Optional<ContentType> getRequestContentType(String rawMimeType, URI requestUri) throws MimeTypeParseException
  {
    if (rawMimeType == null)
    {
      return Optional.of(JSON);
    }
    MimeType parsedMimeType = parseMimeType(rawMimeType);
    ContentTypeProvider provider = SUPPORTED_TYPE_PROVIDERS.get(parsedMimeType.getBaseType().toLowerCase());
    if (provider == null)
    {
      return Optional.empty();
    }
    return Optional.of(provider.getRequestContentType(rawMimeType, parsedMimeType, requestUri));
  }

  /**
   * Get content type to use for encoding the response body.
   *
   * @param rawMimeType Raw value of the mime type.
   * @param requestUri The request URI
   * @param requestHeaders The request headers.
   * @return type of content Rest.li supports. Can be empty if the mime type does not match any of the supported
   * content types.
   *
   * @throws MimeTypeParseException thrown when mime type is not parsable.
   */
  public static Optional<ContentType> getResponseContentType(String rawMimeType, URI requestUri, Map<String, String> requestHeaders)
      throws MimeTypeParseException
  {
    if (rawMimeType == null)
    {
      return Optional.of(JSON);
    }
    MimeType parsedMimeType = parseMimeType(rawMimeType);
    ContentTypeProvider provider = SUPPORTED_TYPE_PROVIDERS.get(parsedMimeType.getBaseType().toLowerCase());
    if (provider == null)
    {
      return Optional.empty();
    }
    return Optional.of(provider.getResponseContentType(rawMimeType, parsedMimeType, requestUri, requestHeaders));
  }

  private static MimeType parseMimeType(String rawMimeType) throws MimeTypeParseException
  {
    try
    {
      return new MimeType(rawMimeType);
    }
    catch (MimeTypeParseException e)
    {
      LOG.error("Exception parsing mime type: " + rawMimeType, e);
      throw e;
    }
  }

  private final String _headerKey;
  private final DataCodec _codec;
  private final StreamDataCodec _streamCodec;

  /**
   * Constructable only through
   * {@link ContentType#getContentType(String, DataCodec, StreamDataCodec)}
   */
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

  /**
   * @deprecated Use {@link #getCodec()} instead, as the headers are no longer read when getting the codec.
   */
  @Deprecated
  public DataCodec getCodec(Map<String, String> requestHeaders)
  {
    return getCodec();
  }

  public boolean supportsStreaming()
  {
    return _streamCodec != null;
  }

  public StreamDataCodec getStreamCodec()
  {
    return _streamCodec;
  }

  /**
   * @deprecated Use {@link #getStreamCodec()} instead, as the headers are no longer read when getting the codec.
   */
  @Deprecated
  public StreamDataCodec getStreamCodec(Map<String, String> requestHeaders)
  {
    return getStreamCodec();
  }

  @Override
  public String toString()
  {
    return _headerKey;
  }
}
