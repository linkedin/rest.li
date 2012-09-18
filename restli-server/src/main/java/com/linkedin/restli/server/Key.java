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

package com.linkedin.restli.server;

/**
 * @author dellamag
 */
public class Key
{
  private final String _name;
  private final Class<?> _type;

  /**
   * Constructor.
   *
   * @param name key name
   * @param type key class
   */
  public Key(final String name, final Class<?> type)
  {
    _name = name;
    _type = type;
  }

  public String getName()
  {
    return _name;
  }

  public Class<?> getType()
  {
    return _type;
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_name == null) ? 0 : _name.hashCode());
    result = prime * result + ((_type == null) ? 0 : _type.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj)
  {
    if (this == obj)
    {
      return true;
    }
    if (obj == null)
    {
      return false;
    }
    if (getClass() != obj.getClass())
    {
      return false;
    }
    Key other = (Key) obj;
    if (_name == null)
    {
      if (other._name != null)
      {
        return false;
      }
    }
    else if (!_name.equals(other._name))
    {
      return false;
    }
    if (_type == null)
    {
      if (other._type != null)
      {
        return false;
      }
    }
    else if (!_type.equals(other._type))
    {
      return false;
    }
    return true;
  }
}
