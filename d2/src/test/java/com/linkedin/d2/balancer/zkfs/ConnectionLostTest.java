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

package com.linkedin.d2.balancer.zkfs;


import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategyFactory;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.common.util.None;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import org.testng.annotations.Test;


/**
 * Test case for connection lost on startup, see SI-317
 *
 * @author <a href="mailto:ksobolev@linkedin.com" title="">Konstantin Sobolev</a>
 */
public class ConnectionLostTest
{
  private static final String BASE_PATH = "/d2";
  private static final int PORT = 5678;

  @Test
  public void testConnectionLost() throws Exception
  {
//    org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.INFO);

    final ZKFSLoadBalancer balancer = getBalancer();
    final FutureCallback<None> cb = new FutureCallback<>();
    new Thread(new FakeZKServer()).start();
    balancer.start(cb);
    cb.get(5, TimeUnit.SECONDS);
  }

  private ZKFSLoadBalancer getBalancer()
  {

    ZKFSTogglingLoadBalancerFactoryImpl f2 = new ZKFSTogglingLoadBalancerFactoryImpl(new ZKFSComponentFactory(),
                                                                                     5, TimeUnit.SECONDS,
                                                                                     BASE_PATH,
                                                                                     System.getProperty("java.io.tmpdir"),
                                                                                     new HashMap<>(),
                                                                                     new HashMap<>());
    return new ZKFSLoadBalancer("localhost:" + PORT, 60000, 5000, f2, null, BASE_PATH);
  }

  private static final class FakeZKServer implements Runnable
  {
    @Override
    public void run()
    {
      try
      {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.socket().bind(new InetSocketAddress(PORT));

        SocketChannel sc = ssc.accept();
        sc.read(ByteBuffer.allocate(128));

        //sniffed ZK 'connection accepted' packet
        byte[] resp = new byte[]{0, 0, 0, 36, 0, 0, 0, 0, 0, 0, -22, 96, 1, 55, 82, -39, 0, -80, 0, 0, 0, 0, 0, 16, -94, -115, 22, 85, -121, 11, -57, 87, 80, -2, -113, -114, -88, 112, 45, 121};
        sc.write(ByteBuffer.wrap(resp));

        sc.close();
        ssc.close();
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
    }
  }
}
