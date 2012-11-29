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
