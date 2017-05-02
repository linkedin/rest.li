/*
   Copyright (c) 2017 LinkedIn Corp.

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
package com.linkedin.d2.balancer.clients;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.HdrHistogram.AbstractHistogram;
import org.HdrHistogram.Histogram;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.BackupRequestsConfiguration;
import com.linkedin.d2.BoundedCostBackupRequests;
import com.linkedin.d2.backuprequests.BackupRequestsStrategyStatsConsumer;
import com.linkedin.d2.backuprequests.BackupRequestsStrategyStatsProvider;
import com.linkedin.d2.backuprequests.ConstantResponseTimeDistribution;
import com.linkedin.d2.backuprequests.EventsArrival;
import com.linkedin.d2.backuprequests.GaussianResponseTimeDistribution;
import com.linkedin.d2.backuprequests.GaussianWithHiccupResponseTimeDistribution;
import com.linkedin.d2.backuprequests.LatencyMetric;
import com.linkedin.d2.backuprequests.PoissonEventsArrival;
import com.linkedin.d2.backuprequests.ResponseTimeDistribution;
import com.linkedin.d2.balancer.LoadBalancer;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.util.JacksonUtil;
import com.linkedin.d2.discovery.event.PropertyEventThread.PropertyEventShutdownCallback;
import com.linkedin.data.ByteString;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;


public class TestBackupRequestsClient
{

  private static final String SERVICE_NAME = "testService";
  private static final String CLUSTER_NAME = "testCluster";
  private static final String PATH = "";
  private static final String STRATEGY_NAME = "degrader";
  private static final ByteString CONTENT = ByteString.copy(new byte[8092]);

  private ScheduledExecutorService _executor;

  @BeforeTest
  public void setUp()
  {
    _executor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
  }

  @AfterTest
  public void shutDown()
  {
    _executor.shutdown();
  }

  @Test
  public void testRequest() throws Exception
  {
    AtomicReference<ServiceProperties> serviceProperties = new AtomicReference<>();
    serviceProperties.set(createServiceProperties(null));
    BackupRequestsClient client = createClient(serviceProperties::get);
    URI uri = URI.create("d2://testService");
    RestRequest restRequest = new RestRequestBuilder(uri).setEntity(CONTENT).build();
    Future<RestResponse> response = client.restRequest(restRequest);
    assertEquals(response.get().getStatus(), 200);
  }

  @Test
  public void testBackupRequestsRun() throws Exception
  {
    final AtomicBoolean shutDown = new AtomicBoolean(false);
    final AtomicLong completed = new AtomicLong(0);

    AtomicReference<ServiceProperties> serviceProperties = new AtomicReference<>();
    TestBackupRequestsStrategyStatsConsumer statsConsumer = new TestBackupRequestsStrategyStatsConsumer();
    serviceProperties.set(createServiceProperties(null));
    final BackupRequestsClient client = createClient(serviceProperties::get, statsConsumer);
    final URI uri = URI.create("d2://testService");

    Thread loadGenerator = new Thread(() -> {
      /*
       * Little's theorem: L = a * W
       * W = 10 ms in the test (not including hiccups).
       * We want L to be 100, so a = 100 / 15 = 6.6 events per millisecond
       */
      EventsArrival arrivals = new PoissonEventsArrival(6.6, TimeUnit.MILLISECONDS);
      long lastNano = System.nanoTime();
      while (!shutDown.get())
      {
        long nextNano = lastNano + arrivals.nanosToNextEvent();
        try
        {
          waitUntil(nextNano);
        } catch (Exception e)
        {
          e.printStackTrace();
        }
        RestRequest restRequest = new RestRequestBuilder(uri).setEntity(CONTENT).build();
        RequestContext requestContext = new RequestContext();
        requestContext.putLocalAttr(R2Constants.OPERATION, "get");
        Set<URI> hosts = new HashSet<>();
        hosts.add(uri);
        requestContext.putLocalAttr("D2-Hint-ExcludedHosts", hosts);
        client.restRequest(restRequest, requestContext, new Callback<RestResponse>()
        {
          @Override
          public void onSuccess(RestResponse result)
          {
            completed.incrementAndGet();
          }

          @Override
          public void onError(Throwable e)
          {
          }
        });
        lastNano = nextNano;
      }
    });
    loadGenerator.start();

    Thread.sleep(10000);
    serviceProperties
        .set(createServiceProperties(Arrays.asList(createBackupRequestsConfiguration(5, "get"))));

    long startTime = System.currentTimeMillis();
    while (statsConsumer.getLatencyWithBackup().size() < 1 && System.currentTimeMillis() - startTime < 30000)
    {
      Thread.sleep(10);
    }
    long endTime = System.currentTimeMillis();

    //this should disable backup requests
    serviceProperties
        .set(createServiceProperties(Arrays.asList(createBackupRequestsConfiguration(5, "batch_get"))));

    Thread.sleep((endTime - startTime) * 2);

    //initialize shutdown of load generator
    shutDown.set(true);

    //sum up histograms
    Histogram withoutBackup = new Histogram(LatencyMetric.LOWEST_DISCERNIBLE_VALUE,
        LatencyMetric.HIGHEST_TRACKABLE_VALUE, LatencyMetric.NUMBER_OF_SIGNIFICANT_VALUE_DIGITS);
    Histogram withBackup = new Histogram(LatencyMetric.LOWEST_DISCERNIBLE_VALUE, LatencyMetric.HIGHEST_TRACKABLE_VALUE,
        LatencyMetric.NUMBER_OF_SIGNIFICANT_VALUE_DIGITS);

    statsConsumer.getLatencyWithoutBackup().stream().forEach(h -> {
      withoutBackup.add(h);
    });
    statsConsumer.getLatencyWithBackup().stream().forEach(h -> {
      withBackup.add(h);
    });

    assertEquals(withoutBackup.getTotalCount(), withBackup.getTotalCount());
    double withoutBackup99 = withoutBackup.getValueAtPercentile(99);
    double withBackup99 = withBackup.getValueAtPercentile(99);

    assertTrue(withBackup99 * 10 < withoutBackup99, "99th percentile is expected to be improved 10x, with backup: "
        + withBackup99 / 1000000 + "ms, without backup: " + withoutBackup99 / 1000000 + "ms");

  }

  private static long waitUntil(long nextNano) throws InterruptedException
  {
    long current = System.nanoTime();
    if ((nextNano - current) > 0)
    {
      return waitNano(nextNano, current);
    } else
    {
      return current;
    }
  }

  private static long waitNano(long nextNano, long current) throws InterruptedException
  {
    long waitTime = nextNano - current;
    long millis = (waitTime >> 20) - 1; //2^20ns = 1048576ns ~ 1ms
    if (millis < 0)
    {
      millis = 0;
    }
    if (millis > 0)
    {
      Thread.sleep(millis);
      return waitUntil(nextNano);
    } else
    {
      return busyWaitUntil(nextNano);
    }
  }

  private static long busyWaitUntil(long nextNano)
  {
    long counter = 0L;
    while (true)
    {
      counter += 1;
      if (counter % 1000 == 0)
      {
        long current = System.nanoTime();
        if (current - nextNano >= 0)
        {
          return current;
        }
      }
    }
  }

  @Test
  public void testStatsConsumerAddRemove() throws Exception
  {
    AtomicReference<ServiceProperties> serviceProperties = new AtomicReference<>();
    TestBackupRequestsStrategyStatsConsumer statsConsumer = new TestBackupRequestsStrategyStatsConsumer();
    serviceProperties.set(createServiceProperties(null));
    BackupRequestsClient client = createClient(serviceProperties::get, statsConsumer);
    URI uri = URI.create("d2://testService");
    RestRequest restRequest = new RestRequestBuilder(uri).setEntity(CONTENT).build();
    RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(R2Constants.OPERATION, "get");
    Future<RestResponse> response = client.restRequest(restRequest, requestContext);
    assertEquals(response.get().getStatus(), 200);
    List<StatsConsumerEvent> events = statsConsumer.getEvents();
    assertEquals(events.size(), 0);

    serviceProperties
        .set(createServiceProperties(Arrays.asList(createBackupRequestsConfiguration(5, "get"))));

    requestContext = new RequestContext();
    requestContext.putLocalAttr(R2Constants.OPERATION, "get");
    response = client.restRequest(restRequest, requestContext);
    assertEquals(response.get().getStatus(), 200);
    events = statsConsumer.getEvents();
    assertEquals(events.size(), 1);
    assertEquals(events.get(0).isEventAdd(), true);
    assertEquals(events.get(0).getService(), SERVICE_NAME);
    assertEquals(events.get(0).getOperation(), "get");
    BackupRequestsStrategyStatsProvider statsProvider = events.get(0).getStatsProvider();
    assertNotNull(statsProvider);

    serviceProperties.set(createServiceProperties(null));
    requestContext = new RequestContext();
    requestContext.putLocalAttr(R2Constants.OPERATION, "get");
    response = client.restRequest(restRequest, requestContext);
    assertEquals(response.get().getStatus(), 200);
    events = statsConsumer.getEvents();
    assertEquals(events.size(), 2);
    assertEquals(events.get(1).isEventAdd(), false);
    assertEquals(events.get(1).getService(), SERVICE_NAME);
    assertEquals(events.get(1).getOperation(), "get");
    BackupRequestsStrategyStatsProvider removedStatsProvider = events.get(1).getStatsProvider();
    assertNotNull(removedStatsProvider);
    assertSame(statsProvider, removedStatsProvider);
  }

  @Test
  public void testStatsConsumerLatencyUpdate() throws Exception
  {
    AtomicReference<ServiceProperties> serviceProperties = new AtomicReference<>();
    TestBackupRequestsStrategyStatsConsumer statsConsumer = new TestBackupRequestsStrategyStatsConsumer();
    serviceProperties.set(createServiceProperties(null));

    BackupRequestsClient client = createClient(serviceProperties::get, statsConsumer,
        new ConstantResponseTimeDistribution(1, TimeUnit.NANOSECONDS));
    URI uri = URI.create("d2://testService");
    RestRequest restRequest = new RestRequestBuilder(uri).setEntity(CONTENT).build();
    RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(R2Constants.OPERATION, "get");
    Future<RestResponse> response = client.restRequest(restRequest, requestContext);
    assertEquals(response.get().getStatus(), 200);
    List<StatsConsumerEvent> events = statsConsumer.getEvents();
    assertEquals(events.size(), 0);

    for (int i = 0; i < Short.MAX_VALUE * 4; i++)
    {
      requestContext = new RequestContext();
      requestContext.putLocalAttr(R2Constants.OPERATION, "get");
      response = client.restRequest(restRequest, requestContext);
      assertEquals(response.get().getStatus(), 200);
    }

    assertEquals(statsConsumer.getLatencyWithBackup().size(), 0);
    assertEquals(statsConsumer.getLatencyWithoutBackup().size(), 0);

    serviceProperties
        .set(createServiceProperties(Arrays.asList(createBackupRequestsConfiguration(5, "get"))));

    while (statsConsumer.getLatencyWithoutBackup().size() < 1)
    {
      requestContext = new RequestContext();
      requestContext.putLocalAttr(R2Constants.OPERATION, "get");
      response = client.restRequest(restRequest, requestContext);
      assertEquals(response.get().getStatus(), 200);
    }

    assertEquals(statsConsumer.getLatencyWithoutBackup().size(), 1);
    assertEquals(statsConsumer.getLatencyWithBackup().size(), 1);
    assertEquals(statsConsumer.getLatencyWithBackup().get(0).getTotalCount(),
        statsConsumer.getLatencyWithoutBackup().get(0).getTotalCount());
  }

  @Test
  public void testStatsConsumerRemoveOne() throws Exception
  {
    AtomicReference<ServiceProperties> serviceProperties = new AtomicReference<>();
    TestBackupRequestsStrategyStatsConsumer statsConsumer = new TestBackupRequestsStrategyStatsConsumer();
    serviceProperties.set(createServiceProperties(null));
    BackupRequestsClient client = createClient(serviceProperties::get, statsConsumer);
    URI uri = URI.create("d2://testService");
    RestRequest restRequest = new RestRequestBuilder(uri).setEntity(CONTENT).build();
    RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(R2Constants.OPERATION, "get");
    Future<RestResponse> response = client.restRequest(restRequest, requestContext);
    assertEquals(response.get().getStatus(), 200);
    List<StatsConsumerEvent> events = statsConsumer.getEvents();
    assertEquals(events.size(), 0);

    serviceProperties
        .set(createServiceProperties(Arrays.asList(createBackupRequestsConfiguration(5, "get"),
            createBackupRequestsConfiguration(1, "batch_get"))));

    requestContext = new RequestContext();
    requestContext.putLocalAttr(R2Constants.OPERATION, "get");
    response = client.restRequest(restRequest, requestContext);
    assertEquals(response.get().getStatus(), 200);
    events = statsConsumer.getEvents();
    assertEquals(events.size(), 2);
    assertEquals(events.get(0).isEventAdd(), true);
    assertEquals(events.get(0).getService(), SERVICE_NAME);
    assertEquals(events.get(0).getOperation(), "get");
    BackupRequestsStrategyStatsProvider statsProvider1 = events.get(0).getStatsProvider();
    assertNotNull(statsProvider1);
    assertEquals(events.get(1).isEventAdd(), true);
    assertEquals(events.get(1).getService(), SERVICE_NAME);
    assertEquals(events.get(1).getOperation(), "batch_get");
    BackupRequestsStrategyStatsProvider statsProvider2 = events.get(1).getStatsProvider();
    assertNotNull(statsProvider2);

    serviceProperties
        .set(createServiceProperties(Arrays.asList(createBackupRequestsConfiguration(5, "get"))));
    requestContext = new RequestContext();
    requestContext.putLocalAttr(R2Constants.OPERATION, "get");
    response = client.restRequest(restRequest, requestContext);
    assertEquals(response.get().getStatus(), 200);
    events = statsConsumer.getEvents();
    assertEquals(events.size(), 3);
    assertEquals(events.get(2).isEventAdd(), false);
    assertEquals(events.get(2).getService(), SERVICE_NAME);
    assertEquals(events.get(2).getOperation(), "batch_get");
    BackupRequestsStrategyStatsProvider removedStatsProvider = events.get(2).getStatsProvider();
    assertNotNull(removedStatsProvider);
    assertSame(statsProvider2, removedStatsProvider);
  }

  @Test
  public void testStatsConsumerUpdateAndRemove() throws Exception
  {
    AtomicReference<ServiceProperties> serviceProperties = new AtomicReference<>();
    TestBackupRequestsStrategyStatsConsumer statsConsumer = new TestBackupRequestsStrategyStatsConsumer();
    serviceProperties.set(createServiceProperties(null));
    BackupRequestsClient client = createClient(serviceProperties::get, statsConsumer);
    URI uri = URI.create("d2://testService");
    RestRequest restRequest = new RestRequestBuilder(uri).setEntity(CONTENT).build();
    RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(R2Constants.OPERATION, "get");
    Future<RestResponse> response = client.restRequest(restRequest, requestContext);
    assertEquals(response.get().getStatus(), 200);
    List<StatsConsumerEvent> events = statsConsumer.getEvents();
    assertEquals(events.size(), 0);

    serviceProperties
        .set(createServiceProperties(Arrays.asList(createBackupRequestsConfiguration(5, "get"),
            createBackupRequestsConfiguration(1, "batch_get"))));

    requestContext = new RequestContext();
    requestContext.putLocalAttr(R2Constants.OPERATION, "get");
    response = client.restRequest(restRequest, requestContext);
    assertEquals(response.get().getStatus(), 200);
    events = statsConsumer.getEvents();
    assertEquals(events.size(), 2);
    assertEquals(events.get(0).isEventAdd(), true);
    assertEquals(events.get(0).getService(), SERVICE_NAME);
    assertEquals(events.get(0).getOperation(), "get");
    BackupRequestsStrategyStatsProvider statsProvider1 = events.get(0).getStatsProvider();
    assertNotNull(statsProvider1);
    assertEquals(events.get(1).isEventAdd(), true);
    assertEquals(events.get(1).getService(), SERVICE_NAME);
    assertEquals(events.get(1).getOperation(), "batch_get");
    BackupRequestsStrategyStatsProvider statsProvider2 = events.get(1).getStatsProvider();
    assertNotNull(statsProvider2);

    serviceProperties
        .set(createServiceProperties(Arrays.asList(createBackupRequestsConfiguration(1, "get"))));
    requestContext = new RequestContext();
    requestContext.putLocalAttr(R2Constants.OPERATION, "get");
    response = client.restRequest(restRequest, requestContext);
    assertEquals(response.get().getStatus(), 200);
    events = statsConsumer.getEvents();
    assertEquals(events.size(), 5);
    assertEquals(events.get(2).isEventAdd(), false);
    assertEquals(events.get(2).getService(), SERVICE_NAME);
    assertEquals(events.get(2).getOperation(), "get");
    BackupRequestsStrategyStatsProvider removedStatsProvider = events.get(2).getStatsProvider();
    assertNotNull(removedStatsProvider);
    assertSame(statsProvider1, removedStatsProvider);
    assertEquals(events.get(3).isEventAdd(), true);
    assertEquals(events.get(3).getService(), SERVICE_NAME);
    assertEquals(events.get(3).getOperation(), "get");
    BackupRequestsStrategyStatsProvider statsProvider3 = events.get(3).getStatsProvider();
    assertNotNull(statsProvider1);
    assertNotSame(statsProvider1, statsProvider3);

    assertEquals(events.get(4).isEventAdd(), false);
    assertEquals(events.get(4).getService(), SERVICE_NAME);
    assertEquals(events.get(4).getOperation(), "batch_get");
    BackupRequestsStrategyStatsProvider removedStatsProvider2 = events.get(4).getStatsProvider();
    assertNotNull(removedStatsProvider);
    assertSame(statsProvider2, removedStatsProvider2);
  }

  private ServiceProperties createServiceProperties(List<Map<String, Object>> backupRequests)
  {
    return new ServiceProperties(SERVICE_NAME, CLUSTER_NAME, PATH, Arrays.asList(STRATEGY_NAME),
        Collections.<String, Object> emptyMap(), Collections.<String, Object> emptyMap(),
        Collections.<String, String> emptyMap(), Collections.<String> emptyList(), Collections.<URI> emptySet(),
        Collections.<String, Object> emptyMap(), backupRequests);
  }

  private BackupRequestsClient createClient(Supplier<ServiceProperties> servicePropertiesSupplier)
  {
    return createClient(servicePropertiesSupplier, null);
  }

  private BackupRequestsClient createClient(Supplier<ServiceProperties> servicePropertiesSupplier,
      TestBackupRequestsStrategyStatsConsumer statsConsumer)
  {
    ResponseTimeDistribution hiccupDistribution =
        new GaussianResponseTimeDistribution(500, 1000, 500, TimeUnit.MILLISECONDS);
    ResponseTimeDistribution responseTime =
        new GaussianWithHiccupResponseTimeDistribution(2, 10, 5, TimeUnit.MILLISECONDS, hiccupDistribution, 0.02);

    return createClient(servicePropertiesSupplier, statsConsumer, responseTime);
  }

  private BackupRequestsClient createClient(Supplier<ServiceProperties> servicePropertiesSupplier,
      TestBackupRequestsStrategyStatsConsumer statsConsumer, ResponseTimeDistribution responseTime)
  {
    TestLoadBalancer loadBalancer = new TestLoadBalancer(responseTime, servicePropertiesSupplier);
    DynamicClient dynamicClient = new DynamicClient(loadBalancer, null);
    return new BackupRequestsClient(dynamicClient, loadBalancer, _executor, statsConsumer, 10, TimeUnit.SECONDS);
  }

  class TestLoadBalancer implements LoadBalancer
  {

    private final TransportClient _transportClient;
    private final Supplier<ServiceProperties> _servicePropertiesSupplier;

    public TestLoadBalancer(final ResponseTimeDistribution responseTime,
        Supplier<ServiceProperties> servicePropertiesSupplier)
    {
      _servicePropertiesSupplier = servicePropertiesSupplier;
      _transportClient = new TransportClient()
      {

        @Override
        public void shutdown(Callback<None> callback)
        {
        }

        @Override
        public void restRequest(RestRequest request, RequestContext requestContext, Map<String, String> wireAttrs,
            TransportCallback<RestResponse> callback)
        {
          _executor.schedule(() -> {
            callback.onResponse(TransportResponseImpl.success(new RestResponseBuilder().build(), wireAttrs));
          } , responseTime.responseTimeNanos(), TimeUnit.NANOSECONDS);
        }
      };
    }

    @Override
    public TransportClient getClient(Request request, RequestContext requestContext) throws ServiceUnavailableException
    {
      return _transportClient;
    }

    @Override
    public void start(Callback<None> callback)
    {
    }

    @Override
    public void shutdown(PropertyEventShutdownCallback shutdown)
    {
    }

    @Override
    public ServiceProperties getLoadBalancedServiceProperties(String serviceName) throws ServiceUnavailableException
    {
      return _servicePropertiesSupplier.get();
    }

  }

  @SuppressWarnings("unchecked")
  private final Map<String, Object> createBackupRequestsConfiguration(int cost, String operation)
      throws JsonParseException, JsonMappingException, IOException
  {
    BackupRequestsConfiguration brc = new BackupRequestsConfiguration();
    BoundedCostBackupRequests bcbr = new BoundedCostBackupRequests();
    bcbr.setCost(cost);
    brc.setOperation(operation);
    brc.setStrategy(BackupRequestsConfiguration.Strategy.create(bcbr));
    String json = new JacksonDataCodec().mapToString(brc.data());
    return JacksonUtil.getObjectMapper().readValue(json, Map.class);
  }

  private class TestBackupRequestsStrategyStatsConsumer implements BackupRequestsStrategyStatsConsumer
  {

    private final List<StatsConsumerEvent> _events = new ArrayList<>();
    private final List<AbstractHistogram> _latencyWithBackup = new ArrayList<>();
    private final List<AbstractHistogram> _latencyWithoutBackup = new ArrayList<>();

    @Override
    public synchronized void addStatsProvider(String service, String operation,
        BackupRequestsStrategyStatsProvider statsProvider)
    {
      _events.add(new StatsConsumerEvent(true, service, operation, statsProvider));
    }

    @Override
    public synchronized void removeStatsProvider(String service, String operation,
        BackupRequestsStrategyStatsProvider statsProvider)
    {
      _events.add(new StatsConsumerEvent(false, service, operation, statsProvider));
    }

    public synchronized List<StatsConsumerEvent> getEvents()
    {
      return _events;
    }

    public synchronized List<AbstractHistogram> getLatencyWithBackup()
    {
      return _latencyWithBackup;
    }

    public synchronized List<AbstractHistogram> getLatencyWithoutBackup()
    {
      return _latencyWithoutBackup;
    }

    @Override
    public synchronized void latencyUpdate(String service, String operation, AbstractHistogram histogram,
        boolean withBackup)
    {
      if (withBackup)
      {
        _latencyWithBackup.add(histogram.copy());
      } else
      {
        _latencyWithoutBackup.add(histogram.copy());
      }
    }
  }

  private class StatsConsumerEvent
  {
    final boolean _isEventAdd;
    final String _service;
    final String _operation;
    final BackupRequestsStrategyStatsProvider _statsProvider;

    public StatsConsumerEvent(boolean isEventAdd, String service, String operation,
        BackupRequestsStrategyStatsProvider statsProvider)
    {
      _isEventAdd = isEventAdd;
      _service = service;
      this._operation = operation;
      _statsProvider = statsProvider;
    }

    public boolean isEventAdd()
    {
      return _isEventAdd;
    }

    public String getService()
    {
      return _service;
    }

    public String getOperation()
    {
      return _operation;
    }

    public BackupRequestsStrategyStatsProvider getStatsProvider()
    {
      return _statsProvider;
    }

    @Override
    public String toString()
    {
      return "StatsConsumerEvent [isEventAdd=" + _isEventAdd + ", service=" + _service + ", operation=" + _operation
          + ", statsProvider:" + _statsProvider.getStats() + "]";
    }

  }

}
