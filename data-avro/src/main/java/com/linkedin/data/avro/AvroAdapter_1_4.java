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

