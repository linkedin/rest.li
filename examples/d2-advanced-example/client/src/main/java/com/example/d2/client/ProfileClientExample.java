package com.example.d2.client;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.D2Client;
import com.linkedin.d2.balancer.D2ClientBuilder;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * This example uses ProfileService cluster only. As explained in the README.md,
 * we have 3 services in ProfileService cluster. The configuration is in d2Config.json
 * file.
 *
 * In this example we will demonstrate partitioning and load balancer.
 *
 * 1.) For partitioning: we have 3 partitions. The servers definition is in server.json
 * ProfileService 1,2 belong to partition 0
 * ProfileService 3,4 belong to partition 1
 * ProfileService 5,6 belong to partition 2
 *
 * We'll show that the request is sent to the correct partition as long as the client's
 * request URI matches the regex parameter defined in the partitionProperties below:
 *
 * "partitionProperties" :
 * {
 *  "partitionType" : "RANGE", <-- type of partitioning, there's "HASH" and "RANGE"
 *  "partitionKeyRegex" : "partitionId=(\d+)", <-- this regex extracts partitionId
 *  "partitionCount" : "3",
 *  "keyRangeStart" : "0",
 *  "partitionSize" : "100"
 * }
 *
 * Notice we use RANGE partitioning, so that means id from 0-99 belongs to partition 0,
 * 100-199 go to partition 1, 200-299 go to partition 2. Any partitionId outside the
 * range 0-299 will not be routed anywhere because there's no partition.
 *
 * So a URI like d2://member/partitionId=2 will be directed to partition 0 and URI
 * like d2://contact/partitionId=159 will be directed to partition 1, etc.
 *
 * 2.) For demonstrating how load balancer works:
 * in d2ConfigJson we set the following
 *
 * "member": {
 *   "path" : "/member",
 *   "loadBalancerStrategyProperties" : {
 *     "http.loadBalancer.lowWaterMark" : "3000",
 *     "http.loadBalancer.highWaterMark" : "6000"
 *   },
 *   "degraderProperties" : {
 *     "degrader.lowLatency" : "1000",
 *     "degrader.highLatency" : "2000"
 *   }
 * },
 * "contact": {
 *   "path" : "/contact",
 *   "loadBalancerStrategyProperties" : {
 *     "http.loadBalancer.lowWaterMark" : "3000",
 *     "http.loadBalancer.highWaterMark" : "6000"
 *   },
 *   "degraderProperties" : {
 *     "degrader.lowLatency" : "2000",
 *     "degrader.highLatency" : "3000"
 *   }
 * },
 * "connection": {
 *   "path" : "/connection",
 *   "loadBalancerStrategyProperties" : {
 *     "http.loadBalancer.lowWaterMark" : "3000",
 *     "http.loadBalancer.highWaterMark" : "6000"
 *   },
 *   "degraderProperties" : {
 *     "degrader.lowLatency" : "3000",
 *     "degrader.highLatency" : "4000"
 *   }
 * }
 *
 * Note that D2 has 2 modes: load balancing and call dropping. Load balancing means
 * shifting traffic to healthier server. This is influenced by degrader.lowLatency
 * and degrader.highLatency. This means if a server's latency is higher than the value
 * for degrader.HighLatency it's considered healthy and if the latency is lower than
 * degrader.lowLatency it's considered healthy.
 *
 * This is different from call dropping. Call dropping measures the average cluster's
 * latency. If the cluster latency is higher than http.loadBalancer.highWaterMark than
 * D2 will start dropping request (not send the request), if the cluster latency is
 * smaller than http.loadBalancer.lowWaterMark then the cluster is considered healthy so
 * D2 won't drop request.
 *
 * So in the above example, we intentionally set the http.loadBalancer.lowWaterMark
 * and http.loadBalancer.highWaterMark to be really high so it won't make D2 goes to
 * call dropping mode.
 *
 * In order to simulate the load balancing, we will set the respond times for the server
 * to:
 * ProfileService1,3,5 has 500ms delay
 * ProfileService2,4,6 has 2500ms delay.
 *
 * We'll start sending traffic and you'll see that for "member", traffic will be
 * shifted to ProfileService1, 3 or 5 server depending on which partition we send to.
 *
 * For "contact" and "connection", traffic will stay the same because the 2500ms delay
 * is less than the highWaterMark for "contact" and "connection".
 *
 * We'll wait for the signal from the user to make all ProfileService servers' latency to
 * 0 ms, then you'll see the traffic for 'member' will be balanced again.
 *
 * At the end of the exercise you'll see the number of requests that hit servers
 * ProfileService1,3,5 exceed 2,4,6 a lot more.
 *
 * @author Oby Sumampouw (osumampo@linkedin.com)
 */
public class ProfileClientExample
{

  public static void main(String[] args) throws Exception
  {
    //get client configuration
    JSONObject json = parseConfig();
    String zkConnectString = (String) json.get("zkConnectString");
    Long zkSessionTimeout = (Long) json.get("zkSessionTimeout");
    String zkBasePath = (String) json.get("zkBasePath");
    Long zkStartupTimeout = (Long) json.get("zkStartupTimeout");
    Long zkLoadBalancerNotificationTimeout = (Long)
        json.get("zkLoadBalancerNotificationTimeout");
    String zkFlagFile = (String) json.get("zkFlagFile");
    String fsBasePath = (String) json.get("fsBasePath");
    final Map<String, Map<String, Long>> trafficProportion =
        (Map<String, Map<String, Long>>) json.get("trafficProportion");
    final Long clientShutdownTimeout = (Long) json.get("clientShutdownTimeout");
    final Long clientStartTimeout = (Long) json.get("clientStartTimeout");

    System.out.println("Finished parsing client config");

    //create d2 client
    final D2Client d2Client = new D2ClientBuilder().setZkHosts(zkConnectString)
                                                   .setZkSessionTimeout(
                                                       zkSessionTimeout,
                                                       TimeUnit.MILLISECONDS)
                                                   .setZkStartupTimeout(
                                                       zkStartupTimeout,
                                                       TimeUnit.MILLISECONDS)
                                                   .setLbWaitTimeout(
                                                       zkLoadBalancerNotificationTimeout,
                                                       TimeUnit.MILLISECONDS)
                                                   .setFlagFile(zkFlagFile)
                                                   .setBasePath(zkBasePath)
                                                   .setFsBasePath(fsBasePath)
                                                   .build();

    System.out.println("Finished creating d2 client, starting d2 client...");

    ScheduledExecutorService
        executorService = Executors.newSingleThreadScheduledExecutor();
    final CountDownLatch latch = new CountDownLatch(1);

    //start d2 client by connecting to zookeeper
    startClient(d2Client, executorService, clientStartTimeout,
                new Callback<None>()
                {
                  @Override
                  public void onError (Throwable e)
                  {
                    System.exit(1);
                  }

                  @Override
                  public void onSuccess (None result)
                  {
                    latch.countDown();
                  }
                });
    latch.await();
    System.out.println("D2 client is sending traffic.");
    System.out.println("Note that traffic for 'member' will go mostly to ProfileService 1,3,5 servers.");
    System.out.println("Because we make ProfileService 2,4,6 servers respond slowly \n");

    ScheduledFuture task = executorService.scheduleAtFixedRate(new Runnable()
    {
      @Override
      public void run ()
      {
        try
        {
          sendTraffic(trafficProportion, d2Client, null);
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    }, 0, 1000, TimeUnit.MILLISECONDS);

    System.out.println("Press enter to restore the health of all the servers\n\n\n");
    System.out.println("After this line you will see d2 client will start logging warning"
                           + " message because server 2,4,6's latencies are higher than" +
                           "the threshold (high water mark).");
    System.out.println("===========================================================");
    System.in.read();
    task.cancel(false);

    task = executorService.scheduleAtFixedRate(new Runnable()
    {
      @Override
      public void run ()
      {
        try
        {
          sendTraffic(trafficProportion, d2Client, 0l);
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    }, 0, 1000, TimeUnit.MILLISECONDS);
    System.out.println("=========================================================\n\n\n");
    System.out.println("Now all servers are healthy. Traffic for 'member' " +
                           "will be balanced between profile service 1,2,3,4,5,6.");
    System.out.println("Press enter to shut down\n\n");
    System.in.read();
    task.cancel(false);

    System.out.println("Shutting down...");
    shutdown(d2Client, executorService, clientShutdownTimeout);
  }

  private static void startClient(final D2Client d2Client,
                           ExecutorService executorService,
                           Long timeout,
                           final Callback<None> callback)
  {
    try
    {
      executorService.submit(new Runnable()
      {
        @Override
        public void run ()
        {
          d2Client.start(new Callback<None>()
          {
            @Override
            public void onError (Throwable e)
            {
              System.err.println("Error starting d2Client. Aborting... ");
              e.printStackTrace();
              System.exit(1);
            }

            @Override
            public void onSuccess (None result)
            {
              System.out.println("D2 client started");
              callback.onSuccess(None.none());
            }
          });
        }
      }).get(timeout, TimeUnit.MILLISECONDS);
    }
    catch (Exception e)
    {
      System.err.println("Cannot start d2 client. Timeout is set to " +
                             timeout + " ms");
      e.printStackTrace();
    }
  }

  private static void shutdown(final D2Client d2Client,
                        ExecutorService executorService,
                        Long timeout)
  {
    try
    {
      executorService.submit(new Runnable()
      {
        @Override
        public void run ()
        {
          d2Client.shutdown(new Callback<None>()
          {
            @Override
            public void onError (Throwable e)
            {
              System.err.println("Error shutting down d2Client.");
              e.printStackTrace();
            }

            @Override
            public void onSuccess (None result)
            {
              System.out.println("D2 client stopped");
            }
          });
        }
      }).get(timeout, TimeUnit.MILLISECONDS);
    }
    catch (Exception e)
    {
      System.err.println("Cannot stop d2 client. Timeout is set to " +
                             timeout + " ms");
      e.printStackTrace();
    }
    finally
    {
      executorService.shutdown();
    }
  }

  private static JSONObject parseConfig()
      throws IOException, ParseException
  {
    String path = new File(new File(".").getAbsolutePath()).getCanonicalPath() +
        "/src/main/config/profileLoadBalancerExample.json";
    JSONParser parser = new JSONParser();
    Object object = parser.parse(new FileReader(path));
    return (JSONObject) object;
  }

  private static void sendTraffic (Map<String, Map<String, Long>> trafficProportion, D2Client d2Client,
                                   Long delay)
      throws Exception
  {
    for (Map.Entry<String, Map<String,Long>> d2Service : trafficProportion.entrySet())
    {
      for (Map.Entry<String, Long> partition : d2Service.getValue().entrySet())
      {
        final URI uri = new URI("d2://" + d2Service.getKey() + "?partitionId=" + partition.getKey());
        RestRequestBuilder requestBuilder = new RestRequestBuilder(uri).setMethod("get");
        if (delay != null)
        {
          requestBuilder.setHeader("delay", delay.toString());
        }
        RestRequest request = requestBuilder.build();
        Long queryPerSecond = partition.getValue();
        for (int i = 0; i < queryPerSecond; i++)
        {
          //we don't care about the result from the server after all,
          //you can see the traffic hits the echo server from stdout
          d2Client.restRequest(request, new Callback<RestResponse>()
          {
            @Override
            public void onError (Throwable e)
            {
              System.err.println("URI = " + uri.toString() + " didn't get any response");
            }

            @Override
            public void onSuccess (RestResponse result)
            {
              System.out.println("URI = " + uri.toString() + " was served by " + result.getEntity().asString("UTF-8"));
            }
          });
        }
      }
    }
  }
}
