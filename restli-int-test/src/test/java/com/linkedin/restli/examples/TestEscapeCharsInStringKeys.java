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


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
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

import java.util.Collections;
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
  private static final Client            CLIENT      = new TransportClientAdapter(new HttpClientFactory().getClient(Collections.<String, String> emptyMap()));
  private static final String            URI_PREFIX  = "http://localhost:1338/";
  private static final RestClient        REST_CLIENT = new RestClient(CLIENT, URI_PREFIX);


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

  @Test(dataProvider = "requestStringKeysBuilderDataProvider")
  public void testGetWithSimpleKey(RootBuilderWrapper<String, Message> builders) throws Exception
  {
    Request<Message> req = builders.get().id(key1()).build();
    Response<Message> response = REST_CLIENT.sendRequest(req).get();

    Assert.assertEquals(response.getEntity().getMessage(), key1(), "Message should match key for key1");
  }

  @Test(dataProvider = "requestStringKeysBuilderDataProvider")
  public void testBatchGetWithSimpleKey(RootBuilderWrapper<String, Message> builders) throws Exception
  {
    Set<String> keys = new HashSet<String>();
    keys.add(key1());
    keys.add(key2());
    Request<BatchResponse<Message>> req = builders.batchGet().ids(keys).build();
    BatchResponse<Message> response = REST_CLIENT.sendRequest(req).get().getEntity();
    Map<String, Message> results = response.getResults();
    Assert.assertEquals(results.get(key1()).getMessage(), key1(), "Message should match key for key1");
    Assert.assertEquals(results.get(key2()).getMessage(), key2(), "Message should match key for key2");
  }

  @Test(dataProvider = "requestAssociationsSubBuilderDataProvider")
  public void testGetWithAssociationKey(RootBuilderWrapper<CompoundKey, Message> builders) throws Exception
  {
    CompoundKey key = new CompoundKey();
    key.append("src", key1());
    key.append("dest", key2());

    Request<Message> request = builders.get().id(key).build();
    Message response = REST_CLIENT.sendRequest(request).get().getEntity();
    Assert.assertNotNull(response);
    Assert.assertEquals(response.getId(), key.getPart("src") + " " + key.getPart("dest"), "Message should be key1 + ' ' + key2 for associationKey(key1,key2)");
  }

  @Test(dataProvider = "requestComplexKeysBuilderDataProvider")
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
    Message response = REST_CLIENT.sendRequest(request).get().getEntity();
    Assert.assertNotNull(response);
    Assert.assertEquals(response.getId(), key.getMajor() + " " + key.getMinor(), "Message should be key1 + ' ' + key2 for complexKey(key1,key2)");
  }

  @Test(dataProvider = "requestStringKeysSubBuilderDataProvider")
  public void testGetSubResourceKeys(RootBuilderWrapper<String, Message> builders) throws Exception
  {
    String parentKey = key1();
    String subKey = key2();

    Request<Message> request = builders.get().setPathKey("parentKey", parentKey).id(subKey).build();
    Message response = REST_CLIENT.sendRequest(request).get().getEntity();
    Assert.assertNotNull(response);
    Assert.assertEquals(response.getId(), parentKey + " " + subKey, "Message should be key1 + ' ' + key2 for subResourceKey(key1,key2)");
  }

  @Test(dataProvider = "requestActionBuilderDataProvider")
  public void testActionWithStringParam(RootBuilderWrapper<?, ?> builders) throws Exception
  {
    Request<String> request = builders.<String>action("Echo").setActionParam("Input", key1()).build();
    String echo = REST_CLIENT.sendRequest(request).get().getEntity();
    Assert.assertEquals(echo, key1(), "Echo response should be key1");
  }

  @Test(dataProvider = "requestStringKeysBuilderDataProvider")
  public void testFinderWithStringParam(RootBuilderWrapper<String, Message> builders) throws Exception
  {
    Request<CollectionResponse<Message>> request = builders.findBy("Search").setQueryParam("keyword", key1()).build();
    CollectionResponse<Message> response = REST_CLIENT.sendRequest(request).get().getEntity();
    List<Message> hits = response.getElements();
    Assert.assertEquals(hits.size(), 1);
    Message hit = hits.get(0);
    Assert.assertEquals(hit.getMessage(), key1(), "Message of matching result should be key1");
  }

  @DataProvider
  private static Object[][] requestStringKeysBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<String, Message>(new StringKeysBuilders()) },
      { new RootBuilderWrapper<String, Message>(new StringKeysRequestBuilders()) }
    };
  }

  @DataProvider
  private static Object[][] requestStringKeysSubBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<String, Message>(new StringKeysSubBuilders()) },
      { new RootBuilderWrapper<String, Message>(new StringKeysSubRequestBuilders()) }
    };
  }

  @DataProvider
  private static Object[][] requestAssociationsSubBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<CompoundKey, Message>(new AssociationsBuilders()) },
      { new RootBuilderWrapper<CompoundKey, Message>(new AssociationsRequestBuilders()) }
    };
  }

  @DataProvider
  private static Object[][] requestComplexKeysBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message>(new ComplexKeysBuilders()) },
      { new RootBuilderWrapper<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message>(new ComplexKeysRequestBuilders()) }
    };
  }

  @DataProvider
  private static Object[][] requestActionBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<Object, RecordTemplate>(new ActionsBuilders()) },
      { new RootBuilderWrapper<Object, RecordTemplate>(new ActionsRequestBuilders()) }
    };
  }
}
