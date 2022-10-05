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


public class TestGetResult
{

  private static final GetResult<TestRecordTemplateClass.Foo> REQUEST_1 = new GetResult<>(FOO_1, HttpStatus.S_200_OK);
  private static final GetResult<TestRecordTemplateClass.Foo> REQUEST_2 = new GetResult<>(FOO_1, HttpStatus.S_200_OK);
  private static final GetResult<TestRecordTemplateClass.Foo> REQUEST_3 = new GetResult<>(FOO_2, HttpStatus.S_200_OK);
  private static final GetResult<TestRecordTemplateClass.Foo> REQUEST_4 =
      new GetResult<>(FOO_1, HttpStatus.S_500_INTERNAL_SERVER_ERROR);
  private static final GetResult<TestRecordTemplateClass.Foo> REQUEST_5 = new GetResult<>(FOO_1, HttpStatus.S_200_OK);
  private static final String NON_GET_RESULT = "test";

  @DataProvider(name = "testEqualsDataProvider")
  public Object[][] testEqualsDataProvider()
  {
    return new Object[][]
        {
            // 0. Basic test case when 2 GetResults are equal
            { true, REQUEST_1, REQUEST_2 },
            // 1. Test case to make sure equals is reflective
            { true, REQUEST_1, REQUEST_1 },
            // 2. Test case to make sure equals is symmetric
            { true, REQUEST_2, REQUEST_1 },
            // 3. Test case to make sure equals is transitive, done together with test case 0 and 4
            { true, REQUEST_2, REQUEST_5 },
            // 4. Test case to make sure equals is transitive, done together with test case 0 and 3
            { true, REQUEST_1, REQUEST_5 },
            // 5. Test case when target object is null
            { false, REQUEST_1, null },
            // 6. Test case when target object is not GetResult class
            { false, REQUEST_1, NON_GET_RESULT },
            // 7. Test case when the value is different
            { false, REQUEST_1, REQUEST_3 },
            // 8. Test case when the status is different
            { false, REQUEST_1, REQUEST_4 }
        };
  }

  @Test(dataProvider = "testEqualsDataProvider")
  public void testEquals
      (
          boolean shouldEquals,
          @Nonnull GetResult<TestRecordTemplateClass.Foo> request,
          @Nullable Object compareObject
      )
  {
    assertEquals(request.equals(compareObject), shouldEquals);
  }

  @DataProvider(name = "testHashCodeDataProvider")
  public Object[][] testHashCodeDataProvider()
  {
    return new Object[][]{
        // 0. Basic test case when 2 GetResult have same hashcode
        { true, REQUEST_1, REQUEST_2 },
        // 1. Test case to make sure hashcode is reflective
        { true, REQUEST_1, REQUEST_1 },
        // 2. Test case when the data list is different
        { false, REQUEST_1, REQUEST_3 },
        // 3. Test case when the data list is different
        { false, REQUEST_1, REQUEST_4 }
    };
  }

  @Test(dataProvider = "testHashCodeDataProvider")
  public void testHashCode
      (
          boolean hasSameHashCode,
          @Nonnull GetResult<TestRecordTemplateClass.Foo> request1,
          @Nonnull GetResult<TestRecordTemplateClass.Foo> request2
      )
  {
    if (hasSameHashCode)
    {
      assertEquals(request1.hashCode(), request2.hashCode());
    }
    else
    {
      assertNotEquals(request1.hashCode(), request2.hashCode());
    }
  }
}
