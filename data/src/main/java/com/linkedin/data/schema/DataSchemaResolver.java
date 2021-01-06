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

package com.linkedin.data.schema;

import com.linkedin.data.schema.resolver.SchemaDirectoryName;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A {@link DataSchemaResolver} is used to resolve names to {@link NamedDataSchema}s.
 * <p>
 * A {@link SchemaParser} uses {@link #existingDataSchema(String)} to determine if a name has already
 * been resolved to a {@link NamedDataSchema}. The returned {@link NamedDataSchema} may not
 * be complete or well-formed due to circular references.
 * <p>
 * A {@link SchemaParser} uses {@link #findDataSchema(String, StringBuilder)} to request the
 * resolving an unresolved name to a {@link NamedDataSchema}. This may be done by searching
 * jar files or directories for files whose file names computed from the provided name
 * similar to how Java resolves class names to source or binary files.
 * <p>
 * The contract between a {@link SchemaParser} and its {@link DataSchemaResolver} requires
 * the {@link SchemaParser} to invoke {@link #bindNameToSchema(Name, NamedDataSchema, DataSchemaLocation)}
 * to bind all possible names (its declared name and aliases) to a newly constructed {@link NamedDataSchema}
 * before {@link SchemaParser} attempts to lookup or resolve names referenced by the {@link NamedDataSchema}.
 * This means that when the name is bound, the {@link NamedDataSchema} is incomplete. This contract
 * is required to implement circular references.
 * <p>
 *
 * @author slim
 */
public interface DataSchemaResolver
{
  /**
   * Return a map of names to {@link NamedDataSchema} bindings that have been resolved.
   *
   * @return map of names to {@link NamedDataSchema} bindings that have been resolved.
   */
  Map<String, NamedDataSchema> bindings();

  /**
   * Return a map of names to {@link DataSchemaLocation}s that have been
   * resolved through this resolver.
   *
   * @return a map of names to {@link DataSchemaLocation}s that have been
   * resolved through this resolver.
   */
  Map<String, DataSchemaLocation> nameToDataSchemaLocations();

  /**
   * Find a {@link NamedDataSchema} for the specified name.
   *
   * If a {@link NamedDataSchema} with the specified name is not found, the resolver
   * will should try its best to find and instantiate a {@link NamedDataSchema} with
   * the specified name.
   *
   * @param name of the schema to find.
   * @param errorMessageBuilder to append error messages to.
   * @return the {@link NamedDataSchema} if it can be located, else return null.
   */
  NamedDataSchema findDataSchema(String name, StringBuilder errorMessageBuilder);

  /**
   * Bind name to the provided {@link NamedDataSchema} and {@link DataSchemaLocation}.
   *
   * @param name to bind to.
   * @param schema provides the {@link NamedDataSchema}
   * @param location provides the {@link DataSchemaLocation}
   * @throws IllegalStateException if name is already bound.
   */
  void bindNameToSchema(Name name, NamedDataSchema schema, DataSchemaLocation location);

  /**
   * Lookup existing {@link NamedDataSchema} with the specified name.
   *
   * This is a pure lookup operation. If a {@link NamedDataSchema} with the specified
   * name does not already exist, then this method will return null, else it
   * returns the existing {@link NamedDataSchema}.
   *
   * @param name of the schema to find.
   * @return the {@link NamedDataSchema} if it already exists, else return null.
   */
  NamedDataSchema existingDataSchema(String name);

  /**
   * Lookup existing {@link NamedDataSchema}'s location with the specified name.
   *
   * This is a pure lookup operation. If a {@link NamedDataSchema} with the specified
   * name does not already exist, then this method will return null, else it
   * returns the location of the existing {@link NamedDataSchema}.
   *
   * @param name of the schema to find.
   * @return the {@link DataSchemaLocation} if the schema already exists, else return null.
   */
  default DataSchemaLocation existingSchemaLocation(String name)
  {
    return nameToDataSchemaLocations().get(name);
  }

  /**
   * Return whether the specified {@link DataSchemaLocation} has been associated with a name.
   *
   * @param location provides the {@link DataSchemaLocation} to check.
   * @return true if the specified {@link DataSchemaLocation} has been associated with a name.
   */
  boolean locationResolved(DataSchemaLocation location);

  /**
   * Add a record that is currently being parsed to the pending schema list. This is used to detect and disallow
   * circular references involving includes.
   * @param name Full name of the record.
   */
  void addPendingSchema(String name);

  /**
   * Update a pending schema to indicate the status of parsing includes for that schema.
   * @param name Schema name
   * @param isParsingInclude status of parsing include. Set to true before parsing includes and cleared after include
   *                         list is processed.
   */
  void updatePendingSchema(String name, Boolean isParsingInclude);

  /**
   * Remove a record from the pending list.
   * @param name Full name of the record.
   */
  void removePendingSchema(String name);

  /**
   * Return the list of records currently in the state of parsing.
   */
  LinkedHashMap<String, Boolean> getPendingSchemas();

  /**
   * Returns the schema file directory name for schemas location
   */
  default SchemaDirectoryName getSchemasDirectoryName()
  {
    return SchemaDirectoryName.PEGASUS;
  }
}
