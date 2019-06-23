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
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;
import com.linkedin.data.ByteString;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.symbol.SymbolTable;
import com.linkedin.data.codec.symbol.SymbolTableProvider;
import com.linkedin.entitystream.EntityStream;
import com.linkedin.entitystream.EntityStreams;

import java.util.concurrent.CompletionStage;

/**
 * An {@link StreamDataCodec} for A LICOR (LinkedIn Compact Object Representation) backed by Jackson's non-blocking
 * parser and generator.
 *
 * <p>LICOR is a tweaked version of JSON that serializes maps as lists, and has support for serializing field IDs
 * in lieu of field names using an optional symbol table. The payload is serialized as JSON or SMILE depending on
 * whether the codec is configured to use binary or not.</p>
 *
 * @author kramgopa
 */
public class JacksonLICORStreamDataCodec implements StreamDataCodec
{
  private static volatile SymbolTableProvider _symbolTableProvider;

  private static final JsonFactory TEXT_FACTORY = new JsonFactory();
  private static final JsonFactory BINARY_FACTORY =
      new SmileFactory().enable(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES);

  static final byte MAP_ORDINAL = 0;
  static final byte LIST_ORDINAL = 1;

  protected final int _bufferSize;
  protected final boolean _useBinary;
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

  public JacksonLICORStreamDataCodec(int bufferSize, boolean useBinary)
  {
    this(bufferSize, useBinary, null);
  }

  public JacksonLICORStreamDataCodec(int bufferSize, boolean useBinary, String symbolTableName)
  {
    _bufferSize = bufferSize;
    _useBinary = useBinary;

    if (symbolTableName != null && _symbolTableProvider != null)
    {
      _symbolTable = _symbolTableProvider.getSymbolTable(symbolTableName);
    }
    else
    {
      _symbolTable = null;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public CompletionStage<DataMap> decodeMap(EntityStream<ByteString> entityStream)
  {
    JacksonLICORDataDecoder<DataMap> decoder = new JacksonLICORDataDecoder<>(_useBinary, false, _symbolTable);
    entityStream.setReader(decoder);
    return decoder.getResult();
  }

  @SuppressWarnings("unchecked")
  @Override
  public CompletionStage<DataList> decodeList(EntityStream<ByteString> entityStream)
  {
    JacksonLICORDataDecoder<DataList> decoder = new JacksonLICORDataDecoder<>(_useBinary, true, _symbolTable);
    entityStream.setReader(decoder);
    return decoder.getResult();
  }

  @Override
  public EntityStream<ByteString> encodeMap(DataMap map)
  {
    JacksonLICORDataEncoder encoder = new JacksonLICORDataEncoder(map, _bufferSize, _useBinary, _symbolTable);
    return EntityStreams.newEntityStream(encoder);
  }

  @Override
  public EntityStream<ByteString> encodeList(DataList list)
  {
    JacksonLICORDataEncoder encoder = new JacksonLICORDataEncoder(list, _bufferSize, _useBinary, _symbolTable);
    return EntityStreams.newEntityStream(encoder);
  }

  static JsonFactory getFactory(boolean encodeBinary)
  {
    return encodeBinary ? BINARY_FACTORY : TEXT_FACTORY;
  }
}
