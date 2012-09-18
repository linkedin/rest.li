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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.JsonDecoder;
import org.apache.avro.io.JsonEncoder;

public class AvroUtil
{
  static public String jsonFromGenericRecord(GenericRecord record) throws IOException
  {
    GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<GenericRecord>();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Encoder jsonEncoder = new JsonEncoder(record.getSchema(), outputStream);
    writer.setSchema(record.getSchema());
    writer.write(record, jsonEncoder);
    jsonEncoder.flush();
    return outputStream.toString();
  }

  static public byte[] bytesFromGenericRecord(GenericRecord record) throws IOException
  {
    GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<GenericRecord>();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Encoder binaryEncoder = new BinaryEncoder(outputStream);
    writer.setSchema(record.getSchema());
    writer.write(record, binaryEncoder);
    binaryEncoder.flush();
    return outputStream.toByteArray();
  }

  static public GenericRecord genericRecordFromBytes(byte[] bytes, Schema schema) throws IOException
  {
    GenericDatumReader<GenericRecord> reader = new GenericDatumReader<GenericRecord>();
    Decoder binaryDecoder = DecoderFactory.defaultFactory().createBinaryDecoder(bytes, null);
    reader.setSchema(schema);
    GenericRecord record = reader.read(null, binaryDecoder);
    return record;
  }

  static public GenericRecord genericRecordFromJson(String json, Schema schema) throws IOException
  {
    GenericDatumReader<GenericRecord> reader = new GenericDatumReader<GenericRecord>();
    Decoder jsonDecoder = new JsonDecoder(schema, json);
    reader.setSchema(schema);
    GenericRecord record = reader.read(null, jsonDecoder);
    return record;
  }
}
