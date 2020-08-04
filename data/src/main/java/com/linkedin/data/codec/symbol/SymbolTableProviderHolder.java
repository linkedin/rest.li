/*
   Copyright (c) 2019 LinkedIn Corp.

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
 * Holder of the global {@link SymbolTableProvider} instance.
 */
public class SymbolTableProviderHolder
{
  /**
   * Shared singleton.
   */
  public static final SymbolTableProviderHolder INSTANCE = new SymbolTableProviderHolder();

  /**
   * The encapsulated {@link SymbolTableProvider}
   */
  private volatile SymbolTableProvider _symbolTableProvider;

  /**
   * Private constructor to prevent external instantiation.
   */
  private SymbolTableProviderHolder()
  {
    _symbolTableProvider = new DefaultSymbolTableProvider();
  }

  /**
   * @return Encapsulated {@link SymbolTableProvider} instance.
   */
  public SymbolTableProvider getSymbolTableProvider()
  {
    return _symbolTableProvider;
  }

  /**
   * Set the encapsulated {@link SymbolTableProvider} instance. This is only meant to be invoked by
   * infrastructure code and not by application code.
   */
  public void setSymbolTableProvider(SymbolTableProvider symbolTableProvider)
  {
    assert symbolTableProvider != null;
    _symbolTableProvider = symbolTableProvider;
  }
}
