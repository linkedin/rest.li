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

package com.linkedin.data.schema.grammar;

import com.linkedin.data.grammar.PdlParser;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;


/**
 * Utility methods for the Pdl.g4 antlr grammar.
 *
 * @author Joe Betz
 */
public class PdlParseUtils
{
  private static final Pattern BACKTICK_PATTERN = Pattern.compile("`");

  /**
   * Given a doc string comment, unescapes and extracts the contents.
   *
   * In PDL, doc string are expected to be formatted using markdown.
   *
   * @param schemaDoc provides the doc comment to extract and unescape.
   * @return a markdown formatted string.
   */
  public static String extractMarkdown(String schemaDoc)
  {
    String trimmed = schemaDoc.trim();
    String withoutMargin = stripMargin(trimmed.substring(3, trimmed.length() - 2).trim());
    return unescapeDocstring(withoutMargin.trim());
  }

  /**
   * Unescapes the markdown contents of a doc string.
   *
   * @param escaped provides the escaped markdown contents of a doc string.
   * @return a markdown formatted string.
   */
  private static String unescapeDocstring(String escaped)
  {
    // unescape "/*" and "*/"
    String commentUnescaped = escaped.replace("&#47;&#42;", "/*").replace("&#42;&#47;", "*/");
    return StringEscapeUtils.unescapeHtml4(commentUnescaped);
  }

  /**
   * Unescapes a PDL string literal.
   *
   * @param stringLiteral provides an escaped PDL string literal.
   * @return a string literal.
   */
  public static String extractString(String stringLiteral)
  {
    return StringEscapeUtils.unescapeJson(stringLiteral.substring(1, stringLiteral.length() - 1));
  }

  /**
   * Strips the left margin, delimited by '*' from text within a PDL doc comment.
   *
   * Based on Scala's implementation of StringLike.stripMargin.
   *
   * @param schemadoc provides a PDL doc contents with the margin still present.
   * @return a doc comment with the margin removed.
   */
  public static String stripMargin(String schemadoc)
  {
    char marginChar = '*';
    StringBuilder buf = new StringBuilder();
    for (String lineWithoutSeparator : schemadoc.split(System.lineSeparator()))
    {
      String line = lineWithoutSeparator + System.lineSeparator();
      int len = line.length();
      int index = 0;
      while (index < len && line.charAt(index) <= ' ')
      {
        index++;
      }

      if (index < len && line.charAt(index) == marginChar)
      {
        buf.append(line.substring(index + 1));
      }
      else
      {
        buf.append(line);
      }
    }
    return buf.toString();
  }

  /**
   * Unescape an escaped PDL identifier that has been escaped using `` to avoid collisions with
   * keywords.  E.g. `namespace`.
   *
   * @param identifier provides the identifier to escape.
   * @return an identifier.
   */
  public static String unescapeIdentifier(String identifier)
  {
    return BACKTICK_PATTERN.matcher(identifier).replaceAll("");
  }

  /**
   * Validate that an identifier is a valid pegasus identifier.
   * Identifiers are used both for property identifiers and pegasus identifiers.  Property
   * identifiers can have symbols (currently only '-') that are not allowed in pegasus identifiers.
   *
   * Because lexers cannot disambiguate between the two types of identifiers, we validate pegasus
   * identifiers in the parser using this method.
   *
   * @param identifier the identifier to validate.
   * @return the validated pegasus identifier.
   */
  public static String validatePegasusId(String identifier)
  {
    if (identifier.contains("-"))
    {
      throw new IllegalArgumentException("Illegal '-' in identifier: " + identifier);
    }
    return identifier;
  }

  /**
   * Concatenates identifiers into a '.' delimited string.
   *
   * @param identifiers provides the ordered list of identifiers to join.
   * @return a string.
   */
  public static String join(List<PdlParser.IdentifierContext> identifiers)
  {
    StringBuilder stringBuilder = new StringBuilder();
    Iterator<PdlParser.IdentifierContext> iter = identifiers.iterator();
    while (iter.hasNext())
    {
      stringBuilder.append(iter.next().value);
      if (iter.hasNext())
      {
        stringBuilder.append(".");
      }
    }
    return stringBuilder.toString();
  }

  /**
   * Deserializes a JSON number to a java Number.
   * @param string provide a string representation of a JSON number.
   * @return a Number.
   */
  public static Number toNumber(String string)
  {
    BigDecimal bigDecimal = new BigDecimal(string);
    if (StringUtils.containsAny(string, '.', 'e', 'E'))
    {
      double d = bigDecimal.doubleValue();
      if (Double.isFinite(d))
      {
        return d;
      }
      else
      {
        return bigDecimal;
      }
    }
    else
    {
      long l = bigDecimal.longValueExact();
      int i = (int) l;
      if (i == l)
      {
        return (int) l;
      }
      else
      {
        return l;
      }
    }
  }
}
