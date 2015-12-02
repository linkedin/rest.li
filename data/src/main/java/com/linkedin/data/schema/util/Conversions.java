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

package com.linkedin.data.schema.util;


import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.schema.PegasusSchemaParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;


/**
 * Contains methods for converting among different schema representations.
 *
 * Package scope for now until we have more use for it.
 */
class Conversions
{
  // strings available to unit test
  static final String UNEXPECTED_IOEXCEPTION = "Unexpected IOException\n";
  static final String WRONG_NUMBER_OF_SCHEMA_LEFT = "No or too many resulting schemas, should have one resulting schema\n";

  /**
   * Return a {@link DataMap} representation of the provided {@link NamedDataSchema}.
   *
   * @param schema provides the {@link NamedDataSchema}.
   * @return a {@link DataMap} representation of the provided {@link NamedDataSchema}.
   */
  public static DataMap dataSchemaToDataMap(NamedDataSchema schema)
  {
    String inputSchemaAsString = schema.toString();
    try
    {
      JacksonDataCodec codec = new JacksonDataCodec();
      DataMap schemaAsDataMap = codec.stringToMap(inputSchemaAsString);
      return schemaAsDataMap;
    }
    catch (IOException e)
    {
      // This should never occur.
      // UTF-8 encoding should always be valid for getBytes
      // codec.readMap from JSON generated from a schema should always be successful.
      throw new IllegalStateException(UNEXPECTED_IOEXCEPTION + inputSchemaAsString, e);
    }
  }

  /**
   * Return a {@link DataSchema} for the provided {@link DataMap}.
   *
   * @param map provides the {@link DataMap} representation of a JSON-encoded {@link DataSchema}.
   * @param parser provides the {@link SchemaParser} that will be used for parsing the map representation of the schema.
   * @return a {@link DataSchema} for the provided {@link DataMap} or null if the map does not represent a valid schema,
   *         parse errors can be obtained from the provided {@link SchemaParser}.
   */
  public static DataSchema dataMapToDataSchema(DataMap map, PegasusSchemaParser parser)
  {
    // Convert DataMap into DataSchema
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    JacksonDataCodec codec = new JacksonDataCodec();
    try
    {
      codec.writeMap(map, outputStream);
    }
    catch (IOException e)
    {
      // This should never occur
      throw new IllegalStateException(UNEXPECTED_IOEXCEPTION + map, e);
    }
    ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    parser.parse(inputStream);
    List<DataSchema> topLevelDataSchemas = parser.topLevelDataSchemas();
    assert(topLevelDataSchemas.size() <= 1);
    if (parser.hasError())
    {
      return null;
    }
    else if (topLevelDataSchemas.size() != 1)
    {
      // This should never occur
      throw new IllegalStateException(WRONG_NUMBER_OF_SCHEMA_LEFT + topLevelDataSchemas);
    }
    return topLevelDataSchemas.get(0);
  }

}

