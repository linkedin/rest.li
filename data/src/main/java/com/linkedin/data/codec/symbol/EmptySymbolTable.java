/*
   Copyright (c) 2020 LinkedIn Corp.

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
 * An empty symbol table meant for avoiding branching checks in parsers/generators when no symbol table is used.
 */
public final class EmptySymbolTable implements SymbolTable
{
  /**
   * Shared singleton
   */
  public static final EmptySymbolTable SHARED = new EmptySymbolTable();

  private EmptySymbolTable()
  {
    // Prevent external instantiation.
  }

  @Override
  public int getSymbolId(String symbolName)
  {
    return SymbolTable.UNKNOWN_SYMBOL_ID;
  }

  @Override
  public String getSymbolName(int symbolId)
  {
    return null;
  }

  @Override
  public String getName()
  {
    // We don't want this table's name ever being inadvertently used.
    throw new UnsupportedOperationException();
  }

  @Override
  public int size()
  {
    return 0;
  }

  @Override
  public int hashCode()
  {
    return 0;
  }
}