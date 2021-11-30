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
import com.linkedin.data.schema.DataSchemaParserFactory;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.PegasusSchemaParser;
import com.linkedin.data.schema.resolver.FileDataSchemaLocation;
import com.linkedin.data.schema.resolver.InJarFileDataSchemaLocation;
import com.linkedin.data.schema.resolver.SchemaDirectory;
import com.linkedin.data.schema.resolver.SchemaDirectoryName;
import com.linkedin.util.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/**
 * Parses a particular Pegasus schema file format into {@link DataSchema} using the provided parser.
 *
 * @author Keren Jin
 * @author Joe Betz
 */
public class FileFormatDataSchemaParser
{
  static final String SCHEMA_PATH_PREFIX = SchemaDirectoryName.PEGASUS.getName() + "/";
  private final DataSchemaResolver _schemaResolver;
  private final DataSchemaParserFactory _schemaParserFactory;
  private final List<SchemaDirectory> _sourceDirectories;

  public FileFormatDataSchemaParser(String resolverPath, DataSchemaResolver schemaResolver, DataSchemaParserFactory schemaParserFactory)
  {
    this(schemaResolver, schemaParserFactory, schemaResolver.getSchemaDirectories());
  }

  public FileFormatDataSchemaParser(DataSchemaResolver schemaResolver,
      DataSchemaParserFactory schemaParserFactory, List<SchemaDirectory> sourceDirectories)
  {
    _schemaResolver = schemaResolver;
    _schemaParserFactory = schemaParserFactory;
    _sourceDirectories = sourceDirectories;
  }

  public DataSchemaParser.ParseResult parseSources(String[] sources) throws IOException
  {
    final DataSchemaParser.ParseResult result = new DataSchemaParser.ParseResult();
    parseSources(sources, result);
    return result;
  }

  void parseSources(String[] sources, DataSchemaParser.ParseResult result) throws IOException
  {
    try
    {
      for (String source : sources)
      {
        final File sourceFile = new File(source);
        if (sourceFile.exists())
        {
          if (sourceFile.isDirectory())
          {
            final FileUtil.FileExtensionFilter filter = new FileUtil.FileExtensionFilter(_schemaParserFactory.getLanguageExtension());
            final List<File> sourceFilesInDirectory = FileUtil.listFiles(sourceFile, filter);
            for (File f : sourceFilesInDirectory)
            {
              parseFile(f, result);
              result.getSourceFiles().add(f);
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
                if (!entry.isDirectory() &&
                    entry.getName().endsWith(_schemaParserFactory.getLanguageExtension()) &&
                    shouldParseFile(entry.getName()))
                {
                  parseJarEntry(jarFile, entry, result);
                  result.getSourceFiles().add(sourceFile);
                }
              }
            }
            else
            {
              parseFile(sourceFile, result);
              result.getSourceFiles().add(sourceFile);
            }
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

      for (Map.Entry<String, DataSchemaLocation> entry : _schemaResolver.nameToDataSchemaLocations().entrySet())
      {
        final DataSchema schema = _schemaResolver.existingDataSchema(entry.getKey());
        result.getSchemaAndLocations().put(schema, entry.getValue());
      }
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

  private boolean shouldParseFile(String path)
  {
    for (SchemaDirectory schemaDirectory : _sourceDirectories)
    {
      if (schemaDirectory.matchesJarFilePath(path))
      {
        return true;
      }
    }
    return false;
  }
  /**
   * Parse a source that specifies a file (not a fully qualified schema name).
   *
   * @param schemaSourceFile provides the source file.
   * @throws IOException if there is a file access error.
   */
  private void parseFile(File schemaSourceFile, DataSchemaParser.ParseResult result)
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

      result.getSchemaAndLocations().put(schema, location);
    }
  }

  private void parseJarEntry(JarFile schemaJarFile, JarEntry jarEntry, DataSchemaParser.ParseResult result)
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

      result.getSchemaAndLocations().put(schema, location);
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
      throw new IllegalArgumentException(namedDataSchema.getFullName() + " has name " + namedDataSchema.getName() + " that does not match path '" +
                                             path + "' -- " + path.substring(path.lastIndexOf(File.separator) + 1));
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
  private List<DataSchema> parseSchemaStream(InputStream schemaInputStream, DataSchemaLocation schemaLocation, DataSchemaParser.ParseResult result)
      throws IOException
  {
    PegasusSchemaParser parser = _schemaParserFactory.create(_schemaResolver);
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
