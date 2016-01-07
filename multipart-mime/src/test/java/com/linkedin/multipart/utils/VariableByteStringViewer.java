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

package com.linkedin.multipart.utils;


import com.linkedin.data.ByteString;
import com.linkedin.util.ArgumentUtil;


/**
 * Unit test equivalent of {@link VariableByteStringWriter}.
 *
 * @author Karim Vidhani
 */
public final class VariableByteStringViewer
{
  private final ByteString _content;
  private final int _chunkSize;
  private int _offset;

  public VariableByteStringViewer(final ByteString content, final int chunkSize)
  {
    ArgumentUtil.notNull(content, "content");
    _content = content;
    _chunkSize = chunkSize;
    _offset = 0;
  }

  public ByteString onWritePossible()
  {
    if (_offset == _content.length())
    {
      return ByteString.empty();
    }
    int bytesToWrite = Math.min(_chunkSize, _content.length() - _offset);
    ByteString slice = _content.slice(_offset, bytesToWrite);
    _offset += bytesToWrite;
    return slice;
  }
}