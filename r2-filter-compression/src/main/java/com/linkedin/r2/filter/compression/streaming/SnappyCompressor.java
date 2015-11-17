/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.r2.filter.compression.streaming;

import com.linkedin.r2.message.stream.entitystream.EntityStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Executor;
import org.iq80.snappy.SnappyInputStream;
import org.iq80.snappy.SnappyOutputStream;


/**
 * @author Ang Xu
 */
public class SnappyCompressor extends AbstractCompressor
{
  private final Executor _executor;

  public SnappyCompressor(Executor executor)
  {
    _executor = executor;
  }

  @Override
  public String getContentEncodingName()
  {
    return StreamEncodingType.SNAPPY_FRAMED.getHttpName();
  }

  @Override
  protected StreamingInflater createInflater(EntityStream underlying)
  {
    return new StreamingInflater(underlying, _executor)
    {
      @Override
      protected InputStream createInputStream(InputStream in) throws IOException
      {
        return new SnappyInputStream(in);
      }
    };
  }

  @Override
  protected StreamingDeflater createDeflater(EntityStream underlying)
  {
    return new StreamingDeflater(underlying)
    {
      @Override
      protected OutputStream createOutputStream(OutputStream out) throws IOException
      {
        return new SnappyOutputStream(out);
      }
    };
  }
}
