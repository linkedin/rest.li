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
 * A top-level "all actions" resource. This is an optional resource type
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RestLiActions
{
  /**
   * The name of the resource.
   */
  String name();

  /**
   * The namespace of the resource, used to qualify the IDL name
   */
  String namespace() default "";

  /**
   * The d2 service name for this resource. Should be set only if the d2 service name is not the same as
   * the Rest.li resource name.
   *
   * <p>This is meant to be a hint to D2 based routing solutions, and is NOT directly used anywhere by
   * the rest.li framework, apart from enforcing that this value once set, cannot be changed for backward
   * compatibility reasons.</p>
   */
  String d2ServiceName() default RestAnnotations.DEFAULT;
}
