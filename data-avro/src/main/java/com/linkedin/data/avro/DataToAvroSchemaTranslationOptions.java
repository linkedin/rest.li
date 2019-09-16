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
import java.util.HashSet;
import java.util.Set;


/**
 * Options that affect the translation of {@link com.linkedin.data.schema.DataSchema} to Avro schema.
 */
public class DataToAvroSchemaTranslationOptions
{
  public static final OptionalDefaultMode DEFAULT_OPTIONAL_DEFAULT_MODE = OptionalDefaultMode.TRANSLATE_DEFAULT;
  public static final JsonBuilder.Pretty DEFAULT_PRETTY = JsonBuilder.Pretty.COMPACT;
  public static final EmbedSchemaMode DEFAULT_EMBED_SCHEMA_MODE = EmbedSchemaMode.NONE;

  /**
   * Default constructor.
   *
   * Sets optional default mode to {@link #DEFAULT_OPTIONAL_DEFAULT_MODE}.
   * Sets pretty mode to {@link #DEFAULT_PRETTY}.
   * Sets embed schema mode to {@link #DEFAULT_EMBED_SCHEMA_MODE}.
   */
  public DataToAvroSchemaTranslationOptions()
  {
    this(DEFAULT_OPTIONAL_DEFAULT_MODE, DEFAULT_PRETTY, DEFAULT_EMBED_SCHEMA_MODE);
  }

  /**
   * Constructor.
   *
   * Sets optional default mode to the specified value.
   *
   * @param optionalDefaultMode specifies the {@link OptionalDefaultMode}.
   */
  public DataToAvroSchemaTranslationOptions(OptionalDefaultMode optionalDefaultMode)
  {
    this(optionalDefaultMode, DEFAULT_PRETTY, DEFAULT_EMBED_SCHEMA_MODE);
  }

  /**
   * Constructor.
   *
   * Sets pretty mode to the specified value.
   *
   * @param pretty specifies the pretty mode.
   */
  public DataToAvroSchemaTranslationOptions(JsonBuilder.Pretty pretty)
  {
    this(DEFAULT_OPTIONAL_DEFAULT_MODE, pretty, DEFAULT_EMBED_SCHEMA_MODE);
  }

  /**
   * Constructor.
   *
   * Sets embedded schema to the specified value.
   *
   * @param embedSchemaMode specifies to the specified {@link EmbedSchemaMode}.
   */
  public DataToAvroSchemaTranslationOptions(EmbedSchemaMode embedSchemaMode )
  {
    this(DEFAULT_OPTIONAL_DEFAULT_MODE, DEFAULT_PRETTY, embedSchemaMode);
  }

  /**
   * Constructor.
   *
   * Sets Pegasus default field translation mode
   *
   * @param defaultFieldTranslationMode
   */
  public DataToAvroSchemaTranslationOptions(PegasusToAvroDefaultFieldTranslationMode defaultFieldTranslationMode)
  {
    this(DEFAULT_OPTIONAL_DEFAULT_MODE, DEFAULT_PRETTY, DEFAULT_EMBED_SCHEMA_MODE);
    this.setDefaultFieldTranslationMode(defaultFieldTranslationMode);
  }

  /**
   * Constructor.
   *
   * @param optionalDefaultMode specifies the optional default mode.
   * @param pretty specifies the pretty mode.
   */
  public DataToAvroSchemaTranslationOptions(OptionalDefaultMode optionalDefaultMode, JsonBuilder.Pretty pretty)
  {
    this(optionalDefaultMode, pretty, DEFAULT_EMBED_SCHEMA_MODE);
  }

  /**
   * Constructor.
   *
   * @param pretty specifies the pretty mode.
   * @param embedSchemaMode specifies the embed schema mode.
   */
  public DataToAvroSchemaTranslationOptions(JsonBuilder.Pretty pretty, EmbedSchemaMode embedSchemaMode)
  {
    this(DEFAULT_OPTIONAL_DEFAULT_MODE, pretty, embedSchemaMode);
  }

  /**
   * Constructor.
   *
   * @param optionalDefaultMode specifies the optional default mode.
   * @param pretty specifies the pretty mode.
   * @param embedSchemaMode specifies the embed schema mode.
   */
  public DataToAvroSchemaTranslationOptions(OptionalDefaultMode optionalDefaultMode, JsonBuilder.Pretty pretty, EmbedSchemaMode embedSchemaMode)
  {
    _optionalDefaultMode = optionalDefaultMode;
    _pretty = pretty;
    _embedSchemaMode = embedSchemaMode;
  }

  /**
   * Set the {@link OptionalDefaultMode}.
   *
   * @param mode provides the new {@link OptionalDefaultMode}.
   * @return {@code this}.
   */
  public DataToAvroSchemaTranslationOptions setOptionalDefaultMode(OptionalDefaultMode mode)
  {
    _optionalDefaultMode = mode;
    return this;
  }

  /**
   * Set default field translation mode.
   * The _defaultFieldTranslationMode is by default PegasusToAvroDefaultFieldTranslationMode.TRANSLATE
   *
   * By default, the schema translator will translate Pegasus default field to Avro default field
   * User can change it through this setter method so the schema translator will translate Pegasus default field to Avro optional field with no default value specified.
   * @param defaultFieldTranslationMode
   * @return {@code this}
   */
  public DataToAvroSchemaTranslationOptions setDefaultFieldTranslationMode(PegasusToAvroDefaultFieldTranslationMode defaultFieldTranslationMode) {
    _defaultFieldTranslationMode = defaultFieldTranslationMode;
    return this;
  }

  /**
   * Set namespace override option.
   * If the overrideNamespace is true, the namespace in avcs generated by pdsc will be prefixed with AVRO_PREFIX.
   *
   * @param overrideNamespace If the overrideNamespace is true,
   *                          the namespace in avcs generated by pdsc will be prefixed with AVRO_PREFIX.
   * @return {@code this}.
   */
  public DataToAvroSchemaTranslationOptions setOverrideNamespace(boolean overrideNamespace)
  {
    _overrideNamespace = overrideNamespace;
    return this;
  }

  /**
   * return override namespace option.
   *
   * @return override namespace option.
   */
  public boolean isOverrideNamespace()
  {
    return _overrideNamespace;
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

  /**
   * Return the translation mode for Default field
   * @return translation mode for default field
   */
  public PegasusToAvroDefaultFieldTranslationMode getDefaultFieldTranslationMode()
  {
    return _defaultFieldTranslationMode;
  }

  /**
   * Set the embed schema mode.
   */
  public DataToAvroSchemaTranslationOptions setEmbedSchemaMode(EmbedSchemaMode embedSchemaMode)
  {
    _embedSchemaMode = embedSchemaMode;
    return this;
  }

  /**
   * Returns the embed schema mode.
   */
  public EmbedSchemaMode getEmbeddedSchema()
  {
    return _embedSchemaMode;
  }

  /**
   * This list is a list of keywords that you want to exclude from translating from
   *  TypeRef to Avro schema.
   *
   * By default properties in TypeRef will be translated to Avro Schema when TypeRef are dereferenced
   * Here using this option,
   * user can provide a list of property keywords that don't want to be translated into Avro Schema
   *
   * @param typerefPropertiesExcludeSet sets of words used as excluding list
   */
  public void setTyperefPropertiesExcludeSet(Set<String> typerefPropertiesExcludeSet)
  {
    this.typerefPropertiesExcludeSet = typerefPropertiesExcludeSet;
  }

  public Set<String> getTyperefPropertiesExcludeSet()
  {
    return typerefPropertiesExcludeSet;
  }


  private OptionalDefaultMode _optionalDefaultMode;
  private JsonBuilder.Pretty  _pretty;
  private EmbedSchemaMode _embedSchemaMode;
  private PegasusToAvroDefaultFieldTranslationMode _defaultFieldTranslationMode =
      PegasusToAvroDefaultFieldTranslationMode.TRANSLATE;
  private boolean _overrideNamespace = false;
  private Set<String> typerefPropertiesExcludeSet = new HashSet<>();
}
