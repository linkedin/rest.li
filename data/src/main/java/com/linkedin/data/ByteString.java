/*
   Copyright (c) 2012 LinkedIn Corp.

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

/* $Id$ */
package com.linkedin.data;


import com.linkedin.util.ArgumentUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * An immutable sequence of bytes.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public final class ByteString
{
  private static final ByteString EMPTY = new ByteString(new byte[0]);

  private final byte[] _bytes;

  /**
   * Returns an empty {@link ByteString}.
   *
   * @return an empty {@link ByteString}
   */
  public static ByteString empty()
  {
    return EMPTY;
  }

  /**
   * Returns a new {@link ByteString} that wraps a copy of the supplied bytes. Changes to the supplied bytes
   * will not be reflected in the returned {@link ByteString}.
   *
   * @param bytes the bytes to copy
   * @return a {@link ByteString} that wraps a copy of the supplied bytes
   * @throws NullPointerException if {@code bytes} is {@code null}.
   */
  public static ByteString copy(byte[] bytes)
  {
    ArgumentUtil.notNull(bytes, "bytes");
    return bytes.length == 0 ? empty() : new ByteString(Arrays.copyOf(bytes, bytes.length));
  }

  /**
   * Returns a new {@link ByteString} that wraps a copy of the bytes in the supplied {@link ByteBuffer}.
   * Changes to the supplied bytes will not be reflected in the returned {@link ByteString}.
   *
   * @param byteBuffer the {@link ByteBuffer} to copy bytes from.
   * @return a {@link ByteString} that wraps a copy of the bytes in the supplied {@link ByteBuffer}.
   * @throws NullPointerException if {@code byteBuffer} is {@code null}.
   */
  public static ByteString copy(ByteBuffer byteBuffer)
  {
    ArgumentUtil.notNull(byteBuffer, "byteBuffer");
    int size = byteBuffer.remaining();
    if (size == 0)
    {
      return empty();
    }
    byte[] bytes = new byte[size];
    byteBuffer.get(bytes);
    return new ByteString(bytes);
  }

  /**
   * Returns a new {@link ByteString} that wraps the bytes generated from the supplied string with the
   * given charset.
   *
   * @param str the string to copy
   * @param charsetName the name of the charset used to encode the bytes
   * @return a {@link ByteString} that wraps a copy of the supplied bytes
   */
  public static ByteString copyString(String str, String charsetName)
  {
    return copyString(str, Charset.forName(charsetName));
  }

  /**
   * Returns a new {@link ByteString} that wraps the bytes generated from the supplied string with the
   * given charset.
   *
   * @param str the string to copy
   * @param charset the charset used to encode the bytes
   * @return a {@link ByteString} that wraps a copy of the supplied bytes
   */
  public static ByteString copyString(String str, Charset charset)
  {
    ArgumentUtil.notNull(str, "str");
    return copy(str.getBytes(charset));
  }

  /**
   * Returns a new {@link ByteString} that wraps the bytes generated from the supplied string
   * using {@link Data#stringToBytes(String, boolean)}.
   *
   * @param string that will be used to generate the bytes.
   * @param validate indicates whether validation is enabled, validation is enabled if true.
   * @return a {@link ByteString} that wraps a copy of the supplied bytes, if validation fails return null.
   * @throws NullPointerException if {@code string} is {@code null}.
   */
  public static ByteString copyAvroString(String string, boolean validate)
  {
    ArgumentUtil.notNull(string, "string");
    if (string.length() == 0)
    {
      return empty();
    }
    byte[] bytes = Data.stringToBytes(string, validate);
    if (bytes == null)
    {
      return null;
    }
    else
    {
      return new ByteString(bytes);
    }
  }

  /**
   * Returns a new {@link ByteString} with bytes read from an {@link InputStream}.
   *
   * If size is zero, then this method will always return the {@link ByteString#empty()},
   * and no bytes will be read from the {@link InputStream}.
   * If size is less than zero, then {@link NegativeArraySizeException} will be thrown
   * when this method attempt to create an array of negative size.
   *
   * @param inputStream that will provide the bytes.
   * @param size provides the number of bytes to read.
   * @return a ByteString that contains the read bytes.
   * @throws IOException from InputStream if requested number of bytes
   *                     cannot be read.
   */
  public static ByteString read(InputStream inputStream, int size) throws IOException
  {
    if (size == 0)
    {
      return empty();
    }

    final byte[] buf = new byte[size];

    int bytesRead, bufIdx = 0;
    while (bufIdx < size &&
           (bytesRead = inputStream.read(buf, bufIdx, size - bufIdx)) != -1)
    {
      bufIdx += bytesRead;
    }

    if (bufIdx != size)
    {
      throw new IOException("Insufficient data in InputStream, requested size " + size + ", read " + bufIdx);
    }

    return new ByteString(buf);
  }

  private ByteString(byte[] bytes)
  {
    assert bytes != null;
    _bytes = bytes;
  }

  /**
   * Returns the number of bytes in this {@link ByteString}.
   *
   * @return the number of bytes in this {@link ByteString}
   */
  public int length()
  {
    return _bytes.length;
  }

  /**
   * Returns a copy of the bytes in this {@link ByteString}. Changes to the returned byte[] will not be
   * reflected in this {@link ByteString}.<p>
   *
   * Where possible prefer other methods for accessing the underlying bytes, such as
   * {@link #asByteBuffer()}, {@link #write(java.io.OutputStream)}, or {@link #asString(Charset)}.
   * The first two make no copy of the byte array, while the last minimizes the amount of copying
   * (constructing a String from a byte[] always involves copying).
   *
   * @return a copy of the bytes in this {@link ByteString}
   */
  public byte[] copyBytes()
  {
    return Arrays.copyOf(_bytes, _bytes.length);
  }

  /**
   * Copy the bytes in this {@link ByteString} to the provided byte[] starting at the specified offset.
   *
   * Where possible prefer other methods for accessing the underlying bytes, such as
   * {@link #asByteBuffer()}, {@link #write(java.io.OutputStream)}, or {@link #asString(Charset)}.
   * The first two make no copy of the byte array, while the last minimizes the amount of copying
   * (constructing a String from a byte[] always involves copying).
   *
   * @param dest is the destination to copy the bytes in this {@link ByteString} to.
   * @param offset is the starting offset in the destination to receive the copy.
   */
  public void copyBytes(byte[] dest, int offset)
  {
    System.arraycopy(_bytes, 0, dest, offset, _bytes.length);
  }

  /**
   * Returns a read only {@link ByteBuffer} view of this {@link ByteString}. This method makes no copy.
   *
   * @return read only {@link ByteBuffer} view of this {@link ByteString}.
   */
  public ByteBuffer asByteBuffer()
  {
    return ByteBuffer.wrap(_bytes).asReadOnlyBuffer();
  }

  /**
   * Return a String representation of the bytes in this {@link ByteString}, decoded using the supplied
   * charset.
   *
   * @param charsetName the name of the charset to use to decode the bytes
   * @return the String representation of this {@link ByteString}
   */
  public String asString(String charsetName)
  {
    return asString(Charset.forName(charsetName));
  }

  /**
   * Return a String representation of the bytes in this {@link ByteString}, decoded using the supplied
   * charset.
   *
   * @param charset the charset to use to decode the bytes
   * @return the String representation of this {@link ByteString}
   */
  public String asString(Charset charset)
  {
    return new String(_bytes, charset);
  }

  /**
   * Return an Avro representation of the bytes in this {@link ByteString}.
   *
   * @return the String representation of this {@link ByteString}
   */
  public String asAvroString()
  {
    return Data.bytesToString(_bytes);
  }

  /**
   * Return an {@link InputStream} view of the bytes in this {@link ByteString}.
   *
   * @return an {@link InputStream} view of the bytes in this {@link ByteString}
   */
  public InputStream asInputStream()
  {
    return new ByteArrayInputStream(_bytes);
  }

  /**
   * Writes this {@link ByteString} to a stream without copying the underlying byte[].
   *
   * @param out the stream to write the bytes to
   *
   * @throws IOException if an error occurs while writing to the stream
   */
  public void write(OutputStream out) throws IOException
  {
    out.write(_bytes);
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
    {
      return true;
    }

    if (o == null || getClass() != o.getClass())
    {
      return false;
    }

    ByteString that = (ByteString) o;
    return Arrays.equals(_bytes, that._bytes);
  }

  @Override
  public int hashCode()
  {
    return Arrays.hashCode(_bytes);
  }

  /**
   * Return a summary of the contents of this {@link ByteString}.  This summary is of reasonable size,
   * regardless of the length of this {@link ByteString}.
   *
   * @return a summary representation of this {@link ByteString}
   */
  @Override
  public String toString()
  {
    final int NUM_BYTES = 4;
    StringBuilder sb = new StringBuilder();
    sb.append("ByteString(length=");
    sb.append(length());
    if (_bytes.length > 0)
    {
      sb.append(",bytes=");
      for (int i = 0; i < Math.min(_bytes.length, NUM_BYTES); i++)
      {
        sb.append(String.format("%02x", (int) _bytes[i] & 0xff));
      }
      if (_bytes.length > NUM_BYTES * 2)
      {
        sb.append("...");
      }
      for (int i = Math.max(NUM_BYTES, _bytes.length - NUM_BYTES); i < _bytes.length; i++)
      {
        sb.append(String.format("%02x", (int)_bytes[i] & 0xff));
      }
    }
    sb.append(")");
    return sb.toString();
  }
}
