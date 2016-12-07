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
import com.linkedin.data.schema.resolver.DefaultDataSchemaResolver;
import com.linkedin.data.schema.resolver.FileDataSchemaResolver;
import com.linkedin.data.schema.resolver.MultiFormatDataSchemaResolver;


/**
 * Basic Utilities for Resource Compatibility.
 * @author Moira Tagle
 */

public class CompatibilityUtil
{
  /**
   * Creates a {@link DataSchemaResolver} based on what resolver path is set to.
   *
   * @param resolverPath resolver path for the created {@link DataSchemaResolver}.
   * @return a {@link FileDataSchemaResolver} referencing what resolverPath is set to if it is not null
   *         otherwise, return {@link DefaultDataSchemaResolver}.
   */
  public static DataSchemaResolver getDataSchemaResolver(String resolverPath)
  {
    if (resolverPath != null)
    {
      return MultiFormatDataSchemaResolver.withBuiltinFormats(resolverPath);
    }
    else
    {
      return new DefaultDataSchemaResolver();
    }
  }
}
