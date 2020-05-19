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

package com.linkedin.restli.client;


import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.clients.DynamicClient;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.discovery.event.PropertyEventThread;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.examples.RestLiIntegrationTest;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.GreetingsBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsRequestBuilders;
import com.linkedin.restli.examples.groups.api.Group;
import com.linkedin.restli.examples.groups.api.GroupMembership;
import com.linkedin.restli.examples.groups.client.GroupMembershipsBuilders;
import com.linkedin.restli.examples.groups.client.GroupMembershipsRequestBuilders;
import com.linkedin.restli.examples.groups.client.GroupsBuilders;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import com.linkedin.test.util.retry.SingleRetry;
import java.util.concurrent.CountDownLatch;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class TestRestLiD2Integration extends RestLiIntegrationTest
{
  private SimpleLoadBalancer _loadBalancer;
  private Client _r2Client;
  private RestClient _restClient;

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

  @BeforeTest
  public void init()
  {
    _loadBalancer = MockLBFactory.createLoadBalancer();
    _r2Client = new DynamicClient(_loadBalancer, null);
    _restClient = new RestClient(_r2Client, "d2://");
  }

  @AfterTest
  public void teardown() throws Exception
  {
    final CountDownLatch latch = new CountDownLatch(1);
    _loadBalancer.shutdown(new PropertyEventThread.PropertyEventShutdownCallback()
    {
      @Override
      public void done()
      {
        latch.countDown();
      }
    });
    latch.await();
  }

  @Test(dataProvider = "requestGreetingBuilderDataProvider", retryAnalyzer = SingleRetry.class) // Allow retry due to CI timeouts
  public void testSuccessfulCall(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Request<Greeting> request = builders.get().id(1L).build();
    ResponseFuture<Greeting> future = _restClient.sendRequest(request);
    Greeting g = future.getResponse().getEntity();
    Assert.assertEquals(g.getId().longValue(), 1L);
    Assert.assertNotNull(g.getMessage());
    Assert.assertEquals(future.getResponse().getHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION),
                        AllProtocolVersions.BASELINE_PROTOCOL_VERSION.toString());
  }

  @Test(dataProvider = "requestGroupBuilderDataProvider")
  public void testRemoteInvocationException(RootBuilderWrapper<Integer, Group> builders)
  {
    Request<Group> request = builders.get().id(1).build();
    ResponseFuture<Group> future = _restClient.sendRequest(request);
    try
    {
      future.getResponse();
      Assert.fail("expected RemoteInvocationException");
    }
    catch (RemoteInvocationException e)
    {
      Assert.assertEquals(e.getClass(), RemoteInvocationException.class);
      Assert.assertTrue(e.getMessage().contains("Failed to get response"));
    }
  }

  @Test(dataProvider = "requestGroupMembershipBuilderDataProvider")
  public void testServiceUnavailableException(RootBuilderWrapper<CompoundKey, GroupMembership> builders)
  {
    Request<GroupMembership> request = builders.get().id(new GroupMembershipsBuilders.Key().setMemberId(1).setGroupId(2)).build();
    ResponseFuture<GroupMembership> future = _restClient.sendRequest(request);
    try
    {
      future.getResponse();
      Assert.fail("expected ServiceUnavailableException");
    }
    catch (RemoteInvocationException e)
    {
      Assert.assertTrue(e.getCause() instanceof ServiceUnavailableException);
    }
  }

  @DataProvider
  private static Object[][] requestGreetingBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders()) }
    };
  }

  @DataProvider
  private static Object[][] requestGroupBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<Integer, Group>(new GroupsBuilders()) },
      { new RootBuilderWrapper<Integer, Group>(new GroupsBuilders()) }
    };
  }

  @DataProvider
  private static Object[][] requestGroupMembershipBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<CompoundKey, GroupMembership>(new GroupMembershipsBuilders()) },
      { new RootBuilderWrapper<CompoundKey, GroupMembership>(new GroupMembershipsRequestBuilders()) }
    };
  }
}
