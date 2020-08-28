package com.linkedin.d2.balancer.event;

import com.linkedin.util.degrader.CallTracker;
import com.linkedin.util.degrader.DegraderControl;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * {@link D2MonitorBuilder} responsible for building up the D2Monitor event for one service.
 */

public class D2MonitorBuilder
{
  private final String _serviceName;
  final private String _clusterName;
  private final D2MonitorClusterStatsBuilder _clusterStatsBuilder;
  private final Map<URI, D2MonitorUriInfoBuilder> _uriInfoBuilderMap;
  private final int _partitionId;

  public D2MonitorBuilder(String serviceName, String clusterName, int partitionId)
  {
    _serviceName = serviceName;
    _clusterName = clusterName;
    _clusterStatsBuilder = new D2MonitorClusterStatsBuilder();
    _uriInfoBuilderMap = new HashMap<>();
    _partitionId = partitionId;
  }

  public D2MonitorClusterStatsBuilder getClusterStatsBuilder()
  {
    return _clusterStatsBuilder;
  }

  public D2MonitorUriInfoBuilder getOrCreateUriInfoBuilder(URI uri)
  {
    return _uriInfoBuilderMap.computeIfAbsent(uri, k -> new D2MonitorUriInfoBuilder(k));
  }

  public D2MonitorUriInfoBuilder addUriInfoBuilder(URI uri, D2MonitorUriInfoBuilder uriInfoBuilder)
  {
    return _uriInfoBuilderMap.putIfAbsent(uri, uriInfoBuilder);
  }

  public int getPartitionId()
  {
    return _partitionId;
  }

  public Map<URI, D2MonitorUriInfoBuilder> getUriInfoBuilderMap()
  {
    return _uriInfoBuilderMap;
  }

  /**
   * Reset the D2MonitorBuilder for next interval
   *
   * ClusterStats is not required to reset as the new snapshot can always overwrite the old one.
   * However it is necessary to clean uriInfoBuilderMap since we only want to keep track of the
   * unhealthy hosts in the past update interval.
   */
  public D2MonitorBuilder reset()
  {
    _clusterStatsBuilder.reset();
    _uriInfoBuilderMap.clear();
    return this;
  }

  /**
   * Build D2Monitor object in accord to current settings.
   *
   * The interval is the duration between this build and previous build (or when D2MonitorBuilder is created).
   *
   * @param intervalMs since last emitting
   */
  public final D2Monitor build(long intervalMs)
  {
    return new D2Monitor(_serviceName, _clusterName, _clusterStatsBuilder.build(),
        _uriInfoBuilderMap.values().stream().map(D2MonitorUriInfoBuilder::build).collect(Collectors.toList()),
        _partitionId, intervalMs);
  }


  public static class D2MonitorClusterStatsBuilder
  {
    private long _clusterCurrentCallCount;
    private double _clusterCurrentAverageLatencyMs;
    private long _clusterCurrentDroppedCalls;
    private long _clusterCurrentErrorCount;
    private long _clusterCurrentFailedToRouteCalls;
    private double _clusterDropLevel;
    private int _clusterNumHosts;

    public D2MonitorClusterStatsBuilder setClusterCurrentCallCount(long clusterCurrentCallCount)
    {
      _clusterCurrentCallCount = clusterCurrentCallCount;
      return this;
    }

    public D2MonitorClusterStatsBuilder setClusterCurrentAverageLatencyMs(double clusterCurrentAverageLatencyMs)
    {
      _clusterCurrentAverageLatencyMs = clusterCurrentAverageLatencyMs;
      return this;
    }

    public D2MonitorClusterStatsBuilder setClusterCurrentDroppedCalls(long clusterCurrentDroppedCalls)
    {
      _clusterCurrentDroppedCalls = clusterCurrentDroppedCalls;
      return this;
    }

    public D2MonitorClusterStatsBuilder setClusterCurrentErrorCount(long clusterCurrentErrorCount)
    {
      _clusterCurrentErrorCount = clusterCurrentErrorCount;
      return this;
    }

    public D2MonitorClusterStatsBuilder setClusterDropLevel(double clusterDropLevel)
    {
      _clusterDropLevel = clusterDropLevel;
      return this;
    }

    public D2MonitorClusterStatsBuilder setClusterCurrentFailedToRouteCalls(long clusterCurrentFailedToRouteCalls)
    {
      _clusterCurrentFailedToRouteCalls = clusterCurrentFailedToRouteCalls;
      return this;
    }

    public D2MonitorClusterStatsBuilder setClusterNumHosts(int clusterNumHosts)
    {
      _clusterNumHosts = clusterNumHosts;
      return this;
    }

    public void reset()
    {
      _clusterCurrentAverageLatencyMs = 0;
      _clusterCurrentCallCount = 0;
      _clusterCurrentDroppedCalls = 0;
      _clusterCurrentErrorCount = 0;
      _clusterCurrentFailedToRouteCalls = 0;
      _clusterDropLevel = 0.0;
      _clusterNumHosts = 0;
    }

    public D2Monitor.ClusterStats build()
    {
      return new D2Monitor.ClusterStats(_clusterCurrentCallCount, _clusterCurrentAverageLatencyMs,
          _clusterCurrentDroppedCalls, _clusterCurrentErrorCount, _clusterCurrentFailedToRouteCalls,
          _clusterDropLevel, _clusterNumHosts);
    }
  }

  public static class D2MonitorUriInfoBuilder
  {
    final private URI _uri;
    private long _currentCallCount;
    private long _totalCallCount;
    private long _outstandingCount;
    private double _currentLatency;
    private int _currentErrorCount;
    private long _50PctLatency;
    private long _90PctLatency;
    private long _95PctLatency;
    private long _99PctLatency;
    private long _quarantineDuration;
    private double _computedDropRate;
    private int _transmissionPoints;

    public D2MonitorUriInfoBuilder(URI uri)
    {
      _uri = uri;
      reset();
    }

    public void reset()
    {
      _currentCallCount = 0;
      _totalCallCount = 0;
      _outstandingCount = 0;
      _currentLatency = 0;
      _currentErrorCount = 0;
      _50PctLatency = 0;
      _90PctLatency = 0;
      _95PctLatency = 0;
      _99PctLatency = 0;
      _quarantineDuration = 0;
      _computedDropRate = 0;
      _transmissionPoints = 0;
    }

    public URI getUri()
    {
      return _uri;
    }

    public D2MonitorUriInfoBuilder setCurrentCallCount(long currentCallCount)
    {
      _currentCallCount = currentCallCount;
      return this;
    }

    public D2MonitorUriInfoBuilder setCurrentLatency(double currentLatency)
    {
      _currentLatency = currentLatency;
      return this;
    }

    public D2MonitorUriInfoBuilder setCurrentErrorCount(int currentErrorCount)
    {
      _currentErrorCount = currentErrorCount;
      return this;
    }

    public D2MonitorUriInfoBuilder setTotalCallCount(long totalCallCount)
    {
      _totalCallCount = totalCallCount;
      return this;
    }

    public D2MonitorUriInfoBuilder setOutstandingCount(long outstandingCount)
    {
      _outstandingCount = outstandingCount;
      return this;
    }

    public D2MonitorUriInfoBuilder set50PctLatency(long a50PctLatency)
    {
      _50PctLatency = a50PctLatency;
      return this;
    }

    public D2MonitorUriInfoBuilder set90PctLatency(long a90PctLatency)
    {
      _90PctLatency = a90PctLatency;
      return this;
    }

    public D2MonitorUriInfoBuilder set95PctLatency(long a95PctLatency)
    {
      _95PctLatency = a95PctLatency;
      return this;
    }

    public D2MonitorUriInfoBuilder setQuarantineDuration(long quarantineDuration)
    {
      _quarantineDuration = quarantineDuration;
      return this;
    }

    public D2MonitorUriInfoBuilder set99PctLatency(long a99PctLatency)
    {
      _99PctLatency = a99PctLatency;
      return this;
    }

    public D2MonitorUriInfoBuilder setComputedDropRate(double computedDropRate)
    {
      _computedDropRate = computedDropRate;
      return this;
    }

    public D2MonitorUriInfoBuilder setTransmissionPoints(int transmissionPoints)
    {
      _transmissionPoints = transmissionPoints;
      return this;
    }

    public D2Monitor.UriInfo build()
    {
      return new D2Monitor.UriInfo(_uri.getHost(), _uri.getPort(), _currentCallCount,
          _totalCallCount, _outstandingCount, _currentLatency, _currentErrorCount, _50PctLatency,
          _90PctLatency, _95PctLatency, _99PctLatency, _quarantineDuration, _computedDropRate, _transmissionPoints);
    }

    public void copyStats(CallTracker.CallStats callStats)
    {
      int callCount = callStats.getCallCount();
      this.setCurrentCallCount(callCount)
          .setCurrentLatency(callStats.getCallTimeStats().getAverage())
          .setTotalCallCount(callStats.getCallCountTotal())
          .setCurrentErrorCount((int)(callStats.getErrorRate() * callCount))
          .setOutstandingCount(callStats.getOutstandingCount())
          .set50PctLatency(callStats.getCallTimeStats().get50Pct())
          .set90PctLatency(callStats.getCallTimeStats().get90Pct())
          .set95PctLatency(callStats.getCallTimeStats().get95Pct())
          .set99PctLatency(callStats.getCallTimeStats().get99Pct());
    }
  }

  public void removeUri(Set<URI> uris)
  {
    _uriInfoBuilderMap.entrySet().removeIf(e -> !uris.contains(e.getKey()));
  }

}
