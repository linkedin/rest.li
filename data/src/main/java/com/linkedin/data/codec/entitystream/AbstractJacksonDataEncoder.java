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
import com.fasterxml.jackson.core.JsonGenerator;
import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.AbstractJacksonDataCodec;
import java.io.IOException;
import java.io.OutputStream;


/**
 * Abstract JSON and JSON-like data type encoder for a {@link com.linkedin.data.DataComplex} object implemented as a
 * {@link com.linkedin.entitystream.Writer} writing to an {@link com.linkedin.entitystream.EntityStream} of
 * {@link ByteString}. The implementation is backed by Jackson's {@link JsonGenerator} or it's subclasses for JSON like
 * formats. The <code>JsonGenerator</code> writes to an internal non-blocking <code>OutputStream</code>
 * implementation that has a fixed-size primary buffer and an unbounded overflow buffer. Because the bytes are pulled
 * from the encoder asynchronously, it needs to keep the state in a stack.
 *
 * @author kramgopa, xma
 */
abstract class AbstractJacksonDataEncoder extends AbstractDataEncoder
{
  protected JsonFactory _jsonFactory;
  protected JsonGenerator _generator;

  protected AbstractJacksonDataEncoder(JsonFactory jsonFactory, DataMap dataMap, int bufferSize)
  {
    super(dataMap, bufferSize);
    _jsonFactory = jsonFactory;
  }

  protected AbstractJacksonDataEncoder(JsonFactory jsonFactory, DataList dataList, int bufferSize)
  {
    super(dataList, bufferSize);
    _jsonFactory = jsonFactory;
  }

  @Override
  protected Data.TraverseCallback createTraverseCallback(OutputStream out) throws IOException
  {
    _generator = _jsonFactory.createGenerator(out);
    return new JacksonStreamTraverseCallback(_generator);
  }

  /**
   * method is moved to @Data.TraverseCallback. Extend JacksonStreamTraverseCallback to override method behaviour.
   * @throws IOException
   */
  @Deprecated
  protected void writeStartObject() throws IOException
  {
    _generator.writeStartObject();
  }

  /**
   * method is moved to @Data.TraverseCallback. Extend JacksonStreamTraverseCallback to override method behaviour.
   * @throws IOException
   */
  @Deprecated
  protected void writeStartArray() throws IOException
  {
    _generator.writeStartArray();
  }

  /**
   * method is moved to @Data.TraverseCallback. Extend JacksonStreamTraverseCallback to override method behaviour.
   * @throws IOException
   */
  @Deprecated
  protected void writeFieldName(String name) throws IOException
  {
    _generator.writeFieldName(name);
  }

  /**
   * method is moved to @Data.TraverseCallback. Extend JacksonStreamTraverseCallback to override method behaviour.
   * @throws IOException
   */
  @Deprecated
  protected void writeEndObject() throws IOException
  {
    _generator.writeEndObject();
  }

  /**
   * method is moved to @Data.TraverseCallback. Extend JacksonStreamTraverseCallback to override method behaviour.
   * @throws IOException
   */
  @Deprecated
  protected void writeEndArray() throws IOException
  {
    _generator.writeEndArray();
  }

  /**
   * method is moved to @Data.TraverseCallback. Extend JacksonStreamTraverseCallback to override method behaviour.
   * @throws IOException
   */
  @Deprecated
  protected void writeByteString(ByteString value) throws IOException
  {
    char[] charArray = value.asAvroCharArray();
    _generator.writeString(charArray, 0, charArray.length);
  }

  protected class JacksonStreamTraverseCallback extends AbstractJacksonDataCodec.JacksonTraverseCallback
  {

    protected JacksonStreamTraverseCallback(JsonGenerator generator)
    {
      super(generator);
    }

    @Override
    public void byteStringValue(ByteString value) throws IOException
    {
      writeByteString(value);
    }

    @Override
    public void startMap(DataMap map) throws IOException
    {
      writeStartObject();
    }

    @Override
    public void key(String key) throws IOException
    {
      writeFieldName(key);
    }

    @Override
    public void endMap() throws IOException
    {
      writeEndObject();
    }

    @Override
    public void startList(DataList list) throws IOException
    {
      writeStartArray();
    }

    @Override
    public void endList() throws IOException
    {
      writeEndArray();
    }
  }
}
