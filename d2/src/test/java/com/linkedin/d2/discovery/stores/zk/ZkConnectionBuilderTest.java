/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.d2.discovery.stores.zk;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.testng.Assert;
import org.testng.annotations.Test;


public class ZkConnectionBuilderTest {
  @Test
  public void testBuilderEquality() {
    ZKConnectionBuilder builder1 = new ZKConnectionBuilder("localhost:2121");
    ZKConnectionBuilder builder2 = new ZKConnectionBuilder("localhost:2121");
    ZKConnectionBuilder builder3 = new ZKConnectionBuilder("localhost:2121");
    ZKConnectionBuilder builder4 = new ZKConnectionBuilder("localhost:2121");

    builder1.setInitInterval(20);
    builder1.setRetryLimit(10);
    builder1.setTimeout(100);
    builder1.setExponentialBackoff(true);
    builder1.setIsSymlinkAware(true);
    builder1.setShutdownAsynchronously(true);

    builder2.setInitInterval(20);
    builder2.setRetryLimit(10);
    builder2.setTimeout(100);
    builder2.setExponentialBackoff(true);
    builder2.setIsSymlinkAware(true);
    builder2.setShutdownAsynchronously(true);

    builder3.setInitInterval(20);
    builder3.setRetryLimit(10);
    builder3.setTimeout(100);
    builder3.setExponentialBackoff(true);
    builder3.setIsSymlinkAware(false);
    builder3.setShutdownAsynchronously(true);

    builder4.setInitInterval(20);
    builder4.setRetryLimit(10);
    builder4.setTimeout(100);
    builder4.setExponentialBackoff(false);
    builder4.setIsSymlinkAware(true);
    builder4.setShutdownAsynchronously(true);

    Set<ZKConnectionBuilder> set = new HashSet<>();
    set.add(builder1);
    Assert.assertTrue(set.contains(builder2));
    Assert.assertTrue(!set.contains(builder3));
    Assert.assertTrue(!set.contains(builder4));
  }
}
