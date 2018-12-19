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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


/**
 * A {@link SymbolTable} that stores symbol mappings in memory.
 */
public class InMemorySymbolTable implements SymbolTable {

  private final Map<String, Integer> _symbolNameToId;
  private final String[] _symbols;

  public InMemorySymbolTable(List<String> symbols)
  {
    _symbolNameToId = new HashMap<>();
    _symbols = new String[symbols.size()];

    for (int i = 0; i < symbols.size(); i++)
    {
      String symbol = symbols.get(i);
      _symbolNameToId.put(symbol, i);
      _symbols[i] = symbol;
    }
  }

  public InMemorySymbolTable(String symbolTableFilePath) throws IOException
  {
    this(getSymbols(symbolTableFilePath));
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
    if (symbolId >= 0 && symbolId < _symbols.length)
    {
      return _symbols[symbolId];
    }

    return null;
  }

  /**
   * Serialize symbol table to the given file path.
   */
  public void writeToFile(String symbolTableFilePath) throws IOException
  {
    Path path = Paths.get(symbolTableFilePath);

    try (BufferedWriter writer = Files.newBufferedWriter(path))
    {
      for (String symbol : _symbols)
      {
        writer.write(symbol);
        writer.write('\n');
      }
    }
  }

  private static List<String> getSymbols(String symbolTableFilePath) throws IOException
  {
    List<String> symbols = new ArrayList<>();
    try (Stream<String> stream = Files.lines(Paths.get(symbolTableFilePath)))
    {
      stream.forEach(symbols::add);
    }

    return symbols;
  }
}
