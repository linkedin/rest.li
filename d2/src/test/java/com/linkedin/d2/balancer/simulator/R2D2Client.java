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

package com.linkedin.d2.balancer.simulator;

import com.linkedin.d2.balancer.clients.DynamicClient;
import com.linkedin.d2.balancer.util.LoadBalancerEchoClient;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class R2D2Client
{
  private final DynamicClient _client;
  private final Random _random;

  private final AtomicLong _total = new AtomicLong();
  private final AtomicLong _success = new AtomicLong();
  private final AtomicLong _error = new AtomicLong();

  public static void main(String[] args) throws Exception
  {
    new R2D2Client(args[0]).run();
  }

  public R2D2Client(String hostPort) throws Exception
  {
    _client = new DynamicClient(LoadBalancerEchoClient.getLoadBalancer(hostPort), null, true);
    _random = new Random();

  }

  public void run() throws Exception
  {

    // now call some stuff
    int size = 24;
    int calls = 10000;

    for (;;)
    {
      System.err.println("running " + size * calls + " calls");

      ExecutorService pool = Executors.newFixedThreadPool(size);

      long start = System.currentTimeMillis();

      for (int i = 0; i < size; ++i)
      {
        pool.execute(new Caller(calls));
      }

      pool.shutdown();
      if (!pool.awaitTermination(600, TimeUnit.SECONDS))
      {
        throw new Exception("Timed out waiting!");
      }

      System.err.println((size * calls) + " calls in "
                                 + (System.currentTimeMillis() - start) + "ms");
      System.err.println("Total calls: " + _total.get() + "; success: " + _success.get() + "; error: " + _error.get());
    }

    // System.err.println("shutting down");
    //
    // // stop everything
    // for (Entry<String, List<LoadBalancerEchoServer>> servers : _clusters.entrySet())
    // {
    // for (final LoadBalancerEchoServer server : servers.getValue())
    // {
    // server.markDown();
    // server.stopServer();
    // }
    // }
    //
    // _client.shutdown(Callbacks.<None> empty());
  }

  private static final byte[] BYTES = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa".getBytes();

  public class Caller implements Runnable
  {
    private int _count;

    public Caller(int count)
    {
      _count = count;
    }

    public void run()
    {
      for (int i = 0; i < _count; ++i)
      {
        //rpcFuture();
        restFuture();
      }
    }

    private void restFuture()
    {
      try
      {
        _total.incrementAndGet();

        URI uri = getRandomServiceUrn();

        RestRequest req =
            new RestRequestBuilder(uri).setEntity(BYTES).build();

        _client.restRequest(req).get();
        _success.incrementAndGet();
      }
      catch (Exception e)
      {
        _error.incrementAndGet();
        e.printStackTrace();
      }
    }

    private URI getRandomServiceUrn() throws URISyntaxException
    {
      return URI.create("d2://service-" + (_random.nextInt(3) + 1) + "-cluster-"
          + (_random.nextInt(2) + 1));
    }
  }

}
