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

package com.linkedin.restli.restspec;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Meta-annotation which turns annotation used in Rest.li resource classes into restspec custom annotation.
 * The member values of such annotations are passed through to the generated .restspec.json files.
 *
 * This meta-annotation can be used to annotate annotation as well as member methods.
 * The method-level restspec annotation always overrides annotation-level counterpart.
 *
 * @author Keren Jin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface RestSpecAnnotation
{
  /**
   * @return subject name of the annotated in .restspec.json
   */
  String name() default DEFAULT_NAME;

  /**
   * @return exclude the annotated
   */
  boolean exclude() default DEFAULT_EXCLUDE;

  /**
   * @return true to skip outputting the member to .restspec.json when its value equals to the default value
   */
  boolean skipDefault() default DEFAULT_SKIP_DEFAULT;

  String DEFAULT_NAME = "";
  boolean DEFAULT_EXCLUDE = false;
  boolean DEFAULT_SKIP_DEFAULT = false;
}
