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

package com.linkedin.restli.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.linkedin.common.callback.Callback;
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.util.hashing.ConsistentHashKeyMapper;
import com.linkedin.d2.balancer.util.hashing.ConsistentHashRing;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.d2.balancer.util.hashing.StaticRingProvider;
import com.linkedin.data.template.FieldDef;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.ResourceSpecImpl;
import com.linkedin.restli.examples.greetings.api.Greeting;

/**
* @author Zhenkai Zhu
* @version $Revision: $
*/
public class TestAllPartitionsRequestBuilder {
  private static final Client CLIENT = new TransportClientAdapter(new HttpClientFactory().
      getClient(Collections.<String, String>emptyMap()));
  private static final String URI_PREFIX = "http://localhost:1338/";
  private static final RestClient REST_CLIENT = new RestClient(CLIENT, URI_PREFIX);
  private static final String TEST_URI = "test";
  private static final ResourceSpec _COLL_SPEC      =
      new ResourceSpecImpl(EnumSet.allOf(ResourceMethod.class),
          Long.class,
          null,
          null,
          Greeting.class,
          Collections.<String, Class<?>> emptyMap());

  @Test
  public static void testBuildAllPartitionsRequest() throws ServiceUnavailableException, URISyntaxException, RestException
  {

    final int PARTITION_NUM = 10;

    ConsistentHashKeyMapper mapper = getKeyToHostMapper(PARTITION_NUM);

    AllPartitionsRequestBuilder<CollectionResponse<Greeting>> searchRB = new AllPartitionsRequestBuilder<CollectionResponse<Greeting>>(mapper);
    FindRequestBuilder<Long, Greeting> findRB = new FindRequestBuilder<Long, Greeting>(TEST_URI, Greeting.class, _COLL_SPEC);
    Collection<RequestContext> requestContexts =
        (searchRB.buildRequestContexts(findRB.fields(Greeting.fields().message()).build(), new RequestContext())).getPartitionInfo();

    Assert.assertEquals(PARTITION_NUM, requestContexts.size());

    Set<String> expectedHostPrefixes = new HashSet<String>();
    for (int i = 0; i < PARTITION_NUM; i++)
    {
      // one host from each partition
      expectedHostPrefixes.add(TEST_URI + i);
    }

    Set<String> actualHostPrefixes = new HashSet<String>();
    for (RequestContext requestContext : requestContexts)
    {
      URI uri = KeyMapper.TargetHostHints.getRequestContextTargetHost(requestContext);
      String prefix = uri.toString().split("_")[0];
      actualHostPrefixes.add(prefix);
    }

    Assert.assertEquals(expectedHostPrefixes, actualHostPrefixes);
  }

  @Test(groups={"integration"})
  public static void testSendAllPartitionsRequests() throws ServiceUnavailableException, URISyntaxException, RestException, InterruptedException
  {
    final int PARTITION_NUM = 10;
    ConsistentHashKeyMapper mapper = getKeyToHostMapper(PARTITION_NUM);
    AllPartitionsRequestBuilder<Greeting> searchRB = new AllPartitionsRequestBuilder<Greeting>(mapper);
    ActionRequestBuilder<Long, Greeting> builder = new ActionRequestBuilder<Long, Greeting>(TEST_URI, Greeting.class,
        null);
    ActionRequest<Greeting> request = builder.name("action").param(new FieldDef<String>("p", String.class), "42").build();

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

    searchRB.sendRequests(REST_CLIENT, request, new RequestContext(), cb);
    latch.await();
    if (!errors.isEmpty())
    {
      Assert.fail("I knew it: " + errors.toString());
    }

    Assert.assertEquals(PARTITION_NUM, responses.size());
  }

  private static ConsistentHashKeyMapper getKeyToHostMapper(int partitionNum) throws  URISyntaxException
  {
    final int partitionSize = 10;
    List<Map<URI, Integer>> mapList = new ArrayList<Map<URI, Integer>>();

    for (int i = 0; i < partitionNum * partitionSize; i++)
    {
      final int index = i / partitionSize;
      if (index == mapList.size())
      {
        mapList.add(new HashMap<URI, Integer>());
      }
      Map<URI, Integer> map = mapList.get(index);
      map.put(new URI(TEST_URI + index + "_" + i), 100);
    }

    List<Ring<URI>> rings = new ArrayList<Ring<URI>>();
    for (final Map<URI, Integer> map : mapList)
    {
      final ConsistentHashRing<URI> ring = new ConsistentHashRing<URI>(map);
      rings.add(ring);
    }

    return new ConsistentHashKeyMapper(new StaticRingProvider(rings));
  }
}
