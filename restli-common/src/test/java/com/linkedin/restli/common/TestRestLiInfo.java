package com.linkedin.restli.common;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author bsoetarm
 */
public class TestRestLiInfo
{
  @DataProvider
  public Object[][] requestBatchSize()
  {
    return new Object[][] { {0}, {1}, {10}, {-1}, {Integer.MAX_VALUE}, {Integer.MIN_VALUE} };
  }

  @DataProvider
  public Object[][] restLiInfo()
  {
    final RestLiInfo info1 = new MutableRestLiInfoImpl();
    final RestLiInfo info2 = new MutableRestLiInfoImpl();
    final RestLiInfo info3 = new MutableRestLiInfoImpl();
    ((MutableRestLiInfoImpl) info3).setRequestBatchSize(1);
    final String notInfo = "";

    return new Object[][] {
        { info1, info1, true },
        { info1, null, false },
        { info1, notInfo, false },
        { info1, info2, true },
        { info1, info3, false }
    };
  }

  @Test
  public void testDefaultValue()
  {
    final RestLiInfo info = new MutableRestLiInfoImpl();
    Assert.assertEquals(0, info.getRequestBatchSize());
  }

  @Test(dataProvider = "requestBatchSize")
  public void testRequestBatchSize(final int requestBatchSize)
  {
    final RestLiInfo info = new MutableRestLiInfoImpl();
    ((MutableRestLiInfoImpl) info).setRequestBatchSize(requestBatchSize);
    Assert.assertEquals(requestBatchSize, info.getRequestBatchSize());
  }

  @Test(dataProvider = "restLiInfo")
  public void testRestLiInfoEquality(final RestLiInfo info1, final Object o, final boolean expectedIsEqual)
  {
    final boolean isEqual = info1.equals(o);
    Assert.assertEquals(expectedIsEqual, isEqual);
  }
}
