package com.linkedin.data.schema.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * This is the java annotation class for a {@link SchemaAnnotationHandler} implementation
 *
 * All custom handlers that implements {@link SchemaAnnotationHandler} need to add this annotation to the class so the Pegasus plugin could
 * recognize the handler in order to process and validate schema annotations.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RestLiSchemaAnnotationHandler
{
}
