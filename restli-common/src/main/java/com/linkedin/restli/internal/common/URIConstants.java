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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class URIConstants
{
  public final static char OBJ_START     = '(';
  public final static char OBJ_END       = ')';
  public final static char KEY_VALUE_SEP = ':';
  public final static char ITEM_SEP      = ',';
  public final static String LIST_PREFIX = "List";
  // empty strings are represented by two single quotes; like '' rather than just empty space.
  public final static char EMPTY_STR_CHAR = '\'';
  public final static String EMPTY_STRING_REP = "''";

  public static final char[] RESERVED_CHARS = { OBJ_START, KEY_VALUE_SEP, OBJ_END, ITEM_SEP, EMPTY_STR_CHAR };
  public static final Set<Character> GRAMMAR_CHARS = new HashSet<>(Arrays.asList(OBJ_START, KEY_VALUE_SEP, OBJ_END, ITEM_SEP));

  /**
   * Determine if this character is a Rest.li 2.0.0 URI grammar character.
   * This hard-coded function is an optimized alternative to checking membership in {@link #GRAMMAR_CHARS}.
   * If the protocol syntax is ever updated, this function must be updated accordingly.
   *
   * @param c the URI character being tested.
   * @return true if the character is a grammar character.
   */
  public static boolean isGrammarCharacter(char c)
  {
    return c == OBJ_START ||
        c == OBJ_END ||
        c == KEY_VALUE_SEP ||
        c == ITEM_SEP;
  }
}
