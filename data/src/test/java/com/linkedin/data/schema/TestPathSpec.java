package com.linkedin.data.schema;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.lang.Boolean.*;


public class TestPathSpec
{

  @Test(dataProvider = "pathSpecsWithEmptyFlag")
  public void testIsEmptyPath(PathSpec testPathSpec, boolean expectedResponse)
  {
    Assert.assertEquals(testPathSpec.isEmptyPath(), expectedResponse);
  }

  @DataProvider
  public static Object[][] pathSpecsWithEmptyFlag()
  {
    PathSpec emptyPathSpecWithAttributes = new PathSpec();
    emptyPathSpecWithAttributes.setAttribute(PathSpec.ATTR_ARRAY_START, 0);
    emptyPathSpecWithAttributes.setAttribute(PathSpec.ATTR_ARRAY_COUNT, 5);

    PathSpec pathSpecWithAttributes = new PathSpec("field1", "field2");
    pathSpecWithAttributes.setAttribute(PathSpec.ATTR_ARRAY_START, 0);
    pathSpecWithAttributes.setAttribute(PathSpec.ATTR_ARRAY_COUNT, 5);

    return new Object[][]{
        {
            new PathSpec(), TRUE
        },
        {
            emptyPathSpecWithAttributes, TRUE
        },
        {
            new PathSpec("field"), FALSE
        },
        {
            new PathSpec("field1", "field2"), FALSE
        },
        {
            pathSpecWithAttributes, FALSE
        },

    };
  }

  @Test(dataProvider = "pathSpecsWithParent")
  public void testGetParent(PathSpec testPathSpec, PathSpec expectedParent)
  {
    Assert.assertEquals(testPathSpec.getParent(), expectedParent);
  }

  @DataProvider
  public static Object[][] pathSpecsWithParent()
  {
    PathSpec pathSpecWithAttributes = new PathSpec("field1", "field2");
    pathSpecWithAttributes.setAttribute(PathSpec.ATTR_ARRAY_START, 0);
    pathSpecWithAttributes.setAttribute(PathSpec.ATTR_ARRAY_COUNT, 5);

    return new Object[][]{
        {
          new PathSpec(), new PathSpec()
        },
        {
          PathSpec.emptyPath(), new PathSpec()
        },
        {
          new PathSpec("field"), new PathSpec()
        },
        {
            new PathSpec("field1", "field2"), new PathSpec("field1")
        },
        {
            new PathSpec("field1", "field2", "field3"), new PathSpec("field1", "field2")
        },
        {
            pathSpecWithAttributes, new PathSpec("field1")
        },

    };
  }
}