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

package com.linkedin.restli.server.annotations;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.server.ResourceConfigException;


/**
 * @author Josh Walker
 * @version $Revision: $
 *
 * Rest method annotations for CRUD rest.li resources that don't extend resource templates with fixed sets of methods.
 *
 * Includes a map of annotations to resource methods.
 */

public class RestMethod
{
  /**
   *  This annotation defines mapping of RestMethod annotations to {@link com.linkedin.restli.common.ResourceMethod}
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.ANNOTATION_TYPE)
  private static @interface ToResourceMethod {
    ResourceMethod value();
  }

  static Map<Class<? extends Annotation>, ResourceMethod> _restMethodAnnotationToResourceMethodMap =
      new HashMap<>();
  // Build annotation-to-resourceMethod mapping. Check for absent or ambiguous mappings
  static
  {
    Class<?>[] classes = RestMethod.class.getClasses();
    for (Class<?> restMethodClass : classes)
    {
      if (Annotation.class.isAssignableFrom(restMethodClass))
      {
        Class<? extends Annotation> restMethodAnnotationClass =
            restMethodClass.asSubclass(Annotation.class);
        ToResourceMethod annotation =
            restMethodAnnotationClass.getAnnotation(ToResourceMethod.class);
        if (annotation == null)
        {
          throw new ResourceConfigException("Annotation "
              + restMethodAnnotationClass.getName()
              + " doesn't have ResourceMethod mapping");
        }
        else
        {
          ResourceMethod existingMapping =
              _restMethodAnnotationToResourceMethodMap.put(restMethodAnnotationClass,
                                                           annotation.value());
          if (existingMapping != null)
          {
            throw new ResourceConfigException("Annotation "
                + restMethodAnnotationClass.getName()
                + " has multiple ResourceMethod mappings");
          }
        }
      }
    }
  }

  public static ResourceMethod getResourceMethod(Class<? extends Annotation> annotationClass)
  {
    return _restMethodAnnotationToResourceMethodMap.get(annotationClass);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @ToResourceMethod(ResourceMethod.GET)
  public @interface Get  { }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @ToResourceMethod(ResourceMethod.CREATE)
  public @interface Create { }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @ToResourceMethod(ResourceMethod.UPDATE)
  public @interface Update { }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @ToResourceMethod(ResourceMethod.PARTIAL_UPDATE)
  public @interface PartialUpdate { }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @ToResourceMethod(ResourceMethod.DELETE)
  public @interface Delete{ }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @ToResourceMethod(ResourceMethod.BATCH_GET)
  public @interface BatchGet  { }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @ToResourceMethod(ResourceMethod.BATCH_CREATE)
  public @interface BatchCreate { }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @ToResourceMethod(ResourceMethod.BATCH_UPDATE)
  public @interface BatchUpdate { }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @ToResourceMethod(ResourceMethod.BATCH_PARTIAL_UPDATE)
  public @interface BatchPartialUpdate { }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @ToResourceMethod(ResourceMethod.BATCH_DELETE)
  public @interface BatchDelete{ }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @ToResourceMethod(ResourceMethod.GET_ALL)
  public @interface GetAll{ }
}
