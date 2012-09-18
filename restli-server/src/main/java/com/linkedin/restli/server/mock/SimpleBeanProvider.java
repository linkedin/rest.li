/*
   Copyright (c) 2012 LinkedIn Corp.

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

/**
 * $Id: $
 */

package com.linkedin.restli.server.mock;

import java.util.HashMap;
import java.util.Map;

import com.linkedin.restli.server.resources.BeanProvider;

/**
 * @author Josh Walker
 * @version $Revision: $
 *
 * Simple bean provider which internally uses a Map as key/value store
 * Intended for usage in test code only
 */
public class SimpleBeanProvider implements BeanProvider
{
  private final Map<String, Object> _beans;

  public SimpleBeanProvider()
  {
    _beans = new HashMap<String, Object>();
  }

  public SimpleBeanProvider add(final String name, final Object bean)
  {
    synchronized (_beans)
    {
      _beans.put(name, bean);
    }
    return this;
  }

  @Override
  public Object getBean(final String name)
  {
    synchronized (_beans)
    {
      return _beans.get(name);
    }
  }

  @Override
  public <T> Map<String, T> getBeansOfType(final Class<T> clazz)
  {
    Map<String, T> result = new HashMap<String, T>();
    synchronized (_beans)
    {
      for (Map.Entry<String, Object> entry : _beans.entrySet())
      {
        if (clazz.isAssignableFrom(entry.getValue().getClass()))
        {
          @SuppressWarnings("unchecked")
          T val = (T) entry.getValue();
          result.put(entry.getKey(), val);
        }
      }
    }
    return result;
  }
}
