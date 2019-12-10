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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.linkedin.r2.message.RequestContext;


/**
 * A timing key uniquely identifies a timing record, which will be saved in {@link RequestContext}.
 *
 * @author Xialin Zhu
 * @see TimingContextUtil
 * @see TimingNameConstants
 */
public class TimingKey
{
  private static final Map<String, TimingKey> _pool = new ConcurrentHashMap<>();

  private final String _name;
  private final String _type;
  private final TimingImportance _timingImportance;

  /**
   * @param name Name of the key
   * @param type String that defines the type of the key
   * @param timingImportance {@link TimingImportance} of the key
   */
  private TimingKey(String name, String type, TimingImportance timingImportance)
  {
    _name = name;
    _type = type;
    _timingImportance = timingImportance;
  }

  public String getName()
  {
    return _name;
  }

  @Override
  public String toString()
  {
    return getName();
  }

  public String getType()
  {
    return _type;
  }

  public TimingImportance getTimingImportance()
  {
    return _timingImportance;
  }

  private static TimingKey registerNewKey(TimingKey timingKey)
  {
    if (_pool.putIfAbsent(timingKey.getName(), timingKey) != null)
    {
      throw new IllegalStateException("Timing key " + timingKey.getName() + " has already been registered!");
    }
    return timingKey;
  }

  /**
   * Register a new timing key for future use with default {@link TimingImportance#LOW}.
   *
   * @param uniqueNameAndType Name of the key (should be unique and defined in {@link TimingNameConstants})
   * @return A new timing key
   * @deprecated Use {{@link #registerNewKey(String, TimingImportance)}} instead.
   */
  @Deprecated
  public static TimingKey registerNewKey(String uniqueNameAndType)
  {
    return registerNewKey(new TimingKey(uniqueNameAndType, uniqueNameAndType, TimingImportance.LOW));
  }

  /**
   * Register a new timing key for future use with default {@link TimingImportance#LOW}.
   *
   * @param uniqueName Name of the key (should be unique and defined in {@link TimingNameConstants})
   * @param type       String that defines the type of the key
   * @return A new timing key
   * @deprecated Use {{@link #registerNewKey(String, String, TimingImportance)}} instead.
   */
  @Deprecated
  public static TimingKey registerNewKey(String uniqueName, String type)
  {
    return registerNewKey(new TimingKey(uniqueName, type, TimingImportance.LOW));
  }

  /**
   * Register a new timing key for future use.
   *
   * @param uniqueNameAndType Name of the key (should be unique and defined in {@link TimingNameConstants})
   * @param timingImportance {@link TimingImportance} of the key
   * @return A new timing key
   */
  public static TimingKey registerNewKey(String uniqueNameAndType, TimingImportance timingImportance)
  {
    return registerNewKey(new TimingKey(uniqueNameAndType, uniqueNameAndType, timingImportance));
  }

  /**
   * Register a new timing key for future use.
   *
   * @param uniqueName Name of the key (should be unique and defined in {@link TimingNameConstants})
   * @param type       String that defines the type of the key
   * @param timingImportance {@link TimingImportance} of the key
   * @return A new timing key
   */
  public static TimingKey registerNewKey(String uniqueName, String type, TimingImportance timingImportance)
  {
    return registerNewKey(new TimingKey(uniqueName, type, timingImportance));
  }
}
