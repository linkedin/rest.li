package com.linkedin.restli.server;

import com.linkedin.restli.common.HttpStatus;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;


public class TestUpdateResponse
{

  private static final UpdateResponse UPDATE_RESPONSE_1 = new UpdateResponse(HttpStatus.S_200_OK);
  private static final UpdateResponse UPDATE_RESPONSE_2 = new UpdateResponse(HttpStatus.S_200_OK);
  private static final String NON_UPDATE_RESPONSE = "test";
  private static final UpdateResponse UPDATE_RESPONSE_3 = new UpdateResponse(HttpStatus.S_201_CREATED);
  private static final UpdateResponse UPDATE_RESPONSE_4 = new UpdateResponse(HttpStatus.S_200_OK);

  @DataProvider(name = "testEqualsDataProvider")
  public Object[][] testEqualsDataProvider()
  {
    return new Object[][]
        {
            // 0. Basic test case when 2 UpdateResponses are equal
            { true, UPDATE_RESPONSE_1, UPDATE_RESPONSE_2 },
            // 1. Test case to make sure equals is reflective
            { true, UPDATE_RESPONSE_1, UPDATE_RESPONSE_1 },
            // 2. Test case to make sure equals is symmetric
            { true, UPDATE_RESPONSE_2, UPDATE_RESPONSE_1 },
            // 3. Test case to make sure equals is transitive, done together with test case 0 and 4
            { true, UPDATE_RESPONSE_2, UPDATE_RESPONSE_4 },
            // 4. Test case to make sure equals is transitive, done together with test case 0 and 3
            { true, UPDATE_RESPONSE_1, UPDATE_RESPONSE_4 },
            // 5. Test case when target object is null
            { false, UPDATE_RESPONSE_1, null },
            // 6. Test case when target object is not UpdateResponse class
            { false, UPDATE_RESPONSE_1, NON_UPDATE_RESPONSE },
            // 7. Test case when the httpStatus is different
            { false, UPDATE_RESPONSE_1, UPDATE_RESPONSE_3 }
        };
  }

  @Test(dataProvider = "testEqualsDataProvider")
  public void testEquals
      (
          boolean shouldEquals,
          @Nonnull UpdateResponse updateResponse,
          @Nullable Object compareObject
      )
  {
    assertEquals(updateResponse.equals(compareObject), shouldEquals);
  }

  @DataProvider(name = "testHashCodeDataProvider")
  public Object[][] testHashCodeDataProvider()
  {
    return new Object[][]{
        // 0. Basic test case when 2 UpdateResponses have same hashcode
        { true, UPDATE_RESPONSE_1, UPDATE_RESPONSE_2 },
        // 1. Test case when the httpStatus is different
        { false, UPDATE_RESPONSE_1, UPDATE_RESPONSE_3 }
    };
  }

  @Test(dataProvider = "testHashCodeDataProvider")
  public void testHashCode
      (
          boolean hasSameHashCode,
          @Nonnull UpdateResponse updateResponse1,
          @Nonnull UpdateResponse updateResponse2
      )
  {
    if (hasSameHashCode)
    {
      assertEquals(updateResponse1.hashCode(), updateResponse2.hashCode());
    }
    else
    {
      assertNotEquals(updateResponse1.hashCode(), updateResponse2.hashCode());
    }
  }
}
