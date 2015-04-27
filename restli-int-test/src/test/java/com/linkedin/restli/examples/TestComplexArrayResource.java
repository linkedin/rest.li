/*
   Copyright (c) 2012 LinkedIn Corp.

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

/**
 * $Id: $
 */

package com.linkedin.restli.examples;


import com.linkedin.data.template.LongArray;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.examples.greetings.api.ComplexArray;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.ComplexArrayBuilders;
import com.linkedin.restli.examples.greetings.client.ComplexArrayRequestBuilders;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;



/**
 * @author Moira Tagle
 * @version $Revision: $
 */
@Test(groups = { "async" })
public class TestComplexArrayResource extends RestLiIntegrationTest
{
  @BeforeClass(alwaysRun=true)
  public void initClass(ITestContext ctx) throws Exception
  {
    Set<String> includedGroups =
        new HashSet<String>(ctx.getCurrentXmlTest().getIncludedGroups());
    super.init(includedGroups.contains("async"));
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testGet(RootBuilderWrapper<ComplexResourceKey<ComplexArray, ComplexArray>, Greeting> builders) throws RemoteInvocationException, CloneNotSupportedException
  {
    // all array are singletons with single element
    LongArray singleton = new LongArray();
    singleton.add(1L);

    ComplexArray next = new ComplexArray().setArray(singleton);
    ComplexArray key = new ComplexArray().setArray(singleton).setNext(next);
    ComplexArray params = new ComplexArray().setArray(singleton).setNext(next);
    ComplexResourceKey<ComplexArray, ComplexArray> complexKey = new ComplexResourceKey<ComplexArray, ComplexArray>(key, params);

    Request<Greeting> request = builders.get().id(complexKey).build();

    getClient().sendRequest(request).getResponse().getEntity();
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "versionWithRequestOptionsDataProvider")
  public void testBatchGet(ProtocolVersion version, RestliRequestOptions options)
    throws RemoteInvocationException
  {
    List<ComplexResourceKey<ComplexArray, ComplexArray>> complexKeys = getBatchCompleKeys();

    ComplexArrayBuilders builders = new ComplexArrayBuilders(options);
    Request<BatchKVResponse<ComplexResourceKey<ComplexArray, ComplexArray>, Greeting>> request =
        builders.batchGet().ids(complexKeys).buildKV();

    Response<BatchKVResponse<ComplexResourceKey<ComplexArray, ComplexArray>, Greeting>> response =
        getClient().sendRequest(request).getResponse();

    Greeting greeting1 = response.getEntity().getResults().get(complexKeys.get(0));
    Assert.assertNotNull(greeting1);

    Greeting greeting2 = response.getEntity().getResults().get(complexKeys.get(1));
    Assert.assertNotNull(greeting2);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "versionWithRequestOptionsDataProvider")
  public void testBatchGetKV(ProtocolVersion version, RestliRequestOptions options)
    throws RemoteInvocationException
  {
    List<ComplexResourceKey<ComplexArray, ComplexArray>> complexKeys = getBatchCompleKeys();

    ComplexArrayBuilders builders = new ComplexArrayBuilders(options);
    Request<BatchKVResponse<ComplexResourceKey<ComplexArray, ComplexArray>, Greeting>> request2 =
      builders.batchGet().ids(complexKeys).buildKV();

    Response<BatchKVResponse<ComplexResourceKey<ComplexArray, ComplexArray>, Greeting>> response2 =
      getClient().sendRequest(request2).getResponse();

    Greeting greeting1 = response2.getEntity().getResults().get(complexKeys.get(0));
    Assert.assertNotNull(greeting1);
    Greeting greeting2 = response2.getEntity().getResults().get(complexKeys.get(1));
    Assert.assertNotNull(greeting2);

  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "versionWithRequestOptionsDataProvider")
  public void testBatchGetEntity(ProtocolVersion version, RestliRequestOptions options)
    throws RemoteInvocationException
  {
    List<ComplexResourceKey<ComplexArray, ComplexArray>> complexKeys = getBatchCompleKeys();

    ComplexArrayRequestBuilders builders = new ComplexArrayRequestBuilders(options);
    Request<BatchKVResponse<ComplexResourceKey<ComplexArray, ComplexArray>, EntityResponse<Greeting>>> request2 =
      builders.batchGet().ids(complexKeys).build();

    Response<BatchKVResponse<ComplexResourceKey<ComplexArray, ComplexArray>, EntityResponse<Greeting>>> response2 =
      getClient().sendRequest(request2).getResponse();

    EntityResponse<Greeting> greeting1 = response2.getEntity().getResults().get(complexKeys.get(0));
    Assert.assertNotNull(greeting1);
    EntityResponse<Greeting> greeting2 = response2.getEntity().getResults().get(complexKeys.get(1));
    Assert.assertNotNull(greeting2);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testFinder(RootBuilderWrapper<ComplexResourceKey<ComplexArray, ComplexArray>, Greeting> builders) throws RemoteInvocationException
  {
    LongArray singleton = new LongArray();
    singleton.add(1L);
    ComplexArray next = new ComplexArray().setArray(singleton);
    ComplexArray array = new ComplexArray().setArray(singleton).setNext(next);

    Request<CollectionResponse<Greeting>> request = builders.findBy("Finder").setQueryParam("array", array).build();
    getClient().sendRequest(request).getResponse().getEntity();
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testAction(RootBuilderWrapper<ComplexResourceKey<ComplexArray, ComplexArray>, Greeting> builders) throws RemoteInvocationException
  {
    LongArray singleton = new LongArray();
    singleton.add(1L);
    ComplexArray next = new ComplexArray().setArray(singleton);
    ComplexArray array = new ComplexArray().setArray(singleton).setNext(next);

    Request<Integer> request = builders.<Integer>action("Action").setActionParam("Array", array).build();
    getClient().sendRequest(request).getResponse().getEntity();
  }

  private static List<ComplexResourceKey<ComplexArray, ComplexArray>> getBatchCompleKeys()
  {
    LongArray singleton1 = new LongArray();
    singleton1.add(1L);
    ComplexArray next1 = new ComplexArray().setArray(singleton1);
    ComplexArray key1 = new ComplexArray().setArray(singleton1).setNext(next1);
    ComplexArray params1 = new ComplexArray().setArray(singleton1).setNext(next1);
    ComplexResourceKey<ComplexArray, ComplexArray> complexKey1 =
      new ComplexResourceKey<ComplexArray, ComplexArray>(key1, params1);

    LongArray singleton2 = new LongArray();
    singleton2.add(2L);
    ComplexArray next2 = new ComplexArray().setArray(singleton2);
    ComplexArray key2 = new ComplexArray().setArray(singleton2).setNext(next2);
    ComplexArray params2 = new ComplexArray().setArray(singleton2).setNext(next2);
    ComplexResourceKey<ComplexArray, ComplexArray> complexKey2 =
      new ComplexResourceKey<ComplexArray, ComplexArray>(key2, params2);

    List<ComplexResourceKey<ComplexArray, ComplexArray>> complexKeys =
      new ArrayList<ComplexResourceKey<ComplexArray, ComplexArray>>();
    complexKeys.add(complexKey1);
    complexKeys.add(complexKey2);

    return complexKeys;
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  private static Object[][] requestBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<ComplexResourceKey<ComplexArray, ComplexArray>, Greeting>(new ComplexArrayBuilders()) },
      { new RootBuilderWrapper<ComplexResourceKey<ComplexArray, ComplexArray>, Greeting>(new ComplexArrayBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<ComplexResourceKey<ComplexArray, ComplexArray>, Greeting>(new ComplexArrayRequestBuilders()) },
      { new RootBuilderWrapper<ComplexResourceKey<ComplexArray, ComplexArray>, Greeting>(new ComplexArrayRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) }
    };
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "versionWithRequestOptionsDataProvider")
  private static Object[][] versionWithRequestOptionsDataProvider()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), RestliRequestOptions.DEFAULT_OPTIONS },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), TestConstants.FORCE_USE_NEXT_OPTIONS },
    };
  }
}
