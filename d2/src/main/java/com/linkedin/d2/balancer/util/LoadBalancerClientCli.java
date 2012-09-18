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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.linkedin.d2.balancer.zkfs.ZKFSUtil;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.linkedin.d2.balancer.strategies.random.RandomLoadBalancerStrategyFactory;
import com.linkedin.d2.discovery.PropertySerializer;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import com.linkedin.d2.discovery.event.PropertyEventBusImpl;
import com.linkedin.d2.discovery.event.PropertyEventThread;
import com.linkedin.d2.discovery.stores.PropertyStore;
import com.linkedin.d2.discovery.stores.PropertyStoreException;
import com.linkedin.d2.discovery.stores.file.FileStore;
import com.linkedin.d2.discovery.stores.zk.ZKConnection;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperEphemeralStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperPermanentStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperPropertyMerger;
import com.linkedin.d2.jmx.JmxManager;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rpc.RpcRequest;
import com.linkedin.r2.message.rpc.RpcRequestBuilder;
import com.linkedin.r2.message.rpc.RpcResponse;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.http.client.HttpClientFactory;

public class LoadBalancerClientCli
{
  private ZKConnection _zkclient;

  private String _zkserver = null;
  private String _d2path   = null;
  private String _cluster  = null;
  private String _service  = "";
  private String _method   = "";
  private String _request  = null;
  private String _requestType = "rest";
  private DynamicClient _client = null;

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
    OptionParser parser = new OptionParser();

    parser.acceptsAll(asList("help", "h"), "Show help.");

    OptionSpec<String> zkserverOption =
        parser.acceptsAll(asList("zkserver", "z"), "ZK server string.").withRequiredArg();
    OptionSpec<String> pathOption =
        parser.acceptsAll(asList("path", "p"), "Discover path.").withRequiredArg();
    OptionSpec<String> clusterOption =
        parser.acceptsAll(asList("cluster", "c"), "Clister name.").withOptionalArg();
    OptionSpec<String> serviceOption =
        parser.acceptsAll(asList("service", "s"), "Service name.").withOptionalArg();
    OptionSpec<String> serviceMethodOption =
        parser.acceptsAll(asList("method", "m"), "Service method name.")
              .withOptionalArg();
    OptionSpec<String> requestOption =
        parser.acceptsAll(asList("request", "r"), "Request string.").withOptionalArg().ofType(String.class );
    OptionSpec<String> requestTypeOption =
      parser.acceptsAll(asList("requestype", "t"), "Request string.").withOptionalArg();

    OptionSpec<String> printStoreOption =
      parser.acceptsAll(asList("printstore", "P"), "Print store.").withOptionalArg();
    OptionSpec<String> printStoresOption =
        parser.acceptsAll(asList("printstores", "S"), "Print stores.").withOptionalArg();

    OptionSpec<String> printSchemaOption =
        parser.acceptsAll(asList("getschema", "H"), "Print service schema.")
              .withOptionalArg();
    OptionSpec<String> sendRequestOption =
        parser.acceptsAll(asList("sendrequest", "R"), "Send request to service method.")
              .withOptionalArg();
    
    OptionSpec<String> printEndpointsOption =
            parser.acceptsAll(asList("endpoints", "e"), "Print service endpoints.")
                  .withOptionalArg();

    OptionSet options = parser.parse(args);

    if (options.has(zkserverOption) && options.has(pathOption))
    {
      LoadBalancerClientCli clobj = new LoadBalancerClientCli(options.valueOf(zkserverOption), options.valueOf(pathOption));

      if (options.has(requestTypeOption))
      {
        clobj.setRequestType(options.valueOf(requestTypeOption));
      }

      clobj.createZkClient(options.valueOf(zkserverOption));
      clobj.startZkClient();

      if (options.has(printStoresOption))
      {
        printStores(clobj.getZKClient(), options.valueOf(zkserverOption), options.valueOf(pathOption));
      }
      else if (options.has(clusterOption) && options.has(serviceOption))
      {
        clobj.setCluster(options.valueOf(clusterOption));
        clobj.setService(options.valueOf(serviceOption));

        if (options.has(printStoreOption))
        {
          printStore(clobj.getZKClient(), options.valueOf(zkserverOption), options.valueOf(pathOption), clobj.getCluster(), clobj.getService());
        }
        else if (options.has(printSchemaOption))
        {
          getSchema(clobj.getZKClient(), options.valueOf(zkserverOption), options.valueOf(pathOption), clobj.getCluster(), clobj.getService());
        }
        else if (options.has(printEndpointsOption))
        {
          clobj.getEndpoints(options.valueOf(zkserverOption), options.valueOf(pathOption), clobj.getCluster(), clobj.getService());
          clobj.shutdown();
        }
        else if (options.has(requestOption) && options.has(sendRequestOption))
        {
          if (options.has(serviceMethodOption))
          {
            clobj.setMethod("/" + options.valueOf(serviceMethodOption));
          }
          clobj.setRequest(options.valueOf(requestOption));
          clobj.createClient();

          sendRequest(clobj.getClient(),clobj.getZKClient(), options.valueOf(zkserverOption), options.valueOf(pathOption),
                      clobj.getCluster(), clobj.getService(),"",clobj.getRequest(), true);
        }
        else
        {
          usage(parser);
        }
      }
      else
      {
        usage(parser);
      }
    }
    else
    {
      usage(parser);
    }

  }

  public void getEndpoints(String zkServer,
		String d2path, String cluster, String service) throws Exception {
	
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

public LoadBalancerClientCli( String zkserverHostPort, String d2path) throws Exception
  {
    _zkserver = zkserverHostPort;
    _zkclient = createZkClient(zkserverHostPort);
    _d2path = d2path;

    startZkClient();
  }

  public LoadBalancerClientCli( String zkserverHostPort, String d2path, String serviceName) throws Exception
  {
    _zkserver = zkserverHostPort;
    _zkclient = createZkClient(zkserverHostPort);
    _d2path = d2path;
    _service = serviceName;

    startZkClient();
  }

  private void usage(OptionParser parser) throws IOException
  {
    System.out.println("Examples");
    System.out.println("========");
    System.out.println();
    System.out.println("Example Print zk stores: lb-client.sh -z=zk://esv4-be32.stg.linkedin.com:12913 -p=/d2 -S");
    System.out.println("Example Print zk stores: lb-client.sh --zkserver=zk://esv4-be32.stg.linkedin.com:12913 --path=/d2 --printstores");
    System.out.println("Example Print single store: lb-client.sh -z=zk://esv4-be32.stg.linkedin.com:12913 -p=/d2 -c='history-read-1' -s=HistoryService -P");
    System.out.println("Example Print single store: lb-client.sh --zkserver=zk://esv4-be32.stg.linkedin.com:12913 --path=/d2 --cluster='history-write-1' --service=HistoryService --printstore");
    System.out.println("Example Get Service Schema: lb-client.sh -z=zk://esv4-be32.stg.linkedin.com:12913 -p=/d2 -c='history-write-1' -s=HistoryService -H  ");
    System.out.println("Example Get Service Schema: lb-client.sh --zkserver=zk://esv4-be32.stg.linkedin.com:12913 --path=/d2 --cluster='history-write-1' --service=HistoryService --getschema");
    System.out.println("Example Get Endpoints: lb-client.sh --zkserver=zk://esv4-be32.stg.linkedin.com:12913 --path=/d2 --cluster identity --endpoints --service identityPrivacySettings");
    System.out.println("Example Send request to service: lb-client.sh -z=zk://esv4-be32.stg.linkedin.com:12913 -p=/d2 -c='history-write-1' -s=HistoryService -m=getCube -r=$stgrequest -R");
    System.out.println("Example Send request to service: lb-client.sh --zkserver=zk://esv4-be32.stg.linkedin.com:12913 --path=/d2 --cluster='history-write-1' --service=HistoryService --method=getCube --request=$stgrequest --sendrequest");
    System.out.println(" where stgrequest=\"{\"query\":{\"query\":[{\"limit\":12,\"transform\":\"SUM\",\"order\":[{\"column\":\"profile_views.tracking_time\",\"ascending\":false}],\"select\":[\"impression\",\"profile_views.tracking_time\"],\"group\":[\"profile_views.tracking_time\"]}],\"ids\":[\"1213\"],\"type\":\"wvmp-cube-profile-views\",\"stringCols\":[]}}\"");
    System.out.println();
    parser.printHelpOn(System.out);
  }

  public void getSchema(ZKConnection zkclient,
                        String zkserver,
                        String d2path,
                        String cluster,
                        String service) throws URISyntaxException,
          InterruptedException,
          ExecutionException,
          IOException,
          PropertyStoreException, TimeoutException
  {
    if (hasService(zkclient, zkserver, d2path, cluster, service))
    {

      DynamicClient client = new DynamicClient(getLoadBalancer(zkclient, zkserver, d2path, service), null);

      URI uri = URI.create("d2://" + service + "/");

      RpcRequest req = new RpcRequestBuilder(uri).setEntity("".getBytes("UTF-8")).build();

      try
      {
        Future<RpcResponse> response = client.rpcRequest(req);
        String responseString = response.get().getEntity().asString("UTF-8");

        System.out.println(uri + " response: " + responseString);
      }
      finally
      {
        client.shutdown();
      }
    }
    else
    {
      System.err.println("Service '" + service + "' is not defined for cluster '"
          + cluster + "'.");
    }

  }

  public void createClient() throws URISyntaxException,
                                          InterruptedException,
                                          ExecutionException,
                                          IOException,
                                          PropertyStoreException,
                                          TimeoutException
  {
    _client = createClient(_zkclient, _zkserver, _d2path, _service) ;
  }

  public DynamicClient createClient(ZKConnection zkclient, String zkserver, String d2path,String service)
                                  throws URISyntaxException,
                                  InterruptedException,
                                  ExecutionException,
                                  IOException,
                                  PropertyStoreException,
                                  TimeoutException
  {
    return new DynamicClient(getLoadBalancer(zkclient, zkserver, d2path, service), null);
  }

  public DynamicClient getClient()
  {
    return _client;
  }

  public String sendRequest(DynamicClient client, String cluster, String service, String request) throws URISyntaxException,
                                                                                                         InterruptedException,
                                                                                                         ExecutionException,
                                                                                                         IOException,
                                                                                                         PropertyStoreException,
                                                                                                         TimeoutException
  {
    return sendRequest(client, _zkclient,_zkserver, _d2path, cluster, service, "", request, false);
  }

  public String sendRequest(DynamicClient client,
                            ZKConnection zkclient,
                            String zkserver,
                            String d2path,
                            String cluster,
                            String service,
                            String method,
                            String request,
                            boolean performShutdown) throws URISyntaxException,
                                                            InterruptedException,
                                                            ExecutionException,
                                                            IOException,
                                                            PropertyStoreException,
                                                            TimeoutException
    {
      String responseString = null;

      URI uri = URI.create("d2://" + service + method);

      try
      {
        if (! _requestType.equalsIgnoreCase("rest"))
        {
          RpcRequest req =
            new RpcRequestBuilder(uri).setEntity(request.getBytes("UTF-8")).build();
          Future<RpcResponse> response = client.rpcRequest(req);
          responseString = response.get().getEntity().asString("UTF-8");
        }
        else
        {
          RestRequest restRequest = new RestRequestBuilder(uri).setEntity(request.getBytes("UTF-8")).build();
          Future<RestResponse> response = client.restRequest(restRequest, new RequestContext());
          responseString = response.get().getEntity().asString("UTF-8");
        }

        System.out.println("===============================");
        System.out.println("REQUEST:" + request);
        int indx = request.indexOf("ids");
        System.out.println(uri + " RESPONSE: ");
        System.out.println(responseString);
      }
      finally
      {
        if (performShutdown)
        {
          client.shutdown();
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

  public void setRequestType (String requestType)
  {
    _requestType = requestType;
  }

  public ZKConnection createZkClient(String zkserverHostPort)
  {
    //URI storeUri = URI.create(zkserverHostPort);
    //_zkclient = new ZKConnection(storeUri.getHost()+":"+storeUri.getPort(), 10000);
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
    _zkclient.waitForState(KeeperState.SyncConnected, 30, TimeUnit.SECONDS);
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

  public void shutdown() throws Exception
  {
    try
    {
      if (_client != null)
      {
        _client.shutdown();
      }
    }
    catch (Exception e)
    {
      _log.error("Failed to shutdown dynamic client.");
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

  @SuppressWarnings("unchecked")
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

    // chains
    PropertyEventThread thread = new PropertyEventThread("lb client event thread");

    // start up the world
    thread.start();

    PropertyEventBus<ServiceProperties> serviceBus =
        new PropertyEventBusImpl<ServiceProperties>(thread, zkServiceRegistry);
    PropertyEventBus<UriProperties> uriBus =
        new PropertyEventBusImpl<UriProperties>(thread, zkUriRegistry);
    PropertyEventBus<ClusterProperties> clusterBus =
        new PropertyEventBusImpl<ClusterProperties>(thread, zkClusterRegistry);

    Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories =
        new HashMap<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>>();

    loadBalancerStrategyFactories.put(zkServiceRegistry.get(service)
                                                       .getLoadBalancerStrategyName(),
                                      new RandomLoadBalancerStrategyFactory());

    Map<String, TransportClientFactory> clientFactories =
        new HashMap<String, TransportClientFactory>();

    clientFactories.put("http", new HttpClientFactory());

    // create the state
    SimpleLoadBalancerState state =
        new SimpleLoadBalancerState(thread,
                                    uriBus,
                                    clusterBus,
                                    serviceBus,
                                    clientFactories,
                                    loadBalancerStrategyFactories);

    SimpleLoadBalancer balancer = new SimpleLoadBalancer(state, 5, TimeUnit.SECONDS);

    FutureCallback<None> callback = new FutureCallback<None>();
    balancer.start(callback);
    callback.get(5, TimeUnit.SECONDS);

    new JmxManager().registerLoadBalancer("balancer", balancer)
                    .registerLoadBalancerState("state", state)
                    .registerPropertyEventThread("thread", thread)
                    .registerZooKeeperPermanentStore("zkClusterRegistry",
                                                     zkClusterRegistry)
                    .registerZooKeeperPermanentStore("zkServiceRegistry",
                                                     zkServiceRegistry)
                    .registerZooKeeperEphemeralStore("zkUriRegistry", zkUriRegistry);

    return balancer;
  }

  public  Set< UriProperties> getServiceURIsProps(String zkserver, String d2path, String serviceName) throws IOException,
  IllegalStateException,
  URISyntaxException,
  PropertyStoreException
  {
    Set<UriProperties> uriprops = new HashSet<UriProperties>();
    // zk stores
    String clstoreString = zkserver + ZKFSUtil.clusterPath(d2path);
    String scstoreString = zkserver + ZKFSUtil.servicePath(d2path);
    String uristoreString = zkserver + ZKFSUtil.uriPath(d2path);

    ZooKeeperPermanentStore<ClusterProperties> zkClusterRegistry =
        (ZooKeeperPermanentStore<ClusterProperties>) getStore(_zkclient,
                                                              clstoreString,
                                                              new ClusterPropertiesJsonSerializer());
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
      String clusterName = zkServiceRegistry.get(serviceName).getClusterName();
      UriProperties uripros = zkUriRegistry.get(clusterName);
      uriprops.add( uripros);
    }

    return uriprops;
  }

  public  Map<String, UriProperties> getServiceClustersURIsInfo(String zkserver, String d2path, String serviceName) throws IOException,
  IllegalStateException,
  URISyntaxException,
  PropertyStoreException
  {
    Map<String,UriProperties> map = new HashMap<String,UriProperties>();
    // zk stores
    String clstoreString = zkserver + ZKFSUtil.clusterPath(d2path);
    String scstoreString = zkserver + ZKFSUtil.servicePath(d2path);
    String uristoreString = zkserver + ZKFSUtil.uriPath(d2path);

    ZooKeeperPermanentStore<ClusterProperties> zkClusterRegistry =
        (ZooKeeperPermanentStore<ClusterProperties>) getStore(_zkclient,
                                                              clstoreString,
                                                              new ClusterPropertiesJsonSerializer());
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

  public static void printStore(ZooKeeperPermanentStore<ClusterProperties> zkClusterRegistry,
                                ZooKeeperEphemeralStore<UriProperties> zkUriRegistry,
                                ZooKeeperPermanentStore<ServiceProperties> zkServiceRegistry,
                                String cluster,
                                String service) throws URISyntaxException,
      PropertyStoreException
  {
    printStore(zkClusterRegistry, zkUriRegistry, cluster);
    if (zkServiceRegistry.get(service).getClusterName().equals(cluster))
    {
      printService(zkServiceRegistry, service);
    }
  }

  public static void printStores(ZKConnection zkclient, String zkserver, String d2path) throws IOException,
      IllegalStateException,
      URISyntaxException,
      PropertyStoreException,
      Exception
  {
    int serviceCount = 0;
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

    System.out.println("ZKServer:" + zkserver + " Total Clusters:"
        + currentclusters.size());
    System.out.println("ZKServer:" + zkserver + " Total Services:"
        + serviceCount);
    System.out.println("ZKServer:" + zkserver + " Total URIs:" + currenturis.size());

    System.out.println("============================================================");
    System.out.println("SERVICE GROUPS");

    for (String serviceGroup : servicesGroupMap.keySet())
    {
      System.out.println("GROUP:"+serviceGroup+"           Services:"+servicesGroupMap.get(serviceGroup));
    }

    for (String cluster : currentclusters)
    {
      int count = 0;
      System.out.println("============================================================");
      System.out.println("CLUSTER '" + cluster + "':");

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
        	    System.out.println("-------------------");
        	    System.out.println("SERVICE '" + service + "':");

        	    printStore(zkClusterRegistry, zkUriRegistry, zkServiceRegistryMap.get(serviceGroup), cluster, service);
        	    count++;
        	    break;
        	  }
        	}
          }
        }
      }
      if (count == 0)
      {
        printStore(zkClusterRegistry, zkUriRegistry, cluster);
        System.out.println("No services were found in this cluster.");
      }
    }
  }

}
