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
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
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
    super.init();
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
    DataMap testCase1Data = new DataMap();
    testCase1Data.put("intDefaultFieldB", -1);
    DataMap case1DataMidLevel = new DataMap();
    case1DataMidLevel.put("intWithDefault", -1);
    DataMap case1DataLowLevel = new DataMap();
    case1DataLowLevel.put("nameWithDefault", "i_am_default_name");
    DataMap case1DataLowLevelDefault = new DataMap();
    case1DataLowLevelDefault.put("nameWithDefault", "mid_default_a");
    case1DataLowLevelDefault.put("nameWithoutDefault", "i_am_without_default_in_midlevel");
    case1DataMidLevel.put("lowLevelRecordWithoutDefault", case1DataLowLevel);
    case1DataMidLevel.put("lowLevelRecordWithDefault", case1DataLowLevelDefault);
    testCase1Data.put("midLevelRecordWithoutDefault", case1DataMidLevel);
    DataMap case1DataMidLevelDefault = new DataMap();
    case1DataMidLevelDefault.put("intWithDefault", 0);
    case1DataMidLevelDefault.put("intWithoutDefault", 0);
    testCase1Data.put("midLevelRecordWithoutDefault", case1DataMidLevel);
    testCase1Data.put("midLevelRecordWithDefault", case1DataMidLevelDefault);

    return new Object[][] {
        {1L, testCase1Data},
    };
  }
}
