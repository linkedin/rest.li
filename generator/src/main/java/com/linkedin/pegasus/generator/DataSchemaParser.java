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
import com.linkedin.util.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


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
   *
   * @return {@link ParseResult} for what were read.
   *
   * @throws IOException if there are problems opening or deleting files.
   */
  public ParseResult parseSources(String sources[])
      throws IOException
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
            final FileUtil.FileExtensionFilter filter = new FileUtil.FileExtensionFilter(FileDataSchemaResolver.DEFAULT_EXTENSION);
            final List<File> sourceFilesInDirectory = FileUtil.listFiles(sourceFile, filter);
            for (File f : sourceFilesInDirectory)
            {
              parseFile(f, result);
            }
          }
          else
          {
            parseFile(sourceFile, result);
          }
        }
        else
        {
          final StringBuilder errorMessage = new StringBuilder();
          final DataSchema schema = _schemaResolver.findDataSchema(source, errorMessage);
          if (schema == null)
          {
            result.getMessage().append("File cannot be opened or schema name cannot be resolved: ").append(source).append("\n");
          }
          if (errorMessage.length() > 0)
          {
            result.getMessage().append(errorMessage.toString());
          }
          if (schema != null)
          {
            result.getSchemaAndNames().add(new CodeUtil.Pair<DataSchema, String>(schema, source));
          }
        }
      }

      if (result.getMessage().length() > 0)
      {
        throw new IOException(result.getMessage().toString());
      }

      appendSourceFilesFromSchemaResolver(result);

      return result;
    }
    catch (RuntimeException e)
    {
      if (result.getMessage().length() > 0)
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
   *
   * @param result {@link ParseResult} to update.
   *
   * @throws IOException if there is a file access error.
   */
  private void parseFile(File schemaSourceFile, ParseResult result)
      throws IOException
  {
    if (wasResolved(schemaSourceFile))
    {
      return;
    }

    final List<DataSchema> schemas = parseSchema(schemaSourceFile, result);

    for (DataSchema schema : schemas)
    {
      validateSchemaWithFilePath(schemaSourceFile, schema);
      result.getSchemaAndFiles().add(new CodeUtil.Pair<DataSchema, File>(schema, schemaSourceFile));
      result.getSourceFiles().add(schemaSourceFile);
    }
  }

  /**
   * Checks that the schema name and namespace match the file name and path.  These must match for FileDataSchemaResolver to find a schema pdscs by fully qualified name.
   */
  private void validateSchemaWithFilePath(File schemaSourceFile, DataSchema schema)
  {
    if (schemaSourceFile != null && schemaSourceFile.isFile() && schema instanceof NamedDataSchema)
    {
      final NamedDataSchema namedDataSchema = (NamedDataSchema) schema;
      final String namespace = namedDataSchema.getNamespace();

      if (!FileUtil.removeFileExtension(schemaSourceFile.getName()).equalsIgnoreCase(namedDataSchema.getName()))
      {
        throw new IllegalArgumentException(namedDataSchema.getFullName() + " has name that does not match filename '" +
                                               schemaSourceFile.getAbsolutePath() + "'");
      }

      final String directory = schemaSourceFile.getParentFile().getAbsolutePath();
      if (!directory.endsWith(namespace.replace('.', File.separatorChar)))
      {
        throw new IllegalArgumentException(namedDataSchema.getFullName() + " has namespace that does not match " +
                                               "file path '" + schemaSourceFile.getAbsolutePath() + "'");
      }
    }
  }

  /**
   * Whether a source file has already been resolved to data schemas.
   *
   * @param schemaSourceFile provides the source file.
   *
   * @return true if this source file has already been resolved to data schemas.
   */
  private boolean wasResolved(File schemaSourceFile)
  {
    final FileDataSchemaLocation schemaLocation = new FileDataSchemaLocation(schemaSourceFile);
    return _schemaResolver.locationResolved(schemaLocation);
  }

  /**
   * Parse a source file to obtain the data schemas contained within.
   *
   * @param schemaSourceFile provides the source file.
   *
   * @param result {@link ParseResult} to update.
   *
   * @return the data schemas within the source file.
   *
   * @throws IOException if there is a file access error.
   */
  private List<DataSchema> parseSchema(final File schemaSourceFile, ParseResult result)
      throws IOException
  {
    final SchemaParser parser = new SchemaParser(_schemaResolver);
    final FileInputStream schemaStream = new SchemaFileInputStream(schemaSourceFile);
    try
    {
      parser.setLocation(new FileDataSchemaLocation(schemaSourceFile));
      parser.parse(schemaStream);
      if (parser.hasError())
      {
        return Collections.emptyList();
      }
      return parser.topLevelDataSchemas();
    }
    finally
    {
      schemaStream.close();
      if (parser.hasError())
      {
        result.getMessage().append(schemaSourceFile.getPath() + ",");
        result.getMessage().append(parser.errorMessage());
      }
    }
  }

  /**
   * Append source files that were resolved through {@link DataSchemaResolver} to the provided list.
   *
   * @param result to append the files that were resolved through {@link DataSchemaResolver}.
   */
  private void appendSourceFilesFromSchemaResolver(ParseResult result)
  {
    for (Map.Entry<String, DataSchemaLocation> entry : _schemaResolver.nameToDataSchemaLocations().entrySet())
    {
      final File sourceFile = entry.getValue().getSourceFile();
      if (sourceFile != null)
      {
        result.getSourceFiles().add(sourceFile);
      }
    }
  }

  /**
   * Represent the result of schema parsing. Consist of two parts: schema from file path and from schema name, based on user input.
   * The two parts are mutually exclusive, and the union of two consists of all schema resolved.
   */
  public static class ParseResult
  {
    // use collections to maintain order
    private final Collection<CodeUtil.Pair<DataSchema, File>> _schemaAndFiles = new ArrayList<CodeUtil.Pair<DataSchema, File>>();
    private final Collection<CodeUtil.Pair<DataSchema, String>> _schemaAndNames = new ArrayList<CodeUtil.Pair<DataSchema, String>>();
    private final Set<File> _sourceFiles = new HashSet<File>();
    private final StringBuilder _message = new StringBuilder();

    /**
     * @return pairs of schema and its corresponding file for those schemas passed with their file paths to the parser
     */
    public Collection<CodeUtil.Pair<DataSchema, File>> getSchemaAndFiles()
    {
      return _schemaAndFiles;
    }

    /**
     * @return pairs of schema and its corresponding name for those schemas passed with their name to the parser
     */
    public Collection<CodeUtil.Pair<DataSchema, String>> getSchemaAndNames()
    {
      return _schemaAndNames;
    }

    /**
     * @return all source files of resolved schema
     */
    public Set<File> getSourceFiles()
    {
      return _sourceFiles;
    }

    public StringBuilder getMessage()
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
