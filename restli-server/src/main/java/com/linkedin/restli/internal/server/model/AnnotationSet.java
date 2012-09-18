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

package com.linkedin.restli.internal.server.model;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class AnnotationSet
{
  private final Map<Class<? extends Annotation>, Annotation> _map;

  /**
   * Construct from annotations array.
   *
   * @param annos array of {@link Annotation}s
   */
  public AnnotationSet(final Annotation[] annos)
  {
    _map = new HashMap<Class<? extends Annotation>, Annotation>();
    for (Annotation anno : annos)
    {
      _map.put(anno.annotationType(), anno);
    }
  }

  /**
   * @param clazz type of the annotation to look up
   * @return true if the annotation with the specified type is present in the set, false
   *         otherwise
   */
  public boolean contains(final Class<? extends Annotation> clazz)
  {
    return _map.containsKey(clazz);
  }

  /**
   * @param clazz type of the annotation to get
   * @return {@link Annotation} matching the provided type
   */
  public <T extends Annotation> T get(final Class<T> clazz)
  {
    return clazz.cast(_map.get(clazz));
  }

  /**
   * Count the number of input annotations present in the set.
   *
   * @param classes annotations to count
   * @return number of annotations out of the input array present in the set
   */
  public int count(final Class<? extends Annotation> ... classes)
  {
    int result = 0;
    for (Class<? extends Annotation> clazz : classes)
    {
      if (_map.containsKey(clazz))
      {
        ++result;
      }
    }
    return result;
  }
}
