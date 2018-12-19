/*
   Copyright (c) 2018 LinkedIn Corp.

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


package com.linkedin.data.codec.symbol;

/**
 * A symbol table provides a two way mapping from string symbols to integer identifiers. Some codecs can optionally use
 * this capability to compress the payload, and make it more efficient to serialize/parse.
 */
public interface SymbolTable {

  /**
   * Placeholder ID for unknown symbols.
   */
  int UNKNOWN_SYMBOL_ID = -1;

  /**
   * Lookup the ID for the given symbol name.
   *
   * @param symbolName The symbol to lookup.
   *
   * @return The ID of the symbol if found, {@link #UNKNOWN_SYMBOL_ID} otherwise.
   */
  int getSymbolId(String symbolName);

  /**
   * Lookup the name for the given symbol ID.
   *
   * @param symbolId The symbol ID to lookup.
   *
   * @return The name of the symbol if found, null otherwise.
   */
  String getSymbolName(int symbolId);
}
