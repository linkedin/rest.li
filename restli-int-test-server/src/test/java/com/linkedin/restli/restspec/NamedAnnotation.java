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
 * @author Keren Jin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@RestSpecAnnotation(name = "namedAnnotation", skipDefault = true)
public @interface NamedAnnotation
{
  enum AnnotationEnum
  {
    ENUM_MEMBER_1,
    ENUM_MEMBER_2
  };

  boolean booleanField() default false;
  byte byteField() default 0;
  int intField() default 0;
  long longField() default 0L;
  float floatField() default 0F;
  double doubleField() default 0D;
  @RestSpecAnnotation(name = "myName")
  String stringField();
  AnnotationEnum enumField() default AnnotationEnum.ENUM_MEMBER_1;
  Class<?> classField() default Object.class;
  byte[] byteStringField() default {};
  int[] intArrayField() default {};
  UnnamedAnnotation[] simpleAnnotationArrayField() default {};
  PartialExclusiveAnnotation[] complexAnnotationArrayField() default {};
  NormalAnnotation normalAnnotationField() default @NormalAnnotation();
}
