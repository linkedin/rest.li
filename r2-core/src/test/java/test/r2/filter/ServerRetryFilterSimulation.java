package test.r2.filter;

import com.linkedin.r2.RetriableRequestException;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.filter.transport.ServerRetryFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.testutils.filter.FilterUtil;
import com.linkedin.r2.transport.http.common.HttpConstants;
import com.linkedin.test.util.ClockedExecutor;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


public class ServerRetryFilterSimulation {
  private static final String EXCLUDED_HOSTS = "excluded";
  private static final String RETRY_MESSAGE = "this is a retry";

  private final ClockedExecutor _clockedExecutor = new ClockedExecutor();

  private DistributionNonDiscreteRing<MockServer> _ring;
  private final int _retryLimit;
  private final double _maxRequestRetryRatio;
  private final long _updateIntervalMs;
  private final long _numIntervals;
  private final Map<MockServer, Integer> _weightedServers;

  public ServerRetryFilterSimulation(int retryLimit, double maxRequestRetryRatio, long updateIntervalMs, int numIntervals)
  {
    _retryLimit = retryLimit;
    _maxRequestRetryRatio = maxRequestRetryRatio;
    _updateIntervalMs = updateIntervalMs;
    _numIntervals = numIntervals;
    _weightedServers = new HashMap<>();
  }

  private MockServer createMockServer(int id, boolean isOverloaded)
  {
    CaptureFilter captureFilter = new CaptureFilter(id);
    ServerRetryFilter serverRetryFilter = new ServerRetryFilter(_clockedExecutor, _retryLimit, _maxRequestRetryRatio, _updateIntervalMs, 5);
    FilterChain fc = FilterChains.createRestChain(captureFilter, serverRetryFilter);

    return new MockServer(id, fc, captureFilter, isOverloaded);
  }

  private void run(int[] serverWeights, boolean[] overloadStatus, int requestCount)
  {
    for (int i = 0; i < serverWeights.length; i++)
    {
      int weight = serverWeights[i];
      boolean isOverloaded = overloadStatus[i];
      MockServer server = createMockServer(i, isOverloaded);
      _weightedServers.put(server, weight);
    }

    _ring = new DistributionNonDiscreteRing<>(_weightedServers);

    runWait(requestCount);

    for (MockServer server : _weightedServers.keySet())
    {
      System.out.println(String.format("Server ID: %s\nTotal Response Count: %s\nRetry Response Count: %s\nOverload Count: %s\n",
          server.getId(), server.getTotalResponseCountList(), server.getRetryResponseCountList(), server.getOverloadCountList()));
    }
  }

  private void runWait(int requestCount)
  {
    Future<Void> running = runRequests(requestCount);
    if (running != null)
    {
      try
      {
        running.get();
      }
      catch (InterruptedException | ExecutionException e)
      {
        throw new RuntimeException(e);
      }
    }
  }

  private Future<Void> runRequests(int requestCount)
  {
    _clockedExecutor.scheduleWithFixedDelay(() ->
        {
          runInterval(requestCount, _updateIntervalMs / requestCount);
          for (MockServer server: _weightedServers.keySet())
          {
            server.reset();
          }
        },
        0, _updateIntervalMs, TimeUnit.MILLISECONDS);
    return _clockedExecutor.runFor(_updateIntervalMs * _numIntervals);
  }

  private void runInterval(int requestCount, long constantLatency)
  {
    for (int i = 0; i < requestCount; i++)
    {
      _clockedExecutor.schedule(() ->
      {
        RequestContext rc = new RequestContext();
        Map<String, String> wireAttrs = new HashMap<>();
        _ring.get(0).onRequest(rc, wireAttrs, 0);
        handleResponse(rc, wireAttrs);
      }, constantLatency, TimeUnit.MILLISECONDS);
    }
  }

  @SuppressWarnings("unchecked")
  private void handleResponse(RequestContext rc, Map<String, String> wireAttrs)
  {
    if (wireAttrs.containsKey(R2Constants.RETRY_MESSAGE_ATTRIBUTE_KEY))
    {
      Set<Integer> excludedHosts = (Set<Integer>) rc.getLocalAttr(EXCLUDED_HOSTS);
      if (excludedHosts != null)
      {
        Iterator<MockServer> iter = _ring.getIterator(0);

        while (iter.hasNext())
        {
          MockServer server = iter.next();

          if (!excludedHosts.contains(server.getId()))
          {
            server.onRequest(rc, wireAttrs, excludedHosts.size());
            handleResponse(rc, wireAttrs);
            break;
          }
        }
      }
    }
  }

  private static class CaptureFilter implements RestFilter
  {
    private final Object _lock = new Object();
    private final int _id;

    private int _totalResponseCount;
    private int _overloadCount;
    private int _retryResponseCount;

    private List<Integer> _totalResponseCountList;
    private List<Integer> _overloadCountList;
    private List<Integer> _retryResponseCountList;

    public CaptureFilter(int id)
    {
      _id = id;
      _totalResponseCountList = new ArrayList<>();
      _overloadCountList = new ArrayList<>();
      _retryResponseCountList = new ArrayList<>();
    }

    @Override
    public void onRestResponse(RestResponse res,
        RequestContext requestContext,
        Map<String, String> wireAttrs,
        NextFilter<RestRequest, RestResponse> nextFilter)
    {
      synchronized (_lock)
      {
        _totalResponseCount += 1;
      }
      requestContext.removeLocalAttr(EXCLUDED_HOSTS);
      nextFilter.onResponse(res, requestContext, wireAttrs);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onRestError(Throwable ex,
        RequestContext requestContext,
        Map<String, String> wireAttrs,
        NextFilter<RestRequest, RestResponse> nextFilter)
    {
      synchronized (_lock)
      {
        if (wireAttrs.containsKey(R2Constants.RETRY_MESSAGE_ATTRIBUTE_KEY))
        {
          _retryResponseCount += 1;
        }

        _overloadCount += 1;
        _totalResponseCount += 1;
      }

      Set<Integer> excludedHosts = (Set<Integer>) requestContext.getLocalAttr(EXCLUDED_HOSTS);
      if (excludedHosts == null)
      {
        excludedHosts = new HashSet<>();
        requestContext.putLocalAttr(EXCLUDED_HOSTS, excludedHosts);
      }
      excludedHosts.add(_id);
      nextFilter.onError(ex, requestContext, wireAttrs);
    }

    public void reset()
    {
      _totalResponseCountList.add(_totalResponseCount);
      _retryResponseCountList.add(_retryResponseCount);
      _overloadCountList.add(_overloadCount);

      _totalResponseCount = 0;
      _retryResponseCount = 0;
      _overloadCount = 0;
    }

    public List<Integer> getTotalResponseCountList() {
      return _totalResponseCountList;
    }

    public List<Integer> getRetryResponseCountList() {
      return _retryResponseCountList;
    }

    public List<Integer> getOverloadCountList() {
      return _overloadCountList;
    }
  }

  private static class MockServer
  {
    private static final RestResponse MY_RESPONSE = new RestResponseBuilder().setStatus(200).setEntity("123".getBytes()).build();
    private int _id;
    private FilterChain _fc;
    private CaptureFilter _captureFilter;
    private boolean _isOverloaded;

    private MockServer(int id, FilterChain fc, CaptureFilter captureFilter, boolean isOverloaded)
    {
      _id = id;
      _fc = fc;
      _captureFilter = captureFilter;
      _isOverloaded = isOverloaded;
    }

    public FilterChain getFilterChain() {
      return _fc;
    }

    public int getId() {
      return _id;
    }

    public void onRequest(RequestContext rc, Map<String, String> wireAttrs, int numberOfRetryAttempts)
    {
      FilterUtil.fireRestRequest(_fc, new RestRequestBuilder(URI.create("/req"))
          .addHeaderValue(HttpConstants.HEADER_NUMBER_OF_RETRY_ATTEMPTS, Integer.toString(numberOfRetryAttempts))
          .build(), rc, wireAttrs);
      if (_isOverloaded)
      {
        FilterUtil.fireRestError(_fc, new RestException(null, new RetriableRequestException(RETRY_MESSAGE)), rc, wireAttrs);
      }
      else
      {
        FilterUtil.fireRestResponse(_fc, MY_RESPONSE, rc, wireAttrs);
      }
    }

    public void reset()
    {
      _captureFilter.reset();
    }

    public List<Integer> getRetryResponseCountList()
    {
      return _captureFilter.getRetryResponseCountList();
    }

    public List<Integer> getTotalResponseCountList()
    {
      return _captureFilter.getTotalResponseCountList();
    }

    public List<Integer> getOverloadCountList()
    {
      return _captureFilter.getOverloadCountList();
    }
  }

  public class DistributionNonDiscreteRing<T>
  {
    private final TreeMap<Integer, T> _cumulativePointsMap;
    private final int _totalPoints;

    public DistributionNonDiscreteRing(Map<T, Integer> pointsMap)
    {
      _cumulativePointsMap = calculateCDF(pointsMap);
      _totalPoints = _cumulativePointsMap.isEmpty() ? 0 : _cumulativePointsMap.lastKey();
    }

    public T get(int unused)
    {
      if (_cumulativePointsMap.isEmpty())
      {
        return null;
      }
      int rand = ThreadLocalRandom.current().nextInt(_totalPoints);
      return _cumulativePointsMap.higherEntry(rand).getValue();
    }

    public Iterator<T> getIterator(int unused)
    {
      List<T> hosts = new ArrayList<>(_cumulativePointsMap.values());
      if (!hosts.isEmpty())
      {
        Collections.shuffle(hosts);
        //we try to put host with higher probability as the first by calling get. This avoids the situation where unhealthy host is returned first.
        try
        {
          Collections.swap(hosts, 0, hosts.indexOf(get(0)));
        } catch (IndexOutOfBoundsException e)
        {
        }
      }
      return hosts.iterator();
    }

    private TreeMap<Integer, T> calculateCDF(Map<T, Integer> pointsMap)
    {
      int cumulativeSum = 0;
      TreeMap<Integer, T> cumulativePointsMap = new TreeMap<Integer, T>();

      for (Map.Entry<T, Integer> entry : pointsMap.entrySet())
      {
        if (entry.getValue() == 0)
        {
          continue;
        }
        cumulativeSum += entry.getValue();
        cumulativePointsMap.put(cumulativeSum, entry.getKey());
      }
      return cumulativePointsMap;
    }
  }

  public static void main(String[] args) {
    int[] serverWeights = {100, 100, 100};
    boolean[] overloadStatus = {true, true, false};

    ServerRetryFilterSimulation serverRetryFilterSimulation = new ServerRetryFilterSimulation(3, 0.1, 5000L, 100);
    serverRetryFilterSimulation.run(serverWeights, overloadStatus, 300);
  }
}
