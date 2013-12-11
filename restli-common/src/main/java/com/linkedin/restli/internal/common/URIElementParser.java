/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.internal.common;


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.jersey.api.uri.UriComponent;

import java.util.LinkedList;
import java.util.Queue;


/**
 * A utility class for parsing Rest.li 2.0 protocol URI elements
 *
 * @see URIParamUtils for creating URI 2.0
 *
 * @author Moira Tagle
 * @version $Revision: $
 */

public class URIElementParser
{
  /**
   * Parse the given element into a {@link com.linkedin.data.DataComplex} or {@link String}.
   *
   * @param element the element to parse
   * @return the parsed object, which will either be a {@link com.linkedin.data.DataComplex} or a {@link String}.
   * @throws PathSegment.PathSegmentSyntaxException if the element is incorrectly formatted.
   */
  public static Object parse(String element) throws PathSegment.PathSegmentSyntaxException
  {
    Queue<Token> tokens = tokenizeElement(element);
    Object result = parseElement(tokens);

    if (!tokens.isEmpty())
    {
      throw new PathSegment.PathSegmentSyntaxException("tokens left over after parsing; first excess token: " + tokens.peek().toErrorString() );
    }

    return result;
  }

  private static Object parseElement(Queue<Token> tokenQueue) throws PathSegment.PathSegmentSyntaxException
  {
    Token nextToken = tokenQueue.peek();
    assertNotNull(nextToken);
    if (nextToken.isGrammar())
    {
      if (nextToken.grammarEquals(GrammarMarker.MAP_START))
      {
        return parseMap(tokenQueue);
      }
      else if (nextToken.grammarEquals(GrammarMarker.LIST_START))
      {
        return parseList(tokenQueue);
      }
      else
      {
        Token errorToken = tokenQueue.poll();
        throw new PathSegment.PathSegmentSyntaxException("unexpected token: " + errorToken.toErrorString() + " at start of element");
      }
    }
    else
    {
      // just a string
      return parseString(tokenQueue);
    }
  }

  private static String parseString(Queue<Token> tokenQueue) throws PathSegment.PathSegmentSyntaxException
  {
    Token strToken = tokenQueue.poll();
    assertNotNull(strToken);
    if (strToken.isGrammar())
    {
      throw new PathSegment.PathSegmentSyntaxException("expected string token, found grammar token: " + strToken.toErrorString());
    }
    return strToken.toString();
  }

  private static DataMap parseMap(Queue<Token> tokenQueue)throws PathSegment.PathSegmentSyntaxException
  {
    DataMap map = new DataMap();

    Token firstToken = tokenQueue.poll();
    assertExpectation(firstToken, GrammarMarker.MAP_START);

    Token nextToken = tokenQueue.peek();
    if (!nextToken.grammarEquals(GrammarMarker.OBJ_END))
    {
      parseMapElements(tokenQueue, map);
    }

    Token lastToken = tokenQueue.poll();
    assertExpectation(lastToken, GrammarMarker.OBJ_END);

    return map;
  }

  /**
   * @param tokenQueue the current {@link Queue} of {@link Token}s
   * @param map a {@link DataMap} to put the parsed elements into
   * @throws PathSegment.PathSegmentSyntaxException
   */
  private static void parseMapElements(Queue<Token> tokenQueue, DataMap map) throws PathSegment.PathSegmentSyntaxException
  {
    parseMapElement(tokenQueue, map);
    while (tokenQueue.peek().grammarEquals(GrammarMarker.ITEM_SEP))
    {
      tokenQueue.remove();
      parseMapElement(tokenQueue, map);
    }
  }

  private static void parseMapElement(Queue<Token> tokenQueue, DataMap map) throws PathSegment.PathSegmentSyntaxException
  {
    String key = parseString(tokenQueue);
    Token mapSep = tokenQueue.poll();
    assertExpectation(mapSep, GrammarMarker.MAP_SEP);
    Object value = parseElement(tokenQueue);
    map.put(key, value);

    Token nextToken = tokenQueue.peek();
    assertNotNull(nextToken);
  }

  private static DataList parseList(Queue<Token> tokenQueue) throws PathSegment.PathSegmentSyntaxException
  {
    DataList list = new DataList();

    Token firstToken = tokenQueue.poll();
    assertExpectation(firstToken, GrammarMarker.LIST_START);

    Token nextToken = tokenQueue.peek();
    if (!nextToken.grammarEquals(GrammarMarker.OBJ_END))
    {
      parseListElements(tokenQueue, list);
    }

    Token lastToken = tokenQueue.poll();
    assertExpectation(lastToken, GrammarMarker.OBJ_END);

    return list;
  }

  /**
   * @param tokenQueue the current {@link Queue} of {@link Token}s
   * @param list a {@link DataList} to put the parsed elements into
   * @throws PathSegment.PathSegmentSyntaxException
   */
  private static void parseListElements(Queue<Token> tokenQueue, DataList list) throws PathSegment.PathSegmentSyntaxException
  {
    list.add(parseListElement(tokenQueue));
    while (tokenQueue.peek().grammarEquals(GrammarMarker.ITEM_SEP))
    {
      tokenQueue.remove();
      list.add(parseListElement(tokenQueue));
    }
  }

  private static Object parseListElement(Queue<Token> tokenQueue) throws PathSegment.PathSegmentSyntaxException
  {
    Object element = parseElement(tokenQueue);
    Token nextToken = tokenQueue.peek();
    assertNotNull(nextToken);
    return element;
  }

  private static void assertExpectation(Token token, GrammarMarker marker) throws PathSegment.PathSegmentSyntaxException
  {
    assertNotNull(token);
    if (!token.grammarEquals(marker))
    {
      throw new PathSegment.PathSegmentSyntaxException("expected '" + marker.stringValue + "' but found " +  token.toErrorString());
    }
  }

  private static void assertNotNull(Token token) throws PathSegment.PathSegmentSyntaxException
  {
    if (token == null)
    {
      throw new PathSegment.PathSegmentSyntaxException("unexpected end of input");
    }
  }

  private static Queue<Token> tokenizeElement(String element) throws PathSegment.PathSegmentSyntaxException
  {
    Queue<Token> tokens = new LinkedList<Token>();
    StringBuilder currentToken = new StringBuilder();
    int currentTokenStartLoc = 0;
    int currentCharIndex = 0;
    for (char c : element.toCharArray())
    {
      if (isGrammarCharacter(c))
      {
        // special case for list start.
        if (c == URIConstants.OBJ_START  && currentToken.toString().equals(URIConstants.LIST_PREFIX))
        {
          tokens.add(new Token(GrammarMarker.LIST_START, currentTokenStartLoc));
          currentTokenStartLoc = currentCharIndex + 1;
        }
        else
        {
          // take care of any previous string token.
          if (currentToken.length() != 0)
          {
            tokens.add(createStringToken(currentToken.toString(), currentTokenStartLoc));
          }
          tokens.add(createGrammarToken(c, currentCharIndex));
          currentTokenStartLoc = currentCharIndex + 1;
        }
        currentToken = new StringBuilder();
      }
      else
      {
        currentToken.append(c);
      }
      currentCharIndex++;
    }

    if (currentToken.length() != 0)
    {
      tokens.add(createStringToken(currentToken.toString(), currentTokenStartLoc));
    }

    return tokens;
  }

  private static Token createStringToken(String strToken, int startLocation) throws PathSegment.PathSegmentSyntaxException
  {
    return new Token(decodeString(strToken), startLocation);
  }

  private  static String decodeString(String str) throws PathSegment.PathSegmentSyntaxException
  {
    if (str.equals(URIConstants.EMPTY_STRING_REP))
    {
      return "";
    }
    else
    {
      return UriComponent.decode(str, null); // todo query param to decode + as ' '?
    }
  }

  private static Token createGrammarToken(char c, int startLocation)
  {
    switch (c)
    {
      case URIConstants.OBJ_START:
        return new Token(GrammarMarker.MAP_START, startLocation);
      case URIConstants.OBJ_END:
        return new Token(GrammarMarker.OBJ_END, startLocation);
      case URIConstants.ITEM_SEP:
        return new Token(GrammarMarker.ITEM_SEP, startLocation);
      case URIConstants.KEY_VALUE_SEP:
        return new Token(GrammarMarker.MAP_SEP, startLocation);
      default:
        throw new IllegalArgumentException("cannot create non-grammar token '" + c + "' as grammar token");
    }
  }

  private static boolean isGrammarCharacter(char c)
  {
    return URIConstants.GRAMMAR_CHARS.contains(c);
  }

  private static enum GrammarMarker
  {
    LIST_START (URIConstants.LIST_PREFIX + URIConstants.OBJ_START),
    MAP_START (String.valueOf(URIConstants.OBJ_START)),
    OBJ_END (String.valueOf(URIConstants.OBJ_END)),
    ITEM_SEP (String.valueOf(URIConstants.ITEM_SEP)),
    MAP_SEP (String.valueOf(URIConstants.KEY_VALUE_SEP));

    public final String stringValue;

    GrammarMarker (String value)
    {
      stringValue = value;
    }
  }

  private static class Token
  {
    private String value;
    private GrammarMarker marker;
    private int startLocation;

    // used only for string tokens
    public Token(String str, int startLocation)
    {
      this.value = str;
      this.marker = null;
      this.startLocation = startLocation;
    }

    // used only for grammar tokens.
    public Token(GrammarMarker marker, int startLocation)
    {
      this.value = null;
      this.marker = marker;
      this.startLocation = startLocation;
    }

    public boolean isGrammar()
    {
      return (marker != null);
    }

    public boolean grammarEquals(GrammarMarker marker)
    {
      return (this.marker == marker);
    }

    public String toString()
    {
      if (isGrammar())
      {
        return marker.stringValue;
      }
      else
      {
        return value;
      }
    }

    public String toErrorString()
    {
      return "'" + toString() + "' (column " + startLocation + ")";
    }
  }

}
