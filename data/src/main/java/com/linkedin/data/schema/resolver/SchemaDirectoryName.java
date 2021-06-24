/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.data.schema.resolver;

/**
 * Directories within resource jar file that holds different types of pegasus schemas. Ex. Data or Extensions
 *
 * @author Aman Gupta
 */
public interface SchemaDirectoryName
{
  SchemaDirectoryName PEGASUS = BuiltInSchemaDirectory.PEGASUS;
  SchemaDirectoryName EXTENSIONS = BuiltInSchemaDirectory.EXTENSIONS;

  /**
   * Return the schema directory name.
   */
  String getName();

  enum BuiltInSchemaDirectory implements SchemaDirectoryName
  {
    /**
     * Directory holds the pegasus schemas. Pegasus parsers and resolvers look for pegasus
     * files(*.pdl, *.pdsc) only within this directory.
     */
    PEGASUS("pegasus"),
    /**
     * Directory holds the Entity Relationship pegasus schemas.
     * Pegasus Extensions schema parsers and resolvers look for pegasus files(*.pdl) only within this directory.
     */
    EXTENSIONS("extensions");

    private final String _name;

    BuiltInSchemaDirectory(String name)
    {
      _name = name;
    }


    @Override
    public String getName()
    {
      return _name;
    }
  }

  /**
   * Checks if the given jar file path starts with this schema directory name.
   */
  default boolean matchesJarFilePath(String path)
  {
    return path.startsWith(getName() + "/");
  }
}
