/*
   Copyright (c) 2021 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.linkedin.restli.internal.server.model;

import com.linkedin.data.ByteString;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.restspec.RestSpecAnnotation;
import com.linkedin.restli.server.annotations.Key;
import com.linkedin.restli.server.annotations.PathKeyParam;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestResourceModelAnnotation
{
  private static final DataMap EMPTY_DATA_MAP = new DataMap();

  private static final String SUPPORTED_ARRAY_MEMBERS = "SupportedArrayMembers";
  private static final String SUPPORTED_EMPTY = "SupportedEmpty";
  private static final String SUPPORTED_SCALAR_MEMBERS = "SupportedScalarMembers";
  private static final String SUPPORTED_SCALAR_MEMBERS_CUSTOMIZED = "SupportedScalarMembers";
  private static final String UNSUPPORTED_SCALAR_MEMBERS = "UnsupportedScalarMembers";

  @Test(description = "Empty input: empty data map")
  public void succeedsOnEmptyArrayInput()
  {
    final DataMap actual = ResourceModelAnnotation.getAnnotationsMap(new Annotation[] { });

    Assert.assertEquals(EMPTY_DATA_MAP, actual);
  }

  @Test(description = "Non-RestSpecAnnotation annotation: annotation is not recorded")
  public void succeedsOnNonRestSpecAnnotation()
  {
    @NonRestSpecAnnotation
    class LocalClass {
    }

    final Annotation[] annotations = LocalClass.class.getAnnotations();
    final DataMap actual = ResourceModelAnnotation.getAnnotationsMap(annotations);

    Assert.assertEquals(EMPTY_DATA_MAP, actual);
  }

  @Test(description = "Empty annotation: data map with annotation + no members")
  public void succeedsOnRestSpecAnnotationWithoutMembers()
  {
    @SupportedEmpty
    class LocalClass {
    }

    final Annotation[] annotations = LocalClass.class.getAnnotations();
    final DataMap actual = ResourceModelAnnotation.getAnnotationsMap(annotations);

    Assert.assertNotNull(actual);
    Assert.assertTrue(actual.get(SUPPORTED_EMPTY) instanceof DataMap);

    final DataMap dataMap = ((DataMap) actual.get(SUPPORTED_EMPTY));

    Assert.assertTrue(dataMap.isEmpty());
  }

  @Test(description = "Non-empty annotation, array members, default values: data map with annotation + all members")
  public void succeedsOnSupportedArrayMembersWithDefaultValues()
  {
    @SupportedArrayMembers
    class LocalClass {
    }

    final Annotation[] annotations = LocalClass.class.getAnnotations();
    final DataMap actual = ResourceModelAnnotation.getAnnotationsMap(annotations);

    Assert.assertNotNull(actual);
    Assert.assertTrue(actual.get(SUPPORTED_ARRAY_MEMBERS) instanceof DataMap);

    final DataMap dataMap = ((DataMap) actual.get(SUPPORTED_ARRAY_MEMBERS));

    Assert.assertEquals(dataMap.size(), 10);
    Assert.assertEquals(dataMap.get("annotationMembers").getClass(), DataList.class);
    Assert.assertEquals(dataMap.get("booleanMembers").getClass(), DataList.class);
    Assert.assertEquals(dataMap.get("byteMembers").getClass(), ByteString.class); // byte string
    Assert.assertEquals(dataMap.get("classMembers").getClass(), DataList.class);
    Assert.assertEquals(dataMap.get("doubleMembers").getClass(), DataList.class);
    Assert.assertEquals(dataMap.get("enumMembers").getClass(), DataList.class);
    Assert.assertEquals(dataMap.get("floatMembers").getClass(), DataList.class);
    Assert.assertEquals(dataMap.get("intMembers").getClass(), DataList.class);
    Assert.assertEquals(dataMap.get("longMembers").getClass(), DataList.class);
    Assert.assertEquals(dataMap.get("stringMembers").getClass(), DataList.class);
  }

  @Test(description = "Non-empty annotation, array members, overridden values: data map with annotation + all members")
  public void succeedsOnSupportedArrayMembersWithOverriddenValues()
  {
    @SupportedArrayMembers(
        booleanMembers = { },
        intMembers = { 1 }
    )
    class LocalClass {
    }

    final Annotation[] annotations = LocalClass.class.getAnnotations();
    final DataMap actual = ResourceModelAnnotation.getAnnotationsMap(annotations);

    Assert.assertNotNull(actual);
    Assert.assertTrue(actual.get(SUPPORTED_ARRAY_MEMBERS) instanceof DataMap);

    final DataMap dataMap = ((DataMap) actual.get(SUPPORTED_ARRAY_MEMBERS));

    Assert.assertEquals(dataMap.size(), 9);
    Assert.assertEquals(dataMap.get("annotationMembers").getClass(), DataList.class);
    Assert.assertNull(dataMap.get("booleanMembers")); // empty array --> null
    Assert.assertEquals(dataMap.get("byteMembers").getClass(), ByteString.class); // byte string
    Assert.assertEquals(dataMap.get("classMembers").getClass(), DataList.class);
    Assert.assertEquals(dataMap.get("doubleMembers").getClass(), DataList.class);
    Assert.assertEquals(dataMap.get("enumMembers").getClass(), DataList.class);
    Assert.assertEquals(dataMap.get("floatMembers").getClass(), DataList.class);
    Assert.assertEquals(dataMap.get("intMembers").getClass(), DataList.class);
    Assert.assertEquals(dataMap.get("longMembers").getClass(), DataList.class);
    Assert.assertEquals(dataMap.get("stringMembers").getClass(), DataList.class);
  }

  @Test(description = "Non-empty annotation, scalar members, default values: data map with annotation + no members")
  public void succeedsOnSupportedScalarMembersWithDefaultValues()
  {
    @SupportedScalarMembers
    class LocalClass {
    }

    final Annotation[] annotations = LocalClass.class.getAnnotations();
    final DataMap actual = ResourceModelAnnotation.getAnnotationsMap(annotations);

    Assert.assertNotNull(actual);
    Assert.assertTrue(actual.get(SUPPORTED_SCALAR_MEMBERS) instanceof DataMap);

    final DataMap dataMap = ((DataMap) actual.get(SUPPORTED_SCALAR_MEMBERS));

    Assert.assertEquals(dataMap.size(), 0);
  }

  @Test(description = "Non-empty annotation, scalar members, overridden values: data map with annotation + members")
  public void succeedsOnSupportedScalarMembersWithOverriddenValues()
  {
    @SupportedScalarMembers(
        annotationMember = @Key(name = "id", type = String.class),
        booleanMember = !SupportedScalarMembers.DEFAULT_BOOLEAN_MEMBER,
        byteMember = SupportedScalarMembers.DEFAULT_BYTE_MEMBER + 1,
        classMember = Test.class,
        doubleMember = SupportedScalarMembers.DEFAULT_DOUBLE_MEMBER +0.5f,
        enumMember = TestEnum.GAMMA,
        floatMember = SupportedScalarMembers.DEFAULT_FLOAT_MEMBER -0.5f,
        intMember = SupportedScalarMembers.DEFAULT_INT_MEMBER - 1,
        longMember = SupportedScalarMembers.DEFAULT_LONG_MEMBER + 1,
        stringMember = SupportedScalarMembers.DEFAULT_STRING_MEMBER + "s"
    )
    class LocalClass {
    }

    final Annotation[] annotations = LocalClass.class.getAnnotations();
    final DataMap actual = ResourceModelAnnotation.getAnnotationsMap(annotations);

    Assert.assertNotNull(actual);
    Assert.assertTrue(actual.get(SUPPORTED_SCALAR_MEMBERS) instanceof DataMap);

    final DataMap dataMap = ((DataMap) actual.get(SUPPORTED_SCALAR_MEMBERS));

    Assert.assertEquals(dataMap.size(), 10);
    Assert.assertEquals(dataMap.get("annotationMember").getClass(), DataMap.class); // from AnnotationEntry#data
    Assert.assertEquals(dataMap.get("booleanMember").getClass(), Boolean.class);
    Assert.assertEquals(dataMap.get("byteMember").getClass(), ByteString.class); // byte string
    Assert.assertEquals(dataMap.get("classMember").getClass(), String.class); // canonical class name
    Assert.assertEquals(dataMap.get("doubleMember").getClass(), Double.class);
    Assert.assertEquals(dataMap.get("enumMember").getClass(), String.class); // enum name
    Assert.assertEquals(dataMap.get("floatMember").getClass(), Float.class);
    Assert.assertEquals(dataMap.get("intMember").getClass(), Integer.class);
    Assert.assertEquals(dataMap.get("longMember").getClass(), Long.class);
    Assert.assertEquals(dataMap.get("stringMember").getClass(), String.class);
  }

  @Test(description = "Non-empty annotation, scalar members, default values, no skip: data map with annotation + members")
  public void succeedsOnSupportedScalarMembersWithDefaultValuesAndNoSkip()
  {
    @SupportedScalarMembersCustomized
    class LocalClass {
    }

    final Annotation[] annotations = LocalClass.class.getAnnotations();
    final DataMap actual = ResourceModelAnnotation.getAnnotationsMap(annotations);

    Assert.assertNotNull(actual);
    Assert.assertTrue(actual.get(SUPPORTED_SCALAR_MEMBERS_CUSTOMIZED) instanceof DataMap);

    final DataMap dataMap = ((DataMap) actual.get(SUPPORTED_SCALAR_MEMBERS_CUSTOMIZED));

    Assert.assertEquals(dataMap.size(), 9);
    Assert.assertEquals(dataMap.get("annotationMember").getClass(), DataMap.class); // from AnnotationEntry#data
    Assert.assertEquals(dataMap.get("booleanMember").getClass(), Boolean.class);
    Assert.assertEquals(dataMap.get("byteMember").getClass(), ByteString.class); // byte string
    Assert.assertEquals(dataMap.get("classMember").getClass(), String.class); // canonical class name
    Assert.assertNull(dataMap.get("doubleMember")); // <<< overridden to be excluded
    Assert.assertEquals(dataMap.get("enumMember").getClass(), String.class); // enum name
    Assert.assertEquals(dataMap.get("floatMember").getClass(), Float.class);
    Assert.assertEquals(dataMap.get("intMember").getClass(), Integer.class);
    Assert.assertEquals(dataMap.get("longMember").getClass(), Long.class);
    Assert.assertEquals(dataMap.get("overriddenStringMember").getClass(), String.class); // <<< name overridden
  }

  // ----------------------------------------------------------------------
  // negative cases
  // ----------------------------------------------------------------------

  @Test(description = "Unsafe call: null input", expectedExceptions = NullPointerException.class)
  public void failsOnNullInput()
  {
    ResourceModelAnnotation.getAnnotationsMap(null);
    Assert.fail("Should fail throwing a NullPointerException");
  }

  @Test(description = "Unsafe call: RestSpecAnnotation annotation with char array member",
      expectedExceptions = NullPointerException.class)
  public void failsOnRestSpecAnnotationCharArrayMember()
  {
    @UnsupportedCharArray
    class LocalClass {
    }

    final Annotation[] annotations = LocalClass.class.getAnnotations();
    ResourceModelAnnotation.getAnnotationsMap(annotations);

    Assert.fail("Should fail throwing a NullPointerException");
  }

  @Test(description = "Unsafe call: RestSpecAnnotation annotation with short array member",
      expectedExceptions = NullPointerException.class)
  public void failsOnRestSpecAnnotationShortArrayMember()
  {
    @UnsupportedShortArray
    class LocalClass {
    }

    final Annotation[] annotations = LocalClass.class.getAnnotations();
    ResourceModelAnnotation.getAnnotationsMap(annotations);

    Assert.fail("Should fail throwing a NullPointerException");
  }

  @Test(description = "RestSpecAnnotation annotation with unsupported members")
  public void unsupportedScalarMembersWithDefaultValues()
  {
    @UnsupportedScalarMembers
    class LocalClass {
    }

    final Annotation[] annotations = LocalClass.class.getAnnotations();
    final DataMap actual = ResourceModelAnnotation.getAnnotationsMap(annotations);

    Assert.assertNotNull(actual);
    Assert.assertTrue(actual.get(UNSUPPORTED_SCALAR_MEMBERS) instanceof DataMap);

    final DataMap dataMap = ((DataMap) actual.get(UNSUPPORTED_SCALAR_MEMBERS));
    Assert.assertEquals(dataMap.size(), 0);
  }

  @Test(description = "RestSpecAnnotation annotation with unsupported members with overrides")
  public void unsupportedScalarMembersWithOverriddenValues()
  {
    @UnsupportedScalarMembers(
        charMember = UnsupportedScalarMembers.DEFAULT_CHAR_MEMBER + 1,
        shortMember = UnsupportedScalarMembers.DEFAULT_SHORT_MEMBER + 1
    )
    class LocalClass {
    }

    final Annotation[] annotations = LocalClass.class.getAnnotations();
    final DataMap actual = ResourceModelAnnotation.getAnnotationsMap(annotations);

    Assert.assertNotNull(actual);
    Assert.assertTrue(actual.get(UNSUPPORTED_SCALAR_MEMBERS) instanceof DataMap);

    final DataMap dataMap = ((DataMap) actual.get(UNSUPPORTED_SCALAR_MEMBERS));
    Assert.assertEquals(dataMap.size(), 0);
  }

  // ----------------------------------------------------------------------
  // helper types used in the test
  // ----------------------------------------------------------------------

  private enum TestEnum {
    ALPHA,
    BETA,
    GAMMA
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  private @interface NonRestSpecAnnotation
  {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @RestSpecAnnotation(name = SUPPORTED_EMPTY)
  private @interface SupportedEmpty
  {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @RestSpecAnnotation(name = SUPPORTED_ARRAY_MEMBERS)
  private @interface SupportedArrayMembers
  {
    Key[] annotationMembers() default { @Key(name = "id1", type = Long.class), @Key(name = "id2", type = String.class) };
    boolean[] booleanMembers() default { false, true };
    byte[] byteMembers() default { 7, 8 };
    Class<?>[] classMembers() default { Object.class, Test.class };
    double[] doubleMembers() default { 1.4d, -1.3d };
    float[] floatMembers() default { -0.34f, 0.35f };
    int[] intMembers() default { 555321, -123555 };
    long[] longMembers() default { -999123, 321999 };
    String[] stringMembers() default { "string", "gnirts" };
    TestEnum[] enumMembers() default { TestEnum.ALPHA, TestEnum.BETA };
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @RestSpecAnnotation(name = SUPPORTED_SCALAR_MEMBERS)
  private @interface SupportedScalarMembers
  {
    Key annotationMember() default @Key(name = "id", type = Long.class);
    boolean booleanMember() default DEFAULT_BOOLEAN_MEMBER;
    byte byteMember() default DEFAULT_BYTE_MEMBER;
    Class<?> classMember() default Object.class;
    double doubleMember() default DEFAULT_DOUBLE_MEMBER;
    float floatMember() default DEFAULT_FLOAT_MEMBER;
    int intMember() default DEFAULT_INT_MEMBER;
    long longMember() default DEFAULT_LONG_MEMBER;
    String stringMember() default DEFAULT_STRING_MEMBER;
    TestEnum enumMember() default TestEnum.ALPHA;

    boolean DEFAULT_BOOLEAN_MEMBER = false;
    byte DEFAULT_BYTE_MEMBER = 7;
    double DEFAULT_DOUBLE_MEMBER = 1.4d;
    float DEFAULT_FLOAT_MEMBER = -0.34f;
    int DEFAULT_INT_MEMBER = 555321;
    long DEFAULT_LONG_MEMBER = -999123;
    String DEFAULT_STRING_MEMBER = "string";
  }

  /**
   * Same as {@link SupportedScalarMembers} but customizes
   * globally so that default values are not skipped. Also
   * customizes locally two annotations-- one is excluded
   * entirely (doubleMember) and one has its name overridden.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @RestSpecAnnotation(name = SUPPORTED_SCALAR_MEMBERS_CUSTOMIZED, skipDefault = false)
  private @interface SupportedScalarMembersCustomized
  {
    Key annotationMember() default @Key(name = "id", type = Long.class);
    boolean booleanMember() default DEFAULT_BOOLEAN_MEMBER;
    byte byteMember() default DEFAULT_BYTE_MEMBER;
    Class<?> classMember() default Object.class;
    // overrides the exclude properties of this annotation
    // expected behavior: excluded from all processing (skipDefault becomes irrelevant)
    @RestSpecAnnotation(exclude = true)
    double doubleMember() default DEFAULT_DOUBLE_MEMBER;
    float floatMember() default DEFAULT_FLOAT_MEMBER;
    int intMember() default DEFAULT_INT_MEMBER;
    long longMember() default DEFAULT_LONG_MEMBER;
    // overrides the name and skipDefault of this annotation
    // must set skipDefault=false explicitly to get the desired behavior (default not skipped)
    @RestSpecAnnotation(name = "overriddenStringMember", skipDefault = false)
    String stringMember() default DEFAULT_STRING_MEMBER;
    TestEnum enumMember() default TestEnum.ALPHA;

    boolean DEFAULT_BOOLEAN_MEMBER = false;
    byte DEFAULT_BYTE_MEMBER = 7;
    double DEFAULT_DOUBLE_MEMBER = 1.4d;
    float DEFAULT_FLOAT_MEMBER = -0.34f;
    int DEFAULT_INT_MEMBER = 555321;
    long DEFAULT_LONG_MEMBER = -999123;
    String DEFAULT_STRING_MEMBER = "string";
  }

  /**
   * Values of type char are always mapped to null (effectively
   * unsupported). So when adding these to the array, we get an
   * NPE.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @RestSpecAnnotation(name = "UnsupportedCharArray")
  private @interface UnsupportedCharArray
  {
    char[] charMembers() default { 'c', 'd' };
  }

  /**
   * Values of type char and short are always mapped to null,
   * whether using a default value or an overridden one.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @RestSpecAnnotation(name = UNSUPPORTED_SCALAR_MEMBERS)
  private @interface UnsupportedScalarMembers
  {
    char charMember() default DEFAULT_CHAR_MEMBER;
    short shortMember() default DEFAULT_SHORT_MEMBER;

    char DEFAULT_CHAR_MEMBER = 'c';
    short DEFAULT_SHORT_MEMBER = 91;
  }

  /**
   * Values of type short are always mapped to null (effectively
   * unsupported). So when adding these to the array, we get an
   * NPE.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @RestSpecAnnotation(name = "UnsupportedShortArray")
  private @interface UnsupportedShortArray
  {
    short[] shortMembers() default { 91, -19 };
  }
}