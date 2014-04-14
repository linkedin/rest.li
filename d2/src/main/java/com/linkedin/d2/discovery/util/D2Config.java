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
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.properties.ServicePropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.util.PropertyUtil;
import com.linkedin.d2.balancer.zkfs.ZKFSUtil;
import com.linkedin.d2.discovery.PropertyBuilder;
import com.linkedin.d2.discovery.PropertySerializer;
import com.linkedin.d2.discovery.stores.zk.SymlinkUtil;
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
 * colo name, or through the master service alias if they are unaware of the underlying colos. In the
 * latter case, server can opt to point their master service alias to either a hard-coded master colo
 * or a d2 symlink in which master colo can be assigned on the fly. If a server has both master colo and
 * enableSymlink == true set in the config, the latter one will be honored.
 *
 * Sample config for a single-master cluster:
 *
 * <entry key="FooServer">
 *   <map>
 *     <entry key="coloVariants" value="colo1, colo2" />
 *     <entry key="masterColo" value="colo1" />
 *     <entry key="enableSymlink" value="false" />
 *     <entry key="services">
 *       <map>
 *         <entry key="fooService">
 *           <map>
 *             <entry key="path" value="/foo" />
 *           </map>
 *         </entry>
 *       </map>
 *     </entry>
 *   </map>
 *</entry>
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

    final String defaultColo = (String)_clusterDefaults.remove(PropertyKeys.DEFAULT_COLO);

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
    // that also had cluster variants as well as coloVariants.
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
      final Object servicesProperty = clusterConfig.remove(PropertyKeys.SERVICES);
      @SuppressWarnings("unchecked")
      Map<String,Map<String,Object>> servicesConfigs = (Map<String,Map<String,Object>>) servicesProperty;
      final Object clusterVariantProperty = clusterConfig.remove(PropertyKeys.CLUSTER_VARIANTS);
      @SuppressWarnings("unchecked")
      Map<String,Map<String,Object>> clusterVariantConfig = (Map<String,Map<String,Object>>) clusterVariantProperty;
      final Object coloVariantsProperty = clusterConfig.remove(PropertyKeys.COLO_VARIANTS);
      @SuppressWarnings("unchecked")
      List<String> coloVariants = (List<String>) coloVariantsProperty;
      final String masterColo = (String)clusterConfig.remove(PropertyKeys.MASTER_COLO);
      final String enableSymlinkString = (String)clusterConfig.remove(PropertyKeys.ENABLE_SYMLINK);
      final boolean enableSymlink;

      if (enableSymlinkString != null && "true".equalsIgnoreCase(enableSymlinkString))
      {
        enableSymlink = true;
      }
      else
      {
        enableSymlink = false;
      }

      // do some sanity check for partitions if any
      // Moving handling of partitionProperties before any coloVariant manipulations
      final Object partitionPropertiesProperty = clusterConfig.get(PropertyKeys.PARTITION_PROPERTIES);
      @SuppressWarnings("unchecked")
      Map<String, Object> partitionProperties = (Map<String, Object>) partitionPropertiesProperty;
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
      if (coloVariants == null || (coloVariants.size() == 1 && coloVariants.contains("")))
      {
        coloVariants = Collections.singletonList("");
      }
      else
      {
        // some sanity checks to make sure that the default and master colos are listed as
        // one of the peer colos, if applicable.
        if (!coloVariants.contains(defaultColo))
        {
          throw new IllegalStateException("The default colo: " + defaultColo + " is not one of the peer colos = " + coloVariants);
        }

        if (masterColo != null && !coloVariants.contains(masterColo) && !enableSymlink)
        {
          throw new IllegalStateException("The master colo: " + masterColo + " is not one of the peer colos = " + coloVariants);
        }
      }

      boolean defaultServicesCreated = false;
      for (String colo : coloVariants)
      {
        // the coloClusterName will be equal to the original cluster name if colo is the empty string
        String coloClusterName = D2Utils.addSuffixToBaseName(clusterName, colo);
        // coloServicesConfigs are the set of d2 services in this cluster in this colo
        // for the regular cluster case I could avoid creation of a new HashMap for both coloServicesConfig
        // and coloServiceConfig, as an optimization at the expense of simplicity.
        Map<String,Map<String,Object>> coloServicesConfigs = new HashMap<String, Map<String, Object>>();

        // Only create the default services once, and only when we have an empty colo string or the
        // colo matches the default colo.
        boolean createDefaultServices = (defaultServicesCreated == false)
                                              ? shouldCreateDefaultServices(colo, defaultColo)
                                              : false;

        for (String serviceName : servicesConfigs.keySet())
        {
          // "resource" level config
          Map<String, Object> serviceConfig = servicesConfigs.get(serviceName);

          // There are some cases where we may not want to create colo variants of a particular service
          // We can't remove properties from the serviceConfig here because we might need to loop
          // over it multiple times.
          String createColoVariants = (String)serviceConfig.get(PropertyKeys.HAS_COLO_VARIANTS);
          boolean createColoVariantsForService = shouldCreateColoVariantsForService(colo, createColoVariants);
          String coloServiceName = serviceName;

          final boolean defaultRoutingToMasterColo =
              serviceConfig.containsKey(PropertyKeys.DEFAULT_ROUTING) &&
              PropertyKeys.MASTER_SUFFIX.equals(serviceConfig.get(PropertyKeys.DEFAULT_ROUTING));

          // if the coloServiceName ends up being the same as the serviceName, then we won't create
          // any colo variants of that serviceName.
          if (createColoVariantsForService)
          {
            coloServiceName = D2Utils.addSuffixToBaseName(serviceName, colo);
          }

          final Object transportClientProperty = serviceConfig.get(PropertyKeys.TRANSPORT_CLIENT_PROPERTIES);
          @SuppressWarnings("unchecked")
          Map<String, Object> transportClientConfig = (Map<String, Object>) transportClientProperty;
          serviceConfig.put(PropertyKeys.TRANSPORT_CLIENT_PROPERTIES, transportClientConfig);

          Map<String,Object> coloServiceConfig = new HashMap<String,Object>(serviceConfig);

          // we will create the default services when this is a non-colo aware cluster or when the colo
          // matches the default colo. This, along with the defaultServicesCreated flag, ensures we
          // only create the default services once and simplifies the handleClusterVariants code
          // so it does not have to know about what are the default services.
          if (createDefaultServices && !defaultServicesCreated)
          {
            // we also need to use the createColoVariantsForService flag to control whether to
            // create the Master version of this service.
            if (masterColo != null && createColoVariantsForService)
            {
              // we need to create a "Master" version of this service to point to the current Master
              // Cluster. Why not just use the original service name? We will point the original
              // service name at the local cluster, as well as to make it explicit that requests
              // sent to this service might cross colos, if the master is located in another colo.
              Map<String,Object> masterServiceConfig = new HashMap<String,Object>(serviceConfig);
              String masterServiceName = serviceName + PropertyKeys.MASTER_SUFFIX;
              String masterClusterName;
              if (enableSymlink)
              {
                masterClusterName = D2Utils.getSymlinkNameForMaster(clusterName);
              }
              else
              {
                masterClusterName = D2Utils.addSuffixToBaseName(clusterName, masterColo);
              }
              masterServiceConfig.put(PropertyKeys.CLUSTER_NAME, masterClusterName);
              masterServiceConfig.put(PropertyKeys.SERVICE_NAME, masterServiceName);
              masterServiceConfig.put(PropertyKeys.IS_MASTER_SERVICE, "true");
              coloServicesConfigs.put(masterServiceName, masterServiceConfig);
            }

            // this block will handle:
            // the colo-agnostic service -> colo-specific default cluster mapping (fooService -> FooCluster-WestCoast)
            // the colo-agnostic service -> colo-agnostic cluster mapping (fooService -> FooCluster)
            // the latter only being done for regular clusters, the former only being done for clusters
            // that have coloVariants specified.
            Map<String,Object> regularServiceConfig = new HashMap<String,Object>(serviceConfig);
            if (createColoVariantsForService)
            {
              // we set isDefaultService flag only if it is a multi-colo aware service.
              regularServiceConfig.put(PropertyKeys.IS_DEFAULT_SERVICE, "true");
            }

            final String defaultColoClusterName = clusterNameWithRouting(clusterName,
                                                                         colo,
                                                                         defaultColo,
                                                                         masterColo,
                                                                         defaultRoutingToMasterColo,
                                                                         enableSymlink);

            regularServiceConfig.put(PropertyKeys.CLUSTER_NAME, defaultColoClusterName);
            regularServiceConfig.put(PropertyKeys.SERVICE_NAME, serviceName);
            coloServicesConfigs.put(serviceName, regularServiceConfig);
          } // end if it's the default colo

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
                                     variantToVariantsMapping, masterColo, enableSymlink);
          if (status != 0)
          {
            return status;
          }
        }
        clusterToServiceMapping.put(clusterName, servicesConfigs);

        // if we haven't yet marked the default services as created and we created them in this loop,
        // the set the flag marking the default services for this cluster as created.
        if (!defaultServicesCreated && createDefaultServices == true )
        {
          defaultServicesCreated = true;
        }
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
        final Object clusterListProperty = configGroupMap.get(PropertyKeys.CLUSTER_LIST);
        @SuppressWarnings("unchecked")
        List<String> clusterList = (List<String>) clusterListProperty;

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

  protected static String clusterNameWithRouting(final String clusterName,
                                                 final String destinationColo,
                                                 final String defaultColo,
                                                 final String masterColo,
                                                 final boolean defaultRoutingToMasterColo,
                                                 final boolean enableSymlink)
  {
    final String defaultColoClusterName;
    if ("".matches(destinationColo))
    {
      // If we didn't have an coloVariants for this cluster, make sure to use the original
      // cluster name.
      defaultColoClusterName = clusterName;
    }
    else if (defaultRoutingToMasterColo)
    {
      // If this service is configured to route all requests to the master colo by default
      // then we need to configure the service to use the master colo.
      if (enableSymlink)
      {
        defaultColoClusterName = D2Utils.getSymlinkNameForMaster(clusterName);
      }
      else
      {
        defaultColoClusterName = D2Utils.addSuffixToBaseName(clusterName, masterColo);
      }
    }
    else
    {
      defaultColoClusterName = D2Utils.addSuffixToBaseName(clusterName, defaultColo);
    }
    return defaultColoClusterName;
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
    PartitionProperties.PartitionType partitionType = PropertyUtil.checkAndGetValue(partitionProperties, PropertyKeys.PARTITION_TYPE,
                                                                                    PartitionProperties.PartitionType.class, clusterName);

    switch (partitionType)
    {

      case RANGE:
      {
        if (partitionProperties.get(PropertyKeys.PARTITION_KEY_REGEX) == null)
        {
          _log.error("null partitionKeyRegex for cluster: " + clusterName);
          return PARTITION_CONFIG_ERROR_EXIT_CODE;
        }
        Long partitionSize = PropertyUtil.parseLong(PropertyKeys.PARTITION_SIZE,
                                                    PropertyUtil.checkAndGetValue(partitionProperties, PropertyKeys.PARTITION_SIZE, String.class, clusterName));
        int partitionCount = PropertyUtil.parseInt(PropertyKeys.PARTITION_COUNT,
                                                   PropertyUtil.checkAndGetValue(partitionProperties, PropertyKeys.PARTITION_COUNT, String.class, clusterName));
        Long start = PropertyUtil.parseLong(PropertyKeys.KEY_RANGE_START,
                                            PropertyUtil.checkAndGetValue(partitionProperties, PropertyKeys.KEY_RANGE_START, String.class, clusterName));

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
        partitionProperties.put(PropertyKeys.PARTITION_SIZE, partitionSize);
        partitionProperties.put(PropertyKeys.PARTITION_COUNT, partitionCount);
        partitionProperties.put(PropertyKeys.KEY_RANGE_START, start);
        clusterConfig.put(PropertyKeys.PARTITION_PROPERTIES, partitionProperties);

      }
      break;

      case HASH:
      {
        if (partitionProperties.get(PropertyKeys.PARTITION_KEY_REGEX) == null)
        {
          _log.error("null partitionKeyRegex for cluster: " + clusterName);
          return PARTITION_CONFIG_ERROR_EXIT_CODE;
        }

        int partitionCount = PropertyUtil.parseInt(PropertyKeys.PARTITION_COUNT,
                                                   PropertyUtil.checkAndGetValue(partitionProperties, PropertyKeys.PARTITION_COUNT, String.class, clusterName));
        if (partitionCount < 0)
        {
          _log.error("partition count needs to be non negative");
          return PARTITION_CONFIG_ERROR_EXIT_CODE;
        }
        // replace string with number so that it works with the serializer
        partitionProperties.put(PropertyKeys.PARTITION_COUNT, partitionCount);
        clusterConfig.put(PropertyKeys.PARTITION_PROPERTIES, partitionProperties);
        try
        {
          String algorithm = PropertyUtil.checkAndGetValue(partitionProperties, PropertyKeys.HASH_ALGORITHM, String.class, clusterName);
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
                                    String coloStr,
                                    Map<String,List<String>> variantToVariantsMapping,
                                    String masterColo,
                                    boolean enableSymlink)
  {
    for (String variant : clusterVariantConfig.keySet())
    {
      Map<String,Object> varConfig = clusterVariantConfig.get(variant);
      String variantColoName = D2Utils.addSuffixToBaseName(variant, coloStr);
      String masterColoName;
      if (enableSymlink)
      {
        masterColoName = D2Utils.getSymlinkNameForMaster(variant);
      }
      else
      {
        masterColoName = D2Utils.addSuffixToBaseName(variant, masterColo);
      }
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
        String masterServiceString = (String)varServiceConfig.get(PropertyKeys.IS_MASTER_SERVICE);
        String defaultServiceString = (String)varServiceConfig.get(PropertyKeys.IS_DEFAULT_SERVICE);
        boolean defaultRoutingToMasterColo =
            varServiceConfig.containsKey(PropertyKeys.DEFAULT_ROUTING) &&
                PropertyKeys.MASTER_SUFFIX.equals(varServiceConfig.get(PropertyKeys.DEFAULT_ROUTING));

        if (masterServiceString != null && "true".equalsIgnoreCase(masterServiceString))
        {
          // for master service variants, we want them to point to the master colo.
          varServiceConfig.put(PropertyKeys.CLUSTER_NAME, masterColoName);
        }
        else if (defaultRoutingToMasterColo &&
                    defaultServiceString != null && "true".equalsIgnoreCase(defaultServiceString))
        {
          // for default services whose defaultRouting = Master, we also want them to
          // point to the master colo.
          varServiceConfig.put(PropertyKeys.CLUSTER_NAME, masterColoName);
        }
        else
        {
          // for all other service variants, we want them to point to the colo specific
          // cluster variant name.
          varServiceConfig.put(PropertyKeys.CLUSTER_NAME, variantColoName);
        }

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

  /**
   * Given a colo, and an optional flag that might have been specified in the service properties,
   * determine if we should create colo variants of this service.
   *
   * @param colo the environment we are creating this service for
   * @param createColoVariants flag that was optionally specific in the service properties
   * @return true if should create colo variants for service, false otherwise.
   */
  private boolean shouldCreateColoVariantsForService(String colo, String createColoVariants)
  {
    // checking against the colo is not entirely necessary, as we'd never create colo variants
    // of this service because the existing checks are sufficient. But for clarity and functional
    // correctness, we should return false for clusters that are not colo aware.
    if ("".equals(colo))
    {
      return false;
    }
    if (createColoVariants == null || !("false".equalsIgnoreCase(createColoVariants)))
    {
      // by default we will create the colo variants of the service
      return true;
    }
    else
    {
      // there was an explicit false value instructing us to not create colo variants of this service.
      return false;
    }
  }

  /**
   * Given a colo, and the defaultColo, determine if we should create the default service names.
   * We want to create the service names when:
   * 1. this is a regular cluster (empty string colo)
   * 2. the colo string matches the defaultColo.
   *
   * The reason for only creating the default service names when we are looping through the
   * matching colo is so that the code handling the cluster variants don't need to both:
   * 1. identify default service names, and
   * 2. pass in the defaultColo to construct a default Colo name.
   *
   * @param colo the environment we are creating this service for
   * @param defaultColo the default non-empty colo string.
   * @return true if should create the default services, false otherwise.
   */
  private boolean shouldCreateDefaultServices(String colo, String defaultColo)
  {
    if ("".equals(colo))
    {
      return true;
    }
    // defaultColo should always be set.
    if (defaultColo.equalsIgnoreCase(colo))
    {
      return true;
    }
    return false;
  }
}
