/*
   Copyright (c) 2018 LinkedIn Corp.

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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.DataMapBuilder;
import com.linkedin.data.collections.CheckedUtil;
import com.linkedin.util.FastByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * Abstract class for JSON and JSON-like formats serialized and deserialized using Jackson.
 *
 * <p>The codec itself doesn't keep state during parsing/generation. Once properly initialized, it's safe to use
 * the same instance of the codec concurrently.</p>
 *
 * @author kramgopa, slim
 */
public abstract class AbstractJacksonDataCodec implements DataCodec
{
  protected static final int DEFAULT_BUFFER_SIZE = 4096;

  protected final JsonFactory _factory;

  protected AbstractJacksonDataCodec(JsonFactory factory)
  {
    _factory = factory;
  }

  @Override
  public byte[] mapToBytes(DataMap map) throws IOException
  {
    return objectToBytes(map);
  }

  @Override
  public byte[] listToBytes(DataList list) throws IOException
  {
    return objectToBytes(list);
  }

  protected byte[] objectToBytes(Object object) throws IOException
  {
    FastByteArrayOutputStream out = new FastByteArrayOutputStream(DEFAULT_BUFFER_SIZE);
    writeObject(object, createJsonGenerator(out));
    return out.toByteArray();
  }

  @Override
  public DataMap bytesToMap(byte[] input) throws IOException
  {
    return parse(_factory.createParser(input), DataMap.class);
  }

  @Override
  public DataList bytesToList(byte[] input) throws IOException
  {
    return parse(_factory.createParser(input), DataList.class);
  }

  @Override
  public void writeMap(DataMap map, OutputStream out) throws IOException
  {
    writeObject(map, createJsonGenerator(out));
  }

  @Override
  public void writeList(DataList list, OutputStream out) throws IOException
  {
    writeObject(list, createJsonGenerator(out));
  }

  protected JsonGenerator createJsonGenerator(OutputStream out) throws IOException
  {
    return _factory.createGenerator(out);
  }

  protected JsonGenerator createJsonGenerator(Writer out) throws IOException
  {
    return _factory.createGenerator(out);
  }

  protected void writeObject(Object object, JsonGenerator generator) throws IOException
  {
    try (Data.TraverseCallback callback = createTraverseCallback(generator))
    {
      Data.traverse(object, callback);
    }
  }

  protected Data.TraverseCallback createTraverseCallback(JsonGenerator generator)
  {
    return createTraverseCallback(generator, false);
  }

  /**
   * Create {@link Data.TraverseCallback} interface instance for Data object traverse
   * @param generator JsonGenerator used during traverse
   * @param traverseMapBySortedKeyOrder indicate whether want the callBack to traverse the data map within data object using the sorted map key order
   * @return
   */
  protected Data.TraverseCallback createTraverseCallback(JsonGenerator generator, boolean traverseMapBySortedKeyOrder)
  {
    return new JacksonTraverseCallback(generator, traverseMapBySortedKeyOrder);
  }

  @Override
  public DataMap readMap(InputStream in) throws IOException
  {
    return parse(_factory.createParser(in), DataMap.class);
  }

  @Override
  public DataList readList(InputStream in) throws IOException
  {
    return parse(_factory.createParser(in), DataList.class);
  }

  protected <T extends DataComplex> T parse(JsonParser jsonParser, Class<T> expectType) throws IOException
  {
    try
    {
      return new Parser().parse(jsonParser, expectType);
    }
    catch (IOException e)
    {
      throw e;
    }
    finally
    {
      DataCodec.closeQuietly(jsonParser);
    }
  }

  /**
   * Uses the {@link JsonParser} and parses its contents into a list of Data objects.
   *
   * @param jsonParser provides the {@link JsonParser}
   * @param mesg provides the {@link StringBuilder} to store validation error messages,
   *             such as duplicate keys in the same {@link DataMap}.
   * @param locationMap provides where to store the mapping of a Data object
   *                    to its location in the in the source backing the {@link JsonParser}. may be
   *                    {@code null} if this mapping is not needed by the caller.
   *                    This map should usually be an {@link IdentityHashMap}.
   * @return the list of Data objects parsed from the {@link JsonParser}.
   * @throws IOException if there is a syntax error in the input.
   */
  protected List<Object> parse(JsonParser jsonParser, StringBuilder mesg, Map<Object, DataLocation> locationMap)
    throws IOException
  {
    try
    {
      return new Parser(true).parse(jsonParser, mesg, locationMap);
    }
    catch (IOException e)
    {
      throw e;
    }
    finally
    {
      DataCodec.closeQuietly(jsonParser);
    }
  }

  /**
   * Reads an {@link InputStream} and parses its contents into a list of Data objects.
   *
   * @param in provides the {@link InputStream}
   * @param mesg provides the {@link StringBuilder} to store validation error messages,
   *             such as duplicate keys in the same {@link DataMap}.
   * @param locationMap provides where to store the mapping of a Data object
   *                    to its location in the in the {@link InputStream}. may be
   *                    {@code null} if this mapping is not needed by the caller.
   *                    This map should usually be an {@link IdentityHashMap}.
   * @return the list of Data objects parsed from the {@link InputStream}.
   * @throws IOException if there is a syntax error in the input.
   */
  public List<Object> parse(InputStream in, StringBuilder mesg, Map<Object, DataLocation> locationMap)
      throws IOException
  {
    return parse(_factory.createParser(in), mesg, locationMap);
  }

  public void objectToJsonGenerator(Object object, JsonGenerator generator) throws IOException
  {
    objectToJsonGenerator(object, generator, false);
  }

  /**
   * Convert an object to Json format representation.
   * @param object the object that needs to be converted
   * @param generator the generator that could generate Json output based on the object
   * @param orderMapByKey if true, map elements in the object (can be the object itself or its nested elements)
   *                      will have their entries sorted by keys in the generated Json.
   * @throws IOException
   */
  public void objectToJsonGenerator(Object object, JsonGenerator generator, boolean orderMapByKey) throws IOException
  {
    Data.TraverseCallback callback = createTraverseCallback(generator, orderMapByKey);
    Data.traverse(object, callback);
  }

  public static class JacksonTraverseCallback implements Data.TraverseCallback
  {
    protected final JsonGenerator _generator;
    private final boolean _orderMapEntriesByKey;

    protected JacksonTraverseCallback(JsonGenerator generator)
    {
      this(generator, false);
    }

    protected JacksonTraverseCallback(JsonGenerator generator, boolean orderMapEntriesByKey)
    {
      _generator = generator;
      _orderMapEntriesByKey = orderMapEntriesByKey;
    }

    @Override
    public void nullValue() throws IOException
    {
      _generator.writeNull();
    }

    @Override
    public void booleanValue(boolean value) throws IOException
    {
      _generator.writeBoolean(value);
    }

    @Override
    public void integerValue(int value) throws IOException
    {
      _generator.writeNumber(value);
    }

    @Override
    public void longValue(long value) throws IOException
    {
      _generator.writeNumber(value);
    }

    @Override
    public void floatValue(float value) throws IOException
    {
      _generator.writeNumber(value);
    }

    @Override
    public void doubleValue(double value) throws IOException
    {
      _generator.writeNumber(value);
    }

    @Override
    public void stringValue(String value) throws IOException
    {
      _generator.writeString(value);
    }

    @Override
    public void byteStringValue(ByteString value) throws IOException
    {
      char[] avroCharArray = value.asAvroCharArray();
      _generator.writeString(avroCharArray, 0, avroCharArray.length);
    }

    @Override
    public void illegalValue(Object value) throws DataEncodingException
    {
      throw new DataEncodingException("Illegal value encountered: " + value);
    }

    @Override
    public void emptyMap() throws IOException
    {
      _generator.writeStartObject();
      _generator.writeEndObject();
    }

    @Override
    public void startMap(DataMap map) throws IOException
    {
      _generator.writeStartObject();
    }

    @Override
    public void key(String key) throws IOException
    {
      _generator.writeFieldName(key);
    }

    @Override
    public Iterable<Map.Entry<String, Object>> orderMap(DataMap map)
    {
      if (_orderMapEntriesByKey)
      {
        return Data.orderMapEntries(map);
      }
      else
      {
        return map.entrySet();
      }
    }

    @Override
    public void endMap() throws IOException
    {
      _generator.writeEndObject();
    }

    @Override
    public void emptyList() throws IOException
    {
      _generator.writeStartArray();
      _generator.writeEndArray();
    }

    @Override
    public void startList(DataList list) throws IOException
    {
      _generator.writeStartArray();
    }

    @Override
    public void index(int index)
    {
    }

    @Override
    public void endList() throws IOException
    {
      _generator.writeEndArray();
    }

    @Override
    public void close() throws IOException
    {
      _generator.flush();
      _generator.close();
    }
  }

  private static class Parser
  {
    /**
     * The Depth of our DataMap recursion (also the size of the HashMap),
     * after which we no longer see gains from instantiating a smaller HashMap.
     *
     * This is based on the fact that default DataMaps instantiate to size 16,
     * with load factor 0.75 and capacity will always be a power of 2.
     * When you add the 7th entry, the capacity will grow to 16 from 8, so 6 is Max Recursive Depth
     */
    private static final int MAX_DATA_MAP_RECURSION_SIZE = 6;

    private StringBuilder _errorBuilder = null;
    private JsonParser _parser = null;
    private boolean _debug = false;
    private Deque<Object> _nameStack = null;
    private Map<Object, DataLocation> _locationMap = null;

    Parser()
    {
      this(false);
    }

    Parser(boolean debug)
    {
      _debug = debug;
    }

    /**
     * Returns map of location to object, sorted by location.
     *
     * May be used to debug location map.
     */
    private Map<DataLocation, Object> sortedLocationsMap()
    {
      if (_locationMap == null)
      {
        return null;
      }

      TreeMap<DataLocation, Object> sortedMap = new TreeMap<DataLocation, Object>();
      for (Map.Entry<Object, DataLocation> e : _locationMap.entrySet())
      {
        sortedMap.put(e.getValue(), e.getKey());
      }
      return sortedMap;
    }

    List<Object> parse(JsonParser parser, StringBuilder mesg, Map<Object, DataLocation> locationMap)
        throws IOException
    {
      _locationMap = locationMap;

      DataList list = new DataList();
      _errorBuilder = mesg;
      if (_debug)
      {
        _nameStack = new ArrayDeque<>();
      }

      _parser = parser;
      JsonToken token;
      while ((token = _parser.nextToken()) != null)
      {
        parse(list, null, token);
      }
      _errorBuilder = null;

      return list;
    }

    <T extends DataComplex> T parse(JsonParser parser, Class<T> expectType) throws IOException
    {
      _errorBuilder = null;
      if (_debug)
      {
        _nameStack = new ArrayDeque<>();
      }

      _parser = parser;
      final JsonToken token = _parser.nextToken();
      final T result;
      if (expectType == DataMap.class)
      {
        if (!JsonToken.START_OBJECT.equals(token))
        {
          throw new DataDecodingException("Object must start with start object token.");
        }

        final DataMap map = parseDataMap();
        if (_errorBuilder != null)
        {
          map.addError(_errorBuilder.toString());
        }
        result = expectType.cast(map);
      }
      else if (expectType == DataList.class)
      {
        if (!JsonToken.START_ARRAY.equals(token))
        {
          throw new DataDecodingException("Array must start with start object token.");
        }

        final DataList list = parseDataList();
        if (_errorBuilder != null)
        {
          //list.addError(_errorBuilder.toString());
        }
        result = expectType.cast(list);
      }
      else
      {
        throw new DataDecodingException("Expected type must be either DataMap or DataList.");
      }

      return result;
    }

    private DataLocation currentDataLocation()
    {
      return _locationMap == null ? null : new Location(_parser.getTokenLocation());
    }

    private void saveDataLocation(Object o, DataLocation location)
    {
      if (_locationMap != null && o != null)
      {
        assert(location != null);
        _locationMap.put(o, location);
      }
    }

    private Object parse(DataComplex parent, String name, JsonToken token) throws IOException
    {
      return parse(parent, name, token, true);
    }

    private Object parse(JsonToken token) throws IOException
    {
      return parse(null, null, token, false);
    }

    private Object parse(DataComplex parent, String name, JsonToken token, boolean shouldUpdateParent) throws IOException
    {
      if (token == null)
      {
        throw new DataDecodingException("Missing token");
      }
      Object value;
      DataLocation location = currentDataLocation();
      switch (token)
      {
        case START_OBJECT:
          value = parseDataMap();
          if (shouldUpdateParent)
          {
            updateParent(parent, name, value);
          }
          break;
        case START_ARRAY:
          value = parseDataList();
          if (shouldUpdateParent)
          {
            updateParent(parent, name, value);
          }
          break;
        default:
          value = parsePrimitive(token);
          if (value != null && shouldUpdateParent)
          {
            updateParent(parent, name, value);
          }
          break;
      }
      saveDataLocation(value, location);
      return value;
    }

    private void updateParent(DataComplex parent, String name, Object value)
    {
      if (parent instanceof DataMap)
      {
        Object replaced = CheckedUtil.putWithoutChecking((DataMap) parent, name, value);
        if (replaced != null)
        {
          if (_errorBuilder == null)
          {
            _errorBuilder = new StringBuilder();
          }
          _errorBuilder.append(new Location(_parser.getTokenLocation())).append(": \"").append(name).append("\" defined more than once.\n");
        }
      }
      else
      {
        CheckedUtil.addWithoutChecking((DataList) parent, value);
      }
    }

    private Object parsePrimitive(JsonToken token) throws IOException
    {
      Object object;
      JsonParser.NumberType numberType;
      switch (token) {
        case VALUE_STRING:
          object = _parser.getText();
          break;
        case VALUE_NUMBER_INT:
        case VALUE_NUMBER_FLOAT:
          numberType = _parser.getNumberType();
          if (numberType == null)
          {
            error(token, null);
            object = null;
            break;
          }
          switch (numberType) {
            case INT:
              object = _parser.getIntValue();
              break;
            case LONG:
              object = _parser.getLongValue();
              break;
            case FLOAT:
              object = _parser.getFloatValue();
              break;
            case DOUBLE:
              object = _parser.getDoubleValue();
              break;
            case BIG_INTEGER:
              // repeat to avoid fall through warning
              error(token, numberType);
              object = null;
              break;
            case BIG_DECIMAL:
            default:
              error(token, numberType);
              object = null;
              break;
          }
          break;
        case VALUE_TRUE:
          object = Boolean.TRUE;
          break;
        case VALUE_FALSE:
          object = Boolean.FALSE;
          break;
        case VALUE_NULL:
          object = Data.NULL;
          break;
        default:
          error(token, null);
          object = null;
          break;
      }
      return object;
    }

    private DataMap parseDataMap() throws IOException
    {
      return parseDataMapRecursive(0);
    }

    /**
     * This parses DataMap's recursively, keeping objects on the stack until the Map gets too large
     * or we are done parsing the map.
     * This prevents creating unnecessarily large DataMaps for a small number of k-v pairs.
     *
     * @param dataMapSize the current size of the DataMap
     */
    private DataMap parseDataMapRecursive(int dataMapSize) throws IOException {
      if (_parser.nextToken() == JsonToken.END_OBJECT) {
        return new DataMap(DataMapBuilder.getOptimumHashMapCapacityFromSize(dataMapSize));
      // prevent stack from getting too deep
      } else if (dataMapSize >= MAX_DATA_MAP_RECURSION_SIZE) {
        return parseDataMapIterative();
      }
      String key = _parser.getCurrentName();
      if (_debug)
      {
        _nameStack.addLast(key);
      }

      JsonToken token = _parser.nextToken();
      Object value = parse(token);
      DataMap map = parseDataMapRecursive(dataMapSize + 1);
      if (value != null) {
        updateParent(map, key, value);
      }

      if (_debug)
      {
        _nameStack.removeLast();
      }
      return map;
    }

    /**
     * this should only be called from parseDataMapRecursive; it assumes the current token is a Map-Key.
     */
    private DataMap parseDataMapIterative() throws IOException {
      DataMap map = new DataMap();
      addToMap(map);
      while (_parser.nextToken() != JsonToken.END_OBJECT)
      {
        addToMap(map);
      }
      return map;
    }

    private void addToMap(DataMap map) throws IOException {
      String key = _parser.getCurrentName();
      if (_debug)
      {
        _nameStack.addLast(key);
      }
      JsonToken token = _parser.nextToken();
      parse(map, key, token);
      if (_debug)
      {
        _nameStack.removeLast();
      }
    }

    private DataList parseDataList() throws IOException
    {
      DataList list = new DataList();
      JsonToken token;
      int index = 0;
      while ((token = _parser.nextToken()) != JsonToken.END_ARRAY)
      {
        if (_debug)
        {
          _nameStack.addLast(index);
          index++;
        }
        parse(list, null, token);
        if (_debug)
        {
          _nameStack.removeLast();
        }
      }
      return list;
    }

    private void error(JsonToken token, JsonParser.NumberType type) throws IOException
    {
      if (_errorBuilder == null)
      {
        _errorBuilder = new StringBuilder();
      }
      _errorBuilder.append(_parser.getTokenLocation()).append(": ");
      if (_debug)
      {
        _errorBuilder.append("name: ");
        Data.appendNames(_errorBuilder, _nameStack);
        _errorBuilder.append(", ");
      }
      _errorBuilder.append("value: ").append(_parser.getText()).append(", token: ").append(token);
      if (type != null)
      {
        _errorBuilder.append(", number type: ").append(type);
      }
      _errorBuilder.append(" not parsed.\n");
    }
  }

  private static class Location implements DataLocation
  {
    private final JsonLocation _location;

    private Location(JsonLocation location)
    {
      _location = location;
    }
    public int getColumn()
    {
      return _location.getColumnNr();
    }
    public int getLine()
    {
      return _location.getLineNr();
    }

    @Override
    public int compareTo(DataLocation other)
    {
      return (int) (_location.getCharOffset() - ((Location) other)._location.getCharOffset());
    }

    @Override
    public String toString()
    {
      return getLine() + "," + getColumn();
    }
  }
}
