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
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;

import com.linkedin.restli.client.*;

import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.CookieGetRequestBuilder;
import com.linkedin.restli.examples.greetings.client.CookieRequestBuilders;
import com.linkedin.restli.internal.common.CookieUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.HttpCookie;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * @author Boyang Chen
 */
public class TestCookieResource extends RestLiIntegrationTest
{
  private static final Client CLIENT = new TransportClientAdapter(new HttpClientFactory.Builder().build().getClient(Collections.<String, String>emptyMap()));
  private static final String URI_PREFIX = "http://localhost:1338/";
  private static final RestClient REST_CLIENT = new RestClient(CLIENT, URI_PREFIX);

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

  /**
   * A customized get test for inquring the cookies routing, look up in Cookie resource file
   *
   * @throws RemoteInvocationException
   */
  @Test
  public void testCookiesNormal() throws RemoteInvocationException
  {
    List<HttpCookie> requestCookies = Arrays.asList(new HttpCookie("GET", "10"));
    GetRequest<Greeting> req = new CookieRequestBuilders().get().id(1L).setCookies(requestCookies).build();
    Response<Greeting> resp = REST_CLIENT.sendRequest(req).getResponse();

    Assert.assertEquals(resp.getCookies(), Collections.singletonList(new HttpCookie("10", "GET")));
  }

  /**
   * Try a batch command get to see the cookie setting is ok or not
   *
   * @throws RemoteInvocationException
   */
  @Test
  public void testCookieBatchGet() throws RemoteInvocationException
  {
    List<HttpCookie> requestCookies = Arrays.asList(new HttpCookie("B", "1"),
                                                    new HttpCookie("A", "2"),
                                                    new HttpCookie("G", "3"),
                                                    new HttpCookie("E", "4"),
                                                    new HttpCookie("T", "5"));
    BatchGetEntityRequest<Long, Greeting> req = new CookieRequestBuilders().batchGet().ids(1L, 2L).setCookies(requestCookies).build();
    Response<BatchKVResponse<Long, EntityResponse<Greeting>>> resp = REST_CLIENT.sendRequest(req).getResponse();

    List<HttpCookie> getBackResponseCookie = Arrays.asList(new HttpCookie("1", "B"), new HttpCookie("2", "A"), new HttpCookie("3", "G"),new HttpCookie("4", "E"), new HttpCookie("5", "T"));;
    Assert.assertEquals(resp.getCookies(), getBackResponseCookie);
  }

  /**
   * Test the add cookie functionality
   *
   * @throws RemoteInvocationException
   */
  @Test
  public void testAddCookies() throws RemoteInvocationException
  {
    CookieGetRequestBuilder builderTmp = new CookieRequestBuilders().get().id(1L);
    builderTmp.addCookie(new HttpCookie("C", "3"));
    builderTmp.addCookie(new HttpCookie("B", "2"));
    builderTmp.addCookie(new HttpCookie("A", "1"));

    GetRequest<Greeting> req = builderTmp.build();
    Response<Greeting> resp = REST_CLIENT.sendRequest(req).getResponse();

    List<HttpCookie> expectedCookies = Arrays.asList(new HttpCookie("3", "C"), new HttpCookie("2", "B"), new HttpCookie("1", "A"));
    Assert.assertEquals(resp.getCookies(), expectedCookies);
  }

  /**
   * Test the clear cookie functionality
   *
   * @throws RemoteInvocationException
   */
  @Test
  public void testClearCookies() throws RemoteInvocationException
  {
    List<HttpCookie> requestCookies = Arrays.asList(new HttpCookie("will", "1"), new HttpCookie("lost", "1"));

    GetRequest<Greeting> req = new CookieRequestBuilders().get().id(1L).setCookies(requestCookies).clearCookies().build();
    Response<Greeting> resp = REST_CLIENT.sendRequest(req).getResponse();

    List<String> responseCookies = CookieUtil.encodeSetCookies(resp.getCookies());
    // Since the cookies are cleared, there should be no response from the server
    Assert.assertEquals(CookieUtil.decodeSetCookies(responseCookies), Collections.singletonList(new HttpCookie("empty_name", "empty_cookie")));
  }

}
