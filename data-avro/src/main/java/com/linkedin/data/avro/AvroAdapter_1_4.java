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
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.JsonDecoder;
import org.apache.avro.io.JsonEncoder;


/**
 * @deprecated Use {@link com.linkedin.avro.compatibility.AvroCompatibilityHelper} instead.
 */
@Deprecated
public class AvroAdapter_1_4 implements AvroAdapter
{
  @Override
  public boolean jsonUnionMemberHasFullName()
  {
    return false;
  }

  @Override
  public GenericData.EnumSymbol createEnumSymbol(Schema avroSchema, String enumValue)
  {
    return new GenericData.EnumSymbol(enumValue);
  }

  @Override
  public Schema stringToAvroSchema(String avroSchemaJson)
  {
    return Schema.parse(avroSchemaJson);
  }

  @Override
  public Decoder createBinaryDecoder(byte[] bytes) throws IOException
  {
    Decoder binaryDecoder = DecoderFactory.defaultFactory().createBinaryDecoder(bytes, null);
    return binaryDecoder;
  }

  @Override
  public Encoder createBinaryEncoder(OutputStream outputStream) throws IOException
  {
    Encoder binaryEncoder = new BinaryEncoder(outputStream);
    return binaryEncoder;
  }

  @Override
  public Decoder createJsonDecoder(Schema schema, String json) throws IOException
  {
    Decoder jsonDecoder = new JsonDecoder(schema, json);
    return jsonDecoder;
  }

  @Override
  public Encoder createJsonEncoder(Schema schema, OutputStream outputStream) throws IOException
  {
    Encoder jsonEncoder = new JsonEncoder(schema, outputStream);
    return jsonEncoder;
  }
}

