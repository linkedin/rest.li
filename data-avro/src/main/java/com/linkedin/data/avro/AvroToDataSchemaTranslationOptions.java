package com.linkedin.data.avro;


/**
 * Options that affect the translation of Avro schema to {@link com.linkedin.data.schema.DataSchema}.
 */
public class AvroToDataSchemaTranslationOptions
{
  public static final AvroToDataSchemaTranslationMode DEFAULT_AVRO_TRANSLATION_MODE = AvroToDataSchemaTranslationMode.RETURN_EMBEDDED_SCHEMA;

  /**
   * Default constructor.
   *
   * Sets translation mode to {@link #DEFAULT_AVRO_TRANSLATION_MODE}.
   */
  public AvroToDataSchemaTranslationOptions()
  {
    this(DEFAULT_AVRO_TRANSLATION_MODE);
  }

  /**
   * Constructor.
   *
   * @param translationMode provides the Avro to Data schema translation mode.
   */
  public AvroToDataSchemaTranslationOptions(AvroToDataSchemaTranslationMode translationMode)
  {
    _translationMode = translationMode;
  }

  /**
   * Set the Avro to Data schema translation mode.
   */
  public AvroToDataSchemaTranslationOptions setTranslationMode(AvroToDataSchemaTranslationMode mode)
  {
    _translationMode = mode;
    return this;
  }

  /**
   * Returns the Avro to Data schema translation mode.
   */
  public AvroToDataSchemaTranslationMode getTranslationMode()
  {
    return _translationMode;
  }

  private AvroToDataSchemaTranslationMode _translationMode;
}
