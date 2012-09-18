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

package com.linkedin.data.codec;

import com.linkedin.data.ByteString;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;

public class BufferChain
{
  static final PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out));

  /**
   * Default buffer size.
   */
  public static final int DEFAULT_BUFFER_SIZE = 8192;

  /**
   * Default byte order is {@link java.nio.ByteOrder#nativeOrder()}.
   */
  public static final ByteOrder DEFAULT_ORDER = ByteOrder.nativeOrder();

  /**
   * Default string length.
   */
  public static final int DEFAULT_STRING_LENGTH = 128;

  private static final byte ZERO_BYTE = 0;

  private static final int SIZE_BYTE = 1;
  private static final int SIZE_SHORT = 2;
  private static final int SIZE_INT = 4;
  private static final int SIZE_LONG = 8;
  private static final int SIZE_FLOAT = 4;
  private static final int SIZE_DOUBLE = 8;
  private static final int MIN_BUFFER_SIZE = 16;

  private static final Charset _charset = Charset.forName("UTF-8");

  private int _currentIndex;
  private ByteBuffer _currentBuffer;
  private ArrayList<ByteBuffer> _bufferList = new ArrayList<ByteBuffer>();
  private int _bufferSize;
  private ByteOrder _order;
  private CharsetDecoder _decoder;
  private CharsetEncoder _encoder;
  private BufferChainInputStream _inputStream;
  private BufferChainOutputStream _outputStream;

  public static final class Position
  {
    Position(BufferChain bufferChain, int index, int position)
    {
      _bufferChain = bufferChain;
      _index = index;
      _position = position;
    }

    final BufferChain _bufferChain;
    final int _index;
    final int _position;
  }

  /**
   * Construct an empty {@link BufferChain} with default byte order and buffer size.
   *
   * @see #DEFAULT_ORDER
   * @see #DEFAULT_BUFFER_SIZE
   */
  public BufferChain()
  {
    this(DEFAULT_ORDER, DEFAULT_BUFFER_SIZE);
  }

  /**
   * Construct an empty {@link BufferChain} with the specified byte order and default buffer size.
   *
   * @param order provides the byte order for the data in the buffer chain.
   */
  public BufferChain(ByteOrder order)
  {
    this(order, DEFAULT_BUFFER_SIZE);
  }

  /**
   * Construct an empty {@link BufferChain} with the specified byte order and buffer size.
   *
   * @param order provides the byte order for the data in the buffer chain.
   * @param bufferSize provides the buffer size fo each buffer in the buffer chain.
   */
  public BufferChain(ByteOrder order, int bufferSize)
  {
    if (bufferSize < MIN_BUFFER_SIZE)
    {
      throw new IllegalArgumentException("Buffer size must be at least " + MIN_BUFFER_SIZE);
    }
    _bufferSize = bufferSize;
    _order = order;
    _currentBuffer = allocateByteBuffer(_bufferSize);
    _currentIndex = 0;
    initCoders();
  }

  /**
   * Construct a {@link BufferChain} with the specified data and data's byte order is the
   * default byte order.
   *
   * The {@link BufferChain} directly references the provided data, i.e. it does not
   * copy the data.
   *
   * @param bytes provides the initial data of the new {@link BufferChain}.
   */
  public BufferChain(byte[] bytes)
  {
    this(DEFAULT_ORDER, bytes);
  }

  /**
   * Construct a {@link BufferChain} with the specified byte order and data.
   *
   * The {@link BufferChain} directly references the provided data, i.e. it does not
   * copy the data.   * @param order provides the byte order of the provided data.
   *
   * @param bytes provides the initial data of the new {@link BufferChain}.
   */
  public BufferChain(ByteOrder order, byte[] bytes)
  {
    _order = order;
    _currentBuffer = ByteBuffer.wrap(bytes);
    _currentBuffer.order(_order);
    _currentIndex = 0;
    _bufferList.add(_currentBuffer);
    initCoders();
  }

  // testing use
  BufferChain(ByteOrder order, byte[] bytes, int bufferSize)
  {
    // dump(out, bytes, 0, bytes.length); out.println(); out.println("-------");
    _bufferSize = bufferSize;
    _order = order;
    int more = bytes.length;
    int offset = 0;
    while (more > 0) {
      _currentBuffer = allocateByteBuffer(_bufferSize);
      _currentIndex = _bufferList.size() - 1;
      int length = (more < _bufferSize ? more : _bufferSize);
      _currentBuffer.put(bytes, offset, length);
      // dump(out, _currentBuffer); out.println();
      offset += length;
      more -= length;
    }
    rewind();
    initCoders();
  }

  /**
   * Provides an {@link InputStream} view of the {@link BufferChain}.
   *
   * This method will return the same {@link InputStream}. State changes
   * to the {@link BufferChain} be reflected by the {@link InputStream}. Similarly,
   * operations on the {@link InputStream} will cause state changes in the
   * {@link BufferChain}.
   *
   * @return an {@link InputStream} view of the {@link BufferChain}.
   */
  public InputStream asInputStream()
  {
    if (_inputStream == null)
    {
      _inputStream = new BufferChainInputStream(this);
    }
    return _inputStream;
  }

  /**
   * Provides an {@link OutputStream} view of the {@link BufferChain}.
   *
   * This method will return the same {@link OutputStream}. State changes
   * to the {@link BufferChain} be reflected by the {@link OutputStream}. Similarly,
   * operations on the {@link OutputStream} will cause state changes in the
   * {@link BufferChain}.
   *
   * @return an {@link OutputStream} view of the {@link BufferChain}.
   */
  public OutputStream asOutputStream()
  {
    if (_outputStream == null)
    {
      _outputStream = new BufferChainOutputStream(this);
    }
    return _outputStream;
  }

  /**
   * Returns the current position.
   *
   * @return the current position.
   */
  public Position position()
  {
    return new Position(this, _currentIndex, _currentBuffer.position());
  }

  /**
   * Set the current position to the specified {@link Position}.
   *
   * @param pos provides the new current position.
   * @return {@code this}.
   */
  public BufferChain position(Position pos)
  {
    if (pos._bufferChain != this)
    {
      throw new IllegalArgumentException("Position does not apply to this BufferChain");
    }
    _currentIndex = pos._index;
    _currentBuffer = _bufferList.get(_currentIndex);
    if (pos._position == _currentBuffer.limit() && _currentIndex < (_bufferList.size() - 1))
    {
      _currentIndex++;
      _currentBuffer = _bufferList.get(_currentIndex);
      _currentBuffer.position(0);
    }
    else
    {
      _currentBuffer.position(pos._position);
    }
    return this;
  }

  /**
   * Returns number of bytes between the start position (inclusive) and end position (exclusive).
   * <p>
   * The offset is zero if the specified start and end positions reference the same position.
   * The offset is one if the end position is one position more advanced then the start position.
     <p>
   * @param startPos provides the start position (inclusive).
   * @param endPos provides the end position (exclusive).
   * @return the number of bytes between the start position (inclusive) and end position (exclusive).
   */
  public int offset(Position startPos, Position endPos)
  {
    if (startPos._bufferChain != this || endPos._bufferChain != this)
    {
      throw new IllegalArgumentException("Position does not apply to this BufferChain");
    }
    if ((startPos._index > endPos._index) ||
        (startPos._index == endPos._index && startPos._position >= endPos._position))
    {
      throw new IllegalArgumentException("Start position is not less than end position");
    }
    int sum;
    if (startPos._index == endPos._index)
    {
      sum = (endPos._position - startPos._position);
    }
    else {
      int index = startPos._index;
      sum = _bufferList.get(index).limit() - startPos._position;
      index++;
      while (index < endPos._index)
      {
        sum += _bufferList.get(index).limit();
        index++;
      }
      sum += endPos._position;
    }
    return sum;
  }

  /**
   * Returns the byte order of the {@link BufferChain}.
   *
   * @return the byte order of the {@link BufferChain}.
   */
  public ByteOrder order()
  {
    return _order;
  }

  /**
   * Returns the next byte in the {@link BufferChain}.
   * <p>
   * This advances the current position by one.
   * <p>
   * @return the next byte in the {@link BufferChain}
   * @throws BufferUnderflowException if the buffer chain has been exhausted.
   */
  public byte get() throws BufferUnderflowException
  {
    byte res = remain(SIZE_BYTE).get();
    return res;
  }

  /**
   * Fill the specified range of the array with data in the {@link BufferChain}.
   * <p>
   * The range of the array to be filled is specified by an offset and length.
   * The current position is advanced by the number of bytes filled even if
   * {@link BufferUnderflowException} is thrown. The specified may be partially
   * filled.
   * <p>
   * @param dst provides the array to be filled.
   * @param offset provides the offset to start filling.
   * @param length provides the number of bytes to be filled.
   * @return {@code this}.
   * @throws BufferUnderflowException if the buffer chain is exhausted before filling
   *                                  the specified number of bytes.
   */
  public BufferChain get(byte[] dst, int offset, int length)
  {
    int read = read(dst, offset, length);
    if (read < length)
    {
      throw new BufferUnderflowException();
    }
    return this;
  }

  private int read(byte[] dst, int offset, int length)
  {
    int more = length;
    while (more > 0 && advanceBufferIfCurrentBufferHasNoRemaining())
    {
      int remaining = _currentBuffer.remaining();
      if (remaining > more)
      {
        remaining = more;
      }
      _currentBuffer.get(dst, offset, remaining);
      offset += remaining;
      more -= remaining;
    }
    return length - more;
  }

  /**
   * Return a {@link ByteBuffer} filled with the specified number of bytes
   * from {@link BufferChain}.
   * <p>
   * The range of the array to be filled is specified by an offset and length.
   * The current position is advanced by the number of bytes filled even if
   * {@link BufferUnderflowException} is thrown.
   * <p>
   * @param length provides the number of bytes to be filled.
   * @return {@code this}.
   * @throws BufferUnderflowException if the buffer chain is exhausted before filling
   *                                  the specified number of bytes.
   */
  public ByteBuffer get(int length)
  {
    ByteBuffer buffer;
    if (advanceBufferIfCurrentBufferHasNoRemaining() == false)
    {
      throw new BufferUnderflowException();
    }
    int remaining = _currentBuffer.remaining();
    if (remaining < length)
    {
      buffer = ByteBuffer.allocate(length);
      byte[] dst = buffer.array();
      int offset = buffer.arrayOffset();
      int more = length;
      while (more > 0 && advanceBufferIfCurrentBufferHasNoRemaining())
      {
        _currentBuffer = _bufferList.get(_currentIndex);
        remaining = _currentBuffer.remaining();
        if (remaining > more)
        {
          remaining = more;
        }
        _currentBuffer.get(dst, offset, remaining);
        offset += remaining;
        more -= remaining;
      }
      if (more > 0)
      {
        throw new BufferUnderflowException();
      }
    }
    else
    {
      buffer = _currentBuffer.slice();
      buffer.limit(length);
      _currentBuffer.position(_currentBuffer.position() + length);
    }
    buffer.flip();
    return buffer;
  }

  /**
   * Get the next variable length encoded unsigned integer.
   * <p>
   * Each byte encodes 7 bits starting with the least significant bits.
   * If the high order bit is set, then there are no more more significant
   * bits, else there are additional more significant bits in the next byte.
   * <p>
   * @return the next variable length encoded unsigned integer.
   * @throws BufferUnderflowException if the buffer chain is exhausted.
   */
  public int getVarUnsignedInt() throws BufferUnderflowException
  {
    int v = 0;
    int shift = 0;
    while (true)
    {
      byte b = get();
      if ((b & (byte) 0x80) == 0)
      {
        // there are more more significant bits
        v = v | (b << shift);
        shift += 7;
        continue;
      }
      else
      {
        // there are no more more significant bits
        v = v | ((b & 0x7f) << shift);
        break;
      }
    }
    return v;
  }

  /**
   * Get the next ZigZag variable length encoded signed integer.
   * <p>
   * First, get the ZigZag encoded unsigned number using {@link #getVarUnsignedInt()}.
   * Then, convert the ZigZag encoded unsigned number to a signed integer,
   * see https://developers.google.com/protocol-buffers/docs/encoding.
   * <p>
   * @return the next ZigZag variable length encoded signed integer.
   * @throws BufferUnderflowException if the buffer chain is exhausted.
   */
  public int getVarInt() throws BufferUnderflowException
  {
    int v = getVarUnsignedInt();
    int result = (v >> 1) ^ (-(v & 1));
    return result;
  }

  /**
   * Get the next byte order encoded short.
   *
   * @return the next byte order encoded short.
   * @throws BufferUnderflowException if the buffer chain is exhausted.
   */
  public short getShort() throws BufferUnderflowException
  {
    return remain(SIZE_SHORT).getShort();
  }

  /**
   * Get the next byte order encoded integer.
   *
   * @return the next byte order encoded integer.
   * @throws BufferUnderflowException if the buffer chain is exhausted.
   */
  public int getInt() throws BufferUnderflowException
  {
    int res = remain(SIZE_INT).getInt();
    return res;
  }

  /**
   * Get the next byte order encoded long.
   *
   * @return the next byte order encoded long.
   * @throws BufferUnderflowException if the buffer chain is exhausted.
   */
  public long getLong() throws BufferUnderflowException
  {
    return remain(SIZE_LONG).getLong();
  }

  /**
   * Get the next byte order encoded float.
   *
   * @return the next byte order encoded float.
   * @throws BufferUnderflowException if the buffer chain is exhausted.
   */
  public float getFloat() throws BufferUnderflowException
  {
    return remain(SIZE_FLOAT).getFloat();
  }

  /**
   * Get the next byte order encoded double.
   *
   * @return the next byte order encoded double.
   * @throws BufferUnderflowException if the buffer chain is exhausted.
   */
  public double getDouble() throws BufferUnderflowException
  {
    return remain(SIZE_DOUBLE).getDouble();
  }

  /**
   * Get the next UTF-8 encoded string.
   *
   * @return the next UTF-8 encoded string.
   * @throws BufferUnderflowException if the buffer chain is exhausted.
   */
  public String getUtf8CString()
  {
    CharBuffer charBuffer = CharBuffer.allocate(DEFAULT_STRING_LENGTH);
    _decoder.reset();

    boolean found = false;
    boolean first = true;
    while (found == false && advanceBufferIfCurrentBufferHasNoRemaining())
    {
      int position = _currentBuffer.position();
      byte[] array = _currentBuffer.array();
      int arrayOffset = _currentBuffer.arrayOffset();
      int limit = _currentBuffer.limit();
      int arrayLimit = arrayOffset + limit;
      int arrayStart = arrayOffset + position;
      int i = arrayStart;
      while (i < arrayLimit && array[i] != ZERO_BYTE)
      {
        i++;
      }
      // out.println("arrayOffset " + arrayOffset + " arrayLimit " + arrayLimit + " i " + i);
      found = (i < arrayLimit);
      if (i == arrayStart && first)
      {
        _currentBuffer.get();
        // fast path for empty string
        return "";
      }
      else if (found)
      {
        int length = i - arrayStart;
        _currentBuffer.limit(_currentBuffer.position() + length);
        _decoder.decode(_currentBuffer, charBuffer, true);
        _currentBuffer.limit(limit);
      }
      else
      {
        _decoder.decode(_currentBuffer, charBuffer, false);
      }
      position = 0;
      first = false;
    }
    if (found) {
      _currentBuffer.get();
    }
    _decoder.flush(charBuffer);
    charBuffer.flip();
    String s = charBuffer.toString();
    // out.println(s);
    return s;
  }

  /**
   * Get the next UTF-8 encoded null-terminated string.
   *
   * @return the next UTF-8 encoded null terminated string.
   * @throws BufferUnderflowException if the buffer chain is exhausted.
   */
  public String getUtf8CString(int length) throws IOException
  {
    if (length == 0)
    {
      throw new DataDecodingException("Length must be at least 1");
    }
    else if (length == 1)
    {
      byte b = get();
      if (b != ZERO_BYTE)
      {
        throw new DataDecodingException("C string not terminated with null");
      }
      return "";
    }

    CharBuffer charBuffer = CharBuffer.allocate(length);
    _decoder.reset();

    int more = length - 1;
    while (more > 0 && advanceBufferIfCurrentBufferHasNoRemaining())
    {
      int remaining = _currentBuffer.remaining();
      if (remaining > more)
      {
        remaining = more;
        int limit = _currentBuffer.limit();
        _currentBuffer.limit(_currentBuffer.position() + remaining);
        _decoder.decode(_currentBuffer, charBuffer, true);
        _currentBuffer.limit(limit);
      }
      else
      {
        _decoder.decode(_currentBuffer, charBuffer, (remaining == more));
      }
      more -= remaining;
    }
    if (more > 0)
    {
      throw new BufferUnderflowException();
    }
    byte b = get();
    if (b != ZERO_BYTE)
    {
      throw new DataDecodingException("C string not terminated with null");
    }
    _decoder.flush(charBuffer);
    charBuffer.flip();
    String s = charBuffer.toString();
    // out.println(s);
    return s;
  }

  /*
  public int getTo(byte match, byte[] bytes)
  {
    int position = _currentBuffer.position();
    int bufferCount = _bufferList.size();
    int dstIndex = 0;
    boolean found = false;
    int bufferPosition = position;
    int bufferIndex = _currentIndex;
    ByteBuffer buffer = _currentBuffer;
    while (dstIndex < bytes.length)
    {
      byte[] array = buffer.array();
      int arrayOffset = buffer.arrayOffset();
      int limit = buffer.limit();
      int arrayLimit = arrayOffset + limit;
      int i = arrayOffset + position;
      // out.println("arrayOffset " + arrayOffset + " arrayLimit " + arrayLimit);
      while (dstIndex < bytes.length && i < arrayLimit)
      {
        byte b = array[i];
        bytes[dstIndex] = b;
        dstIndex++;
        i++;
        if (b == match)
        {
          found = true;
          break;
        }
      }
      if (found)
      {
        bufferPosition = i - arrayOffset;
        break;
      }
      if (bufferIndex >= bufferCount - 1)
      {
        break;
      }
      bufferIndex++;
      buffer = _bufferList.get(bufferIndex);
      position = 0;
    }
    if (found)
    {
      _currentBuffer = buffer;
      _currentIndex = bufferIndex;
      _currentBuffer.position(bufferPosition);
      return dstIndex;
    }
    return 0;
  }
  */

  /**
   * Put a byte.
   *
   * @param value provides the byte to put.
   * @return {@code this}.
   */
  public BufferChain put(byte value)
  {
    reserve(SIZE_BYTE).put(value);
    return this;
  }

  /**
   * Put the bytes within a range in the array.
   * <p>
   * The range is provided by an offset and a length.
   * <p>
   * @param src provides the array containing the range.
   * @param offset provides the start offset of the range.
   * @param length provides the number of bytes to put.
   * @return {@code this}.
   */
  public BufferChain put(byte[] src, int offset, int length)
  {
    int remaining = _currentBuffer.remaining();
    if (remaining < length)
    {
      int more = length;
      while (more > 0)
      {
        _currentBuffer = _bufferList.get(_currentIndex);
        remaining = _currentBuffer.remaining();
        if (remaining == 0)
        {
          reserve(more);
          remaining = _currentBuffer.remaining();
        }
        if (remaining > more)
        {
          remaining = more;
        }
        _currentBuffer.put(src, offset, remaining);
        offset += remaining;
        more -= remaining;
      }
    }
    else
    {
      _currentBuffer.put(src, offset, length);
    }
    return this;
  }

  /**
   * Put variable length encoded unsigned integer.
   * <p>
   * Each byte encodes 7 bits starting with the least significant bits.
   * If the high order bit is set, then there are no more more significant
   * bits, else there are additional more significant bits in the next byte.
   * <p>
   * @param value provides the integer to put.
   * @return {@code this}.
   */
  public BufferChain putVarUnsignedInt(int value)
  {
    int z = value;
    reserve(SIZE_INT + 1);
    while (true) {
      if ((z & 0xffffff80) != 0)
      {
        // there are more more significant bits
        put((byte) (z & 0x7f));
        z = z >> 7;
        continue;
      }
      else
      {
        // no more more significant bits
        put((byte) ((z & 0x7f) | 0x80));
        break;
      }
    }
    return this;
  }

  /**
   * Put ZigZag variable length encoded signed integer.
   * <p>
   * First convert signed number into unsigned ZigZag encoded number,
   * see https://developers.google.com/protocol-buffers/docs/encoding.
   * Then, put unsigned ZigZag encoded number using {@link #putVarUnsignedInt(int)}.
   * <p>
   * @param value provides the integer to put.
   * @return {@code this}.
   */
  public BufferChain putVarInt(int value)
  {
    int v = (value << 1) ^ (value >> 31);
    putVarUnsignedInt(v);
    return this;
  }

  /**
   * Put byte order encoded short.
   *
   * @param value provides the short to put.
   * @return {@code this}.
   */
  public BufferChain putShort(short value)
  {
    reserve(SIZE_SHORT).putShort(value);
    return this;
  }

  /**
   * Put byte order encoded integer.
   *
   * @param value provides the integer to put.
   * @return {@code this}.
   */
  public BufferChain putInt(int value)
  {
    reserve(SIZE_INT).putInt(value);
    return this;
  }

  /**
   * Put byte order encoded long.
   *
   * @param value provides the long to put.
   * @return {@code this}.
   */
  public BufferChain putLong(long value)
  {
    reserve(SIZE_LONG).putLong(value);
    return this;
  }

  /**
   * Put byte order encoded float.
   *
   * @param value provides the float to put.
   * @return {@code this}.
   */
  public BufferChain putFloat(float value)
  {
    reserve(SIZE_FLOAT).putFloat(value);
    return this;
  }

  /**
   * Put byte order encoded double.
   *
   * @param value provides the double to put.
   * @return {@code this}.
   */
  public BufferChain putDouble(double value)
  {
    reserve(SIZE_DOUBLE).putDouble(value);
    return this;
  }

  /**
   * Put string into buffer chain as UTF-8 encoded null-terminated string.
   *
   * @param value provides the string to put.
   * @return {@code this}.
   */
  public BufferChain putUtf8CString(String value) throws CharacterCodingException
  {
    reserve(value.length() * 4);
    _encoder.reset();
    CoderResult result = _encoder.encode(CharBuffer.wrap(value), _currentBuffer, true);
    if (result.isError())
    {
      result.throwException();
    }
    _encoder.flush(_currentBuffer);
    put(ZERO_BYTE);
    return this;
  }

  /**
   * Put bytes in {@link ByteString}.
   *
   * @param value provides the {@link ByteString} to put.
   * @return {@code this}.
   */
  public BufferChain putByteString(ByteString value)
  {
    reserve(value.length());
    _currentBuffer.put(value.asByteBuffer());
    return this;
  }

  /**
   * Return the bytes in the buffer chain.
   *
   * @return the bytes in the buffer chain.
   */
  public byte[] toBytes()
  {
    if (_currentBuffer.remaining() > 0)
    {
      _currentBuffer.limit(_currentBuffer.position());
    }
    rewind();
    int size = 0;
    for (ByteBuffer buffer : _bufferList)
    {
      size += buffer.limit();
      // out.println("limit " + buffer.remaining());
    }
    byte[] bytes = new byte[size];
    int offset = 0;
    for (ByteBuffer buffer : _bufferList)
    {
      int length = buffer.limit();
      buffer.get(bytes, offset, length);
      // dump(out, bytes, offset, length); out.println(); // XXX
      offset += length;
    }
    return bytes;
  }

  /**
   * Rewind the buffer chain, i.e. set the current position to
   * the beginning of the buffer chain.
   *
   * @return {@code this}.
   */
  public BufferChain rewind()
  {
    for (ByteBuffer buffer : _bufferList)
    {
      // out.println("limit " + buffer.limit());
      buffer.rewind();
      // out.println("limit after rewind " + buffer.limit());
    }
    _currentIndex = 0;
    _currentBuffer = _bufferList.get(_currentIndex);
    return this;
  }

  /**
   * Append the data in the specified {@link InputStream} into the buffer chain.
   *
   * @param inputStream provides the data to append.
   * @return {@code this}
   * @throws IOException if operations on the {@link InputStream} throws this exception.
   */
  public BufferChain readFromInputStream(InputStream inputStream) throws IOException
  {
    boolean done = false;
    while (done == false)
    {
      _currentBuffer = _bufferList.get(_currentIndex);
      int remaining = _currentBuffer.remaining();
      if (remaining == 0)
      {
        int newBufferSize = Math.max(inputStream.available(), _bufferSize);
        reserve(newBufferSize);
        remaining = _currentBuffer.remaining();
      }
      int bytesRead = inputStream.read(_currentBuffer.array(),
                                       _currentBuffer.arrayOffset(),
                                       remaining);
      // out.println("remaining " + remaining + " bytesRead " + bytesRead);

      if (bytesRead != -1)
      {
        int newPosition = _currentBuffer.position() + bytesRead;
        _currentBuffer.position(newPosition);
      }
      if (bytesRead < remaining)
      {
        done = true;
      }
    }
    return this;
  }

  /**
   * Write bytes in the buffer chain to the specified {@link OutputStream}.
   *
   * @param outputStream provides the {@link OutputStream} to write to.
   * @return {@code this}
   * @throws IOException if operations on the {@link OutputStream} throws this exception.
   */
  public BufferChain writeToOutputStream(OutputStream outputStream) throws IOException
  {
    if (_currentBuffer.remaining() > 0)
    {
      _currentBuffer.limit(_currentBuffer.position());
    }
    rewind();
    for (ByteBuffer buffer : _bufferList)
    {
      outputStream.write(buffer.array(),
                         buffer.arrayOffset(),
                         buffer.remaining());
    }
    return this;
  }

  private static class BufferChainInputStream extends InputStream
  {
    public BufferChainInputStream(BufferChain bufferChain)
    {
      _bufferChain = bufferChain;
    }

    @Override
    public int read()
    {
      try
      {
        return _bufferChain.get();
      }
      catch (BufferUnderflowException exc)
      {
        return -1;
      }
    }

    @Override
    public int read(byte[] dst, int offset, int length)
    {
      return _bufferChain.read(dst, offset, length);
    }

    @Override
    public int read(byte[] dst)
    {
      return read(dst, 0, dst.length);
    }

    @Override
    public void mark(int readLimit)
    {
      _position = _bufferChain.position();
    }

    @Override
    public void reset() throws IOException
    {
      if (_position == null)
      {
        throw new IOException("Mark not called before reset");
      }
      _bufferChain.position(_position);
    }

    @Override
    public boolean markSupported()
    {
      return true;
    }

    private final BufferChain _bufferChain;
    private Position _position;
  }

  private static class BufferChainOutputStream extends OutputStream
  {
    public BufferChainOutputStream(BufferChain bufferChain)
    {
      _bufferChain = bufferChain;
    }

    @Override
    public void close()
    {
    }

    @Override
    public void flush()
    {
    }

    @Override
    public void write(byte[] src)
    {
      _bufferChain.put(src, 0, src.length);
    }

    @Override
    public void write(byte[] src, int offset, int length)
    {
      _bufferChain.put(src, offset, length);
    }

    @Override
    public void write(int src)
    {
      _bufferChain.put((byte) src);
    }

    private final BufferChain _bufferChain;
  }

  static void dump(PrintStream os, byte[] bytes)
  {
    dump(os, bytes, 0, bytes.length);
  }

  static void dump(PrintStream os, byte[] bytes, int offset, int length)
  {
    for (int i = offset; i < offset + length; ++i)
    {
      byte b = bytes[i];
      if (b >= 32 && b < 127)
      {
        os.print("'" + (char) b + "'");
      }
      else
      {
        os.print((int) b);
      }
      os.print(' ');
    }
  }

  static void dump(PrintStream os, ByteBuffer buffer)
  {
    dump(os, buffer.array(), buffer.arrayOffset(), buffer.limit());
  }

  private void initCoders()
  {
    _decoder = _charset.newDecoder();
    _decoder.onMalformedInput(CodingErrorAction.REPLACE);
    _decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);

    _encoder = _charset.newEncoder();
    _encoder.onMalformedInput(CodingErrorAction.REPLACE);
    _encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
  }

  private final boolean advanceBufferIfCurrentBufferHasNoRemaining()
  {
    int remaining = _currentBuffer.remaining();
    if (remaining > 0)
    {
      return true;
    }
    else if (remaining == 0 && _currentIndex < (_bufferList.size() - 1))
    {
      ++_currentIndex;
      _currentBuffer = _bufferList.get(_currentIndex);
      // dump(out, _currentBuffer); out.println();
      return true;
    }
    else
    {
      return false;
    }
  }

  private final ByteBuffer remain(int length)
  {
    ByteBuffer buffer;
    if (advanceBufferIfCurrentBufferHasNoRemaining() == false)
    {
      throw new BufferUnderflowException();
    }
    int remaining = _currentBuffer.remaining();
    if (remaining < length)
    {
      // out.println("remaining(" + length + ") " + remaining);
      byte[] bytes = new byte[length];
      _currentBuffer.get(bytes, 0, remaining);
      if (advanceBufferIfCurrentBufferHasNoRemaining() == false)
      {
        throw new BufferUnderflowException();
      }
      _currentBuffer.get(bytes, remaining, length - remaining);
      buffer = ByteBuffer.wrap(bytes);
      buffer.order(_order);
    }
    else
    {
      buffer = _currentBuffer;
    }
    return buffer;
  }

  private final ByteBuffer reserve(int size)
  {
    if (_currentBuffer.remaining() < size)
    {
      _currentBuffer.limit(_currentBuffer.position());
      _currentBuffer = allocateByteBuffer(size);
      _currentIndex++;
    }
    return _currentBuffer;
  }

  private ByteBuffer allocateByteBuffer(int size)
  {
    ByteBuffer byteBuffer = ByteBuffer.allocate(size > _bufferSize ? size : _bufferSize);
    byteBuffer.order(_order);
    _bufferList.add(byteBuffer);
    return byteBuffer;
  }
}
