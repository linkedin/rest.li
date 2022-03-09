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


import com.linkedin.avroutil1.compatibility.AvroCompatibilityHelper;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;


/**
 * @deprecated Use {@link com.linkedin.avroutil1.compatibility.AvroCompatibilityHelper} instead.
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
    return AvroCompatibilityHelper.newEnumSymbol(avroSchema, enumValue);
  }

  @Override
  public Schema stringToAvroSchema(String avroSchemaJson)
  {
    return Schema.parse(avroSchemaJson);
  }

  @Override
  public Decoder createBinaryDecoder(byte[] bytes) throws IOException
  {
    return AvroCompatibilityHelper.newBinaryDecoder(bytes);
  }

  @Override
  public Encoder createBinaryEncoder(OutputStream outputStream) throws IOException
  {
    return AvroCompatibilityHelper.newBinaryEncoder(outputStream);
  }

  @Override
  public Decoder createJsonDecoder(Schema schema, String json) throws IOException
  {
    return AvroCompatibilityHelper.newJsonDecoder(schema, json);
  }

  @Override
  public Encoder createJsonEncoder(Schema schema, OutputStream outputStream) throws IOException
  {
    return AvroCompatibilityHelper.newJsonEncoder(schema, outputStream, true);
  }
}

