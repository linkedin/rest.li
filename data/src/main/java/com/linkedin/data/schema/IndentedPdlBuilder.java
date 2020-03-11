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

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.linkedin.data.codec.JacksonDataCodec;
import java.io.IOException;
import java.io.Writer;
import org.apache.commons.lang3.StringUtils;


/**
 * An implementation of {@link PdlBuilder} that encodes PDL in a very neat and human-readable fashion, making use of
 * indentation and newlines.
 *
 * @author Evan Williams
 */
class IndentedPdlBuilder extends PdlBuilder
{
  private final static int DEFAULT_INDENT_WIDTH = 2;

  /**
   * See {@link PdlBuilder.Provider}.
   */
  static class Provider implements PdlBuilder.Provider
  {
    @Override
    public PdlBuilder newInstance(Writer writer)
    {
      return new IndentedPdlBuilder(writer);
    }
  }

  private int _indentDepth = 0;

  /**
   * Must construct via a {@link PdlBuilder.Provider}.
   */
  private IndentedPdlBuilder(Writer writer)
  {
    super(writer);
  }

  /**
   * Write a newline as .pdl source.
   * Typically used in conjunction with indent() and write() to emit an entire line of .pdl source.
   */
  @Override
  PdlBuilder newline() throws IOException
  {
    write(System.lineSeparator());
    return this;
  }

  /**
   * Writes the current indentation as .pdl source. Typically used in conjunction with {@link #write(String)} and
   * {@link #newline()} to emit an entire line of .pdl source.
   */
  @Override
  PdlBuilder indent() throws IOException
  {
    write(getIndentSpaces(_indentDepth));
    return this;
  }

  /**
   * Increase the current indentation.
   */
  @Override
  PdlBuilder increaseIndent()
  {
    _indentDepth++;
    return this;
  }

  /**
   * Decrease the current indentation.
   */
  @Override
  PdlBuilder decreaseIndent()
  {
    _indentDepth--;
    return this;
  }

  /**
   * Write a documentation string to .pdl code. The documentation string will be embedded in a properly indented,
   * javadoc-style doc string using delimiters and margin.
   *
   * @param doc documentation to write.
   * @return true if any doc string was written
   */
  @Override
  boolean writeDoc(String doc) throws IOException
  {
    if (StringUtils.isNotBlank(doc))
    {
      writeLine("/**");
      for (String line : doc.split("\n"))
      {
        indent().write(" * ").write(line).newline();
      }
      writeLine(" */");
      return true;
    }
    return false;
  }

  @Override
  PdlBuilder writeJson(Object value) throws IOException
  {
    JacksonDataCodec jsonCodec = new JacksonDataCodec();
    DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
    prettyPrinter.indentObjectsWith(
        new DefaultIndenter(getIndentSpaces(1), DefaultIndenter.SYS_LF + getIndentSpaces(_indentDepth)));
    jsonCodec.setPrettyPrinter(prettyPrinter);
    write(toJson(value, jsonCodec));
    return this;
  }

  /**
   * Write an intended line of .pdl code.
   * The code will be prefixed by the current indentation and suffixed with a newline.
   * @param code provide the line of .pdl code.
   */
  private void writeLine(String code) throws IOException
  {
    indent().write(code).newline();
  }

  private String getIndentSpaces(int indentDepth)
  {
    final int numSpaces = indentDepth * DEFAULT_INDENT_WIDTH;
    final StringBuilder sb = new StringBuilder(numSpaces);
    for (int i = 0; i < numSpaces; i++)
    {
      sb.append(" ");
    }
    return sb.toString();
  }
}
