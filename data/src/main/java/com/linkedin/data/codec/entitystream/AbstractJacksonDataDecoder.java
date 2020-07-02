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

package com.linkedin.data.codec.entitystream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.linkedin.data.ByteString;
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.DataMapBuilder;
import com.linkedin.data.collections.CheckedUtil;
import com.linkedin.data.parser.NonBlockingDataParser;
import java.io.IOException;
import java.util.EnumSet;

import static com.linkedin.data.parser.NonBlockingDataParser.Token.*;


/**
 * A JSON or JSON-like format decoder for a {@link DataComplex} object implemented as a
 * {@link com.linkedin.entitystream.Reader} reading from an {@link com.linkedin.entitystream.EntityStream} of
 * ByteString. The implementation is backed by a non blocking {@link JsonParser}. Because the raw bytes are
 * pushed to the decoder, it keeps the partially built data structure in a stack.
 *
 * @author kramgopa, xma
 */
class AbstractJacksonDataDecoder<T extends DataComplex> extends AbstractDataDecoder<T>
{
  /**
   * Internal tokens. Each token is presented by a bit in a byte.
   * Deprecated, use {@link NonBlockingDataParser.Token}
   */
  @Deprecated
  enum Token
  {
    START_OBJECT(0b00000001),
    END_OBJECT  (0b00000010),
    START_ARRAY (0b00000100),
    END_ARRAY   (0b00001000),
    FIELD_NAME  (0b00010000),
    SIMPLE_VALUE(0b00100000);

    final byte bitPattern;

    Token(int bp)
    {
      bitPattern = (byte) bp;
    }
  }

  protected final JsonFactory _jsonFactory;
  private DataMapBuilder _currDataMapBuilder;

  /**
   * Deprecated, use {@link #AbstractJacksonDataDecoder(JsonFactory, EnumSet)} instead
   */
  @Deprecated
  protected AbstractJacksonDataDecoder(JsonFactory jsonFactory, byte expectedFirstToken)
  {
    super();
    _jsonFactory = jsonFactory;
    EnumSet<NonBlockingDataParser.Token> expectedDataToken = NONE;
    if ((expectedFirstToken & Token.START_OBJECT.bitPattern) != 0) {
      expectedDataToken.add(NonBlockingDataParser.Token.START_OBJECT);
    }
    if ((expectedFirstToken & Token.START_ARRAY.bitPattern) != 0) {
      expectedDataToken.add(NonBlockingDataParser.Token.START_ARRAY);
    }
    _expectedTokens = expectedDataToken;
  }

  protected AbstractJacksonDataDecoder(JsonFactory jsonFactory)
  {
    this(jsonFactory, START_TOKENS);
  }

  protected AbstractJacksonDataDecoder(JsonFactory jsonFactory, EnumSet<NonBlockingDataParser.Token> expectedFirstTokens)
  {
    super(expectedFirstTokens);
    _jsonFactory = jsonFactory;
  }

  @Override
  protected NonBlockingDataParser createDataParser() throws IOException
  {
    return new JacksonStreamDataParser(_jsonFactory);
  }

  @Override
  protected DataComplex createDataObject(NonBlockingDataParser parser)
  {
    if (_currDataMapBuilder == null || _currDataMapBuilder.inUse())
    {
      _currDataMapBuilder = new DataMapBuilder();
    }
    _currDataMapBuilder.setInUse(true);
    return _currDataMapBuilder;
  }

  @Override
  protected DataComplex createDataList(NonBlockingDataParser parser)
  {
    return new DataList();
  }

  @Override
  protected DataComplex postProcessDataComplex(DataComplex dataComplex)
  {
    if (dataComplex instanceof DataMapBuilder)
    {
      dataComplex = ((DataMapBuilder) dataComplex).convertToDataMap();
    }
    return dataComplex;
  }

  @Override
  protected void addEntryToDataObject(DataComplex dataComplex, String currField, Object currValue)
  {
    if (dataComplex instanceof DataMapBuilder)
    {
      DataMapBuilder dataMapBuilder = (DataMapBuilder) dataComplex;
      if (dataMapBuilder.smallHashMapThresholdReached())
      {
        DataMap dataMap = dataMapBuilder.convertToDataMap();
        replaceObjectStackTop(dataMap);
        CheckedUtil.putWithoutChecking(dataMap, currField, currValue);
      }
      else
      {
        dataMapBuilder.addKVPair(currField, currValue);
      }
    }
    else
    {
      CheckedUtil.putWithoutChecking((DataMap) dataComplex, currField, currValue);
    }
  }

  class JacksonStreamDataParser implements NonBlockingDataParser
  {
    private final JsonParser _jsonParser;
    private final ByteArrayFeeder _byteArrayFeeder;
    private JsonToken _previousTokenReturned;

    public JacksonStreamDataParser(JsonFactory jsonFactory) throws IOException
    {
      _jsonParser = jsonFactory.createNonBlockingByteArrayParser();
      _byteArrayFeeder = (ByteArrayFeeder) _jsonParser;
    }

    @Override
    public void feedInput(byte[] data, int offset, int len) throws IOException
    {
      if(_byteArrayFeeder.needMoreInput())
      {
        _byteArrayFeeder.feedInput(data, offset, offset + len);
      }
      else
      {
        throw new IOException("Invalid state: Parser cannot accept more data");
      }
    }

    @Override
    public void endOfInput()
    {
      _byteArrayFeeder.endOfInput();
    }

    @Override
    public NonBlockingDataParser.Token nextToken() throws IOException
    {
      _previousTokenReturned = _jsonParser.nextToken();
      if (_previousTokenReturned == null)
      {
        return EOF_INPUT;
      }
      switch (_previousTokenReturned)
      {
        case START_OBJECT:
          return START_OBJECT;
        case END_OBJECT:
          return END_OBJECT;
        case START_ARRAY:
          return START_ARRAY;
        case END_ARRAY:
          return END_ARRAY;
        case FIELD_NAME:
        case VALUE_STRING:
          return STRING;
        case VALUE_NUMBER_INT:
        case VALUE_NUMBER_FLOAT:
          JsonParser.NumberType numberType = _jsonParser.getNumberType();
          switch (numberType)
          {
            case INT:
              return INTEGER;
            case LONG:
              return LONG;
            case FLOAT:
              return FLOAT;
            case DOUBLE:
              return DOUBLE;
            default:
              throw new IOException(
                  "Unexpected number value type " + numberType + " at " + _jsonParser.getTokenLocation());
          }
        case VALUE_TRUE:
          return BOOL_TRUE;
        case VALUE_FALSE:
          return BOOL_FALSE;
        case VALUE_NULL:
          return NULL;
        case NOT_AVAILABLE:
          return NOT_AVAILABLE;
        default:
          throw new IOException("Unexpected token " + _previousTokenReturned + " at " + _jsonParser.getTokenLocation());
      }
    }

    @Override
    public String getString() throws IOException {
      return _previousTokenReturned == JsonToken.FIELD_NAME ? _jsonParser.getCurrentName() : _jsonParser.getText();
    }

    @Override
    public ByteString getRawBytes() throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getIntValue() throws IOException {
      return _jsonParser.getIntValue();
    }

    @Override
    public long getLongValue() throws IOException {
      return _jsonParser.getLongValue();
    }

    @Override
    public float getFloatValue() throws IOException {
      return _jsonParser.getFloatValue();
    }

    @Override
    public double getDoubleValue() throws IOException {
      return _jsonParser.getDoubleValue();
    }
  }
}
