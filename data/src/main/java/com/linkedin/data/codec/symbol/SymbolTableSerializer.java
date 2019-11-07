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

package com.linkedin.data.codec.symbol;

import com.linkedin.data.ByteString;
import com.linkedin.data.DataList;
import com.linkedin.data.codec.DataCodec;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;


/**
 * Utilities to serialize/deserialize a symbol table to/from raw bytes.
 *
 * <p>Symbol tables are serialized as {@link DataList}s with the first element containing the symbol table name,
 * followed by an ordered list of symbols.</p>
 */
public class SymbolTableSerializer
{
  private SymbolTableSerializer()
  {
    // Prevent external instantiation.
  }

  /**
   * Deserialize a symbol table.
   *
   * @param byteString    The serialized representation.
   * @param codec         The {@link DataCodec} to use to deserialize.
   * @return The deserialized table.
   * @throws IOException  If any exception occurred during deserialization.
   */
  public static SymbolTable fromByteString(ByteString byteString, DataCodec codec) throws IOException
  {
    return fromByteString(byteString, codec, null);
  }

  /**
   * Deserialize a symbol table.
   *
   * @param byteString               The serialized representation.
   * @param codec                    The {@link DataCodec} to use to deserialize.
   * @param symbolTableRenamer       An optional function to rename the deserialized symbol table.
   * @return The deserialized table.
   * @throws IOException  If any exception occurred during deserialization.
   */
  @SuppressWarnings("unchecked")
  public static SymbolTable fromByteString(ByteString byteString, DataCodec codec, Function<String, String> symbolTableRenamer) throws IOException
  {
    DataList dataList = codec.readList(byteString.asInputStream());
    String symbolTableName = (String) dataList.get(0);
    if (symbolTableRenamer != null)
    {
      symbolTableName = symbolTableRenamer.apply(symbolTableName);
    }

    return new InMemorySymbolTable(symbolTableName, (List<String>)(List<?>) dataList.subList(1, dataList.size()));
  }

  /**
   * Serialize a symbol table.
   *
   * @param codec                    The {@link DataCodec} to use to serialize.
   * @param symbolTable              The symbol table to serialize.
   * @return The serialized table.
   * @throws IOException  If any exception occurred during serialization.
   */
  public static ByteString toByteString(DataCodec codec, SymbolTable symbolTable) throws IOException
  {
    DataList dataList = new DataList(symbolTable.size() + 1);
    dataList.add(symbolTable.getName());
    for (int i = 0; i < symbolTable.size(); i++)
    {
      dataList.add(symbolTable.getSymbolName(i));
    }

    return ByteString.unsafeWrap(codec.listToBytes(dataList));
  }
}
