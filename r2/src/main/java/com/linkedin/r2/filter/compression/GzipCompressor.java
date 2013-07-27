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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;

/**
 * Wrapper class for gzip compression
 * */
public class GzipCompressor implements Compressor
{
  private static final String HTTP_NAME = "gzip";

  //Consider changing input param as streams rather than fixed bytes?
  @Override
  public byte[] inflate(InputStream data) throws CompressionException
  {
    ByteArrayOutputStream out;
    GZIPInputStream gzip = null;

    try
    {
      out = new ByteArrayOutputStream();
      gzip = new GZIPInputStream(data);

      IOUtils.copy(gzip, out);
    }
    catch (IOException e)
    {
      throw new CompressionException(CompressionConstants.DECODING_ERROR + getContentEncodingName(), e);
    }
    finally
    {
      if (gzip != null)
      {
        IOUtils.closeQuietly(gzip);
      }
    }

    return out.toByteArray();
  }

  @Override
  public byte[] deflate(InputStream data) throws CompressionException
  {
    ByteArrayOutputStream out;
    GZIPOutputStream gzip = null;

    try
    {
      out = new ByteArrayOutputStream();
      gzip = new GZIPOutputStream(out);

     IOUtils.copy(data, gzip);
    }
    catch (IOException e)
    {
      throw new CompressionException(CompressionConstants.DECODING_ERROR + getContentEncodingName(), e);
    }
    finally
    {
      if (gzip != null)
      {
        IOUtils.closeQuietly(gzip);
      }
    }

    return out.toByteArray();
  }

  @Override
  public String getContentEncodingName()
  {
    return HTTP_NAME;
  }
}
