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
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.SchemaParser;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.JarFile;

/**
 * A {@link DataSchemaResolver} that searches the file system to locate a {@link NamedDataSchema}.
 * <p>
 *
 * The possible locations to search are each search path appended with
 * the transformed fully qualified name.
 * <p>
 *
 * The name is transformed by replacing dot ('.') with {@link File#separator}, and
 * appending the name with the specified file extension (provided to the constructor.)
 * If a specific file extension is not provided, the default file extension is
 * ".pdsc" (defined by the {@link #DEFAULT_EXTENSION} constant.)
 * <p>
 *
 * For example, if path is "/a:/b/c", the extension is ".pdsc" and
 * the name to find is "foo.bar.baz", then the possible
 * locations to search are "/a/foo/bar/baz.pdsc" and "/b/c/foo/bar/baz.pdsc".
 * <p>
 *
 * @author slim
 */
public class FileDataSchemaResolver extends AbstractDataSchemaResolver
{
  /**
   * The default path separator is provided by the system through {@link File#pathSeparator}.
   */
  public static final String DEFAULT_PATH_SEPARATOR = File.pathSeparator;

  /**
   * The default file name extension is ".pdsc".
   */
  public static final String DEFAULT_EXTENSION = SchemaParser.FILE_EXTENSION;

  /**
   * The file directory name for different types of schemas. Default is {@link SchemaDirectoryName#PEGASUS}
   * Ex "pegasus" for data or "extensions" for relationship extension schema files
   */
  private SchemaDirectoryName _schemasDirectoryName = SchemaDirectoryName.PEGASUS;

  /**
   * Constructor.
   *
   * @param parserFactory to be used to construct {@link SchemaParser}'s to parse located files.
   */
  public FileDataSchemaResolver(DataSchemaParserFactory parserFactory)
  {
    super(parserFactory);
  }

  /**
   * Constructor.
   *
   * @param parserFactory to be used to construct {@link SchemaParser}'s to parse located files.
   * @param dependencyResolver provides the parser used to resolve dependencies.  Note that
   *                     when multiple file formats (e.g. both .pdsc and .pdl) are in use,
   *                     a resolver that supports multiple file formats such as
   *                     {@link MultiFormatDataSchemaResolver} must be provided.
   */
  public FileDataSchemaResolver(DataSchemaParserFactory parserFactory, DataSchemaResolver dependencyResolver)
  {
    super(parserFactory, dependencyResolver);
  }

  /**
   * Constructor.
   *
   * @param parserFactory to be used to construct {@link SchemaParser}'s to parse located files.
   * @param paths is the search paths delimited by the default path separator.
   */
  public FileDataSchemaResolver(DataSchemaParserFactory parserFactory, String paths)
  {
    this(parserFactory);
    setPaths(paths);
  }

  /**
   * Constructor.
   *
   * @param parserFactory to be used to construct {@link SchemaParser}'s to parse located files.
   * @param paths is the search paths delimited by the default path separator.
   * @param dependencyResolver provides the parser used to resolve dependencies.  Note that
   *                     when multiple file formats (e.g. both .pdsc and .pdl) are in use,
   *                     a resolver that supports multiple file formats such as
   *                     {@link MultiFormatDataSchemaResolver} must be provided.
   */
  public FileDataSchemaResolver(DataSchemaParserFactory parserFactory, String paths, DataSchemaResolver dependencyResolver)
  {
    this(parserFactory, dependencyResolver);
    setPaths(paths);
  }

  /**
   * Constructor.
   *
   * @param parserFactory to be used to construct {@link SchemaParser}'s to parse located files.
   * @param paths is a list of search paths.
   */
  public FileDataSchemaResolver(DataSchemaParserFactory parserFactory, List<String> paths)
  {
    this(parserFactory);
    setPaths(paths);
  }

  /**
   * Specify the search paths as a string, with each search paths separated by
   * default path separator.
   *
   * @param paths is the search paths delimited by the default path separator.
   */
  public void setPaths(String paths)
  {
    setPaths(paths, DEFAULT_PATH_SEPARATOR);
  }

  /**
   * Specify the search paths as a string, with each search paths separated by
   * the provided separator.
   *
   * @param paths provides the search paths separated by the provided separator, or null for no search paths.
   * @param separator contain the characters that separate each search path.
   */
  public void setPaths(String paths, String separator)
  {
    List<String> list = new ArrayList<>();
    if (paths != null)
    {
      StringTokenizer tokenizer = new StringTokenizer(paths, separator);
      while (tokenizer.hasMoreTokens())
      {
        list.add(tokenizer.nextToken());
      }
    }
    setPaths(list);
  }

  /**
   * Specify the search paths as a list of paths.
   *
   * @param paths is a list of search paths.
   */
  public void setPaths(List<String> paths)
  {
    _paths = paths;
  }

  /**
   * Return the current search paths.
   *
   * @return the current search paths.
   */
  public List<String> getPaths()
  {
    return _paths;
  }

  /**
   * Return the current schema file directory name for schemas location
   */
  public SchemaDirectoryName getSchemasDirectoryName()
  {
    return _schemasDirectoryName;
  }

  /**
   * Sets the file directory name for schemas location dir.
   * If not set Defaults to {@link SchemaDirectoryName#PEGASUS}
   *
   * @param schemasDirectoryName schema directory name.
   */
  void setSchemasDirectoryName(SchemaDirectoryName schemasDirectoryName)
  {
    _schemasDirectoryName = schemasDirectoryName;
  }

  /**
   * Set the file extension to append.
   *
   * @param extension to append.
   */
  public void setExtension(String extension)
  {
    _extension = extension;
  }

  /**
   * Return the file extension to append.
   *
   * @return the file extension to append.
   */
  public String getExtension()
  {
    return _extension;
  }

  /**
   * The possible locations to search for the {@link NamedDataSchema}.
   */
  @Override
  protected Iterator<DataSchemaLocation> possibleLocations(String name)
  {
    name = name.replace('.', File.separatorChar);
    if (_extension.isEmpty() == false)
    {
      name += _extension;
    }
    final String transformedName = name;

    return new AbstractIterator(_paths)
    {
      @Override
      protected DataSchemaLocation transform(String path)
      {
        boolean isJar = path.endsWith(JAR_EXTENSION);
        if (isJar)
        {
          JarFile jarFile = _pathToJarFile.get(path);
          if (jarFile == null)
          {
            try
            {
              jarFile = new JarFile(path);
            }
            catch (IOException exc)
            {
              _pathToJarFile.put(path, null);
              return null;
            }
          }
          StringBuilder builder = new StringBuilder();
          // within a JAR file, files are treated as resources. Thus, we should lookup using the resource separator
          // character, which is '/'
          builder.append(_schemasDirectoryName.getName())
              .append('/')
              .append(transformedName.replace(File.separatorChar, '/'));
          return new InJarFileDataSchemaLocation(jarFile, builder.toString());
        }
        else
        {
          StringBuilder builder = new StringBuilder();
          builder.append(path);
          if (path.length() > 0 && path.charAt(path.length() - 1) != File.separatorChar)
          {
            builder.append(File.separatorChar);
          }
          builder.append(transformedName);
          return new FileDataSchemaLocation(new File(builder.toString()));
        }
      }
    };
  }

  @Override
  protected InputStream locationToInputStream(DataSchemaLocation location,
                                              StringBuilder errorMessageBuilder)
  {
    InputStream inputStream = ((InputStreamProvider) location).asInputStream(errorMessageBuilder);
    return inputStream;
  }

  private List<String> _paths = _emptyPaths;
  private String _extension = DEFAULT_EXTENSION;
  private final Map<String, JarFile> _pathToJarFile = new HashMap<>();

  private static final List<String> _emptyPaths = Collections.emptyList();

  /**
   * The jar file extension is ".jar".
   */
  private static final String JAR_EXTENSION = ".jar";
}
