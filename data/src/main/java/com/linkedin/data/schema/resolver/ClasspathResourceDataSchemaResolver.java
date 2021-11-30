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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


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
   * Construct a new instance that uses the {@link Thread#getContextClassLoader()} for the current thread.
   */
  public ClasspathResourceDataSchemaResolver()
  {
    this(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Construct a new instance that uses the {@link Thread#getContextClassLoader()} for the current thread.
   * @param schemaDirectoryName resource directory name for the schemas to be parsed. Ex. pegasus or extensions.
   */
  public ClasspathResourceDataSchemaResolver(SchemaDirectoryName schemaDirectoryName)
  {
    this(Thread.currentThread().getContextClassLoader(), schemaDirectoryName);
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
   * @param classLoader provides the {@link ClassLoader}.
   * @param schemaDirectoryName The file directory name for different types of schemas.
   *                            Default is {@link SchemaDirectoryName#PEGASUS}
   * @deprecated Use {@link ClasspathResourceDataSchemaResolver#ClasspathResourceDataSchemaResolver(ClassLoader, List)}
   * instead.
   */
  @Deprecated
  public ClasspathResourceDataSchemaResolver(ClassLoader classLoader, SchemaDirectoryName schemaDirectoryName)
  {
    List<SchemaDirectory> schemaDirectories = new ArrayList<>();
    schemaDirectories.add(schemaDirectoryName);
    // The below logic is kept for backwards compatibility. Ideally the constructor that accepts list of schema
    // directories should be used to configure all the resolver directores.
    if (schemaDirectoryName == SchemaDirectoryName.EXTENSIONS)
    {
      schemaDirectories.add(SchemaDirectoryName.PEGASUS);
    }
    for (DataSchemaParserFactory parserForFormat: BUILTIN_FORMAT_PARSER_FACTORIES)
    {
      SingleFormatClasspathSchemaResolver resolver = new SingleFormatClasspathSchemaResolver(parserForFormat);
      resolver.setSchemaDirectories(schemaDirectories);
      addResolver(resolver);
    }
    _classLoader = classLoader;
    setSchemaDirectories(schemaDirectories);
  }

  /**
   * Construct a new instance that uses the specified {@link ClassLoader} and uses the provided schema directories
   * to for resolving schema references.
   *
   * @param classLoader provides the {@link ClassLoader}.
   * @param schemaDirectories The list of schema directories to use for resolving referenced schemas.
   */
  public ClasspathResourceDataSchemaResolver(ClassLoader classLoader, List<SchemaDirectory> schemaDirectories)
  {
    for (DataSchemaParserFactory parserForFormat: BUILTIN_FORMAT_PARSER_FACTORIES)
    {
      SingleFormatClasspathSchemaResolver resolver = new SingleFormatClasspathSchemaResolver(parserForFormat);
      resolver.setSchemaDirectories(schemaDirectories);
      addResolver(resolver);
    }
    _classLoader = classLoader;
    setSchemaDirectories(schemaDirectories);
  }

  @Override
  @SuppressWarnings("deprecation")
  public SchemaDirectoryName getSchemasDirectoryName()
  {
    assert getSchemaDirectories().size() > 0;
    return (SchemaDirectoryName) getSchemaDirectories().get(0);
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

    /**
     * Construct a new instance that uses the {@link Thread#getContextClassLoader()} for the current thread.
     * @deprecated use {@link #SingleFormatClasspathSchemaResolver(DataSchemaParserFactory)} and
     * {@link #setSchemaDirectories(List)} instead to configure the resolver directories.
     */
    @Deprecated
    public SingleFormatClasspathSchemaResolver(DataSchemaParserFactory parserFactory,
        SchemaDirectoryName schemaDirectoryName)
    {
      super(parserFactory, ClasspathResourceDataSchemaResolver.this);
      this._extension = "." + parserFactory.getLanguageExtension();
      setSchemaDirectories(Collections.singletonList(schemaDirectoryName));
    }

    private Collection<String> getDataSchemaResourcePaths(String schemaName)
    {
      List<String> resourcePaths = new ArrayList<>(getSchemaDirectories().size());
      getSchemaDirectories().forEach(directory -> resourcePaths.add(
          directory.getName() + "/" + schemaName.replace('.', '/') + _extension));
      return resourcePaths;
    }

    @Override
    protected NamedDataSchema locateDataSchema(String schemaName, StringBuilder errorMessageBuilder)
    {
      for (String schemaResourcePath : getDataSchemaResourcePaths(schemaName))
      {
        try (InputStream stream = _classLoader.getResourceAsStream(schemaResourcePath))
        {
          if (stream != null)
          {
            DataSchemaLocation location = new FileDataSchemaLocation(new File(schemaResourcePath));
            return parse(stream, location, schemaName, errorMessageBuilder);
          }
        }
        catch (IOException e)
        {
          errorMessageBuilder.append(String.format("Failed to read/close data schema file \"%s\" in classpath: \"%s\"", schemaResourcePath, e.getMessage()));
        }
      }
      return null;
    }
  }
}