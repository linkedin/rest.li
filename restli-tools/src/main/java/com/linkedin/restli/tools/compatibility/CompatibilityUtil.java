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

package com.linkedin.restli.tools.compatibility;

import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.SchemaParserFactory;
import com.linkedin.data.schema.generator.AbstractGenerator;
import com.linkedin.data.schema.resolver.DefaultDataSchemaResolver;
import com.linkedin.data.schema.resolver.FileDataSchemaResolver;

/**
 * Basic Utilities for Resource Compatibility.
 * @author Moira Tagle
 */

public class CompatibilityUtil
{
  /**
   * Creates a {@link DataSchemaResolver} based on what {@link AbstractGenerator#GENERATOR_RESOLVER_PATH} is set to.
   * @return A {@link FileDataSchemaResolver} referencing what {@link AbstractGenerator#GENERATOR_RESOLVER_PATH} is set to,
   * if it is set; or {@link DefaultDataSchemaResolver} if it is not set.
   */
  public static DataSchemaResolver getDataSchemaResolver()
  {
    final String resolverPath = System.getProperty(AbstractGenerator.GENERATOR_RESOLVER_PATH);
    if (resolverPath != null)
    {
      return new FileDataSchemaResolver(SchemaParserFactory.instance(), resolverPath);
    }
    else
    {
      return new DefaultDataSchemaResolver();
    }
  }
}
