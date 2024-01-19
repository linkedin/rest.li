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

import com.linkedin.common.callback.Function;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.SchemaParserFactory;
import com.linkedin.data.schema.resolver.FileDataSchemaResolver;


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
   *
   * <p>Note that calling {@link #setDataSchemaResolverProvider(Function)} and this method are mutually exclusive since
   * both internally set the same DataSchemaResolver provider instance.</p>
   */
  public AvroToDataSchemaTranslationOptions setFileResolutionPaths(String schemaResolverPaths)
  {
    return setDataSchemaResolverProvider(schemaParserFactory -> {
      FileDataSchemaResolver resolver = new FileDataSchemaResolver(schemaParserFactory, schemaResolverPaths);
      resolver.setExtension(SchemaTranslator.AVRO_FILE_EXTENSION);
      return resolver;
    });
  }

  /**
   * Set the DataSchemaResolver provider to use.
   *
   * <p>Note that calling {@link #setFileResolutionPaths(String)} and this method are mutually exclusive since
   * both internally set the same DataSchemaResolver provider instance.</p>
   */
  public AvroToDataSchemaTranslationOptions setDataSchemaResolverProvider(
      Function<SchemaParserFactory, DataSchemaResolver> provider)
  {
    _schemaResolverProvider = provider;
    return this;
  }

  /**
   * Returns the Avro schema search paths, delimited by the default path separator.
   */
  public DataSchemaResolver getDataSchemaResolver(SchemaParserFactory schemaParserFactory)
  {
    return _schemaResolverProvider != null ? _schemaResolverProvider.map(schemaParserFactory) : null;
  }

  /**
   * Set if the translator should round trip translated schemas after translation by serializing and deserializing
   * them back or not.
   */
  public AvroToDataSchemaTranslationOptions setShouldRoundTripTranslatedSchemas(boolean roundTripTranslatedSchema)
  {
    _roundTripTranslatedSchema = roundTripTranslatedSchema;
    return this;
  }

  /**
   * Returns if the translator should round trip translated schemas after translation by serializing and deserializing
   * them back or not.
   */
  public boolean shouldRoundTripTranslatedSchemas()
  {
    return _roundTripTranslatedSchema;
  }

  private AvroToDataSchemaTranslationMode _translationMode;
  private Function<SchemaParserFactory, DataSchemaResolver> _schemaResolverProvider = null;
  private boolean _roundTripTranslatedSchema = false;
}
