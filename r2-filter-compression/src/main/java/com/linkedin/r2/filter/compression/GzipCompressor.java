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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


/**
 * Wrapper class for gzip compression
 * */
public class GzipCompressor extends AbstractCompressor
{
  private static final String HTTP_NAME = "gzip";

  @Override
  protected InputStream createInflaterInputStream(InputStream compressedDataStream) throws IOException
  {
    return new GZIPInputStream(compressedDataStream);
  }

  @Override
  protected OutputStream createDeflaterOutputStream(OutputStream decompressedDataStream) throws IOException
  {
    return new GZIPOutputStream(decompressedDataStream);
  }

  @Override
  public String getContentEncodingName()
  {
    return HTTP_NAME;
  }
}
