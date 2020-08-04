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
 * Encapsulates symbol table metadata.
 */
public class SymbolTableMetadata
{
  /**
   * The server URI from which this symbol table was served.
   */
  private final String _serverNodeUri;

  /**
   * The name of the symbol table.
   */
  private final String _symbolTableName;

  /**
   * True if this symbol table is from a remote server, false otherwise.
   */
  private final boolean _isRemote;

  public SymbolTableMetadata(String serverNodeUri, String symbolTableName, boolean isRemote)
  {
    _serverNodeUri = serverNodeUri;
    _symbolTableName = symbolTableName;
    _isRemote = isRemote;
  }

  /**
   * @return The server URI from which this symbol table was served.
   */
  public String getServerNodeUri()
  {
    return _serverNodeUri;
  }

  /**
   * @return The name of the symbol table.
   */
  public String getSymbolTableName()
  {
    return _symbolTableName;
  }

  /**
   * @return True if this symbol table is from a remote server, false otherwise.
   */
  public boolean isRemote()
  {
    return _isRemote;
  }
}
