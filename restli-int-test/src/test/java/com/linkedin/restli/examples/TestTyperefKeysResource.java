/*
   Copyright (c) 2015 LinkedIn Corp.

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


import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.BatchGetEntityRequest;
import com.linkedin.restli.client.BatchGetKVRequest;
import com.linkedin.restli.client.BatchGetRequest;
import com.linkedin.restli.client.CreateIdRequest;
import com.linkedin.restli.client.CreateRequest;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.IdResponse;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.TyperefKeysBuilders;
import com.linkedin.restli.examples.greetings.client.TyperefKeysRequestBuilders;
import com.linkedin.restli.internal.common.TestConstants;

import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author xma
 */
public class TestTyperefKeysResource extends RestLiIntegrationTest
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

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  @SuppressWarnings("deprecation")
  public void testCreate(RestliRequestOptions requestOptions) throws RemoteInvocationException
  {
    Greeting greeting = new Greeting().setId(1L).setMessage("Foo").setTone(Tone.FRIENDLY);
    CreateRequest<Greeting> req = new TyperefKeysBuilders(requestOptions).create().input(greeting).build();

    Response<EmptyRecord> resp = getClient().sendRequest(req).getResponse();
    Assert.assertEquals(resp.getId(), "1");
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  public void testCreateId(RestliRequestOptions requestOptions) throws RemoteInvocationException
  {
    Greeting greeting = new Greeting().setId(1L).setMessage("Foo").setTone(Tone.FRIENDLY);
    CreateIdRequest<Long, Greeting> req = new TyperefKeysRequestBuilders(requestOptions).create().input(greeting).build();

    Response<IdResponse<Long>> resp = getClient().sendRequest(req).getResponse();
    Assert.assertEquals(resp.getEntity().getId(), new Long(1L));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  public void testBatchGet(RestliRequestOptions requestOptions) throws RemoteInvocationException
  {
    BatchGetRequest<Greeting> req = new TyperefKeysBuilders(requestOptions).batchGet().ids(1L, 2L).build();
    Response<BatchResponse<Greeting>> resp = getClient().sendRequest(req).getResponse();

    Map<String, Greeting> results = resp.getEntity().getResults();
    Assert.assertEquals(results.get("1").getId(), new Long(1L));
    Assert.assertEquals(results.get("2").getId(), new Long(2L));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  public void testBatchGetKV(RestliRequestOptions requestOptions) throws RemoteInvocationException
  {
    BatchGetKVRequest<Long,Greeting> req = new TyperefKeysBuilders(requestOptions).batchGet().ids(1L, 2L).buildKV();
    Response<BatchKVResponse<Long, Greeting>> resp = getClient().sendRequest(req).getResponse();

    Map<Long, Greeting> results = resp.getEntity().getResults();
    Assert.assertEquals(results.get(1L).getId(), new Long(1L));
    Assert.assertEquals(results.get(2L).getId(), new Long(2L));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  public void testBatchGetEntity(RestliRequestOptions requestOptions) throws RemoteInvocationException
  {
    BatchGetEntityRequest<Long,Greeting> req = new TyperefKeysRequestBuilders(requestOptions).batchGet().ids(1L, 2L).build();
    Response<BatchKVResponse<Long, EntityResponse<Greeting>>> resp = getClient().sendRequest(req).getResponse();

    Map<Long, EntityResponse<Greeting>> results = resp.getEntity().getResults();
    Assert.assertEquals(results.get(1L).getEntity().getId(), new Long(1L));
    Assert.assertEquals(results.get(2L).getEntity().getId(), new Long(2L));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  private static Object[][] requestOptionsDataProvider()
  {
    return new Object[][] {
      { RestliRequestOptions.DEFAULT_OPTIONS },
      { com.linkedin.restli.examples.TestConstants.FORCE_USE_NEXT_OPTIONS }
    };
  }
}
