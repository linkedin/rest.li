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

import com.linkedin.data.schema.DataSchemaParserFactory;
import com.linkedin.data.schema.DataSchemaResolver;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Combines schema resolver for pegasus data and extensions schema directory.
 *
 * @author Aman Gupta
 * @deprecated Recommended way to handle parsing extension schemas is by using DataSchemaParser initialized with correct
 * source and resolver directories. Correct way to build the parser:
 * <p/>
 * <pre>{@code
 * List<SchemaDirectoryName> resolverDirectories = Arrays.asList(
 *     SchemaDirectoryName.EXTENSIONS, SchemaDirectoryName.PEGASUS);
 * List<SchemaDirectoryName> sourceDirectories =
 *     Collections.singletonList(SchemaDirectoryName.EXTENSIONS);
 * DataSchemaParser parser = new DataSchemaParser.Builder(jarFile)
 *     .setResolverDirectories(resolverDirectories)
 *     .setSourceDirectories(sourceDirectories)
 *     .build();
 * }</pre>
 */
@Deprecated
public class ExtensionsDataSchemaResolver extends AbstractMultiFormatDataSchemaResolver
{
  private static final List<SchemaDirectoryName> RESOLVER_SCHEMA_DIRECTORIES =
      Arrays.asList(SchemaDirectoryName.PEGASUS, SchemaDirectoryName.EXTENSIONS);
  public ExtensionsDataSchemaResolver(String resolverPath)
  {
    for (DataSchemaParserFactory parserFactory : AbstractMultiFormatDataSchemaResolver.BUILTIN_FORMAT_PARSER_FACTORIES)
    {
      addResolver(createSchemaResolver(resolverPath, this, parserFactory));
    }
  }

  public ExtensionsDataSchemaResolver(String resolverPath, DataSchemaResolver dependencyResolver)
  {
    for (DataSchemaParserFactory parserFactory : AbstractMultiFormatDataSchemaResolver.BUILTIN_FORMAT_PARSER_FACTORIES)
    {
      addResolver(createSchemaResolver(resolverPath, dependencyResolver, parserFactory));
    }
  }

  private FileDataSchemaResolver createSchemaResolver(String resolverPath,
      DataSchemaResolver dependencyResolver, DataSchemaParserFactory parserFactory)
  {
    FileDataSchemaResolver resolver =
        new FileDataSchemaResolver(parserFactory, resolverPath, dependencyResolver);
    resolver.setExtension("." + parserFactory.getLanguageExtension());
    resolver.setSchemaDirectories(RESOLVER_SCHEMA_DIRECTORIES);
    return resolver;
  }

  @Override
  public List<SchemaDirectoryName> getSchemaDirectories()
  {
    // This override is to maintain backwards compatibility with the old behavior which used the schema directory name
    // to parse the source files. Limiting the extension resolver to load only the extension schemas by using only
    // the extension schema directory.
    return Collections.singletonList(SchemaDirectoryName.EXTENSIONS);
  }
}
