package com.linkedin.restli.server.annotations;

import com.linkedin.restli.restspec.RestSpecAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indicating an entity is returned for the Rest.li method.
 *
 * @author Boyang Chen
 */
@Retention(RetentionPolicy.RUNTIME)
@RestSpecAnnotation(name = "returnEntity")
@Target(ElementType.METHOD)
public @interface ReturnEntity
{
}
