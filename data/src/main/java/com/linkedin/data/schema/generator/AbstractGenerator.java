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
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.schema.SchemaParserFactory;
import com.linkedin.data.schema.resolver.DefaultDataSchemaResolver;
import com.linkedin.data.schema.resolver.FileDataSchemaLocation;
import com.linkedin.data.schema.resolver.FileDataSchemaResolver;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayDeque;
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

  /**
   * Handle a data schema belonging to a source.
   *
   * @param schema belonging to a source.
   */
  protected abstract void handleSchema(DataSchema schema);

  protected StringBuilder getMessage()
  {
    return _message;
  }

  protected DataSchemaResolver getSchemaResolver()
  {
    return _schemaResolver;
  }

  /**
   * Return the resolver path by looking up the system property with {@link #GENERATOR_RESOLVER_PATH} as key.
   *
   * @return the resolver path.
   */
  protected String getResolverPath()
  {
    return System.getProperty(GENERATOR_RESOLVER_PATH);
  }

  /**
   * Initialize my {@link DataSchemaResolver} with the resolver path returned
   * by {@link #getResolverPath()}.
   */
  protected void initSchemaResolver()
  {
    String resolverPath = getResolverPath();
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
    List<File> sourceFiles = new ArrayList<File>();

    for (String source : sources)
    {
      File sourceFile = new File(source);
      if (sourceFile.exists())
      {
        if (sourceFile.isDirectory())
        {
          List<File> sourceFilesInDirectory = sourceFilesInDirectory(sourceFile, new NameEndsWithFilter(FileDataSchemaResolver.DEFAULT_EXTENSION));
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

  /**
   * Scan a directory for source files.
   *
   * Recursively scans a directory for source files.
   * Recursive into each directory.
   * Invoke the provided filter on each non-directory file, if the
   * filter accepts the file, then add this file to the list of
   * files to return.
   *
   * @param directory provides the directory to scan for source files.
   * @param fileFilter to apply to each non-directory file.
   * @return list of source files found in the directory.
   */
  protected static List<File> sourceFilesInDirectory(File directory, FileFilter fileFilter)
  {
    List<File> result = new ArrayList<File>();
    ArrayDeque<File> deque = new ArrayDeque<File>();
    deque.addFirst(directory);
    while (deque.isEmpty() == false)
    {
      File file = deque.removeFirst();
      if (file.isDirectory())
      {
        File[] filesInDirectory = file.listFiles();
        for (File f : filesInDirectory)
        {
          deque.addLast(f);
        }
      }
      else if (fileFilter.accept(file))
      {
        result.add(file);
      }
    }
    return result;
  }

  protected static class NameEndsWithFilter implements FileFilter
  {
    private final String _extension;

    public NameEndsWithFilter(String extension)
    {
      _extension = extension;
    }

    public boolean accept(File file)
    {
      return file.getName().endsWith(_extension);
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
      handleSchema(schema);
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
    parser.setLocation(new FileDataSchemaLocation(schemaSourceFile));
    parser.parse(new FileInputStream(schemaSourceFile) {
      @Override
      public String toString()
      {
        return schemaSourceFile.toString();
      }
    });
    if (parser.hasError())
    {
      getMessage().append(parser.errorMessage());
      return Collections.emptyList();
    }
    return parser.topLevelDataSchemas();
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

  /**
   * Compute the most recent last modified time of the provided files.
   *
   * @param files to compute most recent modified time from.
   * @return the most resent last modified of the provided files.
   */
  protected long mostRecentLastModified(List<File> files)
  {
    long mostRecent = 0L;
    for (File file : files)
    {
      long fileLastModified = file.lastModified();
      if (mostRecent < fileLastModified)
      {
        mostRecent = fileLastModified;
      }
    }
    return mostRecent;
  }

  /**
   * Determine whether the provided files has been modified more recently than the provided time.
   *
   * @param files whose last modified times will be compared to provided time.
   * @param time to compare the files' last modified times to.
   * @return true if the provided files has been modified more recently than the provided time.
   */
  protected boolean filesLastModifiedMoreRecentThan(List<File> files, long time)
  {
    for (File file : files)
    {
      if (! file.exists() || time >= file.lastModified())
      {
        return false;
      }
    }
    return true;
  }

  /**
   * Whether the files that would be generated into the specified target directory
   * are more recent than the most recent source files.
   *
   * This used to check if the output file is already up-to-date and need not be
   * overwritten with generated output.
   *
   * @param sourceFiles provides the source files that were parsed.
   * @param targetFiles provides the files that would have been generated.
   * @return true if the files that would be generated are more recent than the most recent source files.
   */
  protected boolean upToDate(List<File> sourceFiles, List<File> targetFiles)
  {
    long sourceLastModified = mostRecentLastModified(sourceFiles);
    return filesLastModifiedMoreRecentThan(targetFiles, sourceLastModified);
  }
}
