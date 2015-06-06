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
  private final int _offset;
  private final int _length;

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
   * Returns a new {@link ByteString} that wraps a copy of the desired region of supplied bytes. Changes to the supplied
   * bytes will not be reflected in the returned {@link ByteString}.
   *
   * @param bytes the bytes to copy
   * @param offset the starting point of region to be copied
   * @param length the length of the region to be copied
   * @return a {@link ByteString} that wraps a copy of the desired region of supplied bytes
   * @throws NullPointerException if {@code bytes} is {@code null}.
   * @throws IndexOutOfBoundsException if offset or length is negative, or offset + length is larger than the length
   * of the bytes
   */
  public static ByteString copy(byte[] bytes, int offset, int length)
  {
    ArgumentUtil.notNull(bytes, "bytes");
    ArgumentUtil.checkBounds(bytes.length, offset, length);
    return length == 0 ? empty() : new ByteString(Arrays.copyOfRange(bytes, offset, offset + length));
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
    ArgumentUtil.notNull(bytes, "bytes");
    _bytes = bytes;
    _offset = 0;
    _length = bytes.length;
  }

  /**
   * This is internally used to create slice or copySlice of ByteString.
   */
  private ByteString(byte[] bytes, int offset, int length)
  {
    ArgumentUtil.notNull(bytes, "bytes");
    _bytes = bytes;
    _offset = offset;
    _length = length;
  }

  /**
   * Returns the number of bytes in this {@link ByteString}.
   *
   * @return the number of bytes in this {@link ByteString}
   */
  public int length()
  {
    return _length;
  }

  /**
   * Checks whether this {@link ByteString} is empty or not.
   * @return true for an empty {@link ByteString}, false otherwise
   */
  public boolean isEmpty()
  {
    return _length == 0;
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
    return Arrays.copyOfRange(_bytes, _offset, _offset + _length);
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
    System.arraycopy(_bytes, _offset, dest, offset, _length);
  }

  /**
   * Returns a read only {@link ByteBuffer} view of this {@link ByteString}. This method makes no copy.
   *
   * @return read only {@link ByteBuffer} view of this {@link ByteString}.
   */
  public ByteBuffer asByteBuffer()
  {
    return ByteBuffer.wrap(_bytes, _offset, _length).asReadOnlyBuffer();
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
    return new String(_bytes, _offset, _length, charset);
  }

  /**
   * Return an Avro representation of the bytes in this {@link ByteString}.
   *
   * @return the String representation of this {@link ByteString}
   */
  public String asAvroString()
  {
    return Data.bytesToString(_bytes, _offset, _length);
  }

  /**
   * Return an {@link InputStream} view of the bytes in this {@link ByteString}.
   *
   * @return an {@link InputStream} view of the bytes in this {@link ByteString}
   */
  public InputStream asInputStream()
  {
    return new ByteArrayInputStream(_bytes, _offset, _length);
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
    out.write(_bytes, _offset, _length);
  }

  /**
   * Returns a slice of ByteString.
   * This create a "view" of this ByteString, which holds the entire content of the original ByteString. If your code
   * only needs a small portion of a large ByteString and is not interested in the rest of that ByteString, it is better
   * to use {@link #copySlice} method.
   *
   * @param offset the starting point of the slice
   * @param length the length of the slice
   * @return a slice of ByteString backed by the same backing byte array
   * @throws IndexOutOfBoundsException if offset or length is negative, or offset + length is larger than the length
   * of this ByteString
   */
  public ByteString slice(int offset, int length)
  {
    ArgumentUtil.checkBounds(_length, offset, length);
    return new ByteString(_bytes, _offset + offset, length);
  }

  /**
   * Returns a slice of ByteString backed by a new byte array.
   * This copies the content from the desired portion of the original ByteString and does not hold reference to the
   * original ByteString.
   *
   * @param offset the starting point of the slice
   * @param length the length of the slice
   * @return a slice of ByteString backed by a new byte array
   * @throws IndexOutOfBoundsException if offset or length is negative, or offset + length is larger than the length
   * of this ByteString
   */
  public ByteString copySlice(int offset, int length)
  {
    ArgumentUtil.checkBounds(_length, offset, length);
    int from = _offset + offset;
    int to = from + length;
    byte[] content = Arrays.copyOfRange(_bytes, from, to);
    return new ByteString(content);
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

    if (_length == that._length)
    {
      for (int i = _offset, j = that._offset; i < _offset + _length; i++, j++)
      {
        if (_bytes[i] != that._bytes[j])
        {
          return false;
        }
      }
      return true;
    }

    return false;
  }

  @Override
  public int hashCode()
  {
    int result = 1;
    for (int i = _offset; i < _offset + _length; i++)
    {
      result = result * 31 + _bytes[i];
    }
    return result;
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
    if (_length > 0)
    {
      sb.append(",bytes=");
      for (int i = _offset; i < _offset + Math.min(_length, NUM_BYTES); i++)
      {
        sb.append(String.format("%02x", (int) _bytes[i] & 0xff));
      }
      if (_length > NUM_BYTES * 2)
      {
        sb.append("...");
      }
      for (int i = _offset + Math.max(NUM_BYTES, _length - NUM_BYTES); i < _offset + _length; i++)
      {
        sb.append(String.format("%02x", (int)_bytes[i] & 0xff));
      }
    }
    sb.append(")");
    return sb.toString();
  }
}
