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
package com.linkedin.restli.examples;

import com.linkedin.data.DataMap;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.GetRequest;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.examples.defaults.api.FillInDefaultsGetRequestBuilder;
import com.linkedin.restli.examples.defaults.api.FillInDefaultsRequestBuilders;
import com.linkedin.restli.examples.defaults.api.HighLevelRecordWithDefault;
import java.io.IOException;
import java.util.Collections;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 *
 * @author Brian Pin
 */
public class TestFillInDefaultValue  extends RestLiIntegrationTest
{
  @BeforeClass
  public void initClass() throws Exception
  {
    super.init(Collections.emptyList());
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  @Test(dataProvider = "testCaseProvider")
  public void testClientControlledDefaultFilledIn(Long id, DataMap expectedData)
      throws RemoteInvocationException, IOException
  {
    FillInDefaultsRequestBuilders requestBuilders = new FillInDefaultsRequestBuilders();
    FillInDefaultsGetRequestBuilder getRequestBuilder = requestBuilders.get();
    GetRequest<HighLevelRecordWithDefault> req = getRequestBuilder.id(1L).setParam(RestConstants.FILL_DEFAULT_VALUE_IN_RESPONSE_PARAM, true).build();
    HighLevelRecordWithDefault expectedRecord = new HighLevelRecordWithDefault(expectedData);
    Response<HighLevelRecordWithDefault> response = getClient().sendRequest(req).getResponse();
    Assert.assertEquals(response.getEntity(), expectedRecord);
  }

  @DataProvider(name = "testCaseProvider")
  private static Object[][] testCaseProvider()
  {
    // case 1
    DataMap testCase1Expected = new DataMap();
    testCase1Expected.put("intDefaultFieldB", -1);
    DataMap case1MidLevelRecordWithDefault = new DataMap();
    case1MidLevelRecordWithDefault.put("intWithDefault", 0);
    case1MidLevelRecordWithDefault.put("intWithoutDefault", 0);
    DataMap case1LowLevelRecordWithDefault = new DataMap();
    case1LowLevelRecordWithDefault.put("nameWithDefault", "a");
    case1LowLevelRecordWithDefault.put("nameWithoutDefault", "b");
    case1MidLevelRecordWithDefault.put("lowLevelRecordWithDefault", case1LowLevelRecordWithDefault);
    testCase1Expected.put("midLevelRecordWithDefault", case1MidLevelRecordWithDefault);

    return new Object[][] {
        {1L, testCase1Expected},
    };
  }
}
