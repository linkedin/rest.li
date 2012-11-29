package com.linkedin.restli.restspec;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Normal Java annotation
 *
 * @author Keren Jin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface NormalAnnotation
{
  String included() default "";
  @RestSpecAnnotation(exclude = true)
  String excluded() default "";
}
