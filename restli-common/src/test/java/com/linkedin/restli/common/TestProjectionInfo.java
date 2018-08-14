package com.linkedin.restli.common;

import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Min Chen
 */
public class TestProjectionInfo
{
  @Test
  public void testMutation()
  {
    ProjectionInfo projection = new ProjectionInfoImpl(false);
    Assert.assertFalse(projection.isProjectionPresent());
    Assert.assertTrue(projection instanceof MutableProjectionInfo);
    ((MutableProjectionInfo)projection).setProjectionPresent(true);
    Assert.assertTrue(projection.isProjectionPresent());
  }
}
