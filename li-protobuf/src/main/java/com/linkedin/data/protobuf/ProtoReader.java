/*
   Copyright (c) 2019 LinkedIn Corp.

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

// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
// https://developers.google.com/protocol-buffers/
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//     * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.linkedin.data.protobuf;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class for reading Protocol Buffers encoded binary data.
 */
public abstract class ProtoReader
{
  static final int DEFAULT_BYTE_BUFFER_SIZE = 4096;
  static final int DEFAULT_TEXT_BUFFER_SIZE = 1024;
  static final int DEFAULT_SIZE_LIMIT = Integer.MAX_VALUE;

  /**
   * Create a new ProtoReader wrapping the given InputStream.
   */
  public static ProtoReader newInstance(final InputStream input)
  {
    return new InputStreamReader(input, DEFAULT_BYTE_BUFFER_SIZE);
  }

  /**
   * Create a new ProtoReader wrapping the given byte array.
   */
  public static ProtoReader newInstance(final byte[] buf)
  {
    return newInstance(buf, 0, buf.length);
  }

  /**
   * Create a new ProtoReader wrapping the given byte array slice.
   */
  public static ProtoReader newInstance(final byte[] buf, final int off, final int len)
  {
    return new ByteArrayReader(buf, off, len);
  }

  protected final TextBuffer _textBuffer;

  /**
   * Enable construction via inheritance.
   */
  protected ProtoReader()
  {
    _textBuffer = new TextBuffer(DEFAULT_TEXT_BUFFER_SIZE);
  }

  /**
   * Read a {@code string} field value from the stream. If the stream contains malformed UTF-8,
   * replace the offending bytes with the standard UTF-8 replacement character.
   */
  public abstract String readString() throws IOException;

  /**
   * Read an ASCII only {@code string} field value from the stream. If the stream contains non ASCII characters,
   * then the resultant string may be malformed.
   */
  public String readASCIIString() throws IOException
  {
    // For backward compatibility, invoke readString() by default.
    return readString();
  }

  /**
   * Read a {@code bytes} field value from the stream.
   */
  public abstract byte[] readByteArray() throws IOException;

  /**
   * Read a raw Varint from the stream. If larger than 32 bits, discard the upper bits.
   */
  public abstract int readInt32() throws IOException;

  /**
   * Read a raw Varint from the stream.
   */
  public abstract long readInt64() throws IOException;

  /**
   * Read a fixed 32-bit int from the stream.
   */
  public int readFixedInt32() throws IOException
  {
    // For backward compatibility at build time, implement but throw an UnsupportedOperationException.
    throw new UnsupportedOperationException();
  }

  /**
   * Read a fixed 64-bit int from the stream.
   */
  public long readFixedInt64() throws IOException
  {
    // For backward compatibility at build time, implement but throw an UnsupportedOperationException.
    throw new UnsupportedOperationException();
  }

  /**
   * Read one byte from the _input.
   *
   * @throws EOFException The end of the stream or the current _limit was reached.
   */
  public abstract byte readRawByte() throws IOException;
}
