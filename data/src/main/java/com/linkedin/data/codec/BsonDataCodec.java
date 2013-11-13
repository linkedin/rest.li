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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * A JSON codec that implements its own serialization and de-serialization.
 *
 * @author slim
 */
public class BsonDataCodec implements DataCodec
{
  private static final String UTF_8 = "UTF-8";
  private Integer _bufferSize;
  private boolean _testMode;

  public BsonDataCodec()
  {
    _bufferSize = null;
  }

  public BsonDataCodec(int bufferSize)
  {
    this(bufferSize, false);
  }

  public BsonDataCodec(int bufferSize, boolean testMode)
  {
    _bufferSize = bufferSize;
    _testMode = testMode;
  }

  public void setBufferSize(int bufferSize)
  {
    _bufferSize = bufferSize;
  }

  protected byte[] complexToBytes(DataComplex complex) throws IOException
  {
    try
    {
      BsonTraverseCallback callback =
        (_bufferSize == null) ?
        new BsonTraverseCallback() :
        new BsonTraverseCallback(_bufferSize);
      Data.traverse(complex, callback);
      byte[] bytes = callback.toBytes();
      return bytes;
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

  protected <T extends DataComplex> T bytesToComplex(byte[] input, Class<T> clazz) throws IOException
  {
    try
    {
      BufferChain buffer =
        (_testMode && _bufferSize != null) ?
        new BufferChain(ByteOrder.LITTLE_ENDIAN, input, _bufferSize) :
        new BufferChain(ByteOrder.LITTLE_ENDIAN, input);
      BsonParser bsonParser = new BsonParser(buffer);
      return bsonParser.parseComplex(clazz);
    }
    catch (RuntimeException exc)
    {
      // do not want RuntimeException from BufferChain propagating
      // as RuntimeException to client code.
      throw new IOException("Unexpected RuntimeException", exc);
    }
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
      BsonTraverseCallback callback =
          (_bufferSize == null) ?
              new BsonTraverseCallback() :
              new BsonTraverseCallback(_bufferSize);
      Data.traverse(complex, callback);
      callback.writeToOutputStream(out);
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
          (_testMode && _bufferSize != null) ?
              new BufferChain(ByteOrder.LITTLE_ENDIAN, _bufferSize) :
              new BufferChain(ByteOrder.LITTLE_ENDIAN);
      buffer.readFromInputStream(in);
      buffer.rewind();
      BsonParser bsonParser = new BsonParser(buffer);
      return bsonParser.parseComplex(clazz);
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

  static final byte ZERO_BYTE = 0;
  static final byte ONE_BYTE = 1;

  static final byte BSON_DOUBLE = 1;
  static final byte BSON_STRING = 2;
  static final byte BSON_EMBEDDED_DOCUMENT = 3;
  static final byte BSON_ARRAY = 4;
  static final byte BSON_BINARY = 5;
  static final byte BSON_DEPRECATED = 6;
  static final byte BSON_OBJECTID = 7;
  static final byte BSON_BOOLEAN = 8;
  static final byte BSON_UTC_DATETIME = 9;
  static final byte BSON_NULL = 10;
  static final byte BSON_REGEX = 11;
  static final byte BSON_DBPOINTER_DEPRECATED = 12;
  static final byte BSON_JAVASCRIPT_CODE = 13;
  static final byte BSON_SYMBOL = 14;
  static final byte BSON_JAVASCRIPT_CODE_WITH_SCOPE = 15;
  static final byte BSON_32BIT_INTEGER = 16;
  static final byte BSON_TIMESTAMP = 17;
  static final byte BSON_64BIT_INTEGER = 18;
  static final byte BSON_MINKEY = -1;
  static final byte BSON_MAXKEY = 127;

  protected static class BsonTraverseCallback implements Data.TraverseCallback
  {
    private final BufferChain _buffer;
    private final Deque<BufferChain.Position> _positionStack = new ArrayDeque<BufferChain.Position>();
    private String _currentName = null;

    BsonTraverseCallback()
    {
      _buffer = new BufferChain(ByteOrder.LITTLE_ENDIAN);
    }

    BsonTraverseCallback(int bufferSize)
    {
      _buffer = new BufferChain(ByteOrder.LITTLE_ENDIAN, bufferSize);
    }

    @Override
    public Iterable<Map.Entry<String,Object>> orderMap(DataMap map)
    {
      return map.entrySet();
    }

    @Override
    public void nullValue() throws CharacterCodingException
    {
      _buffer.put(BSON_NULL);
      putName();
    }

    @Override
    public void booleanValue(boolean value) throws CharacterCodingException
    {
      _buffer.put(BSON_BOOLEAN);
      putName();
      _buffer.put(value ? ONE_BYTE : ZERO_BYTE);
    }

    @Override
    public void integerValue(int value) throws CharacterCodingException
    {
      _buffer.put(BSON_32BIT_INTEGER);
      putName();
      _buffer.putInt(value);
    }

    @Override
    public void longValue(long value) throws CharacterCodingException
    {
      _buffer.put(BSON_64BIT_INTEGER);
      putName();
      _buffer.putLong(value);
    }

    @Override
    public void floatValue(float value) throws CharacterCodingException
    {
      doubleValue(value);
    }

    @Override
    public void doubleValue(double value) throws CharacterCodingException
    {
      _buffer.put(BSON_DOUBLE);
      putName();
      _buffer.putDouble(value);
    }

    @Override
    public void stringValue(String value) throws CharacterCodingException
    {
      _buffer.put(BSON_STRING);
      putName();
      putString(value);
    }

    @Override
    public void byteStringValue(ByteString value) throws CharacterCodingException
    {
      _buffer.put(BSON_BINARY);
      putName();
      _buffer.putInt(value.length());
      _buffer.put((byte) 0);
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
      emptyDocument(BSON_EMBEDDED_DOCUMENT);
    }

    @Override
    public void startMap(DataMap map) throws CharacterCodingException
    {
      startDocument(BSON_EMBEDDED_DOCUMENT);
    }

    @Override
    public void key(String key)
    {
      _currentName = key;
    }

    @Override
    public void endMap()
    {
      endDocument();
    }

    @Override
    public void emptyList() throws CharacterCodingException
    {
      emptyDocument(BSON_ARRAY);
    }

    @Override
    public void startList(DataList list) throws CharacterCodingException
    {
      startDocument(BSON_ARRAY);
    }

    @Override
    public void index(int index)
    {
      _currentName = String.valueOf(index);
    }

    @Override
    public void endList()
    {
      endDocument();
    }

    private final void putName() throws CharacterCodingException
    {
      putCString(_currentName);
    }

    private final void putString(String s) throws CharacterCodingException
    {
      BufferChain.Position startPos = _buffer.position();
      _buffer.putInt(0);
      _buffer.putUtf8CString(s);
      BufferChain.Position endPos = _buffer.position();
      _buffer.position(startPos);
      _buffer.putInt(_buffer.offset(startPos, endPos) - 4);
      _buffer.position(endPos);
    }

    private final byte[] toBytes()
    {
      return _buffer.toBytes();
    }

    private final void writeToOutputStream(OutputStream out) throws IOException
    {
      _buffer.writeToOutputStream(out);
    }

    private final void putCString(String s) throws CharacterCodingException
    {
      _buffer.putUtf8CString(s);
    }

    private void emptyDocument(byte bsonType) throws CharacterCodingException
    {
      if (_currentName != null)
      {
        _buffer.put(bsonType);
        putName();
      }
      _buffer.putInt(1);
      _buffer.put(ZERO_BYTE);
    }

    private void startDocument(byte bsonType) throws CharacterCodingException
    {
      if (_currentName != null)
      {
        _buffer.put(bsonType);
        putName();
      }
      BufferChain.Position pos = _buffer.position();
      _positionStack.addLast(pos);
      _buffer.putInt(0);
    }

    private void endDocument()
    {
      _buffer.put(ZERO_BYTE);
      BufferChain.Position startPos = _positionStack.removeLast();
      BufferChain.Position endPos = _buffer.position();
      int length = _buffer.offset(startPos, endPos);
      _buffer.position(startPos);
      _buffer.putInt(length);
      _buffer.position(endPos);
    }
  }

  protected static class BsonParser
  {
    BsonParser(BufferChain buffer)
    {
      _buffer = buffer;
    }

    void parseDocument(DataList list, DataMap map) throws IOException
    {
      String name;
      _buffer.getInt();
      byte bsonType = _buffer.get();
      while (bsonType != ZERO_BYTE)
      {
        name = _buffer.getUtf8CString();
        Object o = null;
        boolean valid = true;
        switch (bsonType)
        {
          case BSON_EMBEDDED_DOCUMENT:
            DataMap childMap = new DataMap();
            updateParent(list, map, name, childMap);
            parseDocument(null, childMap);
            break;

          case BSON_ARRAY:
            DataList childList = new DataList();
            updateParent(list, map, name, childList);
            parseDocument(childList, null);
            break;

          case BSON_32BIT_INTEGER:
            o = _buffer.getInt();
            break;
          case BSON_DOUBLE:
            o = _buffer.getDouble();
            break;
          case BSON_STRING:
            o = getString();
            break;
          case BSON_BOOLEAN:
            byte b = _buffer.get();
            o = new Boolean(b != ZERO_BYTE);
            break;
          case BSON_64BIT_INTEGER:
            o = _buffer.getLong();
            break;

          case BSON_BINARY:
            int length = _buffer.getInt();
            _buffer.get();
            o = ByteString.read(_buffer.asInputStream(), length);
            break;
          case BSON_DEPRECATED:
          case BSON_OBJECTID:
            valid = false;
            break;
          case BSON_UTC_DATETIME:
          case BSON_TIMESTAMP:
            o = _buffer.getLong();
            break;
          case BSON_NULL:
            o = Data.NULL;
            break;
          case BSON_REGEX:
            valid = false;
            _buffer.getUtf8CString();
            _buffer.getUtf8CString();
            break;
          case BSON_DBPOINTER_DEPRECATED:
            o = getString();
            byte[] dbPointer = new byte[12];
            _buffer.get(dbPointer, 0, dbPointer.length);
            valid = false;
            break;
          case BSON_JAVASCRIPT_CODE:
          case BSON_SYMBOL:
            o = getString();
            break;
          case BSON_JAVASCRIPT_CODE_WITH_SCOPE:
            valid = false;
            break;
          default:
            valid = false;
            break;
        }
        if (valid == false)
        {
          throw new IOException("Illegal BSON element code " + bsonType);
        }
        if (o != null)
        {
          updateParent(list, map, name, o);
        }
        bsonType = _buffer.get();
      }
    }

    <T extends DataComplex> T parseComplex(Class<T> clazz) throws IOException
    {
      if (clazz == DataMap.class)
      {
        return clazz.cast(parseMap());
      }
      else if (clazz == DataList.class)
      {
        return clazz.cast(parseList());
      }
      else
      {
        throw new IllegalStateException("Unknown DataComplex class " + clazz.getName());
      }
    }

    DataMap parseMap() throws IOException
    {
      DataMap map = new DataMap();
      parseDocument(null, map);
      return map;
    }

    DataList parseList() throws IOException
    {
      DataList list = new DataList();
      parseDocument(list, null);
      return list;
    }

    private void updateParent(DataList parentList, DataMap parentMap, String name, Object value)
    {
      if (parentMap != null)
      {
        CheckedUtil.putWithoutChecking(parentMap, name, value);
      }
      else
      {
        CheckedUtil.addWithoutChecking(parentList, value);
      }
    }

    String getString() throws IOException
    {
      int length = _buffer.getInt();
      if (length == 0)
      {
        throw new DataDecodingException("String size should not be 0");
      }
      return _buffer.getUtf8CString(length);
    }

    private final BufferChain _buffer;
  }
}
