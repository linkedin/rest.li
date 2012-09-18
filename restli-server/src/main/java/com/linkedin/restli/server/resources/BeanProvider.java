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

package com.linkedin.restli.server.resources;

import java.util.Map;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public interface BeanProvider
{
  /**
   * Returns bean by name or null if such bean does not exist.
   * @param name name of bean
   * @return bean instance or null if bean with given name does not exist
   */
  Object getBean(String name);

  /**
   * Returns map (name -> bean) of all beans which can be assigned to a variable declared
   * with class <code>clazz</code>. It means that only beans which have type of class
   * clazz or it's subclasses will be returned
   *
   * @param clazz
   * @return map (name -> bean) of all beans which can be assigned to a variable declared
   *         with class <code>clazz</code>
   */
  <T> Map<String, T> getBeansOfType(Class<T> clazz);
}
