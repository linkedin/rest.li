/*
   Copyright (c) 2021 LinkedIn Corp.

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
import com.linkedin.util.FastByteArrayOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;


/**
 * An abstract class housing common compression/decompression operations
 */
abstract class AbstractCompressor implements Compressor
{
  @Override
  public byte[] inflate(InputStream data) throws CompressionException
  {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    InputStream inflaterStream = null;

    try
    {
      inflaterStream = createInflaterInputStream(data);
      IOUtils.copy(inflaterStream, out);
    }
    catch (IOException e)
    {
      throw new CompressionException(CompressionConstants.DECODING_ERROR + getContentEncodingName(), e);
    }
    finally
    {
      IOUtils.closeQuietly(inflaterStream);
    }

    return out.toByteArray();
  }

  @Override
  public ByteString inflate(ByteString data) throws CompressionException
  {
    // Use FastByteArrayOutputStream to avoid array copies when merging arrays.
    FastByteArrayOutputStream out = new FastByteArrayOutputStream();
    InputStream inflaterStream = null;

    try
    {
      inflaterStream = createInflaterInputStream(data.asInputStream());
      IOUtils.copy(inflaterStream, out);
    }
    catch (IOException e)
    {
      throw new CompressionException(CompressionConstants.DECODING_ERROR + getContentEncodingName(), e);
    }
    finally
    {
      IOUtils.closeQuietly(inflaterStream);
    }

    // Create an unsafe ByteString directly from the stream to save on memcopies.
    return out.toUnsafeByteString();
  }

  @Override
  public byte[] deflate(InputStream data) throws CompressionException
  {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    OutputStream deflaterStream = null;

    try
    {
      deflaterStream = createDeflaterOutputStream(out);
      IOUtils.copy(data, deflaterStream);
    }
    catch (IOException e)
    {
      throw new CompressionException(CompressionConstants.DECODING_ERROR + getContentEncodingName(), e);
    }
    finally
    {
      IOUtils.closeQuietly(deflaterStream);
    }

    return out.toByteArray();
  }

  @Override
  public ByteString deflate(ByteString data) throws CompressionException
  {
    // Use FastByteArrayOutputStream to avoid array copies when merging arrays.
    FastByteArrayOutputStream out = new FastByteArrayOutputStream();
    OutputStream deflaterStream = null;

    try
    {
      deflaterStream = createDeflaterOutputStream(out);
      // Write the ByteString directly to the stream to avoid buffer copies.
      data.write(deflaterStream);
    }
    catch (IOException e)
    {
      throw new CompressionException(CompressionConstants.DECODING_ERROR + getContentEncodingName(), e);
    }
    finally
    {
      IOUtils.closeQuietly(deflaterStream);
    }

    // Create an unsafe ByteString directly from the stream to save on memcopies.
    return out.toUnsafeByteString();
  }

  /**
   * Create and retuen a {@link InputStream} that decompresses bytes read from the compressed stream.
   *
   * @param compressedDataStream The compressed input stream.
   * @return The decompressed input stream
   * @throws IOException If any exception occurred during stream creation.
   */
  protected abstract InputStream createInflaterInputStream(InputStream compressedDataStream) throws IOException;

  /**
   * Create and retuen a {@link OutputStream} that compresses bytes read from the decompressed stream.
   *
   * @param decompressedDataStream The decompressed ouput stream.
   * @return The compressed output stream
   * @throws IOException If any exception occurred during stream creation.
   */
  protected abstract OutputStream createDeflaterOutputStream(OutputStream decompressedDataStream) throws IOException;
}
