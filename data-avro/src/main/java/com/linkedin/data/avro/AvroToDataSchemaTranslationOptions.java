/*
   Copyright (c) 2013 LinkedIn Corp.

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

  /**
   * Set the Avro schema search paths, delimited by the default path separator.
   */
  public AvroToDataSchemaTranslationOptions setFileResolutionPaths(String schemaResolverPaths)
  {
    _schemaResolverPaths = schemaResolverPaths;
    return this;
  }

  /**
   * Returns the Avro schema search paths, delimited by the default path separator.
   */
  public String getFileResolutionPaths()
  {
    return _schemaResolverPaths;
  }

  private AvroToDataSchemaTranslationMode _translationMode;
  private String _schemaResolverPaths = null;
}
