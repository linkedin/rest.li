/*
   Copyright (c) 2022 LinkedIn Corp.

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

package com.linkedin.restli.server;

import java.util.Arrays;
import java.util.Collections;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.restli.server.TestConstants.FOO_1;
import static com.linkedin.restli.server.TestConstants.FOO_2;
import static com.linkedin.restli.server.TestConstants.MD_1;
import static com.linkedin.restli.server.TestConstants.MD_2;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;


public class TestCollectionResult
{

  private static final CollectionResult<TestRecordTemplateClass.Foo, TestRecordTemplateClass.Bar> COLLECTION_RESULT_1 =
      new CollectionResult<>
          (
              Collections.unmodifiableList(Arrays.asList(FOO_1)), 1, MD_1, CollectionResult.PageIncrement.RELATIVE
          );
  private static final CollectionResult<TestRecordTemplateClass.Foo, TestRecordTemplateClass.Bar> COLLECTION_RESULT_2 =
      new CollectionResult<>
          (
              Collections.unmodifiableList(Arrays.asList(FOO_1)), 1, MD_1, CollectionResult.PageIncrement.RELATIVE
          );
  private static final String NON_COLLECTION_RESULT = "test";
  private static final CollectionResult<TestRecordTemplateClass.Foo, TestRecordTemplateClass.Bar> COLLECTION_RESULT_3 =
      new CollectionResult<>
          (
              Collections.unmodifiableList(Arrays.asList(FOO_2)), 1, MD_1, CollectionResult.PageIncrement.RELATIVE
          );
  private static final CollectionResult<TestRecordTemplateClass.Foo, TestRecordTemplateClass.Bar> COLLECTION_RESULT_4 =
      new CollectionResult<>
          (
              Collections.unmodifiableList(Arrays.asList(FOO_1)), 2, MD_1, CollectionResult.PageIncrement.RELATIVE
          );
  private static final CollectionResult<TestRecordTemplateClass.Foo, TestRecordTemplateClass.Bar> COLLECTION_RESULT_5 =
      new CollectionResult<>
          (
              Collections.unmodifiableList(Arrays.asList(FOO_1)), 1, MD_2, CollectionResult.PageIncrement.RELATIVE
          );
  private static final CollectionResult<TestRecordTemplateClass.Foo, TestRecordTemplateClass.Bar> COLLECTION_RESULT_6 =
      new CollectionResult<>
          (
              Collections.unmodifiableList(Arrays.asList(FOO_1)), 1, MD_1, CollectionResult.PageIncrement.FIXED
          );
  private static final CollectionResult<TestRecordTemplateClass.Foo, TestRecordTemplateClass.Bar> COLLECTION_RESULT_7 =
      new CollectionResult<>
          (
              Collections.unmodifiableList(Arrays.asList(FOO_1)), 1, MD_1, CollectionResult.PageIncrement.RELATIVE
          );

  @DataProvider(name = "testEqualsDataProvider")
  public Object[][] testEqualsDataProvider()
  {
    return new Object[][]
        {
            // 0. Basic test case when 2 CollectionResults are equal
            { true, COLLECTION_RESULT_1, COLLECTION_RESULT_2 },
            // 1. Test case to make sure equals is reflective
            { true, COLLECTION_RESULT_1, COLLECTION_RESULT_1 },
            // 2. Test case to make sure equals is symmetric
            { true, COLLECTION_RESULT_2, COLLECTION_RESULT_1 },
            // 3. Test case to make sure equals is transitive, done together with test case 0 and 4
            { true, COLLECTION_RESULT_2, COLLECTION_RESULT_7 },
            // 4. Test case to make sure equals is transitive, done together with test case 0 and 3
            { true, COLLECTION_RESULT_1, COLLECTION_RESULT_7 },
            // 5. Test case when target object is null
            { false, COLLECTION_RESULT_1, null },
            // 6. Test case when target object is not CollectionResult class
            { false, COLLECTION_RESULT_1, NON_COLLECTION_RESULT },
            // 7. Test case when the elements list is different
            { false, COLLECTION_RESULT_1, COLLECTION_RESULT_3 },
            // 8. Test case when the total is different
            { false, COLLECTION_RESULT_1, COLLECTION_RESULT_4 },
            // 9. Test case when the metadata is different
            { false, COLLECTION_RESULT_1, COLLECTION_RESULT_5 },
            // 10. Test case when the pageIncrement is different
            { false, COLLECTION_RESULT_1, COLLECTION_RESULT_6 }
        };
  }

  @Test(dataProvider = "testEqualsDataProvider")
  public void testEquals
      (
          boolean shouldEquals,
          @Nonnull CollectionResult<TestRecordTemplateClass.Foo, TestRecordTemplateClass.Bar> collectionResult,
          @Nullable Object compareObject
      )
  {
    assertEquals(collectionResult.equals(compareObject), shouldEquals);
  }

  @DataProvider(name = "testHashCodeDataProvider")
  public Object[][] testHashCodeDataProvider()
  {
    return new Object[][]{
        // 0. Basic test case when 2 CollectionResults have same hashcode
        { true, COLLECTION_RESULT_1, COLLECTION_RESULT_2 },
        // 1. Test case when the elements list is different
        { false, COLLECTION_RESULT_1, COLLECTION_RESULT_3 },
        // 2. Test case when the total is different
        { false, COLLECTION_RESULT_1, COLLECTION_RESULT_4 },
        // 3. Test case when the metadata is different
        { false, COLLECTION_RESULT_1, COLLECTION_RESULT_5 },
        // 4. Test case when the pageIncrement is different
        { false, COLLECTION_RESULT_1, COLLECTION_RESULT_6 }
    };
  }

  @Test(dataProvider = "testHashCodeDataProvider")
  public void testHashCode
      (
          boolean hasSameHashCode,
          @Nonnull CollectionResult<TestRecordTemplateClass.Foo, TestRecordTemplateClass.Bar> collectionResult1,
          @Nonnull CollectionResult<TestRecordTemplateClass.Foo, TestRecordTemplateClass.Bar> collectionResult2
      )
  {
    if (hasSameHashCode)
    {
      assertEquals(collectionResult1.hashCode(), collectionResult2.hashCode());
    }
    else
    {
      assertNotEquals(collectionResult1.hashCode(), collectionResult2.hashCode());
    }
  }
}