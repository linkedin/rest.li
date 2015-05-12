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

package com.linkedin.data.schema.generator;


import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaLocation;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.schema.SchemaParserFactory;
import com.linkedin.data.schema.resolver.DefaultDataSchemaResolver;
import com.linkedin.data.schema.resolver.FileDataSchemaLocation;
import com.linkedin.data.schema.resolver.FileDataSchemaResolver;
import com.linkedin.util.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Supports parsing of input files and generating output derived from the input files.
 */
public abstract class AbstractGenerator
{
  /**
   * The system property that provides the resolver path.
   */
  public static final String GENERATOR_RESOLVER_PATH = "generator.resolver.path";

  private final StringBuilder _message = new StringBuilder();
  private DataSchemaResolver _schemaResolver = new DefaultDataSchemaResolver();

  protected static class Config
  {
    public Config(String resolverPath)
    {
      _resolverPath = resolverPath;
    }

    /**
     * @return the resolver path for the schema resolver.
     */
    public String getResolverPath()
    {
      return _resolverPath;
    }

    private final String _resolverPath;
  }

  /**
   * Handle a data schema belonging to a source.
   *
   * @param schema belonging to a source.
   */
  protected abstract void handleSchema(DataSchema schema);

  protected abstract Config getConfig();

  protected StringBuilder getMessage()
  {
    return _message;
  }

  protected DataSchemaResolver getSchemaResolver()
  {
    return _schemaResolver;
  }

  /**
   * Initialize my {@link DataSchemaResolver} with the resolver path.
   */
  protected void initSchemaResolver()
  {
    final String resolverPath = getConfig().getResolverPath();
    if (resolverPath != null)
    {
      _schemaResolver = new FileDataSchemaResolver(SchemaParserFactory.instance(), resolverPath);
    }
  }

  /**
   * Parses sources that specify paths to schema files and/or fully qualified schema names.
   *
   * @param sources provides the paths to schema files and/or fully qualified schema names.
   * @return source files that were read.
   * @throws IOException if there are problems opening or deleting files.
   */
  protected List<File> parseSources(String sources[]) throws IOException
  {
    try
    {
      List<File> sourceFiles = new ArrayList<File>();

      for (String source : sources)
      {
        File sourceFile = new File(source);
        if (sourceFile.exists())
        {
          if (sourceFile.isDirectory())
          {
            FileUtil.FileExtensionFilter filter = new FileUtil.FileExtensionFilter(FileDataSchemaResolver.DEFAULT_EXTENSION);
            List<File> sourceFilesInDirectory = FileUtil.listFiles(sourceFile, filter);
            for (File f : sourceFilesInDirectory)
            {
              parseFile(f);
              sourceFiles.add(f);
            }
          }
          else
          {
            parseFile(sourceFile);
            sourceFiles.add(sourceFile);
          }
        }
        else
        {
          StringBuilder errorMessage = new StringBuilder();
          DataSchema schema = getSchemaResolver().findDataSchema(source, errorMessage);
          if (schema == null)
          {
            getMessage().append("File cannot be opened or schema name cannot be resolved: " + source + "\n");
          }
          if (errorMessage.length() > 0)
          {
            getMessage().append(errorMessage.toString());
          }
          if (schema != null)
          {
            handleSchema(schema);
          }
        }
      }

      if (getMessage().length() > 0)
      {
        throw new IOException(getMessage().toString());
      }

      appendSourceFilesFromSchemaResolver(sourceFiles);

      return sourceFiles;
    }
    catch (RuntimeException e)
    {
      if (getMessage().length() > 0)
      {
        e = new RuntimeException("Unexpected " + e.getClass().getSimpleName() + " encountered.\n" +
                                 "This may be caused by the following parsing or processing errors:\n" +
                                 getMessage(),
                                 e);
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
  protected void parseFile(File schemaSourceFile) throws IOException
  {
    if (wasResolved(schemaSourceFile))
    {
      return;
    }

    List<DataSchema> schemas = parseSchema(schemaSourceFile);

    for (DataSchema schema : schemas)
    {
      validateSchemaWithFilepath(schemaSourceFile, schema);
      handleSchema(schema);
    }
  }

  /**
   * Checks that the schema name and namespace match the file name and path.  These must match for
   * FileDataSchemaResolver to find a schema pdscs by fully qualified name.
   *
   */
  private void validateSchemaWithFilepath(File schemaSourceFile, DataSchema schema)
  {
    if(schemaSourceFile != null && schemaSourceFile.isFile() && schema instanceof NamedDataSchema)
    {
      NamedDataSchema namedDataSchema = (NamedDataSchema)schema;
      String namespace = namedDataSchema.getNamespace();

      if(!FileUtil.removeFileExtension(schemaSourceFile.getName()).equalsIgnoreCase(namedDataSchema.getName()))
      {
        throw new IllegalArgumentException(namedDataSchema.getFullName() + " has name that does not match filename '" +
                schemaSourceFile.getAbsolutePath() + "'");
      }

      String directory = schemaSourceFile.getParentFile().getAbsolutePath();
      if(!directory.endsWith(namespace.replace('.', File.separatorChar)))
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
   * @return true if this source file has already been resolved to data schemas.
   */
  protected boolean wasResolved(File schemaSourceFile)
  {
    FileDataSchemaLocation schemaLocation = new FileDataSchemaLocation(schemaSourceFile);
    return getSchemaResolver().locationResolved(schemaLocation);
  }

  class SchemaFileInputStream extends FileInputStream
  {
    private File _schemaSourceFile;

    public SchemaFileInputStream(File file)
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

  /**
   * Parse a source file to obtain the data schemas contained within.
   *
   * @param schemaSourceFile provides the source file.
   * @return the data schemas within the source file.
   * @throws IOException if there is a file access error.
   */
  protected List<DataSchema> parseSchema(final File schemaSourceFile) throws IOException
  {
    SchemaParser parser = new SchemaParser(getSchemaResolver());
    FileInputStream schemaStream = new SchemaFileInputStream(schemaSourceFile);
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
        getMessage().append(schemaSourceFile.getPath() + ",");
        getMessage().append(parser.errorMessage());
      }
    }
  }

  /**
   * Append source files that were resolved through {@link DataSchemaResolver} to the provided list.
   *
   * @param sourceFiles to append the files that were resolved through {@link DataSchemaResolver}.
   */
  protected void appendSourceFilesFromSchemaResolver(List<File> sourceFiles)
  {
    for (Map.Entry<String, DataSchemaLocation> entry : getSchemaResolver().nameToDataSchemaLocations().entrySet())
    {
      DataSchemaLocation location = entry.getValue();
      File sourceFile = location.getSourceFile();
      if (sourceFile != null)
      {
        sourceFiles.add(sourceFile);
      }
    }
  }
}
