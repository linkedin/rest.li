/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.pegasus.generator;


import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaLocation;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.schema.SchemaParserFactory;
import com.linkedin.data.schema.resolver.FileDataSchemaLocation;
import com.linkedin.data.schema.resolver.FileDataSchemaResolver;
import com.linkedin.data.schema.resolver.InJarFileDataSchemaLocation;
import com.linkedin.util.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/**
 * Parse various forms of source into {@link DataSchema}.
 *
 * @author Keren Jin
 */
public class DataSchemaParser
{
  private final String _resolverPath;
  private final DataSchemaResolver _schemaResolver;
  private final SchemaParserFactory _schemaParserFactory;
  private final String _fileExtension;

  /**
   * Initialize my {@link DataSchemaResolver} with the resolver path.
   */
  public DataSchemaParser(String resolverPath)
  {
    this(resolverPath, CodeUtil.createSchemaResolver(resolverPath), SchemaParserFactory.instance(), FileDataSchemaResolver.DEFAULT_EXTENSION);
  }

  public String getResolverPath()
  {
    return _resolverPath;
  }

  public DataSchemaResolver getSchemaResolver()
  {
    return _schemaResolver;
  }

  public DataSchemaParser(String resolverPath, DataSchemaResolver schemaResolver, SchemaParserFactory schemaParserFactory, String fileExtension)
  {
    _resolverPath = resolverPath;
    _schemaResolver = schemaResolver;
    _schemaParserFactory = schemaParserFactory;
    _fileExtension = fileExtension;
  }

  /**
   * Parses sources that specify paths to schema files and/or fully qualified schema names.
   *
   * @param sources provides the paths to schema files and/or fully qualified schema names.
   * @return {@link ParseResult} for what were read.
   * @throws IOException if there are problems opening or deleting files.
   */
  public ParseResult parseSources(String sources[]) throws IOException
  {
    final ParseResult result = new ParseResult();

    try
    {
      for (String source : sources)
      {
        final File sourceFile = new File(source);
        if (sourceFile.exists())
        {
          if (sourceFile.isDirectory())
          {
            final FileUtil.FileExtensionFilter filter = new FileUtil.FileExtensionFilter(_fileExtension);
            final List<File> sourceFilesInDirectory = FileUtil.listFiles(sourceFile, filter);
            for (File f : sourceFilesInDirectory)
            {
              parseFile(f, result);
              result._sourceFiles.add(f);
            }
          }
          else
          {
            if (sourceFile.getName().endsWith(".jar"))
            {
              final JarFile jarFile = new JarFile(sourceFile);
              final Enumeration<JarEntry> entries = jarFile.entries();
              while (entries.hasMoreElements())
              {
                final JarEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(FileDataSchemaResolver.DEFAULT_EXTENSION))
                {
                  parseJarEntry(jarFile, entry, result);
                }
              }
            }
            else
            {
              parseFile(sourceFile, result);
            }

            result._sourceFiles.add(sourceFile);
          }
        }
        else
        {
          final StringBuilder errorMessage = new StringBuilder();
          final DataSchema schema = _schemaResolver.findDataSchema(source, errorMessage);
          if (schema == null)
          {
            result._messageBuilder.append("File cannot be opened or schema name cannot be resolved: ").append(source).append("\n");
          }
          if (errorMessage.length() > 0)
          {
            result._messageBuilder.append(errorMessage.toString());
          }
        }
      }

      if (result._messageBuilder.length() > 0)
      {
        throw new IOException(result.getMessage());
      }

      for (Map.Entry<String, DataSchemaLocation> entry : _schemaResolver.nameToDataSchemaLocations().entrySet()) {
        final DataSchema schema = _schemaResolver.bindings().get(entry.getKey());
        result._schemaAndLocations.put(schema, entry.getValue());
      }

      return result;
    }
    catch (RuntimeException e)
    {
      if (result._messageBuilder.length() > 0)
      {
        e = new RuntimeException("Unexpected " + e.getClass().getSimpleName() + " encountered.\n" +
                                     "This may be caused by the following parsing or processing errors:\n" +
                                     result.getMessage(), e);
      }
      throw e;
    }
  }

  /**
   * Parse a source that specifies a file (not a fully qualified schema name).
   *
   * @param schemaSourceFile provides the source file.
   * @throws IOException if there is a file access error.
   */
  private void parseFile(File schemaSourceFile, ParseResult result)
      throws IOException
  {
    final DataSchemaLocation location = getSchemaLocation(schemaSourceFile);
    // if a the data schema has been resolved before, must skip parsing again, because one name can't be bound to two data schemas
    if (_schemaResolver.locationResolved(location))
    {
      return;
    }

    final InputStream inputStream = new SchemaFileInputStream(schemaSourceFile);
    final List<DataSchema> schemas = parseSchemaStream(inputStream, location, result);

    for (DataSchema schema : schemas)
    {
      if (schema instanceof NamedDataSchema)
      {
        validateSchemaWithPath(schemaSourceFile.getAbsolutePath(), (NamedDataSchema) schema);
      }

      result._schemaAndLocations.put(schema, location);
    }
  }

  private void parseJarEntry(JarFile schemaJarFile, JarEntry jarEntry, ParseResult result)
      throws IOException
  {
    final DataSchemaLocation location = getSchemaLocation(schemaJarFile, jarEntry.getName());
    if (_schemaResolver.locationResolved(location))
    {
      return;
    }

    final InputStream jarStream = schemaJarFile.getInputStream(jarEntry);
    final List<DataSchema> schemas = parseSchemaStream(jarStream, location, result);

    for (DataSchema schema : schemas)
    {
      if (schema instanceof NamedDataSchema)
      {
        validateSchemaWithPath(location.toString(), (NamedDataSchema) schema);
      }

      result._schemaAndLocations.put(schema, location);
    }
  }

  private DataSchemaLocation getSchemaLocation(File schemaFile)
  {
    return new FileDataSchemaLocation(schemaFile);
  }

  private DataSchemaLocation getSchemaLocation(JarFile jarFile, String pathInJar)
  {
    return new InJarFileDataSchemaLocation(jarFile, pathInJar);
  }

  /**
   * Checks that the schema name and namespace match the file name and path.  These must match for FileDataSchemaResolver to find a schema pdscs by fully qualified name.
   */
  private void validateSchemaWithPath(String path, NamedDataSchema namedDataSchema)
  {
    final String namespace = namedDataSchema.getNamespace();

    if (!FileUtil.removeFileExtension(path.substring(path.lastIndexOf(File.separator) + 1)).equalsIgnoreCase(namedDataSchema.getName()))
    {
      throw new IllegalArgumentException(namedDataSchema.getFullName() + " has name that does not match path '" +
                                             path + "'");
    }

    final String parent = path.substring(0, path.lastIndexOf(File.separator));
    if (!parent.endsWith(namespace.replace('.', File.separatorChar)))
    {
      throw new IllegalArgumentException(namedDataSchema.getFullName() + " has namespace that does not match " +
                                             "parent path '" + parent + "'");
    }
  }

  /**
   * Parse a source file to obtain the data schemas contained within.
   * This method will cause the {@link DataSchemaResolver} to resolve any referenced named and unnamed schemas,
   * as well as registering named schemas in its bindings.
   *
   * @param schemaInputStream provides the source data.
   * @return the top-level data schemas within the source file.
   * @throws IOException if there is a file access error.
   */
  private List<DataSchema> parseSchemaStream(InputStream schemaInputStream, DataSchemaLocation schemaLocation, ParseResult result)
      throws IOException
  {
    SchemaParser parser = _schemaParserFactory.create(_schemaResolver);
    try
    {
      parser.setLocation(schemaLocation);
      parser.parse(schemaInputStream);
      if (parser.hasError())
      {
        return Collections.emptyList();
      }
      return parser.topLevelDataSchemas();
    }
    finally
    {
      schemaInputStream.close();
      if (parser.hasError())
      {
        result._messageBuilder.append(schemaLocation.toString()).append(",").append(parser.errorMessage());
      }
    }
  }

  /**
   * Represent the result of schema parsing. Consist of two parts: schema from file path and from schema name, based on user input.
   * The two parts are mutually exclusive, and the union of two consists of all schema resolved.
   *
   * The result contains all resolved data schemas, both directly defined by the source files, or transitively referenced by the former.
   * Both top-level and embedded named schemas are included. Only top-level unnamed schemas are included.
   */
  public static class ParseResult
  {
    private final Map<DataSchema, DataSchemaLocation> _schemaAndLocations = new HashMap<DataSchema, DataSchemaLocation>();
    private final Set<File> _sourceFiles = new HashSet<File>();
    private final StringBuilder _messageBuilder = new StringBuilder();

    public Map<DataSchema, DataSchemaLocation> getSchemaAndLocations()
    {
      return _schemaAndLocations;
    }

    public Set<File> getSourceFiles()
    {
      return _sourceFiles;
    }

    public String getMessage()
    {
      return _messageBuilder.toString();
    }
  }

  private static class SchemaFileInputStream extends FileInputStream
  {
    private File _schemaSourceFile;

    private SchemaFileInputStream(File file)
        throws FileNotFoundException
    {
      super(file);
      _schemaSourceFile = file;
    }

    @Override
    public String toString()
    {
      return _schemaSourceFile.toString();
    }
  }
}
