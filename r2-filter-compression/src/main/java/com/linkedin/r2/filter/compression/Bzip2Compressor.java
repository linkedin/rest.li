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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;


/**
 * Wrapper class for bzip2 compression
 * */
public class Bzip2Compressor extends AbstractCompressor {
  private static final String HTTP_NAME = "bzip2";

  @Override
  public String getContentEncodingName()
  {
    return HTTP_NAME;
  }

  @Override
  protected InputStream createInflaterInputStream(InputStream compressedDataStream) throws IOException
  {
    return new BZip2CompressorInputStream(compressedDataStream);
  }

  @Override
  protected OutputStream createDeflaterOutputStream(OutputStream decompressedDataStream) throws IOException
  {
    return new BZip2CompressorOutputStream(decompressedDataStream);
  }
}
