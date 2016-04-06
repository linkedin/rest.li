/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.client.util;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * This class holds methods useful when manipulating Client Builders.
 *
 * @author David Hoa
 * @version $Revision: $
 */
public class RestliBuilderUtils
{
  /**
   * Get the static original resource name from the client builder.
   *
   * @param type to get the primary resource name from. Has to be a master client builder, which is
   *             the builder that corresponds directly to the restli resource, and from which
   *             sub-builders are created.
   * @return name of the resource
   * @throws Runtime exception if unable to get the resource name from the builder
   */
  public static String getPrimaryResourceName(Class<?> type)
  {
    try
    {
      Method resourceMethod = type.getDeclaredMethod("getPrimaryResource");
      return (String)resourceMethod.invoke(null);
    }
    catch (IllegalAccessException e)
    {
      throw new RuntimeException("Error evaluating resource name for class: " + type.getName(), e);
    }
    catch (NoSuchMethodException e)
    {
      throw new RuntimeException("Class missing resource field: " + type.getName(), e);
    }
    catch (InvocationTargetException e)
    {
      throw new RuntimeException("Unable to instantiate class: " + type.getName(), e);
    }
  }
}
