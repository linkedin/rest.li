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

package com.linkedin.data.schema.resolver;


import com.linkedin.data.schema.DataSchemaLocation;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.schema.SchemaParserFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;


/**
 * Resolve a data schema name into its data schema, assuming the data schema PDSC is loaded as resources using the {@link ClassLoader}.
 *
 * @author Min Chen
 */
public class ClasspathResourceDataSchemaResolver extends DefaultDataSchemaResolver
{
  /**
   * The directory within the resource jar file that holds schema files.
   */
  private static final String DIR_IN_JAR = "pegasus";
  /**
   * The default file name extension is ".pdsc".
   */
  public static final String DEFAULT_EXTENSION = SchemaParser.FILE_EXTENSION;

  /**
   * Construct a new instance that uses the {@link Thread#getContextClassLoader()} for the current thread.
   */
  public ClasspathResourceDataSchemaResolver(SchemaParserFactory parserFactory)
  {
    super(parserFactory);
    _classLoader = Thread.currentThread().getContextClassLoader();
  }

  /**
   * Construct a new instance that uses the specified {@link ClassLoader}.
   *
   * @param classLoader provides the {@link ClassLoader}.
   */
  public ClasspathResourceDataSchemaResolver(SchemaParserFactory parserFactory, ClassLoader classLoader)
  {
    super(parserFactory);
    _classLoader = classLoader;
  }

  private String getDataSchemaResourcePath(String schemaName)
  {
    return DIR_IN_JAR + "/" + schemaName.replace('.', '/') + DEFAULT_EXTENSION;
  }

  @Override
  protected NamedDataSchema locateDataSchema(String schemaName, StringBuilder errorMessageBuilder)
  {
    NamedDataSchema schema = null;
    final String schemaResourcePath = getDataSchemaResourcePath(schemaName);
    try (InputStream stream = _classLoader.getResourceAsStream(schemaResourcePath))
    {
      if (stream == null)
      {
        errorMessageBuilder.append(String.format("Unable to find data schema file \"%s\" in classpath.", schemaResourcePath));
      }
      else
      {
        DataSchemaLocation location = new FileDataSchemaLocation(new File(schemaResourcePath));
        schema = parse(stream, location, schemaName, errorMessageBuilder);
      }
    }
    catch (IOException e)
    {
      errorMessageBuilder.append(String.format("Failed to read/close data schema file \"%s\" in classpath: \"%s\"", schemaResourcePath, e.getMessage()));
    }
    return schema;
  }

  private final ClassLoader _classLoader;
}