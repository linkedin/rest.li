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

package com.linkedin.data.codec;

import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.Data.TraverseCallback;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.protobuf.ProtoReader;
import com.linkedin.data.protobuf.ProtoWriter;
import com.linkedin.data.codec.symbol.SymbolTable;
import com.linkedin.data.collections.CheckedUtil;
import com.linkedin.util.FastByteArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;


/**
 * A codec that implements serialization and deserialization using Google Protocol Buffers.
 *
 * <p>This codec supports compacting strings (typically field names and enum constants) as integers using an
 * optional symbol table. </p>
 *
 * @author kramgopa
 */
public class ProtobufDataCodec implements DataCodec
{
  private static final byte MAP_ORDINAL = 0;
  private static final byte LIST_ORDINAL = 1;
  private static final byte STRING_LITERAL_ORDINAL = 2;
  private static final byte STRING_REFERENCE_ORDINAL = 3;
  private static final byte INTEGER_ORDINAL = 4;
  private static final byte LONG_ORDINAL = 5;
  private static final byte FLOAT_ORDINAL = 6;
  private static final byte DOUBLE_ORDINAL = 7;
  private static final byte BOOLEAN_TRUE_ORDINAL = 8;
  private static final byte BOOLEAN_FALSE_ORDINAL = 9;
  private static final byte RAW_BYTES_ORDINAL = 10;
  private static final byte NULL_ORDINAL = 11;

  private static final Map<Class<?>, Byte> ORDINAL_MAP = new HashMap<>();
  static {
    ORDINAL_MAP.put(DataMap.class, MAP_ORDINAL);
    ORDINAL_MAP.put(DataList.class, LIST_ORDINAL);
    ORDINAL_MAP.put(String.class, STRING_LITERAL_ORDINAL);
    ORDINAL_MAP.put(Integer.class, INTEGER_ORDINAL);
    ORDINAL_MAP.put(Long.class, LONG_ORDINAL);
    ORDINAL_MAP.put(Float.class, FLOAT_ORDINAL);
    ORDINAL_MAP.put(Double.class,DOUBLE_ORDINAL);
    ORDINAL_MAP.put(Boolean.class, BOOLEAN_FALSE_ORDINAL);
    ORDINAL_MAP.put(ByteString.class, RAW_BYTES_ORDINAL);
  }

  protected final SymbolTable _symbolTable;

  public ProtobufDataCodec()
  {
    this((SymbolTable) null);
  }

  public ProtobufDataCodec(SymbolTable symbolTable)
  {
    _symbolTable = symbolTable;
  }

  /**
   * @deprecated Use {@link #ProtobufDataCodec(SymbolTable)} instead. This constructor ignores its argument.
   */
  @Deprecated
  public ProtobufDataCodec(String symbolTableName)
  {
    this((SymbolTable) null);
  }

  @Override
  public byte[] mapToBytes(DataMap map) throws IOException
  {
    FastByteArrayOutputStream baos = new FastByteArrayOutputStream();
    writeMap(map, baos);
    return baos.toByteArray();
  }

  @Override
  public byte[] listToBytes(DataList list) throws IOException
  {
    FastByteArrayOutputStream baos = new FastByteArrayOutputStream();
    writeList(list, baos);
    return baos.toByteArray();
  }

  @Override
  public void writeMap(DataMap map, OutputStream out) throws IOException
  {
    ProtoWriter protoWriter = new ProtoWriter(out);
    TraverseCallback callback = createTraverseCallback(protoWriter, _symbolTable);
    Data.traverse(map, callback);
    protoWriter.flush();
  }

  @Override
  public void writeList(DataList list, OutputStream out) throws IOException
  {
    ProtoWriter protoWriter = new ProtoWriter(out);
    TraverseCallback callback = createTraverseCallback(protoWriter, _symbolTable);
    Data.traverse(list, callback);
    protoWriter.flush();
  }

  @Override
  public DataMap bytesToMap(byte[] input) throws IOException
  {
    return (DataMap) readValue(ProtoReader.newInstance(input), this::isMap);
  }

  @Override
  public DataList bytesToList(byte[] input) throws IOException
  {
    return (DataList) readValue(ProtoReader.newInstance(input), this::isList);
  }

  @Override
  public DataMap readMap(InputStream in) throws IOException
  {
    return (DataMap) readValue(ProtoReader.newInstance(in), this::isMap);
  }

  @Override
  public DataList readList(InputStream in) throws IOException
  {
    return (DataList) readValue(ProtoReader.newInstance(in), this::isList);
  }

  protected TraverseCallback createTraverseCallback(ProtoWriter protoWriter, SymbolTable symbolTable)
  {
    return new ProtobufTraverseCallback(protoWriter, symbolTable);
  }

  protected Object readUnknownValue(byte ordinal) throws IOException
  {
    throw new DataDecodingException("Unknown ordinal: " + ordinal);
  }

  private DataList readList(ProtoReader reader) throws IOException
  {
    int size = reader.readInt32();
    DataList dataList = new DataList(size);
    for (int i = 0; i < size; i++)
    {
      CheckedUtil.addWithoutChecking(dataList, readValue(reader, null));
    }

    return dataList;
  }

  private DataMap readMap(ProtoReader reader) throws IOException
  {
    int size = reader.readInt32();
    DataMap dataMap = new DataMap(size);
    for (int i = 0; i < size; i++)
    {
      CheckedUtil.putWithoutChecking(dataMap, (String) readValue(reader, this::isString), readValue(reader, null));
    }

    return dataMap;
  }

  private String readStringReference(ProtoReader reader) throws IOException
  {
    String value;
    if (_symbolTable == null || (value = _symbolTable.getSymbolName(reader.readInt32())) == null)
    {
      throw new DataDecodingException("Error decoding string reference");
    }
    return value;
  }

  private String readStringLiteral(ProtoReader reader) throws IOException
  {
    return reader.readString();
  }

  private Object readValue(ProtoReader reader, Function<Byte, Boolean> matcher) throws IOException
  {
    byte ordinal = reader.readRawByte();
    if (matcher != null && !matcher.apply(ordinal))
    {
      throw new DataDecodingException("Unable to find expected ordinal. Read: " + ordinal);
    }

    switch (ordinal)
    {
      case MAP_ORDINAL: return readMap(reader);
      case LIST_ORDINAL: return readList(reader);
      case STRING_LITERAL_ORDINAL: return readStringLiteral(reader);
      case STRING_REFERENCE_ORDINAL: return readStringReference(reader);
      case INTEGER_ORDINAL: return reader.readInt32();
      case LONG_ORDINAL: return reader.readInt64();
      case FLOAT_ORDINAL: return Float.intBitsToFloat(reader.readInt32());
      case DOUBLE_ORDINAL: return Double.longBitsToDouble(reader.readInt64());
      case BOOLEAN_TRUE_ORDINAL: return true;
      case BOOLEAN_FALSE_ORDINAL: return false;
      case RAW_BYTES_ORDINAL: return ByteString.unsafeWrap(reader.readByteArray());
      case NULL_ORDINAL: return Data.NULL;
    }

    return readUnknownValue(ordinal);
  }

  protected boolean isString(byte ordinal)
  {
    return ordinal == STRING_LITERAL_ORDINAL || ordinal == STRING_REFERENCE_ORDINAL;
  }

  protected boolean isList(byte ordinal)
  {
    return ordinal == LIST_ORDINAL;
  }

  protected boolean isMap(byte ordinal)
  {
    return ordinal == MAP_ORDINAL;
  }

  public static class ProtobufTraverseCallback implements TraverseCallback
  {
    protected final ProtoWriter _protoWriter;
    protected final SymbolTable _symbolTable;

    public ProtobufTraverseCallback(ProtoWriter protoWriter, SymbolTable symbolTable)
    {
      _protoWriter = protoWriter;
      _symbolTable = symbolTable;
    }

    public void nullValue() throws IOException
    {
      _protoWriter.writeByte(NULL_ORDINAL);
    }

    /**
     * Invoked when a boolean value is traversed.
     *
     * @param value the boolean value.
     */
    public void booleanValue(boolean value) throws IOException
    {
      if (value)
      {
        _protoWriter.writeByte(BOOLEAN_TRUE_ORDINAL);
      }
      else
      {
        _protoWriter.writeByte(BOOLEAN_FALSE_ORDINAL);
      }
    }

    /**
     * Invoked when a integer value is traversed.
     *
     * @param value the integer value.
     */
    public void integerValue(int value) throws IOException
    {
      _protoWriter.writeByte(INTEGER_ORDINAL);
      _protoWriter.writeInt32(value);
    }

    /**
     * Invoked when a long value is traversed.
     *
     * @param value the long value.
     */
    public void longValue(long value) throws IOException
    {
      _protoWriter.writeByte(LONG_ORDINAL);
      _protoWriter.writeInt64(value);
    }

    /**
     * Invoked when a float value is traversed.
     *
     * @param value the float value.
     */
    public void floatValue(float value) throws IOException
    {
      _protoWriter.writeByte(FLOAT_ORDINAL);
      _protoWriter.writeInt32(Float.floatToIntBits(value));
    }

    /**
     * Invoked when a double value is traversed.
     *
     * @param value the double value.
     */
    public void doubleValue(double value) throws IOException
    {
      _protoWriter.writeByte(DOUBLE_ORDINAL);
      _protoWriter.writeInt64(Double.doubleToLongBits(value));
    }

    /**
     * Invoked when a string value is traversed.
     *
     * @param value the string value.
     */
    public void stringValue(String value) throws IOException
    {
      int symbolId;
      if (_symbolTable != null && (symbolId = _symbolTable.getSymbolId(value)) != SymbolTable.UNKNOWN_SYMBOL_ID)
      {
        _protoWriter.writeByte(STRING_REFERENCE_ORDINAL);
        _protoWriter.writeUInt32(symbolId);
      }
      else
      {
        _protoWriter.writeByte(STRING_LITERAL_ORDINAL);
        _protoWriter.writeString(value);
      }
    }

    /**
     * Invoked when a {@link ByteString} value is traversed.
     *
     * @param value the string value.
     */
    public void byteStringValue(ByteString value) throws IOException
    {
      _protoWriter.writeByte(RAW_BYTES_ORDINAL);
      value.write(_protoWriter);
    }

    /**
     * Invoked when an illegal value is traversed.
     * This occurs when the value's type is not one of the allowed types.
     *
     * @param value the illegal value.
     */
    public void illegalValue(Object value) throws IOException
    {
      throw new DataEncodingException("Illegal value encountered: " + value);
    }

    /**
     * Invoked when an empty {@link DataMap} is traversed.
     * The {@link #startMap}, {@link #key(String)}, various value,
     * and {@link #endMap} callbacks will not
     * be invoked for an empty {@link DataMap}.
     */
    public void emptyMap() throws IOException
    {
      _protoWriter.writeByte(MAP_ORDINAL);
      _protoWriter.writeUInt32(0);
    }

    /**
     * Invoked when the start of {@link DataMap} is traversed.
     *
     * @param map provides the {@link DataMap}to be traversed.
     */
    public void startMap(DataMap map) throws IOException
    {
      _protoWriter.writeByte(MAP_ORDINAL);
      _protoWriter.writeUInt32(map.size());
    }

    /**
     * Invoked when the key of {@link DataMap} entry is traversed.
     * This callback is invoked before the value callback.
     *
     * @param key of the {@link DataMap} entry.
     */
    public void key(String key) throws IOException
    {
      stringValue(key);
    }

    /**
     * Invoked when an empty list is traversed.
     * There {@link #startList}, {@link #index(int)}, various value, and
     * {@link #endList} callbacks will not
     * be invoked for an empty {@link DataList}.
     */
    public void emptyList() throws IOException
    {
      _protoWriter.writeByte(LIST_ORDINAL);
      _protoWriter.writeUInt32(0);
    }

    /**
     * Invoked when the start of a {@link DataList} is traversed.
     *
     * @param list provides the {@link DataList}to be traversed.
     */
    public void startList(DataList list) throws IOException
    {
      _protoWriter.writeByte(LIST_ORDINAL);
      _protoWriter.writeUInt32(list.size());
    }
  }
}