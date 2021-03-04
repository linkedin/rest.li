/*
   Copyright (c) 2013 LinkedIn Corp.

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

package com.linkedin.r2.filter.compression;

import com.linkedin.data.ByteString;
import java.io.InputStream;


/*
 * Interface for compressors.
 */
public interface Compressor
{
  /**
   * @return Corresponding value for the content-encoding for the implemented
   * compression method.
   * */
  String getContentEncodingName();

  /** Decompression function.
   *
   * @param data Byte array of data to be decompressed
   * @return Newly allocated byte array of decompressed of data, or null if error
   * @throws CompressionException if the data cannot be properly decompressed
   * */
  byte[] inflate(InputStream data) throws CompressionException;

  /**
   * Decompression function.
   *
   * @param data {@link ByteString} of compressed data.
   * @return {@link ByteString} with decompressed data.
   * @throws CompressionException if the data cannot be properly decompressed
   * */
  default ByteString inflate(ByteString data) throws CompressionException
  {
    return ByteString.unsafeWrap(inflate(data.asInputStream()));
  }

  /**
   * Compression function.
   *
   * @param data Byte array of data to be compressed
   * @return Newly allocated byte array of compressed data, or null if error
   * @throws CompressionException  if the data cannot be properly compressed
   * */
  byte[] deflate(InputStream data) throws CompressionException;

  /**
   * Compression function.
   *
   * @param data {@link ByteString} of decompressed data.
   * @return {@link ByteString} with compressed data.
   * @throws CompressionException if the data cannot be properly compressed
   * */
  default ByteString deflate(ByteString data) throws CompressionException
  {
    return ByteString.unsafeWrap(deflate(data.asInputStream()));
  }
}
