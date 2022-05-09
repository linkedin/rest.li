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


import com.linkedin.data.Data;
import com.linkedin.data.schema.DataSchema;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecord;


/**
 * Custom Data translator for translating data to and from Avro and Pegasus representation.
 */
public interface CustomDataTranslator
{
  public static final Object AVRO_BAD_RESULT = null;
  public static final Object DATA_BAD_RESULT = Data.NULL;

  /**
   * Translate data in Avro generic representation to Pegasus Data representation.
   *
   * @param context for interacting with caller.
   * @param avroData is data in Avro representation.
   * @param avroSchema provides Avro schema of data.
   * @param schema provides the Pegasus schema of data.
   * @return the Pegasus Data representation of data.
   */
  Object avroGenericToData(DataTranslatorContext context, Object avroData, Schema avroSchema, DataSchema schema);

  /**
   * Translate data in Pegasus Data representation to Avro generic representation.
   *
   * @param context for interacting with caller.
   * @param data is data in Pegasus representation.
   * @param schema provides the Pegasus schema of data.
   * @param avroSchema provides Avro schema of data.
   * @return the Avro generic representation of data.
   */
  Object dataToAvroGeneric(DataTranslatorContext context, Object data, DataSchema schema, Schema avroSchema);

  default <T extends SpecificRecord> T dataToAvroSpecific(DataTranslatorContext context, Object data, DataSchema schema,
      Schema avroSchema) {
    return null;
  }
}
