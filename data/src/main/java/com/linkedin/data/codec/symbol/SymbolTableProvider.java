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

import java.net.URI;
import java.util.Map;


/**
 * An abstraction to manage shared symbol tables keyed by name.
 */
public interface SymbolTableProvider {

  /**
   * Get the symbol table with the given name.
   *
   * @param symbolTableName The name of the symbol table to lookup.
   *
   * @return The symbol table if found, null otherwise.
   */
  default SymbolTable getSymbolTable(String symbolTableName)
  {
    if (symbolTableName != null)
    {
      throw new IllegalStateException("Not configured to fetch symbol table with name: " + symbolTableName);
    }

    return null;
  }

  /**
   * Get the symbol table for the given a request.
   *
   * @param requestUri The URI of the request.
   *
   * @return The symbol table if found, null otherwise.
   */
  default SymbolTable getRequestSymbolTable(URI requestUri)
  {
    return null;
  }

  /**
   * Get the response symbol table.
   *
   * @param requestUri     The request URI.
   * @param requestHeaders The request headers.
   *
   * <p>Choosing the response symbol table based on the request URI and headers is useful in scenarios where the
   * client may not have access to the latest server symbol table at runtime (eg: Remote clients).</p>
   *
   * @return The symbol table if found, null otherwise.
   */
  default SymbolTable getResponseSymbolTable(URI requestUri, Map<String, String> requestHeaders)
  {
    return null;
  }
}
