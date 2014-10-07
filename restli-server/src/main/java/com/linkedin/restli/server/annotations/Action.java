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

import com.linkedin.data.template.TyperefInfo;
import com.linkedin.restli.server.ResourceLevel;


/**
* @author Josh Walker
* @version $Revision: $
*/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Action
{
  String name();

  /**
   * Optional attribute used to indicate whether action should be exposed at the
   * collection level or the entity level. Default is ANY which indicates that the
   * action is defined on the containing resource.
   */
  ResourceLevel resourceLevel() default ResourceLevel.ANY;

  /**
   * Optional attribute used to indicate the desired typeref to use for primitive types.
   */
  Class<? extends TyperefInfo> returnTyperef() default RestAnnotations.NULL_TYPEREF_INFO.class;
}
