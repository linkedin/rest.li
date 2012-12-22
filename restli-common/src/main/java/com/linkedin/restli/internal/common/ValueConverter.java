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

package com.linkedin.restli.internal.common;


import com.linkedin.data.ByteString;
import com.linkedin.data.template.DataTemplateUtil;


/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class ValueConverter
{
  /**
   * Attempt to coerce the given string into an object of the given class.
   *
   * @param value the String value to coercer. could be null
   * @param clazz the class to coercer the string into
   *              valid values are string, short, int, long, float, double (and equivalent boxed types), {@link ByteString} or enum.
   * @return an Object of the type coerced to. if value is null, return null
   */
  public static Object coerceString(String value, Class<?> clazz)
  {
    if (value == null)
    {
      return null;
    }
    else if (String.class.equals(clazz))
    {
      return value;
    }
    else if (Short.TYPE.equals(clazz) || Short.class.equals(clazz))
    {
      return Short.valueOf(value);
    }
    else if (Double.TYPE.equals(clazz) || Double.class.equals(clazz))
    {
      return Double.valueOf(value);
    }
    else if (Float.TYPE.equals(clazz) || Float.class.equals(clazz))
    {
      return Float.valueOf(value);
    }
    else if (Boolean.class.equals(clazz) || Boolean.TYPE.equals(clazz))
    {
      return Boolean.valueOf(value);
    }
    else if (Integer.TYPE.equals(clazz) || Integer.class.equals(clazz))
    {
      return Integer.valueOf(value);
    }
    else if (Long.TYPE.equals(clazz) || Long.class.equals(clazz))
    {
      return Long.valueOf(value);
    }
    else if (ByteString.class.equals(clazz))
    {
      return ByteString.copyAvroString(value, true);
    }
    else if (clazz.isEnum())
    {
      return DataTemplateUtil.coerceOutput(value, clazz);
    }

    throw new IllegalArgumentException("Cannot coerce String to type: " + value + " -> " + clazz.getName());
  }
}
