package com.linkedin.darkcluster.impl;

import com.linkedin.darkcluster.api.DarkClusterResponseValidationMetricsCollector;
import com.linkedin.darkcluster.api.ResponseValidationMetricsHeader;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.linkedin.util.clock.Clock;


/**
 * This is executed on dark cluster where it collects metrics from multiple sources by maintaining an in-memory map of sources to metrics.
 * It has two responsibilities:
 * 1. Populate the in-memory map with the metrics from incoming request headers. This will be an async call where multiple
 * threads may try to aggregate metrics as and when there are incoming dark requests. This ensures that we do not double
 * count metrics from the same source.
 * 2. A single threaded getter to retrieve the aggregated metrics from across all sources. This is single threaded to ensure
 * that the consumer of metrics is always called synchronously so that the consumer receives metrics that are increasing
 * monotonically. This is all the more important if the consumer emits ingraph metrics (most consumers use this to emit to ingraphs)
 * as counters where counters are meant to be increasing monotonically.
 */
public class DarkResponseValidationMetricsCollectorImpl implements DarkClusterResponseValidationMetricsCollector {
  /**
   * this is a a map of source -> metrics where metrics is a map of metric name -> metric value
   * Example: host1 -> ((success_count -> 10, failure_count -> 1), 12345L)
   */
  private final Map<String, Long> _defaultBucketMetrics = new ConcurrentHashMap<>();
  private final Map<String, Lock> _sourceLockMap = new ConcurrentHashMap<>();
  private final Map<String, MetricsInternal> _internalMetricsMap = new ConcurrentHashMap<>();
  private final Object _defaultBucketLock = new Object();
  private final Clock _clock;
  private final long _collectionFrequencyInMillis;

  private static final Logger LOG = LoggerFactory.getLogger(DarkResponseValidationMetricsCollectorImpl.class);

  public DarkResponseValidationMetricsCollectorImpl(Clock clock, long collectionFrequencyInMillis) {
    _clock = clock;
    _collectionFrequencyInMillis = collectionFrequencyInMillis;
  }

  /**
   * This method is used for collecting the incoming request header's metrics into the existing in-memory map of metrics.
   * We do an inplace update of the local in-memory map corresponding to the source of the incoming header. We do this
   * only if the metrics corresponding to the source has not been updated in the last <code>_collectionFrequencyInMillis</code>ms.
   * The goal is to keep the in-memory map of metrics updated at a frequency which matches with that of metrics reporting
   * frequency which means that there is no need to update the metrics for every incoming metric. With this, the probability
   * of multiple threads trying to update the map reduces significantly.
   * The source already handles the logic of aggregating per-dark cluster metric counts so that the dark cluster need not
   * manage incrementing counters here.
   * In addition to doing an in-place update, it also handles other edge cases:
   * 1) The requests from source may be coming out of order to dark cluster: the source tags every outgoing counter in
   * the header with a timestamp. We use this to determine whether or not to update metrics. If the incoming timestamp < existing timestamp,
   * we discard the incoming header and do nothing.
   * 2) A source bouncing will lead to a reduced value of counter than was seen previously by the dark cluster:
   * In order to handle this scenario, we compare the values of each metric in the existing in-memory map corresponding to the
   * incoming header's source. If any metric in the incoming header turns out to be lower than that of the in-memory
   * map, we roll up existing metrics of the source into a default bucket. And then do an in-place update of the in-memory map
   * of the source
   * Example:
   * let's say existing map is:
   * host1 -> ((success_count -> 10, failure_count -> 1), sourceTimestamp -> 1)
   * host2 -> ((success_count -> 20, failure_count -> 2), sourceTimestamp -> 1)
   * default bucket -> ()
   *
   * let's say incoming header is:
   * host2 -> ((success_count -> 5, failure_count -> 0), sourceTimestamp -> 2)
   *
   * We change it to:
   * host1 -> ((success_count -> 10, failure_count -> 1), sourceTimestamp -> 1)
   * host2 -> ((success_count -> 5, failure_count -> 0), sourceTimestamp -> 2)
   * default bucket -> (success_count -> 20, failure_count -> 2)
   *
   * let's say there's another incoming header:
   * host1 -> ((success_count -> 10, failure_count -> 0), sourceTimestamp -> 3)
   * We change it to:
   * host1 -> ((success_count -> 10, failure_count -> 0), sourceTimestamp -> 3)
   * host2 -> ((success_count -> 5, failure_count -> 0), sourceTimestamp -> 2)
   * default bucket -> (success_count -> 30, failure_count -> 3) // we add the corresponding metrics to the default bucket here
   * so that the total count reflects older values
   *
   * But let's say there is a scenario which is a combination of 1 and 2. Eg:
   * incoming header h1: host1 -> ((success_count -> 100, failure_count -> 5), sourceTimestamp -> 1)
   * incoming header h2: host1 -> ((success_count -> 5, failure_count -> 0), sourceTimestamp -> 2)
   * Note that h2 is a header from host1 after a restart of the application. Also let's say we get h2 first and then h1.
   * In such a case, we ignore h1 completely. This is going to be a rare scenario and it should be ok to ignore some of the metrics
   * from being aggregated.
   *
   * This method is crafted to handle multiple threads trying to update metrics by obtaining two locks:
   * 1. Lock on the individual entry for the given source in the in-memory map: this is to ensure that all updates to
   * the metrics for a given source is always done synchronously. Since this lock is per entry in the map, simultaneous
   * updates to other source entries can happen without waiting among threads.
   * 2. Lock on the default bucket in case this is to be updated: this is a common lock used by all threads so it may block
   * multiple threads when they all have to update this bucket. However, this can happen only when the bucket needs to be
   * updated i.e., when the respective source has bounced. For subsequent requests from the source, we need not
   * update the bucket. Considering this is a relatively low occurrence, it should not impact performance significantly.
   */
  @Override
  public void collect(ResponseValidationMetricsHeader header) {
    String source = header.getSource();
    ResponseValidationMetricsHeader.ResponseValidationMetrics sourceMetrics = header.getValidationMetrics();
    if (!_sourceLockMap.containsKey(source)) {
      _sourceLockMap.putIfAbsent(source, new ReentrantLock());
    }
    Lock sourceLock = _sourceLockMap.get(source);
    MetricsInternal existingMetrics = _internalMetricsMap.get(source);
    long currentTimestamp = _clock.currentTimeMillis();
    if (existingMetrics == null) {
      // multiple threads may satisfy this condition but only one thread will obtain the lock
      if (sourceLock.tryLock()) {
        try {
          _internalMetricsMap.put(source, new MetricsInternal(sourceMetrics, currentTimestamp));
        } finally {
          sourceLock.unlock();
        }
      } // else do nothing here since a different thread might be attempting to update the metrics
    } else {
      if (existingMetrics._sourceTimestamp < sourceMetrics.getTimestamp()
          && currentTimestamp - existingMetrics._clientTimestamp >= _collectionFrequencyInMillis) {
        if (sourceLock.tryLock()) {
          try {
            if (isGreaterThan(existingMetrics._metrics, sourceMetrics.getMetricsMap())) {
              // obtain the default bucket lock (global level lock) iff the incoming metrics are less than existing metrics
              synchronized (_defaultBucketLock) {
                existingMetrics._metrics.forEach((name, value) -> _defaultBucketMetrics.merge(name, value, (oldVal, newVal) -> oldVal + newVal));
                _internalMetricsMap.put(source, new MetricsInternal(sourceMetrics, currentTimestamp));
              }
            } else {
              _internalMetricsMap.put(source, new MetricsInternal(sourceMetrics, currentTimestamp));
            }
          } finally {
            sourceLock.unlock();
          }
        } // else do nothing here since a different thread might be attempting to update the metrics
      }
    }
  }

  /**
   * Aggregates metrics from all sources and returns the aggregated metrics
   * Example:
   * let's say existing sourceMetrics map at time t1 is:
   * host1 -> (success_count -> 10, failure_count -> 1)
   * host2 -> (success_count -> 20, failure_count -> 2)
   * result -> (success_count -> 20, failure_count -> 2) => success rate = success_count / (success_count + failure_count) = 30 / 33 ~ 0.9
   *
   * Assume we get an incoming metrics from host1 at time t2:
   * host1 -> (success_count -> 20, failure_count -> 10)
   *
   * this method replaces the value for host1 with the new incoming metrics and aggregates from both hosts:
   * host1 -> (success_count -> 20, failure_count -> 10)
   * host2 -> (success_count -> 20, failure_count -> 2)
   * result -> (success_count -> 40, failure_count -> 12) => success rate = 40 / 52 ~ 0.76
   * Note that the consumer calling this method should do so in a single thread so as to guarantee that all metrics are increasing monotonically.
   * A consumer (typically one that emits ingraphs) can emit the metrics as counters.
   * Also, we do not lock the in-memory map here so as to not impact performance. We only read the snapshot
   * of the metrics at a given time and emit them although there might be simultaneous updates to these metrics
   */
  @Override
  public Map<String, Long> get() {
    Stream<Map<String, Long>> sourceMetricStream = _internalMetricsMap.values().stream()
        .map(metricsInternal -> metricsInternal._metrics);
    Stream<Map<String, Long>> defaultBucketMetricStream = Stream.of(_defaultBucketMetrics);
    Map<String, Long> metrics = Stream.concat(sourceMetricStream, defaultBucketMetricStream)
        .map(Map::entrySet)
        .flatMap(Collection::stream)
        .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue(), (oldV, newV) -> oldV + newV));
    LOG.debug("Collected metrics so far: {}", metrics);
    return metrics;
  }

  /**
   * returns true if any metric in {@param metrics1} is greater than that of {@param metrics2}
   */
  private boolean isGreaterThan(Map<String, Long> metrics1, Map<String, Long> metrics2) {
    for (Map.Entry<String, Long> entry : metrics1.entrySet()) {
      String name = entry.getKey();
      long value = entry.getValue();
      if (metrics2.containsKey(name) && value > metrics2.get(name)) {
        return true;
      }
    }
    return false;
  }

  private static class MetricsInternal {
    final long _sourceTimestamp;
    final Map<String, Long> _metrics;
    final long _clientTimestamp;

    MetricsInternal(ResponseValidationMetricsHeader.ResponseValidationMetrics sourceMetricsData, long clientTimestamp) {
      _sourceTimestamp = sourceMetricsData.getTimestamp();
      _metrics = sourceMetricsData.getMetricsMap();
      _clientTimestamp = clientTimestamp;
    }

    @Override
    public String toString() {
      return String.format("metrics:%s, sourceTime: %s, clientTime: %s", _metrics, _sourceTimestamp, _clientTimestamp);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      MetricsInternal that = (MetricsInternal) o;
      return _sourceTimestamp == that._sourceTimestamp
          && _clientTimestamp == that._clientTimestamp
          && Objects.equals(_metrics, that._metrics);
    }

    @Override
    public int hashCode() {
      return Objects.hash(_sourceTimestamp, _clientTimestamp, _metrics);
    }
  }
}

