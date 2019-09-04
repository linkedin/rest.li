package com.linkedin.data.codec;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.Data.TraverseCallback;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.symbol.SymbolTable;
import com.linkedin.data.codec.symbol.SymbolTableProvider;
import com.linkedin.util.FastByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
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
  private static volatile SymbolTableProvider _symbolTableProvider;

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

  /**
   * Set the symbol table provider. This will be used by all codec instances.
   *
   * <p>It is the responsibility of the application to set this provider if it wants shared symbol
   * tables to be used.</p>
   *
   * @param symbolTableProvider The provider to set.
   */
  public static void setSymbolTableProvider(SymbolTableProvider symbolTableProvider)
  {
    _symbolTableProvider = symbolTableProvider;
  }

  public ProtobufDataCodec()
  {
    _symbolTable = null;
  }

  public ProtobufDataCodec(String symbolTableName)
  {
    if (symbolTableName != null && _symbolTableProvider != null)
    {
      _symbolTable = _symbolTableProvider.getSymbolTable(symbolTableName);
    }
    else
    {
      _symbolTable = null;
    }
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
    CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(out);
    TraverseCallback callback = createTraverseCallback(codedOutputStream, _symbolTable);
    Data.traverse(map, callback);
    codedOutputStream.flush();
  }

  @Override
  public void writeList(DataList list, OutputStream out) throws IOException
  {
    CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(out);
    TraverseCallback callback = createTraverseCallback(codedOutputStream, _symbolTable);
    Data.traverse(list, callback);
    codedOutputStream.flush();
  }

  @Override
  public DataMap bytesToMap(byte[] input) throws IOException
  {
    return (DataMap) readValue(CodedInputStream.newInstance(input), this::isMap);
  }

  @Override
  public DataList bytesToList(byte[] input) throws IOException
  {
    return (DataList) readValue(CodedInputStream.newInstance(input), this::isList);
  }

  @Override
  public DataMap readMap(InputStream in) throws IOException
  {
    return (DataMap) readValue(CodedInputStream.newInstance(in), this::isMap);
  }

  @Override
  public DataList readList(InputStream in) throws IOException
  {
    return (DataList) readValue(CodedInputStream.newInstance(in), this::isList);
  }

  protected TraverseCallback createTraverseCallback(CodedOutputStream outputStream, SymbolTable symbolTable)
  {
    return new ProtobufTraverseCallback(outputStream, symbolTable);
  }

  protected Object readUnknownValue(byte ordinal) throws IOException
  {
    return null;
  }

  private DataList readList(CodedInputStream in) throws IOException
  {
    int size = in.readRawVarint32();
    DataList dataList = new DataList(size);
    List<Object> underlying = dataList.getUnderlying();
    for (int i = 0; i < size; i++)
    {
      underlying.add(readValue(in, null));
    }

    return dataList;
  }

  private DataMap readMap(CodedInputStream in) throws IOException
  {
    int size = in.readRawVarint32();
    DataMap dataMap = new DataMap(size);
    Map<String, Object>  underlying = dataMap.getUnderlying();
    for (int i = 0; i < size; i++)
    {
      underlying.put((String) readValue(in, this::isString), readValue(in, null));
    }

    return dataMap;
  }

  private String readStringReference(CodedInputStream in) throws IOException
  {
    String value;
    if (_symbolTable == null || (value = _symbolTable.getSymbolName(in.readRawVarint32())) == null)
    {
      throw new DataDecodingException("Error decoding string reference");
    }
    return value;
  }

  private String readStringLiteral(CodedInputStream in) throws IOException
  {
    return in.readString();
  }

  private Object readValue(CodedInputStream in, Function<Byte, Boolean> matcher) throws IOException
  {
    byte ordinal = in.readRawByte();
    if (matcher != null && !matcher.apply(ordinal))
    {
      throw new DataDecodingException("Unable to find expected ordinal. Read: " + ordinal);
    }

    switch (ordinal)
    {
      case MAP_ORDINAL: return readMap(in);
      case LIST_ORDINAL: return readList(in);
      case STRING_LITERAL_ORDINAL: return readStringLiteral(in);
      case STRING_REFERENCE_ORDINAL: return readStringReference(in);
      case INTEGER_ORDINAL: return in.readRawVarint32();
      case LONG_ORDINAL: return in.readRawVarint64();
      case FLOAT_ORDINAL: return Float.intBitsToFloat(in.readRawVarint32());
      case DOUBLE_ORDINAL: return Double.longBitsToDouble(in.readRawVarint64());
      case BOOLEAN_TRUE_ORDINAL: return true;
      case BOOLEAN_FALSE_ORDINAL: return false;
      case RAW_BYTES_ORDINAL: return ByteString.unsafeWrap(in.readByteArray());
      case NULL_ORDINAL: return Data.NULL;
    }

    Object unknownValue = readUnknownValue(ordinal);
    if (unknownValue != null)
    {
      return unknownValue;
    }

    throw new DataDecodingException("Unknown ordinal: " + ordinal);
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
    protected final CodedOutputStream _codedOutputStream;
    protected final SymbolTable _symbolTable;

    public ProtobufTraverseCallback(CodedOutputStream codedOutputStream, SymbolTable symbolTable)
    {
      _codedOutputStream = codedOutputStream;
      _symbolTable = symbolTable;
    }

    public void nullValue() throws IOException
    {
      _codedOutputStream.writeRawByte(NULL_ORDINAL);
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
        _codedOutputStream.writeRawByte(BOOLEAN_TRUE_ORDINAL);
      }
      else
      {
        _codedOutputStream.writeRawByte(BOOLEAN_FALSE_ORDINAL);
      }
    }

    /**
     * Invoked when a integer value is traversed.
     *
     * @param value the integer value.
     */
    public void integerValue(int value) throws IOException
    {
      _codedOutputStream.writeRawByte(INTEGER_ORDINAL);
      _codedOutputStream.writeInt32NoTag(value);
    }

    /**
     * Invoked when a long value is traversed.
     *
     * @param value the long value.
     */
    public void longValue(long value) throws IOException
    {
      _codedOutputStream.writeRawByte(LONG_ORDINAL);
      _codedOutputStream.writeInt64NoTag(value);
    }

    /**
     * Invoked when a float value is traversed.
     *
     * @param value the float value.
     */
    public void floatValue(float value) throws IOException
    {
      _codedOutputStream.writeRawByte(FLOAT_ORDINAL);
      _codedOutputStream.writeInt32NoTag(Float.floatToIntBits(value));
    }

    /**
     * Invoked when a double value is traversed.
     *
     * @param value the double value.
     */
    public void doubleValue(double value) throws IOException
    {
      _codedOutputStream.writeRawByte(DOUBLE_ORDINAL);
      _codedOutputStream.writeInt64NoTag(Double.doubleToLongBits(value));
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
        _codedOutputStream.writeRawByte(STRING_REFERENCE_ORDINAL);
        _codedOutputStream.writeUInt32NoTag(symbolId);
      }
      else
      {
        _codedOutputStream.writeRawByte(STRING_LITERAL_ORDINAL);
        _codedOutputStream.writeStringNoTag(value);
      }
    }

    /**
     * Invoked when a {@link ByteString} value is traversed.
     *
     * @param value the string value.
     */
    public void byteStringValue(ByteString value) throws IOException
    {
      _codedOutputStream.writeRawByte(RAW_BYTES_ORDINAL);
      value.write(_codedOutputStream);
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
      _codedOutputStream.writeRawByte(MAP_ORDINAL);
      _codedOutputStream.writeUInt32NoTag(0);
    }

    /**
     * Invoked when the start of {@link DataMap} is traversed.
     *
     * @param map provides the {@link DataMap}to be traversed.
     */
    public void startMap(DataMap map) throws IOException
    {
      _codedOutputStream.writeRawByte(MAP_ORDINAL);
      _codedOutputStream.writeUInt32NoTag(map.size());
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
      _codedOutputStream.writeRawByte(LIST_ORDINAL);
      _codedOutputStream.writeUInt32NoTag(0);
    }

    /**
     * Invoked when the start of a {@link DataList} is traversed.
     *
     * @param list provides the {@link DataList}to be traversed.
     */
    public void startList(DataList list) throws IOException
    {
      _codedOutputStream.writeRawByte(LIST_ORDINAL);
      _codedOutputStream.writeUInt32NoTag(list.size());
    }
  }
}
