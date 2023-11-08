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

package com.linkedin.d2.balancer.properties.util;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.linkedin.util.ArgumentUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertyUtil
{
  // check notNull for required argument
  // catch ClassCastException or any other exception and throw IllegalArgumentException
  // with the scope, the exact key and value that caused the problem
  public static <T> T checkAndGetValue(Map<String, Object> map, String key, Class<T> clazz, String scope)
  {
    Object obj = map.get(key);
    try
    {
      ArgumentUtil.notNull(obj, key);
      if (clazz.isEnum())
      {
        @SuppressWarnings({"unchecked", "rawtypes"})
        T result = (T) Enum.valueOf((Class) clazz, ((String)obj).toUpperCase());
        return result;
      }
      else
      {
        return clazz.cast(obj);
      }
    }
    catch (NullPointerException e)
    {
      throw new IllegalArgumentException("In " + scope + ": illegal argument " + key + " is missing or null", e);
    }
    catch (ClassCastException e)
    {
      throw new IllegalArgumentException("In " + scope + ": illegal argument " + key + ": " + obj + " can not be casted to " + clazz, e);
    }
    catch (Exception e)
    {
      throw new IllegalArgumentException("In " + scope + ": illegal argument " + key + ": " + obj, e);
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> T mapGet(Map<String, Object> map, String key)
  {
    return (T) map.get(key);
  }

  @SuppressWarnings("unchecked")
  public static <T> T mapGetOrDefault(Map<String, Object> map, String key, T defaultValue)
  {
    T value = (T) map.get(key);
    if (value == null)
    {
      value = defaultValue;
    }
    return value;
  }

  public static Integer parseInt(String key, String intStr)
  {
    try
    {
      return Integer.parseInt(intStr);
    }
    catch (NumberFormatException e)
    {
      throw new IllegalArgumentException(key + "is not an Integer", e);
    }
  }

  public static Long parseLong(String key, String intStr)
  {
    try
    {
      return Long.parseLong(intStr);
    }
    catch (NumberFormatException e)
    {
      throw new IllegalArgumentException(key + "is not an long Integer", e);
    }
  }

  public static Double parseDouble(String key, String doubleStr)
  {
    try
    {
      return Double.parseDouble(doubleStr);
    }
    catch (NumberFormatException e)
    {
      throw new IllegalArgumentException(key + "is not a double", e);
    }
  }

  @SuppressWarnings({"unchecked"})
  public static <T> T coerce (Object value, Class<T> clazz)
  {
    if (clazz.isAssignableFrom(value.getClass()))
    {
      return (T) value;
    }
    if (value instanceof String)
    {
      String str = (String) value;
      if (clazz.equals(Double.class))
      {
        return (T) Double.valueOf(Double.parseDouble(str));
      }
      if (clazz.equals(Float.class))
      {
        return (T) Float.valueOf(Float.parseFloat(str));
      }
      if (clazz.equals(Long.class))
      {
        return (T) Long.valueOf(Long.parseLong(str));
      }
      if (clazz.equals(Integer.class))
      {
        return (T) Integer.valueOf(Integer.parseInt(str));
      }
      if (clazz.equals(Boolean.class))
      {
        return (T) Boolean.valueOf(Boolean.parseBoolean(str));
      }
    }
    else if (value instanceof Double && clazz.equals(Integer.class))
    {
      return (T) Integer.valueOf((int) ((Double) value).doubleValue());
    }
    else
    {
      throw new IllegalArgumentException("Cannot convert value of " + value.getClass() +
          " to class = " + clazz.getName());
    }
    return (T) value;
  }

  /**
   * Efficiently translates a proto JSON {@link Struct} into a {@code Map<String, Object>} without additional
   * serialization or deserialization.
   */
  public static Map<String, Object> protoStructToMap(Struct struct)
  {
    if (struct.getFieldsCount() == 0) {
      return Collections.emptyMap();
    }
    Map<String, Object> map = new HashMap<>(struct.getFieldsMap().size());
    for (Map.Entry<String, Value> entry : struct.getFieldsMap().entrySet())
    {
      map.put(entry.getKey(), valueToObject(entry.getValue()));
    }
    return map;
  }

  private static Object valueToObject(Value value)
  {
    if (value.hasBoolValue())
    {
      return value.getBoolValue();
    }
    else if (value.hasStringValue())
    {
      return value.getStringValue();
    }
    else if (value.hasNumberValue())
    {
      return value.getNumberValue();
    }
    else if (value.hasNullValue())
    {
      return null;
    }
    else if (value.hasStructValue())
    {
      Map<String, Object> map = new HashMap<>(value.getStructValue().getFieldsCount());
      for (Map.Entry<String, Value> entry : value.getStructValue().getFieldsMap().entrySet())
      {
        map.put(entry.getKey(), valueToObject(entry.getValue()));
      }
      return map;
    }
    else if (value.hasListValue())
    {
      List<Object> list = new ArrayList<>(value.getListValue().getValuesCount());
      for (Value element : value.getListValue().getValuesList())
      {
        list.add(valueToObject(element));
      }
      return list;
    }
    else
    {
      throw new RuntimeException("Unexpected proto value of unknown type: " + value);
    }
  }
}
