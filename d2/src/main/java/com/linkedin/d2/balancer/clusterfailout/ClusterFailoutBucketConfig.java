package com.linkedin.d2.balancer.clusterfailout;

import java.util.Map;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a simple data structure for tracking the traffic state of a given application.
 */


/*
failedoutBuckets: array[BucketFailoutConfig] {
    fabric: FabricUrn
    partition: string
    // e.g. buckets 0-4 are offline
    bucketIdRange: IntRange
    offlineAt: Time
    onlineAt: optional Time
  }
 */
public class ClusterFailoutBucketConfig
{
  // Fabric that this configuration is addressing.
  private final String _fabricUrn;
  // Traffic partition, eg. Main, Guest, Static, etc.
  private final String _partition;
  // Range of buckets offline. Eg, 5, so buckets 0-4 are offline.
  private final Integer _bucketOfflineRange;
  // What time should the buckets go offline at?
  private final Date _offlineAt;
  // What time should they come back online?
  private final Date _onlineAt;

  private final static String FABRIC_URN_PROPERTY = "fabric";
  private final static String PARTITION_PROPERTY = "partition";
  private final static String BUCKET_ID_RANGE = "bucketIdRange";
  private final static String OFFLINE_AT_PROPERTY = "offlineAt";
  private final static String ONLINE_AT_PROPERTY = "onlineAt";

  private static final Logger _logger = LoggerFactory.getLogger(
      ClusterFailoutBucketConfig.class);

  public ClusterFailoutBucketConfig(String fabricUrn,
      String partition,
      Integer bucketOfflineRange,
      Date offlineAt,
      Date onlineAt)
  {
    _fabricUrn = fabricUrn;
    _partition = partition;
    _bucketOfflineRange = bucketOfflineRange;
    _offlineAt = offlineAt;
    _onlineAt = onlineAt;
  }

  public static ClusterFailoutBucketConfig createFromMap(Map<String, Object> configMap) {
    try {
      return new ClusterFailoutBucketConfig(
          (String) configMap.get(ClusterFailoutBucketConfig.FABRIC_URN_PROPERTY),
          (String) configMap.get(ClusterFailoutBucketConfig.PARTITION_PROPERTY),
          (Integer) configMap.get(ClusterFailoutBucketConfig.BUCKET_ID_RANGE),
          (Date) configMap.get(ClusterFailoutBucketConfig.OFFLINE_AT_PROPERTY),
          (Date) configMap.get(ClusterFailoutBucketConfig.ONLINE_AT_PROPERTY));
    }
    catch(Exception e) {
      _logger.error("Error while converting SLF properties: " + e.getMessage());
      return null;
    }
  }

  public String getFabricUrn() {
    return _fabricUrn;
  }
  public String getPartition() {
    return _partition;
  }
  public Integer getBucketOfflineRange() {
    return _bucketOfflineRange;
  }
  public Date getOfflineAt() {
    return _offlineAt;
  }
  public Date getOnlineAt() {
    return _onlineAt;
  }


  @Override
  public String toString() {
    return "ClusterFailoutBucketConfig [_fabricUrn=" + _fabricUrn
        + ", partition=" + _partition
        + ", bucketOfflineRange=" + _bucketOfflineRange
        + ", offlineAt=" + _offlineAt
        + ", onlineAt=" + _onlineAt
        + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + _fabricUrn.hashCode();
    result = prime * result + _partition.hashCode();
    result = prime * result + _bucketOfflineRange.hashCode();
    result = prime * result + _offlineAt.hashCode();
    result = prime * result + _onlineAt.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;

    ClusterFailoutBucketConfig other = (ClusterFailoutBucketConfig) obj;
    return _fabricUrn.equals(other.getFabricUrn()) &&
        _partition.equals(other.getPartition()) &&
        _bucketOfflineRange.equals(other.getBucketOfflineRange()) &&
        _offlineAt.equals(other.getOfflineAt()) &&
        _onlineAt.equals(other.getOnlineAt()));
  }
}
