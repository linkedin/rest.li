package com.linkedin.restli.common;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestResourceMethodIdentifierGenerator {
  @Test(dataProvider = "testData")
  public void testResourceMethodIdentifierGenerator(String baseUriTemplate, ResourceMethod method, String methodName,
      String expected) {
    Assert.assertEquals(ResourceMethodIdentifierGenerator.generate(baseUriTemplate, method, methodName),
        expected, "ResourceMethodIdentifier is incorrect");
  }

  @DataProvider
  public Object[][] testData()
  {
    return new Object[][] {
        new Object[]{"photos", ResourceMethod.GET, null, "photos:get"},
        new Object[]{"photos", ResourceMethod.ACTION, "testAction", "photos:action:testAction"},
        new Object[]{"photos", ResourceMethod.FINDER, "testFinder", "photos:finder:testFinder"},
        new Object[]{"photos", ResourceMethod.BATCH_FINDER, "testFinder", "photos:batch_finder:testFinder"},
        new Object[]{"album/photos", ResourceMethod.GET, null, "album/photos:get"},
        new Object[]{"album/photos/date", ResourceMethod.GET, null, "album/photos/date:get"},
        new Object[]{"album/{albumId}", ResourceMethod.GET, null, "album:get"},
        new Object[]{"album/{albumId}/photos/{photoId}", ResourceMethod.GET, null, "album/photos:get"},
        new Object[]{"album/{x, y}/photos/{x}", ResourceMethod.GET_ALL, null, "album/photos:get_all"},
        new Object[]{"a/{x}{y}{z}/b/{x}/c", ResourceMethod.UPDATE, null, "a/b/c:update"},
        new Object[]{"album{id}/photo{id}", ResourceMethod.DELETE, "garbage", "album/photo:delete"},
        new Object[]{"/garbage/in/garbage/out/", ResourceMethod.GET, null, "/garbage/in/garbage/out/:get"},
        new Object[]{"garbage/{in/garbage/{out", ResourceMethod.PARTIAL_UPDATE, null, "garbage/{in/garbage/{out:partial_update"},
        new Object[]{"garbage/in}/garbage/out}", ResourceMethod.PARTIAL_UPDATE, null, "garbage/in}/garbage/out}:partial_update"},
        new Object[]{"", ResourceMethod.GET, null, ":get"},
        new Object[]{null, ResourceMethod.GET_ALL, null, ":get_all"},
        new Object[]{null, ResourceMethod.ACTION, "fubar", ":action:fubar"},
        new Object[]{"error", ResourceMethod.ACTION, null, "error:action:null"}
    };
  }

}
