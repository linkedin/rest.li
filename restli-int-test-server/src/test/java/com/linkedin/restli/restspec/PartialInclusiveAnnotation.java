package com.linkedin.restli.restspec;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @author Keren Jin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
// annotation without @RestSpecAnnotation is always excluded
// alternatively, annotate with @RestSpecAnnotation(exclude = true)
public @interface PartialInclusiveAnnotation
{
  @RestSpecAnnotation(skipDefault = true)
  int used() default 0;

  String unused() default "";
}
