package com.linkedin.data.avro;


/**
 * Mode to use when translation from {@link com.linkedin.data.schema.DataSchema} to Avro schema.
 */
public enum AvroToDataSchemaTranslationMode
{
  /**
   * Always perform translation.
   * <p>
   * If embedded schema is present, it is ignored.
   */
  TRANSLATE,
  /**
   * If embedded schema is available, return the embedded schema without verification.
   */
  RETURN_EMBEDDED_SCHEMA,
  /**
   * If embedded schema is available, return the embedded schema after verifying that it translates the
   * provided Avro schema. If verification fails, then throw {@link IllegalArgumentException}.
   */
  VERIFY_EMBEDDED_SCHEMA
}
