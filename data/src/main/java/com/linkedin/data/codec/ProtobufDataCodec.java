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
import com.linkedin.data.DataMapBuilder;
import com.linkedin.data.codec.symbol.SymbolTable;
import com.linkedin.data.collections.CheckedUtil;
import com.linkedin.data.protobuf.ProtoReader;
import com.linkedin.data.protobuf.ProtoWriter;
import com.linkedin.util.FastByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
  //
  // List of protobuf type ordinals. 12-19 and 30-127 are reserved for custom ordinals that extenders of this codec
  // may choose to use. 0-11 and 20-29 are reserved for use by this codec.
  //

  public static final byte MAP_ORDINAL = 0;
  public static final byte LIST_ORDINAL = 1;
  public static final byte STRING_LITERAL_ORDINAL = 2;
  public static final byte STRING_REFERENCE_ORDINAL = 3;
  public static final byte INTEGER_ORDINAL = 4;
  public static final byte LONG_ORDINAL = 5;
  public static final byte FLOAT_ORDINAL = 6;
  public static final byte DOUBLE_ORDINAL = 7;
  public static final byte BOOLEAN_TRUE_ORDINAL = 8;
  public static final byte BOOLEAN_FALSE_ORDINAL = 9;
  public static final byte RAW_BYTES_ORDINAL = 10;
  public static final byte NULL_ORDINAL = 11;
  public static final byte ASCII_STRING_LITERAL_ORDINAL = 20;
  public static final byte FIXED_FLOAT_ORDINAL = 21;
  public static final byte FIXED_DOUBLE_ORDINAL = 22;

  /**
   * @deprecated Use {@link #_options} and invoke {@link ProtobufCodecOptions#getSymbolTable()} instead.
   */
  @Deprecated
  protected final SymbolTable _symbolTable;

  /**
   * @deprecated Use {@link #_options} and invoke {@link ProtobufCodecOptions#shouldEnableASCIIOnlyStrings()} instead.
   */
  @Deprecated
  protected final boolean _supportsASCIIOnlyStrings;

  protected final ProtobufCodecOptions _options;

  public ProtobufDataCodec()
  {
    this(new ProtobufCodecOptions.Builder().build());
  }

  /**
   * @deprecated Use {@link ProtobufDataCodec#ProtobufDataCodec(ProtobufCodecOptions)} instead. This constructor
   * ignores its argument.
   */
  @Deprecated
  public ProtobufDataCodec(String symbolTableName)
  {
    this(new ProtobufCodecOptions.Builder().build());
  }

  /**
   * @deprecated Use {@link ProtobufDataCodec#ProtobufDataCodec(ProtobufCodecOptions)} instead.
   */
  @Deprecated
  public ProtobufDataCodec(SymbolTable symbolTable)
  {
    this(new ProtobufCodecOptions.Builder().setSymbolTable(symbolTable).build());
  }

  /**
   * @deprecated Use {@link ProtobufDataCodec#ProtobufDataCodec(ProtobufCodecOptions)} instead.
   */
  @Deprecated
  public ProtobufDataCodec(SymbolTable symbolTable, boolean supportsASCIIOnlyStrings)
  {
    this(new ProtobufCodecOptions.Builder()
        .setSymbolTable(symbolTable)
        .setEnableASCIIOnlyStrings(supportsASCIIOnlyStrings)
        .build());
  }

  public ProtobufDataCodec(ProtobufCodecOptions options)
  {
    _options = options;
    _symbolTable = options.getSymbolTable();
    _supportsASCIIOnlyStrings = options.shouldEnableASCIIOnlyStrings();
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
    try(TraverseCallback callback = createTraverseCallback(new ProtoWriter(out)))
    {
      Data.traverse(map, callback);
    }
  }

  @Override
  public void writeList(DataList list, OutputStream out) throws IOException
  {
    try(TraverseCallback callback = createTraverseCallback(new ProtoWriter(out)))
    {
      Data.traverse(list, callback);
    }
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
    try
    {
      return (DataMap) readValue(ProtoReader.newInstance(in), this::isMap);
    }
    finally
    {
      DataCodec.closeQuietly(in);
    }
  }

  @Override
  public DataList readList(InputStream in) throws IOException
  {
    try
    {
      return (DataList) readValue(ProtoReader.newInstance(in), this::isList);
    }
    finally
    {
      DataCodec.closeQuietly(in);
    }
  }

  @Override
  public DataMap readMap(ByteString in) throws IOException
  {
    return (DataMap) readValue(in.asProtoReader(), this::isMap);
  }

  @Override
  public DataList readList(ByteString in) throws IOException
  {
    return (DataList) readValue(in.asProtoReader(), this::isList);
  }

  /**
   * @deprecated Override {@link #createTraverseCallback(ProtoWriter)} instead. This method
   * is no longer invoked by this class.
   */
  @Deprecated
  protected TraverseCallback createTraverseCallback(ProtoWriter protoWriter, SymbolTable symbolTable)
  {
    return new ProtobufTraverseCallback(protoWriter,
        new ProtobufCodecOptions.Builder().setSymbolTable(symbolTable).build());
  }

  protected TraverseCallback createTraverseCallback(ProtoWriter protoWriter)
  {
    return new ProtobufTraverseCallback(protoWriter, _options);
  }

  protected Object readUnknownValue(byte ordinal, ProtoReader reader) throws IOException
  {
    throw new DataDecodingException("Unknown ordinal: " + ordinal);
  }

  protected final DataList readList(ProtoReader reader) throws IOException
  {
    int size = reader.readInt32();
    DataList dataList = new DataList(size);
    for (int i = 0; i < size; i++)
    {
      CheckedUtil.addWithoutChecking(dataList, readValue(reader, null));
    }

    return dataList;
  }

  protected final DataMap readMap(ProtoReader reader) throws IOException
  {
    int size = reader.readInt32();
    DataMap dataMap = new DataMap(DataMapBuilder.getOptimumHashMapCapacityFromSize(size));
    for (int i = 0; i < size; i++)
    {
      CheckedUtil.putWithoutChecking(dataMap, (String) readValue(reader, this::isString), readValue(reader, null));
    }

    return dataMap;
  }

  protected final String readStringReference(ProtoReader reader) throws IOException
  {
    String value;
    if ((value = _options.getSymbolTable().getSymbolName(reader.readInt32())) == null)
    {
      throw new DataDecodingException("Error decoding string reference");
    }
    return value;
  }

  protected final String readASCIIStringLiteral(ProtoReader reader) throws IOException
  {
    return reader.readASCIIString();
  }

  protected final String readStringLiteral(ProtoReader reader) throws IOException
  {
    return reader.readString();
  }

  protected final Object readValue(ProtoReader reader, Function<Byte, Boolean> matcher) throws IOException
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
      case ASCII_STRING_LITERAL_ORDINAL: return readASCIIStringLiteral(reader);
      case STRING_LITERAL_ORDINAL: return readStringLiteral(reader);
      case STRING_REFERENCE_ORDINAL: return readStringReference(reader);
      case INTEGER_ORDINAL: return reader.readInt32();
      case LONG_ORDINAL: return reader.readInt64();
      case FLOAT_ORDINAL: return Float.intBitsToFloat(reader.readInt32());
      case FIXED_FLOAT_ORDINAL: return Float.intBitsToFloat(reader.readFixedInt32());
      case DOUBLE_ORDINAL: return Double.longBitsToDouble(reader.readInt64());
      case FIXED_DOUBLE_ORDINAL: return Double.longBitsToDouble(reader.readFixedInt64());
      case BOOLEAN_TRUE_ORDINAL: return true;
      case BOOLEAN_FALSE_ORDINAL: return false;
      case RAW_BYTES_ORDINAL: return ByteString.unsafeWrap(reader.readByteArray());
      case NULL_ORDINAL: return Data.NULL;
    }

    return readUnknownValue(ordinal, reader);
  }

  protected boolean isString(byte ordinal)
  {
    return ordinal == STRING_LITERAL_ORDINAL
        || ordinal == ASCII_STRING_LITERAL_ORDINAL
        || ordinal == STRING_REFERENCE_ORDINAL;
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

    /**
     * @deprecated Use {@link #_options} and invoke {@link ProtobufCodecOptions#getSymbolTable()} instead.
     */
    @Deprecated
    protected final SymbolTable _symbolTable;

    /**
     * @deprecated Use {@link #_options} and invoke {@link ProtobufCodecOptions#shouldEnableASCIIOnlyStrings()} instead.
     */
    @Deprecated
    protected final boolean _supportsASCIIOnlyStrings;

    protected final ProtobufCodecOptions _options;

    /**
     * @deprecated Use {@link ProtobufTraverseCallback#ProtobufTraverseCallback(ProtoWriter, ProtobufCodecOptions)}
     * instead.
     */
    @Deprecated
    public ProtobufTraverseCallback(ProtoWriter protoWriter, SymbolTable symbolTable)
    {
      this(protoWriter, new ProtobufCodecOptions.Builder().setSymbolTable(symbolTable).build());
    }

    /**
     * @deprecated Use {@link ProtobufTraverseCallback#ProtobufTraverseCallback(ProtoWriter, ProtobufCodecOptions)}
     * instead.
     */
    @Deprecated
    public ProtobufTraverseCallback(ProtoWriter protoWriter, SymbolTable symbolTable, boolean supportsASCIIOnlyStrings)
    {
      this(protoWriter, new ProtobufCodecOptions.Builder()
          .setSymbolTable(symbolTable)
          .setEnableASCIIOnlyStrings(supportsASCIIOnlyStrings)
          .build());
    }

    public ProtobufTraverseCallback(ProtoWriter protoWriter, ProtobufCodecOptions options)
    {
      _protoWriter = protoWriter;
      _options = options;
      _symbolTable = options.getSymbolTable();
      _supportsASCIIOnlyStrings = options.shouldEnableASCIIOnlyStrings();
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
      if (_options.shouldEnableFixedLengthFloatDoubles())
      {
        _protoWriter.writeByte(FIXED_FLOAT_ORDINAL);
        _protoWriter.writeFixedInt32(Float.floatToRawIntBits(value));
      }
      else
      {
        _protoWriter.writeByte(FLOAT_ORDINAL);
        _protoWriter.writeInt32(Float.floatToRawIntBits(value));
      }
    }

    /**
     * Invoked when a double value is traversed.
     *
     * @param value the double value.
     */
    public void doubleValue(double value) throws IOException
    {
      if (_options.shouldEnableFixedLengthFloatDoubles())
      {
        _protoWriter.writeByte(FIXED_DOUBLE_ORDINAL);
        _protoWriter.writeFixedInt64(Double.doubleToRawLongBits(value));
      }
      else
      {
        _protoWriter.writeByte(DOUBLE_ORDINAL);
        _protoWriter.writeInt64(Double.doubleToRawLongBits(value));
      }
    }

    /**
     * Invoked when a string value is traversed.
     *
     * @param value the string value.
     */
    public void stringValue(String value) throws IOException
    {
      int symbolId;
      if ((symbolId = _options.getSymbolTable().getSymbolId(value)) != SymbolTable.UNKNOWN_SYMBOL_ID)
      {
        _protoWriter.writeByte(STRING_REFERENCE_ORDINAL);
        _protoWriter.writeUInt32(symbolId);
      }
      else
      {
        // If the byte length is the same as the string length, then this is an ASCII-only string.
        _protoWriter.writeString(value, byteLength ->
            (_options.shouldEnableASCIIOnlyStrings() && value.length() == byteLength) ?
                ASCII_STRING_LITERAL_ORDINAL : STRING_LITERAL_ORDINAL);
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

    @Override
    public void close() throws IOException
    {
      _protoWriter.close();
    }
  }
}