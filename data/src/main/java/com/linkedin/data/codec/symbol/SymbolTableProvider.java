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
 * An abstraction to manage vending symbol tables keyed by name.
 */
public interface SymbolTableProvider {

  /**
   * Get the symbol table with the given name.
   *
   * @param symbolTableName The name of the symbol table to lookup.
   *
   * @return The symbol table if found, null otherwise.
   */
  SymbolTable getSymbolTable(String symbolTableName);
}
