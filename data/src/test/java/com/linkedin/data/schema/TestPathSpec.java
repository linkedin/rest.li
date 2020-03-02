package com.linkedin.data.schema;

import java.util.Collections;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.collections.Lists;


public class TestPathSpec {
  @DataProvider(name = "pathSegmentsForListSegmentsConstructor")
  private Object[][] pathSegmentsForListSegmentsConstructor() {
    return new Object[][] {
        {
          Collections.emptyList()
        },
        {
          Lists.newArrayList("a")
        },
        {
          Lists.newArrayList("a", "b")
        }
    };
  }

  @Test(dataProvider = "pathSegmentsForListSegmentsConstructor")
  public void testConstructor_withListOfPathSegments_shouldProducePathSpecWithExpectedComponents(
      List<String> pathSegments) {
    PathSpec constructedPathSpec = new PathSpec(pathSegments);

    Assert.assertEquals(constructedPathSpec.getPathComponents(), pathSegments);
  }
}
