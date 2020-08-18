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
import com.linkedin.data.schema.DataSchemaParserFactory;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.schema.SchemaParserFactory;

import com.linkedin.internal.common.InternalConstants;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;


/**
 * Resolve a data schema name into its data schema, assuming the data schema PDSC/PDL is loaded as resources using the {@link ClassLoader}.
 *
 * @author Min Chen
 */
public class ClasspathResourceDataSchemaResolver extends AbstractMultiFormatDataSchemaResolver
{
  /**
   * The default file name extension is ".pdsc".
   * @deprecated Do not use.
   */
  @Deprecated
  public static final String DEFAULT_EXTENSION = SchemaParser.FILE_EXTENSION;

  private final ClassLoader _classLoader;

  /**
   * The file directory name for different types of schemas. Default is {@link SchemaDirectoryName#PEGASUS}
   * Ex "pegasus" for data or "extensions" for relationship extension schema files
   */
  private SchemaDirectoryName _schemasDirectoryName = SchemaDirectoryName.PEGASUS;


  /**
   * Construct a new instance that uses the {@link Thread#getContextClassLoader()} for the current thread.
   */
  public ClasspathResourceDataSchemaResolver()
  {
    this(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Construct a new instance that uses the {@link Thread#getContextClassLoader()} for the current thread.
   *
   * @deprecated The parserFactory is not needed as this class now uses builtin parsers. Use
   * {@link #ClasspathResourceDataSchemaResolver()} instead
   */
  @Deprecated
  public ClasspathResourceDataSchemaResolver(SchemaParserFactory parserFactory)
  {
    this(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Construct a new instance that uses the specified {@link ClassLoader}.
   *
   * @param classLoader provides the {@link ClassLoader}.
   */
  public ClasspathResourceDataSchemaResolver(ClassLoader classLoader)
  {
    for (DataSchemaParserFactory parserForFormat: BUILTIN_FORMAT_PARSER_FACTORIES)
    {
      addResolver(new SingleFormatClasspathSchemaResolver(parserForFormat));
    }
    _classLoader = classLoader;
  }

  /**
   * Construct a new instance that uses the specified {@link ClassLoader}.
   *
   * @deprecated The parserFactory is not needed as this class now uses builtin parsers. Use
   * {@link #ClasspathResourceDataSchemaResolver(ClassLoader)} instead
   * @param classLoader provides the {@link ClassLoader}.
   */
  @Deprecated
  public ClasspathResourceDataSchemaResolver(SchemaParserFactory parserFactory, ClassLoader classLoader)
  {
    this(classLoader);
  }

  private class SingleFormatClasspathSchemaResolver extends DefaultDataSchemaResolver
  {
    private final String _extension;

    /**
     * Construct a new instance that uses the {@link Thread#getContextClassLoader()} for the current thread.
     */
    public SingleFormatClasspathSchemaResolver(DataSchemaParserFactory parserFactory)
    {
      super(parserFactory, ClasspathResourceDataSchemaResolver.this);
      this._extension = "." + parserFactory.getLanguageExtension();
    }

    private String getDataSchemaResourcePath(String schemaName)
    {
      return _schemasDirectoryName.getName() + "/" + schemaName.replace('.', '/') + _extension;
    }

    @Override
    protected NamedDataSchema locateDataSchema(String schemaName, StringBuilder errorMessageBuilder)
    {
      NamedDataSchema schema = null;
      final String schemaResourcePath = getDataSchemaResourcePath(schemaName);
      try (InputStream stream = _classLoader.getResourceAsStream(schemaResourcePath))
      {
        if (stream != null)
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
  }

  /**
   * Return the current schema file directory name for schemas location
   *
   * @return the current search paths.
   */
  public SchemaDirectoryName getSchemasDirectoryName()
  {
    return _schemasDirectoryName;
  }

  /**
   * Sets the file directory name for schemas location dir.
   * If not set Defaults to {@link SchemaDirectoryName#PEGASUS}
   *
   * @param schemasDirectoryName path suffix.
   */
  public void setSchemasDirectoryName(SchemaDirectoryName schemasDirectoryName)
  {
    _schemasDirectoryName = schemasDirectoryName;
  }
}