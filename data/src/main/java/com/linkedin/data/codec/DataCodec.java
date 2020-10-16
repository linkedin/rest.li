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


import com.linkedin.data.ByteString;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.util.FastByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface for a coder and decoder that serializes and de-serializes
 * {@link DataMap}'s or {@link DataList}'s to and from binary data.
 *
 * The input binary data may be a byte array or an {@link InputStream}.
 * The output binary data may be a byte array or an {@link OutputStream}.
 *
 * @author slim
 */
public interface DataCodec
{
  /**
   * Serialize a {@link DataMap} to a byte array.
   *
   * @param map to serialize.
   * @return the output serialized from the {@link DataMap}.
   * @throws IOException if there is a serialization error.
   */
  byte[] mapToBytes(DataMap map) throws IOException;

  /**
   * Serialize a {@link DataList} to a byte array.
   *
   * @param list to serialize.
   * @return the output serialized from the {@link DataList}.
   * @throws IOException if there is a serialization error.
   */
  byte[] listToBytes(DataList list) throws IOException;

  /**
   * Serialize a {@link DataMap} to a {@link ByteString}.
   *
   * @param map to serialize.
   * @return the output serialized from the {@link DataMap}.
   * @throws IOException if there is a serialization error.
   */
  default ByteString mapToByteString(DataMap map) throws IOException
  {
    FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream();
    writeMap(map, outputStream);
    return outputStream.toUnsafeByteString();
  }

  /**
   * Serialize a {@link DataList} to a {@link ByteString}
   *
   * @param list to serialize.
   * @return the output serialized from the {@link DataList}.
   * @throws IOException if there is a serialization error.
   */
  default ByteString listToByteString(DataList list) throws IOException
  {
    FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream();
    writeList(list, outputStream);
    return outputStream.toUnsafeByteString();
  }

  /**
   * De-serialize a byte array to a {@link DataMap}.
   *
   * @param input to de-serialize.
   * @return the {@link DataMap} de-serialized from the input.
   * @throws IOException if there is a de-serialization error.
   */
  DataMap bytesToMap(byte[] input) throws IOException;

  /**
   * De-serialize a byte array to a {@link DataList}.
   *
   * @param input to de-serialize.
   * @return the {@link DataList} de-serialized from the input.
   * @throws IOException if there is a de-serialization error.
   */
  DataList bytesToList(byte[] input) throws IOException;

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
   * @return a {@link DataMap} representation of read from the {@link InputStream}.
   * @throws IOException if there is an error during de-serialization.
   */
  DataMap readMap(InputStream in) throws IOException;

  /**
   * Writes a {@link DataList} to the supplied {@link OutputStream}.
   *
   * @param list the map to write to {@code out}
   * @param out the {@link OutputStream} to write to
   * @throws IOException if there is an error during serialization
   */
  void writeList(DataList list, OutputStream out) throws IOException;

  /**
   * Returns a {@link DataList} from data consumed from the given {@link InputStream}.
   *
   * @param in the {@link InputStream} from which to read.
   * @return a {@link DataList} representation of read from the {@link InputStream}.
   * @throws IOException if there is an error during de-serialization.
   */
  DataList readList(InputStream in) throws IOException;

  /**
   * Returns a {@link DataMap} from data consumed from the given {@link ByteString}.
   *
   * @param in the {@link ByteString} from which to read.
   * @return a {@link DataMap} representation of read from the {@link ByteString}.
   * @throws IOException if there is an error during de-serialization.
   */
  default DataMap readMap(ByteString in) throws IOException
  {
    return readMap(in.asInputStream());
  }

  /**
   * Returns a {@link DataList} from data consumed from the given {@link ByteString}.
   *
   * @param in the {@link ByteString} from which to read.
   * @return a {@link DataList} representation of read from the {@link ByteString}.
   * @throws IOException if there is an error during de-serialization.
   */
  default DataList readList(ByteString in) throws IOException
  {
    return readList(in.asInputStream());
  }

  /**
   * Close the given closeable, silently swallowing any {@link IOException} that arises as a result of
   * invoking {@link Closeable#close()}.
   */
  static void closeQuietly(Closeable closeable)
  {
    if (closeable != null)
    {
      try
      {
        closeable.close();
      }
      catch (IOException e)
      {
        // TODO: use Java 7 try-with-resources statement and Throwable.getSuppressed()
      }
    }
  }
}
