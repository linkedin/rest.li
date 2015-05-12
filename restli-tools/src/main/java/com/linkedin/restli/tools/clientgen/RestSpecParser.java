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

package com.linkedin.restli.tools.clientgen;


import com.linkedin.pegasus.generator.CodeUtil;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.RestSpecCodec;
import com.linkedin.util.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * Generate {@link ResourceSchema} from Rest.li idl file.
 *
 * @author Keren Jin
 */
public class RestSpecParser
{
  private static final RestSpecCodec _codec = new RestSpecCodec();

  /**
   * @param sourcePaths can be either a directory (all idl files from it) or individual idl files
   */
  public ParseResult parseSources(String[] sourcePaths)
  {
    final ParseResult result = new ParseResult();

    for (String sourcePath : sourcePaths)
    {
      final File source = new File(sourcePath);
      if (!source.exists())
      {
        result._message.append("IDL file or directory doesn't exist: ").append(source.getAbsolutePath());
      }
      else
      {
        final File[] sources;

        if (source.isDirectory())
        {
          final FileUtil.FileExtensionFilter filter = new FileUtil.FileExtensionFilter(RestConstants.RESOURCE_MODEL_FILENAME_EXTENSION);
          final List<File> sourceFilesInDirectory = FileUtil.listFiles(source, filter);
          sources = sourceFilesInDirectory.toArray(new File[0]);
        }
        else
        {
          sources = new File[]{source};
        }

        for (File sourceFile : sources)
        {
          try
          {
            final ResourceSchema resource = _codec.readResourceSchema(new FileInputStream(sourceFile));
            result._schemaAndFiles.add(new CodeUtil.Pair<ResourceSchema, File>(resource, sourceFile));
            result._sourceFiles.add(sourceFile);
          }
          catch (IOException e)
          {
            result._message.append("Error processing file [").append(sourceFile.getAbsolutePath()).append(']').append(e.getMessage());
          }
        }
      }
    }

    return result;
  }

  public static class ParseResult
  {
    // use collections to maintain order
    private final Collection<CodeUtil.Pair<ResourceSchema, File>> _schemaAndFiles = new ArrayList<CodeUtil.Pair<ResourceSchema, File>>();
    private final Collection<File> _sourceFiles = new ArrayList<File>();
    private final StringBuilder _message = new StringBuilder();

    /**
     * @return pairs of schema and its corresponding file for those schemas passed with their file paths to the parser
     */
    public Collection<CodeUtil.Pair<ResourceSchema, File>> getSchemaAndFiles()
    {
      return _schemaAndFiles;
    }

    /**
     * @return all source files of resolved schema
     */
    public Collection<File> getSourceFiles()
    {
      return _sourceFiles;
    }

    public StringBuilder getMessage()
    {
      return _message;
    }
  }
}
