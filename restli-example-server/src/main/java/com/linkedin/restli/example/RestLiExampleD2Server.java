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

package com.linkedin.restli.example;


import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.servers.ZooKeeperConnectionManager;
import com.linkedin.r2.transport.http.server.HttpServer;
import com.linkedin.restli.example.impl.ZooKeeperConnectionBuilder;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;


/**
 * Example server to demonstrate a HTTP server with basic D2 features.
 * Server will be listening on localhost:7279.
 * Assume ZooKeeper is running on localhost:2121.
 *
 * @author Keren Jin
 */
public class RestLiExampleD2Server
{
  public static void main(String[] args) throws Exception
  {
    // write D2-related configuration to ZooKeeper
    // all the configuration here are permanent, no need to re-write as long as ZooKeeper still has the data
    // therefore in real case, do this "discovery" step separately

    final HttpServer server = RestLiExampleBasicServer.createServer();
    final ZooKeeperConnectionManager zkConn = createZkConn();

    startServer(server, zkConn);

    System.out.println("Example server with D2 is running with URL " + RestLiExampleBasicServer.getServerUrl() + ". Press any key to stop server.");
    System.in.read();

    stopServer(server, zkConn);
  }

  /**
   * @return {@link ZooKeeperConnectionManager} with the server and cluster information set up
   */
  private static ZooKeeperConnectionManager createZkConn()
  {
    return new ZooKeeperConnectionBuilder()
        .setZooKeeperHostname(D2ConfigDiscovery.ZOOKEEPER_HOSTNAME)
        .setZooKeeperPort(D2ConfigDiscovery.ZOOKEEPER_PORT)
        .setBasePath(D2ConfigDiscovery.ZOOKEEPER_BASE_PATH)
        .setCluster("RestLiExampleCluster")
        .setUri(RestLiExampleBasicServer.getServerUrl())
        .build();
  }

  private static void startServer(HttpServer server, final ZooKeeperConnectionManager zkConn)
      throws IOException, InterruptedException
  {
    RestLiExampleBasicServer.startServer(server);

    /* start ZooKeeper connection and announce server information in ZooKeeper asynchronously
     * {@link ZooKeeperConnectionManager} will maintain announcement even the connection is interrupted and resumed
     *
     * make sure announce server after it become ready
     *
     * if no callback is needed, simple version is
     * zkConn.start(new FutureCallback<None>());
     */
    final CountDownLatch latch = new CountDownLatch(1);
    zkConn.start(new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
        throw new RuntimeException("Unable to start ZooKeeper announcement.", e);
      }

      @Override
      public void onSuccess(None result)
      {
        latch.countDown();
        System.out.println("Server is announced in ZooKeeper and ready to serve D2 request.");
      }
    });
    latch.await();
  }

  private static void stopServer(HttpServer server, final ZooKeeperConnectionManager zkConn)
      throws IOException, InterruptedException
  {
    /* remove the announcement information so that client will not try to send request to the server any more
     * then shutdown the ZooKeeper connection
     *
     * make sure to remove announcement before server gets shutdown
     *
     * if no callback is needed, use FutureCallback<None>
     */
    final CountDownLatch latch = new CountDownLatch(1);
    zkConn.shutdown(new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
        throw new RuntimeException("Unable to remove server announcement from ZooKeeper.", e);
      }

      @Override
      public void onSuccess(None result)
      {
        latch.countDown();
        System.out.println("Server announcement is removed from ZooKeeper.");
      }
    });
    latch.await();

    RestLiExampleBasicServer.stopServer(server);
  }
}
