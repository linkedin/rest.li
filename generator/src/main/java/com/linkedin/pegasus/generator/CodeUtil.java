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

package com.linkedin.pegasus.generator;


import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.SchemaParserFactory;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.resolver.DefaultDataSchemaResolver;
import com.linkedin.data.schema.resolver.FileDataSchemaResolver;

import java.util.EnumSet;
import java.util.Set;


/**
 * Language-agnostic utility functions for data template code generation.
 *
 * @author Keren Jin
 */
public class CodeUtil
{
  public static class Pair<T0, T1>
  {
    public T0 first;
    public T1 second;

    public Pair(T0 first, T1 second)
    {
      this.first = first;
      this.second = second;
    }
  }

  // used by isDirectType to determine which types are direct vs wrapped.
  private static final Set<DataSchema.Type> _directTypes = EnumSet.of(DataSchema.Type.BOOLEAN,
                                                                      DataSchema.Type.INT,
                                                                      DataSchema.Type.LONG,
                                                                      DataSchema.Type.FLOAT,
                                                                      DataSchema.Type.DOUBLE,
                                                                      DataSchema.Type.STRING,
                                                                      DataSchema.Type.BYTES,
                                                                      DataSchema.Type.ENUM);

  /**
   * Create {@link DataSchemaResolver} with specified resolver path.
   *
   * @param resolverPath colon-separated string containing all paths of schema source to resolve
   * @return {@link DataSchemaResolver} configured with the resolver path
   */
  public static DataSchemaResolver createSchemaResolver(String resolverPath)
  {
    if (resolverPath == null)
    {
      return new DefaultDataSchemaResolver();
    }
    else
    {
      return new FileDataSchemaResolver(SchemaParserFactory.instance(), resolverPath);
    }
  }

  /**
   * Capitalize the input name.
   *
   * @param name the string whose first character will be converted to uppercase
   * @return the converted name
   */
  public static String capitalize(String name)
  {
    if (name == null || name.isEmpty())
    {
      return name;
    }
    else
    {
      return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
  }

  /**
   * Determine if the {@link DataSchema} requires wrapping or not.
   *
   * @param schema to be tested
   * @return true if no wrapping is required
   */
  public static boolean isDirectType(DataSchema schema)
  {
    return _directTypes.contains(schema.getDereferencedType());
  }

  /**
   * Return the union member name for the specified member {@link DataSchema}.
   *
   * @param memberType {@link DataSchema} for the member
   * @return result union member name
   */
  public static String getUnionMemberName(DataSchema memberType)
  {
    String name;
    if (memberType.getType() == DataSchema.Type.TYPEREF)
    {
      name = ((TyperefDataSchema) memberType).getName();
    }
    else
    {
      name = memberType.getUnionMemberKey();
      final int lastIndex = name.lastIndexOf('.');
      if (lastIndex >= 0)
      {
        name = name.substring(lastIndex + 1);
      }
    }
    return capitalize(name);
  }
}
