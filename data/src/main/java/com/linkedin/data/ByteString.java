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


import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.codec.PsonDataCodec;
import com.linkedin.util.ArgumentUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An immutable sequence of bytes.
 *
 * Extra effort is taken to avoid extra copying during streaming when there is a need to assemble multiple ByteStrings
 * into on ByteString (e.g. receiving request/responses via chunked transfer encoding).
 * When constructing the new larger ByteString with {@link Builder()}, we don't actually do the copy but simply keep
 * reference to backing data of the smaller ByteStrings. As a result, for some important use cases,
 * e.g. asInputStream(), the extra copy can be avoided.
 * However, for some other use cases such as asString(), we still need to do the copy due to the fact that a single
 * byte array is required to construct those values.
 *
 * @author Chris Pettitt
 * @author Zhenkai Zhu
 * @version $Revision$
 */
public final class ByteString
{
  private static final PsonDataCodec PSON_DATA_CODEC = new PsonDataCodec();
  private static final JacksonDataCodec JACKSON_DATA_CODEC = new JacksonDataCodec();
  private static final ByteString EMPTY = new ByteString(new byte[0]);

  // backing data structure
  private final ByteArrayVector _byteArrays;

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
   * Returns a new {@link ByteString} that wraps the bytes generated by serializing the dataMap into a json.
   * @param dataMap dataMap that will be used to serialized into json as bytes
   * @return a {@link ByteString} that wraps the generated bytes
   * @throws IOException if fail to serialize {@code dataMap} to json.
   */
  public static ByteString copyFromDataMapAsJson(DataMap dataMap) throws IOException
  {
    return new ByteString(JACKSON_DATA_CODEC.mapToBytes(dataMap));
  }

  /**
   * Returns a new {@link ByteString} that wraps the bytes generated by serializing the dataMap into a pson.
   * @param dataMap dataMap that will be used to serialized into pson as bytes
   * @return a {@link ByteString} that wraps the generated bytes
   * @throws IOException if fail to serialize {@code dataMap} to pson.
   */
  public static ByteString copyFromDataMapAsPson(DataMap dataMap) throws IOException
  {
    return new ByteString(PSON_DATA_CODEC.mapToBytes(dataMap));
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

  /**
   * Returns a new {@link ByteString} with bytes read from an {@link InputStream} with unknown size.
   *
   * @param inputStream that will provide the bytes.
   * @return a ByteString that contains the read bytes.
   */
  public static ByteString read(InputStream inputStream) throws IOException
  {
    NoCopyByteArrayOutputStream bos = new NoCopyByteArrayOutputStream();
    byte[] buf = new byte[4096];
    int bytesRead;
    while((bytesRead = inputStream.read(buf, 0, buf.length)) != -1)
    {
      bos.write(buf, 0, bytesRead);
    }

    return new ByteString(bos.getBytes(), 0, bos.getBytesCount());
  }

  private ByteString(byte[] bytes)
  {
    this(ArgumentUtil.ensureNotNull(bytes, "bytes"), 0, bytes.length);
  }

  /**
   * This is internally used to create slice or copySlice of ByteString.
   */
  private ByteString(byte[] bytes, int offset, int length)
  {
    ArgumentUtil.notNull(bytes, "bytes");
    ByteArray[] byteArrays = new ByteArray[1];
    byteArrays[0] = new ByteArray(bytes, offset, length);
    _byteArrays = new ByteArrayVector(byteArrays);
  }

  /**
   * This is internally used to create a new ByteString with existing backing byte arrays
   *
   * @param byteArrays ByteArrayVector constructed with existing backing byte arrays
   */
  private ByteString(ByteArrayVector byteArrays)
  {
    ArgumentUtil.notNull(byteArrays, "byteArrays");
    _byteArrays = byteArrays;
  }

  /**
   * Returns the number of bytes in this {@link ByteString}.
   *
   * @return the number of bytes in this {@link ByteString}
   */
  public int length()
  {
    return _byteArrays.getBytesNum();
  }

  /**
   * Checks whether this {@link ByteString} is empty or not.
   * @return true for an empty {@link ByteString}, false otherwise
   */
  public boolean isEmpty()
  {
    return _byteArrays.getBytesNum() == 0;
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
    byte[] result = new byte[_byteArrays.getBytesNum()];

    copyBytes(result, 0);

    return result;
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
    int position = offset;
    for (int i = 0; i < _byteArrays.getArraySize(); i ++)
    {
      ByteArray byteArray = _byteArrays.get(i);
      System.arraycopy(byteArray.getArray(), byteArray.getOffset(), dest, position, byteArray.getLength());
      position += byteArray.getLength();
    }
  }

  /**
   * Returns a read only {@link ByteBuffer} view of this {@link ByteString}. This method makes no copy.
   *
   * @return read only {@link ByteBuffer} view of this {@link ByteString}.
   */
  public ByteBuffer asByteBuffer()
  {
    // we cannot supply an array of byte array to ByteBuffer, so we have to copy to a new larger continuous byte array
    // if needed
    ByteArray byteArray = assembleIfNeeded();
    return ByteBuffer.wrap(byteArray.getArray(), byteArray.getOffset(), byteArray.getLength()).asReadOnlyBuffer();
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
    // we cannot supply an array of byte array to String, so we have to copy to a new larger continuous byte array
    // if needed
    ByteArray byteArray = assembleIfNeeded();
    return new String(byteArray.getArray(), byteArray.getOffset(), byteArray.getLength(), charset);
  }

  /**
   * Return an Avro representation of the bytes in this {@link ByteString}.
   *
   * @return the String representation of this {@link ByteString}
   */
  public String asAvroString()
  {
    // we cannot supply an array of byte array to String, so we have to copy to a new larger continuous byte array
    // if needed
    ByteArray byteArray = assembleIfNeeded();
    return Data.bytesToString(byteArray.getArray(), byteArray.getOffset(), byteArray.getLength());
  }

  /**
   * Return an {@link InputStream} view of the bytes in this {@link ByteString}.
   *
   * @return an {@link InputStream} view of the bytes in this {@link ByteString}
   */
  public InputStream asInputStream()
  {
    return new ByteArrayVectorInputStream(_byteArrays);
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
    for (int i = 0; i < _byteArrays.getArraySize(); i++)
    {
      ByteArray byteArray = _byteArrays.get(i);
      out.write(byteArray.getArray(), byteArray.getOffset(), byteArray.getLength());
    }
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
    ArgumentUtil.checkBounds(_byteArrays.getBytesNum(), offset, length);
    return new ByteString(_byteArrays.slice(offset, length));
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
    ArgumentUtil.checkBounds(_byteArrays.getBytesNum(), offset, length);
    return new ByteString(slice(offset, length).copyBytes());
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

    int length = length();

    if (length != that.length())
    {
      return false;
    }

    // compare bytes one by one to determine whether the contents are equivalent
    int arrayIndex = 0;
    int arrayOffset = 0;
    int thatArrayIndex = 0;
    int thatArrayOffset = 0;
    int pos = 0;

    while(pos < length)
    {
      if (_byteArrays.get(arrayIndex).get(arrayOffset) != that._byteArrays.get(thatArrayIndex).get(thatArrayOffset))
      {
        return false;
      }

      arrayOffset++;
      thatArrayOffset++;

      if (arrayOffset == _byteArrays.get(arrayIndex).getLength())
      {
        arrayIndex++;
        arrayOffset = 0;
      }

      if (thatArrayOffset == that._byteArrays.get(thatArrayIndex).getLength())
      {
        thatArrayIndex++;
        thatArrayOffset = 0;
      }
      pos++;
    }

    return true;
  }

  @Override
  public int hashCode()
  {
    int result = 1;
    for (int index = 0; index < _byteArrays.getArraySize(); index++)
    {
      for (int offset = 0; offset < _byteArrays.get(index).getLength(); offset++)
      {
        result = result * 31 + _byteArrays.get(index).get(offset);
      }
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
    if (length() > 0)
    {
      sb.append(",bytes=");
      for (int i = 0; i < Math.min(length(), NUM_BYTES); i++)
      {
        sb.append(String.format("%02x", (int) _byteArrays.getByte(i) & 0xff));
      }
      if (length() > NUM_BYTES * 2)
      {
        sb.append("...");
      }
      for (int i =  Math.max(NUM_BYTES, length() - NUM_BYTES); i < length(); i++)
      {
        sb.append(String.format("%02x", (int)_byteArrays.getByte(i) & 0xff));
      }
    }
    sb.append(")");
    return sb.toString();
  }

  /**
   * A builder to assemble multiple ByteStrings into one ByteString without copying the backing byte arrays.
   *
   * This class is not thread safe
   */
  public static class Builder
  {
    private final List<ByteString> _chunks;

    public Builder()
    {
      _chunks = new ArrayList<ByteString>();
    }

    public Builder append(ByteString dataChunk)
    {
      ArgumentUtil.notNull(dataChunk, "dataChunk");
      if (!EMPTY.equals(dataChunk))
      {
        _chunks.add(dataChunk);
      }
      return this;
    }

    public ByteString build()
    {
      if (_chunks.isEmpty())
      {
        return empty();
      }
      else if (_chunks.size() == 1) // return the ByteString directly if there is only one to be assembled
      {
        return _chunks.get(0);
      }
      else
      {
        // each ByteString could have multiple backing arrays, so we first count the number of backing arrays
        int totalByteArraySize = 0;
        for (ByteString chunk: _chunks)
        {
          totalByteArraySize += chunk._byteArrays.getArraySize();
        }

        ByteArray[] byteArrays = new ByteArray[totalByteArraySize];

        int index = 0;
        for (ByteString chunk: _chunks)
        {
          for (int i = 0; i < chunk._byteArrays.getArraySize(); i++)
          {
            byteArrays[index] = chunk._byteArrays.get(i);
            index++;
          }
        }

        return new ByteString(new ByteArrayVector(byteArrays));
      }
    }
  }

  private ByteArray assembleIfNeeded()
  {
    if (_byteArrays.getArraySize() == 1)
    {
      return _byteArrays.get(0);
    }
    else
    {
      return new ByteArray(copyBytes(), 0, _byteArrays.getBytesNum());
    }
  }

  /**
   * This class is intended for internal use only. The output stream should not be passed around after
   * the construction; otherwise the internal representation of the ByteString would change, voiding the
   * immutability guarantee.
   */
  private static class NoCopyByteArrayOutputStream extends ByteArrayOutputStream
  {
    byte[] getBytes()
    {
      return super.buf;
    }

    int getBytesCount()
    {
      return super.count;
    }
  }


  /**
   * This is a convenient class to hold a byte array and keep the offset & effective length to refer to
   * a visible portion of the original byte array
   */
  private static class ByteArray
  {
    private final byte[] _bytes;
    private final int _offset;
    private final int _length;

    /**
     *
     * @param bytes the backing byte array
     * @param offset the offset in the byte array for the visible range
     * @param length the length of the visible range in the byte array
     */
    ByteArray(byte[] bytes, int offset, int length)
    {
      ArgumentUtil.notNull(bytes, "bytes");
      ArgumentUtil.checkBounds(bytes.length, offset, length);
      _bytes = bytes;
      _offset = offset;
      _length = length;
    }

    /**
     * Returns the backing array as quite a few APIs require raw byte array
     * @return the backing byte array
     */
    byte[] getArray()
    {
      return _bytes;
    }

    /**
     * @return the offset of the visible portion
     */
    int getOffset()
    {
      return _offset;
    }

    /**
     *
     * @return the length of the visible portion
     */
    int getLength()
    {
      return _length;
    }

    /**
     * @param i the index of the byte (relative to _offset)
     * @return the ith byte in the visible portion of byte array
     */
    byte get(int i)
    {
      if (i >= _length || i < 0)
      {
        throw new IndexOutOfBoundsException("i: " + i);
      }
      return _bytes[_offset + i];
    }

    /**
     * Creat a slice of this ByteArray using the same backing array
     * @param offset the start point of the slice
     * @param length the length of the slice
     * @return a slice of this ByteArray starting at offset with specified length
     */
    ByteArray slice(int offset, int length)
    {
      ArgumentUtil.checkBounds(_length, offset, length);
      return new ByteArray(_bytes, _offset + offset, length);
    }

    /**
     * Create a slice of this ByteArray using the same backing array
     * @param offset the start point of the slice
     * @return a slice of this ByteArray starting at offset til the end
     */
    ByteArray slice(int offset)
    {
      if (offset > _length || offset < 0)
      {
        throw new IndexOutOfBoundsException("offset: " + offset);
      }

      return new ByteArray(_bytes, _offset + offset, _length - offset);
    }
  }

  /**
   * This is a convenient class to hold an array of ByteArray.
   */
  private static class ByteArrayVector
  {
    // the backing array of ByteArray
    private final ByteArray[] _byteArrays;
    /**
     * This is an array to record the accumulated bytes at each position. E.g. _accumulatedLens[i]
     * is the number of bytes held in ByteArrays from 0 to i-1. It's like an index so that we can
     * quickly locate which ByteArray contains the byte in a desired position.
     */
    private final int[] _accumulatedLens;
    // the total number of bytes held
    private final int _totalLength;

    ByteArrayVector(ByteArray[] byteArrays)
    {
      _byteArrays = byteArrays;
      int arrayNum = _byteArrays.length;
      _accumulatedLens = new int[arrayNum];
      int accuLen = 0;
      for (int i = 0; i < arrayNum; i++)
      {
        _accumulatedLens[i] = accuLen;
        accuLen += _byteArrays[i].getLength();
      }
      _totalLength = accuLen;
    }

    /**
     * Get the ith ByteArray
     * @param i the index of the desired ByteArray
     * @return the ByteArray
     */
    ByteArray get(int i)
    {
      if (i >= _byteArrays.length || i < 0)
      {
        throw new IndexOutOfBoundsException("i: " + i);
      }
      return _byteArrays[i];
    }

    /**
     * Get the desired byte from the ByteArrayVector.
     * This is not the efficient way to access bytes sequentially.
     *
     * @param offset the offset in terms of bytes num of the desired byte
     * @return the byte
     */
    byte getByte(int offset)
    {
      if (offset >= _totalLength || offset < 0)
      {
        throw new IndexOutOfBoundsException("offset: " + offset);
      }
      // get the index of the ByteArray that contains this byte
      int index = locate(offset, 0, _byteArrays.length - 1);
      return _byteArrays[index].get(offset - _accumulatedLens[index]);
    }

    /**
     * Create a slice of this ByteArrayVector
     * @param offset the offset in terms of the bytes num
     * @param length the length of the slice
     * @return a ByteArrayVector that is a slice of the current ByteArrayVector
     */
    ByteArrayVector slice(int offset, int length)
    {
      ArgumentUtil.checkBounds(_totalLength, offset, length);

      int startIndex = locate(offset, 0, _byteArrays.length - 1);
      int endIndex = locate(offset + length, startIndex, _byteArrays.length - 1);

      ByteArray[] byteArrays;
      if (startIndex == endIndex)
      {
        byteArrays = new ByteArray[1];
        byteArrays[0] = _byteArrays[startIndex].slice(offset - _accumulatedLens[startIndex], length);
      }
      else
      {
        int arrayLen = endIndex - startIndex + 1;
        byteArrays = new ByteArray[arrayLen];
        byteArrays[0] = _byteArrays[startIndex].slice(offset - _accumulatedLens[startIndex]);
        byteArrays[arrayLen - 1] = _byteArrays[endIndex].slice(0, offset + length - _accumulatedLens[endIndex]);

        for (int i = 1; i < arrayLen - 1; i ++)
        {
          byteArrays[i] = _byteArrays[startIndex + i];
        }
      }
      return new ByteArrayVector(byteArrays);
    }

    int getBytesNum()
    {
      return _totalLength;
    }

    int getArraySize()
    {
      return _byteArrays.length;
    }

    /**
     * Locate the ByteArray where the ith byte is located
     * @param i the ith byte
     * @param startIndex the index of the first ByteArray to look at
     * @param endIndex the index of the last ByteArray to look at
     * @return the index of the target ByteArray
     */
    private int locate(int i, int startIndex, int endIndex)
    {
      if (startIndex > endIndex)
      {
        throw new IllegalArgumentException("location " + i + " is out of bound");
      }

      int mid = (startIndex + endIndex) / 2;

      if (_accumulatedLens[mid] > i)
      {
        return locate(i, startIndex, mid - 1);
      }
      else if (_accumulatedLens[mid] == i)
      {
        return mid;
      }
      else if (mid == endIndex || _accumulatedLens[mid + 1] > i)
      {
        return mid;
      }
      else
      {
        return locate(i, mid + 1, endIndex);
      }
    }

  }

  /**
   * An inputstream backed by a ByteArrayVector.
   * Unlike ByteArrayInputStream, this inputstream does not synchronize on methods.
   */
  private static class ByteArrayVectorInputStream extends InputStream
  {
    private final ByteArrayVector _byteArrays;
    private int _pos;
    private int _arrayIndex;
    private int _arrayOffset;
    private int _mark;
    private int _count;

    ByteArrayVectorInputStream(ByteArrayVector byteArrays)
    {
      _byteArrays = byteArrays;
      _count = _byteArrays.getBytesNum();
      _pos = 0;
      _mark = _pos;
      _arrayIndex = 0;
      _arrayOffset = 0;
    }

    @Override
    public int available()
    {
      return _count - _pos;
    }

    @Override
    public void mark(int readLimit)
    {
      // readLimit is ignored per Java class Lib. book,  p.220.
      _mark = _pos;
    }

    @Override
    public boolean markSupported()
    {
      return true;
    }

    @Override
    public int read()
    {
      if (_pos < _count)
      {
        _pos++;
        byte result= _byteArrays.get(_arrayIndex).get(_arrayOffset);

        _arrayOffset++;

        if (_arrayOffset == _byteArrays.get(_arrayIndex).getLength())
        {
          _arrayIndex++;
          _arrayOffset = 0;
        }

        return ((int) result) & 0xFF;
      }

      return -1;
    }

    @Override
    public int read(byte[] buffer, int offset, int length)
    {
      if (_pos >= _count)
      {
        return -1;
      }

      int numBytes = Math.min(_count - _pos, length);

      int copiedBytesNum = 0;
      while (copiedBytesNum < numBytes)
      {
        ByteArray byteArray = _byteArrays.get(_arrayIndex);
        int len = Math.min(byteArray.getLength() - _arrayOffset, numBytes - copiedBytesNum);
        System.arraycopy(byteArray.getArray(), byteArray.getOffset() + _arrayOffset, buffer, offset + copiedBytesNum, len);
        copiedBytesNum += len;

        if (len == byteArray.getLength() - _arrayOffset)
        {
          _arrayIndex++;
          _arrayOffset = 0;
        }
        else
        {
          _arrayOffset += len;
        }
      }
      _pos += numBytes;
      return numBytes;
    }

    @Override
    public void reset()
    {
      _pos = _mark;
      _arrayIndex = _byteArrays.locate(_pos, 0, _byteArrays.getArraySize());
      _arrayOffset = _pos - _byteArrays._accumulatedLens[_arrayIndex];
    }

    @Override
    public long skip(long num)
    {
      long numBytes = Math.min((long)(_count - _pos), num < 0 ? 0L : num);
      _pos += numBytes;
      _arrayIndex = _byteArrays.locate(_pos, 0, _byteArrays.getArraySize());
      _arrayOffset = _pos - _byteArrays._accumulatedLens[_arrayIndex];
      return numBytes;
    }
  }

}
