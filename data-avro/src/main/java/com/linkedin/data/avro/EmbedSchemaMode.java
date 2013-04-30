package com.linkedin.data.avro;


/**
 * Determines whether input schema is embedded in the translated schema.
 */
public enum EmbedSchemaMode
{
  /**
   * Original input schema is not embedded in translated schema.
   */
  NONE,
  /**
   * Original input schema is embedded in the root output schema.
   */
  ROOT_ONLY
};
