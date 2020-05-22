/*
   Copyright (c) 2020 LinkedIn Corp.

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

import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.linkedin.data.ByteString;
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.DataMapBuilder;
import com.linkedin.data.parser.NonBlockingDataParser;
import com.linkedin.data.codec.DataDecodingException;
import com.linkedin.data.codec.symbol.EmptySymbolTable;
import com.linkedin.data.codec.symbol.SymbolTable;
import com.linkedin.data.protobuf.ProtoReader;
import com.linkedin.data.protobuf.ProtoWriter;
import com.linkedin.data.protobuf.TextBuffer;
import com.linkedin.data.protobuf.Utf8Utils;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;

import static com.linkedin.data.parser.NonBlockingDataParser.Token.*;
import static com.linkedin.data.codec.ProtobufDataCodec.*;

/**
 * A ProtoBuf format decoder for a {@link DataComplex} object, reading from an
 * {@link com.linkedin.entitystream.EntityStream} of ByteString.
 * The implementation is backed by a non blocking {@link ProtobufStreamDataParser}. Because the raw bytes are
 * pushed to the decoder, it keeps the partially built data structure in a stack.
 *
 * @author amgupta1
 */
class ProtobufDataDecoder<T extends DataComplex> extends AbstractDataDecoder<T>
{

  private final SymbolTable _symbolTable;

  protected ProtobufDataDecoder(SymbolTable symbolTable, EnumSet<NonBlockingDataParser.Token> expectedFirstToken)
  {
    super(expectedFirstToken);
    _symbolTable = symbolTable == null ? EmptySymbolTable.SHARED : symbolTable;
  }

  @Override
  protected NonBlockingDataParser createDataParser() throws IOException
  {
    return new ProtobufStreamDataParser(_symbolTable);
  }

  @Override
  protected DataComplex createDataObject(NonBlockingDataParser parser)
  {
    return new DataMap(DataMapBuilder.getOptimumHashMapCapacityFromSize(parser.getComplexObjSize()));
  }

  @Override
  protected DataComplex createDataList(NonBlockingDataParser parser)
  {
    return new DataList(parser.getComplexObjSize());
  }

  class ProtobufStreamDataParser implements NonBlockingDataParser
  {
    private final SymbolTable _symbolTable;

    private final Deque<Integer> _complexObjTokenSizeStack = new ArrayDeque<>();
    private int _currComplexObjTokenSize = -1;

    private byte[] _input;  //holds feed input bytes
    private int _limit;
    private int _pos;

    private boolean _eofInput;  //no more inputs can be feed if this is set to true

    private final TextBuffer _textBuffer;  //buffer to hold parsed string characters.
    private int _bufferPos = -1;  //signify no. of chars in text buffers as buffer is reused to avoid thrashing

    private int _pendingCharUtfRep;  // no. of bytes used by Utf-8 multi-byte representation of pending char
    private int _pendingIntShifts = -1;  // remaining bits/bytes for int32/64
    private long _pendingInt64;
    private int _pendingInt32;

    // Stores current token returned from #nextToken else Token#NOT_AVAILABLE
    private Token _currentToken;
    private byte _currentOrdinal = -1;

    //Below value variables hold parsed value for current token returned from #nextToken
    private byte[] _bytesValue;
    private String _stringValue;
    private int _intValue;
    private long _longValue;

    ProtobufStreamDataParser(SymbolTable symbolTable)
    {
      _symbolTable = symbolTable == null ? EmptySymbolTable.SHARED : symbolTable;
      _textBuffer = new TextBuffer(ProtoReader.DEFAULT_TEXT_BUFFER_SIZE);
    }

    @Override
    public void feedInput(byte[] data, int offset, int len) throws IOException
    {
      if (data == null || data.length < offset + len)
      {
        throw new IllegalArgumentException("Bad arguments");
      }

      if (_pos >= _limit && !_eofInput)
      {
        _pos = offset;
        _limit = offset + len;
        _input = data;
      }
      else
      {
        throw new IOException("Invalid state: Parser cannot accept more data");
      }
    }

    @Override
    public void endOfInput()
    {
      _eofInput = true;
    }

    @Override
    public Token nextToken() throws IOException
    {
      // First: regardless of where we really are, need at least one more byte;
      // can simplify some of the checks by short-circuiting right away
      if (_pos >= _limit) {
        Token endComplexObjToken = readEndComplexObj();
        if (endComplexObjToken != NOT_AVAILABLE)
        {
          return finishToken(endComplexObjToken);
        }
        if (_eofInput) {
          return EOF_INPUT;
        }
        return NOT_AVAILABLE;
      }
      if (_currentToken != NOT_AVAILABLE)
      {
        _currentToken = readEndComplexObj();
        if (_currentToken != NOT_AVAILABLE)
        {
          return finishToken(_currentToken);
        }
        _currentOrdinal = _input[_pos++];
        //release bytes array if previous token was Token#RAW_BYTES
        _bytesValue = null;
      }
      Token currToken;
      switch (_currentOrdinal)
      {
        case MAP_ORDINAL:
          currToken = readInt32();
          if (currToken == INTEGER)
          {
            currToken = START_OBJECT;
          }
          break;
        case LIST_ORDINAL:
          currToken = readInt32();
          if (currToken == INTEGER)
          {
            currToken = START_ARRAY;
          }
          break;
        case ASCII_STRING_LITERAL_ORDINAL:
          currToken = readASCIIString();
          break;
        case STRING_LITERAL_ORDINAL:
          currToken = readString();
          break;
        case STRING_REFERENCE_ORDINAL:
          currToken = readStringReference();
          break;
        case INTEGER_ORDINAL:
          currToken = readInt32();
          break;
        case LONG_ORDINAL:
          currToken = readInt64();
          break;
        case FLOAT_ORDINAL:
          currToken = readInt32();
          if (currToken == INTEGER)
          {
            currToken = FLOAT;
          }
          break;
        case FIXED_FLOAT_ORDINAL:
          currToken = readFixedInt32();
          if (currToken == INTEGER)
          {
            currToken = FLOAT;
          }
          break;
        case DOUBLE_ORDINAL:
          currToken = readInt64();
          if (currToken == LONG)
          {
            currToken = DOUBLE;
          }
          break;
        case FIXED_DOUBLE_ORDINAL:
          currToken = readFixedInt64();
          if (currToken == LONG)
          {
            currToken = DOUBLE;
          }
          break;
        case BOOLEAN_TRUE_ORDINAL:
          currToken = BOOL_TRUE;
          break;
        case BOOLEAN_FALSE_ORDINAL:
          currToken = BOOL_FALSE;
          break;
        case RAW_BYTES_ORDINAL:
          currToken = readByteArray();
          break;
        case NULL_ORDINAL:
          currToken = NULL;
          break;
        default: throw new DataDecodingException("Unknown ordinal: " + _currentOrdinal);
      }
      return finishToken(currToken);
    }

    private Token readEndComplexObj()
    {
      if(_currComplexObjTokenSize == 0)
      {
        if (!_complexObjTokenSizeStack.isEmpty())
        {
          _currComplexObjTokenSize = _complexObjTokenSizeStack.pop();
        }
        return isCurrList() ? END_ARRAY : END_OBJECT;
      }
      return NOT_AVAILABLE;
    }

    private Token readStringReference() throws IOException
    {
      Token refToken = readInt32();
      if (refToken == NOT_AVAILABLE)
      {
        return NOT_AVAILABLE;
      }
      if ((_stringValue = _symbolTable.getSymbolName(_intValue)) == null)
      {
        throw new DataDecodingException("Error decoding string reference");
      }
      return STRING;
    }

    private Token finishToken(Token token)
    {
      _currentToken = token;
      switch (_currentToken)
      {
        case START_OBJECT:
          if (_currComplexObjTokenSize > 0)
          {
            _complexObjTokenSizeStack.push(_currComplexObjTokenSize);
          }
          _currComplexObjTokenSize = _intValue << 1;
          break;
        case START_ARRAY:
          if (_currComplexObjTokenSize > 0)
          {
            _complexObjTokenSizeStack.push(_currComplexObjTokenSize);
          }
          _currComplexObjTokenSize = _intValue;
          break;
        case NOT_AVAILABLE:
          break;
        default:
          _currComplexObjTokenSize--;
      }
      return _currentToken;
    }

    @Override
    public int getComplexObjSize()
    {
      return _currentToken == START_OBJECT || _currentToken == START_ARRAY ? _intValue : -1;
    }

    @Override
    public String getString() throws IOException
    {
      if (_currentToken != STRING)
      {
        throw new DataDecodingException("Unexpected call: String value is not available");
      }
      return _stringValue;
    }

    @Override
    public ByteString getRawBytes() throws IOException {
      if (_currentToken != RAW_BYTES)
      {
        throw new DataDecodingException("Unexpected call: Raw bytes value is not available");
      }
      return ByteString.unsafeWrap(_bytesValue);
    }

    @Override
    public int getIntValue() throws IOException
    {
      if (_currentToken != INTEGER)
      {
        throw new DataDecodingException("Unexpected call: int value is not available");
      }
      return _intValue;
    }

    @Override
    public long getLongValue() throws IOException
    {
      if (_currentToken != LONG)
      {
        throw new DataDecodingException("Unexpected call: Raw bytes value is not available");
      }
      return _longValue;
    }

    @Override
    public float getFloatValue() throws IOException
    {
      if (_currentToken != FLOAT)
      {
        throw new DataDecodingException("Unexpected call: Raw bytes value is not available");
      }
      return Float.intBitsToFloat(_intValue);
    }

    @Override
    public double getDoubleValue() throws IOException
    {
      if (_currentToken != DOUBLE)
      {
        throw new DataDecodingException("Unexpected call: Raw bytes value is not available");
      }
      return Double.longBitsToDouble(_longValue);
    }

    /*
    * Non blocking ProtoReader Implementation
    */

    private Token readASCIIString() throws IOException {
      if (_bufferPos == -1)
      {
        Token sizeToken = readInt32();
        if (sizeToken == NOT_AVAILABLE)
        {
          return NOT_AVAILABLE;
        }
      }
      int remainingSize = _intValue;
      if (remainingSize > 0)
      {
        if (_bufferPos == -1 && remainingSize <= _limit - _pos)
        {
          // If we can read from the current chunk, read directly.
          _stringValue = Utf8Utils.decodeASCII(_input, _pos, remainingSize, _textBuffer);
          _pos += remainingSize;
          return STRING;
        }
        else
        {
          char[] resultArr = null;
          try
          {
            if (_bufferPos == -1)
            {
              resultArr = _textBuffer.getBuf(remainingSize);
              _bufferPos = 0;
            }
            else
            {
              resultArr = _textBuffer.getBuf();
            }
            while (_pos < _limit && remainingSize > 0)
            {
              resultArr[_bufferPos++] = (char) _input[_pos++];
              remainingSize--;
            }
            if (remainingSize == 0)
            {
              _stringValue = new String(resultArr, 0, _bufferPos);
              _bufferPos = -1;
              return STRING;
            }
            else
            {
              _intValue = remainingSize;
              return NOT_AVAILABLE;
            }
          }
          finally
          {
            _textBuffer.returnBuf(resultArr);
          }
        }
      }
      else if (remainingSize == 0)
      {
        _stringValue = "";
        return STRING;
      }
      else
      {
        throw new DataDecodingException("Read negative size: " + remainingSize + ". Invalid string");
      }
    }

    private Token readString() throws IOException
    {
      if (_bufferPos == -1)
      {
        Token sizeToken = readInt32();
        if (sizeToken == NOT_AVAILABLE)
        {
          return NOT_AVAILABLE;
        }
      }
      int remainingSize = _intValue;
      if (remainingSize > 0)
      {
        if (_bufferPos == -1 && remainingSize <= _limit - _pos)
        {
          // If we can read from the current chunk, read directly.
          _stringValue = Utf8Utils.decode(_input, _pos, remainingSize, _textBuffer);
          _pos += remainingSize;
          return STRING;
        }
        else
        {
          char[] resultArr = null;
          try
          {
            if (_bufferPos == -1)
            {
              resultArr = _textBuffer.getBuf(remainingSize);
              _bufferPos = 0;
            }
            else
            {
              resultArr = _textBuffer.getBuf();
            }
            while (_pos < _limit && remainingSize > 0)
            {
              int i;
              if (_pendingIntShifts != -1)
              {
                i = _pendingInt32;
              }
              else
              {
                i = _input[_pos++] & 0xff;
                _pendingCharUtfRep = Utf8Utils.lookupUtfTable(i);
                remainingSize--;
                _pendingIntShifts = 1;
              }
              switch (_pendingCharUtfRep)
              {
                case 0:
                  // ASCII. Nothing to do, since byte is same as char.
                  break;
                case 2:
                  // 2 byte unicode
                  if (_pos == _limit)
                  {
                    _pendingInt32 = i;
                    _intValue = remainingSize;
                    return NOT_AVAILABLE;
                  }
                  else
                  {
                    i = ((i & 0x1F) << 6) | (_input[_pos++] & 0x3F);
                    remainingSize--;
                  }
                  break;
                case 3:
                  // 3 byte unicode
                  for (; _pendingIntShifts < _pendingCharUtfRep; ++_pendingIntShifts)
                  {
                    if (_pos == _limit)
                    {
                      _pendingInt32 = i;
                      _intValue = remainingSize;
                      return NOT_AVAILABLE;
                    }
                    else
                    {
                      if (_pendingIntShifts == 1)
                      {
                        i = ((i & 0x0F) << 12) | ((_input[_pos++] & 0x3F) << 6);
                      }
                      else if (_pendingIntShifts == 2)
                      {
                        i |= (_input[_pos++] & 0x3F);
                      }
                      remainingSize--;
                    }
                  }
                  break;
                case 4:
                  // 4 byte unicode
                  for (; _pendingIntShifts < _pendingCharUtfRep; ++_pendingIntShifts)
                  {
                    if (_pos == _limit)
                    {
                      _pendingInt32 = i;
                      _intValue = remainingSize;
                      return NOT_AVAILABLE;
                    }
                    else
                    {
                      if (_pendingIntShifts == 1)
                      {
                        i = ((i & 0x07) << 18) | ((_input[_pos++] & 0x3F) << 12);
                      }
                      else if (_pendingIntShifts == 2)
                      {
                        i |= ((_input[_pos++] & 0x3F) << 6);
                      }
                      else if (_pendingIntShifts == 3)
                      {
                        i |= (_input[_pos++] & 0x3F);
                        // Split the codepoint
                        i -= 0x10000;
                        resultArr[_bufferPos++] = (char) (0xD800 | (i >> 10));
                        i = 0xDC00 | (i & 0x3FF);
                      }
                      remainingSize--;
                    }
                  }
                  break;
                default:
                  throw new IllegalArgumentException("Invalid UTF-8. UTF-8 character cannot be " + Utf8Utils.lookupUtfTable(i) + "bytes");
              }
              resultArr[_bufferPos++] = (char) i;
              _pendingIntShifts = -1;
            }
            if (remainingSize == 0)
            {
              _stringValue = new String(resultArr, 0, _bufferPos);
              _bufferPos = -1;
              return STRING;
            }
            else
            {
              _intValue = remainingSize;
              return NOT_AVAILABLE;
            }
          }
          finally
          {
            _textBuffer.returnBuf(resultArr);
          }
        }
      }
      else if (remainingSize == 0)
      {
        _stringValue = "";
        return STRING;
      }
      else
      {
        throw new DataDecodingException("Read negative size: " + remainingSize + ". Invalid string");
      }
    }

    private Token readByteArray() throws IOException
    {
      if (_bytesValue == null)
      {
        Token sizeToken = readInt32();
        if (sizeToken == NOT_AVAILABLE)
        {
          return NOT_AVAILABLE;
        }
      }
      int size = _intValue;
      if (size < 0)
      {
        throw new DataDecodingException("Read negative size: " + size + ". Invalid byte array");
      }
      if (_bytesValue == null)
      {
        _bytesValue = new byte[size];
      }

      if (size <= _limit - _pos)
      {
        System.arraycopy(_input, _pos, _bytesValue, _bytesValue.length - size, size);
        _pos += size;
        return RAW_BYTES;
      }
      else
      {
        System.arraycopy(_input, _pos, _bytesValue, _bytesValue.length - size, _limit - _pos);
        _intValue -= _limit - _pos;
        _pos = _limit;
        return NOT_AVAILABLE;
      }
    }

    private Token readInt32() throws IOException
    {
      // See implementation notes for readInt64
      fastpath:
      {
        int tempPos = _pos;

        if (_pos == _limit || _pendingIntShifts != -1)
        {
          break fastpath;
        }

        final byte[] buffer = _input;
        int x;
        if ((x = buffer[tempPos++]) >= 0)
        {
          _pos = tempPos;
          _intValue = x;
          return INTEGER;
        }
        else if (_limit - tempPos < 9)
        {
          break fastpath;
        }
        else if ((x ^= (buffer[tempPos++] << 7)) < 0)
        {
          x ^= (~0 << 7);
        }
        else if ((x ^= (buffer[tempPos++] << 14)) >= 0)
        {
          x ^= (~0 << 7) ^ (~0 << 14);
        }
        else if ((x ^= (buffer[tempPos++] << 21)) < 0)
        {
          x ^= (~0 << 7) ^ (~0 << 14) ^ (~0 << 21);
        }
        else
        {
          int y = buffer[tempPos++];
          x ^= y << 28;
          x ^= (~0 << 7) ^ (~0 << 14) ^ (~0 << 21) ^ (~0 << 28);
          if (y < 0
              && buffer[tempPos++] < 0
              && buffer[tempPos++] < 0
              && buffer[tempPos++] < 0
              && buffer[tempPos++] < 0
              && buffer[tempPos++] < 0)
          {
            break fastpath; // Will throw malformedVarint()
          }
        }
        _pos = tempPos;
        _intValue = x;
        return INTEGER;
      }
      Token token = readRawVarint64SlowPath();
      if (token == LONG)
      {
        _intValue = (int) _pendingInt64;
        token = INTEGER;
      }
      return token;
    }

    private Token readInt64() throws IOException
    {
      // Implementation notes:
      //
      // Optimized for one-byte values, expected to be common.
      // The particular code below was selected from various candidates
      // empirically, by winning VarintBenchmark.
      //
      // Sign extension of (signed) Java bytes is usually a nuisance, but
      // we exploit it here to more easily obtain the sign of bytes read.
      // Instead of cleaning up the sign extension bits by masking eagerly,
      // we delay until we find the final (positive) byte, when we clear all
      // accumulated bits with one xor.  We depend on javac to constant fold.
      fastpath:
      {
        if (_pos == _limit || _pendingIntShifts != -1)
        {
          break fastpath;
        }
        int tempPos = _pos;
        final byte[] buffer = _input;
        long x;
        int y;
        if ((y = buffer[tempPos++]) >= 0)
        {
          _pos = tempPos;
          _longValue = y;
          return LONG;
        }
        else if (_limit - tempPos < 9)
        {
          break fastpath;
        }
        else if ((y ^= (buffer[tempPos++] << 7)) < 0)
        {
          x = y ^ (~0 << 7);
        }
        else if ((y ^= (buffer[tempPos++] << 14)) >= 0)
        {
          x = y ^ ((~0 << 7) ^ (~0 << 14));
        }
        else if ((y ^= (buffer[tempPos++] << 21)) < 0)
        {
          x = y ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21));
        }
        else if ((x = y ^ ((long) buffer[tempPos++] << 28)) >= 0L)
        {
          x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28);
        }
        else if ((x ^= ((long) buffer[tempPos++] << 35)) < 0L)
        {
          x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35);
        }
        else if ((x ^= ((long) buffer[tempPos++] << 42)) >= 0L)
        {
          x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42);
        }
        else if ((x ^= ((long) buffer[tempPos++] << 49)) < 0L)
        {
          x ^=
              (~0L << 7)
                  ^ (~0L << 14)
                  ^ (~0L << 21)
                  ^ (~0L << 28)
                  ^ (~0L << 35)
                  ^ (~0L << 42)
                  ^ (~0L << 49);
        }
        else
        {
          x ^= ((long) buffer[tempPos++] << 56);
          x ^=
              (~0L << 7)
                  ^ (~0L << 14)
                  ^ (~0L << 21)
                  ^ (~0L << 28)
                  ^ (~0L << 35)
                  ^ (~0L << 42)
                  ^ (~0L << 49)
                  ^ (~0L << 56);
          if (x < 0L)
          {
            if (buffer[tempPos++] < 0L)
            {
              break fastpath; // Will throw malformedVarint()
            }
          }
        }
        _pos = tempPos;
        _longValue = x;
        return LONG;
      }
      Token token = readRawVarint64SlowPath();
      if (token == LONG)
      {
        _longValue = _pendingInt64;
      }
      return token;
    }

    private Token readRawVarint64SlowPath() throws IOException
    {
      if (_pendingIntShifts == -1)
      {
        _pendingInt64 = 0;
        _pendingIntShifts = 0;
      }
      for (; _pendingIntShifts < 64; _pendingIntShifts += 7)
      {
        final byte b;
        if (_pos < _limit)
        {
          b = _input[_pos++];
        }
        else
        {
          return NOT_AVAILABLE;
        }
        _pendingInt64 |= (long) (b & 0x7F) << _pendingIntShifts;
        if ((b & 0x80) == 0)
        {
          _pendingIntShifts = -1;
          return LONG;
        }
      }
      _pendingIntShifts = -1;
      throw new DataDecodingException("Malformed VarInt");
    }

    private Token readFixedInt32()
    {
      if (_pendingIntShifts != -1 || _limit - _pos < ProtoWriter.FIXED32_SIZE)
      {
        if (_pendingIntShifts == -1)
        {
          _pendingIntShifts = 0;
          _pendingInt32 = 0;
        }
        while (_pos < _limit && _pendingIntShifts <= 24)
        {
          _pendingInt32 |= ((_input[_pos++] & 0xff) << _pendingIntShifts);
          _pendingIntShifts += 8;
        }
        if (_pendingIntShifts > 24)
        {
          _intValue = _pendingInt32;
          _pendingIntShifts = -1;
          return INTEGER;
        }
        else
        {
          return NOT_AVAILABLE;
        }
      }

      _intValue = (((_input[_pos++] & 0xff))
          | ((_input[_pos++] & 0xff) << 8)
          | ((_input[_pos++] & 0xff) << 16)
          | ((_input[_pos++] & 0xff) << 24));
      return INTEGER;
    }

    private Token readFixedInt64()
    {
      if (_pendingIntShifts != -1 || _limit - _pos < ProtoWriter.FIXED64_SIZE)
      {
        if (_pendingIntShifts == -1)
        {
          _pendingIntShifts = 0;
          _pendingInt64 = 0;
        }
        while (_pos < _limit && _pendingIntShifts <= 56)
        {
          _pendingInt64 |= ((_input[_pos++] & 0xffL) << _pendingIntShifts);
          _pendingIntShifts += 8;
        }
        if (_pendingIntShifts > 56)
        {
          _longValue = _pendingInt64;
          _pendingIntShifts = -1;
          return LONG;
        }
        else
        {
          return NOT_AVAILABLE;
        }
      }

      _longValue = (((_input[_pos++] & 0xffL))
          | ((_input[_pos++] & 0xffL) << 8)
          | ((_input[_pos++] & 0xffL) << 16)
          | ((_input[_pos++] & 0xffL) << 24)
          | ((_input[_pos++] & 0xffL) << 32)
          | ((_input[_pos++] & 0xffL) << 40)
          | ((_input[_pos++] & 0xffL) << 48)
          | ((_input[_pos++] & 0xffL) << 56));
      return LONG;
    }
  }
}
