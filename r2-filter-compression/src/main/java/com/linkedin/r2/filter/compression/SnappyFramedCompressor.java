package com.linkedin.r2.filter.compression;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.iq80.snappy.SnappyFramedInputStream;
import org.iq80.snappy.SnappyFramedOutputStream;


/**
 * Compressor for "x-snappy-framed" Encoding.
 *
 * @author Ang Xu
 */
public class SnappyFramedCompressor extends AbstractCompressor {

  private static final String HTTP_NAME = "x-snappy-framed";

  @Override
  public String getContentEncodingName()
  {
    return HTTP_NAME;
  }

  @Override
  protected InputStream createInflaterInputStream(InputStream compressedDataStream) throws IOException
  {
    return new SnappyFramedInputStream(compressedDataStream, true);
  }

  @Override
  protected OutputStream createDeflaterOutputStream(OutputStream decompressedDataStream) throws IOException
  {
    return new SnappyFramedOutputStream(decompressedDataStream);
  }
}
