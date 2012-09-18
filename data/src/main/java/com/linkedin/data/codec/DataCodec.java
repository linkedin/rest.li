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


import com.linkedin.data.DataMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface for a coder and decoder that serializes a {@link DataMap} to a byte array
 * and de-serializes byte array to a {@link DataMap}.
 *
 * @author slim
 */
public interface DataCodec
{
  /**
   * Return the encoding used to encode serialized strings, usually it is UTF-8.
   *
   * @return the encoding used to encode serialized strings.
   */
  public String getStringEncoding();
  /**
   * Serialize a {@link DataMap} to a byte array.
   *
   * @param map to serialize.
   * @return the output serialized from the {@link DataMap}.
   * @throws IOException if there is a serialization error.
   */
  public byte[] mapToBytes(DataMap map) throws IOException;
  /**
   * De-serialize a byte array to a DataMap.
   *
   * @param input to de-serialize.
   * @return the DataMap de-serialized from the input.
   * @throws IOException if there is a de-serialization error.
   */
  public DataMap bytesToMap(byte[] input) throws IOException;

  /**
   * Writes a {@link DataMap} to the supplied {@link OutputStream}.
   *
   * @param map the map to write to {@code out}
   * @param out the {@link OutputStream} to write to
   * @throws IOException if there is an error during serialization
   */
  void writeMap(DataMap map, OutputStream out) throws IOException;

  /**
   * Returns a {@link DataMap} from data consumed from the given {@link InputStream}.
   *
   * @param in the {@link InputStream} from which to read.
   * @return a {@link DataMap} representation of the {@link InputStream}.
   * @throws IOException if there is an error during de-serialization.
   */
  DataMap readMap(InputStream in) throws IOException;
}
