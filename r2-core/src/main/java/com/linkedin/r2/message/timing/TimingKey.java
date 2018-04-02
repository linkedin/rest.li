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

package com.linkedin.r2.message.timing;

import com.linkedin.r2.message.RequestContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * A timing key uniquely identifies a timing record, which will be saved in {@link RequestContext}.
 *
 * @see TimingContextUtil
 * @author Xialin Zhu
 */
public class TimingKey
{
  private static final Map<String, TimingKey> _pool = new ConcurrentHashMap<>();

  private final String _name;

  private TimingKey(String name)
  {
    _name = name;
  }

  public String getName()
  {
    return _name;
  }

  /**
   * Register a new timing key for future use.
   * @param name Name of the key
   * @return A new timing key
   */
  public static TimingKey registerNewKey(String name)
  {
    if (_pool.containsKey(name))
    {
      throw new IllegalStateException("Timing key " + name + " has already been registered!");
    }
    TimingKey timingKey = new TimingKey(name);
    _pool.put(name, timingKey);
    return timingKey;
  }
}
