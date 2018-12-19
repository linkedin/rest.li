package com.linkedin.data.codec;

import com.linkedin.data.codec.entitystream.StreamDataCodec;
import java.util.Map;


/**
 * Provides a custom codec instance based on the request headers.
 *
 * <p>This is useful for codecs like KSON that pick the symbol table to use based on the header.</p>
 */
public interface HeaderBasedCodecProvider {

  /**
   * Get a codec instance based on the given request headers.
   *
   * @param requestHeaders The request headers
   * @return The codec for the given request header mapping.
   */
  DataCodec getCodec(Map<String, String> requestHeaders);

  /**
   * Get a stream codec instance based on the given request headers.
   *
   * @param requestHeaders The request headers
   * @return The stream codec for the given request header mapping.
   */
  StreamDataCodec getStreamCodec(Map<String, String> requestHeaders);
}
