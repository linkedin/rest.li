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

package com.linkedin.internal.common;

/**
 * Directories within resource jar file that holds different types of pegasus schemas. Ex. Data or Extensions
 *
 * @author Aman Gupta
 */
public enum SchemaDirLocation
{
  /**
   * Directory holds the pegasus schemas. Pegasus parsers and resolvers look for pegasus
   * files(*.pdl, *.pdsc) only within this directory.
   */
  pegasus,
  /**
   * Directory holds the Entity Relationship pegasus schemas.
   * Pegasus Extensions schema parsers and resolvers look for pegasus files(*.pdl) only within this directory.
   */
  extensions
}
