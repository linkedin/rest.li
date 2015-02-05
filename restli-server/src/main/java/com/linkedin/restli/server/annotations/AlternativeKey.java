/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.server.annotations;


import com.linkedin.data.template.KeyCoercer;
import com.linkedin.data.template.TyperefInfo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @author Moira Tagle
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AlternativeKey
{
  String name();

  /**
   * Class that can convert the alternative key to the canonical key and the canonical key to the alternative key.
   */
  Class<? extends KeyCoercer> keyCoercer();

  /**
   * Type of the alternative key.  Must be a String, primitive, or a custom type with a registered coercer.
   */
  Class<?> keyType();

  /**
   * The typeref of the custom key, if the key is a custom type.
   */
  Class<? extends TyperefInfo> keyTyperefClass() default RestAnnotations.NULL_TYPEREF_INFO.class;

  // request builders will be added in a later change as an optional parameter.
}
