package com.linkedin.data.avro;

import com.linkedin.data.schema.DataSchema;
import java.io.IOException;
import java.io.OutputStream;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;

/**
 * Adapter for Avro 1.6
 */
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
