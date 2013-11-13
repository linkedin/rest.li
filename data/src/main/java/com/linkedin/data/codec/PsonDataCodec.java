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


/**
 * Prototype higher performance codec
 */

import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.collections.CheckedUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A prototype non-standard performance optimized JSON codec.
 *
 * <p>
 * When coding, this codec maintains a map of the keys that have been seen
 * the beginning of the coding operation. Each key is assigned an unique
 * numerical index starting at 1. If the key as not been seen before,
 * encode the negative value of the index and the value of the key.
 * If the key has been before, encode the positive value of the key's
 * index.
 *
 * <p>
 * Similarly, when decoding, the codec maintains an array that maps key
 * index to the value of the key. When decoding, if the encoded index is
 * negative, then it is followed by the value of the key to associate with
 * the positive value of the encoded index. If the encoded index is positive,
 * then the encoded index is used to lookup the value of the key.
 *
 * <p>
 * The encoded index is 0, then there are no more keys in the JSON object.
 *
 * @author slim
 */
public class PsonDataCodec implements DataCodec
{
  private static final byte[] HEADER = { 0x23, 0x21, 0x50, 0x53, 0x4f, 0x4e, 0x31, 0x0a };  // #!PSON1\n

  private boolean _testMode;
  private Options _options = new Options();

  public static class Options
  {
    public Options setEncodeStringLength(boolean value)
    {
      _encodeStringLength = value;
      return this;
    }

    public boolean getEncodeStringLength()
    {
      return _encodeStringLength;
    }

    public Options setEncodeCollectionCount(boolean value)
    {
      _encodeCollectionCount = value;
      return this;
    }

    public boolean getEncodeCollectionCount()
    {
      return _encodeCollectionCount;
    }

    public Options setBufferSize(Integer value)
    {
      _bufferSize = value;
      return this;
    }

    public Integer getBufferSize()
    {
      return _bufferSize;
    }

    @Override
    public String toString()
    {
      return
        "encodeCollectionCount=" + _encodeCollectionCount +
        ", encodeStringLength=" + _encodeStringLength +
        (_bufferSize != null ? ", bufferSize=" + _bufferSize : "");
    }

    @Override
    public boolean equals(Object o)
    {
      if (o == null)
        return false;
      if (o == this)
        return true;
      if (o instanceof Options == false)
        return false;
      Options other = (Options) o;
      return
        (_encodeCollectionCount == other._encodeCollectionCount) &&
        (_encodeStringLength == other._encodeStringLength) &&
        (_bufferSize == null ? _bufferSize == other._bufferSize : _bufferSize.equals(other._bufferSize));
    }

    @Override
    public int hashCode()
    {
      return
        ((_encodeCollectionCount ? 3131 : 0) +
         (_encodeStringLength ? 31310000 : 0)) ^
        (_bufferSize != null ? _bufferSize.hashCode() : 0);
    }

    private boolean _encodeStringLength = true;
    private boolean _encodeCollectionCount = false;
    private Integer _bufferSize = null;
  }

  public PsonDataCodec()
  {
  }

  public PsonDataCodec(boolean testMode)
  {
    _testMode = testMode;
  }

  public PsonDataCodec setOptions(Options options)
  {
    _options = options;
    return this;
  }

  public Options getOptions()
  {
    return _options;
  }

  private PsonSerializer serialize(DataComplex map) throws IOException
  {
    PsonSerializer serializer = new PsonSerializer();
    serializer.serialize(map);
    return serializer;
  }

  protected byte[] complexToBytes(DataComplex complex) throws IOException
  {
    try
    {
      byte[] bytes = serialize(complex).toBytes();
      return bytes;
    }
    catch (RuntimeException exc)
    {
      // do not want RuntimeException from BufferChain propagating
      // as RuntimeException to client code.
      throw new IOException("Unexpected RuntimeException", exc);
    }
  }

  protected <T extends DataComplex> T bytesToComplex(byte[] input, Class<T> clazz) throws IOException
  {
    try
    {
      BufferChain buffer =
        (_testMode && _options.getBufferSize() != null) ?
          new BufferChain(ByteOrder.LITTLE_ENDIAN, input, _options.getBufferSize()) :
          new BufferChain(ByteOrder.LITTLE_ENDIAN, input);
      PsonParser psonParser = new PsonParser(buffer);
      return clazz.cast(psonParser.read());
    }
    catch (RuntimeException exc)
    {
      // do not want RuntimeException from BufferChain propagating
      // as RuntimeException to client code.
      throw new IOException("Unexpected RuntimeException", exc);
    }
  }

  @Override
  public byte[] mapToBytes(DataMap map) throws IOException
  {
    return complexToBytes(map);
  }

  @Override
  public byte[] listToBytes(DataList list) throws IOException
  {
    return complexToBytes(list);
  }

  @Override
  public DataMap bytesToMap(byte[] input) throws IOException
  {
    return bytesToComplex(input, DataMap.class);
  }

  @Override
  public DataList bytesToList(byte[] input) throws IOException
  {
    return bytesToComplex(input, DataList.class);
  }

  protected void writeComplex(DataComplex complex, OutputStream out) throws IOException
  {
    try
    {
      serialize(complex).writeToOutputStream(out);
    }
    catch (RuntimeException exc)
    {
      // do not want RuntimeException from BufferChain propagating
      // as RuntimeException to client code.
      throw new IOException("Unexpected RuntimeException", exc);
    }
  }

  @Override
  public void writeMap(DataMap map, OutputStream out) throws IOException
  {
    writeComplex(map, out);
  }

  @Override
  public void writeList(DataList list, OutputStream out) throws IOException
  {
    writeComplex(list, out);
  }

  protected <T extends DataComplex> T readComplex(InputStream in, Class<T> clazz) throws IOException
  {
    try
    {
      BufferChain buffer =
        (_testMode && _options.getBufferSize() != null) ?
          new BufferChain(ByteOrder.LITTLE_ENDIAN, _options.getBufferSize()) :
          new BufferChain(ByteOrder.LITTLE_ENDIAN);
      buffer.readFromInputStream(in);
      buffer.rewind();
      PsonParser psonParser = new PsonParser(buffer);
      return clazz.cast(psonParser.read());
    }
    catch (RuntimeException exc)
    {
      // do not want RuntimeException from BufferChain propagating
      // as RuntimeException to client code.
      throw new IOException("Unexpected RuntimeException", exc);
    }
  }

  @Override
  public DataMap readMap(InputStream in) throws IOException
  {
    return readComplex(in, DataMap.class);
  }

  @Override
  public DataList readList(InputStream in) throws IOException
  {
    return readComplex(in, DataList.class);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "(" + _options + ")";
  }

  final static byte ZERO_BYTE = 0;
  final static byte ONE_BYTE = 1;

  final static int PSON_INVALID_KEY_INDEX = 0;

  final static byte PSON_NULL = 0;
  final static byte PSON_BOOLEAN = 1;
  final static byte PSON_INT = 2;
  final static byte PSON_LONG = 3;
  final static byte PSON_FLOAT = 4;
  final static byte PSON_DOUBLE = 5;
  final static byte PSON_BINARY = 6;
  final static byte PSON_STRING_EMPTY = 8;
  final static byte PSON_STRING = 9;
  final static byte PSON_STRING_WITH_LENGTH_4 = 10;
  final static byte PSON_STRING_WITH_LENGTH_2 = 11;
  final static byte PSON_ARRAY_EMPTY = 16;
  final static byte PSON_ARRAY = 17;
  final static byte PSON_ARRAY_WITH_COUNT = 18;
  final static byte PSON_OBJECT_EMPTY = 32;
  final static byte PSON_OBJECT = 33;
  final static byte PSON_OBJECT_WITH_COUNT = 34;
  final static byte PSON_LAST = (byte) 0xff;

  private final static int MAX_STRING_WITH_LENGTH_2 = Short.MAX_VALUE / 2 - 1;

  protected class PsonSerializer implements Data.TraverseCallback
  {
    private final BufferChain _buffer;
    private final HashMap<String, Integer> _keyMap = new HashMap<String, Integer>(200);
    private int _keyIndex = 1;
    private final boolean _encodeStringLength = _options.getEncodeStringLength();
    private final boolean _encodeCollectionCount = _options.getEncodeCollectionCount();

    protected PsonSerializer()
    {
      _buffer =
        _options.getBufferSize() == null ?
          new BufferChain(ByteOrder.LITTLE_ENDIAN) :
          new BufferChain(ByteOrder.LITTLE_ENDIAN, _options.getBufferSize());
    }

    @Override
    public Iterable<Map.Entry<String,Object>> orderMap(DataMap map)
    {
      return map.entrySet();
    }

    @Override
    public void nullValue() throws CharacterCodingException
    {
      _buffer.put(PSON_NULL);
    }

    @Override
    public void booleanValue(boolean value) throws CharacterCodingException
    {
      _buffer.put(PSON_BOOLEAN);
      _buffer.put(value ? ONE_BYTE : ZERO_BYTE);
    }

    @Override
    public void integerValue(int value) throws CharacterCodingException
    {
      _buffer.put(PSON_INT);
      _buffer.putInt(value);
    }

    @Override
    public void longValue(long value) throws CharacterCodingException
    {
      _buffer.put(PSON_LONG);
      _buffer.putLong(value);
    }

    @Override
    public void floatValue(float value) throws CharacterCodingException
    {
      _buffer.put(PSON_FLOAT);
      _buffer.putFloat(value);
    }

    @Override
    public void doubleValue(double value) throws CharacterCodingException
    {
      _buffer.put(PSON_DOUBLE);
      _buffer.putDouble(value);
    }

    @Override
    public void stringValue(String value) throws CharacterCodingException
    {
      if (value.isEmpty())
      {
        _buffer.put(PSON_STRING_EMPTY);
      }
      else if (_encodeStringLength)
      {
        putStringWithLength(value);
      }
      else
      {
        _buffer.put(PSON_STRING);
        _buffer.putUtf8CString(value);
      }
    }

    @Override
    public void byteStringValue(ByteString value) throws CharacterCodingException
    {
      _buffer.put(PSON_BINARY);
      _buffer.putInt(value.length());
      _buffer.putByteString(value);
    }

    @Override
    public void illegalValue(Object value) throws IOException
    {
      throw new IOException("Illegal type encountered: " + value.getClass());
    }

    @Override
    public void emptyMap() throws CharacterCodingException
    {
      _buffer.put(PSON_OBJECT_EMPTY);
    }

    @Override
    public void startMap(DataMap map) throws CharacterCodingException
    {
      if (_encodeCollectionCount)
      {
        start(PSON_OBJECT_WITH_COUNT);
        _buffer.putVarUnsignedInt(map.size());
      }
      else
      {
        start(PSON_OBJECT);
      }
    }

    @Override
    public void key(String key) throws CharacterCodingException
    {
      Integer found = _keyMap.get(key);
      int index;
      if (found == null)
      {
        _keyMap.put(key, _keyIndex);
        index = -_keyIndex;
        _buffer.putVarInt(index);
        _buffer.putUtf8CString(key);
        _keyIndex++;
      }
      else
      {
        index = found;
        _buffer.putVarInt(index);
      }
    }

    @Override
    public void endMap()
    {
      _buffer.putVarInt(PSON_INVALID_KEY_INDEX);
    }

    @Override
    public void emptyList() throws CharacterCodingException
    {
      _buffer.put(PSON_ARRAY_EMPTY);
    }

    @Override
    public void startList(DataList list) throws CharacterCodingException
    {
      if (_encodeCollectionCount)
      {
        start(PSON_ARRAY_WITH_COUNT);
        _buffer.putVarUnsignedInt(list.size());
      }
      else
      {
        start(PSON_ARRAY);
      }
    }

    @Override
    public void index(int index)
    {
    }

    @Override
    public void endList()
    {
      _buffer.put(PSON_LAST);
    }

    private final void putStringWithLength(String s) throws CharacterCodingException
    {
      int stringLength = s.length();
      if (false && stringLength <= MAX_STRING_WITH_LENGTH_2)
      {
        _buffer.put(PSON_STRING_WITH_LENGTH_2);
        BufferChain.Position startPos = _buffer.position();
        _buffer.putShort((short) 0);
        _buffer.putUtf8CString(s);
        BufferChain.Position endPos = _buffer.position();
        _buffer.position(startPos);
        _buffer.putShort((short) (_buffer.offset(startPos, endPos) - 2));
        _buffer.position(endPos);
      }
      else
      {
        _buffer.put(PSON_STRING_WITH_LENGTH_4);
        BufferChain.Position startPos = _buffer.position();
        _buffer.putInt(0);
        _buffer.putUtf8CString(s);
        BufferChain.Position endPos = _buffer.position();
        _buffer.position(startPos);
        _buffer.putInt(_buffer.offset(startPos, endPos) - 4);
        _buffer.position(endPos);
      }
    }

    private void serialize(DataComplex map) throws IOException
    {
      _buffer.put(HEADER, 0, HEADER.length);
      Data.traverse(map, this);
    }

    private final byte[] toBytes()
    {
      return _buffer.toBytes();
    }

    private final void writeToOutputStream(OutputStream out) throws IOException
    {
      _buffer.writeToOutputStream(out);
    }

    private void start(byte psonType) throws CharacterCodingException
    {
      _buffer.put(psonType);
    }
  }

  protected static class PsonParser
  {

    PsonParser(BufferChain buffer)
    {
      _buffer = buffer;
    }

    static final String HEX = "0123456789ABCDEF";

    static String bytesToString(byte bytes[])
    {
      StringBuffer sb = new StringBuffer(bytes.length * 2 + 4);
      sb.append("0x");
      for (int i = 0; i < bytes.length; i++)
      {
        int v = bytes[i];
        sb.append(HEX.charAt(v >> 4));
        sb.append(HEX.charAt(v & 0x0f));
      }
      return sb.toString();
    }

    Object read() throws IOException
    {
      byte header[] = new byte[HEADER.length];
      _buffer.get(header, 0, header.length);
      if (Arrays.equals(header, HEADER) == false)
      {
        throw new IOException("Expecting header " + bytesToString(HEADER) + " but got " + bytesToString(header));
      }

      return parseValue();
    }

    DataList parseArray(boolean withCount) throws IOException
    {
      int size = (withCount ? _buffer.getVarUnsignedInt() : -1);
      DataList list = (size >= 0 ? new DataList(size) : new DataList());
      int count = 0;
      for (count = 0; ; count++)
      {
        Object item = parseValue();
        if (item == null)
        {
          break;
        }
        CheckedUtil.addWithoutChecking(list, item);
      }

      if (size >= 0 && count != size)
      {
        throw new IOException("Actual number array items (" + count + ") is not the same as expected (" + size + ")");
      }

      return list;
    }

    DataMap parseMap(boolean withCount) throws IOException
    {
      int size = (withCount ? _buffer.getVarUnsignedInt() : -1);
      DataMap map = (size >= 0 ? new DataMap((int) ((size * 1.5) + 0.5)) : new DataMap());
      int count;
      for (count = 0; ; count++)
      {
        int keyIndex = _buffer.getVarInt();
        String key;
        if (keyIndex == PSON_INVALID_KEY_INDEX)
        {
          break;
        }
        if (keyIndex < 0)
        {
          keyIndex = -keyIndex;
          if (keyIndex != _expectedKeyIndex)
          {
            throw new IOException("Received new key index " + keyIndex + " but expecting " + _expectedKeyIndex);
          }
          _expectedKeyIndex++;
          if (keyIndex >= _keyArray.length)
          {
            resizeKeyArray();
          }
          assert(_keyArray[keyIndex] == null);
          key = _buffer.getUtf8CString();
          _keyArray[keyIndex] = key;
        }
        else
        {
          key = _keyArray[keyIndex];
          assert(key != null);
        }
        Object item = parseValue();
        if (item == null)
        {
          throw new IOException("Unexpected end of array");
        }
        CheckedUtil.putWithoutChecking(map, key, item);
      }

      if (size >= 0 && count != size)
      {
        throw new IOException("Actual number object fields (" + count + ") is not the same as expected (" + size + ")");
      }

      return map;
    }

    private void resizeKeyArray()
    {
      String[] newKeyArray = new String[_keyArray.length * 2];
      System.arraycopy(_keyArray, 0, newKeyArray, 0, _keyArray.length);
      _keyArray = newKeyArray;
    }

    Object parseValue() throws IOException
    {
      byte psonType = _buffer.get();

      Object o = null;
      boolean valid = true;
      switch (psonType)
      {
        case PSON_OBJECT_EMPTY:
          o = new DataMap();
          break;
        case PSON_OBJECT:
          o = parseMap(false);
          break;
        case PSON_OBJECT_WITH_COUNT:
          o = parseMap(true);
          break;
        case PSON_ARRAY_EMPTY:
          o = new DataList();
          break;
        case PSON_ARRAY:
          o = parseArray(false);
          break;
        case PSON_ARRAY_WITH_COUNT:
          o = parseArray(true);
          break;
        case PSON_INT:
          o = _buffer.getInt();
          break;
        case PSON_LONG:
          o = _buffer.getLong();
          break;
        case PSON_FLOAT:
          o = _buffer.getFloat();
          break;
        case PSON_DOUBLE:
          o = _buffer.getDouble();
          break;
        case PSON_STRING_EMPTY:
          o = "";
          break;
        case PSON_STRING:
          o = _buffer.getUtf8CString();
          break;
        case PSON_STRING_WITH_LENGTH_4:
          o = getStringWithLength(_buffer.getInt());
          break;
        case PSON_STRING_WITH_LENGTH_2:
          o = getStringWithLength(_buffer.getShort());
          break;
        case PSON_BOOLEAN:
          byte b = _buffer.get();
          o = new Boolean(b != ZERO_BYTE);
          break;
        case PSON_BINARY:
          int length = _buffer.getInt();
          o = ByteString.read(_buffer.asInputStream(), length);
          break;
        case PSON_NULL:
          o = Data.NULL;
          break;
        case PSON_LAST:
          o = null;
          break;
        default:
          valid = false;
          break;
      }
      if (valid == false)
      {
        throw new IOException("Illegal PSON element code " + psonType);
      }
      return o;
    }

    private String getStringWithLength(int length) throws IOException
    {
      if (length == 0)
      {
        throw new DataDecodingException("String size should not be 0");
      }
      return _buffer.getUtf8CString(length);
    }

    private final BufferChain _buffer;
    private String _keyArray[] = new String[100];
    private int _expectedKeyIndex = 1;
  }
}

