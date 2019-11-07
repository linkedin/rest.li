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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * A {@link SymbolTable} that stores symbol mappings in memory.
 */
public class InMemorySymbolTable implements SymbolTable {

  private final Map<String, Integer> _symbolNameToId;
  private final String[] _symbols;
  private final String _symbolTableName;

  public InMemorySymbolTable(String symbolTableName, List<String> symbols)
  {
    _symbolTableName = symbolTableName;
    _symbolNameToId = new HashMap<>();
    _symbols = new String[symbols.size()];

    for (int i = 0; i < symbols.size(); i++)
    {
      String symbol = symbols.get(i);
      _symbolNameToId.put(symbol, i);
      _symbols[i] = symbol;
    }
  }

  @Override
  public int getSymbolId(String symbolName)
  {

    Integer symbolId = _symbolNameToId.get(symbolName);
    if (symbolId != null)
    {
      return symbolId;
    }

    return UNKNOWN_SYMBOL_ID;
  }

  @Override
  public String getSymbolName(int symbolId)
  {
    try
    {
      return _symbols[symbolId];
    }
    catch (ArrayIndexOutOfBoundsException e)
    {
      return null;
    }
  }

  @Override
  public String getName() {
    return _symbolTableName;
  }

  @Override
  public int size()
  {
    return _symbols.length;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
    {
      return true;
    }

    if (o == null || getClass() != o.getClass())
    {
      return false;
    }

    InMemorySymbolTable that = (InMemorySymbolTable) o;
    return Arrays.equals(_symbols, that._symbols) && Objects.equals(_symbolTableName, that._symbolTableName);
  }

  @Override
  public int hashCode()
  {
    int result = Objects.hash(_symbolTableName);
    result = 31 * result + Arrays.hashCode(_symbols);
    return result;
  }
}
