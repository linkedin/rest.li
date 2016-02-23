package com.linkedin.r2.filter.compression;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.iq80.snappy.Snappy;
import org.iq80.snappy.SnappyInputStream;
import org.iq80.snappy.SnappyOutputStream;


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
    try (SnappyInputStream snappy = new SnappyInputStream(data))
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
    try (SnappyOutputStream snappy = new SnappyOutputStream(out))
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
