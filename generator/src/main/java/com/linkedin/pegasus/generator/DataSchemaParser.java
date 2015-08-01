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

  /**
   * Initialize my {@link DataSchemaResolver} with the resolver path.
   */
  public DataSchemaParser(String resolverPath)
  {
    _resolverPath = resolverPath;
    _schemaResolver = CodeUtil.createSchemaResolver(resolverPath);
  }

  public String getResolverPath()
  {
    return _resolverPath;
  }

  public DataSchemaResolver getSchemaResolver()
  {
    return _schemaResolver;
  }

  /**
   * Parses sources that specify paths to schema files and/or fully qualified schema names.
   *
   * @param sources provides the paths to schema files and/or fully qualified schema names.
   * @return {@link ParseResult} for what were read.
   * @throws IOException if there are problems opening or deleting files.
   */
  public ParseResult parseSources(String sources[])
      throws IOException
  {
    final StringBuilder messageBuilder = new StringBuilder();

    try
    {
      for (String source : sources)
      {
        final File sourceFile = new File(source);
        if (sourceFile.exists())
        {
          if (sourceFile.isDirectory())
          {
            final FileUtil.FileExtensionFilter filter = new FileUtil.FileExtensionFilter(FileDataSchemaResolver.DEFAULT_EXTENSION);
            final List<File> sourceFilesInDirectory = FileUtil.listFiles(sourceFile, filter);
            for (File f : sourceFilesInDirectory)
            {
              parseFile(f, messageBuilder);
            }
          }
          else if (sourceFile.getName().endsWith(".jar"))
          {
            final JarFile jarFile = new JarFile(sourceFile);
            final Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements())
            {
              final JarEntry entry = entries.nextElement();
              if (!entry.isDirectory() && entry.getName().endsWith(FileDataSchemaResolver.DEFAULT_EXTENSION))
              {
                parseJarEntry(jarFile, entry, messageBuilder);
              }
            }
          }
          else
          {
            parseFile(sourceFile, messageBuilder);
          }
        }
        else
        {
          final StringBuilder errorMessage = new StringBuilder();
          final DataSchema schema = _schemaResolver.findDataSchema(source, errorMessage);
          if (schema == null)
          {
            messageBuilder.append("File cannot be opened or schema name cannot be resolved: ").append(source).append("\n");
          }
          if (errorMessage.length() > 0)
          {
            messageBuilder.append(errorMessage.toString());
          }
        }
      }

      if (messageBuilder.length() > 0)
      {
        throw new IOException(messageBuilder.toString());
      }

      return new ParseResult(messageBuilder.toString());
    }
    catch (RuntimeException e)
    {
      if (messageBuilder.length() > 0)
      {
        e = new RuntimeException("Unexpected " + e.getClass().getSimpleName() + " encountered.\n" +
                                     "This may be caused by the following parsing or processing errors:\n" +
                                     messageBuilder, e);
      }
      throw e;
    }
  }

  /**
   * Parse a source that specifies a file (not a fully qualified schema name).
   *
   * @param schemaSourceFile provides the source file.
   * @param messageBuilder {@link StringBuilder} to update message.
   * @throws IOException if there is a file access error.
   */
  private void parseFile(File schemaSourceFile, StringBuilder messageBuilder)
      throws IOException
  {
    final DataSchemaLocation location = getSchemaLocation(schemaSourceFile);
    // Whether a source file has already been resolved to data schemas
    if (_schemaResolver.locationResolved(location))
    {
      return;
    }

    final InputStream inputStream = new SchemaFileInputStream(schemaSourceFile);
    final List<DataSchema> schemas = parseSchemaStream(inputStream, location, messageBuilder);

    for (DataSchema schema : schemas)
    {
      validateSchemaWithPath(schemaSourceFile.getAbsolutePath(), schema);
    }
  }

  private void parseJarEntry(JarFile schemaJarFile, JarEntry jarEntry, StringBuilder messageBuilder)
      throws IOException
  {
    final DataSchemaLocation location = getSchemaLocation(schemaJarFile, jarEntry.getName());
    if (_schemaResolver.locationResolved(location))
    {
      return;
    }

    final InputStream jarStream = schemaJarFile.getInputStream(jarEntry);
    final List<DataSchema> schemas = parseSchemaStream(jarStream, location, messageBuilder);

    for (DataSchema schema : schemas)
    {
      validateSchemaWithPath(location.toString(), schema);
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
  private void validateSchemaWithPath(String path, DataSchema schema)
  {
    if (schema instanceof NamedDataSchema)
    {
      final NamedDataSchema namedDataSchema = (NamedDataSchema) schema;
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
  }

  /**
   * Parse a source file to obtain the data schemas contained within.
   *
   * @param schemaInputStream provides the source data.
   * @param messageBuilder {@link StringBuilder} to update message.
   * @return the data schemas within the source file.
   * @throws IOException if there is a file access error.
   */
  private List<DataSchema> parseSchemaStream(InputStream schemaInputStream, DataSchemaLocation schemaLocation, StringBuilder messageBuilder)
      throws IOException
  {
    final SchemaParser parser = new SchemaParser(_schemaResolver);
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
        messageBuilder.append(schemaLocation.toString())
            .append(",").append(parser.errorMessage());
      }
    }
  }

  /**
   * Represent the result of schema parsing. Consist of two parts: schema from file path and from schema name, based on user input.
   * The two parts are mutually exclusive, and the union of two consists of all schema resolved.
   */
  public class ParseResult
  {
    private final Map<NamedDataSchema, DataSchemaLocation> _schemaAndLocations = new HashMap<NamedDataSchema, DataSchemaLocation>();
    private final Set<File> _sourceFiles = new HashSet<File>();
    private final String _message;

    public ParseResult(String message)
    {
      for (Map.Entry<String, DataSchemaLocation> entry : _schemaResolver.nameToDataSchemaLocations().entrySet()) {
        final NamedDataSchema schema = _schemaResolver.bindings().get(entry.getKey());
        _schemaAndLocations.put(schema, entry.getValue());
        _sourceFiles.add(entry.getValue().getSourceFile());
      }

      _message = message;
    }

    public Map<NamedDataSchema, DataSchemaLocation> getSchemaAndLocations()
    {
      return _schemaAndLocations;
    }

    public Set<File> getSourceFiles()
    {
      return _sourceFiles;
    }

    public String getMessage()
    {
      return _message;
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
