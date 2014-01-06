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


import com.linkedin.common.callback.Callback;
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.util.hashing.ConsistentHashKeyMapper;
import com.linkedin.d2.balancer.util.hashing.ConsistentHashRing;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.d2.balancer.util.hashing.StaticRingProvider;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.client.uribuilders.RestliUriBuilderUtil;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionRequest;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.examples.RestLiIntegrationTest;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.GreetingsBatchDeleteBuilder;
import com.linkedin.restli.examples.greetings.client.GreetingsBatchGetBuilder;
import com.linkedin.restli.examples.greetings.client.GreetingsBatchUpdateBuilder;
import com.linkedin.restli.examples.greetings.client.GreetingsBuilders;
import com.linkedin.restli.internal.client.CollectionRequestUtil;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class TestScatterGather extends RestLiIntegrationTest
{
  private static final Client CLIENT = new TransportClientAdapter(new HttpClientFactory().getClient(
            Collections.<String, String>emptyMap()));
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

  @Test
  public static void testBuildSGRequests() throws URISyntaxException, RestException, ServiceUnavailableException
  {
    testBuildSGRequests(10, 0);
  }

  @Test
  public static void testBuildSGRequestsWithPartitions() throws URISyntaxException, RestException, ServiceUnavailableException
  {
    testBuildSGRequests(12, 3);
  }

  public static void testBuildSGRequests(int endPointsNum, int partitionNum) throws URISyntaxException, RestException, ServiceUnavailableException
  {
    final int NUM_ENDPOINTS = endPointsNum;
    ConsistentHashKeyMapper mapper;
    if (partitionNum > 0)
    {
      mapper = getKeyToHostMapper(endPointsNum, partitionNum);
    }
    else
    {
      mapper = getKeyToHostMapper(endPointsNum);
    }
    ScatterGatherBuilder<Greeting> sg = new ScatterGatherBuilder<Greeting>(mapper);

    final int NUM_IDS = 100;
    Long[] ids = generateIds(NUM_IDS);
    Map<Long, Greeting> updates = generateUpdates(ids);
    testBuildSGGetRequests(NUM_ENDPOINTS, sg, ids);
    testBuildSGDeleteRequests(NUM_ENDPOINTS, sg, ids);
    testBuildSGUpdateRequests(NUM_ENDPOINTS, sg, updates);
  }

  private static void testBuildSGDeleteRequests(int numEndpoints,
                                                ScatterGatherBuilder<Greeting> sg,
                                                Long[] ids)
    throws ServiceUnavailableException
  {
    Collection<ScatterGatherBuilder.KVRequestInfo<Long ,UpdateStatus>> requests = buildScatterGatherDeleteRequests(sg, ids);
    Assert.assertEquals(requests.size(), numEndpoints);

    Set<Set<String>> requestIdSets = new HashSet<Set<String>>();
    Set<Long> requestIds = new HashSet<Long>();
    for (ScatterGatherBuilder.KVRequestInfo<Long, UpdateStatus> requestInfo : requests)
    {
      BatchRequest<BatchKVResponse<Long, UpdateStatus>> request = requestInfo.getRequest();
      Set<String> expectedParams = new HashSet<String>();
      expectedParams.add("ids");

      testRequest(request, expectedParams, null, null, requestIdSets, requestIds);
    }
    Assert.assertTrue(requestIds.containsAll(Arrays.asList(ids)));
    Assert.assertEquals(requestIds.size(), ids.length);
  }

  private static void testBuildSGUpdateRequests(int numEndpoints,
                                                ScatterGatherBuilder<Greeting> sg,
                                                Map<Long, Greeting> greetingMap)
    throws ServiceUnavailableException
  {
    Collection<ScatterGatherBuilder.KVRequestInfo<Long, UpdateStatus>> requests = buildScatterGatherUpdateRequests(
      sg, greetingMap);
    Assert.assertEquals(requests.size(), numEndpoints);

    Set<Set<String>> requestIdSets = new HashSet<Set<String>>();
    Set<Long> requestIds = new HashSet<Long>();
    for (ScatterGatherBuilder.KVRequestInfo<Long, UpdateStatus> requestInfo : requests)
    {
      BatchRequest<BatchKVResponse<Long,UpdateStatus>> request = requestInfo.getRequest();
      Set<String> expectedParams = new HashSet<String>();
      expectedParams.add("ids");

      testRequest(request, expectedParams, null, greetingMap, requestIdSets, requestIds);
    }
    Set<Long> ids = greetingMap.keySet();
    Assert.assertTrue(requestIds.containsAll(ids));
    Assert.assertEquals(requestIds.size(), ids.size());
  }

  private static void testBuildSGGetRequests(int numEndpoints,
                                             ScatterGatherBuilder<Greeting> sg,
                                             Long[] ids)
            throws ServiceUnavailableException
  {
    Collection<ScatterGatherBuilder.RequestInfo<Greeting>> requests = buildScatterGatherGetRequests(
      sg, ids);
    Assert.assertEquals(requests.size(), numEndpoints);

    Set<Set<String>> requestIdSets = new HashSet<Set<String>>();
    Set<Long> requestIds = new HashSet<Long>();
    for (ScatterGatherBuilder.RequestInfo<Greeting> requestInfo : requests)
    {
      //URI will be something like "greetings/?ids=21&ids=4&ids=53&ids=60&ids=66&ids=88&ids=93"
      BatchRequest<BatchResponse<Greeting>> request = requestInfo.getBatchRequest();
      Set<String> expectedParams = new HashSet<String>();
      expectedParams.add("ids");
      expectedParams.add("fields");
      Set<String> expectedFields = Collections.singleton("message");

      testRequest(request, expectedParams, expectedFields, null, requestIdSets, requestIds);
    }
    Assert.assertTrue(requestIds.containsAll(Arrays.asList(ids)));
    Assert.assertEquals(requestIds.size(), ids.length);
  }

  @SuppressWarnings("unchecked")
  private static void testRequest(BatchRequest<?> request,
                                  Set<String> expectedParams,
                                  Set<String> expectedFields,
                                  Map<Long, Greeting> expectedInput,
                                  Set<Set<String>> requestIdSets,
                                  Set<Long> requestIds)
  {
    String[] queryParams = RestliUriBuilderUtil.createUriBuilder(request).build().getQuery().split("&");
    Map<String, List<String>> params = new HashMap<String, List<String>>();
    for (String paramString : queryParams)
    {
      String[] keyValue = paramString.split("=");
      Assert.assertEquals(keyValue.length, 2);
      if (! params.containsKey(keyValue[0]))
      {
        params.put(keyValue[0], new ArrayList<String>());
      }
      params.get(keyValue[0]).add(keyValue[1]);
    }
    Assert.assertEquals(params.keySet(), expectedParams);

    if (expectedFields != null)
    {
      Assert.assertTrue(params.get("fields").containsAll(expectedFields));
    }

    Set<String> uriIds = new HashSet<String>();
    for (String value : params.get("ids"))
    {
      uriIds.addAll(Arrays.asList(value.split(",")));
    }

    if (expectedInput != null)
    {
      RecordTemplate inputRecordTemplate;
      if (request instanceof BatchUpdateRequest)
      {
        ResourceSpec resourceSpec = request.getResourceSpec();

        CollectionRequest inputRecord = (CollectionRequest)request.getInputRecord();

        inputRecordTemplate = CollectionRequestUtil.convertToBatchRequest(inputRecord,
                                                                          resourceSpec.getKeyClass(),
                                                                          resourceSpec.getKeyKeyClass(),
                                                                          resourceSpec.getKeyParamsClass(),
                                                                          resourceSpec.getKeyParts(),
                                                                          resourceSpec.getValueClass());
      }
      else
      {
        inputRecordTemplate = request.getInputRecord();
      }
      checkInput(inputRecordTemplate.data().getDataMap(com.linkedin.restli.common.BatchRequest.ENTITIES),
                 expectedInput,
                 uriIds);
    }

    @SuppressWarnings("unchecked")
    Set<Object> idObjects = request.getObjectIds();
    Set<String> theseIds = new HashSet<String>(idObjects.size());
    for (Object o : idObjects)
    {
      theseIds.add(o.toString());
    }

    Assert.assertEquals(uriIds, theseIds);

    Assert.assertFalse(requestIdSets.contains(theseIds)); //no duplicate requests
    for (String id : theseIds)
    {
      Assert.assertFalse(requestIds.contains(Long.parseLong(id))); //no duplicate ids
      requestIds.add(Long.parseLong(id));
    }
    requestIdSets.add(theseIds);
  }

  // TODO modify this method to accept a CollectionRequest as it's first parameter once our server code has been
  //      updated to work with the new representation of BatchUpdateRequests and BatchPartialUpdateRequests. As of now
  //      we are still converting to the old representation using CollectionRequestUtil.convertToBatchRequest
  private static void checkInput(DataMap dataMap, Map<Long, Greeting> inputMap, Set<String> uriIds)
  {
    Assert.assertEquals(dataMap.size(), uriIds.size());

    for(String key : dataMap.keySet())
    {
      DataMap inputDM = dataMap.getDataMap(key);
      Greeting expectedGreeting = inputMap.get(Long.parseLong(key));
      Assert.assertTrue(uriIds.contains(key));
      Assert.assertTrue(inputDM.equals(expectedGreeting.data()));
    }
  }

  @Test
  public static void testSendSGRequests()
          throws URISyntaxException, InterruptedException, ServiceUnavailableException
  {
    final int NUM_ENDPOINTS = 4;
    ConsistentHashKeyMapper mapper = getKeyToHostMapper(NUM_ENDPOINTS);
    ScatterGatherBuilder<Greeting> sg = new ScatterGatherBuilder<Greeting>(mapper);

    final int NUM_IDS = 20;
    Long[] requestIds = generateIds(NUM_IDS);
    Map<Long, Greeting> input = generateUpdates(requestIds);
    testSendGetSGRequests(sg, requestIds);
    testSendSGDeleteRequests(sg, requestIds);
    testSendSGUpdateRequests(sg, input);
  }

  private static void testSendGetSGRequests(ScatterGatherBuilder<Greeting> sg,
                                            Long[] requestIds)
    throws ServiceUnavailableException, InterruptedException
  {
    Collection<ScatterGatherBuilder.RequestInfo<Greeting>> scatterGatherRequests =
      buildScatterGatherGetRequests(sg, requestIds);

    final Map<String, Greeting> results = new ConcurrentHashMap<String, Greeting>();
    final CountDownLatch latch = new CountDownLatch(scatterGatherRequests.size());
    final List<Throwable> errors = new ArrayList<Throwable>();

    final List<BatchResponse<Greeting>> responses = new ArrayList<BatchResponse<Greeting>>();
    for (ScatterGatherBuilder.RequestInfo<Greeting> requestInfo : scatterGatherRequests)
    {
      Callback<Response<BatchResponse<Greeting>>> cb = new Callback<Response<BatchResponse<Greeting>>>()
      {
        @Override
        public void onSuccess(Response<BatchResponse<Greeting>> response)
        {
          results.putAll(response.getEntity().getResults());
          synchronized (responses)
          {
            responses.add(response.getEntity());
          }
          latch.countDown();
        }

        @Override
        public void onError(Throwable e)
        {
          synchronized (errors)
          {
            errors.add(e);
          }
          latch.countDown();
        }
      };

      REST_CLIENT.sendRequest(requestInfo.getRequest(), requestInfo.getRequestContext(), cb);
    }
    latch.await();

    if (!errors.isEmpty())
    {
      Assert.fail("Errors in scatter/gather: " + errors.toString());
    }

    Assert.assertEquals(results.values().size(), requestIds.length);

    Set<Set<String>> responseIdSets = new HashSet<Set<String>>();
    Set<Long> responseIds = new HashSet<Long>();
    for (BatchResponse<Greeting> response : responses)
    {
      Set<String> theseIds = response.getResults().keySet();
      Assert.assertFalse(responseIdSets.contains(theseIds)); //no duplicate requests
      for (String id : theseIds)
      {
        Assert.assertFalse(responseIds.contains(Long.parseLong(id))); //no duplicate ids
        responseIds.add(Long.parseLong(id));
      }
      responseIdSets.add(theseIds);
    }
    Assert.assertTrue(responseIds.containsAll(Arrays.asList(requestIds)));
    Assert.assertEquals(responseIds.size(), requestIds.length);
  }

  public static void testSendSGDeleteRequests(ScatterGatherBuilder<Greeting> sg, Long[] requestIds)
    throws ServiceUnavailableException, InterruptedException
  {
    Collection<ScatterGatherBuilder.KVRequestInfo<Long, UpdateStatus>> scatterGatherRequests =
      buildScatterGatherDeleteRequests(sg, requestIds);

    testSendSGKVRequests(scatterGatherRequests, requestIds);
  }

  public static void testSendSGUpdateRequests(ScatterGatherBuilder<Greeting> sg, Map<Long, Greeting> input)
    throws ServiceUnavailableException, InterruptedException
  {
    Collection<ScatterGatherBuilder.KVRequestInfo<Long, UpdateStatus>> scatterGatherRequests =
      buildScatterGatherUpdateRequests(sg, input);

    testSendSGKVRequests(scatterGatherRequests, input.keySet().toArray(new Long[input.size()]));
  }

  private static void testSendSGKVRequests(Collection<ScatterGatherBuilder.KVRequestInfo<Long, UpdateStatus>> scatterGatherRequests,
                                           Long[] requestIds) throws InterruptedException
  {
    final Map<Long, UpdateStatus> results = new ConcurrentHashMap<Long, UpdateStatus>();
    final CountDownLatch latch = new CountDownLatch(scatterGatherRequests.size());
    final List<Throwable> errors = new ArrayList<Throwable>();

    final List<BatchKVResponse<Long, UpdateStatus>> responses = new ArrayList<BatchKVResponse<Long, UpdateStatus>>();
    for (ScatterGatherBuilder.KVRequestInfo<Long, UpdateStatus> requestInfo : scatterGatherRequests)
    {
      Callback<Response<BatchKVResponse<Long, UpdateStatus>>> cb = new Callback<Response<BatchKVResponse<Long, UpdateStatus>>>()
      {
        @Override
        public void onSuccess(Response<BatchKVResponse<Long, UpdateStatus>> response)
        {
          results.putAll(response.getEntity().getResults());
          synchronized (responses)
          {
            responses.add(response.getEntity());
          }
          latch.countDown();
        }

        @Override
        public void onError(Throwable e)
        {
          synchronized (errors)
          {
            errors.add(e);
          }
          latch.countDown();
        }
      };

      BatchRequest<BatchKVResponse<Long, UpdateStatus>> request = requestInfo.getRequest();
      RequestContext requestContext = requestInfo.getRequestContext();
      REST_CLIENT.sendRequest(request, requestContext, cb);
    }
    latch.await();

    if (!errors.isEmpty())
    {
      Assert.fail("Errors in scatter/gather: " + errors.toString());
    }

    Assert.assertEquals(results.values().size(), requestIds.length);

    Set<Set<Long>> responseIdSets = new HashSet<Set<Long>>();
    Set<Long> responseIds = new HashSet<Long>();
    for (BatchKVResponse<Long, UpdateStatus> response : responses)
    {
      Set<Long> theseIds = response.getResults().keySet();
      Assert.assertFalse(responseIdSets.contains(theseIds)); //no duplicate requests
      for (Long id : theseIds)
      {
        Assert.assertFalse(responseIds.contains(id)); //no duplicate ids
        responseIds.add(id);
      }
      responseIdSets.add(theseIds);
    }
    Assert.assertTrue(responseIds.containsAll(Arrays.asList(requestIds)));
    Assert.assertEquals(responseIds.size(), requestIds.length);
  }

  //@Test
  public static void testScatterGatherLoadBalancerIntegration() throws Exception
  {
    SimpleLoadBalancer loadBalancer = MockLBFactory.createLoadBalancer();

    KeyMapper keyMapper = new ConsistentHashKeyMapper(loadBalancer);

    try
    {
      @SuppressWarnings("deprecation")
      Map<URI, Set<String>> result = keyMapper.mapKeys(URI.create("http://badurischeme/"), new HashSet<String>());
      Assert.fail("keyMapper should reject non-D2 URI scheme");
    }
    catch (IllegalArgumentException e)
    {
      //expected
    }

    ScatterGatherBuilder<Greeting> sg = new ScatterGatherBuilder<Greeting>(keyMapper);

    final int NUM_IDS = 20;
    Long[] requestIds = generateIds(NUM_IDS);
    Collection<ScatterGatherBuilder.RequestInfo<Greeting>> scatterGatherRequests =
      buildScatterGatherGetRequests(sg, requestIds);
  }

  private static Collection<ScatterGatherBuilder.RequestInfo<Greeting>> buildScatterGatherGetRequests(
    ScatterGatherBuilder<Greeting> sg,
    Long[] ids)
          throws ServiceUnavailableException
  {
    GreetingsBatchGetBuilder greetingsRB = new GreetingsBuilders().batchGet().ids(ids);

    return sg.buildRequestsV2(greetingsRB.fields(Greeting.fields().message()).build(), new RequestContext()).getRequestInfo();
  }

  private static Collection<ScatterGatherBuilder.KVRequestInfo<Long, UpdateStatus>> buildScatterGatherDeleteRequests(
    ScatterGatherBuilder<Greeting> sg,
    Long[] ids)
          throws ServiceUnavailableException
  {
    GreetingsBatchDeleteBuilder greetingsRB = new GreetingsBuilders().batchDelete().ids(ids);

    return sg.buildRequests(greetingsRB.build(), new RequestContext()).getRequestInfo();
  }

  private static Collection<ScatterGatherBuilder.KVRequestInfo<Long, UpdateStatus>> buildScatterGatherUpdateRequests(
    ScatterGatherBuilder<Greeting> sg,
    Map<Long, Greeting> inputs)
          throws ServiceUnavailableException
  {
    GreetingsBatchUpdateBuilder greetingsRB = new GreetingsBuilders().batchUpdate().inputs(inputs);

    return sg.buildRequests(greetingsRB.build(), new RequestContext()).getRequestInfo();
  }

  private static Long[] generateIds(int n)
  {
    Long[] ids = new Long[n];
    for (int ii=0; ii<n; ++ii)
    {
      ids[ii] = (long)ii+1; //GreetingsResource is 1-indexed
    }
    return ids;
  }

  private static Map<Long, Greeting> generateUpdates(Long[] ids)
  {
    Map<Long, Greeting> updates = new HashMap<Long, Greeting>();
    for (long l : ids)
    {
      Greeting greeting = new Greeting();
      greeting.setId(l).setMessage("update message").setTone(Tone.SINCERE);
      updates.put(l,greeting);
    }
    return updates;
  }

  private static ConsistentHashKeyMapper getKeyToHostMapper(int n) throws URISyntaxException
  {
    Map<URI, Integer> endpoints = new HashMap<URI, Integer>();
    for (int ii=0; ii<n; ++ii)
    {
      endpoints.put(new URI("test" + String.valueOf(ii)), 100);
    }

    ConsistentHashRing<URI> testRing = new ConsistentHashRing<URI>(endpoints);
    ConsistentHashKeyMapper mapper = new ConsistentHashKeyMapper(new StaticRingProvider(testRing));

    return mapper;
  }

  private static ConsistentHashKeyMapper getKeyToHostMapper(int n, int partitionNum) throws  URISyntaxException
  {
    Map<URI, Integer> endpoints = new HashMap<URI, Integer>();
    for (int ii=0; ii<n; ++ii)
    {
      endpoints.put(new URI("test" + String.valueOf(ii)), 100);
    }

    final int partitionSize = endpoints.size() / partitionNum;
    List<Map<URI, Integer>> mapList = new ArrayList<Map<URI, Integer>>();
    int count = 0;
    for(final URI uri : endpoints.keySet())
    {
      final int index = count / partitionSize;
      if (index == mapList.size())
      {
        mapList.add(new HashMap<URI, Integer>());
      }
      Map<URI, Integer> map = mapList.get(index);
      map.put(uri, endpoints.get(uri));
      count++;
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
