/*
   Copyright (c) 2016 LinkedIn Corp.

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

import com.linkedin.restli.common.HttpStatus;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.restli.server.TestConstants.FOO_1;
import static com.linkedin.restli.server.TestConstants.FOO_2;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;


public class TestUpdateEntityResponse
{

  private static final UpdateEntityResponse<TestRecordTemplateClass.Foo> UPDATE_ENTITY_RESPONSE_1 =
      new UpdateEntityResponse<>(HttpStatus.S_200_OK, FOO_1);
  private static final UpdateEntityResponse<TestRecordTemplateClass.Foo> UPDATE_ENTITY_RESPONSE_2 =
      new UpdateEntityResponse<>(HttpStatus.S_200_OK, FOO_1);
  private static final String NON_UPDATE_ENTITY_RESPONSE = "test";
  private static final UpdateEntityResponse<TestRecordTemplateClass.Foo> UPDATE_ENTITY_RESPONSE_3 =
      new UpdateEntityResponse<>(HttpStatus.S_201_CREATED, FOO_1);
  private static final UpdateEntityResponse<TestRecordTemplateClass.Foo> UPDATE_ENTITY_RESPONSE_4 =
      new UpdateEntityResponse<>(HttpStatus.S_200_OK, FOO_2);
  private static final UpdateEntityResponse<TestRecordTemplateClass.Foo> UPDATE_ENTITY_RESPONSE_5 =
      new UpdateEntityResponse<>(HttpStatus.S_200_OK, FOO_1);

  @DataProvider(name = "testEqualsDataProvider")
  public Object[][] testEqualsDataProvider()
  {
    return new Object[][]
        {
            // 0. Basic test case when 2 UpdateEntityResponses are equal
            { true, UPDATE_ENTITY_RESPONSE_1, UPDATE_ENTITY_RESPONSE_2 },
            // 1. Test case to make sure equals is reflective
            { true, UPDATE_ENTITY_RESPONSE_1, UPDATE_ENTITY_RESPONSE_1 },
            // 2. Test case to make sure equals is symmetric
            { true, UPDATE_ENTITY_RESPONSE_2, UPDATE_ENTITY_RESPONSE_1 },
            // 3. Test case to make sure equals is transitive, done together with test case 0 and 4
            { true, UPDATE_ENTITY_RESPONSE_2, UPDATE_ENTITY_RESPONSE_5 },
            // 4. Test case to make sure equals is transitive, done together with test case 0 and 3
            { true, UPDATE_ENTITY_RESPONSE_1, UPDATE_ENTITY_RESPONSE_5 },
            // 5. Test case when target object is null
            { false, UPDATE_ENTITY_RESPONSE_1, null },
            // 6. Test case when target object is not UpdateEntityResponse class
            { false, UPDATE_ENTITY_RESPONSE_1, NON_UPDATE_ENTITY_RESPONSE },
            // 7. Test case when the httpStatus is different
            { false, UPDATE_ENTITY_RESPONSE_1, UPDATE_ENTITY_RESPONSE_3 },
            // 8. Test case when the entity is different
            { false, UPDATE_ENTITY_RESPONSE_1, UPDATE_ENTITY_RESPONSE_4 }
        };
  }

  @Test(dataProvider = "testEqualsDataProvider")
  public void testEquals
      (
          boolean shouldEquals,
          @Nonnull UpdateEntityResponse<TestRecordTemplateClass.Foo> updateEntityResponse,
          @Nullable Object compareObject
      )
  {
    assertEquals(updateEntityResponse.equals(compareObject), shouldEquals);
  }

  @DataProvider(name = "testHashCodeDataProvider")
  public Object[][] testHashCodeDataProvider()
  {
    return new Object[][]{
        // 0. Basic test case when 2 UpdateEntityResponses have same hashcode
        { true, UPDATE_ENTITY_RESPONSE_1, UPDATE_ENTITY_RESPONSE_2 },
        // 1. Test case when the httpStatus is different
        { false, UPDATE_ENTITY_RESPONSE_1, UPDATE_ENTITY_RESPONSE_3 },
        // 2. Test case when the entity is different
        { false, UPDATE_ENTITY_RESPONSE_1, UPDATE_ENTITY_RESPONSE_4 }
    };
  }

  @Test(dataProvider = "testHashCodeDataProvider")
  public void testHashCode
      (
          boolean hasSameHashCode,
          @Nonnull UpdateEntityResponse<TestRecordTemplateClass.Foo> updateEntityResponse1,
          @Nonnull UpdateEntityResponse<TestRecordTemplateClass.Foo> updateEntityResponse2
      )
  {
    if (hasSameHashCode)
    {
      assertEquals(updateEntityResponse1.hashCode(), updateEntityResponse2.hashCode());
    }
    else
    {
      assertNotEquals(updateEntityResponse1.hashCode(), updateEntityResponse2.hashCode());
    }
  }
}
