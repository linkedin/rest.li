/*
 * Copyright 2015 Coursera Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.linkedin.data.schema.resolver;

import com.linkedin.data.schema.DataSchemaParserFactory;
import java.util.List;


/**
 * Combines multiple file format specific resolvers (and respective file format specific parsers)
 * into a single resolver.
 *
 * E.g. a resolver for the ".pdsc" file format and the ".pdl" file format, each with their
 * own file format specific parsers, can be combined into a single resolver able to look up
 * schemas of either file format.
 */
public class MultiFormatDataSchemaResolver extends AbstractMultiFormatDataSchemaResolver
{
  /**
   * Create a MultiFormatDataSchemaResolver able to resolve all builtin file formats (.pdsc and .pdl).
   */
  public static MultiFormatDataSchemaResolver withBuiltinFormats(String resolverPath)
  {
    return new MultiFormatDataSchemaResolver(resolverPath, BUILTIN_FORMAT_PARSER_FACTORIES);
  }

  /**
   * Initializes a new resolver with a specific set of file format parsers.  Use @{link withBuiltinFormats}
   * instead to initialize with the default file format parsers.
   *
   * @param resolverPath provides the search paths separated by the provided separator, or null for no search paths.
   * @param parsersForFormats provides a list of parser factories, one for each file format (e.g. PDSC, PDL)
   *                          this resolver supports.
   */
  public MultiFormatDataSchemaResolver(
      String resolverPath,
      List<DataSchemaParserFactory> parsersForFormats)
  {
    for (DataSchemaParserFactory parserForFormat: parsersForFormats)
    {
      FileDataSchemaResolver resolver = new FileDataSchemaResolver(parserForFormat, resolverPath, this);
      resolver.setExtension("." + parserForFormat.getLanguageExtension());
      addResolver(resolver);
    }
  }
}
