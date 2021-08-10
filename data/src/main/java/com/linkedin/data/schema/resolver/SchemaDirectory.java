/*
   Copyright (c) 2021 LinkedIn Corp.

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
 * Represents a schema directory relative to the resolver path (directory or Jar file). This is used to customize
 * schema resolvers to limit which directories are used for resolving schema references.
 *
 * @author Karthik Balasubramanian
 */
public interface SchemaDirectory
{
  /**
   * Return the schema directory name.
   */
  String getName();

  /**
   * Checks if the given jar file path starts with this schema directory name.
   */
  default boolean matchesJarFilePath(String path)
  {
    return path.startsWith(getName() + "/");
  }
}
