package com.linkedin.data.avro;


import java.io.IOException;
import java.io.OutputStream;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;

public class TestAvroAdapter implements AvroAdapter
{
  @Override
  public boolean jsonUnionMemberHasFullName()
  {
    return false;
  }

  @Override
  public GenericData.EnumSymbol createEnumSymbol(Schema avroSchema, String enumValue)
  {
    return null;
  }

  @Override
  public Schema stringToAvroSchema(String avroSchemaJson)
  {
    return null;
  }

  @Override
  public Decoder createBinaryDecoder(byte[] bytes)
    throws IOException
  {
    return null;
  }

  @Override
  public Encoder createBinaryEncoder(OutputStream outputStream)
    throws IOException
  {
    return null;
  }

  @Override
  public Decoder createJsonDecoder(Schema schema, String json)
    throws IOException
  {
    return null;
  }

  @Override
  public Encoder createJsonEncoder(Schema schema, OutputStream outputStream)
    throws IOException
  {
    return null;
  }
}
