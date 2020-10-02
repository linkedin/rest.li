/*
 * Copyright 2015 Coursera Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.linkedin.pegasus.generator;

import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaLocation;
import com.linkedin.data.schema.DataSchemaParserFactory;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.resolver.AbstractMultiFormatDataSchemaResolver;
import com.linkedin.data.schema.resolver.FileDataSchemaLocation;
import com.linkedin.data.schema.resolver.InJarFileDataSchemaLocation;
import com.linkedin.data.schema.resolver.MultiFormatDataSchemaResolver;
import com.linkedin.data.schema.resolver.SchemaDirectoryName;
import com.linkedin.util.FileUtil;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;


/**
 * Combines multiple file format specific parsers into a single parser for ".pdsc" and ".pdl" files.
 *
 * @author Joe Betz
 */
public class DataSchemaParser
{
  private final String _resolverPath;
  private final Map<String, FileFormatDataSchemaParser> _parserByFileExtension;
  private final AbstractMultiFormatDataSchemaResolver _resolver;

  /**
   * @param resolverPath provides the search paths separated by the system file separator, or null for no search paths.
   */
  public DataSchemaParser(String resolverPath)
  {
    this(resolverPath, AbstractMultiFormatDataSchemaResolver.BUILTIN_FORMAT_PARSER_FACTORIES);
  }

  /**
   * @param resolverPath provides the search paths separated by the system file separator, or null for no search paths.
   * @param parserFactoriesForFormats list of different formats that we want to parse
   */
  public DataSchemaParser(
      String resolverPath,
      List<DataSchemaParserFactory> parserFactoriesForFormats)
  {
    _parserByFileExtension = new HashMap<>();
    _resolverPath = resolverPath;
    MultiFormatDataSchemaResolver resolver =
      new MultiFormatDataSchemaResolver(resolverPath, parserFactoriesForFormats);
    this._resolver = resolver;
    init(resolver, resolverPath, parserFactoriesForFormats);
  }

  /**
   * @param resolverPath provides the search paths separated by the system file separator, or null for no search paths.
   * @param resolver A resolver that address its own specific requirement, for example, resolving extension schemas in a Jar file
   */
  public DataSchemaParser(String resolverPath, AbstractMultiFormatDataSchemaResolver resolver)
  {
    _parserByFileExtension = new HashMap<>();
    _resolverPath = resolverPath;
    this._resolver = resolver;
    init(resolver, resolverPath, MultiFormatDataSchemaResolver.BUILTIN_FORMAT_PARSER_FACTORIES);
  }

  public String getResolverPath()
  {
    return _resolverPath;
  }

  private static class FileExtensionFilter implements FileFilter
  {
    private final Set<String> extensions;

    public FileExtensionFilter(Set<String> extensions)
    {
      this.extensions = extensions;
    }

    @Override
    public boolean accept(File file)
    {
      return extensions.contains(FilenameUtils.getExtension(file.getName()));
    }
  }

  public DataSchemaResolver getSchemaResolver()
  {
    return _resolver;
  }

  public DataSchemaParser.ParseResult parseSources(String sources[]) throws IOException
  {
    Set<String> fileExtensions = _parserByFileExtension.keySet();
    Map<String, List<String>> byExtension = new HashMap<>(fileExtensions.size());
    for (String fileExtension : fileExtensions)
    {
      byExtension.put(fileExtension, new ArrayList<>());
    }

    // Extract all schema files from the given source paths and group by extension (JARs are handled specially)
    for (String source : sources)
    {
      final File sourceFile = new File(source);
      if (sourceFile.exists())
      {
        if (sourceFile.isDirectory())
        {
          // Source path is a directory, so recursively find all schema files contained therein
          final FileExtensionFilter filter = new FileExtensionFilter(fileExtensions);
          final List<File> sourceFilesInDirectory = FileUtil.listFiles(sourceFile, filter);
          // Add each schema to the corresponding extension's source list
          for (File f : sourceFilesInDirectory)
          {
            String ext = FilenameUtils.getExtension(f.getName());
            List<String> filesForExtension = byExtension.get(ext);
            if (filesForExtension != null)
            {
              filesForExtension.add(f.getAbsolutePath());
            }
          }
        }
        else if (sourceFile.getName().endsWith(".jar"))
        {
          // Source path is a JAR, so add it to each extension's source list.
          // The file-based parser for each extension will extract the JAR and process only files matching the extension
          byExtension.values().forEach(files -> files.add(sourceFile.getAbsolutePath()));
        }
        else
        {
          // Source path is a non-JAR file, so add it to the corresponding extension's source list
          String ext = FilenameUtils.getExtension(sourceFile.getName());
          List<String> filesForExtension = byExtension.get(ext);
          if (filesForExtension != null)
          {
            filesForExtension.add(sourceFile.getAbsolutePath());
          }
        }
      }
    }

    // Parse all schema files and JARs using the appropriate file format parser
    final ParseResult result = new ParseResult();
    for (Map.Entry<String, List<String>> entry : byExtension.entrySet())
    {
      String ext = entry.getKey();
      List<String> files = entry.getValue();
      _parserByFileExtension.get(ext).parseSources(files.toArray(new String[files.size()]), result);
    }

    return result;
  }

  private void init(AbstractMultiFormatDataSchemaResolver resolver,
      String resolverPath,
      List<DataSchemaParserFactory> parserFactoriesForFormats)
  {
    for (DataSchemaParserFactory parserForFormat : parserFactoriesForFormats)
    {
      FileFormatDataSchemaParser fileFormatParser =
          new FileFormatDataSchemaParser(resolverPath, resolver, parserForFormat);
      _parserByFileExtension.put(parserForFormat.getLanguageExtension(), fileFormatParser);
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
    private static final String EXTENSION_FILENAME_SUFFIX = "Extensions.pdl";
    // Sort the results to ensure that the output is deterministic for a given set of source inputs
    private final Map<DataSchema, DataSchemaLocation> _schemaAndLocations = new TreeMap<>(Comparator.comparing(DataSchema::toString));
    private final Set<File> _sourceFiles = new HashSet<>();
    protected final StringBuilder _messageBuilder = new StringBuilder();

    public Map<DataSchema, DataSchemaLocation> getSchemaAndLocations()
    {
      return _schemaAndLocations;
    }

    public Map<DataSchema, DataSchemaLocation> getBaseDataSchemaAndLocations()
    {
      return _schemaAndLocations.entrySet().stream().filter(entry -> !isExtensionSchemaLocation(entry))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<DataSchema, DataSchemaLocation> getExtensionDataSchemaAndLocations()
    {
      return _schemaAndLocations.entrySet().stream().filter(DataSchemaParser.ParseResult::isExtensionSchemaLocation)
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    static boolean isExtensionSchemaLocation(Map.Entry<DataSchema, DataSchemaLocation> entry)
    {
      DataSchemaLocation dataSchemaLocation = entry.getValue();
      if (dataSchemaLocation instanceof InJarFileDataSchemaLocation)
      {
        InJarFileDataSchemaLocation inJarFileDataSchemaLocation = (InJarFileDataSchemaLocation) dataSchemaLocation;
        return inJarFileDataSchemaLocation.getPathInJar().startsWith(SchemaDirectoryName.EXTENSIONS.getName());
      }
      else if (dataSchemaLocation instanceof FileDataSchemaLocation)
      {
        FileDataSchemaLocation fileDataSchemaLocation = (FileDataSchemaLocation) dataSchemaLocation;
        return fileDataSchemaLocation.getSourceFile().getName().endsWith(EXTENSION_FILENAME_SUFFIX) &&
            fileDataSchemaLocation.getSourceFile().getParent().indexOf(SchemaDirectoryName.EXTENSIONS.getName()) > 0;
      }
      return false;
    }

    public Set<File> getSourceFiles()
    {
      return _sourceFiles;
    }

    public String getMessage()
    {
      return _messageBuilder.toString();
    }

    public ParseResult addMessage(String message)
    {
      _messageBuilder.append(message);
      return this;
    }
  }
}
