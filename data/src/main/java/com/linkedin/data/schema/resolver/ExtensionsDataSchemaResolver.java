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
import com.linkedin.data.schema.grammar.PdlSchemaParser;
import com.linkedin.data.schema.grammar.PdlSchemaParserFactory;


/**
 * Combines schema resolver for pegasus data and extensions schema directory.
 *
 * @author Aman Gupta
 */
public class ExtensionsDataSchemaResolver extends AbstractMultiFormatDataSchemaResolver
{
  public ExtensionsDataSchemaResolver(String resolverPath)
  {
    for (DataSchemaParserFactory parserFactory : AbstractMultiFormatDataSchemaResolver.BUILTIN_FORMAT_PARSER_FACTORIES)
    {
      addResolver(createSchemaResolver(resolverPath, SchemaDirectoryName.PEGASUS, this, parserFactory));
      addResolver(createSchemaResolver(resolverPath, SchemaDirectoryName.EXTENSIONS, this, parserFactory));
    }
  }

  public ExtensionsDataSchemaResolver(String resolverPath, DataSchemaResolver dependencyResolver)
  {
    for (DataSchemaParserFactory parserFactory : AbstractMultiFormatDataSchemaResolver.BUILTIN_FORMAT_PARSER_FACTORIES)
    {
      addResolver(createSchemaResolver(resolverPath, SchemaDirectoryName.PEGASUS, dependencyResolver, parserFactory));
      addResolver(createSchemaResolver(resolverPath, SchemaDirectoryName.EXTENSIONS, dependencyResolver, parserFactory));
    }
  }

  private FileDataSchemaResolver createSchemaResolver(String resolverPath, SchemaDirectoryName schemaDirectoryName,
      DataSchemaResolver dependencyResolver, DataSchemaParserFactory parserFactory)
  {
    FileDataSchemaResolver resolver =
        new FileDataSchemaResolver(parserFactory, resolverPath, dependencyResolver);
    resolver.setExtension("." + parserFactory.getLanguageExtension());
    resolver.setSchemasDirectoryName(schemaDirectoryName);
    return resolver;
  }

  @Override
  public SchemaDirectoryName getSchemasDirectoryName()
  {
    return SchemaDirectoryName.EXTENSIONS;
  }
}
