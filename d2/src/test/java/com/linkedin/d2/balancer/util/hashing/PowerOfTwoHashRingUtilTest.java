/*
   Copyright (c) 2021 LinkedIn Corp.

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
package com.linkedin.d2.balancer.util.hashing;

import org.testng.Assert;
import org.testng.annotations.Test;


public class PowerOfTwoHashRingUtilTest {

  @Test
  public void testPickLessLoadedHost()
  {
    int hostOnePicked = 0;
    int hostTwoPicked = 0;

    for (int i = 0; i < 100; i ++)
    {
      // Having 2 host, host 1 load is 60 and host 2 load is 40.
      if (PowerOfTwoHashRingUtil.shouldPickFirstHost(60, 40))
      {
        hostOnePicked ++;
      } else
      {
        hostTwoPicked ++;
      }
    }
    Assert.assertTrue(hostOnePicked < hostTwoPicked);
  }

  @Test
  public void testServerReportedLoadNotAvailable()
  {
    int hostOnePicked = 0;
    int hostTwoPicked = 0;

    for (int i = 0; i < 1000; i ++)
    {
      // None of the hosts returned server reported load
      if (PowerOfTwoHashRingUtil.shouldPickFirstHost(-1, -1))
      {
        hostOnePicked ++;
      } else
      {
        hostTwoPicked ++;
      }
    }
    Assert.assertTrue(hostOnePicked < 600 && hostTwoPicked < 600);
  }
}
