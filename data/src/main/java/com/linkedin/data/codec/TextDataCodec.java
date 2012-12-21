/*
   Copyright (c) 2012 LinkedIn Corp.

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

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * Interface for a coder and decoder that serializes and de-serializes
 * {@link DataMap}'s or {@link DataList}'s to and from readable text.
 *
 * The input text may be a string or a {@link Reader}.
 * The output text may be a string or a {@link Writer}.
 *
 * @author slim
 */
public interface TextDataCodec extends DataCodec
{
  /**
   * Return the encoding used to encode text to bytes, usually it is UTF-8.
   *
   * <p>
   * This method provides the encoding used to serialize output text to bytes by
   * methods such as {@link #mapToString(com.linkedin.data.DataMap)} and
   * {@link #writeMap(com.linkedin.data.DataMap, java.io.OutputStream)}.
   * This encoding is also the encoding for bytes provided as input to
   * methods such as {@link #bytesToMap(byte[])} and {@link #readMap(java.io.InputStream)}.
   *
   * @return the encoding used to encode text to bytes.
   */
  public String getStringEncoding();

  /**
   * Serialize a {@link DataMap} to a string.
   *
   * @param map to serialize.
   * @return the output serialized from the {@link DataMap}.
   * @throws IOException if there is a serialization error.
   */
  public String mapToString(DataMap map) throws IOException;

  /**
   * Serialize a {@link DataList} to a string.
   *
   * @param list to serialize.
   * @return the output serialized from the {@link DataList}.
   * @throws IOException if there is a serialization error.
   */
  public String listToString(DataList list) throws IOException;

  /**
   * De-serialize a string to a {@link DataMap}.
   *
   * @param input to de-serialize.
   * @return the {@link DataMap} de-serialized from the input.
   * @throws IOException if there is a de-serialization error.
   */
  public DataMap stringToMap(String input) throws IOException;

  /**
   * De-serialize a string to a {@link DataList}.
   *
   * @param input to de-serialize.
   * @return the {@link DataList} de-serialized from the input.
   * @throws IOException if there is a de-serialization error.
   */
  public DataList stringToList(String input) throws IOException;

  /**
   * Writes a {@link DataMap} to the supplied {@link Writer}.
   *
   * @param map the map to write to {@code out}
   * @param out the {@link Writer} to write to
   * @throws IOException if there is an error during serialization
   */
  void writeMap(DataMap map, Writer out) throws IOException;

  /**
   * Returns a {@link DataMap} from data consumed from the given {@link Reader}.
   *
   * @param in the {@link Reader} from which to read.
   * @return a {@link DataMap} representation read from the {@link Reader}.
   * @throws IOException if there is an error during de-serialization.
   */
  DataMap readMap(Reader in) throws IOException;

  /**
   * Writes a {@link DataList} to the supplied {@link Writer}.
   *
   * @param list the map to write to {@code out}
   * @param out the {@link Writer} to write to
   * @throws IOException if there is an error during serialization
   */
  void writeList(DataList list, Writer out) throws IOException;

  /**
   * Returns a {@link DataList} from data consumed from the given {@link Reader}.
   *
   * @param in the {@link Reader} from which to read.
   * @return a {@link DataList} representation read from the {@link Reader}.
   * @throws IOException if there is an error during de-serialization.
   */
  DataList readList(Reader in) throws IOException;
}
