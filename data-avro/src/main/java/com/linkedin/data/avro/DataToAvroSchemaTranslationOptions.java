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

package com.linkedin.data.avro;

import com.linkedin.data.schema.JsonBuilder;

/**
 * Options that affect the translation of {@link com.linkedin.data.schema.DataSchema} to Avro schema.
 */
public class DataToAvroSchemaTranslationOptions
{
  public static final OptionalDefaultMode DEFAULT_OPTIONAL_DEFAULT_MODE = OptionalDefaultMode.TRANSLATE_DEFAULT;
  public static final JsonBuilder.Pretty DEFAULT_PRETTY = JsonBuilder.Pretty.COMPACT;

  /**
   * Default constructor.
   *
   * Sets optional default mode to {@link #DEFAULT_OPTIONAL_DEFAULT_MODE}.
   * Sets pretty mode to {@link #DEFAULT_PRETTY}.
   */
  public DataToAvroSchemaTranslationOptions()
  {
    _optionalDefaultMode = DEFAULT_OPTIONAL_DEFAULT_MODE;
    _pretty = DEFAULT_PRETTY;
  }

  /**
   * Constructor.
   *
   * Sets optional default mode to the specified value.
   * Sets pretty mode to {@link #DEFAULT_PRETTY}.
   *
   * @param optionalDefaultMode specifies the {@link OptionalDefaultMode}.
   */
  public DataToAvroSchemaTranslationOptions(OptionalDefaultMode optionalDefaultMode)
  {
    _optionalDefaultMode = optionalDefaultMode;
    _pretty = DEFAULT_PRETTY;
  }

  /**
   * Constructor.
   *
   * Sets pretty mode to the specified value.
   * Sets optional default mode to {@link #DEFAULT_OPTIONAL_DEFAULT_MODE}.
   *
   * @param pretty specifies the pretty mode.
   */
  public DataToAvroSchemaTranslationOptions(JsonBuilder.Pretty pretty)
  {
    _optionalDefaultMode = DEFAULT_OPTIONAL_DEFAULT_MODE;
    _pretty = pretty;
  }

  /**
   * Constructor.
   *
   * @param optionalDefaultMode specifies the optional default mode.
   * @param pretty specifies the pretty mode.
   */
  public DataToAvroSchemaTranslationOptions(OptionalDefaultMode optionalDefaultMode, JsonBuilder.Pretty pretty)
  {
    _optionalDefaultMode = optionalDefaultMode;
    _pretty = pretty;
  }

  public DataToAvroSchemaTranslationOptions setOptionalDefaultMode(OptionalDefaultMode mode)
  {
    _optionalDefaultMode = mode;
    return this;
  }

  /**
   * Return how an optional field and associated default value should be translated.
   *
   * @return how an optional field and associated default value should be translated.
   */
  public OptionalDefaultMode getOptionalDefaultMode()
  {
    return _optionalDefaultMode;
  }

  /**
   * Set the pretty mode.
   *
   * @param pretty provides the new pretty mode.
   * @return {@code this}.
   */
  public DataToAvroSchemaTranslationOptions setPretty(JsonBuilder.Pretty pretty)
  {
    _pretty = pretty;
    return this;
  }

  /**
   * Return the pretty mode.
   *
   * @return the pretty mode.
   */
  public JsonBuilder.Pretty getPretty()
  {
    return _pretty;
  }

  private OptionalDefaultMode _optionalDefaultMode;
  private JsonBuilder.Pretty  _pretty;
}
