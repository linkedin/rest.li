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

import com.linkedin.data.ByteString;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.symbol.SymbolTable;
import java.io.IOException;

/**
 * KSON encoder for a {@link com.linkedin.data.DataComplex} object implemented as a
 * {@link com.linkedin.entitystream.Writer} writing to an {@link com.linkedin.entitystream.EntityStream} of
 * {@link com.linkedin.data.ByteString}. The implementation is backed by Jackson's
 * {@link com.fasterxml.jackson.core.JsonGenerator} or {@link com.fasterxml.jackson.dataformat.smile.SmileGenerator},
 * depending on whether the codec is configured to emit text or binary. The generator writes to an
 * internal non-blocking <code>OutputStream</code> implementation that has a fixed-size primary buffer and
 * an unbounded overflow buffer. Because the bytes are pulled from the encoder asynchronously, it needs to keep the
 * state in a stack.
 *
 * <p>KSON is a tweaked version of JSON that serializes maps as lists, and has support for serializing field IDs
 * in lieu of field names using an optional symbol table.</p>
 *
 * @author kramgopa
 */
public class KSONDataEncoder extends AbstractJacksonDataEncoder
{
  private final SymbolTable _symbolTable;

  public KSONDataEncoder(DataMap dataMap, int bufferSize, boolean encodeBinary)
  {
    this(dataMap, bufferSize, encodeBinary, null);
  }

  public KSONDataEncoder(DataList dataList, int bufferSize, boolean encodeBinary)
  {
    this(dataList, bufferSize, encodeBinary, null);
  }

  public KSONDataEncoder(DataMap dataMap, int bufferSize, boolean encodeBinary, SymbolTable symbolTable)
  {
    super(KSONStreamDataCodec.getFactory(encodeBinary), dataMap, bufferSize);
    _symbolTable = symbolTable;
  }

  public KSONDataEncoder(DataList dataList, int bufferSize, boolean encodeBinary, SymbolTable symbolTable)
  {
    super(KSONStreamDataCodec.getFactory(encodeBinary), dataList, bufferSize);
    _symbolTable = symbolTable;
  }

  protected void writeStartObject() throws IOException
  {
    _generator.writeStartArray();
    _generator.writeNumber(KSONStreamDataCodec.MAP_ORDINAL);
  }

  protected void writeStartArray() throws IOException
  {
    _generator.writeStartArray();
    _generator.writeNumber(KSONStreamDataCodec.LIST_ORDINAL);
  }

  protected void writeFieldName(String name) throws IOException
  {
    int fieldId;
    if (_symbolTable != null && (fieldId = _symbolTable.getSymbolId(name)) != SymbolTable.UNKNOWN_SYMBOL_ID)
    {
      _generator.writeNumber(fieldId);
    }
    else
    {
      _generator.writeString(name);
    }
  }

  protected void writeEndObject() throws IOException
  {
    writeEndArray();
  }
}
