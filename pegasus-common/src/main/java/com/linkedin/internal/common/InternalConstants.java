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
 * Restli internal constants.
 */
public final class InternalConstants
{
  /**
   * Directory within resource jar file that holds the pegasus schemas. Pegasus parsers and resolvers look for pegasus
   * files(*.pdl, *.pdsc) only within this directory.
   */
  public static final String PEGASUS_DIR_IN_JAR = "pegasus";

  /**
   * Directory within resource jar file that holds the Entity Relationship pegasus schemas.
   * Pegasus Extensions schema parsers and resolvers look for pegasus files(*.pdl) only within this directory.
   */
  public static final String PEGASUS_EXTENSIONS_DIR_IN_JAR = "extensions";

  private InternalConstants() {}
}
