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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.KeyValuePair;

import org.apache.zookeeper.Watcher.Event.KeeperState;

import sun.jvmstat.monitor.HostIdentifier;
import sun.jvmstat.monitor.MonitoredHost;

import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ClusterPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.ServicePropertiesJsonSerializer;
import com.linkedin.d2.discovery.PropertySerializer;
import com.linkedin.d2.discovery.event.PropertyEventThread.PropertyEventShutdownCallback;
import com.linkedin.d2.discovery.stores.PropertyStore;
import com.linkedin.d2.discovery.stores.PropertyStoreException;
import com.linkedin.d2.discovery.stores.file.FileStore;
import com.linkedin.d2.discovery.stores.zk.ZKConnection;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperPermanentStore;

public class LoadBalancerCli
{
  private static final String CONNECTOR_ADDRESS =
                                                    "com.sun.management.jmxremote.localConnectorAddress";

  public static void main(String[] args) throws Exception
  {
    new LoadBalancerCli(args);
  }

  public static List<String> asList(String... args)
  {
    return Arrays.asList(args);
  }

  public LoadBalancerCli(String[] args) throws Exception
  {
    OptionParser parser = new OptionParser();

    parser.acceptsAll(asList("help", "h"), "Show help.");

    OptionSpec<String> delete =
        parser.acceptsAll(asList("delete", "d"), "Delete a property.").withRequiredArg();
    OptionSpec<String> get =
        parser.acceptsAll(asList("get", "g"), "Read a property.").withRequiredArg();
    OptionSpec<String> toggle =
        parser.acceptsAll(asList("toggle", "t"),
                          "Toggle all toggling stores. By default, will connect to all localhost JMX processes, and toggle all toggling stores in com.linkedin.d2 to \"enabled\".")
              .withOptionalArg();
    OptionSpec<String> enabled =
        parser.acceptsAll(asList("enabled", "e"),
                          "To be used in conjunction with --toggle. Enabled allows you to toggle a toggling store on, or off.")
              .withRequiredArg();
    OptionSpec<String> store =
        parser.acceptsAll(asList("store", "s"), "Specify the store to interact with.")
              .withRequiredArg();
    OptionSpec<String> serializer =
        parser.acceptsAll(asList("serializer", "z"),
                          "Specify a serializer to when reading/writing a cluster or service.")
              .withRequiredArg();
    OptionSpec<String> service =
        parser.acceptsAll(asList("put-service", "S"), "Put a service into a store.")
              .withRequiredArg();
    OptionSpec<String> serviceCluster =
        parser.acceptsAll(asList("cluster", "c"),
                          "Specify a cluster that a service belongs to.")
              .withRequiredArg();
    OptionSpec<String> servicePath =
        parser.acceptsAll(asList("path", "p"), "Specify a path for a service.")
              .withRequiredArg();
    OptionSpec<String> serviceBalancer =
        parser.acceptsAll(asList("balancer", "b"),
                          "Specify a balancer to be used with a service.")
              .withRequiredArg();
    OptionSpec<String> cluster =
        parser.acceptsAll(asList("put-cluster", "C"), "Put a cluster into a store.")
              .withRequiredArg();
    OptionSpec<String> clusterScheme =
        parser.acceptsAll(asList("schemes", "H"),
                          "Specify prioritized schemes for a cluster.").withRequiredArg();
    OptionSpec<String> clusterBanned =
        parser.acceptsAll(asList("ban", "B"), "Bans a URI from a cluster.")
              .withRequiredArg();
    OptionSpec<String> clusterProperty =
        parser.acceptsAll(asList("property", "p"),
                          "Specify arbitrary properties for a cluster").withRequiredArg();

    OptionSet options = parser.parse(args);

    if (options.has(service))
    {
      putService(options.valueOf(service),
                 options.valueOf(serviceCluster),
                 options.valueOf(servicePath),
                 options.valueOf(serviceBalancer),
                 options.valueOf(store));
    }
    else if (options.has(cluster))
    {
      Map<String, String> properties = new HashMap<String, String>();
      Set<URI> banned = new HashSet<URI>();

      if (options.has(clusterProperty))
      {
        for (String propertyString : options.valuesOf(clusterBanned))
        {
          KeyValuePair kv = KeyValuePair.valueOf(propertyString);

          properties.put(kv.key, kv.value);
        }
      }

      if (options.has(clusterBanned))
      {
        for (String bannedString : options.valuesOf(clusterBanned))
        {
          banned.add(URI.create(bannedString));
        }
      }

      putCluster(options.valueOf(cluster),
                 options.valuesOf(clusterScheme),
                 properties,
                 banned,
                 options.valueOf(store));
    }
    else if (options.has(delete))
    {
      delete(options.valueOf(delete), options.valueOf(store));
    }
    else if (options.has(get))
    {
      get(options.valueOf(get), options.valueOf(serializer), options.valueOf(store));
    }
    else if (options.has(toggle))
    {
      String host = options.valueOf(toggle);
      boolean toggled = !"false".equals(options.valueOf(enabled));

      resetTogglingStores((host == null) ? "localhost" : host, toggled);
    }
    else
    {
      System.out.println("Examples");
      System.out.println("========");
      System.out.println();
      System.out.println("--put-cluster=cluster-1 --schemes=http --store=zk://localhost:2181/d2/clusters");
      System.out.println("--put-service=service-1 --cluster=cluster-1 --path=/service-1 --balancer=degrader --store=zk://localhost:2181/d2/services");
      System.out.println("--get=service-1 --serializer=com.linkedin.d2.balancer.properties.ServicePropertiesJsonSerializer --store=zk://localhost:2181/d2/services");
      System.out.println("--get=cluster-1 --serializer=com.linkedin.d2.balancer.properties.ClusterPropertiesJsonSerializer --store=zk://localhost:2181/d2/clusters");
      System.out.println("--delete=cluster-1 --store=zk://localhost:2181/d2/clusters");
      System.out.println("--toggle");
      System.out.println("--toggle=localhost --enabled=false");
      System.out.println();
      parser.printHelpOn(System.out);
    }
  }

  public static void putService(String serviceName,
                                String clusterName,
                                String path,
                                String balancer,
                                String storeString) throws URISyntaxException,
          InterruptedException, IOException, PropertyStoreException, TimeoutException,
          ExecutionException
  {
    ServiceProperties serviceProperties =
        new ServiceProperties(serviceName, clusterName, path, balancer);

    PropertyStore<ServiceProperties> store =
        getStore(storeString, new ServicePropertiesJsonSerializer());

    store.put(serviceName, serviceProperties);
    shutdownAndWait(store, 60, TimeUnit.SECONDS);
  }

  public static void putCluster(String clusterName,
                                List<String> schemes,
                                Map<String, String> properties,
                                Set<URI> banned,
                                String storeString) throws URISyntaxException,
          InterruptedException, IOException, PropertyStoreException, TimeoutException,
          ExecutionException
  {
    ClusterProperties clusterProperties =
        new ClusterProperties(clusterName, schemes, properties, banned);

    PropertyStore<ClusterProperties> store =
        getStore(storeString, new ClusterPropertiesJsonSerializer());

    store.put(clusterName, clusterProperties);
    shutdownAndWait(store, 60, TimeUnit.SECONDS);
  }

  public static void delete(String listenTo, String storeString) throws URISyntaxException,
          InterruptedException, IOException, PropertyStoreException, TimeoutException,
          ExecutionException
  {
    PropertyStore<?> store = getStore(storeString, null);

    store.remove(listenTo);
    shutdownAndWait(store, 60, TimeUnit.SECONDS);
  }

  @SuppressWarnings("unchecked")
  public static <T> void get(String listenTo, String serializerString, String storeString)
          throws URISyntaxException,
          InstantiationException,
          IllegalAccessException,
          ClassNotFoundException,
          UnsupportedEncodingException,
          InterruptedException,
          IOException,
          PropertyStoreException, TimeoutException, ExecutionException
  {
    PropertySerializer<T> serializer =
        (PropertySerializer<T>) (Class.forName(serializerString)).newInstance();

    PropertyStore<T> store = getStore(storeString, serializer);

    System.out.println(new String(serializer.toBytes(store.get(listenTo)), "UTF-8"));

    shutdownAndWait(store, 60, TimeUnit.SECONDS);
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

        // no connector address, so we start the JMX agent
        // if (connectorAddress == null)
        // {
        // String agent =
        // vm.getSystemProperties().getProperty("java.home") + File.separator + "lib"
        // + File.separator + "management-agent.jar";
        // vm.loadAgent(agent);
        //
        // // agent is started, get the connector address
        // connectorAddress = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
        // }

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

  private static <T> PropertyStore<T> getStore(String store,
                                               PropertySerializer<T> serializer)
          throws URISyntaxException,
          IOException, PropertyStoreException, InterruptedException, TimeoutException,
          ExecutionException
  {
    URI storeUri = URI.create(store);

    if (storeUri.getScheme() != null)
    {
      if (storeUri.getScheme().equals("zk"))
      {
        ZKConnection zk = new ZKConnection(storeUri.getHost() + ":" + storeUri.getPort(), 30000);
        zk.start();

        zk.waitForState(KeeperState.SyncConnected, 30, TimeUnit.SECONDS);

        ZooKeeperPermanentStore<T> zks = new ZooKeeperPermanentStore<T>(zk, serializer, storeUri.getPath());
        FutureCallback<None> callback = new FutureCallback<None>();
        zks.start(callback);
        callback.get(30, TimeUnit.SECONDS);
        return zks;
      }
      else
      {
        throw new URISyntaxException(store,
                                     "Unable to parse store uri. Only zk and file stores are supported.");
      }
    }
    else
    {
      // assume it's a local file
      return new FileStore<T>(storeUri.getPath(), ".json", serializer);
    }

  }

  public static void shutdownAndWait(PropertyStore<?> store, long timeout, TimeUnit unit)
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
