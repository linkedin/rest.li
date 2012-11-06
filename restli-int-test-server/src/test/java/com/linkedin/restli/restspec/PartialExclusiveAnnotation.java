package com.linkedin.restli.restspec;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @author Keren Jin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@RestSpecAnnotation(name = "partial", skipDefault = true)
public @interface PartialExclusiveAnnotation
{
  int used1() default 1;
  int used2() default 2;

  @RestSpecAnnotation(exclude = true)
  String unused() default "";
}
