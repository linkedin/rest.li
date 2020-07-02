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
import com.linkedin.restli.client.ActionRequest;
import com.linkedin.restli.client.BatchFindRequest;
import com.linkedin.restli.client.BatchGetEntityRequest;
import com.linkedin.restli.client.FindRequest;
import com.linkedin.restli.client.GetAllRequest;
import com.linkedin.restli.client.GetRequest;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.BatchFinderCriteriaResult;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.examples.defaults.api.FillInDefaultsGetRequestBuilder;
import com.linkedin.restli.examples.defaults.api.FillInDefaultsRequestBuilders;
import com.linkedin.restli.examples.defaults.api.HighLevelRecordWithDefault;
import com.linkedin.restli.examples.defaults.api.RecordCriteria;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  private DataMap expectedTestData;

  @BeforeClass
  public void initClass() throws Exception
  {
    super.init(Collections.emptyList());
    expectedTestData = new DataMap();
    expectedTestData.put("intDefaultFieldB", -1);
    DataMap case1MidLevelRecordWithDefault = new DataMap();
    case1MidLevelRecordWithDefault.put("intWithDefault", 0);
    case1MidLevelRecordWithDefault.put("intWithoutDefault", 0);
    DataMap case1LowLevelRecordWithDefault = new DataMap();
    case1LowLevelRecordWithDefault.put("nameWithDefault", "a");
    case1LowLevelRecordWithDefault.put("nameWithoutDefault", "b");
    case1MidLevelRecordWithDefault.put("lowLevelRecordWithDefault", case1LowLevelRecordWithDefault);
    expectedTestData.put("midLevelRecordWithDefault", case1MidLevelRecordWithDefault);
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  @DataProvider(name = "testGetData")
  private Object[][] testGetData() throws CloneNotSupportedException
  {
    HighLevelRecordWithDefault expected = new HighLevelRecordWithDefault(expectedTestData.clone()).setNoDefaultFieldA(1);
    return new Object[][] {{1L, expected},};
  }

  @Test(dataProvider = "testGetData")
  public void testGet(Long id, HighLevelRecordWithDefault expectedRecord)
      throws RemoteInvocationException, IOException
  {
    FillInDefaultsRequestBuilders requestBuilders = new FillInDefaultsRequestBuilders();
    FillInDefaultsGetRequestBuilder getRequestBuilder = requestBuilders.get();
    GetRequest<HighLevelRecordWithDefault> req = getRequestBuilder.id(id).setParam(RestConstants.FILL_DEFAULT_VALUE_IN_RESPONSE_PARAM, true).build();
    HighLevelRecordWithDefault actual = getClient().sendRequest(req).getResponse().getEntity();
    Assert.assertEquals(actual, expectedRecord);
  }

  @DataProvider(name = "testBatchGetData")
  private Object[][] testBatchGetData() throws CloneNotSupportedException
  {
    HighLevelRecordWithDefault a = new HighLevelRecordWithDefault(expectedTestData.clone()).setNoDefaultFieldA(1);
    HighLevelRecordWithDefault b = new HighLevelRecordWithDefault(expectedTestData.clone()).setNoDefaultFieldA(2);
    HighLevelRecordWithDefault c = new HighLevelRecordWithDefault(expectedTestData.clone()).setNoDefaultFieldA(3);
    return new Object[][]{
            {new Long[]{1L, 2L, 3L}, new HighLevelRecordWithDefault[]{a, b, c}}
    };
  }

  @Test(dataProvider = "testBatchGetData")
  public void testFillInDefaultBatchGet(Long[] ids, HighLevelRecordWithDefault[] expected) throws RemoteInvocationException
  {
    Map<Integer, HighLevelRecordWithDefault> idToRecord = new HashMap<>();
    for (int i = 0; i < ids.length; i++)
    {
      idToRecord.put(Math.toIntExact(ids[i]), expected[i]);
    }
    FillInDefaultsRequestBuilders builders = new FillInDefaultsRequestBuilders();
    BatchGetEntityRequest<Long, HighLevelRecordWithDefault> request =
        builders.batchGet().setParam(RestConstants.FILL_DEFAULT_VALUE_IN_RESPONSE_PARAM, true).ids(ids).build();
    BatchKVResponse<Long, EntityResponse<HighLevelRecordWithDefault>> batchKVResponse =
        getClient().sendRequest(request).getResponse().getEntity();
    for (Map.Entry<Long, EntityResponse<HighLevelRecordWithDefault>> entry : batchKVResponse.getResults().entrySet())
    {
      HighLevelRecordWithDefault actualEntity = entry.getValue().getEntity();
      Assert.assertEquals(actualEntity, idToRecord.getOrDefault(actualEntity.getNoDefaultFieldA(), null));
    }
  }

  @DataProvider(name = "testGetAllData")
  private Object[] testGetAllData() throws CloneNotSupportedException {
    HighLevelRecordWithDefault a = new HighLevelRecordWithDefault(expectedTestData.clone()).setNoDefaultFieldA(0);
    HighLevelRecordWithDefault b = new HighLevelRecordWithDefault(expectedTestData.clone()).setNoDefaultFieldA(1);
    HighLevelRecordWithDefault c = new HighLevelRecordWithDefault(expectedTestData.clone()).setNoDefaultFieldA(2);
    Map<Integer, HighLevelRecordWithDefault> expect = new HashMap<>();
    expect.put(0, a);
    expect.put(1, b);
    expect.put(2, c);
    return new Object[]{expect};
  }

  @Test(dataProvider = "testGetAllData")
  public void testFillInDefaultGetAll(Map<Integer, HighLevelRecordWithDefault> expected) throws RemoteInvocationException
  {
    FillInDefaultsRequestBuilders builders = new FillInDefaultsRequestBuilders();
    GetAllRequest<HighLevelRecordWithDefault> request =
        builders.getAll().setParam(RestConstants.FILL_DEFAULT_VALUE_IN_RESPONSE_PARAM, true).build();
    List<HighLevelRecordWithDefault> allResult = getClient().sendRequest(request).getResponse().getEntity().getElements();
    Map<Integer, HighLevelRecordWithDefault> actual = new HashMap<>();
    for (HighLevelRecordWithDefault oneRecord : allResult)
    {
      actual.put(oneRecord.getNoDefaultFieldA(), oneRecord);
    }
    Assert.assertEquals(actual, expected);
  }

  @DataProvider(name = "testFinderData")
  private Object[] testFinderData()
  {
    return new Integer[]{2, 3};
  }

  @Test(dataProvider = "testFinderData")
  public void testFillInDefaultFinder(Integer count) throws RemoteInvocationException
  {
    FillInDefaultsRequestBuilders builders = new FillInDefaultsRequestBuilders();
    FindRequest<HighLevelRecordWithDefault> request = builders.findByHighLevelRecord()
        .setParam(RestConstants.FILL_DEFAULT_VALUE_IN_RESPONSE_PARAM, true)
        .setParam("totalCount", count).build();
    List<HighLevelRecordWithDefault> result = getClient().sendRequest(request).getResponse().getEntity().getElements();
    Set<HighLevelRecordWithDefault> actual = new HashSet<>(result);
    Set<HighLevelRecordWithDefault> expect = new HashSet<>();
    for (int i = 0; i < count; i++)
    {
      expect.add(new HighLevelRecordWithDefault(expectedTestData).setNoDefaultFieldA(i));
    }
    Assert.assertEquals(actual, expect);
  }

  @DataProvider(name = "testBatchFinderData")
  private Object[][] testBatchFinderData() throws CloneNotSupportedException {

    HighLevelRecordWithDefault expected1 = new HighLevelRecordWithDefault(expectedTestData.clone()).setNoDefaultFieldA(1);
    HighLevelRecordWithDefault expected2 = new HighLevelRecordWithDefault(expectedTestData.clone()).setNoDefaultFieldA(2);
    return new Object[][]{
            {
              new RecordCriteria[]{new RecordCriteria().setIntWithoutDefault(1), new RecordCriteria().setIntWithoutDefault(2)},
              new HighLevelRecordWithDefault[]{expected1, expected2}
            }
    };
  }

  @Test(dataProvider = "testBatchFinderData")
  public void testFillInDefaultBatchFinder(Object[] criteria, HighLevelRecordWithDefault[] expected)
          throws RemoteInvocationException
  {
    FillInDefaultsRequestBuilders builders = new FillInDefaultsRequestBuilders();
    BatchFindRequest<HighLevelRecordWithDefault> request = builders.batchFindBySearchRecords()
        .addCriteriaParam((RecordCriteria) criteria[0])
        .addCriteriaParam((RecordCriteria) criteria[1])
        .setParam(RestConstants.FILL_DEFAULT_VALUE_IN_RESPONSE_PARAM, true).build();
    List<BatchFinderCriteriaResult<HighLevelRecordWithDefault>> batchFinderCriteriaResults = getClient()
        .sendRequest(request).getResponse().getEntity().getResults();
    Set<HighLevelRecordWithDefault> actualActionResponse = new HashSet<>();
    for (BatchFinderCriteriaResult<HighLevelRecordWithDefault> result : batchFinderCriteriaResults)
    {
      actualActionResponse.addAll(result.getElements());
    }
    Set<HighLevelRecordWithDefault> expectedActionResponse = new HashSet<>(Arrays.asList(expected));
    Assert.assertEquals(actualActionResponse, expectedActionResponse);
  }

  @DataProvider(name = "testActionData")
  private Object[] testActionData()
  {
    return new Long[]{1L, 2L};
  }

  @Test(dataProvider = "testActionData")
  public void testFillInDefaultAction(Long actionParam) throws RemoteInvocationException {
    FillInDefaultsRequestBuilders builders = new FillInDefaultsRequestBuilders();
    ActionRequest<HighLevelRecordWithDefault> request = builders.actionDefaultFillAction().actionParamParam(actionParam)
        .setParam(RestConstants.FILL_DEFAULT_VALUE_IN_RESPONSE_PARAM, true)
        .build();
    HighLevelRecordWithDefault actual = getClient().sendRequest(request).getResponse().getEntity();
    HighLevelRecordWithDefault expect = new HighLevelRecordWithDefault(expectedTestData)
            .setNoDefaultFieldA(Math.toIntExact(actionParam));
    Assert.assertEquals(actual, expect);
  }
}
