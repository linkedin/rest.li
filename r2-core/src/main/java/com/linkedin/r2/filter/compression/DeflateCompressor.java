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
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.apache.commons.io.IOUtils;

/**
 * Wrapper class for zlib compression.
 * */
public class DeflateCompressor implements Compressor
{
  private final static String HTTP_NAME = "deflate";

  @Override
  public String getContentEncodingName()
  {
    return HTTP_NAME;
  }

  @Override
  public byte[] inflate(InputStream data) throws CompressionException
  {
    byte[] input;
    try
    {
      input = IOUtils.toByteArray(data);
    }
    catch (IOException e)
    {
      throw new CompressionException(CompressionConstants.DECODING_ERROR + CompressionConstants.BAD_STREAM, e);
    }

    Inflater zlib = new Inflater();
    zlib.setInput(input);

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    byte[] temp = new byte[CompressionConstants.BUFFER_SIZE];

    int bytesRead;
    while(!zlib.finished())
    {
      try
      {
        bytesRead = zlib.inflate(temp);
      }
      catch (DataFormatException e)
      {
        throw new CompressionException(CompressionConstants.DECODING_ERROR + getContentEncodingName(), e);
      }
      if (bytesRead == 0)
      {
        if (!zlib.needsInput())
        {
          throw new CompressionException(CompressionConstants.DECODING_ERROR + getContentEncodingName());
        }
        else
        {
          break;
        }
      }

      if (bytesRead > 0)
      {
        output.write(temp, 0, bytesRead);
      }
    }

    zlib.end();
    return output.toByteArray();
  }


  @Override
  public byte[] deflate(InputStream data) throws CompressionException
  {
    byte[] input;
    try
    {
      input = IOUtils.toByteArray(data);
    }
    catch (IOException e)
    {
      throw new CompressionException(CompressionConstants.DECODING_ERROR + CompressionConstants.BAD_STREAM, e);
    }

    Deflater zlib = new Deflater();
    zlib.setInput(input);
    zlib.finish();

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    byte[] temp = new byte[CompressionConstants.BUFFER_SIZE];

    int bytesRead;
    while(!zlib.finished())
    {
      bytesRead = zlib.deflate(temp);

      if (bytesRead == 0)
      {
        if (!zlib.needsInput())
        {
          throw new CompressionException(CompressionConstants.DECODING_ERROR + getContentEncodingName());
        }
        else
        {
          break;
        }
      }
      output.write(temp, 0, bytesRead);
    }
    zlib.end();

    return output.toByteArray();
  }
}
