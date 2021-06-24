/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.example.d2.server;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.config.PartitionDataFactory;
import com.linkedin.d2.balancer.servers.ZKUriStoreFactory;
import com.linkedin.d2.balancer.servers.ZooKeeperAnnouncer;
import com.linkedin.d2.balancer.servers.ZooKeeperConnectionManager;
import com.linkedin.d2.balancer.servers.ZooKeeperServer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * creates dummy EchoServers and announces information about these servers to zookeeper.
 * The servers are created based on the configuration located in:
 * d2-quickstart/server/src/main/config/server.json
 *
 * @author Oby Sumampouw
 */
public class ExampleD2Server
{
  public static void main(String[] args)
      throws IOException, ParseException
  {
    //get server configuration
    JSONObject json = parseConfig();
    System.out.println("Finished parsing the server config");
    List<Map<String, Object>> d2ServersConfigs = (List <Map<String, Object>>)
        json.get("d2Servers");
    List<Map<String, Object>> echoServerConfigs = (List <Map<String, Object>>)
        json.get("echoServers");

    Long timeout = (Long)json.get("announcerStartTimeout");
    Long shutdownTimeout = (Long)json.get("announcerShutdownTimeout");

    //create and start echo servers
    List<EchoServer> echoServers = createAndStartEchoServers(echoServerConfigs);
    System.out.println("Finished creating echo servers");

    //create d2 announcers (for announcing the existence of these servers to the world)
    ZooKeeperAnnouncer[] zkAnnouncers = createZkAnnouncers(d2ServersConfigs);

    //manager will keep track of the lifecycle of d2 announcers i.e. start publishing
    //once connected to zookeeper, reconnect if zookeeper is disconnected, etc
    ZooKeeperConnectionManager manager = createZkManager(json, zkAnnouncers);

    System.out.println("Starting zookeeper announcers");
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    startAnnouncers(manager, executorService, timeout);

    //waiting for user to turn off this process
    System.in.read();

    System.out.println("shutting down...");
    shutdown(echoServers, manager, executorService, shutdownTimeout);
  }

  private static JSONObject parseConfig ()
      throws IOException, ParseException
  {
    String path = new File(new File(".").getAbsolutePath()).getCanonicalPath() +
        "/src/main/config/server.json";
    JSONParser parser = new JSONParser();
    Object object = parser.parse(new FileReader(path));
    return (JSONObject) object;
  }

  private static List<EchoServer> createAndStartEchoServers(List<Map<String, Object>>
                                                                echoServerConfigs)
      throws IOException
  {
    List<EchoServer> echoServers = new ArrayList<>();
    for (Map<String, Object> echoServerConfig : echoServerConfigs)
    {
      int port = ((Long)echoServerConfig.get("port")).intValue();
      int threadPoolSize = ((Long)echoServerConfig.get("threadPoolSize")).intValue();
      long delay = (Long)echoServerConfig.get("delay");
      String name = (String)echoServerConfig.get("name");
      List<String> contextPaths = (List<String>)echoServerConfig.get("contextPaths");

      EchoServer echoServer = new EchoServer(port, name, contextPaths,
                                             threadPoolSize, delay);
      echoServer.start();
      echoServers.add(echoServer);
    }
    return echoServers;
  }

  private static ZooKeeperAnnouncer[] createZkAnnouncers(List<Map<String, Object>>
                                                             d2ServersConfigs)
  {
    ZooKeeperAnnouncer[] zkAnnouncers = new ZooKeeperAnnouncer[d2ServersConfigs.size()];
    for (int i = 0; i < d2ServersConfigs.size(); i++)
    {
      Map<String, Object> d2ServerConfig = d2ServersConfigs.get(i);
      ZooKeeperServer server = new ZooKeeperServer();
      ZooKeeperAnnouncer announcer = new ZooKeeperAnnouncer(server);
      Map<String, Object> partitionDataMap = (Map<String, Object>)
          d2ServerConfig.get("partitionData");
      announcer.setCluster((String) d2ServerConfig.get("d2Cluster"));
      announcer.setUri((String)d2ServerConfig.get("serverUri"));
      announcer.setWeightOrPartitionData(PartitionDataFactory.
                                                createPartitionDataMap(partitionDataMap));
      zkAnnouncers[i] = announcer;
    }
    return zkAnnouncers;
  }

  private static ZooKeeperConnectionManager createZkManager(JSONObject json,
                                                       ZooKeeperAnnouncer[] zkAnnouncers)
  {
    ZKUriStoreFactory factory = new ZKUriStoreFactory();
    String zkConnectString = (String)json.get("zkConnectString");
    int zkRetryLimit = ((Long)json.get("zkRetryLimit")).intValue();
    int zkSessionTimeout = ((Long)json.get("zkSessionTimeout")).intValue();
    String zkBasePath = (String)json.get("zkBasePath");
    return new ZooKeeperConnectionManager(zkConnectString,
                                          zkSessionTimeout,
                                          zkBasePath,
                                          factory,
                                          zkRetryLimit,
                                          zkAnnouncers);
  }

  private static void startAnnouncers(final ZooKeeperConnectionManager manager,
                                        ExecutorService executorService,
                                        Long timeout)
  {
    Future task = executorService.submit(new Runnable()
    {
      @Override
      public void run ()
      {
        manager.start(new Callback<None>()
        {
          @Override
          public void onError (Throwable e)
          {
            System.err.println("problem starting D2 announcers. Aborting...");
            e.printStackTrace();
            System.exit(1);
          }

          @Override
          public void onSuccess (None result)
          {
            System.out.println("D2 announcers successfully started. ");
            System.out.println("Press enter to stop echo servers and d2 announcers...");
          }
        });
      }
    });

    try
    {
      task.get(timeout, TimeUnit.MILLISECONDS);
    }
    catch (Exception e)
    {
      System.err.println("Cannot start zookeeper announcer. Timeout is set to " +
                             timeout + " ms");
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static void shutdown(List<EchoServer> echoServers,
                               final ZooKeeperConnectionManager manager,
                               ExecutorService executorService,
                               Long timeout)
      throws IOException
  {
    try
    {
      executorService.submit(new Runnable()
      {
        @Override
        public void run ()
        {
          manager.shutdown(new Callback<None>()
          {
            @Override
            public void onError (Throwable e)
            {
              System.err.println("problem stopping D2 announcers.");
              e.printStackTrace();
            }

            @Override
            public void onSuccess (None result)
            {
              System.out.println("D2 announcers successfully stopped.");
            }
          });
        }
      }).get(timeout, TimeUnit.MILLISECONDS);
    }
    catch (Exception e)
    {
      System.err.println("Cannot stop zookeeper announcer. Timeout is set to " +
                             timeout + " ms");
      e.printStackTrace();
    }
    finally
    {
      for (EchoServer echoServer : echoServers)
      {
        echoServer.stop();
      }
      executorService.shutdown();
    }
  }

}
