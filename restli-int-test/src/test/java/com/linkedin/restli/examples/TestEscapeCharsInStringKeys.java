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

package com.linkedin.restli.examples;


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.TwoPartKey;
import com.linkedin.restli.examples.greetings.client.ActionsBuilders;
import com.linkedin.restli.examples.greetings.client.ActionsRequestBuilders;
import com.linkedin.restli.examples.greetings.client.AssociationsBuilders;
import com.linkedin.restli.examples.greetings.client.AssociationsRequestBuilders;
import com.linkedin.restli.examples.greetings.client.ComplexKeysBuilders;
import com.linkedin.restli.examples.greetings.client.ComplexKeysRequestBuilders;
import com.linkedin.restli.examples.greetings.client.StringKeysBuilders;
import com.linkedin.restli.examples.greetings.client.StringKeysRequestBuilders;
import com.linkedin.restli.examples.greetings.client.StringKeysSubBuilders;
import com.linkedin.restli.examples.greetings.client.StringKeysSubRequestBuilders;
import com.linkedin.restli.test.util.RootBuilderWrapper;

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
 * Tests cases where strings can be used as keys with strings containing chars that must be escaped both for URL
 * encoding and for rest.li to be able to lex the URL.
 *
 * @author jbetz
 *
 */
public class TestEscapeCharsInStringKeys extends RestLiIntegrationTest
{
  private final boolean useStringsRequiringEscaping = true;

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

  private String key1()
  {
    return useStringsRequiringEscaping ? StringTestKeys.URL : StringTestKeys.SIMPLEKEY;
  }

  private String key2()
  {
    return useStringsRequiringEscaping ? StringTestKeys.URL2 : StringTestKeys.SIMPLEKEY2;
  }

  private String key3()
  {
    return useStringsRequiringEscaping ? StringTestKeys.URL3 : StringTestKeys.SIMPLEKEY3;
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestStringKeysBuilderDataProvider")
  public void testGetWithSimpleKey(RootBuilderWrapper<String, Message> builders) throws Exception
  {
    Request<Message> req = builders.get().id(key1()).build();
    Response<Message> response = getClient().sendRequest(req).get();

    Assert.assertEquals(response.getEntity().getMessage(), key1(), "Message should match key for key1");

    Request<Message> req2 = builders.get().id(StringTestKeys.COMPLICATED_KEY).build();
    Response<Message> response2 = getClient().sendRequest(req2).get();

    Assert.assertEquals(response2.getEntity().getMessage(), StringTestKeys.COMPLICATED_KEY, "Message should match key for COMPLICATED_KEY");
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestStringKeysOptionsDataProvider")
  public void testBatchGetWithSimpleKey(RestliRequestOptions requestOptions) throws Exception
  {
    Set<String> keys = new HashSet<String>();
    keys.add(key1());
    keys.add(key2());
    Request<BatchResponse<Message>> req = new StringKeysBuilders(requestOptions).batchGet().ids(keys).build();
    BatchResponse<Message> response = getClient().sendRequest(req).get().getEntity();
    Map<String, Message> results = response.getResults();
    Assert.assertEquals(results.get(key1()).getMessage(), key1(), "Message should match key for key1");
    Assert.assertEquals(results.get(key2()).getMessage(), key2(), "Message should match key for key2");
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestStringKeysOptionsDataProvider")
  public void testBatchGetKVWithSimpleKey(RestliRequestOptions requestOptions) throws Exception
  {
    Set<String> keys = new HashSet<String>();
    keys.add(key1());
    keys.add(key2());
    Request<BatchKVResponse<String, Message>> req = new StringKeysBuilders(requestOptions).batchGet().ids(keys).buildKV();
    BatchKVResponse<String,Message> response = getClient().sendRequest(req).get().getEntity();
    Map<String, Message> results = response.getResults();
    Assert.assertEquals(results.get(key1()).getMessage(), key1(), "Message should match key for key1");
    Assert.assertEquals(results.get(key2()).getMessage(), key2(), "Message should match key for key2");
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestStringKeysOptionsDataProvider")
  public void testBatchGetEntityWithSimpleKey(RestliRequestOptions requestOptions) throws Exception
  {
    Set<String> keys = new HashSet<String>();
    keys.add(key1());
    keys.add(key2());
    Request<BatchKVResponse<String, EntityResponse<Message>>> req = new StringKeysRequestBuilders(requestOptions).batchGet().ids(keys).build();
    BatchKVResponse<String, EntityResponse<Message>> response = getClient().sendRequest(req).get().getEntity();
    Map<String, EntityResponse<Message>> results = response.getResults();
    Assert.assertEquals(results.get(key1()).getEntity().getMessage(), key1(), "Message should match key for key1");
    Assert.assertEquals(results.get(key2()).getEntity().getMessage(), key2(), "Message should match key for key2");
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestAssociationsSubBuilderDataProvider")
  public void testGetWithAssociationKey(RootBuilderWrapper<CompoundKey, Message> builders) throws Exception
  {
    CompoundKey key = new CompoundKey();
    key.append("src", key1());
    key.append("dest", key2());

    Request<Message> request = builders.get().id(key).build();
    Message response = getClient().sendRequest(request).get().getEntity();
    Assert.assertNotNull(response);
    Assert.assertEquals(response.getId(), key.getPart("src") + " " + key.getPart("dest"), "Message should be key1 + ' ' + key2 for associationKey(key1,key2)");
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestComplexKeysBuilderDataProvider")
  public void testGetWithComplexKey(RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> builders) throws Exception
  {
    TwoPartKey key = new TwoPartKey();
    key.setMajor(key1());
    key.setMinor(key2());

    TwoPartKey params = new TwoPartKey();
    params.setMajor(key1());
    params.setMinor(key3());

    ComplexResourceKey<TwoPartKey, TwoPartKey> complexKey = new ComplexResourceKey<TwoPartKey, TwoPartKey>(key, params);
    Request<Message> request = builders.get().id(complexKey).build();
    Message response = getClient().sendRequest(request).get().getEntity();
    Assert.assertNotNull(response);
    Assert.assertEquals(response.getId(), key.getMajor() + " " + key.getMinor(), "Message should be key1 + ' ' + key2 for complexKey(key1,key2)");
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestStringKeysSubBuilderDataProvider")
  public void testGetSubResourceKeys(RootBuilderWrapper<String, Message> builders) throws Exception
  {
    String parentKey = key1();
    String subKey = key2();

    Request<Message> request = builders.get().setPathKey("parentKey", parentKey).id(subKey).build();
    Message response = getClient().sendRequest(request).get().getEntity();
    Assert.assertNotNull(response);
    Assert.assertEquals(response.getId(), parentKey + " " + subKey, "Message should be key1 + ' ' + key2 for subResourceKey(key1,key2)");
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestActionBuilderDataProvider")
  public void testActionWithStringParam(RootBuilderWrapper<?, ?> builders) throws Exception
  {
    Request<String> request = builders.<String>action("Echo").setActionParam("Input", key1()).build();
    String echo = getClient().sendRequest(request).get().getEntity();
    Assert.assertEquals(echo, key1(), "Echo response should be key1");
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestStringKeysBuilderDataProvider")
  public void testFinderWithStringParam(RootBuilderWrapper<String, Message> builders) throws Exception
  {
    Request<CollectionResponse<Message>> request = builders.findBy("Search").setQueryParam("keyword", key1()).build();
    CollectionResponse<Message> response = getClient().sendRequest(request).get().getEntity();
    List<Message> hits = response.getElements();
    Assert.assertEquals(hits.size(), 1);
    Message hit = hits.get(0);
    Assert.assertEquals(hit.getMessage(), key1(), "Message of matching result should be key1");
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestStringKeysBuilderDataProvider")
  private static Object[][] requestStringKeysBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<String, Message>(new StringKeysBuilders()) },
      { new RootBuilderWrapper<String, Message>(new StringKeysBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<String, Message>(new StringKeysRequestBuilders()) },
      { new RootBuilderWrapper<String, Message>(new StringKeysRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) }
    };
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestStringKeysOptionsDataProvider")
  private static Object[][] requestStringKeysOptionsDataProvider()
  {
    return new Object[][] {
      { RestliRequestOptions.DEFAULT_OPTIONS },
      { TestConstants.FORCE_USE_NEXT_OPTIONS }
    };
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestStringKeysSubBuilderDataProvider")
  private static Object[][] requestStringKeysSubBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<String, Message>(new StringKeysSubBuilders()) },
      { new RootBuilderWrapper<String, Message>(new StringKeysSubBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<String, Message>(new StringKeysSubRequestBuilders()) },
      { new RootBuilderWrapper<String, Message>(new StringKeysSubRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) }
    };
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestAssociationsSubBuilderDataProvider")
  private static Object[][] requestAssociationsSubBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<CompoundKey, Message>(new AssociationsBuilders()) },
      { new RootBuilderWrapper<CompoundKey, Message>(new AssociationsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<CompoundKey, Message>(new AssociationsRequestBuilders()) },
      { new RootBuilderWrapper<CompoundKey, Message>(new AssociationsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) }
    };
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestComplexKeysBuilderDataProvider")
  private static Object[][] requestComplexKeysBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message>(new ComplexKeysBuilders()) },
      { new RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message>(new ComplexKeysBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message>(new ComplexKeysRequestBuilders()) },
      { new RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message>(new ComplexKeysRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) }
    };
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestActionBuilderDataProvider")
  private static Object[][] requestActionBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<Object, RecordTemplate>(new ActionsBuilders()) },
      { new RootBuilderWrapper<Object, RecordTemplate>(new ActionsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<Object, RecordTemplate>(new ActionsRequestBuilders()) },
      { new RootBuilderWrapper<Object, RecordTemplate>(new ActionsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) }
    };
  }
}
