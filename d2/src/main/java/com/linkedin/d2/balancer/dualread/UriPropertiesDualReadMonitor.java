package com.linkedin.d2.balancer.dualread;

import com.linkedin.d2.balancer.properties.UriProperties;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UriPropertiesDualReadMonitor
{
  private static final Logger LOG = LoggerFactory.getLogger(UriPropertiesDualReadMonitor.class);

  private final Map<String, Cluster> _clusters = new HashMap<>();
  private double _totalUris = 0;
  private double _matchedUris = 0;
  private final DualReadLoadBalancerJmx _dualReadLoadBalancerJmx;

  public UriPropertiesDualReadMonitor(DualReadLoadBalancerJmx dualReadLoadBalancerJmx)
  {
    _dualReadLoadBalancerJmx = dualReadLoadBalancerJmx;
  }

  public void reportData(String clusterName, UriProperties property, boolean fromNewLb)
  {
    Cluster cluster = _clusters.computeIfAbsent(clusterName, k -> new Cluster());

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

    if (cluster._oldLb == null && cluster._newLb == null)
    {
      _clusters.remove(clusterName);
      return;
    }

    cluster._matched = 0;

    if (cluster._oldLb == null || cluster._newLb == null)
    {
      cluster._uris = (cluster._oldLb == null) ? cluster._oldLb.Uris().size() : cluster._newLb.Uris().size();
      _totalUris += cluster._uris;
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

    cluster._uris += newLbUris.size();

    _totalUris += cluster._uris;
    _matchedUris += cluster._matched;
    _dualReadLoadBalancerJmx.setUriPropertiesSimilarity(_matchedUris / _totalUris);
  }

  private static class Cluster
  {
    @Nullable
    private UriProperties _oldLb;
    @Nullable
    private UriProperties _newLb;
    private double _uris;
    private double _matched;
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
    if (Objects.equals(oldData, newData) || isEmptyMap(oldData) == isEmptyMap(newData))
    {
      return true;
    }

    LOG.debug("URI {} for {}/{} mismatched between old and new LB.\nOld LB: {}\nNew LB: {}",
        type, cluster, uri, oldData, newData);
    return false;
  }

  private static <K, V> boolean isEmptyMap(Map<K, V> map)
  {
    return map == null || map.isEmpty();
  }
}
