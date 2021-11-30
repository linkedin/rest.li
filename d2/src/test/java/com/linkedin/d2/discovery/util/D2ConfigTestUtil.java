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

package com.linkedin.d2.discovery.util;


import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.util.JacksonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class D2ConfigTestUtil
{
  private static final Logger _log = LoggerFactory.getLogger(D2ConfigTestUtil.class);

  private static int _sessionTimeout = 10000;
  private static String _basePath = "/d2";
  private static long _timeout = 5000;
  private static int _retryLimit = 10;
  private static int _requestTimeout = 10000;
  private static int _updateIntervalsMs = 5000;
  private static int _maxClusterLatencyWithoutDegrading = 500;
  private static double _defaultSuccessfulTransmissionWeight = 1.0;
  private static int _pointsPerWeight = 100;
  private static String _prioritizedSchemes = "http";
  private static List<String> _loadBalancerStrategyList = Arrays.asList("degrader", "degraderV3");
  private Map<String,Object> _clusterProperties = new HashMap<>();
  private Map<String, Object> _clusterDefaults = new HashMap<>();
  private Map<String, Object> _serviceDefaults = new HashMap<>();
  private Map<String,Object> _loadBalancerStrategyProperties = new HashMap<>();

  private Map<String, Object> _clusterServiceConfigurations = new HashMap<>();
  private Map<String, Object> _extraClusterServiceConfigurations = new HashMap<>();
  private Map<String, Object> _serviceVariants = new HashMap<>();

  private boolean _useDeltaWrite = false;
  private int _maxOutstandingWrites = 1;

  public D2ConfigTestUtil()
  {
  }

  public D2ConfigTestUtil(Map<String,List<String>> clustersData)
  {
    setDefaults();
    generateClusters(clustersData);
  }

  public D2ConfigTestUtil(Map<String,List<String>> clustersData, String defaultColo,
                          Map<String,Map<String,Object>> extraClusterProperties)
  {
    this(clustersData, defaultColo, extraClusterProperties, Collections.<String>emptySet());
  }

  public D2ConfigTestUtil(Map<String,List<String>> clustersData, String defaultColo,
                          Map<String,Map<String,Object>> extraClusterProperties,
                          Set<String> servicesWithDefaultRoutingToMaster)
  {
    this(clustersData, defaultColo, extraClusterProperties, new HashMap<>(),
         servicesWithDefaultRoutingToMaster);
  }

  public D2ConfigTestUtil(Map<String,List<String>> clustersData, String defaultColo,
                          Map<String,Map<String,Object>> extraClusterProperties,
                          Map<String,List<String>> serviceGroupsData)
  {
    this(clustersData, defaultColo, extraClusterProperties, serviceGroupsData,
         Collections.<String>emptySet());
  }

  public D2ConfigTestUtil(Map<String,List<String>> clustersData, String defaultColo,
                          Map<String,Map<String,Object>> extraClusterProperties,
                          Map<String,List<String>> serviceGroupsData,
                          Set<String> servicesWithDefaultRoutingToMaster)
  {
    this(clustersData, defaultColo, extraClusterProperties, serviceGroupsData, null,
         servicesWithDefaultRoutingToMaster);
  }

  public D2ConfigTestUtil(Map<String,List<String>> clustersData, String defaultColo,
                          Map<String,Map<String,Object>> extraClusterProperties,
                          Map<String,List<String>> serviceGroupsData,
                          List<String> excludeServiceList)
  {
    this(clustersData, defaultColo, extraClusterProperties, serviceGroupsData, excludeServiceList,
         Collections.<String>emptySet());
  }

  public D2ConfigTestUtil(Map<String,List<String>> clustersData, String defaultColo,
                          Map<String,Map<String,Object>> extraClusterProperties,
                          Map<String,List<String>> serviceGroupsData,
                          List<String> excludeServiceList,
                          Set<String> servicesWithDefaultRoutingToMaster)
  {
    _loadBalancerStrategyList = Arrays.asList(new String[]{"degrader","degraderV3"});
    setDefaults();
    _clusterDefaults.put("defaultColo", defaultColo);
    generateClusters(clustersData, extraClusterProperties, serviceGroupsData, excludeServiceList,
                     servicesWithDefaultRoutingToMaster);
  }

  public D2ConfigTestUtil(Map<String, List<String>> clusterData, Map<String, Object> partitionData)
  {
    _loadBalancerStrategyList = Arrays.asList(new String[]{"degraderV3"});
    setDefaults();
    generateClusters(clusterData, partitionData);
  }

  public D2ConfigTestUtil(String mainClusterName,
                          Map<String,String> servicesData,
                          Map<String,String> serviceGroupsData)
  {
    this(mainClusterName, servicesData, serviceGroupsData, Collections.<String>emptySet());
  }

  public D2ConfigTestUtil(String mainClusterName,
                          Map<String,String> servicesData,
                          Map<String,String> serviceGroupsData,
                          Set<String> servicesWithDefaultRoutingToMaster)
  {
    setDefaults();
    generateClusters(mainClusterName, servicesData, serviceGroupsData,
                     servicesWithDefaultRoutingToMaster);
  }

  public void setDefaults()
  {
    setClusterProperties();
    setClusterDefaults();
    setLoadBalancerStrategyProperties();
    setServiceDefaults();
  }

  public Map<String, Object> getClusterDefaults()
  {
    return _clusterDefaults;
  }

  public Map<String, Object> getServiceDefaults()
  {
    return _serviceDefaults;
  }

  public Map<String, Object> getClusterProperties()
  {
    return _clusterProperties;
  }

  public Map<String, Object> getClusterServiceConfigurations()
  {
    return _clusterServiceConfigurations;
  }

  public int getSessionTimeout ()
  {
    return _sessionTimeout;
  }

  public String getBasePath ()
  {
   return  _basePath;
  }

  public List<String> getLoadBalancerStrategyList()
  {
    return _loadBalancerStrategyList;
  }

  public long getTimeout ()
  {
    return _timeout;
  }

  public int getRequestTimeout ()
  {
    return _requestTimeout;
  }

  public int getUpdateIntervalsMs ()
  {
    return _updateIntervalsMs;
  }

  public int getMaxClusterLatencyWithoutDegrading ()
  {
    return _maxClusterLatencyWithoutDegrading;
  }

  public String getPrioritizedSchemes ()
  {
    return _prioritizedSchemes;
  }

  public void setClusterServiceConfigurations( Map<String, Object> clusterServiceConfigurations)
  {
    _clusterServiceConfigurations = clusterServiceConfigurations;
  }

  public void setServiceVariants(Map<String, Object> serviceVariants)
  {
    _serviceVariants = serviceVariants;
  }

  public double setDefaultSuccessfulTransmissionWeight ()
  {
    return _defaultSuccessfulTransmissionWeight;
  }

  public int setPointsPerWeight ()
  {
    return _pointsPerWeight;
  }

  public void setSessionTimeout (int sessionTimeout)
  {
    _sessionTimeout = sessionTimeout;
  }

  public void setBasePath (String basePath)
  {
    _basePath = basePath;
  }

  public void setTimeout (long timeout )
  {
    _timeout = timeout;
  }

  public void setRequestTimeout (int requestTimeout)
  {
    _requestTimeout = requestTimeout;
  }

  public void setUpdateIntervalsMs (int updateIntervalsMs)
  {
    _updateIntervalsMs = updateIntervalsMs;
  }

  public void setMaxClusterLatencyWithoutDegrading (int maxClusterLatencyWithoutDegrading)
  {
    _maxClusterLatencyWithoutDegrading = maxClusterLatencyWithoutDegrading;
  }

  public void setDefaultSuccessfulTransmissionWeight (double defaultSuccessfulTransmissionWeight)
  {
     _defaultSuccessfulTransmissionWeight= defaultSuccessfulTransmissionWeight;
  }

  public void setPointsPerWeight (int pointsPerWeight)
  {
    _pointsPerWeight = pointsPerWeight;
  }

  public void setClusterProperties()
  {
    // Cluster Defaults
    _clusterProperties.put("requestTimeout", String.valueOf(_requestTimeout));
  }

  public void setClusterProperties(int getTimeOut, int requestTimeout)
  {
    // Cluster Defaults
    _clusterProperties.put("requestTimeout", String.valueOf(requestTimeout));
  }

  public void setClusterDefaults()
  {
    _clusterDefaults.put("prioritizedSchemes", Arrays.asList(new String[]{_prioritizedSchemes}));
    _clusterDefaults.put("properties",_clusterProperties);
  }

  public void setClusterDefaults(String _prioritizedSchemes, String _clusterProperties)
  {
    _clusterDefaults.put("prioritizedSchemes", Arrays.asList(new String[]{_prioritizedSchemes}));
    _clusterDefaults.put("properties",_clusterProperties);
  }

  public void setLoadBalancerStrategyProperties()
  {
    _loadBalancerStrategyProperties.put("updateIntervalsMs", String.valueOf(_updateIntervalsMs));
    _loadBalancerStrategyProperties.put("maxClusterLatencyWithoutDegrading", String.valueOf(_maxClusterLatencyWithoutDegrading));
    _loadBalancerStrategyProperties.put("defaultSuccessfulTransmissionWeight", String.valueOf(_defaultSuccessfulTransmissionWeight));
    _loadBalancerStrategyProperties.put("pointsPerWeight", String.valueOf(_pointsPerWeight));
  }

  public void setLoadBalancerStrategyProperties(int updateIntervalsMs, int maxClusterLatencyWithoutDegrading, double defaultSuccessfulTransmissionWeight, int pointsPerWeight)
  {
    _loadBalancerStrategyProperties.put("updateIntervalsMs", String.valueOf(_updateIntervalsMs));
    _loadBalancerStrategyProperties.put("maxClusterLatencyWithoutDegrading", String.valueOf(_maxClusterLatencyWithoutDegrading));
    _loadBalancerStrategyProperties.put("defaultSuccessfulTransmissionWeight", String.valueOf(_defaultSuccessfulTransmissionWeight));
    _loadBalancerStrategyProperties.put("pointsPerWeight", String.valueOf(_pointsPerWeight));
  }

  public void setServiceDefaults()
  {
    _serviceDefaults.put("loadBalancerStrategyProperties", _loadBalancerStrategyProperties);
    _serviceDefaults.put("loadBalancerStrategyList", _loadBalancerStrategyList);
  }

  public void setServiceDefaults(List<String> loadBalancerStrategyList)
  {
    _serviceDefaults.put("loadBalancerStrategyProperties", _loadBalancerStrategyProperties);
    _serviceDefaults.put("loadBalancerStrategyList", loadBalancerStrategyList);
  }

  public void setUseDeltaWrite(boolean useDeltaWrite)
  {
    _useDeltaWrite = useDeltaWrite;
  }

  public void setMaxOutstandingWrites(int maxOutstandingWrites)
  {
    _maxOutstandingWrites = maxOutstandingWrites;
  }

  public void generateClusters(String clusterNamePrefix, String serviceNamePrefix, int totalClusters, int servicesPerCluster)
  {
    //Cluster Service Configurations
    for (int i=1; i <= totalClusters+1; i++)
    {
      _log.info("Creating cluster data: cluster"+i);
      Map<String, Object> services = new HashMap<>();
      services.put("services",generateServicesMap(servicesPerCluster, serviceNamePrefix+i, null));

      _clusterServiceConfigurations.put(clusterNamePrefix+i, services);
    }
  }

  public void generateClusters(Map<String,List<String>> clustersData)
  {
    generateClusters(clustersData, null, new HashMap<>());
  }

  public void generateClusters(Map<String,List<String>> clustersData, Map<String,Map<String,Object>> clustersProperties,
                               Map<String,List<String>> serviceGroupsData)
  {
    generateClusters(clustersData, clustersProperties, serviceGroupsData, null,
                     Collections.<String>emptySet());
  }
  public void generateClusters(Map<String,List<String>> clustersData, Map<String,Map<String,Object>> clustersProperties,
                               Map<String,List<String>> serviceGroupsData, List<String> excludeServiceList,
                               Set<String> servicesWithDefaultRoutingToMaster)
  {

    //Cluster Service Configurations
    for (String clusterName : clustersData.keySet())
    {
      _log.info("Creating cluster data:"+clusterName);
      Map<String, Object> services = new HashMap<>();
      Map<String, Object> tmps = new HashMap<>();

      for (String serviceName : clustersData.get(clusterName))
      {
        Map<String, Object> service = new HashMap<>();
        service.put("path","/"+serviceName);
        if (excludeServiceList != null && excludeServiceList.contains(serviceName))
        {
          service.put(PropertyKeys.HAS_COLO_VARIANTS, "false");
        }
        if (servicesWithDefaultRoutingToMaster.contains(serviceName))
        {
          service.put(PropertyKeys.DEFAULT_ROUTING, PropertyKeys.MASTER_SUFFIX);
        }
        tmps.put(serviceName, service);
      }
      services.put("services",tmps);

      // clusterProperties like masterColo, peerColos, clusterVariants
      if (clustersProperties != null)
      {
        Map<String,Object> clusterProperties = clustersProperties.get(clusterName);
        if (clusterProperties != null)
        {
          services.putAll(clusterProperties);
        }
      }

      _clusterServiceConfigurations.put(clusterName, services);

      // Service variants
      for (String serviceGroupName : serviceGroupsData.keySet())
      {
        Map<String, Object> serviceGroup = new HashMap<>();
        serviceGroup.put("type", "clusterVariantsList");
        serviceGroup.put("clusterList", serviceGroupsData.get(serviceGroupName));
        _serviceVariants.put(serviceGroupName, serviceGroup);
      }
    }
  }

  public void generateClusters(Map<String, List<String>> clustersData,
                               Map<String, Object> partitionData)
  {
    //Cluster Service Configurations
    for (String clusterName : clustersData.keySet())
    {
      _log.info("Creating cluster data:"+clusterName);
      final Map<String, Object> services = new HashMap<>();
      final Map<String, Object> tmps = new HashMap<>();

      for (String serviceName : clustersData.get(clusterName))
      {
        final Map<String, Object> service = new HashMap<>();
        service.put("path","/"+serviceName);
        tmps.put(serviceName, service);
      }
      services.put("services",tmps);
      services.putAll(partitionData);

      _clusterServiceConfigurations.put(clusterName, services);
    }
  }

  public void generateClusters(String mainClusterName,
                               Map<String,String> servicesData,
                               Map<String,String> serviceGroupsData,
                               Set<String> servicesWithDefaultRoutingToMaster)
  {
    //Cluster Service Configurations
    // Services
    Map<String, Object> services = new HashMap<>();
    Map<String, Object> sp = new HashMap<>();

    for (String serviceName : servicesData.keySet())
    {
      Map<String, Object> service = new HashMap<>();
      service.put("path","/"+servicesData.get(serviceName));
      if (servicesWithDefaultRoutingToMaster.contains(serviceName))
      {
        service.put(PropertyKeys.DEFAULT_ROUTING, PropertyKeys.MASTER_SUFFIX);
      }
      sp.put(serviceName, service);
    }
    services.put("services",sp);

    // Cluster Variants
    Map<String, Object> clusterVariants = new HashMap<>();

    for (String clusterName : serviceGroupsData.values())
    {
      clusterVariants.put(clusterName, new HashMap<>());
    }
    services.put("clusterVariants",clusterVariants);

    // Clusters
    _clusterServiceConfigurations.put(mainClusterName, services);

    // Service variants
    for (String serviceGroupName : serviceGroupsData.keySet())
    {
      Map<String, Object> serviceGroup = new HashMap<>();
      serviceGroup.put("type", "clusterVariantsList");
      serviceGroup.put("clusterList", Arrays.asList(serviceGroupsData.get(serviceGroupName)));
      _serviceVariants.put(serviceGroupName, serviceGroup);
    }
  }

  public static Map<String,Object> generateServicesMap(int totalServices, String serviceNamePrefix, String servicePath)
  {
    Map<String, Object> services = new HashMap<>();

    for (int j=1; j <= totalServices+1; j++)
    {
      String serviceName = serviceNamePrefix+"_"+j;
      Map<String, Object> service = new HashMap<>();
      if (servicePath == null)
      {
        servicePath = serviceName;
      }
      service.put("path","/"+servicePath);
      services.put(serviceName, service);
    }

    return services;
  }

  public static Map<String, Object>  getD2ConfigDataMap(String jsonConfigData) throws Exception
  {
    @SuppressWarnings("unchecked")
    Map<String, Object> configMap = (Map<String, Object>) JacksonUtil.getObjectMapper().readValue(jsonConfigData, HashMap.class);

    return configMap;
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object>  getClusterServiceConfiguration(String jsonConfigData) throws Exception
  {
    Map<String, Object> configMap = (Map<String, Object>) JacksonUtil.getObjectMapper().readValue(jsonConfigData, HashMap.class);

    return (Map<String, Object>) configMap.get("clusterServiceConfigurations");
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object>  getClusterDefaults(String jsonConfigData) throws Exception
  {
    Map<String, Object> configMap = (Map<String, Object>) JacksonUtil.getObjectMapper().readValue(jsonConfigData, HashMap.class);

    return (Map<String, Object>) configMap.get("clusterDefaults");
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object>  getServiceDefaults(String jsonConfigData) throws Exception
  {
    Map<String, Object> configMap = (Map<String, Object>) JacksonUtil.getObjectMapper().readValue(jsonConfigData, HashMap.class);

    return (Map<String, Object>) configMap.get("serviceDefaults");
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object>  getExtraClusterServiceConfigurations(String jsonConfigData) throws Exception
  {
    Map<String, Object> configMap = (Map<String, Object>) JacksonUtil.getObjectMapper().readValue(jsonConfigData, HashMap.class);

    return (Map<String, Object>) configMap.get("extraClusterServiceConfigurations");
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object>  getServiceVariants(String jsonConfigData) throws Exception
  {
    Map<String, Object> configMap = (Map<String, Object>) JacksonUtil.getObjectMapper().readValue(jsonConfigData, HashMap.class);

    return (Map<String, Object>) configMap.get("serviceVariants");
  }

  public int runDiscovery(String zkHosts) throws IOException, InterruptedException, URISyntaxException, Exception
  {
    D2Config discovery = new D2Config(zkHosts, _sessionTimeout, _basePath,
                                      _timeout, _retryLimit,
                                      _clusterDefaults,
                                      _serviceDefaults,
                                      _clusterServiceConfigurations,
                                      _extraClusterServiceConfigurations,
                                      _serviceVariants,
                                      _useDeltaWrite,
                                      _maxOutstandingWrites);

    int exitCode = discovery.configure();

   return exitCode;
  }

}
