package com.linkedin.r2.filter.compression;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.iq80.snappy.SnappyFramedInputStream;
import org.iq80.snappy.SnappyFramedOutputStream;


/**
 * Compressor for "x-snappy-framed" Encoding.
 *
 * @author Ang Xu
 */
public class SnappyFramedCompressor implements Compressor {

  private static final String HTTP_NAME = "x-snappy-framed";

  @Override
  public String getContentEncodingName()
  {
    return HTTP_NAME;
  }

  @Override
  public byte[] inflate(InputStream data) throws CompressionException
  {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (SnappyFramedInputStream snappy = new SnappyFramedInputStream(data, true))
    {
      IOUtils.copy(snappy, out);
    }
    catch (IOException e)
    {
      throw new CompressionException(CompressionConstants.DECODING_ERROR + getContentEncodingName(), e);
    }
    return out.toByteArray();
  }

  @Override
  public byte[] deflate(InputStream data) throws CompressionException
  {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (SnappyFramedOutputStream snappy = new SnappyFramedOutputStream(out))
    {
      IOUtils.copy(data, snappy);
    }
    catch (IOException e)
    {
      throw new CompressionException(CompressionConstants.DECODING_ERROR + getContentEncodingName(), e);
    }
    return out.toByteArray();
  }

}
