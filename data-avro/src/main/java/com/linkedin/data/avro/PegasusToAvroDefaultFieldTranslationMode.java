/*
   Copyright (c) 2019 LinkedIn Corp.

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
 *
 * This enum is used as option for both {@link DataMapToAvroRecordTranslationOptions} and {@link DataToAvroSchemaTranslationOptions}
 * and when setting the enum value for {@link DataMapToAvroRecordTranslationOptions} and {@link DataToAvroSchemaTranslationOptions}
 * during schema translation and corresponding data translation, the value should be the same.
 *
 * <p>
 * <b>When used for Pegasus DataSchema to Avro Schema translation:</b>
 *
 * Provides an option to translate required Pegasus fields with defaults to either default or optional fields in Avro
 * during pegasus schema {@link com.linkedin.data.schema.DataSchema}
 * to Avro Schema {@link org.apache.avro.Schema} translation
 *
 * By default, the schema translator will translate "required and default" field in pegasus schema
 * to a field in Avro Schema which should also be "required and default"
 *
 * Users can use this mode to opt to translate "default" Pegasus field into a "optional" field avro field
 * by setting this mode and pass it to {@link SchemaTranslator} through {@link DataToAvroSchemaTranslationOptions} during schema translation.
 *
 * This option should have no impact on
 * (1) any fields in the schema with no default value.
 * (2) or optional field. For optional field, see {@link OptionalDefaultMode}.
 * </p>
 *
 * <p>
 * <b>When used for DataMap to AvroRecord translation:</b>
 *
 * Depends on whether there is value present for the default field in DataMap {@link com.linkedin.data.DataMap}, user can choose this mode to
 * specify whether the default value for Pegasus schema field should be filled to Avro record {@link org.apache.avro.generic.GenericRecord},
 * during DataMap {@link com.linkedin.data.DataMap} translation to Avro record
 * </p>
 *
 *
 */
public enum PegasusToAvroDefaultFieldTranslationMode
{
  /**
   * <p>
   * <b>When used for Pegasus DataSchema to Avro Schema translation:</b>
   * Translate a field with default value in Pegasus schema to a field with "default" value in Avro schema
   * Concrete use cases could be:
   *  (1) Translate "required and default" field in Pegasus to "required and default" field in Avro schema
   *  (2) Translate "optional and default" field in Pegasus to "optional and default" field in Avro schema
   * This is the default behavior
   * </p>
   *
   * <p>
   * <b>When used for DataMap to AvroRecord translation:</b>
   * Translate the default field in Pegasus schema as an default field in Avro record, i.e.
   * if value present in {@link com.linkedin.data.DataMap} for the field, translate it to the Avro record
   * if no value present in {@link com.linkedin.data.DataMap}, default value will be translated into Avro record
   *
   * This is the default behavior if not otherwise specified
   * </p>
   */
  TRANSLATE,
  /**
   * <p>
   * <b>When used for Pegasus DataSchema to Avro Schema translation:</b>
   * Translate a required Pegasus schema field with default value to an "optional" Avro schema field
   * which will have no default value, i.e. "null" is used as default
   * </p>
   *
   *
   * <p>
   * <b>When used for DataMap to AvroRecord translation:</b>
   * Translate the default field as an optional, i.e.
   * if value present in {@link com.linkedin.data.DataMap} for the field, translate it and fill into the Avro record
   * if no value present in {@link com.linkedin.data.DataMap} , no value will be filled into Avro record
   * </p>
   */
  DO_NOT_TRANSLATE,
}
