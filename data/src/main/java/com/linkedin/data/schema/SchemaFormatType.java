/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.data.schema;

import com.linkedin.data.schema.grammar.PdlSchemaParserFactory;


/**
 * Representation of a particular schema format type.
 *
 * @author Evan Williams
 */
public enum SchemaFormatType
{
  PDSC(SchemaParserFactory.instance()),
  PDL(PdlSchemaParserFactory.instance());

  SchemaFormatType(DataSchemaParserFactory schemaParserFactory)
  {
    _schemaParserFactory = schemaParserFactory;
  }

  private final DataSchemaParserFactory _schemaParserFactory;

  public DataSchemaParserFactory getSchemaParserFactory()
  {
    return _schemaParserFactory;
  }

  /**
   * Determines the schema format type corresponding with a given filename, or null if it's indeterminable.
   *
   * @param filename filename
   * @return schema format type or null
   */
  public static SchemaFormatType fromFilename(String filename)
  {
    if (filename == null)
    {
      return null;
    }

    final int startIndex = filename.lastIndexOf(".") + 1;

    if (startIndex == filename.length())
    {
      return null;
    }

    return fromFileExtension(filename.substring(startIndex));
  }

  /**
   * Given some string file extension, determines the schema format type it represents.
   * Returns null if the file extension is an unrecognized file extension.
   *
   * @param fileExtension file extension string
   * @return schema format type or null
   */
  public static SchemaFormatType fromFileExtension(String fileExtension)
  {
    for (SchemaFormatType fileType : SchemaFormatType.values())
    {
      if (fileType.getSchemaParserFactory().getLanguageExtension().equalsIgnoreCase(fileExtension)) {
        return fileType;
      }
    }
    return null;
  }
}
