package com.linkedin.data.avro;


import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;


/**
 * Adapter to handle incompatibilities among different versions of Avro.
 *
 * <p>
 * @see AvroAdapterFinder
 */
public interface AvroAdapter
{
  /* see AVRO-656 */
  boolean jsonUnionMemberHasFullName();

  GenericData.EnumSymbol createEnumSymbol(Schema avroSchema, String enumValue);

  Schema stringToAvroSchema(String avroSchemaJson);

  Decoder createBinaryDecoder(byte[] bytes) throws IOException;

  Encoder createBinaryEncoder(OutputStream outputStream) throws IOException;

  Decoder createJsonDecoder(Schema schema, String json) throws IOException;

  Encoder createJsonEncoder(Schema schema, OutputStream outputStream) throws IOException;
}
