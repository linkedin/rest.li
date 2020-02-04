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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;
import com.linkedin.data.Data;
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.symbol.SymbolTable;
import com.linkedin.data.collections.CheckedUtil;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A codec that serializes to and deserializes from LICOR (LinkedIn Compact Object Representation) encoded data, using
 * the Jackson {@link JsonFactory} under the hood.
 *
 * <p>LICOR is a tweaked version of JSON that serializes maps as lists, and has support for serializing field IDs
 * in lieu of field names using an optional symbol table. The payload is serialized as JSON or SMILE depending on
 * whether the codec is configured to use binary or not.</p>
 *
 * @author kramgopa
 */
public class JacksonLICORDataCodec extends AbstractJacksonDataCodec
{
  private static final JsonFactory TEXT_FACTORY = new JsonFactory();
  private static final JsonFactory BINARY_FACTORY =
      new SmileFactory().enable(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES);
  private static final byte MAP_ORDINAL = 0;
  private static final byte LIST_ORDINAL = 1;

  protected final SymbolTable _symbolTable;

  public JacksonLICORDataCodec(boolean useBinary)
  {
    this(useBinary, (SymbolTable) null);
  }

  public JacksonLICORDataCodec(boolean useBinary, SymbolTable symbolTable)
  {
    super(getFactory(useBinary));

    _symbolTable = symbolTable;
  }

  /**
   * @deprecated Use {@link #JacksonLICORDataCodec(boolean, SymbolTable)} instead. This constructor ignores the
   *             second argument.
   */
  @Deprecated
  public JacksonLICORDataCodec(boolean useBinary, String symbolTableName)
  {
    this(useBinary);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected <T extends DataComplex> T parse(JsonParser jsonParser, Class<T> expectType) throws IOException
  {
    try
    {
      Object object = new LICORParser(jsonParser, _symbolTable).parse(true);
      if (expectType == DataMap.class && (object instanceof DataMap))
      {
        return (T)object;
      }
      else if (expectType == DataList.class && (object instanceof DataList))
      {
        return (T)object;
      }
      else
      {
        throw new DataDecodingException("Unexpected object type: " + object.getClass() + ", expected " + expectType);
      }
    }
    finally
    {
      DataCodec.closeQuietly(jsonParser);
    }
  }

  @Override
  protected List<Object> parse(JsonParser jsonParser, StringBuilder mesg, Map<Object, DataLocation> locationMap)
      throws IOException
  {
    throw new UnsupportedOperationException("Debug mode is not supported with LICOR");
  }

  @Override
  protected Data.TraverseCallback createTraverseCallback(JsonGenerator generator)
  {
    return new LICORTraverseCallback(generator, _symbolTable);
  }

  private static JsonFactory getFactory(boolean encodeBinary)
  {
    return encodeBinary ? BINARY_FACTORY : TEXT_FACTORY;
  }

  private static class LICORParser
  {
    private final JsonParser _parser;
    private final SymbolTable _symbolTable;

    LICORParser(JsonParser jsonParser, SymbolTable symbolTable)
    {
      _parser = jsonParser;
      _symbolTable = symbolTable;
    }

    Object parse(boolean moveToNext) throws IOException
    {
      final JsonToken token = moveToNext ? _parser.nextToken() : _parser.currentToken();
      if (token == null)
      {
        return null;
      }

      switch(token) {
        case START_ARRAY:
        {
          _parser.nextToken();
          byte marker = _parser.getByteValue();
          switch (marker)
          {
            case MAP_ORDINAL: {
              DataMap dataMap = new DataMap();
              while (_parser.nextToken() != JsonToken.END_ARRAY)
              {
                String key;
                switch (_parser.currentToken())
                {
                  case VALUE_STRING: {
                    key = _parser.getValueAsString();
                    break;
                  }
                  case VALUE_NUMBER_INT: {
                    int symbol = _parser.getIntValue();
                    if (_symbolTable == null || (key = _symbolTable.getSymbolName(symbol)) == null) {
                      throw new DataDecodingException("No mapping found for symbol " + symbol);
                    }
                    break;
                  }
                  default:
                    throw new DataDecodingException("Unexpected token: " + _parser.currentToken().asString());
                }
                JsonToken valueType = _parser.nextToken();
                if (valueType == null)
                {
                  throw new DataDecodingException("Found key: " + key + " without corresponding value");
                }
                Object value = parse(false);
                CheckedUtil.putWithoutChecking(dataMap, key, value);
              }
              return dataMap;
            }
            case LIST_ORDINAL: {
              DataList dataList = new DataList();

              do {
                JsonToken elementType = _parser.nextToken();
                if (elementType == JsonToken.END_ARRAY)
                {
                  return dataList;
                }
                Object listElement = parse(false);
                if (listElement != null)
                {
                  CheckedUtil.addWithoutChecking(dataList, listElement);
                }
              } while(true);
            }
            default: {
              throw new DataDecodingException("Unexpected marker: " + marker);
            }
          }
        }
        case VALUE_STRING:
          return _parser.getValueAsString();
        case VALUE_NUMBER_INT:
        case VALUE_NUMBER_FLOAT:
          JsonParser.NumberType numberType = _parser.getNumberType();
          if (numberType == null)
          {
            throw new DataDecodingException("Unexpected Number Type: " + token.asString());
          }
          switch (numberType) {
            case INT:
              return _parser.getIntValue();
            case LONG:
              return _parser.getLongValue();
            case FLOAT:
              return _parser.getFloatValue();
            case DOUBLE:
              return _parser.getDoubleValue();
            case BIG_INTEGER:
            case BIG_DECIMAL:
            default:
              throw new DataDecodingException("Unexpected Number Type: " + token.asString());
          }
        case VALUE_TRUE:
          return Boolean.TRUE;
        case VALUE_FALSE:
          return Boolean.FALSE;
        case VALUE_NULL:
          return Data.NULL;
      }

      throw new DataDecodingException("Unexpected JSON Type: " + token.asString());
    }
  }

  public static class LICORTraverseCallback extends JacksonTraverseCallback
  {
    private final SymbolTable _symbolTable;

    public LICORTraverseCallback(JsonGenerator generator, SymbolTable symbolTable)
    {
      super(generator);
      _symbolTable = symbolTable;
    }

    @Override
    public void key(String key) throws IOException {
      int token;
      if (_symbolTable != null && (token = _symbolTable.getSymbolId(key)) != SymbolTable.UNKNOWN_SYMBOL_ID)
      {
        _generator.writeNumber(token);
      }
      else
      {
        _generator.writeString(key);
      }
    }

    @Override
    public void startList(DataList list) throws IOException {
      _generator.writeStartArray();
      _generator.writeNumber(LIST_ORDINAL);
    }

    @Override
    public void startMap(DataMap map) throws IOException {
      _generator.writeStartArray();
      _generator.writeNumber(MAP_ORDINAL);
    }

    @Override
    public void emptyList() throws IOException {
      _generator.writeStartArray();
      _generator.writeNumber(LIST_ORDINAL);
      endList();
    }

    @Override
    public void emptyMap() throws IOException {
      _generator.writeStartArray();
      _generator.writeNumber(MAP_ORDINAL);
      endMap();
    }

    @Override
    public void endMap() throws IOException {
      _generator.writeEndArray();
    }
  }
}
