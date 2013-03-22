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


import com.linkedin.common.util.MapUtil;
import com.linkedin.d2.balancer.config.ConfigWriter;
import com.linkedin.d2.balancer.properties.ClusterPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.HashBasedPartitionProperties;
import com.linkedin.d2.balancer.properties.PartitionProperties;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.properties.ServicePropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.util.PropertyUtil;
import com.linkedin.d2.balancer.zkfs.ZKFSUtil;
import com.linkedin.d2.discovery.PropertyBuilder;
import com.linkedin.d2.discovery.PropertySerializer;
import com.linkedin.d2.discovery.stores.zk.ZKConnection;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperPermanentStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * D2Config
 *
 * This tool takes as input some maps containing cluster-service configurations, and writes them out
 * to zookeeper in a format that D2 can use. The inputs are structured so that it is easy to see
 * the d2 services associated with a particular cluster; all those d2 services are grouped under the
 * cluster name. D2, however, expects clients to directly address a D2 service, and so these D2 services
 * are converted into top level entries, with a reference to what cluster they belong to. This
 * provides a level of indirection that allows great flexibility when configuring clusters. For instance,
 * the same client code in two different instances referencing the same d2 service name can be configured
 * to talk to different sets of machines (ie cluster variants) for QOS or isolation purposes.
 *
 * This tool has also been extended to support multiple datacenters (aka colos). While a multi-colo
 * setup should have most communication staying within any particular colo, there may be single-master
 * cases and multi-master cases that require cross colo communication. In those cases, servers may
 * announce themselves in multiple colos using a colo-specific clustername, and clients will be able
 * to direct requests to those remote clusters through a known combination of the service name and
 * colo name.
 *
 * @author Steven Ihde, David Hoa
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
    // original map derived from properties file
    Map<String,Object> clusterServiceConfiguration = merge(_clusterServiceConfigurations);
    // map of clusterName -> cluster configuration
    Map<String,Map<String,Object>> clusters = new HashMap<String,Map<String,Object>>();
    // map of serviceName -> service configuration
    Map<String,Map<String,Object>> services = new HashMap<String,Map<String,Object>>();
    // Ugly. But this is a map of service groups, so it needs to reflect multiple services maps.
    Map<String,Map<String,Map<String,Object>>> serviceVariants = new HashMap<String,Map<String,Map<String,Object>>>();

    // temporary mapping from cluster name to services map, to aid in create cluster variants and
    // service groups.
    Map<String,Map<String,Map<String,Object>>> clusterToServiceMapping = new HashMap<String,Map<String,Map<String,Object>>>();
    int status;
    // temporary mapping from cluster name to the list of colo variants it has.
    Map<String,List<String>> variantToVariantsMapping = new HashMap<String, List<String>>();

    _log.info("basePath: " + _basePath);
    _log.info("clusterDefaults: " + _clusterDefaults);
    _log.info("serviceDefaults: " + _serviceDefaults);

    // the defaultColo can either be passed in via the cluster defaults, or through a
    // constructor argument. any preference?
    String defaultColo = (String)_clusterDefaults.remove(PropertyKeys.DEFAULT_COLO);

    // Prior to supporting colos, we had a double for loop: foreach cluster ... foreach service
    // inside that cluster. This was so users could easily associate a group of services with a
    // particular cluster, and this d2Config class would create a top-level mapping from service to
    // cluster, so that client apps only needed to know the service name, and D2 would take care of
    // mapping it to a particular cluster and machine.
    //
    // In order to support multiple colos, we need to create variations of certain
    // (not all, in fact very few) cluster-service combinations. The complication comes in when we
    // have to support the combination of clusterVariants with coloVariants. While this would be a
    // horrible combination, we need to support the possibility of this.
    //
    // Here are the choices, note that the triple for loop still is effectively a double for loop
    // for most clusters, because most clusters won't have coloVariants. Most clusters won't have
    // coloVariants because there shouldn't be very few needs to make real-time cross-colo requests;
    // it should be fine for local clusters to satisfy local requests.
    //
    // 1. foreach cluster ... foreach service ... foreach coloVariant
    // The benefit of this approach is that it makes it easy to both create the colo-specific
    // service -> colo-specific cluster mapping and the colo-agnostic service -> colo-specific cluster
    // mapping (ie for both the original service name as well as the master service name, if any).
    // The downside is that this algorithm would require extra bookkeeping to create a cluster
    // that also had cluster variants as well as coloVariants. We would have to keep track of
    //
    // 2. foreach cluster ... foreach coloVariant ... foreach service inside that cluster
    // Creating the colo-specific service -> colo-specific cluster mapping is still easy with this
    // approach, and provided all the service->cluster mappings are created inside the service for loop,
    // we will have easy access to all the service-cluster mappings needed to create cluster variants
    // of each coloVariant. The downside of this algorithm is that it's not as obvious where the
    // colo-agnostic service -> colo-specific cluster mappings should be done. Choices would be to
    // create them on the first coloVariant pass, or make a separate loop through the cluster after
    // iterating through all the coloVariants.  The latter is not really a choice because it would
    // mess up creating the cluster variant mappings.
    //
    // 3. keep the double for loop but keep track of which services have colo variants. This
    // possibility requires too much bookkeeping, and none of the steps are easy.
    //
    // Solution 2 is the approach taken below.
    for (String clusterName : clusterServiceConfiguration.keySet())
    {
      @SuppressWarnings("unchecked")
      Map<String,Object> clusterConfig = (Map<String,Object>)clusterServiceConfiguration.get(clusterName);
      clusterConfig.put(PropertyKeys.CLUSTER_NAME, clusterName);
      @SuppressWarnings("unchecked")
      Map<String,Map<String,Object>> servicesConfigs = (Map<String,Map<String,Object>>)clusterConfig.remove(PropertyKeys.SERVICES);
      @SuppressWarnings("unchecked")
      Map<String,Map<String,Object>> clusterVariantConfig = (Map<String,Map<String,Object>>)clusterConfig.remove(PropertyKeys.CLUSTER_VARIANTS);
      @SuppressWarnings("unchecked")
      List<String> coloVariants = (List<String>)clusterConfig.remove(PropertyKeys.COLO_VARIANTS);
      String masterColo = (String)clusterConfig.remove(PropertyKeys.MASTER_COLO);

      // do some sanity check for partitions if any
      // Moving handling of partitionProperties before any coloVariant manipulations
      @SuppressWarnings("unchecked")
      Map<String, Object> partitionProperties = (Map<String, Object>)clusterConfig.get(PropertyKeys.PARTITION_PROPERTIES);
      if (partitionProperties != null)
      {
        status = handlePartitionProperties(partitionProperties, clusterConfig, clusterName);
        if (status != 0)
        {
          return status;
        }
      }

      // rather than handling the coloVariant case separately from the regular cluster case, we will
      // treat regular clusters as having an empty-string coloVariant list. This allows us to have a
      // single codepath that creates all the structures we need, rather than duplicating code with
      // lots of if/else.
      if (coloVariants == null)
      {
        coloVariants = Collections.singletonList("");
      }

      boolean firstColoVariant = true;
      for (String colo : coloVariants)
      {
        // the coloClusterName will be equal to the original cluster name if colo is the empty string
        String coloClusterName = D2Utils.addSuffixToBaseName(clusterName, colo);
        // coloServicesConfigs are the set of d2 services in this cluster in this colo
        // for the regular cluster case I could avoid creation of a new HashMap for both coloServicesConfig
        // and coloServiceConfig, as an optimization at the expense of simplicity.
        Map<String,Map<String,Object>> coloServicesConfigs = new HashMap<String, Map<String, Object>>();

        for (String serviceName : servicesConfigs.keySet())
        {
          String coloServiceName = D2Utils.addSuffixToBaseName(serviceName, colo);
          Map<String, Object> serviceConfig = servicesConfigs.get(serviceName);

          @SuppressWarnings("unchecked")
          Map<String, Object> transportClientConfig = (Map<String, Object>)serviceConfig.get(PropertyKeys.
                                                                                           TRANSPORT_CLIENT_PROPERTIES);
          serviceConfig.put(PropertyKeys.TRANSPORT_CLIENT_PROPERTIES, transportClientConfig);

          Map<String,Object> coloServiceConfig = new HashMap<String,Object>(serviceConfig);

          if (firstColoVariant)
          {
            if (masterColo != null)
            {
              // we need to create a "Master" version of this service to point to the current Master
              // Cluster. Why not just use the original service name? We will point the original
              // service name at the local cluster, as well as to make it explicit that requests
              // sent to this service might cross colos, if the master is located in another colo.
              Map<String,Object> masterServiceConfig = new HashMap<String,Object>(serviceConfig);
              String masterServiceName = serviceName + PropertyKeys.MASTER_SUFFIX;
              String masterClusterName = D2Utils.addSuffixToBaseName(clusterName, masterColo);
              masterServiceConfig.put(PropertyKeys.CLUSTER_NAME, masterClusterName);
              masterServiceConfig.put(PropertyKeys.SERVICE_NAME, masterServiceName);
              coloServicesConfigs.put(masterServiceName, masterServiceConfig);
            }

            // this block will handle:
            // the colo-agnostic service -> colo-specific default cluster mapping (fooService -> FooCluster-WestCoast)
            // the colo-agnostic service -> colo-agnostic cluster mapping (fooService -> FooCluster)
            // the latter only being done for regular clusters, the former only being done for clusters
            // that have coloVariants specified.
            Map<String,Object> regularServiceConfig = new HashMap<String,Object>(serviceConfig);
            // if we didn't have an coloVariants for this cluster, make sure to use the original
            // cluster name
            String defaultColoClusterName = D2Utils.addSuffixToBaseName(clusterName, ("".matches(colo) ? null :defaultColo));
            regularServiceConfig.put(PropertyKeys.CLUSTER_NAME, defaultColoClusterName);
            regularServiceConfig.put(PropertyKeys.SERVICE_NAME, serviceName);
            coloServicesConfigs.put(serviceName, regularServiceConfig);
          } // end if it's the first colo variant

          if (!serviceName.equals(coloServiceName))
          {
            // this block will handle:
            // the colo-specific service-> colo-specific cluster mapping (fooService-WestCoast -> FooCluster-WestCoast,
            // fooService-EastCoast -> FooCluster-EastCoast)
            coloServiceConfig.put(PropertyKeys.CLUSTER_NAME, coloClusterName);
            coloServiceConfig.put(PropertyKeys.SERVICE_NAME, coloServiceName);
            coloServicesConfigs.put(coloServiceName, coloServiceConfig);
          }

        } // end for each service

        status = addServicesToServicesMap(coloServicesConfigs, services, coloClusterName);
        if (status != NO_ERROR_EXIT_CODE)
        {
          return status;
        }
        // Now that we've created colo-specific service to colo-specific cluster mappings, we now need
        // to actually create those colo-specific clusters.
        Map<String,Object> coloClusterConfig = clusterConfig;
        if (clusterName != coloClusterName)
        {
          coloClusterConfig = new HashMap<String,Object>(clusterConfig);
          coloClusterConfig.put(PropertyKeys.CLUSTER_NAME, coloClusterName);
        }
        clusters.put(coloClusterName, coloClusterConfig);


        // for each cluster variant, copy the default cluster properties into this variant cluster, and
        // add it to the cluster list. It is not necessary that the default cluster be added to the cluster
        // list before the cluster variants.
        if (clusterVariantConfig != null)
        {
          Map<String,Map<String,Object>> coloClusterVariantConfig = new HashMap<String,Map<String,Object>>(clusterVariantConfig);
          status = handleClusterVariants(coloClusterVariantConfig, clusterConfig, clusters,
                                     coloServicesConfigs,  clusterToServiceMapping, colo,
                                     variantToVariantsMapping);
          if (status != 0)
          {
            return status;
          }
        }
        clusterToServiceMapping.put(clusterName, servicesConfigs);
        firstColoVariant = false;
      } // end for each colo variant
    } // end for each cluster

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
        String type = (String)configGroupMap.get(PropertyKeys.TYPE);
        @SuppressWarnings("unchecked")
        List<String> clusterList = (List<String>)configGroupMap.get(PropertyKeys.CLUSTER_LIST);

        // create an alternate service table for the services specified by these cluster variants
        for (Iterator<String> iter = clusterList.listIterator(); iter.hasNext();)
        {
          String clusterItem = iter.next();

          List<String> coloClusterVariantList = variantToVariantsMapping.get(clusterItem);
          if (coloClusterVariantList == null)
          {
            // the service group had an unknown cluster!
            _log.error("Unknown cluster specified: " + clusterItem);
            return EXCEPTION_EXIT_CODE;
          }

          // we need to iterate through the coloVariants of this clusterVariant, and add the services
          // in those coloVariants to this service group's list of services.
          for (String coloClusterVariant : coloClusterVariantList)
          {
            Map<String,Map<String,Object>> candidateServices = clusterToServiceMapping.get(coloClusterVariant);

            if (candidateServices == null)
            {
              // the service group had an unknown cluster!
              _log.error("Unknown cluster specified: " + coloClusterVariant);
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
        }

        if(PropertyKeys.CLUSTER_VARIANTS_LIST.equals(type))
        {
          // start from the full list of services, and then overwrite the services specified by the
          // cluster variants.
          Map<String,Map<String,Object>> fullServiceList = new HashMap<String,Map<String,Object>>(services);
          fullServiceList.putAll(servicesGroupConfig);
          serviceVariants.put(serviceGroup, fullServiceList);
        }
        else if (PropertyKeys.FULL_CLUSTER_LIST.equals(type))
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

    try
    {
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
    finally
    {
      try
      {
        _zkConnection.shutdown();
      }
      catch (InterruptedException e)
      {
        Thread.currentThread().interrupt();
        _log.warn("ZooKeeper shutdown interrupted", e);
      }
    }
  }

  private static Map<String, Object> getTransportClientConfigFromCluster(Map<String, Object> clusterConfig)
  {
    Map<String, Object> transportClientConfig = new HashMap<String, Object>();
    insertKeyToMapIfKeyExistInAnotherMap(transportClientConfig, clusterConfig, PropertyKeys.HTTP_REQUEST_TIMEOUT);
    insertKeyToMapIfKeyExistInAnotherMap(transportClientConfig, clusterConfig, PropertyKeys.HTTP_MAX_RESPONSE_SIZE);
    insertKeyToMapIfKeyExistInAnotherMap(transportClientConfig, clusterConfig, PropertyKeys.HTTP_POOL_SIZE);
    insertKeyToMapIfKeyExistInAnotherMap(transportClientConfig, clusterConfig, PropertyKeys.HTTP_IDLE_TIMEOUT);
    insertKeyToMapIfKeyExistInAnotherMap(transportClientConfig, clusterConfig, PropertyKeys.HTTP_SHUTDOWN_TIMEOUT);
    insertKeyToMapIfKeyExistInAnotherMap(transportClientConfig, clusterConfig, PropertyKeys.HTTP_SSL_CONTEXT);
    insertKeyToMapIfKeyExistInAnotherMap(transportClientConfig, clusterConfig, PropertyKeys.HTTP_SSL_PARAMS);
    insertKeyToMapIfKeyExistInAnotherMap(transportClientConfig, clusterConfig, PropertyKeys.OLD_CLUSTER_GET_TIMEOUT);
    insertKeyToMapIfKeyExistInAnotherMap(transportClientConfig, clusterConfig, PropertyKeys.OLD_CLUSTER_REQUEST_TIMEOUT);
    insertKeyToMapIfKeyExistInAnotherMap(transportClientConfig, clusterConfig, PropertyKeys.OLD_CLUSTER_MAX_RESPONSE_SIZE);
    insertKeyToMapIfKeyExistInAnotherMap(transportClientConfig, clusterConfig, PropertyKeys.OLD_CLUSTER_POOL_SIZE);
    insertKeyToMapIfKeyExistInAnotherMap(transportClientConfig, clusterConfig, PropertyKeys.OLD_CLUSTER_IDLE_TIMEOUT);
    insertKeyToMapIfKeyExistInAnotherMap(transportClientConfig, clusterConfig, PropertyKeys.OLD_CLUSTER_SHUTDOWN_TIMEOUT);
    insertKeyToMapIfKeyExistInAnotherMap(transportClientConfig, clusterConfig, PropertyKeys.OLD_CLUSTER_SSL_CONTEXT);
    insertKeyToMapIfKeyExistInAnotherMap(transportClientConfig, clusterConfig, PropertyKeys.OLD_CLUSTER_SSL_PARAMS);
    if (!transportClientConfig.isEmpty())
    {
      return transportClientConfig;
    }
    else
    {
      return null;
    }
  }

  private static void insertKeyToMapIfKeyExistInAnotherMap(Map<String,Object> toBeInserted, Map<String,Object> originalMap,
                                                    String key)
  {
    if (originalMap.containsKey(key))
    {
      toBeInserted.put(key, originalMap.get(key));
    }
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

  private int handlePartitionProperties(Map<String, Object> partitionProperties,
                                         Map<String,Object> clusterConfig,
                                         String clusterName)
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
    return NO_ERROR_EXIT_CODE;
  }

  // clusterToServiceMapping will store the servicesConfig using the clusterVariant name as the key.
  // with colos, we need to modify the clusterVariant name, and this will be called for each cluster
  // and for each colo (if applicable)
  private int handleClusterVariants(Map<String,Map<String,Object>> clusterVariantConfig,
                                    Map<String,Object> clusterConfig,
                                    Map<String,Map<String,Object>> clusters,
                                    Map<String,Map<String,Object>> servicesConfigs,
                                    Map<String,Map<String,Map<String,Object>>> clusterToServiceMapping,
                                    String coloStr, Map<String,List<String>> variantToVariantsMapping)
  {
    for (String variant : clusterVariantConfig.keySet())
    {
      Map<String,Object> varConfig = clusterVariantConfig.get(variant);
      String variantColoName = D2Utils.addSuffixToBaseName(variant, coloStr);
      // clusterConfig is the default cluster's info, and varConfig is this cluster variant's info.
      // We are copying from clusterConfig into varConfig if there is no such property in varConfig.
      varConfig = ConfigWriter.merge(varConfig, clusterConfig);
      // We put the cluster name inside varConfig after the merge has copied the Map to a new object.
      varConfig.put(PropertyKeys.CLUSTER_NAME, variantColoName);
      Map<String,Object> oldCluster = clusters.put(variantColoName, varConfig);
      if(oldCluster != null)
      {
        _log.error("Cluster variant name: " + variantColoName + " is not unique!");
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
        varServiceConfig.put(PropertyKeys.CLUSTER_NAME, variantColoName);
        varServicesConfig.put(entry.getKey(), varServiceConfig);
      }
      clusterToServiceMapping.put(variantColoName, varServicesConfig);

      // store a temporary mapping between the cluster variants and the colo-specific
      // cluster variants. We need this because the service groups only refer to the cluster
      // variants, and we need a mapping between the original cluster variant name and the
      // new colo-specific clusterVariant name generated inside handleClusterVariants(). e.g.
      // if we had a cluster FooCluster in colos WestCoast and EastCoast, with a cluster variant
      // named FooVariant1, then we will have created cluster variants named FooVariant1-WestCoast
      // and FooVariant1-EastCoast. We want to create a mapping between FooVariant1, which will be
      // referenced in a service group, to clusterVariants FooVariant1-WestCoast and
      // FooVariant1-EastCoast
      //
      // Note that for regular (ie non-colo aware clusters) we still create a mapping between
      // itself and itself, ie BarVariant -> list that includes BarVariant.
      List<String> variantsList = variantToVariantsMapping.get(variant);
      if (variantsList != null)
      {
        variantsList.add(variantColoName);
      }
      else
      {
        variantsList = new ArrayList<String>();
        variantsList.add(variantColoName);
        variantToVariantsMapping.put(variant, variantsList);
      }
    } // end for each cluster variant
    return NO_ERROR_EXIT_CODE;
  }

  private int addServicesToServicesMap(Map<String,Map<String,Object>> servicesConfigs,
                                        Map<String,Map<String,Object>> services,
                                        String clusterName)
  {
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
    return NO_ERROR_EXIT_CODE;
  }
}
