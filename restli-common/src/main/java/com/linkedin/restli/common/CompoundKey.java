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

package com.linkedin.restli.common;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.restli.internal.common.ValueConverter;


/**
 * Represents a compound identifier.
 *
 * @author dellamag
 */
public class CompoundKey
{
  private final Map<String, Object> _keys;

  public CompoundKey()
  {
    _keys = new HashMap<String, Object>(4);
  }

  /**
   * Initialize and return a CompoundKey.
   *
   * @param fieldValues DataMap representing the CompoundKey
   * @param fieldTypes Map of key name to class
   * @return a CompoundKey
   */
  public static CompoundKey fromValues(DataMap fieldValues, Map<String, Class<?>> fieldTypes)
  {
    CompoundKey result = new CompoundKey();
    for (Map.Entry<String, Object> entry : fieldValues.entrySet())
    {
      result.append(entry.getKey(),
                    ValueConverter.coerceString(entry.getValue().toString(), fieldTypes.get(entry.getKey())));
    }
    return result;
  }

  /**
   * Add the key with the given name and value to the CompoundKey.
   *
   * @param name name of the key
   * @param value value of the key
   * @return this
   */
  public CompoundKey append(String name, Object value)
  {
    if (name==null)
    {
      throw new IllegalArgumentException("name of CompoundKey part cannot be null");
    }
    if (value==null)
    {
      throw new IllegalArgumentException("value of CompoundKey part cannot be null");
    }

    _keys.put(name, value);
    return this;
  }

  /**
   * Get the Object associated with the given key name.
   *
   * @param name name of the key
   * @return an Object
   */
  public Object getPart(String name)
  {
    return _keys.get(name);
  }

  /**
   * Get the Object associated with the given key name as an Integer.
   *
   * @param name name of the key
   * @return an Integer
   */
  public Integer getPartAsInt(String name)
  {
    return (Integer)getPart(name);
  }

  /**
   * Get the Object associated with the given key name as a Long.
   *
   * @param name name of the key
   * @return a Long
   */
  public Long getPartAsLong(String name)
  {
    return (Long)getPart(name);
  }

  /**
   * Get the Object associated with the given key name as a String.
   *
   * @param name name of the key
   * @return a String
   */
  public String getPartAsString(String name)
  {
    return (String)getPart(name);
  }

  /**
   * @return the number of keys in this CompoundKey
   */
  public int getNumParts()
  {
    return _keys.size();
  }

  /**
   * @return a set of all the key names in this CompoundKey
   */
  public Set<String> getPartKeys()
  {
    return _keys.keySet();
  }

  @Override
  public int hashCode()
  {
    return _keys.hashCode();
  }

  @Override
  public boolean equals(Object obj)
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
    CompoundKey other = (CompoundKey) obj;
    if (!_keys.equals(other._keys))
    {
      return false;
    }

    return true;
  }

  @Override
  public String toString()
  {
    List<String> keyList = new ArrayList<String>(_keys.keySet());
    Collections.sort(keyList);

    StringBuilder b = new StringBuilder();
    boolean delimit=false;
    for (String keyPart : keyList)
    {
      if (delimit)
      {
        b.append(RestConstants.SIMPLE_KEY_DELIMITER);
      }
      try
      {
        b.append(URLEncoder.encode(keyPart, RestConstants.DEFAULT_CHARSET_NAME));
        b.append(RestConstants.KEY_VALUE_DELIMITER);
        b.append(URLEncoder.encode(stringifySimpleValue(_keys.get(keyPart)), RestConstants.DEFAULT_CHARSET_NAME));
      }
      catch (UnsupportedEncodingException e)
      {
        throw new RuntimeException("UnsupportedEncodingException while trying to encode the key", e);
      }
      delimit = true;
    }
    return b.toString();
  }

  // replicated from AbstractRequestBuilder.
  private static String stringifySimpleValue(Object value)
  {
    Class<?> valueClass = value.getClass();
    if (DataTemplateUtil.hasCoercer(valueClass))
    {
      @SuppressWarnings("unchecked")
      Class<Object> fromClass = (Class<Object>) value.getClass();
      return DataTemplateUtil.coerceInput(value, fromClass, Object.class).toString();
    }
    return value.toString();
  }
}
