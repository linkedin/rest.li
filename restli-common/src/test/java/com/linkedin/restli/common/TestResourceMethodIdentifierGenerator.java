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

package com.linkedin.restli.common;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestResourceMethodIdentifierGenerator {
  @Test(dataProvider = "testData")
  public void testResourceMethodIdentifierGenerator(String baseUriTemplate, ResourceMethod method, String methodName,
      String expected) {
    final String resourceMethodIdentifier = ResourceMethodIdentifierGenerator.generate(baseUriTemplate, method, methodName);
    final String keylessRMI = ResourceMethodIdentifierGenerator.stripPathKeys(resourceMethodIdentifier);
    final String keylessBaseUriTemplate = ResourceMethodIdentifierGenerator.stripPathKeys(baseUriTemplate);

    Assert.assertEquals(resourceMethodIdentifier, expected, "ResourceMethodIdentifier is incorrect");
    Assert.assertFalse(keylessRMI.contains("{}"), "keylessRMI should not contain key pattern: " + keylessRMI);
    Assert.assertEquals(keylessRMI, resourceMethodIdentifier.replaceAll("/?\\{}", ""),
        "keylessRMI is incorrect for " + resourceMethodIdentifier);
    if (baseUriTemplate != null) {
      Assert.assertEquals(keylessBaseUriTemplate, baseUriTemplate.replaceAll("/?\\{[^}]*}", ""),
          "Keyless baseUriTemplate is incorrect for " + baseUriTemplate);
    } else {
      Assert.assertNull(keylessBaseUriTemplate);
    }
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
        new Object[]{"album/{albumId}", ResourceMethod.GET, null, "album/{}:get"},
        new Object[]{"album/{albumId}/photos/{photoId}", ResourceMethod.GET, null, "album/{}/photos/{}:get"},
        new Object[]{"album/{x, y}/photos/{x}", ResourceMethod.GET_ALL, null, "album/{}/photos/{}:get_all"},
        new Object[]{"a/{x}{y}{z}/b/{x}/c", ResourceMethod.UPDATE, null, "a/{}{}{}/b/{}/c:update"},
        new Object[]{"album{id}/photo{id}", ResourceMethod.DELETE, "garbage", "album{}/photo{}:delete"},
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
