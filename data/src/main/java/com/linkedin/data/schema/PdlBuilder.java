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

import com.linkedin.data.ByteString;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.Null;
import com.linkedin.data.codec.JacksonDataCodec;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringEscapeUtils;


/**
 * A {@link PdlBuilder} is used to build PDL output.
 *
 * <p>Unlike {@link SchemaToPdlEncoder}, this class handles the low-level details of encoding PDL data. Specific
 * implementations of this class are able to customize this logic in order to produce special formatting.</p>
 *
 * @author Evan Williams
 */
abstract class PdlBuilder
{
  private static final JacksonDataCodec JSON_CODEC = new JacksonDataCodec();

  // TODO: Put these in a unified "PDL constants" file
  private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
      "array", "enum", "fixed", "import", "includes", "map", "namespace", "optional", "package",
      "record", "typeref", "union", "null", "true", "false"
  ));
  private static final char ESCAPE_CHAR = '`';

  /**
   * Each subclass must define a provider for creating new instances.
   */
  protected interface Provider
  {
    PdlBuilder newInstance(Writer writer);
  }

  private final Writer _writer;

  PdlBuilder(Writer writer)
  {
    _writer = writer;
  }

  /**
   * Write raw .pdl text verbatim. All attempts to write to the writer are done via this method. Specific builder
   * implementations can override how write attempts are handled.
   * @param text text to write
   */
  PdlBuilder write(String text) throws IOException
  {
    _writer.write(text);
    return this;
  }

  /**
   * Writes a comma character.
   */
  PdlBuilder writeComma() throws IOException
  {
    write(",");
    return this;
  }

  /**
   * Writes a space character.
   */
  PdlBuilder writeSpace() throws IOException
  {
    write(" ");
    return this;
  }

  /**
   * Write a newline as .pdl source. Typically used in conjunction with {@link #indent()} and {@link #write(String)} to
   * emit an entire line of .pdl source.
   */
  abstract PdlBuilder newline() throws IOException;

  /**
   * Writes the current indentation as .pdl source. Typically used in conjunction with {@link #write(String)} and
   * {@link #newline()} to emit an entire line of .pdl source.
   */
  abstract PdlBuilder indent() throws IOException;

  /**
   * Increase the current indentation.
   */
  abstract PdlBuilder increaseIndent();

  /**
   * Decrease the current indentation.
   */
  abstract PdlBuilder decreaseIndent();

  /**
   * Write a documentation string to .pdl code.
   *
   * @param doc documentation to write.
   * @return true if any doc string was written
   */
  abstract boolean writeDoc(String doc) throws IOException;

  /**
   * Writes a set of schema properties that share a common prefix to .pdl.
   *
   * TODO: Support configuration for preferring collapsed "map" style over expanded "path" style.
   *
   * @param prefix provides the common prefix of all the properties.
   * @param properties provides the properties to write.
   */
  PdlBuilder writeProperties(List<String> prefix, Map<String, Object> properties) throws IOException
  {
    for (Map.Entry<String, Object> entry : properties.entrySet())
    {
      String key = entry.getKey();
      Object value = entry.getValue();

      // Copy the prefix path segments and append the current segment
      ArrayList<String> pathParts = new ArrayList<>(prefix);
      pathParts.add(key);

      if (value instanceof DataMap)
      {
        // Favor @x.y.z = "value" property encoding style over @x = { "y": { "z": "value" } }
        DataMap dm = (DataMap) value;
        if (!dm.isEmpty() && dm.size() == 1)
        {
          // encode non-empty value property like @x.y.z = "value"
          writeProperties(pathParts, dm);
        }
        else if (!pathParts.isEmpty())
        {
          // encode empty value property like @x.y = {}
          writeProperty(pathParts, dm);
        }
      }
      else if (Boolean.TRUE.equals(value))
      {
        // Use shorthand for boolean true.  Instead of writing "@deprecated = true",
        // write "@deprecated".
        indent().write("@").writePath(pathParts).newline();
      }
      else
      {
        writeProperty(pathParts, value);
      }
    }
    return this;
  }

  /**
   * Write a property string to this encoder's writer.
   * @param path provides the property's full path.
   * @param value provides the property's value, it may be any valid pegasus Data binding type (DataList, DataMap,
   *              String, Int, Long, Float, Double, Boolean, ByteArray)
   */
  private void writeProperty(List<String> path, Object value) throws IOException
  {
    indent().write("@").writePath(path).writeSpace().write("=").writeSpace().writeJson(value).newline();
  }

  /**
   * Writes a property path list as an escaped .pdl path string.
   * @param path property path list.
   */
  private PdlBuilder writePath(List<String> path) throws IOException
  {
    write(path.stream().map(PdlBuilder::escapePropertyKey).collect(Collectors.joining(".")));
    return this;
  }

  /**
   * Escape a property key for use in .pdl source code, for keys that would conflict with .pdl keywords or those with
   * dots it returns key escaped with a back-tick '`' character.
   * Eg, `namespace`
   *     `com.linkedin.validate.CustomValidator`
   *
   * @param propertyKey provides the property key to escape.
   * @return an escaped property key for use in .pdl source code.
   */
  private static String escapePropertyKey(String propertyKey)
  {
    propertyKey = propertyKey.trim();
    if (KEYWORDS.contains(propertyKey) || propertyKey.contains("."))
    {
      return ESCAPE_CHAR + propertyKey + ESCAPE_CHAR;
    }
    else
    {
      return propertyKey;
    }
  }

  /**
   * Writes an escaped identifier given an unescaped identifier.
   * @param unescapedIdentifier unescaped string to be escaped and written
   */
  PdlBuilder writeIdentifier(String unescapedIdentifier) throws IOException
  {
    write(escapeIdentifier(unescapedIdentifier));
    return this;
  }

  /**
   * Escape an identifier for use in .pdl source code, replacing all identifiers that would conflict with .pdl
   * keywords with a '`' escaped identifier. The identifier may be either qualified or unqualified.
   *
   * @param identifier provides the identifier to escape.
   * @return an escaped identifier for use in .pdl source code.
   */
  private static String escapeIdentifier(String identifier)
  {
    return Arrays.stream(identifier.split("\\.")).map(part -> {
      if (KEYWORDS.contains(part))
      {
        return ESCAPE_CHAR + part.trim() + ESCAPE_CHAR;
      }
      else
      {
        return part.trim();
      }
    }).collect(Collectors.joining("."));
  }

  /**
   * Writes an object as raw encoded JSON text.
   * Valid types: DataList, DataMap, String, Int, Long, Float, Double, Boolean, ByteArray
   *
   * @param value JSON object to write
   */
  PdlBuilder writeJson(Object value) throws IOException
  {
    write(toJson(value));
    return this;
  }

  /**
   * Serializes a pegasus Data binding type to JSON.
   * Valid types: DataMap, DataList, String, Number, Boolean, ByteString, Null
   *
   * @param value the value to serialize to JSON.
   * @return a JSON serialized string representation of the data value.
   */
  private String toJson(Object value) throws IOException
  {
    if (value instanceof DataMap)
    {
      return JSON_CODEC.mapToString((DataMap) value);
    }
    else if (value instanceof DataList)
    {
      return JSON_CODEC.listToString((DataList) value);
    }
    else if (value instanceof String)
    {
      return escapeJsonString((String) value);
    }
    else if (value instanceof Number)
    {
      return String.valueOf(value);
    }
    else if (value instanceof Boolean)
    {
      return String.valueOf(value);
    }
    else if (value instanceof ByteString)
    {
      return escapeJsonString(((ByteString) value).asAvroString());
    }
    else if (value instanceof Null)
    {
      // Some legacy schemas use union[null, xxx] to represent an optional field
      return "null";
    }
    else
    {
      throw new IllegalArgumentException("Unsupported data type: " + value.getClass());
    }
  }

  /**
   * JSON also allows the '/' char to be written in strings both unescaped ("/") and escaped ("\/").
   * StringEscapeUtils.escapeJson always escapes '/' so we deliberately use escapeJava instead, which
   * is exactly like escapeJson but without the '/' escaping.
   *
   * @param value unescaped string
   * @return escaped and quoted JSON string
   */
  private String escapeJsonString(String value)
  {
    return "\"" + StringEscapeUtils.escapeJava(value) + "\"";
  }
}
