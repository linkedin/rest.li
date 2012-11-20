package com.linkedin.restli.restspec;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Empty custom annotation for .restspec.json which should show up as
 * "com.linkedin.restli.restspec.EmptyAnnotation" : {}
 *
 * @author Keren Jin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@RestSpecAnnotation
public @interface EmptyAnnotation
{
}
