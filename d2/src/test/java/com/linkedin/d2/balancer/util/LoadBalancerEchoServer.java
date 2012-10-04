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

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.LoadBalancerServer;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.properties.UriPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.UriPropertiesMerger;
import com.linkedin.d2.balancer.servers.ZooKeeperServer;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.d2.discovery.stores.PropertyStoreException;
import com.linkedin.d2.discovery.stores.zk.ZKConnection;
import com.linkedin.d2.discovery.stores.zk.ZKTestUtil;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperEphemeralStore;
import com.linkedin.d2.jmx.JmxManager;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.message.rpc.RpcRequest;
import com.linkedin.r2.message.rpc.RpcResponse;
import com.linkedin.r2.message.rpc.RpcResponseBuilder;
import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.r2.transport.common.RpcRequestHandler;
import com.linkedin.r2.transport.common.Server;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import com.linkedin.r2.transport.http.server.HttpJettyServer;
import com.linkedin.r2.transport.http.server.HttpServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


// Server startup
// ./lb-echo-server.sh 127.0.0.1 2181 127.0.0.1 2345 http /d2 cluster1 service1 service2 service3

public class LoadBalancerEchoServer
{
  private final String       _basePath;
  private final String       _host;
  private final int          _port;
  private final String       _scheme;
  private final String       _cluster;
  private final Set<String>  _validPaths;
  private final URI          _uri;
  private Server             _server;
  private LoadBalancerServer _announcer;
  private boolean            _isStopped = false;
  private int                _timeout = 5000;
  private final Map<Integer, Double> _partitionWeight;

  private final static String RESPONSE_POSTFIX = ".FromEchoServerPort:";

  private static final Logger _log = LoggerFactory.getLogger(LoadBalancerEchoServer.class);

  public static void main(String[] args) throws InterruptedException,
      IOException,
      URISyntaxException,
      ExecutionException,
      PropertyStoreException,
      NumberFormatException,
      TimeoutException
  {
    LoadBalancerEchoServer server =
        new LoadBalancerEchoServer(args[0],
                                   Integer.parseInt(args[1]),
                                   args[2],
                                   Integer.parseInt(args[3]),
                                   args[4],
                                   args[5],
                                   args[6],
                                   Arrays.copyOfRange(args, 7, args.length));

    server.startServer();
    server.markUp();
  }

  public LoadBalancerEchoServer(String zookeeperHost,
                                int zookeeperPort,
                                String echoServerHost,
                                int echoServerPort,
                                String scheme,
                                String basePath,
                                String cluster,
                                String... services) throws IOException,
      PropertyStoreException,
      InterruptedException,
      TimeoutException
  {
    this(zookeeperHost, zookeeperPort, echoServerHost, echoServerPort, 5000, scheme, basePath, cluster, null, services); 
  }
  
  public LoadBalancerEchoServer(String zookeeperHost,
                                int zookeeperPort,
                                String echoServerHost,
                                int echoServerPort,
                                String scheme,
                                String basePath,
                                String cluster,
                                Map<Integer, Double> partitionWeight,
                                String... services) throws IOException,
      PropertyStoreException,
      InterruptedException,
      TimeoutException
  {
    this(zookeeperHost, zookeeperPort, echoServerHost, echoServerPort, 5000, scheme, basePath, cluster, partitionWeight, services); 
  }

  public LoadBalancerEchoServer(String zookeeperHost,
                                int zookeeperPort,
                                String echoServerHost,
                                int echoServerPort,
                                int timeout,
                                String scheme,
                                String basePath,
                                String cluster,
                                String... services) throws IOException,
      PropertyStoreException,
      InterruptedException,
      TimeoutException
  {
    this(zookeeperHost, zookeeperPort, echoServerHost, echoServerPort, timeout, scheme, basePath, cluster, null, services); 
  }
  
  public LoadBalancerEchoServer(String zookeeperHost,
                                int zookeeperPort,
                                String echoServerHost,
                                int echoServerPort,
                                int timeout,
                                String scheme,
                                String basePath,
                                String cluster,
                                Map<Integer, Double> partitionWeight,
                                String... services) throws IOException,
      PropertyStoreException,
      InterruptedException,
      TimeoutException
  {
    _host = echoServerHost;
    _port = echoServerPort;
    _scheme = scheme;
    _timeout = timeout;
    _cluster = cluster;
    _partitionWeight = partitionWeight;
    _basePath = basePath;
    _uri = URI.create(_scheme + "://" + echoServerHost + ":" + _port + "/" + _cluster);

    _log.info("Server Uri:"+_uri);

    Set<String> validPaths = new HashSet<String>();

    for (String service : services)
    {
      validPaths.add(File.separator + _cluster + File.separator + service);
    }

    _validPaths = Collections.unmodifiableSet(validPaths);

    // set up the lb announcer. if we can't connect, give up. in production, there would
    // be a JMX hook to "retry" if we're not connected.

    final ZKConnection zkClient = ZKTestUtil.getConnection(zookeeperHost+":"+zookeeperPort, _timeout);

    ZooKeeperEphemeralStore<UriProperties> zk =
        new ZooKeeperEphemeralStore<UriProperties>(zkClient,
                                                   new UriPropertiesJsonSerializer(),
                                                   new UriPropertiesMerger(),
                                                   _basePath+"/uris");

    final CountDownLatch wait = new CountDownLatch(1);

    zk.start(new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
        throw new RuntimeException("ultra fail", e);
      }

      @Override
      public void onSuccess(None t)
      {
        wait.countDown();
      }
    });

    wait.await();

    _announcer = new ZooKeeperServer(zk);

    new JmxManager().registerZooKeeperServer("server", (ZooKeeperServer) _announcer);
    new JmxManager().registerZooKeeperEphemeralStore("uris", zk);
    // announce that the server has started
  }

  public String getHost()
  {
    return _host;
  }

  public int getPort()
  {
    return _port;
  }

  public Map<Integer, Double> getWeight()
  {
    return _partitionWeight;
  }
  
  public void startServer() throws IOException,
      InterruptedException,
      URISyntaxException
  {
    final RpcDispatcher rpcDispatcher = new RpcDispatcher();
    final RestDispatcher restDispatcher = new RestDispatcher();

    final TransportDispatcherBuilder dispatcherBuilder = new TransportDispatcherBuilder();
    for (String validPath : _validPaths)
    {
      dispatcherBuilder.addRpcHandler(URI.create(validPath), rpcDispatcher);
      dispatcherBuilder.addRestHandler(URI.create(validPath), restDispatcher);
    }
    final TransportDispatcher dispatcher = dispatcherBuilder.build();

    // start the server
    if (_scheme.equals("http"))
    {
      _server = getHttpServer(dispatcher);
    }

    _server.start();
  }

  public void stopServer() throws IOException, InterruptedException, NoSuchFieldException, IllegalAccessException
  {
    _server.stop();
    _server.waitForStop();
  }

  public boolean isStopped()
  {
    try
    {
      Field serverField = HttpJettyServer.class.getDeclaredField("_server");
      serverField.setAccessible(true);
      org.mortbay.jetty.Server jettyServer = (org.mortbay.jetty.Server)serverField.get(_server);

     _isStopped = jettyServer.isStopped();

    }
    catch (NoSuchFieldException e)
    {
      // do nothing
      e.printStackTrace();
    }
    catch (IllegalAccessException e)
    {
      e.printStackTrace();
    }

    return _isStopped;

  }

  public void markUp() throws PropertyStoreException
  {
    markUp(_partitionWeight);
  }
  
  public void markUp(Map<Integer, Double> partitionWeight) throws PropertyStoreException
  {
    FutureCallback<None> callback = new FutureCallback<None>();
    Map<Integer, PartitionData> partitionDataMap = new HashMap<Integer, PartitionData>();
    if (partitionWeight != null)
    {
      for (int partitionId : partitionWeight.keySet())
      {
        partitionDataMap.put(partitionId, new PartitionData(partitionWeight.get(partitionId)));
      }
    }
    else
    {
      partitionDataMap.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
    }
    _announcer.markUp(_cluster, _uri, partitionDataMap, callback);

    try
    {
      callback.get(10, TimeUnit.SECONDS);
    }
    catch (Exception e)
    {
      throw new PropertyStoreException(e);
    }
  }

  public void markDown() throws PropertyStoreException
  {
    FutureCallback<None> callback = new FutureCallback<None>();
    _announcer.markDown(_cluster, _uri, callback);
    try
    {
      callback.get(10, TimeUnit.SECONDS);
    }
    catch (Exception e)
    {
      throw new PropertyStoreException(e);
    }
  }

  public RestResponse getExceptionTypeFromRequest(String request)
  {
    if (request.contains("PORT:"+_port))
    {
      Pattern pattern = Pattern.compile("EXCEPTION=(\\w+)", Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(request);
      int status = -9999;
      while (matcher.find())
      {
        if (matcher.group(1).contains("NOT_FOUND"))
        {
          status = RestStatus.NOT_FOUND;
        }
        else if (matcher.group(1).contains("INTERNAL_SERVER_ERROR"))
        {
          status = RestStatus.INTERNAL_SERVER_ERROR;
        }
        else if (matcher.group(1).contains("BAD_REQUEST"))
        {
          status = RestStatus.BAD_REQUEST;
        }

        final RestResponse res = new RestResponseBuilder().setStatus(status).build();
        final RestException restException = new RestException(res);

        return restException.getResponse();
      }
    }
    return null;
  }
  
  private Server getHttpServer(TransportDispatcher dispatcher)
  {
    return new HttpServerFactory().createServer(_port, dispatcher);
  }


  public long getDelayValueFromRequest(String request)
  {
    if (request.contains("PORT:"+_port))
    {
      Pattern pattern = Pattern.compile("DELAY=(\\d+)", Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(request);
      while (matcher.find())
      {
        return Long.parseLong(matcher.group(1));
      }
    }
    return 0;
  }

  public static String getResponsePostfixString()
  {
    return RESPONSE_POSTFIX;
  }
  
  public String getResponsePostfixStringWithPort()
  {
    return RESPONSE_POSTFIX+_port;
  }
  
  private String printWeights()
  {
    StringBuilder sb = new StringBuilder();
    Map<Integer, Double> partitionDataMap = new HashMap<Integer, Double>();
    if (_partitionWeight != null)
    {
      partitionDataMap = _partitionWeight;
    }
    else
    {
      partitionDataMap.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new Double(1d));
    }
      
    for (int partitionId : partitionDataMap.keySet())
    {
      sb.append(((sb.length() > 0) ? "," : ""));
      sb.append(partitionId);
      sb.append("/");
      sb.append(partitionDataMap.get(partitionId));
    }

    return sb.toString();
  }
  
  
  public class RpcDispatcher implements RpcRequestHandler
  {
    @Override
    public void handleRequest(RpcRequest request, final Callback<RpcResponse> callback)
    {
      System.out.println("RPC server request: " +
      request.getEntity().asString("UTF-8"));

      callback.onSuccess(new RpcResponseBuilder().setEntity(request.getEntity()).build());
    }
  }

  public class RestDispatcher implements RestRequestHandler
  {
    @Override
    public void handleRequest(RestRequest request, final Callback<RestResponse> callback)
    {
      System.out.println("REST server request: " + request.getEntity().asString("UTF-8"));

      String requestStr = request.getEntity().asString("UTF-8");
      String response = request.getEntity() + ";WEIGHT=" + printWeights() + getResponsePostfixStringWithPort();
      isStopped();

       // Return response only if server is running
      if (! _isStopped)
      {
        try
        {
          Thread.sleep(getDelayValueFromRequest(requestStr));
        }
        catch (InterruptedException e)
        {
        }

        if (requestStr.contains("EXCEPTION="))
        {
          callback.onSuccess(getExceptionTypeFromRequest(requestStr));
        }
        else
        {
          callback.onSuccess(new RestResponseBuilder().setEntity(response.getBytes()).build());
        }
      }
    }
  }

}
