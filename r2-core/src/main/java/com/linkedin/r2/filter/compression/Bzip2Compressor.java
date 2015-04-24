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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.IOUtils;

/**
 * Wrapper class for bzip2 compression
 * */
public class Bzip2Compressor implements Compressor {
  private static final String HTTP_NAME = "bzip2";

  @Override
  public String getContentEncodingName()
  {
    return HTTP_NAME;
  }

  @Override
  public byte[] inflate(InputStream data) throws CompressionException
  {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    BZip2CompressorInputStream bzip2 = null;

    try
    {
      bzip2 = new BZip2CompressorInputStream(data);
      IOUtils.copy(bzip2, out);
    }
    catch (IOException e)
    {
      throw new CompressionException(CompressionConstants.DECODING_ERROR + getContentEncodingName(), e);
    }
    finally
    {
      if (bzip2 != null)
      {
        IOUtils.closeQuietly(bzip2);
      }
    }

    return out.toByteArray();
  }

  @Override
  public byte[] deflate(InputStream data) throws CompressionException
  {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    BZip2CompressorOutputStream compressor = null;

    try
    {
      out = new ByteArrayOutputStream();
      compressor = new BZip2CompressorOutputStream(out);

      IOUtils.copy(data, compressor);
      compressor.finish();
    }
    catch (IOException e)
    {
      throw new CompressionException(CompressionConstants.DECODING_ERROR + getContentEncodingName(), e);
    }
    finally
    {
      if (compressor != null)
      {
        IOUtils.closeQuietly(compressor);
      }
    }

    return out.toByteArray();
  }
}
