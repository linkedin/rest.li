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
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.ActionRequest;
import com.linkedin.restli.client.BatchGetRequest;
import com.linkedin.restli.client.FindRequest;
import com.linkedin.restli.client.GetRequest;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.examples.greetings.api.ComplexArray;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.ComplexArrayBuilders;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;


/**
 * @author Moira Tagle
 * @version $Revision: $
 */
public class TestComplexArrayResource  extends RestLiIntegrationTest
{
  private static final Client CLIENT = new TransportClientAdapter(new HttpClientFactory().getClient(Collections.<String, String>emptyMap()));
  private static final String URI_PREFIX = "http://localhost:1338/";
  private static final RestClient REST_CLIENT = new RestClient(CLIENT, URI_PREFIX);
  private static final ComplexArrayBuilders BUILDERS = new ComplexArrayBuilders();

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

  @Test
  public void testGet() throws RemoteInvocationException, CloneNotSupportedException
  {
    // all array are singletons with single element
    LongArray singleton = new LongArray();
    singleton.add(1L);

    ComplexArray next = new ComplexArray().setArray(singleton);
    ComplexArray key = new ComplexArray().setArray(singleton).setNext(next);
    ComplexArray params = new ComplexArray().setArray(singleton).setNext(next);
    ComplexResourceKey<ComplexArray, ComplexArray> complexKey = new ComplexResourceKey<ComplexArray, ComplexArray>(key, params);

    GetRequest<Greeting> request = BUILDERS.get().id(complexKey).build();

    REST_CLIENT.sendRequest(request).getResponse().getEntity();
  }

  @Test
  public void testBatchGet() throws RemoteInvocationException
  {
    LongArray singleton1 = new LongArray();
    singleton1.add(1L);
    ComplexArray next1 = new ComplexArray().setArray(singleton1);
    ComplexArray key1 = new ComplexArray().setArray(singleton1).setNext(next1);
    ComplexArray params1 = new ComplexArray().setArray(singleton1).setNext(next1);
    ComplexResourceKey<ComplexArray, ComplexArray> complexKey1 = new ComplexResourceKey<ComplexArray, ComplexArray>(key1, params1);

    LongArray singleton2 = new LongArray();
    singleton2.add(2L);
    ComplexArray next2 = new ComplexArray().setArray(singleton2);
    ComplexArray key2 = new ComplexArray().setArray(singleton2).setNext(next2);
    ComplexArray params2 = new ComplexArray().setArray(singleton2).setNext(next2);
    ComplexResourceKey<ComplexArray, ComplexArray> complexKey2 = new ComplexResourceKey<ComplexArray, ComplexArray>(key2, params2);

    Collection<ComplexResourceKey<ComplexArray, ComplexArray>> complexKeys = new ArrayList<ComplexResourceKey<ComplexArray, ComplexArray>>();
    complexKeys.add(complexKey1);
    complexKeys.add(complexKey2);

    BatchGetRequest<Greeting> request = BUILDERS.batchGet().ids(complexKeys).build();

    REST_CLIENT.sendRequest(request).getResponse().getEntity();
  }

  @Test
  public void testFinder() throws RemoteInvocationException
  {
    LongArray singleton = new LongArray();
    singleton.add(1L);
    ComplexArray next = new ComplexArray().setArray(singleton);
    ComplexArray array = new ComplexArray().setArray(singleton).setNext(next);

    FindRequest<Greeting> request = BUILDERS.findByFinder().arrayParam(array).build();
    REST_CLIENT.sendRequest(request).getResponse().getEntity();
  }

  @Test
  public void testAction() throws RemoteInvocationException
  {
    LongArray singleton = new LongArray();
    singleton.add(1L);
    ComplexArray next = new ComplexArray().setArray(singleton);
    ComplexArray array = new ComplexArray().setArray(singleton).setNext(next);

    ActionRequest<Integer> request = BUILDERS.actionAction().paramArray(array).build();
    REST_CLIENT.sendRequest(request).getResponse().getEntity();
  }
}
