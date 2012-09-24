/*
   Copyright (c) 2012 LinkedIn Corp.

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

/**
 * $Id: $
 */

package com.linkedin.d2.discovery.util;


import com.linkedin.d2.balancer.config.ConfigWriter;
import com.linkedin.d2.balancer.properties.ClusterPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.HashBasedPartitionProperties;
import com.linkedin.d2.balancer.properties.PartitionProperties;
import com.linkedin.d2.balancer.properties.ServicePropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.util.PropertyUtil;
import com.linkedin.d2.balancer.zkfs.ZKFSUtil;
import com.linkedin.d2.discovery.PropertyBuilder;
import com.linkedin.d2.discovery.PropertySerializer;
import com.linkedin.d2.discovery.stores.zk.ZKConnection;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperPermanentStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class D2Config
{
  private static final Logger _log = LoggerFactory.getLogger(D2Config.class);
  public static final int NO_ERROR_EXIT_CODE = 0;
  public static final int CMD_LINE_ERROR_EXIT_CODE = 1;
  public static final int EXCEPTION_EXIT_CODE = 2;
  public static final int PARTITION_CONFIG_ERROR_EXIT_CODE = 3;

  private final ZKConnection _zkConnection;
  private final String _basePath;
  private final List<Map<String,Object>> _clusterServiceConfigurations;
  private final Map<String,Object> _clusterDefaults;
  private final Map<String,Object> _serviceDefaults;
  private final Map<String,Object> _serviceVariants;
  private final long _timeout;
  private final int _retryLimit;

  @SuppressWarnings("unchecked")
  public D2Config (String zkHosts, int sessionTimeout, String basePath,
                         long timeout, int retryLimit,
                         Map<String, Object> clusterDefaults,
                         Map<String, Object> serviceDefaults,
                         Map<String, Object> clusterServiceConfigurations,
                         Map<String, Object> extraClusterServiceConfigurations,
                         Map<String, Object> serviceVariants)
  {
    _retryLimit = retryLimit;
    // use retry zkConnection
    _zkConnection = new ZKConnection(zkHosts, sessionTimeout, _retryLimit, false, null, 0);
    _basePath = basePath;
    _timeout = timeout;
    _clusterServiceConfigurations = Arrays.asList(clusterServiceConfigurations, extraClusterServiceConfigurations);
    _clusterDefaults = clusterDefaults;
    _serviceDefaults = serviceDefaults;
    _serviceVariants = serviceVariants;
  }

  public int configure() throws Exception
  {
    Map<String,Object> clusterServiceConfiguration = merge(_clusterServiceConfigurations);

    Map<String,Map<String,Object>> clusters = new HashMap<String,Map<String,Object>>();
    Map<String,Map<String,Object>> services = new HashMap<String,Map<String,Object>>();
    // Ugly. But this is a map of service groups, so it needs to reflect multiple services maps.
    Map<String,Map<String,Map<String,Object>>> serviceVariants = new HashMap<String,Map<String,Map<String,Object>>>();

    // temporary mapping from cluster name to services map
    Map<String,Map<String,Map<String,Object>>> clusterToServiceMapping = new HashMap<String,Map<String,Map<String,Object>>>();

    _log.info("basePath: " + _basePath);
    _log.info("clusterDefaults: " + _clusterDefaults);
    _log.info("serviceDefaults: " + _serviceDefaults);

    for (String clusterName : clusterServiceConfiguration.keySet())
    {
      @SuppressWarnings("unchecked")
      Map<String,Object> clusterConfig = (Map<String,Object>)clusterServiceConfiguration.get(clusterName);
      clusterConfig.put("clusterName", clusterName);
      @SuppressWarnings("unchecked")
      Map<String,Map<String,Object>> servicesConfigs = (Map<String,Map<String,Object>>)clusterConfig.remove("services");
      @SuppressWarnings("unchecked")
      Map<String,Map<String,Object>> clusterVariantConfig = (Map<String,Map<String,Object>>)clusterConfig.remove("clusterVariants");
      for (String serviceName : servicesConfigs.keySet())
      {
        Map<String,Object> serviceConfig = servicesConfigs.get(serviceName);
        serviceConfig.put("clusterName", clusterName);
        serviceConfig.put("serviceName", serviceName);
      }

      // do some sanity check for partitions if any
      @SuppressWarnings("unchecked")
      Map<String, Object> partitionProperties = (Map<String, Object>)clusterConfig.get("partitionProperties");
      if (partitionProperties != null)
      {
        PartitionProperties.PartitionType partitionType = PropertyUtil.checkAndGetValue(partitionProperties, "partitionType",
            PartitionProperties.PartitionType.class, clusterName);

        switch (partitionType)
        {

          case RANGE:
          {
            if (partitionProperties.get("partitionKeyRegex") == null)
            {
              _log.error("null partitionKeyRegex for cluster: " + clusterName);
              return PARTITION_CONFIG_ERROR_EXIT_CODE;
            }
            Long partitionSize = PropertyUtil.parseLong("partitionSize",
                PropertyUtil.checkAndGetValue(partitionProperties, "partitionSize", String.class, clusterName));
            int partitionCount = PropertyUtil.parseInt("partitionCount",
                PropertyUtil.checkAndGetValue(partitionProperties, "partitionCount", String.class, clusterName));
            Long start = PropertyUtil.parseLong("keyRangeStart",
                PropertyUtil.checkAndGetValue(partitionProperties, "keyRangeStart", String.class, clusterName));

            if (partitionSize <= 0)
            {
              _log.error("Non-positive partition size! Cluster: " + clusterName);
              return PARTITION_CONFIG_ERROR_EXIT_CODE;
            }
            if (start < 0)
            {
              _log.error("partition id needs to be non negative");
              return PARTITION_CONFIG_ERROR_EXIT_CODE;
            }
            if (partitionCount < 0)
            {
              _log.error("partition count needs to be non negative");
              return PARTITION_CONFIG_ERROR_EXIT_CODE;
            }

            // replace string with numbers so that it works with the serializer
            partitionProperties.put("partitionSize", partitionSize);
            partitionProperties.put("partitionCount", partitionCount);
            partitionProperties.put("keyRangeStart", start);
            clusterConfig.put("partitionProperties", partitionProperties);

          }
          break;

          case HASH:
          {
            if (partitionProperties.get("partitionKeyRegex") == null)
            {
              _log.error("null partitionKeyRegex for cluster: " + clusterName);
              return PARTITION_CONFIG_ERROR_EXIT_CODE;
            }

            int partitionCount = PropertyUtil.parseInt("partitionCount",
                PropertyUtil.checkAndGetValue(partitionProperties, "partitionCount", String.class, clusterName));
            if (partitionCount < 0)
            {
              _log.error("partition count needs to be non negative");
              return PARTITION_CONFIG_ERROR_EXIT_CODE;
            }
            // replace string with number so that it works with the serializer
            partitionProperties.put("partitionCount", partitionCount);
            clusterConfig.put("partitionProperties", partitionProperties);
            try
            {
              String algorithm = PropertyUtil.checkAndGetValue(partitionProperties, "hashAlgorithm", String.class, clusterName);
              HashBasedPartitionProperties.HashAlgorithm.valueOf(algorithm.toUpperCase());
            }
            catch(Exception e)
            {
              _log.error("Hash algorithm not supported", e);
              return PARTITION_CONFIG_ERROR_EXIT_CODE;
            }

          }
          break;

          default:
            break;
        }
      }

      // Can't detect duplicate clusters because the cfg2 system has already collapsed it for us,
      // they would need to do the duplicate checking
      clusters.put(clusterName, clusterConfig);

      for (Map.Entry<String, Map<String,Object>> entry : servicesConfigs.entrySet())
      {
        Object previousEntry = services.put(entry.getKey(), entry.getValue());
        if (previousEntry != null)
        {
          _log.error("Identical service name found in multiple clusters! Service: " + entry.getKey() +
          ", cluster that caused conflict: " + clusterName);
          return EXCEPTION_EXIT_CODE;
        }
      }

      // for each cluster variant, copy the default cluster properties into this variant cluster, and
      // add it to the cluster list. Note that we do this after the default cluster has been added
      // to the cluster list.
      if (clusterVariantConfig != null)
      {
        for (String variant : clusterVariantConfig.keySet())
        {
          Map<String,Object> varConfig = clusterVariantConfig.get(variant);
          varConfig.put("clusterName", variant);
          // clusterConfig is the default cluster's info, and varConfig is this cluster variant's info.
          // We are copying from clusterConfig into varConfig if there is no such property in varConfig.
          varConfig = ConfigWriter.merge(varConfig, clusterConfig);
          Map<String,Object> oldCluster = clusters.put(variant, varConfig);
          if(oldCluster != null)
          {
            _log.error("Cluster variant name: " + variant + " is not unique!");
            return EXCEPTION_EXIT_CODE;
          }

          Map<String,Map<String,Object>> varServicesConfig = new HashMap<String,Map<String,Object>>();

          // now take a copy of the services for the default sibling cluster and point the
          // services to the cluster variant. We form this clusterToServiceMapping here so it is
          // easy for us to create the serviceGroups later.
          for (Map.Entry<String,Map<String,Object>> entry : servicesConfigs.entrySet())
          {
            // Deep copy each of the services into the new map
            Map<String,Object> varServiceConfig = ConfigWriter.merge(entry.getValue(), null);
            varServiceConfig.put("clusterName", variant);
            varServicesConfig.put(entry.getKey(), varServiceConfig);
          }
          clusterToServiceMapping.put(variant, varServicesConfig);
        }
      }

      clusterToServiceMapping.put(clusterName, servicesConfigs);
    }

    // there are service variants
    if (_serviceVariants != null)
    {
      for (String serviceGroup : _serviceVariants.keySet())
      {
        // each service group contains a list of cluster names and a type field that
        // describes how to treat the list. We group together the services described by these
        // listed clusters, and prep that for writing to a different znode than the default service
        // znode directory. Note that we had already pointed those services to the appropriate cluster
        // variant earlier.
        Map<String,Map<String,Object>> servicesGroupConfig = new HashMap<String,Map<String,Object>>();
        @SuppressWarnings("unchecked")
        Map<String,Object> configGroupMap = (Map<String,Object>) _serviceVariants.get(serviceGroup);
        String type = (String)configGroupMap.get("type");
        @SuppressWarnings("unchecked")
        List<String> clusterList = (List<String>)configGroupMap.get("clusterList");

        // create an alternate service table for the services specified by these cluster variants
        for (Iterator<String> iter = clusterList.listIterator(); iter.hasNext();)
        {
          String clusterItem = iter.next();

          Map<String,Map<String,Object>> candidateServices = clusterToServiceMapping.get(clusterItem);

          if (candidateServices == null)
          {
            // the service group had an unknown cluster!
            _log.error("Unknown cluster specified: " + clusterItem);
            return EXCEPTION_EXIT_CODE;
          }

          for (Map.Entry<String,Map<String,Object>> mapEntry :candidateServices.entrySet())
          {
            Object testValue = servicesGroupConfig.put(mapEntry.getKey(), mapEntry.getValue());
            if (testValue != null)
            {
              // We shouldn't have had conflicting services, two variants of the same cluster
              // were probably specified in the same service group.
              _log.error("Service group has variants of the same cluster: " + serviceGroup );
              return EXCEPTION_EXIT_CODE;
            }
          }
        }

        if("clusterVariantsList".equals(type))
        {
          // start from the full list of services, and then overwrite the services specified by the
          // cluster variants.
          Map<String,Map<String,Object>> fullServiceList = new HashMap<String,Map<String,Object>>(services);
          fullServiceList.putAll(servicesGroupConfig);
          serviceVariants.put(serviceGroup, fullServiceList);
        }
        else if ("fullClusterList".equals(type))
        {
          // The use has explicitly indicated that we should put these and only the services that
          // correspond to the named clusters in the serviceGroup.
          serviceVariants.put(serviceGroup, servicesGroupConfig);
        }
        else
        {
          _log.error("unknown serviceVariant type: " + type);
          return EXCEPTION_EXIT_CODE;
        }
      }
    }

    _log.debug("serviceVariants: "+ serviceVariants);

    _zkConnection.start();

    _log.info("Cluster configuration:\n" + clusters);
    writeConfig(ZKFSUtil.clusterPath(_basePath), new ClusterPropertiesJsonSerializer(),
                new ClusterPropertiesJsonSerializer(), clusters, _clusterDefaults);
    _log.info("Wrote cluster configuration");

    _log.info("Service configuration:\n" + services);
    writeConfig(ZKFSUtil.servicePath(_basePath), new ServicePropertiesJsonSerializer(),
                new ServicePropertiesJsonSerializer(), services, _serviceDefaults);
    _log.info("Wrote service configuration");

    if (!serviceVariants.isEmpty())
    {
      for (Map.Entry<String,Map<String,Map<String,Object>>> entry : serviceVariants.entrySet())
      {
        if (_log.isDebugEnabled())
        {
          _log.info("serviceVariant: "+ entry + "\n");
        }
        else
        {
          _log.info("serviceVariant: "+ entry.getKey() + "\n");
        }
        writeConfig(ZKFSUtil.servicePath(_basePath,entry.getKey()), new ServicePropertiesJsonSerializer(),
                    new ServicePropertiesJsonSerializer(), entry.getValue(), _serviceDefaults);
      }
      _log.info("Wrote service variant configurations");
    }

    _log.info("Configuration complete");

    return NO_ERROR_EXIT_CODE;
  }

  private <T> void writeConfig(String path, PropertySerializer<T> serializer,
                               PropertyBuilder<T> builder,
                               Map<String, Map<String, Object>> properties,
                               Map<String, Object> propertyDefaults) throws Exception
  {
    ZooKeeperPermanentStore<T> store = new ZooKeeperPermanentStore<T>(_zkConnection, serializer, path);
    ConfigWriter<T> writer = new ConfigWriter<T>(store, builder, properties, propertyDefaults, _timeout, TimeUnit.MILLISECONDS);
    writer.writeConfig();
  }

  private Map<String,Object> merge(List<Map<String,Object>> maps)
  {
    Map<String,Object> result = new HashMap<String,Object>();
    for (Map<String,Object> map : maps)
    {
      for (Map.Entry<String,Object> e : map.entrySet())
      {
        if (result.put(e.getKey(), e.getValue()) != null)
        {
          throw new IllegalArgumentException("Cluster " + e.getKey() + " is present in multiple maps");
        }
      }
    }
    return result;
  }
}
