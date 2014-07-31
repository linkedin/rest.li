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

package com.linkedin.d2.balancer.util;


import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.clients.DynamicClient;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ClusterPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.ServicePropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.properties.UriPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.UriPropertiesMerger;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyFactoryV2;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyFactoryV2_1;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyFactoryV3;
import com.linkedin.d2.balancer.strategies.random.RandomLoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.zkfs.ZKFSComponentFactory;
import com.linkedin.d2.balancer.zkfs.ZKFSLoadBalancer;
import com.linkedin.d2.balancer.zkfs.ZKFSTogglingLoadBalancerFactoryImpl;
import com.linkedin.d2.balancer.zkfs.ZKFSUtil;
import com.linkedin.d2.discovery.PropertySerializer;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import com.linkedin.d2.discovery.event.PropertyEventBusImpl;
import com.linkedin.d2.discovery.event.PropertyEventThread.PropertyEventShutdownCallback;
import com.linkedin.d2.discovery.stores.PropertyStore;
import com.linkedin.d2.discovery.stores.PropertyStoreException;
import com.linkedin.d2.discovery.stores.file.FileStore;
import com.linkedin.d2.discovery.stores.zk.ZKConnection;
import com.linkedin.d2.discovery.stores.zk.ZooKeeper;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperEphemeralStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperPermanentStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperPropertyMerger;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperStore;
import com.linkedin.d2.discovery.util.D2Config;
import com.linkedin.d2.jmx.JmxManager;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.util.NamedThreadFactory;

import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.jvmstat.monitor.HostIdentifier;
import sun.jvmstat.monitor.MonitoredHost;


public class LoadBalancerClientCli
{
  private ZKFSLoadBalancer _zkfsLoadBalancer;
  private ZKConnection _zkclient;
  private File _tmpDir;
  private static final String CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";
  private static final long             TIMEOUT = 5000;
  private static final int      SESSION_TIMEOUT = 60000;
  private String            _zkConnectionString = null;
  private String                      _d2path   = null;
  private String                      _cluster  = null;
  private String                      _service  = "";
  private String                      _method   = "";
  private String                      _request  = null;
  private DynamicClient                 _client = null;
  private static String             _tmpdirName = "temp-d2TmpFileStore" +  Long.toString(System.nanoTime());
  private ZooKeeperPermanentStore<ClusterProperties> _zkClusterRegistry = null;
  private ZooKeeperPermanentStore<ServiceProperties> _zkServiceRegistry = null;
  private ZooKeeperEphemeralStore<UriProperties>         _zkUriRegistry = null;

  private static final Options OPTIONS = new Options();
  private static final Logger _log = LoggerFactory.getLogger(LoadBalancerClientCli.class);

  public static void main(String[] args) throws Exception
  {
    new LoadBalancerClientCli(args);
  }

  public static List<String> asList(String... args)
  {
    return Arrays.asList(args);
  }

  public LoadBalancerClientCli(String[] args) throws Exception
  {
    OPTIONS.addOption("h", "help", false, "Show help.");
    OPTIONS.addOption("z", "zkserver", true, "Zookeeper server string (example:zk://localhost:2121).");
    OPTIONS.addOption("p", "path", true, "Discovery path (example: /d2).");
    OPTIONS.addOption("h", "host", true, "Host name.");
    OPTIONS.addOption("b", "enabled", true, "Enabled toggling store (value either 'true' or 'false'.");
    OPTIONS.addOption("f", "file", true, "D2 clusters/services configuration file.");
    OPTIONS.addOption("c", "cluster", true, "Cluster name.");
    OPTIONS.addOption("s", "service", true, "Service name.");
    OPTIONS.addOption("m", "method", true, "Service method name.");
    OPTIONS.addOption("r", "request", true, "Request string or file.");
    OPTIONS.addOption("t", "requestype", true, "Request type: value either rpc or rest (default - rest).");
    OPTIONS.addOption("d", "delete", true, "Delete store (cluster or service name).");
    OPTIONS.addOption("n", "storename", true, "Store name (value either 'clusters' or 'services' or 'uris').");
    OPTIONS.addOption("D", "rundiscovery", false, "Run discovery (register clusters/services with zk).");
    OPTIONS.addOption("P", "printstore", false, "Print single store.");
    OPTIONS.addOption("S", "printstores", false, "Print all stores.");
    OPTIONS.addOption("H", "printschema", false, "Print service schema.");
    OPTIONS.addOption("R", "sendrequest", false, "Send request to service.");
    OPTIONS.addOption("e", "endpoints", false, "Print service endpoints.");
    OPTIONS.addOption("T", "toggle", false, "Reset toggling store.");

    CommandLine cl = null;
    try
    {
      final CommandLineParser parser = new GnuParser();
      cl = parser.parse(OPTIONS, args);
    }
    catch (ParseException e)
    {
      System.err.println("Invalid arguments: " + e.getMessage());
      usage();
    }

    if (cl.hasOption("z") && cl.hasOption("p"))
    {
      LoadBalancerClientCli clobj = new LoadBalancerClientCli(cl.getOptionValue("z"), cl.getOptionValue("p"));

      clobj.createZkClient(cl.getOptionValue("z"));
      clobj.startZkClient();

      if (cl.hasOption("D") && cl.hasOption("f"))
      {
        runDiscovery(cl.getOptionValue("z"), cl.getOptionValue("p"), new File(cl.getOptionValue("f")));
        clobj.shutdown();
      }
      else if (cl.hasOption("d") && cl.hasOption("n"))
      {
        deleteStore(clobj.getZKClient(), cl.getOptionValue("z"), cl.getOptionValue("p"), cl.getOptionValue("n"), cl.getOptionValue("d"));
        clobj.shutdown();
      }
      else if (cl.hasOption("S"))
      {
        System.err.println(printStores(clobj.getZKClient(), cl.getOptionValue("z"), cl.getOptionValue("p")));
      }
      else if (cl.hasOption("T") && cl.hasOption("h") && cl.hasOption("b"))
      {
        String host = cl.getOptionValue("h");
        boolean toggled = !"false".equals(cl.getOptionValue("b"));

        resetTogglingStores((host == null) ? "localhost" : host, toggled);
      }
      else if (cl.hasOption("c") && cl.hasOption("s"))
      {
        String requestType = "rest";
        if (cl.hasOption("t"))
        {
          requestType = cl.getOptionValue("t");
        }

        clobj.setCluster(cl.getOptionValue("c"));
        clobj.setService(cl.getOptionValue("s"));

        if (cl.hasOption("P"))
        {
          printStore(clobj.getZKClient(), cl.getOptionValue("z"), cl.getOptionValue("p"), clobj.getCluster(), clobj.getService());
        }
        else if (cl.hasOption("H"))
        {
          System.err.println(getSchema(clobj.getZKClient(), cl.getOptionValue("z"), cl.getOptionValue("p"), clobj.getCluster(), clobj.getService(), requestType));
          clobj.shutdown();
        }
        else if (cl.hasOption("e"))
        {
          clobj.getEndpoints(cl.getOptionValue("z"), cl.getOptionValue("p"), clobj.getCluster(), clobj.getService());
          clobj.shutdown();
        }
        else if (cl.hasOption("R") && cl.hasOption("r"))
        {
          if  (cl.hasOption("m"))
          {
            clobj.setMethod("/" + cl.getOptionValue("m"));
          }
          clobj.setRequest(cl.getOptionValue("r"));
          clobj.createClient();

          System.err.println("RESPONSE:" + sendRequest(clobj.getClient(),
                                                       clobj.getZKClient(),
                                                       cl.getOptionValue("z"),
                                                       cl.getOptionValue("p"),
                                                       clobj.getCluster(),
                                                       clobj.getService(),
                                                       "",
                                                       clobj.getRequest(),
                                                       requestType, true));
        }
        else
        {
          usage();
        }
      }
      else
      {
        usage();
      }
    }
    else
    {
      usage();
    }
  }

  public LoadBalancerClientCli( String zkserverHostPort, String d2path) throws Exception
  {
    _zkConnectionString = zkserverHostPort;
    _zkclient = createZkClient(zkserverHostPort);
    _d2path = d2path;

    startZkClient();
  }

  public LoadBalancerClientCli( String zkserverHostPort, String d2path, String serviceName) throws Exception
  {
    _zkConnectionString = zkserverHostPort;
    _zkclient = createZkClient(zkserverHostPort);
    _d2path = d2path;
    _service = serviceName;

    startZkClient();
  }

  private void usage() throws IOException
  {
    StringBuilder sb = new StringBuilder();
    sb.append("\nExamples");
    sb.append("\n========");
    sb.append("\nExample RunDiscovery (register clusters/services with zk): lb-client.sh -z zk://localhost:2121 -p /d2 -f d2_config_example.json -D");
    sb.append("\nExample RunDiscovery (register clusters/services with zk): lb-client.sh --zkserver zk://localhost:2121 --path /d2 --file d2_config_example.json --rundiscovery");
    sb.append("\nExample Print zk stores: lb-client.sh -z zk://localhost:2121 -p /d2 -c cluster-1 -s service-1_1 -S");
    sb.append("\nExample Print zk stores: lb-client.sh --zkserver zk://localhost:2121 --path /d2 --cluster cluster-1 --service service-1_1 --printstores");
    sb.append("\nExample Print single store: lb-client.sh -z=zk://localhost:2181 -p=/d2 -c='cluster-1' -s=service-1_1 -P");
    sb.append("\nExample Print single store: lb-client.sh --zkserver=zk://localhost:2181 --path=/d2 --cluster='cluster-1' --service=service-1_1 --printstore");
    sb.append("\nExample Delete store: lb-client.sh -z zk://localhost:2121 -p /d2 -d cluster-2 -n clusters");
    sb.append("\nExample Delete store: lb-client.sh -z zk://localhost:2121 -p /d2 -d service-3_3 -n services");
    sb.append("\nExample Delete store: lb-client.sh --zkserver zk://localhost:2121 --path /d2  --delete cluster-2 -storename clusters");
    sb.append("\nExample Print Service Schema: lb-client.sh -z zk://localhost:2121 -p /d2 -c 'cluster-1' -s service-1_1 -H");
    sb.append("\nExample Print Service Schema: lb-client.sh --zkserver zk://localhost:2181 --path /d2 --cluster 'cluster-1' --service service-1_1 --printschema");
    sb.append("\nExample Get Endpoints: lb-client.sh --zkserver zk://localhost:2121 --path /d2 --cluster cluster-1 --endpoints --service service-1_1");
    sb.append("\nExample Send request to service: lb-client.sh -z zk://localhost:2181 -p /d2 -c 'cluster-1' -s service-1_1 -r 'test' -R");
    sb.append("\nExample Send request to service: lb-client.sh -z zk://localhost:2181 -p /d2 -c 'cluster-1' -s service-1_1 -r 'test' -t rpc -R");
    sb.append("\nExample Send request to service: lb-client.sh --zkserver zk://localhost:2181 --path /d2 --cluster 'cluster-1' --service service-1_1 --request 'test' --sendrequest");
    sb.append("\nExample Send request to service: lb-client.sh -z zk://localhost:2181 -p /d2 -c 'history-write-1' -s HistoryService -m getCube -r 'test' -R");
    sb.append("\nExample Send request to service: lb-client.sh --zkserver zk://localhost:2181 --path /d2 --cluster 'history-write-1' --service HistoryService --method getCube --request 'test' --sendrequest");
    sb.append("\nExample Reset toggling stores: lb-client.sh -z zk://localhost:2121 -p /d2 -h localhost -b false -T");
    sb.append("\nExample Reset toggling stores: lb-client.sh --zkserver zk://localhost:2121 --path /d2 --host localhost --enabled false --toggle");
    sb.append("\n");

    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("lb-client.sh -z=zk://<zk host port> -p=<d2 path> ... parameters..." + sb.toString(), OPTIONS);
    System.exit(0);
  }

  public void getEndpoints(String zkServer, String d2path, String cluster, String service) throws Exception
  {
    Map<String, UriProperties> scMap = getServiceClustersURIsInfo(zkServer, d2path, service);
    if (scMap != null && scMap.get(cluster) != null)
    {
      for (Map.Entry<URI, Map<Integer, PartitionData>> uriEntry: scMap.get(cluster).getPartitionDesc().entrySet())
      {
        System.out.println("uri: " + uriEntry.getKey());
        for (Map.Entry<Integer, PartitionData> pData : uriEntry.getValue().entrySet())
        {
          System.out.println("  " + pData.getKey() + ": " + pData.getValue());
        }
      }
    }
    else
    {
      System.out.println("No cluster information found for service: " + service + ", in cluster: " + cluster);
    }
  }

  public static int runDiscovery(String zkserverHostPort, String d2path, File jsonConfigFile) throws Exception
  {
    if (jsonConfigFile.exists())
    {
      _log.info("Reading d2 config data:" + jsonConfigFile.getAbsolutePath());
      @SuppressWarnings("unchecked")
      Map<String, Object> configMap = JacksonUtil.getObjectMapper().readValue(jsonConfigFile, HashMap.class);
      return runDiscovery(zkserverHostPort, d2path, configMap);
    }
    else
    {
      _log.error("File " + jsonConfigFile.getAbsolutePath() + " does not exist. Check your data.");
    }

    return -1;
  }

  public static int runDiscovery(String zkserverHostPort, String d2path, String jsonConfigData) throws Exception
  {
    _log.info("Reading d2 config data:" + jsonConfigData);
    @SuppressWarnings("unchecked")
    Map<String, Object> configMap = (Map<String, Object>) JacksonUtil.getObjectMapper().readValue(jsonConfigData, HashMap.class);

    return runDiscovery(zkserverHostPort, d2path, configMap);
  }

  @SuppressWarnings("unchecked")
  private static int runDiscovery(String zkserverHostPort, String d2path, Map<String, Object> configMap) throws Exception
  {
    String zkHosts = zkserverHostPort.replace("zk://", "");

    Map<String, Object> clusterDefaults = (Map<String,Object>) configMap.get("clusterDefaults");
    Map<String, Object> serviceDefaults = (Map<String,Object>) configMap.get("serviceDefaults");
    Map<String, Object> clusterServiceConfigurations = (Map<String,Object>) configMap.get("clusterServiceConfigurations");
    Map<String, Object> extraClusterServiceConfigurations = (Map<String,Object>) configMap.get("extraClusterServiceConfigurations");
    Map<String, Object> serviceVariants = (Map<String,Object>) configMap.get("serviceVariants");

    D2Config d2conf = new D2Config (zkHosts, 10000, d2path,
                                    5000L, 10,
                                    clusterDefaults,
                                    serviceDefaults,
                                    clusterServiceConfigurations,
                                    extraClusterServiceConfigurations,
                                    serviceVariants);
    return d2conf.configure();
  }

  public String getSchema(ZKConnection zkclient,
                        String zkserver,
                        String d2path,
                        String cluster,
                        String service,
                        String requestType) throws URISyntaxException,
          InterruptedException,
          ExecutionException,
          IOException,
          PropertyStoreException, TimeoutException
  {
    String responseString = null;
    if (hasService(zkclient, zkserver, d2path, cluster, service))
    {
      DynamicClient client = new DynamicClient(getLoadBalancer(zkclient, zkserver, d2path, service), null);
      URI uri = URI.create("d2://" + service + "/");

      try
      {
        RestRequest restRequest = new RestRequestBuilder(uri).setEntity("".getBytes("UTF-8")).build();
        Future<RestResponse> response = client.restRequest(restRequest, new RequestContext());
        responseString = response.get().getEntity().asString("UTF-8");
      }
      finally
      {
        LoadBalancerUtil.syncShutdownClient(_client, _log);
        zkclient.shutdown();
      }
    }
    else
    {
      System.out.println("Service '" + service + "' is not defined for cluster '"
          + cluster + "'.");
    }
    return responseString;
  }

  public void createClient() throws URISyntaxException,
                                          InterruptedException,
                                          ExecutionException,
                                          IOException,
                                          PropertyStoreException,
                                          TimeoutException
  {
    _client = createClient(_zkclient, _zkConnectionString, _d2path, _service) ;
  }

  public DynamicClient createClient(ZKConnection zkclient, String zkserver, String d2path, String service)
                                  throws URISyntaxException,
                                  InterruptedException,
                                  ExecutionException,
                                  IOException,
                                  PropertyStoreException,
                                  TimeoutException
  {
    return new DynamicClient(getLoadBalancer(zkclient, zkserver, d2path, service), null);
  }

  public DynamicClient createZKFSTogglingLBClient(String zkHostsPortsConnectionString, String d2path, String servicePath)
  throws URISyntaxException,
  InterruptedException,
  ExecutionException,
  IOException,
  PropertyStoreException,
  TimeoutException,
  Exception
  {
    _zkfsLoadBalancer = getZKFSLoadBalancer(zkHostsPortsConnectionString, d2path, servicePath);
    FutureCallback<None> startupCallback = new FutureCallback<None>();
    _zkfsLoadBalancer.start(startupCallback);
    startupCallback.get(5000, TimeUnit.MILLISECONDS);

    return new DynamicClient(_zkfsLoadBalancer, null);
  }

  public DynamicClient getClient()
  {
    return _client;
  }

  public String sendRequest(DynamicClient client, String cluster, String service, String request) throws Exception
  {
    return sendRequest(client, _zkclient, _zkConnectionString, _d2path, cluster, service, "", request, "rest", false);
  }

  public String sendRequest(DynamicClient client, String cluster, String service, String request, String requestType) throws  Exception
  {
    return sendRequest(client, _zkclient, _zkConnectionString, _d2path, cluster, service, "", request, requestType, false);
  }

  public String sendRequest(DynamicClient client,
                            ZKConnection zkclient,
                            String zkserver,
                            String d2path,
                            String cluster,
                            String service,
                            String method,
                            String request,
                            String requestType,
                            boolean performShutdown) throws Exception
    {
      String responseString = null;

      URI uri = URI.create("d2://" + service + method);

      try
      {
        RestRequest restRequest = new RestRequestBuilder(uri).setEntity(request.getBytes("UTF-8")).build();
        Future<RestResponse> response = client.restRequest(restRequest, new RequestContext());
        responseString = response.get().getEntity().asString("UTF-8");
      }
      finally
      {
        if (performShutdown)
        {
          LoadBalancerUtil.syncShutdownClient(_client, _log);
          zkclient.shutdown();
        }
      }

      return responseString;
    }

  public void setCluster(String cluster)
  {
    _cluster = cluster;
  }

  public String getCluster()
  {
    return _cluster;
  }

  public void setD2Path(String d2path)
  {
    _d2path = d2path;
  }

  public String getD2Path()
  {
    return _d2path;
  }

  public void setService(String service)
  {
    _service = service;
  }

  public String getService()
  {
    return _service;
  }

  public ZKConnection createZkClient(String zkserverHostPort)
  {
    _zkclient = new ZKConnection(zkserverHostPort.replace("zk://",""), 10000);

    return _zkclient;
  }

  public ZKConnection getZKClient()
  {
    return _zkclient;
  }

  public void startZkClient() throws IOException, InterruptedException, TimeoutException
  {
    _zkclient.start();
    _zkclient.waitForState(KeeperState.SyncConnected, 40, TimeUnit.SECONDS);
  }

  public void setMethod(String method)
  {
    _method = method;
  }

  public String getMethod()
  {
    return _method;
  }

  public void setRequest(String request)
  {
    _request = request;
  }

  public String getRequest()
  {
    return _request;
  }

  public void deleteStore(ZKConnection zkclient,
                                 String zkserverHostPort,
                                 String d2path,
                                 String storeName,
                                 String listenTo) throws Exception
  {
    String storeString = zkserverHostPort + d2path + "/" + storeName;
    PropertyStore<?> store = getStore(zkclient, storeString, null);

    store.remove(listenTo);
    shutdownPropertyStore(store, 60, TimeUnit.SECONDS);
  }

  public static <T> PropertyStore<T> getStore(ZKConnection zkclient,
                                               String store,
                                               PropertySerializer<T> serializer) throws URISyntaxException,
                                                                                        IOException,
                                                                                        PropertyStoreException
  {
    URI storeUri = URI.create(store);

    if (storeUri.getScheme() != null)
    {
      if (storeUri.getScheme().equals("zk"))
      {

        ZooKeeperPermanentStore<T> zkStore = new ZooKeeperPermanentStore<T>(
                zkclient, serializer, storeUri.getPath());
        startStore(zkStore);
        return zkStore;
      }
      else
      {
        throw new URISyntaxException(store, "Unable to parse store uri. Only zk and file stores are supported.");
      }
    }
    else
    {
      // assume it's a local file
      return new FileStore<T>(storeUri.getPath(), ".json", serializer);
    }

  }

  public static List<String> getServicesGroups (ZKConnection zkclient, String basePath) throws Exception
  {
    List<String> servicesGroups = new ArrayList<String>();
    ZooKeeper zook = zkclient.getZooKeeper();

    List<String> children = zook.getChildren(basePath,false);

    for (String child : children)
    {
      if (! child.equalsIgnoreCase("clusters") && ! child.equalsIgnoreCase("uris"))
      {
        servicesGroups.add(child);
      }
    }

    return servicesGroups;
  }

  public static <T> PropertyStore<T> getEphemeralStore(ZKConnection zkclient,
                                                        String store,
                                                        PropertySerializer<T> serializer,
                                                        ZooKeeperPropertyMerger<T> merger) throws URISyntaxException,
                                                                                                  IOException,
                                                                                                  PropertyStoreException
  {
    URI storeUri = URI.create(store);

    if (storeUri.getScheme() != null)
    {
      if (storeUri.getScheme().equals("zk"))
      {

        ZooKeeperEphemeralStore<T> zkStore = new ZooKeeperEphemeralStore<T>( zkclient, serializer, merger, storeUri.getPath());
        startStore(zkStore);
        return zkStore;
      }
      else
      {
        throw new URISyntaxException(store, "Unable to parse store uri. Only zk and file stores are supported.");
      }
    }
    else
    {
      // assume it's a local file
      return new FileStore<T>(storeUri.getPath(), ".json", serializer);
    }

  }

  private static <T> void startStore(PropertyStore<T> store) throws PropertyStoreException
  {
    try
    {
      FutureCallback<None> callback = new FutureCallback<None>();
      store.start(callback);
      callback.get(30, TimeUnit.SECONDS);
    }
    catch (Exception e)
    {
      throw new PropertyStoreException("Failed to start store", e);
    }
  }

  public static SimpleLoadBalancer getLoadBalancer(ZKConnection zkclient,
                                                   String zkserver,
                                                   String d2path,
                                                   String service) throws IOException,
                                                                          IllegalStateException,
                                                                          URISyntaxException,
                                                                          PropertyStoreException,
                                                                          ExecutionException,
                                                                          TimeoutException,
                                                                          InterruptedException
  {
    // zk stores
    String clstoreString = zkserver + ZKFSUtil.clusterPath(d2path);
    String scstoreString = zkserver + ZKFSUtil.servicePath(d2path);
    String uristoreString = zkserver + ZKFSUtil.uriPath(d2path);

    ZooKeeperPermanentStore<ClusterProperties> zkClusterRegistry =
        (ZooKeeperPermanentStore<ClusterProperties>) getStore(zkclient,
                                                              clstoreString,
                                                              new ClusterPropertiesJsonSerializer());
    ZooKeeperPermanentStore<ServiceProperties> zkServiceRegistry =
        (ZooKeeperPermanentStore<ServiceProperties>) getStore(zkclient,
                                                              scstoreString,
                                                              new ServicePropertiesJsonSerializer());
    ZooKeeperEphemeralStore<UriProperties> zkUriRegistry =
        (ZooKeeperEphemeralStore<UriProperties>) getEphemeralStore(zkclient,
                                                                   uristoreString,
                                                                   new UriPropertiesJsonSerializer(),
                                                                   new UriPropertiesMerger());

    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("D2 PropertyEventExecutor"));

    PropertyEventBus<ServiceProperties> serviceBus =
        new PropertyEventBusImpl<ServiceProperties>(executor, zkServiceRegistry);
    PropertyEventBus<UriProperties> uriBus =
        new PropertyEventBusImpl<UriProperties>(executor, zkUriRegistry);
    PropertyEventBus<ClusterProperties> clusterBus =
        new PropertyEventBusImpl<ClusterProperties>(executor, zkClusterRegistry);

    Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories =
        new HashMap<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>>();

    loadBalancerStrategyFactories.put("random", new RandomLoadBalancerStrategyFactory());
    loadBalancerStrategyFactories.put("degrader", new DegraderLoadBalancerStrategyFactoryV2());
    loadBalancerStrategyFactories.put("degraderV2", new DegraderLoadBalancerStrategyFactoryV2());
    loadBalancerStrategyFactories.put("degraderV3", new DegraderLoadBalancerStrategyFactoryV3());
    loadBalancerStrategyFactories.put("degraderV2_1", new DegraderLoadBalancerStrategyFactoryV2_1());

    Map<String, TransportClientFactory> clientFactories =
        new HashMap<String, TransportClientFactory>();

    clientFactories.put("http", new HttpClientFactory());

    // create the state
    SimpleLoadBalancerState state =
        new SimpleLoadBalancerState(executor,
                                    uriBus,
                                    clusterBus,
                                    serviceBus,
                                    clientFactories,
                                    loadBalancerStrategyFactories,
                                    null, null, false);

    SimpleLoadBalancer balancer = new SimpleLoadBalancer(state, 5, TimeUnit.SECONDS);
    FutureCallback<None> callback = new FutureCallback<None>();
    balancer.start(callback);
    callback.get(5, TimeUnit.SECONDS);

    new JmxManager().registerLoadBalancer("balancer", balancer)
                    .registerLoadBalancerState("state", state)
                    .registerScheduledThreadPoolExecutor("executorService", executor)
                    .registerZooKeeperPermanentStore("zkClusterRegistry", zkClusterRegistry)
                    .registerZooKeeperPermanentStore("zkServiceRegistry",
                                                     zkServiceRegistry)
                    .registerZooKeeperEphemeralStore("zkUriRegistry", zkUriRegistry);

    return balancer;
  }

  public ZKFSLoadBalancer getZKFSLoadBalancer(String zkConnectString, String d2path, String d2ServicePath) throws Exception
  {
    _tmpDir = createTempDirectory(_tmpdirName);

	ZKFSComponentFactory componentFactory = new ZKFSComponentFactory();
	if(d2ServicePath == null || d2ServicePath.isEmpty())
    {
      d2ServicePath = "services";
    }

    Map<String, TransportClientFactory> clientFactories = new HashMap<String, TransportClientFactory>();
    clientFactories.put("http", new HttpClientFactory());

    Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories =
    new HashMap<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>>();

    loadBalancerStrategyFactories.put("random", new RandomLoadBalancerStrategyFactory());
    loadBalancerStrategyFactories.put("degrader", new DegraderLoadBalancerStrategyFactoryV2());
    loadBalancerStrategyFactories.put("degraderV2", new DegraderLoadBalancerStrategyFactoryV2());
    loadBalancerStrategyFactories.put("degraderV3", new DegraderLoadBalancerStrategyFactoryV3());
    loadBalancerStrategyFactories.put("degraderV2_1", new DegraderLoadBalancerStrategyFactoryV2_1());

	ZKFSTogglingLoadBalancerFactoryImpl factory = new ZKFSTogglingLoadBalancerFactoryImpl(componentFactory,
                                        TIMEOUT, TimeUnit.MILLISECONDS,
                                        d2path, _tmpDir.getAbsolutePath(),
                                        clientFactories,
                                        loadBalancerStrategyFactories,
                                        d2ServicePath, null, null, false);

	return new ZKFSLoadBalancer(zkConnectString, SESSION_TIMEOUT, (int) TIMEOUT, factory, null, d2path);
  }

  public  Set< UriProperties> getServiceURIsProps(String zkserver, String d2path, String serviceName) throws IOException,
  IllegalStateException,
  URISyntaxException,
  PropertyStoreException
  {
    Set<UriProperties> uriprops = new HashSet<UriProperties>();
    // zk stores
    String scstoreString = zkserver + ZKFSUtil.servicePath(d2path);
    String uristoreString = zkserver + ZKFSUtil.uriPath(d2path);

    ZooKeeperPermanentStore<ServiceProperties> zkServiceRegistry =
        (ZooKeeperPermanentStore<ServiceProperties>) getStore(_zkclient,
                                                              scstoreString,
                                                              new ServicePropertiesJsonSerializer());
    ZooKeeperEphemeralStore<UriProperties> zkUriRegistry =
        (ZooKeeperEphemeralStore<UriProperties>) getEphemeralStore(_zkclient,
                                                                   uristoreString,
                                                                   new UriPropertiesJsonSerializer(),
                                                                   new UriPropertiesMerger());

    String clusterName = zkServiceRegistry.get(serviceName).getClusterName();
    UriProperties uripros = zkUriRegistry.get(clusterName);
    uriprops.add( uripros);
    return uriprops;
  }

  public  Map<String, UriProperties> getServiceClustersURIsInfo(String zkserver, String d2path, String serviceName) throws IOException,
  IllegalStateException,
  URISyntaxException,
  PropertyStoreException
  {
    Map<String,UriProperties> map = new HashMap<String,UriProperties>();
    // zk stores
    String scstoreString = zkserver + ZKFSUtil.servicePath(d2path);
    String uristoreString = zkserver + ZKFSUtil.uriPath(d2path);

    ZooKeeperPermanentStore<ServiceProperties> zkServiceRegistry =
        (ZooKeeperPermanentStore<ServiceProperties>) getStore(_zkclient,
                                                              scstoreString,
                                                              new ServicePropertiesJsonSerializer());
    ZooKeeperEphemeralStore<UriProperties> zkUriRegistry =
        (ZooKeeperEphemeralStore<UriProperties>) getEphemeralStore(_zkclient,
                                                                   uristoreString,
                                                                   new UriPropertiesJsonSerializer(),
                                                                   new UriPropertiesMerger());

    List<String> currentservices = zkServiceRegistry.ls();

    for (String service : currentservices)
    {
      if (service.equals(serviceName))
      {
        String clusterName = zkServiceRegistry.get(serviceName).getClusterName();
        UriProperties uripros = zkUriRegistry.get(clusterName);
        map.put( clusterName, uripros);
      }
    }

    return map;
  }


  public static boolean hasService(ZKConnection zkclient,
                                   String zkserver,
                                   String d2path,
                                   String cluster,
                                   String service) throws URISyntaxException,
                                                          IOException,
                                                          PropertyStoreException
  {
    ZooKeeperPermanentStore<ServiceProperties> zkServiceRegistry = null;

    String scstoreString = zkserver + ZKFSUtil.servicePath(d2path);

    zkServiceRegistry =
        (ZooKeeperPermanentStore<ServiceProperties>) getStore(zkclient,
                                                              scstoreString,
                                                              new ServicePropertiesJsonSerializer());

    return zkServiceRegistry.get(service).getClusterName().equals(cluster);

  }

  public static String printStore(ZKConnection zkclient,
                                String zkserver,
                                String d2path,
                                String cluster,
                                String service) throws URISyntaxException,
                                                       IOException,
                                                       PropertyStoreException
  {
    return printStore(zkclient, zkserver, d2path, cluster, service, null);
  }

  public static String printStore(ZKConnection zkclient,
                                  String zkserver,
                                  String d2path,
                                  String cluster,
                                  String service,
                                  String serviceGroup) throws URISyntaxException,
        IOException,
        PropertyStoreException
    {
      StringBuilder sb = new StringBuilder();
      ZooKeeperPermanentStore<ClusterProperties> zkClusterRegistry = null;
      ZooKeeperPermanentStore<ServiceProperties> zkServiceRegistry = null;
      ZooKeeperEphemeralStore<UriProperties> zkUriRegistry = null;

      String clstoreString = zkserver + ZKFSUtil.clusterPath(d2path);
      String uristoreString = zkserver + ZKFSUtil.uriPath(d2path);

      zkClusterRegistry =
        (ZooKeeperPermanentStore<ClusterProperties>) getStore(zkclient,
                                                              clstoreString,
                                                              new ClusterPropertiesJsonSerializer());
      zkUriRegistry =
        (ZooKeeperEphemeralStore<UriProperties>) getEphemeralStore(zkclient,
                                                                   uristoreString,
                                                                   new UriPropertiesJsonSerializer(),
                                                                   new UriPropertiesMerger());



      if (serviceGroup != null)
      {
        String scstoreString = zkserver + ZKFSUtil.servicePath(d2path, serviceGroup);
        zkServiceRegistry = (ZooKeeperPermanentStore<ServiceProperties>) getStore(zkclient,
                                                                scstoreString,
                                                                new ServicePropertiesJsonSerializer());
      }
      else
      {
        String scstoreString = zkserver + ZKFSUtil.servicePath(d2path);
        zkServiceRegistry = (ZooKeeperPermanentStore<ServiceProperties>) getStore(zkclient,
                                                                scstoreString,
                                                                new ServicePropertiesJsonSerializer());
      }

      sb.append(printStore(zkClusterRegistry, zkUriRegistry, cluster));

      if (zkServiceRegistry.get(service).getClusterName().equals(cluster))
      {
        sb.append(printService(zkServiceRegistry, service));
      }

      return sb.toString();
    }

  private static <T> String printService(PropertyStore<T> zkServiceRegistry, String service) throws URISyntaxException,
      PropertyStoreException
  {
    String serviceInfo = "Service '" + service + "':" + zkServiceRegistry.get(service).toString();

    System.out.println(serviceInfo);

    return serviceInfo;
  }

  private static <T> String printStore(PropertyStore<T> zkClusterRegistry,
                                     ZooKeeperEphemeralStore<UriProperties> zkUriRegistry,
                                     String cluster) throws URISyntaxException,
      PropertyStoreException
  {
    StringBuilder sb = new StringBuilder();
    sb.append("\nCluster '" );
    sb.append(cluster);
    sb.append("':");
    sb.append(zkClusterRegistry.get(cluster).toString());

    sb.append("\nCluster '" );
    sb.append(cluster);
    sb.append("' UriProperties:");
    sb.append(zkUriRegistry.get(cluster));

    return sb.toString();
  }

  public static String printStore(ZooKeeperPermanentStore<ClusterProperties> zkClusterRegistry,
                                ZooKeeperEphemeralStore<UriProperties> zkUriRegistry,
                                ZooKeeperPermanentStore<ServiceProperties> zkServiceRegistry,
                                String cluster,
                                String service) throws URISyntaxException,
      PropertyStoreException
  {
    StringBuilder sb = new StringBuilder();

    sb.append(printStore(zkClusterRegistry, zkUriRegistry, cluster));
    if (zkServiceRegistry.get(service).getClusterName().equals(cluster))
    {
      sb.append(printService(zkServiceRegistry, service));
    }

    return sb.toString();
  }

  public static String printStores(ZKConnection zkclient, String zkserver, String d2path) throws IOException,
      IllegalStateException,
      URISyntaxException,
      PropertyStoreException,
      Exception
  {
    int serviceCount = 0;
    String zkstr = "\nZKServer:" + zkserver;
    StringBuilder sb = new StringBuilder();
    Set<String> currentservices = new HashSet<String>();
    Map<String,ZooKeeperPermanentStore<ServiceProperties>> zkServiceRegistryMap = new HashMap<String,ZooKeeperPermanentStore<ServiceProperties>>();
    Map<String,List<String>> servicesGroupMap = new HashMap<String,List<String>>();

    // zk stores
    String clstoreString = zkserver + ZKFSUtil.clusterPath(d2path);
    String uristoreString = zkserver + ZKFSUtil.uriPath(d2path);

    ZooKeeperPermanentStore<ClusterProperties> zkClusterRegistry =
        (ZooKeeperPermanentStore<ClusterProperties>) getStore(zkclient,
                                                              clstoreString,
                                                              new ClusterPropertiesJsonSerializer());
    ZooKeeperEphemeralStore<UriProperties> zkUriRegistry =
        (ZooKeeperEphemeralStore<UriProperties>) getEphemeralStore(zkclient,
                                                                   uristoreString,
                                                                   new UriPropertiesJsonSerializer(),
                                                                   new UriPropertiesMerger());

    List<String> currentclusters = zkClusterRegistry.ls();
    List<String> currenturis = zkUriRegistry.ls();
    List<String> servicesGroups = getServicesGroups (zkclient, d2path);

    for (String serviceGroup : servicesGroups)
    {
      String scstoreString = zkserver + ZKFSUtil.servicePath(d2path, serviceGroup);
      ZooKeeperPermanentStore<ServiceProperties> zkServiceRegistry =
        (ZooKeeperPermanentStore<ServiceProperties>) getStore(zkclient,
                                                              scstoreString,
                                                              new ServicePropertiesJsonSerializer());
      zkServiceRegistryMap.put(serviceGroup, zkServiceRegistry);
      List<String> services = zkServiceRegistry.ls();
      currentservices.addAll(services);
      servicesGroupMap.put(serviceGroup, services);
      serviceCount += services.size();
    }

    sb.append(zkstr);
    sb.append(" Total Clusters:");
    sb.append(currentclusters.size());
    sb.append(zkstr);
    sb.append(" Total Services:");
    sb.append(serviceCount);
    sb.append(zkstr);
    sb.append(" Total URIs:");
    sb.append(currenturis.size());
    sb.append("\n============================================================");
    sb.append("\nSERVICE GROUPS");

    for (String serviceGroup : servicesGroupMap.keySet())
    {
      sb.append("\nGROUP:"+serviceGroup+"           Services:"+servicesGroupMap.get(serviceGroup));
    }

    for (String cluster : currentclusters)
    {
      int count = 0;
      sb.append("\n============================================================");
      sb.append("\nCLUSTER '");
      sb.append(cluster);
      sb.append("':");

      for (String service : currentservices)
      {
        for (String serviceGroup : servicesGroupMap.keySet())
        {
          ZooKeeperPermanentStore<ServiceProperties> zkStorePropsForSerivceGroup = zkServiceRegistryMap.get(serviceGroup);
          if (zkStorePropsForSerivceGroup != null)
          {
            ServiceProperties serviceProps = zkStorePropsForSerivceGroup.get(service);
        	if (serviceProps != null)
        	{
        	  if (cluster.equals(serviceProps.getClusterName()))
        	  {
                sb.append("\n-------------------");
                sb.append("\nSERVICE '" + service + "':");

                sb.append(printStore(zkClusterRegistry, zkUriRegistry, zkServiceRegistryMap.get(serviceGroup), cluster, service));
        	    count++;
        	    break;
        	  }
        	}
          }
        }
      }
      if (count == 0)
      {
        sb.append(printStore(zkClusterRegistry, zkUriRegistry, cluster));
        sb.append("\nNo services were found in this cluster.");
      }
    }

    return sb.toString();
  }

  public static void resetTogglingStores(String host, boolean enabled) throws Exception
  {

    MonitoredHost _host = MonitoredHost.getMonitoredHost(new HostIdentifier(host));

    for (Object pidObj : _host.activeVms())
    {
      int pid = (Integer) pidObj;

      System.out.println("checking pid: " + pid);

      JMXServiceURL jmxUrl = null;
      com.sun.tools.attach.VirtualMachine vm =
          com.sun.tools.attach.VirtualMachine.attach(pid + "");

      try
      {
        // get the connector address
        String connectorAddress = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
        // establish connection to connector server
        if (connectorAddress != null)
        {
          jmxUrl = new JMXServiceURL(connectorAddress);
        }
      }
      finally
      {
        vm.detach();
      }

      if (jmxUrl != null)
      {
        System.out.println("got jmx url: " + jmxUrl);

        // connect to jmx
        JMXConnector connector = JMXConnectorFactory.connect(jmxUrl);

        connector.connect();

        MBeanServerConnection mbeanServer = connector.getMBeanServerConnection();

        // look for all beans in the d2 name space
        Set<ObjectInstance> objectInstances =
            mbeanServer.queryMBeans(new ObjectName("com.linkedin.d2:*"), null);

        for (ObjectInstance objectInstance : objectInstances)
        {
          System.err.println("checking object: " + objectInstance.getObjectName());

          // if we've found a toggling store, then toggle it
          if (objectInstance.getObjectName().toString().endsWith("TogglingStore"))
          {
            System.out.println("found toggling zk store, so toggling to: " + enabled);

            mbeanServer.invoke(objectInstance.getObjectName(),
                               "setEnabled",
                               new Object[] { enabled },
                               new String[] { "boolean" });
          }
        }
      }
      else
      {
        System.out.println("pid is not a jmx process: " + pid);
      }
    }
  }

  private void deleteTempDir() throws IOException
  {
    if (_tmpDir.exists())
    {
      try
      {
        FileUtils.deleteDirectory(_tmpDir);
      }
      catch (IOException e)
      {
        throw new IOException("Could not delete temp file: " + _tmpDir.getAbsolutePath());
      }
    }
  }

  private static File createTempDirectory(String name) throws IOException
  {
    final File temp;

    temp = new File(System.getProperty("java.io.tmpdir") + File.separator + name);

    if (temp.exists() && temp.isDirectory())
    {
      try
      {
        FileUtils.deleteDirectory(temp);
      }
      catch (IOException e)
      {
        throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
      }
    }
    if (!(temp.mkdir()))
    {
      throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
    }

    return (temp);
  }

  public void shutdown() throws Exception
  {
    if (_zkClusterRegistry != null)
    {
      try
      {
        shutdownZKRegistry(_zkClusterRegistry);
      }
      catch (Exception e)
      {
        _log.error("Failed to shutdown ZooKeeperPermanentStore<ClusterProperties> zkClusterRegistry.");
      }
    }

    if (_zkServiceRegistry != null)
    {
      try
      {
        shutdownZKRegistry(_zkServiceRegistry);
      }
      catch (Exception e)
      {
        _log.error("Failed to shutdown ZooKeeperPermanentStore<ServiceProperties> zkServiceRegistry.");
      }
    }

    if (_zkUriRegistry != null)
    {
      try
      {
        shutdownZKRegistry(_zkUriRegistry);
      }
      catch (Exception e)
      {
        _log.error("Failed to shutdown ZooKeeperEphemeralStore<UriProperties> zkUriRegistry.");
      }
    }

    try
    {
      if (_client != null)
      {
        LoadBalancerUtil.syncShutdownClient(_client, _log);
      }
    }
    catch (Exception e)
    {
      _log.error("Failed to shutdown dynamic client.");
    }

    if (_zkfsLoadBalancer != null)
    {
      try
      {
        final CountDownLatch latch = new CountDownLatch(1);
        _zkfsLoadBalancer.shutdown(new PropertyEventShutdownCallback()
        {
          @Override
          public void done()
          {
            latch.countDown();
          }
        });

        if (!latch.await(5, TimeUnit.SECONDS))
        {
          _log.error("unable to shut down store");
        }
      }
      catch (Exception e)
      {
        _log.error("Failed to shutdown zkfsLoadBalancer.");
      }
    }

    try
    {
      deleteTempDir();
    }
    catch (Exception e)
    {
      _log.error("Failed to delete directory " + _tmpDir);
    }

    try
    {
      _zkclient.shutdown();
    }
    catch (Exception e)
    {
      _log.error("Failed to shutdown zk client.");
    }
  }

  private void shutdownZKRegistry(ZooKeeperStore<?> zkregistry) throws Exception
  {
    if (zkregistry != null)
    {
      FutureCallback<None> shutdownCallback = new FutureCallback<None>();
      zkregistry.shutdown(shutdownCallback);
      shutdownCallback.get(5000, TimeUnit.MILLISECONDS);
    }
  }

  private void shutdownPropertyStore(PropertyStore<?> store, long timeout, TimeUnit unit) throws Exception
  {
    final CountDownLatch registryLatch = new CountDownLatch(1);

    store.shutdown(new PropertyEventShutdownCallback()
    {
      @Override
      public void done()
      {
        registryLatch.countDown();
      }
    });

    try
    {
      registryLatch.await(timeout, unit);
    }
    catch (InterruptedException e)
    {
      System.err.println("unable to shutdown store: " + store);
    }
  }
}
