package com.linkedin.darkcluster.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;


/**
 * This represents the response validation metrics sent to dark clusters in the form of a request header
 * This header stores two fields:
 * 1) source: uniquely identifying the instance of the application running on a dispatcher. For eg: combination of hostname and instance identifier
 * 2) metrics: the metrics collected so far on the dispatcher
 * 3) timestamp: when the metrics were updated
 */
public class ResponseValidationMetricsHeader {
  private final String _source;
  private final ResponseValidationMetrics _validationMetrics;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * Represents the header value sent out to dark cluster
   * @param source uniquely identifying the dispatcher sending out the dark cluster response validation metrics
   * @param validationMetrics the actual response validation metrics sent out to dark cluster
   */
  @JsonCreator
  public ResponseValidationMetricsHeader(@JsonProperty("source") String source,
      @JsonProperty("validationMetrics") ResponseValidationMetrics validationMetrics) {
    _source = source;
    _validationMetrics = validationMetrics;
  }

  public String getSource() {
    return _source;
  }

  public ResponseValidationMetrics getValidationMetrics() {
    return _validationMetrics;
  }

  public static ResponseValidationMetricsHeader deserialize(String json) throws IOException {
    return OBJECT_MAPPER.readValue(json, ResponseValidationMetricsHeader.class);
  }

  public String serialize() throws JsonProcessingException {
    return OBJECT_MAPPER.writeValueAsString(this);
  }

  @Override
  public String toString() {
    return String.format("[source: %s, metrics: %s]", _source, _validationMetrics);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_source, _validationMetrics);
  }

  @Override
  public boolean equals(Object that) {
    if (that == null || !(that instanceof ResponseValidationMetricsHeader)) {
      return false;
    }
    ResponseValidationMetricsHeader header = (ResponseValidationMetricsHeader) that;
    return Objects.equals(_source, header._source)
        && Objects.equals(_validationMetrics, header._validationMetrics);
  }

  public static class ResponseValidationMetrics {
    private final Map<String, Long> _metricsMap;
    private final long _timestamp;

    /**
     * POJO to hold metrics corresponding to response validation performed against a dark cluster
     * @param metricsMap key / value pairs representing the metric name and the metric value
     * @param timestamp The dispatcher stamps every dark request that it sends to dark cluster with a timestamp representing the
     *                  time at which the aggregation of metrics was done. The dispatcher sends these metrics to dark cluster as
     *                  request headers while making dark request calls. Since these calls are asynchronous, they can go to
     *                  dark cluster in any random order. In order to enable the dark cluster to consume latest metrics, the
     *                  dispatcher stamps every call with a timestamp.
     */
    @JsonCreator
    public ResponseValidationMetrics(@JsonProperty("metricsMap") Map<String, Long> metricsMap,
        @JsonProperty("timestamp") long timestamp) {
      _metricsMap = metricsMap;
      _timestamp = timestamp;
    }

    public Map<String, Long> getMetricsMap() {
      return _metricsMap;
    }

    public long getTimestamp() {
      return _timestamp;
    }

    @Override
    public int hashCode() {
      return Objects.hash(_metricsMap, _timestamp);
    }

    @Override
    public boolean equals(Object that) {
      if (that == null || !(that instanceof ResponseValidationMetrics)) {
        return false;
      }
      ResponseValidationMetrics metrics = (ResponseValidationMetrics) that;
      return Objects.equals(_metricsMap, metrics._metricsMap)
          && Objects.equals(_metricsMap, metrics._metricsMap);
    }

    @Override
    public String toString() {
      return String.format("[metrics: %s, timestamp: %s]", _metricsMap, _timestamp);
    }
  }
}

