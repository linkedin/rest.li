package com.linkedin.restli.server.annotations;


import com.linkedin.restli.restspec.RestSpecAnnotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Marks a rest.li method as test only.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@RestSpecAnnotation(name = "testMethod", skipDefault = true)
public @interface TestMethod
{
  String doc() default "";
}
