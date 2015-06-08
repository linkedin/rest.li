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

import com.linkedin.d2.balancer.LoadBalancerState;
import com.linkedin.d2.balancer.PartitionedLoadBalancerTestState;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.util.HostSet;
import com.linkedin.d2.balancer.util.hashing.ConsistentHashKeyMapperTest;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessor;
import com.linkedin.restli.client.RestliRequestOptions;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.linkedin.common.callback.Callback;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.util.hashing.ConsistentHashKeyMapper;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.data.template.FieldDef;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.restli.client.ActionRequest;
import com.linkedin.restli.client.ActionRequestBuilder;
import com.linkedin.restli.client.AllPartitionsRequestBuilder;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.ResourceSpecImpl;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;

/**
* @author Zhenkai Zhu
* @version $Revision: $
*/
public class TestAllPartitionsRequestBuilder extends RestLiIntegrationTest
{
  private static final String TEST_URI = "greetings";
  private static final ResourceSpec _COLL_SPEC      =
      new ResourceSpecImpl(EnumSet.allOf(ResourceMethod.class),
          Collections.<String, DynamicRecordMetadata> emptyMap(),
          Collections.<String, DynamicRecordMetadata> emptyMap(),
          Long.class,
          null,
          null,
          Greeting.class,
          Collections.<String, Class<?>> emptyMap());

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

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "restliRequestOptions")
  public void testSendAllPartitionsRequests(RestliRequestOptions options) throws ServiceUnavailableException, URISyntaxException, RestException, InterruptedException
  {
    final int PARTITION_NUM = 5;
    List<URI> expectedUris = new ArrayList<URI>();
    ConsistentHashKeyMapper mapper = getKeyToHostMapper(PARTITION_NUM, expectedUris);
    AllPartitionsRequestBuilder<Greeting> searchRB = new AllPartitionsRequestBuilder<Greeting>(mapper);
    ActionRequestBuilder<Long, Greeting> builder = new ActionRequestBuilder<Long, Greeting>(TEST_URI,
                                                                                            Greeting.class,
                                                                                            _COLL_SPEC,
                                                                                            options);
    ActionRequest<Greeting> request = builder.name("updateTone").id(1L).
        setParam(new FieldDef<Tone>("newTone", Tone.class, DataTemplateUtil.getSchema(Tone.class)), Tone.FRIENDLY).build();

    final Map<String, Greeting> results = new ConcurrentHashMap<String, Greeting>();
    final CountDownLatch latch = new CountDownLatch(PARTITION_NUM);
    final List<Throwable> errors = new ArrayList<Throwable>();
    final List<Greeting> responses = new ArrayList<Greeting>();

    Callback<Response<Greeting>> cb = new Callback<Response<Greeting>>()
    {
      @Override
      public void onError(Throwable e)
      {
        synchronized (errors)
        {
          errors.add(e);
        }
        latch.countDown();
      }

      @Override
      public void onSuccess(Response<Greeting> response)
      {
        results.put(response.getEntity().toString(), response.getEntity());
        synchronized (responses)
        {
          responses.add(response.getEntity());
        }
        latch.countDown();
      }
    };

    HostSet hostsResult = searchRB.sendRequests(getClient(), request, new RequestContext(), cb);

    List<URI> uris = hostsResult.getAllHosts();
    Assert.assertTrue(uris.containsAll(expectedUris));
    Assert.assertTrue(expectedUris.containsAll(uris));

    latch.await();
    if (!errors.isEmpty())
    {
      Assert.fail("I knew it: " + errors.toString());
    }

    Assert.assertEquals(PARTITION_NUM, responses.size());
  }

  private ConsistentHashKeyMapper getKeyToHostMapper(int partitionNum, List<URI> expectedUris) throws  URISyntaxException
  {
    Map<URI, Map<Integer, PartitionData>> partitionDescriptions = new HashMap<URI, Map<Integer, PartitionData>>();

    for (int i = 0; i < partitionNum; i++)
    {
      final URI foo = new URI("http://foo" + i + ".com");
      expectedUris.add(foo);
      Map<Integer, PartitionData> foo1Data = new HashMap<Integer, PartitionData>();
      foo1Data.put(i, new PartitionData(1.0));
      partitionDescriptions.put(foo, foo1Data);
    }

    List<LoadBalancerState.SchemeStrategyPair> orderedStrategies = new ArrayList<LoadBalancerState.SchemeStrategyPair>();
    LoadBalancerStrategy strategy = new ConsistentHashKeyMapperTest.TestLoadBalancerStrategy(partitionDescriptions);
    orderedStrategies.add(new LoadBalancerState.SchemeStrategyPair("http", strategy));

    PartitionAccessor accessor = new ConsistentHashKeyMapperTest.TestPartitionAccessor();

    SimpleLoadBalancer balancer = new SimpleLoadBalancer(new PartitionedLoadBalancerTestState(
            "clusterName", "serviceName", "path", "strategyName", partitionDescriptions, orderedStrategies,
            accessor
    ));

    return new ConsistentHashKeyMapper(balancer, balancer);
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "restliRequestOptions")
  public Object[][] restliRequestOptions()
  {
    return new Object[][]
      {
        { RestliRequestOptions.DEFAULT_OPTIONS },
        { TestConstants.FORCE_USE_NEXT_OPTIONS }
      };
  }
}
