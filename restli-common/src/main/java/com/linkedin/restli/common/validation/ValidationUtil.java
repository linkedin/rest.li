/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.common.validation;


import com.linkedin.data.element.DataElement;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.UnionDataSchema;

import java.util.StringTokenizer;

/**
 * Utility functions related to Rest.li data validation.
 *
 * @author Soojung Ha
 */
public class ValidationUtil
{
  /**
   * Determines whether a path is valid according to the given data schema.
   * The path must be a field of a record, and not an enum constant, a member of a union, etc.
   *
   * @param schema data schema
   * @param pathWithoutKeys full path for a field, without map key names and array indices.
   * @return true if path denotes a field in the data schema
   */
  public static boolean containsPath(DataSchema schema, String pathWithoutKeys)
  {
    return getField(schema, pathWithoutKeys) != null;
  }

  /**
   * Returns the Field corresponding to the path.
   *
   * @param schema data schema
   * @param pathWithoutKeys full path for a field, without map key names and array indices.
   * @return field corresponding to the path, or null if not found
   */
  public static RecordDataSchema.Field getField(DataSchema schema, String pathWithoutKeys)
  {
    StringTokenizer st = new StringTokenizer(pathWithoutKeys, DataElement.SEPARATOR.toString());
    DataSchema dataSchema = schema;
    RecordDataSchema.Field field;
    if (!st.hasMoreTokens())
    {
      return null;
    }
    String comp = st.nextToken();
    while (true)
    {
      dataSchema = dataSchema.getDereferencedDataSchema();
      if (dataSchema.getType() == DataSchema.Type.MAP)
      {
        dataSchema = ((MapDataSchema) dataSchema).getValues();
        // Parent is a map, so skipped component is a key name
        continue;
      }
      else if (dataSchema.getType() == DataSchema.Type.ARRAY)
      {
        dataSchema = ((ArrayDataSchema) dataSchema).getItems();
        // Parent is an array, so skipped component is an index
        continue;
      }
      else if (dataSchema.getType() == DataSchema.Type.RECORD)
      {
        field = ((RecordDataSchema) dataSchema).getField(comp);
        if (field == null) return null;
        if (!st.hasMoreTokens()) return field;
        dataSchema = field.getType();
      }
      else if (dataSchema.getType() == DataSchema.Type.UNION)
      {
        dataSchema = ((UnionDataSchema) dataSchema).getType(comp);
        if (dataSchema == null) return null;
      }
      else
      {
        // Parent cannot be a primitive type
        return null;
      }
      if (st.hasMoreTokens())
      {
        comp = st.nextToken();
      }
      else
      {
        break;
      }
    }
    return null;
  }
}
