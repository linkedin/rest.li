package com.linkedin.d2.balancer.clusterfailout;

import java.util.Map;
import java.util.Date;

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

  public ClusterFailoutBucketConfig(Map<String, Object> configMap) {
    _fabricUrn = (String)configMap.get("fabric");
    _partition = (String)configMap.get("partition");
    _bucketOfflineRange = (Integer)configMap.get("bucketOfflineRange");
    _offlineAt = (Date)configMap.get("offlineAt");
    _onlineAt = (Date)configMap.get("onlineAt");
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
    if (!_fabricUrn.equals(other.getFabricUrn()))
      return false;
    if (!_partition.equals(other.getPartition()))
      return false;
    if (!_bucketOfflineRange.equals(other.getBucketOfflineRange()))
      return false;
    if (!_offlineAt.equals(other.getOfflineAt()))
      return false;
    if (!_onlineAt.equals(other.getOnlineAt()))
      return false;
    return true;
  }
}
