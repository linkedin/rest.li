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
import com.linkedin.data.it.Builder;
import com.linkedin.data.it.IterationOrder;
import com.linkedin.data.it.Predicate;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.SchemaParser;

import static com.linkedin.data.schema.util.Conversions.dataMapToDataSchema;
import static com.linkedin.data.schema.util.Conversions.dataSchemaToDataMap;


/**
 * Provides methods for filtering a {@link DataSchema} by removing unwanted fields or custom properties of the schema.
 */
public class Filters
{
  // strings available to unit test
  static final String NO_SCHEMA_LEFT = "No schema is left after filtering, root object has been removed\n";
  static final String INVALID_SCHEMA_LEFT = "DataMap left after filtering is not a valid schema\n";

  /**
   * Remove parts of a {@link NamedDataSchema} that matches the specified predicate.
   *
   * This method obtains a {@link DataMap} representation of the {@link NamedDataSchema}
   * by invoking {@link com.linkedin.data.schema.util.Conversions#dataSchemaToDataMap}.
   * Then it performs an pre-order traversal of this {@link DataMap} and evaluates the
   * provided predicate on each Data object visited. If the predicate evaluates to true, the
   * matching Data object will be removed. After the {@link DataMap} has been traversed and
   * matching Data objects have been removed, the provided {@link SchemaParser} will be used
   * to parse the JSON representation of the filtered {@link DataMap}. If there are no
   * parsing errors, this method returns the {@link NamedDataSchema} parsed from the JSON representation.
   *
   * If there are parsing errors, the errors may be obtained from the provided {@link SchemaParser}.
   *
   * @param schema provides the {@link NamedDataSchema} to be filtered.
   * @param predicate provides the {@link Predicate} to be evaluated.
   * @param parser provides the {@link SchemaParser} to be used to parse the filtered {@link DataMap}.
   * @return a filtered {@link NamedDataSchema} if the filtered schema is valid, else return null.
   */
  public static NamedDataSchema removeByPredicate(NamedDataSchema schema, Predicate predicate, SchemaParser parser)
  {
    DataMap schemaAsDataMap = dataSchemaToDataMap(schema);

    DataMap map = (DataMap) Builder.create(schemaAsDataMap, null, IterationOrder.PRE_ORDER).filterBy(predicate).remove();
    if (map == null)
    {
      parser.errorMessageBuilder().append(NO_SCHEMA_LEFT);
    }

    DataSchema resultDataSchema = dataMapToDataSchema(map, parser);
    if (resultDataSchema == null)
    {
      parser.errorMessageBuilder().append(INVALID_SCHEMA_LEFT + map);
    }
    return (NamedDataSchema) resultDataSchema;
  }
}
