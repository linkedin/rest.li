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

package com.linkedin.data.avro;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;

/**
 * Adapter for Avro 1.6
 * @deprecated use Use {@link com.linkedin.avro.compatibility.AvroCompatibilityHelper} instead.
 */
@Deprecated
public class AvroAdapter_1_6 implements AvroAdapter
{
  private final DecoderFactory _decoderFactory = DecoderFactory.get();
  private final EncoderFactory _encoderFactory = EncoderFactory.get();

  @Override
  public boolean jsonUnionMemberHasFullName()
  {
    return true;
  }

  @Override
  public GenericData.EnumSymbol createEnumSymbol(Schema avroSchema, String enumValue)
  {
    return new GenericData.EnumSymbol(avroSchema, enumValue);
  }

  @Override
  public Schema stringToAvroSchema(String avroSchemaJson)
  {
    return new Schema.Parser().parse(avroSchemaJson);
  }

  @Override
  public Decoder createBinaryDecoder(byte[] bytes) throws IOException
  {
    return _decoderFactory.binaryDecoder(bytes, null);
  }

  @Override
  public Encoder createBinaryEncoder(OutputStream outputStream) throws IOException
  {
    return _encoderFactory.binaryEncoder(outputStream, null);
  }

  @Override
  public Decoder createJsonDecoder(Schema schema, String json) throws IOException
  {
    return _decoderFactory.jsonDecoder(schema, json);
  }

  @Override
  public Encoder createJsonEncoder(Schema schema, OutputStream outputStream) throws IOException
  {
    return _encoderFactory.jsonEncoder(schema, outputStream);
  }
}
