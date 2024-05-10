package com.linkedin.d2.balancer.dualread;

import com.google.common.annotations.VisibleForTesting;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.util.RateLimitedLogger;
import com.linkedin.util.clock.SystemClock;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UriPropertiesDualReadMonitor
{
  private static final Logger LOG = LoggerFactory.getLogger(UriPropertiesDualReadMonitor.class);

  private final HashMap<String, ClusterMatchRecord> _clusters = new HashMap<>();
  // Limit error report logging to every 10 minutes
  private final RateLimitedLogger RATE_LIMITED_LOGGER =
      new RateLimitedLogger(LOG, TimeUnit.MINUTES.toMillis(10), SystemClock.instance());
  private int _totalUris = 0;
  private int _matchedUris = 0;
  private final DualReadLoadBalancerJmx _dualReadLoadBalancerJmx;

  public UriPropertiesDualReadMonitor(DualReadLoadBalancerJmx dualReadLoadBalancerJmx)
  {
    _dualReadLoadBalancerJmx = dualReadLoadBalancerJmx;
  }

  public synchronized void reportData(String clusterName, UriProperties property, boolean fromNewLb)
  {
    ClusterMatchRecord cluster = _clusters.computeIfAbsent(clusterName, k -> new ClusterMatchRecord());

    if (fromNewLb)
    {
      cluster._newLb = property;
    }
    else
    {
      cluster._oldLb = property;
    }

    _totalUris -= cluster._uris;
    _matchedUris -= cluster._matched;

    LOG.debug("Updated URI properties for cluster {}:\nOld LB: {}\nNew LB: {}",
        clusterName, cluster._oldLb, cluster._newLb);

    if (cluster._oldLb == null && cluster._newLb == null)
    {
      _clusters.remove(clusterName);
      updateJmxMetrics(clusterName, null);
      return;
    }

    cluster._matched = 0;

    if (cluster._oldLb == null || cluster._newLb == null)
    {
      LOG.debug("Added new URI properties for {} for {} LB.", clusterName, fromNewLb ? "New" : "Old");

      cluster._uris = (cluster._oldLb == null) ? cluster._newLb.Uris().size() : cluster._oldLb.Uris().size();
      _totalUris += cluster._uris;

      updateJmxMetrics(clusterName, cluster);
      return;
    }

    cluster._uris = cluster._oldLb.Uris().size();
    Set<URI> newLbUris = new HashSet<>(cluster._newLb.Uris());

    for (URI uri : cluster._oldLb.Uris())
    {
      if (!newLbUris.remove(uri))
      {
        continue;
      }

      if (compareURI(uri, cluster._oldLb, cluster._newLb))
      {
        cluster._matched++;
      }
    }
    // add the remaining unmatched URIs in newLbUris to the uri count
    cluster._uris += newLbUris.size();

    if (cluster._matched != cluster._uris)
    {
      infoOrDebugIfLimited(
          "Mismatched uri properties for cluster {} (match score: {}, total uris: {}):\nOld LB: {}\nNew LB: {}",
          clusterName, (double) cluster._matched / (double) cluster._uris, cluster._uris, cluster._oldLb,
          cluster._newLb);
    }
    else
    {
      LOG.debug("Matched uri properties for cluster {} (matched {} out of {} URIs)", clusterName,
          cluster._matched, cluster._uris);
    }

    _totalUris += cluster._uris;
    _matchedUris += cluster._matched;

    updateJmxMetrics(clusterName, cluster);
  }

  private void updateJmxMetrics(String clusterName, ClusterMatchRecord cluster)
  {
    // set a copy of cluster match record to jmx to avoid jmx reading the record in the middle of an update
    _dualReadLoadBalancerJmx.setClusterMatchRecord(clusterName, cluster == null ? null : cluster.copy());
    _dualReadLoadBalancerJmx.setUriPropertiesSimilarity((double) _matchedUris / (double) _totalUris);
  }

  private static boolean compareURI(URI uri, UriProperties oldLb, UriProperties newLb)
  {
    String clusterName = oldLb.getClusterName();
    return compareMaps("partition desc", clusterName, uri, UriProperties::getPartitionDesc, oldLb, newLb) &&
        compareMaps("specific properties", clusterName, uri, UriProperties::getUriSpecificProperties, oldLb, newLb);
  }

  private static <K, V> boolean compareMaps(
      String type, String cluster, URI uri, Function<UriProperties, Map<URI, Map<K, V>>> extractor,
      UriProperties oldLb, UriProperties newLb
  )
  {
    Map<K, V> oldData = extractor.apply(oldLb).get(uri);
    Map<K, V> newData = extractor.apply(newLb).get(uri);
    if (Objects.equals(oldData, newData))
    {
      return true;
    }

    LOG.debug("URI {} for {}/{} mismatched between old and new LB.\nOld LB: {}\nNew LB: {}",
        type, cluster, uri, oldData, newData);
    return false;
  }

  private void infoOrDebugIfLimited(String msg, Object... args)
  {
    if (RATE_LIMITED_LOGGER.logAllowed())
    {
      LOG.info(msg, args);
    }
    else
    {
      LOG.debug(msg, args);
    }
  }

  @VisibleForTesting
  synchronized int getTotalUris()
  {
    return _totalUris;
  }

  @VisibleForTesting
  synchronized int getMatchedUris()
  {
    return _matchedUris;
  }

  @VisibleForTesting
  synchronized ClusterMatchRecord getMatchRecord(String cluster)
  {
    return _clusters.get(cluster).copy();
  }

  public static class ClusterMatchRecord
  {
    @Nullable
    @VisibleForTesting
    UriProperties _oldLb;

    @Nullable
    @VisibleForTesting
    UriProperties _newLb;

    @VisibleForTesting
    int _uris;

    @VisibleForTesting
    int _matched;

    private ClusterMatchRecord()
    {
    }

    @VisibleForTesting
    ClusterMatchRecord(@Nullable UriProperties oldLb, @Nullable UriProperties newLb, int uris, int matched)
    {
      _oldLb = oldLb;
      _newLb = newLb;
      _uris = uris;
      _matched = matched;
    }

    ClusterMatchRecord copy()
    {
      return new ClusterMatchRecord(_oldLb, _newLb, _uris, _matched);
    }

    @Override
    public String toString()
    {
      return "ClusterMatchRecord{ " +
          "\nTotal Uris: " + _uris + ", Matched: " + _matched +
          "\nOld LB: " + _oldLb +
          "\nNew LB: " + _newLb +
          '}';
    }

    @Override
    public boolean equals(Object obj)
    {
      if (this == obj)
      {
        return true;
      }
      if (obj == null)
      {
        return false;
      }
      if (getClass() != obj.getClass())
      {
        return false;
      }

      ClusterMatchRecord o = (ClusterMatchRecord) obj;

      return Objects.equals(_oldLb, o._oldLb)
          && Objects.equals(_newLb, o._newLb)
          && _uris == o._uris
          && _matched == o._matched;
    }

    @Override
    public int hashCode()
    {
      return Objects.hash(_oldLb, _newLb, _uris, _matched);
    }
  }
}
