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
@RestSpecAnnotation(isDefaultSentinel = false)
public @interface UnnamedAnnotation
{
  int value() default -1;
}
