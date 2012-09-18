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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates implementations of {@link com.linkedin.restli.server.resources.CollectionResource} and variants.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RestLiCollection
{
  /** The parent resource class.  Optional - if not specified, this resource will be a
   * root resource */
  Class<?> parent() default RestAnnotations.ROOT.class;

  String name();

  /** The namespace of the resource, used to qualify the IDL name*/
  String namespace() default "";

  /** The symbolic name of the key for this resource e.g. 'groupID'. Optional, defaults to "id" */
  String keyName() default RestAnnotations.DEFAULT;
}
