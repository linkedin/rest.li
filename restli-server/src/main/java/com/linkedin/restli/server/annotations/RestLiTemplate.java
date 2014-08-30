package com.linkedin.restli.server.annotations;


import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotation applied to the resource template class, e.g. {@link com.linkedin.restli.server.resources.CollectionResourceTemplate}.
 * We will check if the actual resource class is annotated with the correct annotation expected by the template class, expressed in
 * this annotation. If not, resource model generation will fail.
 *
 * @author Keren Jin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RestLiTemplate
{
  /**
   * @return Annotation expected by the resource template class
   */
  Class<? extends Annotation> expectedAnnotation();
}
