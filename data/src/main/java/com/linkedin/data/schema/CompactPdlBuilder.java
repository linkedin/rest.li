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

import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.template.JacksonDataTemplateCodec;
import java.io.IOException;
import java.io.Writer;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;


/**
 * An implementation of {@link PdlBuilder} that encodes PDL as compact as possible in order to reduce the size of the
 * resulting text.
 *
 * TODO: In order to further maximize space-efficiency, only use imports if the result is more compact.
 *
 * @author Evan Williams
 */
class CompactPdlBuilder extends PdlBuilder
{
  private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z0-9_\\-`]");
  private static final JacksonDataCodec JSON_CODEC = new JacksonDataCodec();
  private static final JacksonDataTemplateCodec JSON_DATA_TEMPLATE_CODEC = new JacksonDataTemplateCodec();

  static
  {
    JSON_CODEC.setSortKeys(true);
  }

  /**
   * See {@link PdlBuilder.Provider}.
   */
  static class Provider implements PdlBuilder.Provider
  {
    @Override
    public PdlBuilder newInstance(Writer writer)
    {
      return new CompactPdlBuilder(writer);
    }
  }

  // These are used to minimize whitespace
  private boolean _needsWhitespacePadding;
  private String _whitespaceBuffer;

  /**
   * Must construct via a {@link PdlBuilder.Provider}.
   */
  private CompactPdlBuilder(Writer writer)
  {
    super(writer);
  }

  /**
   * Write raw .pdl text verbatim.
   * @param text text to write
   */
  @Override
  PdlBuilder write(String text) throws IOException
  {
    if (text != null && !text.isEmpty())
    {
      final boolean writeWhitespaceBuffer = isIdentifierCharacter(text.charAt(0));
      processWhitespaceBuffer(writeWhitespaceBuffer);

      super.write(text);

      _needsWhitespacePadding = isIdentifierCharacter(text.charAt(text.length() - 1));
    }

    return this;
  }

  /**
   * Process the current whitespace buffer by clearing it and optionally writing it.
   * @param writeBuffer whether to write the whitespace buffer
   */
  private void processWhitespaceBuffer(boolean writeBuffer) throws IOException
  {
    if (_whitespaceBuffer != null && writeBuffer)
    {
      super.write(_whitespaceBuffer);
    }
    _whitespaceBuffer = null;
  }

  /**
   * Returns true if the character is part of the "identifier" character set, as specified in the grammar.
   * @param c character to check
   * @return whether this is an identifier character
   */
  private boolean isIdentifierCharacter(char c)
  {
    return IDENTIFIER_PATTERN.matcher(Character.toString(c)).matches();
  }

  /**
   * Writes a comma character.
   */
  @Override
  PdlBuilder writeComma()
  {
    writeWhitespace(",");
    return this;
  }

  /**
   * Writes a space character.
   */
  @Override
  PdlBuilder writeSpace()
  {
    writeWhitespace(" ");
    return this;
  }

  /**
   * Used to write anything considered by the grammar to be "whitespace". If appropriate, loads whitespace into a buffer
   * before actually writing it in order to minimize whitespace usage.
   * @param whitespace whitespace string to write
   */
  private void writeWhitespace(String whitespace)
  {
    if (_needsWhitespacePadding)
    {
      _whitespaceBuffer = whitespace;
      _needsWhitespacePadding = false;
    }
  }

  /**
   * Write a newline as .pdl source.
   * Typically used in conjunction with indent() and write() to emit an entire line of .pdl source.
   */
  @Override
  PdlBuilder newline()
  {
    writeComma();
    return this;
  }

  /**
   * Writes the current indentation as .pdl source. Typically used in conjunction with {@link #write(String)} and
   * {@link #newline()} to emit an entire line of .pdl source.
   */
  @Override
  PdlBuilder indent()
  {
    return this;
  }

  /**
   * Increase the current indentation.
   */
  @Override
  PdlBuilder increaseIndent()
  {
    return this;
  }

  /**
   * Decrease the current indentation.
   */
  @Override
  PdlBuilder decreaseIndent()
  {
    return this;
  }

  /**
   * Write a documentation string to .pdl code. Write the doc so that it occupies only one line.
   *
   * @param doc documentation to write.
   * @return true if any doc string was written
   */
  @Override
  boolean writeDoc(String doc) throws IOException
  {
    if (StringUtils.isNotBlank(doc))
    {
      write("/**").write(doc).write("*/");
      return true;
    }
    return false;
  }

  @Override
  PdlBuilder writeJson(Object value, DataSchema schema) throws IOException
  {
    if (schema != null)
    {
      write(toJson(value, JSON_DATA_TEMPLATE_CODEC, schema));
    }
    else
    {
      write(toJson(value, JSON_CODEC));
    }
    return this;
  }
}
