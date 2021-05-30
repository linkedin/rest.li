package com.linkedin.darkcluster.impl;

import com.linkedin.darkcluster.api.DispatcherResponseValidationMetricsHolder;
import com.linkedin.darkcluster.api.ResponseValidationMetricsHeader;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Impl of {@link DispatcherResponseValidationMetricsHolder} which uses in-memory map to store response validation metrics for each
 * dark cluster. These metrics are essentially counters which are added up as and when response validation is done.
 * This is an object that lives through the course of application's life cycle, incrementing validation metrics over time.
 */
public class DispatcherResponseValidationMetricsHolderImpl implements DispatcherResponseValidationMetricsHolder {
  /**
   * this is a a map of dark cluster -> metrics where metrics is a map of metric name -> metric value
   * Example: dark_cluster1 -> (success_count -> 10, failure_count -> 1)
   */
  private final ConcurrentMap<String, ConcurrentMap<String, LongAdder>> _darkClusterToMetricsMap = new ConcurrentHashMap<>();
  private final Clock _clock;

  private static final Logger LOG = LoggerFactory.getLogger(DispatcherResponseValidationMetricsHolderImpl.class);

  public DispatcherResponseValidationMetricsHolderImpl(@Nonnull Clock clock) {
    _clock = clock;
  }

  /**
   * Returns metrics for a given dark cluster. It can be null if there are no metrics corresponding to the dark cluster.
   * It sums up all the metrics collected over time and returns the sum. However, the call to sum() is not a blocking
   * operation which means it is possible that there may be threads simultaneously updating the metrics which may not
   * be accounted for while returning the sum. This is acceptable since we do not really care so much about accuracy here.
   * These metrics are continually increasing and they may get reported in the next call.
   */
  @Override
  public ResponseValidationMetricsHeader.ResponseValidationMetrics get(String darkClusterName) {
    Map<String, LongAdder> perClusterMetrics = _darkClusterToMetricsMap.get(darkClusterName);
    if (perClusterMetrics != null) {
      Map<String, Long> metricsMap = perClusterMetrics.keySet()
          .stream()
          .collect(Collectors.toMap(Function.identity(), name -> perClusterMetrics.get(name).sum()));
      LOG.debug("Aggregated metrics for dark cluster: {}, metrics: {}", darkClusterName, metricsMap);
      return new ResponseValidationMetricsHeader.ResponseValidationMetrics(metricsMap, _clock.millis());
    }
    LOG.debug("No metrics found for darkCluster: {}", darkClusterName);
    return null;
  }

  /**
   * Add metrics for the given dark cluster. It adds these metrics onto the existing counters if it is present, else it
   * will create a new entry in the mapping.
   *
   * Example:
   * Lets say dark cluster to metrics map at time t1 is:
   * darkhost1 -> ((success_count -> 10, failure_count -> 1))
   * darkhost2 -> ((success_count -> 20, failure_count -> 2))
   *
   * Assume there is an incoming metrics at time t2 for darkhost1: (success_count -> 15, failure_count -> 5)
   *
   * The dark host to metrics map will be updated to:
   * darkhost1 -> (success_count -> 25, failure_count -> 6)
   * darkhost2 -> (success_count -> 20, failure_count -> 2)
   *
   * Note that all metrics are increasing monotonically over time.
   * Every metric is incremented by the incoming value atomically. However it does not guarantee that all the incoming
   * metrics are incremented in a single atomic operation since this is not necessary.
   */
  @Override
  public void add(String darkClusterName, Map<String, Long> metrics) {
    if (!_darkClusterToMetricsMap.containsKey(darkClusterName)) {
      _darkClusterToMetricsMap.putIfAbsent(darkClusterName, new ConcurrentHashMap<>());
    }
    Map<String, LongAdder> darkMetrics = _darkClusterToMetricsMap.get(darkClusterName);
    metrics.forEach((name, value) -> {
      if (!darkMetrics.containsKey(name)) {
        darkMetrics.putIfAbsent(name, new LongAdder());
      }
      darkMetrics.get(name).add(value);
    });
  }
}
