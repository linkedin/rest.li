/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.darkcluster;

import com.linkedin.darkcluster.api.DarkClusterDispatcher;
import com.linkedin.darkcluster.impl.BaseDarkClusterDispatcherImpl;
import com.linkedin.darkcluster.impl.DefaultDarkClusterDispatcherImpl;
import com.linkedin.r2.message.RequestContext;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestBaseDarkClusterDispatcher
{
  private static final String DARK_CLUSTER_NAME = "fooCluster-dark";


  @DataProvider
  public Object[][] provideKeys()
  {
    return new Object[][] {
      // numDuplicates, failRequests, requestSent, expectedSuccessCount, expectedRequestCount, expectedExceptionCount
      {0, false, false, 0, 0, 0},
      {1, false, true, 1, 1, 0},
      {2, false, true, 2, 2, 0},
      {3, false, true, 3, 3, 0},
      {4, false, true, 4, 4, 0},
      {5, false, true, 5, 5, 0},
      // now throw exceptions from the MockClient
      {0, true, false, 0, 0, 0},
      {1, true, true, 0, 1, 1},
      {2, true, true, 0, 2, 2}
    };
  }
  @Test(dataProvider = "provideKeys")
  public void testBaseDispatcher(int numDuplicates, boolean failRequests, boolean requestSent, int expectedSuccessCount,
                            int expectedRequestCount, int expectedExceptionCount)
  {
    DarkClusterDispatcher darkClusterDispatcher = new DefaultDarkClusterDispatcherImpl(new MockClient(failRequests));
    BaseDarkClusterDispatcherImpl baseDispatcher = new BaseDarkClusterDispatcherImpl(DARK_CLUSTER_NAME,
                                                                                 darkClusterDispatcher,
                                                                                 new DoNothingNotifier(),
                                                                                 new CountingVerifierManager());
    boolean result = baseDispatcher.sendRequest(new TestRestRequest(), new TestRestRequest(), new RequestContext(), numDuplicates);
    Assert.assertEquals(result, requestSent, "expected: " + requestSent);
    Assert.assertEquals(baseDispatcher.getSuccessCount(), expectedSuccessCount, "unexpected successCount");
    Assert.assertEquals(baseDispatcher.getRequestCount(), expectedRequestCount, "unexpected requestCount");
    Assert.assertEquals(baseDispatcher.getExceptionCount(), expectedExceptionCount, "unexpected exceptionCount");
  }
}
