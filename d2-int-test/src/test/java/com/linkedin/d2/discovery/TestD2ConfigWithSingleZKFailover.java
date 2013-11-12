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

package com.linkedin.d2.discovery;


import com.linkedin.d2.D2BaseTest;
import com.linkedin.d2.balancer.clients.DynamicClient;
import com.linkedin.d2.balancer.util.LoadBalancerClientCli;
import com.linkedin.d2.balancer.util.LoadBalancerEchoServer;
import com.linkedin.d2.balancer.util.LoadBalancerUtil;
import com.linkedin.d2.discovery.stores.zk.ZKServer;
import com.linkedin.d2.discovery.stores.zk.ZKTestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

@Test (groups = {"d2integration"})
public class TestD2ConfigWithSingleZKFailover extends D2BaseTest
{
  private static final Logger _log = LoggerFactory.getLogger(TestD2ConfigWithSingleZKFailover.class);
  private ArrayList<LoadBalancerEchoServer> _echoServers;
  private static final String ZK_HOST = "127.0.0.1";
  private int                 _zkPort = -999;

  private static final String ECHO_SERVER_HOST = "127.0.0.1";
  private static final int ECHO_SERVER_PORT1 = 2351;
  private static final int ECHO_SERVER_PORT2 = 2352;
  private static final int ECHO_SERVER_PORT3 = 2353;

  private String _zkHosts;
  private String _zkUriString;
  private ZKServer _zkServer;
  private LoadBalancerClientCli _cli;
  private DynamicClient _client;

  @BeforeTest
  public void setup() throws IOException, Exception
  {
    // zkServer
    _zkServer = ZKTestUtil.startZKServer();
    _zkPort = _zkServer.getPort();
    _zkHosts = ZK_HOST+":" + _zkPort;
    _zkUriString = "zk://"+_zkHosts;
    
    // Register clusters/services  (two services per cluster)
    LoadBalancerClientCli.runDiscovery(_zkHosts, "/d2", D2_CONFIG_DATA);
    
    // Get LoadBalancer Client
    _cli = new LoadBalancerClientCli(_zkHosts, "/d2");

    // Echo servers startup
    startAllEchoServers();
    assertAllEchoServersRunning(_echoServers);

    _client = _cli.createClient(_cli.getZKClient(), _zkUriString, "/d2", "service-1_1");
    _log.info(LoadBalancerClientCli.printStores(_cli.getZKClient(), _zkUriString, "/d2"));

    assertAllEchoServersRegistered(_cli.getZKClient(), _zkUriString, _echoServers);
  }

  @BeforeMethod
  public void setupMethod() throws IOException, InterruptedException
  {
    try
    {
      _zkServer.startup();
    }
    catch (Exception e)
    {
    }

    try
    {
      _echoServers.get(1).startServer();
      _echoServers.get(2).startServer();
    }
    catch (Exception e)
    {
    }

    try
    {
      _echoServers.get(1).markUp();
      _echoServers.get(2).markUp();
    }
    catch (Exception e)
    {
    }

  }

  @AfterTest
  public void teardown() throws IOException, InterruptedException, Exception
  {
    try
    {
      _cli.shutdown();
    }
    catch (Exception e)
    {
      _log.info("LoadBalancerClientCli shutdown failed.");
      e.printStackTrace();
    }

    try
    {
      LoadBalancerUtil.syncShutdownClient(_client, _log);
    }
    catch (Exception e)
    {
    }

    try
    {
      _zkServer.shutdown();
    }
    catch (Exception e)
    {
      _log.info("zk server shutdown failed.");
    }

    stopAllEchoServers(_echoServers);
  }

  @Test
  public void testZkServerShutdown() throws IOException, InterruptedException, URISyntaxException, Exception
  {
    String [] expectedResponses;

    String msg = generateMessage(_zkUriString);
    expectedResponses = getExpectedResponses(0, msg);

    assertMatch(_cli.sendRequest(_client, "cluster-1","service-1_1", msg), expectedResponses);
    assertMatch(_cli.sendRequest(_client, "cluster-1","service-1_2", msg), expectedResponses);

    // Shutdown zookeeper
    _zkServer.shutdown(false);

    msg = generateMessage(_zkUriString);
    expectedResponses = getExpectedResponses(0, msg);
    // Verify echo servers are still processing responses when zookeeper is down
    assertMatch(_cli.sendRequest(_client, "cluster-1","service-1_1", msg), expectedResponses);
    assertMatch(_cli.sendRequest(_client, "cluster-1","service-1_2", msg), expectedResponses);
  }

  @Test
  public void testZkServerMultipleShutdownRestarts() throws IOException, InterruptedException, URISyntaxException, Exception
  {
    String [] expectedResponses;

    for (int i=0; i < 10; i++)
    {
      String msg = generateMessage(_zkUriString);
      expectedResponses = getExpectedResponses(0, msg);

      assertMatch(_cli.sendRequest(_client, "cluster-1","service-1_1", msg), expectedResponses);
      assertMatch(_cli.sendRequest(_client, "cluster-1","service-1_2", msg), expectedResponses);
      assertEquals(_cli.sendRequest(_client, "cluster-2","service-2_1", msg), getExpectedResponse(0, msg,_echoServers.get(2).getResponsePostfixStringWithPort()));

      // Shutdown zookeeper
      _zkServer.shutdown(false);

      msg = generateMessage(_zkUriString);
      expectedResponses = getExpectedResponses(0, msg);
      // Verify echo servers are still processing responses when zookeeper is down
      assertMatch(_cli.sendRequest(_client, "cluster-1","service-1_1", msg), expectedResponses);
      assertMatch(_cli.sendRequest(_client, "cluster-1","service-1_2", msg), expectedResponses);
      assertEquals(_cli.sendRequest(_client, "cluster-2","service-2_1", msg), getExpectedResponse(0, msg,_echoServers.get(2).getResponsePostfixStringWithPort()));

      // Restart zookeeper
      _zkServer.startup();
    }

    _zkServer.shutdown(false);
  }

   @Test
   public void testEchoServerMarkDownUp() throws IOException, InterruptedException, URISyntaxException, Exception
   {
     String [] expectedResponses;

     String msg = generateMessage(_zkUriString);
     expectedResponses = getExpectedResponses(0, msg);

     assertMatch(_cli.sendRequest(_client, "cluster-1","service-1_1", msg), expectedResponses);
     assertMatch(_cli.sendRequest(_client, "cluster-1","service-1_2", msg), expectedResponses);
     assertEquals(_cli.sendRequest(_client, "cluster-2","service-2_1", msg), getExpectedResponse(0, msg,_echoServers.get(2).getResponsePostfixStringWithPort()));

     // Echo Server mark down
     _echoServers.get(1).markDown();  // mark down echo server with ECHO_SERVER_PORT2
     _echoServers.get(2).markDown();  // mark down echo server with ECHO_SERVER_PORT3

     String store = LoadBalancerClientCli.printStore(_cli.getZKClient(), _zkUriString, "/d2", "cluster-1", "service-1_1");
     assertTrue(! store.contains(ECHO_SERVER_HOST+":"+ECHO_SERVER_PORT2),"Echo server with port "+ECHO_SERVER_PORT2+" was marked down but is still registered with ZK.");

     msg = generateMessage(_zkUriString);
     // Verify requests are not routed to echo servers with ECHO_SERVER_PORT2 and ECHO_SERVER_PORT3
     assertEquals(_cli.sendRequest(_client, "cluster-1","service-1_1", msg), getExpectedResponse(0, msg,_echoServers.get(0).getResponsePostfixStringWithPort()));
     assertEquals(_cli.sendRequest(_client, "cluster-1","service-1_2", msg), getExpectedResponse(0, msg,_echoServers.get(0).getResponsePostfixStringWithPort()));
     try
     {
       String response = _cli.sendRequest(_client, "cluster-2","service-2_1", msg);
       fail("Received response from marked down echo server with port=" + ECHO_SERVER_PORT3 + " "+response);
     }
     catch (Exception e)
     {
       // expected
     }

     // Echo Server mark up
     _echoServers.get(1).markUp();
     _echoServers.get(2).markUp();
     
     msg = generateMessage(_zkUriString);
     expectedResponses =  getExpectedResponses(0, msg);

     assertMatch(_cli.sendRequest(_client, "cluster-1","service-1_1", msg), expectedResponses);
     assertMatch(_cli.sendRequest(_client, "cluster-1","service-1_2", msg), expectedResponses);
     assertEquals(_cli.sendRequest(_client, "cluster-2","service-2_1", msg), getExpectedResponse(0, msg,_echoServers.get(2).getResponsePostfixStringWithPort()));

   }

   @Test
   public void testEchoServerRestart() throws IOException, InterruptedException, URISyntaxException, Exception
   {
     String [] expectedResponses;

     String msg = generateMessage(_zkUriString);
     expectedResponses  = getExpectedResponses(0, msg);

     assertMatch(_cli.sendRequest(_client, "cluster-1","service-1_1", msg), expectedResponses);
     assertMatch(_cli.sendRequest(_client, "cluster-1","service-1_2", msg), expectedResponses);
     assertEquals(_cli.sendRequest(_client, "cluster-2","service-2_2", msg), getExpectedResponse(0, msg,_echoServers.get(2).getResponsePostfixStringWithPort()));

     // Stop Echo Server
     _echoServers.get(1).markDown();
     _echoServers.get(1).stopServer(); // since we are running echo server in the same VM, this is not a real shutdown

     msg = generateMessage(_zkUriString);

     assertEquals(_cli.sendRequest(_client, "cluster-1","service-1_1", msg), getExpectedResponse(0, msg,_echoServers.get(0).getResponsePostfixStringWithPort()));
     assertEquals(_cli.sendRequest(_client, "cluster-1","service-1_2", msg), getExpectedResponse(0, msg,_echoServers.get(0).getResponsePostfixStringWithPort()));
     assertEquals(_cli.sendRequest(_client, "cluster-2","service-2_2", msg), getExpectedResponse(0, msg,_echoServers.get(2).getResponsePostfixStringWithPort()));

     // Start Echo Server
     try
     {
       _echoServers.get(1).startServer();
       _echoServers.get(1).markUp();
     }
     catch (Exception e)
     {
     }

     msg = generateMessage(_zkUriString);
     expectedResponses = getExpectedResponses(0, msg);
     assertMatch(_cli.sendRequest(_client, "cluster-1","service-1_2", msg), expectedResponses);
     assertEquals(_cli.sendRequest(_client, "cluster-2","service-2_1", msg), getExpectedResponse(0, msg,_echoServers.get(2).getResponsePostfixStringWithPort()));
   }

   private String[] getExpectedResponses(int partitionId, String msg)
   {
     String resp = msg + ";WEIGHT="+partitionId+"/1.0";
     // Two servers ECHO_SERVER_PORT1 and ECHO_SERVER_PORT2 are registered to the same "clister-1" and either one can return response
     return new String[] {resp + _echoServers.get(0).getResponsePostfixStringWithPort(), resp + _echoServers.get(1).getResponsePostfixStringWithPort()};
   }

   private String getExpectedResponse(int partitionId, String msg, String postfix)
   {
     return msg + ";WEIGHT="+partitionId+"/1.0" + postfix;
   }

   private void startAllEchoServers() throws Exception
   {
     _echoServers = new ArrayList<LoadBalancerEchoServer>();
     _echoServers.add(startEchoServer(ZK_HOST, _zkPort, ECHO_SERVER_HOST, ECHO_SERVER_PORT1, "cluster-1", "service-1_1", "service-1_2", "service-1_3" ));
     _echoServers.add(startEchoServer(ZK_HOST, _zkPort, ECHO_SERVER_HOST, ECHO_SERVER_PORT2, "cluster-1", "service-1_1", "service-1_2", "service-1_3" ));
     _echoServers.add(startEchoServer(ZK_HOST, _zkPort, ECHO_SERVER_HOST, ECHO_SERVER_PORT3, "cluster-2", "service-2_1", "service-2_2", "service-2_3" ));
   }
}
