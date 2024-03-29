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

package com.linkedin.data.avro.util;

import com.linkedin.avroutil1.compatibility.AvroCompatibilityHelper;
import com.linkedin.avroutil1.compatibility.AvroVersion;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;


/**
 * @deprecated please use {@link com.linkedin.avroutil1.compatibility.AvroCodecUtil}
 */
@Deprecated
public class AvroUtil
{
  public static String jsonFromGenericRecord(GenericRecord record) throws IOException
  {
    return jsonFromGenericRecord(record, true);
  }

  public static String jsonFromGenericRecord(GenericRecord record, boolean pretty) throws IOException
  {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    return jsonFromGenericRecord(record,
        outputStream,
        AvroCompatibilityHelper.newJsonEncoder(record.getSchema(), outputStream, pretty));

  }

  public static String jsonFromGenericRecord(GenericRecord record, boolean pretty, AvroVersion version) throws IOException
  {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    return jsonFromGenericRecord(record,
        outputStream,
        AvroCompatibilityHelper.newJsonEncoder(record.getSchema(), outputStream, pretty, version));
  }

  private static String jsonFromGenericRecord(
      GenericRecord record,
      ByteArrayOutputStream outputStream,
      Encoder jsonEncoder) throws IOException
  {
    GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<>();
    writer.setSchema(record.getSchema());
    writer.write(record, jsonEncoder);
    jsonEncoder.flush();
    return outputStream.toString(StandardCharsets.UTF_8.name());
  }

  public static byte[] bytesFromGenericRecord(GenericRecord record) throws IOException
  {
    GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<>();
    ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
    Encoder binaryEncoder = AvroCompatibilityHelper.newBinaryEncoder(byteOutputStream, false, null);
    writer.setSchema(record.getSchema());
    writer.write(record, binaryEncoder);
    binaryEncoder.flush();
    return byteOutputStream.toByteArray();
  }

  public static GenericRecord genericRecordFromBytes(byte[] bytes, Schema schema) throws IOException
  {
    GenericDatumReader<GenericRecord> reader = new GenericDatumReader<>();
    Decoder binaryDecoder = AvroCompatibilityHelper.newBinaryDecoder(
        new ByteArrayInputStream(bytes), false, null);
    reader.setSchema(schema);
    GenericRecord record = reader.read(null, binaryDecoder);
    return record;
  }

  public static GenericRecord genericRecordFromJson(String json, Schema schema) throws IOException
  {
    GenericDatumReader<GenericRecord> reader = new GenericDatumReader<>(schema, schema);
    Decoder jsonDecoder = AvroCompatibilityHelper.newCompatibleJsonDecoder(schema, json);
    GenericRecord record = reader.read(null, jsonDecoder);
    return record;
  }
}
