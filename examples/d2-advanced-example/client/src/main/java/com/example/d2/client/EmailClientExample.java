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
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * In this example we will demonstrate the effects of assigning weights in partition.
 * As explained in the README.me we have 2 partitions: partition 0 and partition 1.
 * We have 2 servers. Both servers belong to partition 0 and 1 but the difference is
 * we set weight for server 1 to be 10x more than server 2.
 *
 * We use HASH partitioning scheme for this example. So when we send a request, D2 will
 * parse the partition key using this regex "user=(.*)".
 *
 * This regex is defined in d2Config.json
 *
 * So we can extract the key "oby" from a URI like d2://inbox?user=oby. If we cannot get
 * the key from the URI, we'll throw PartitionAccessException.
 *
 * Because we specified MD5 algorithm in d2Config.json, after we get the key we'll md5
 * hash the key and determine the partition from the hash mod number of partitions.
 *
 * So in this example you will see server1 will receive 10x more requests
 * than server2.
 *
 * @author Oby Sumampouw (osumampo@linkedin.com)
 */
public class EmailClientExample
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
    final Map<String, Long> trafficProportion =
        (Map<String, Long>) json.get("trafficProportion");
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
    System.out.println("D2 client is sending traffic to both " +
                           "partition 0 and partition1.");
    System.out.println("Note that traffic for server1 will be 22x more than server2");

    ScheduledFuture task = executorService.scheduleAtFixedRate(new Runnable()
    {
      @Override
      public void run ()
      {
        try
        {
          sendTraffic(trafficProportion, d2Client);
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    }, 0, 1000, TimeUnit.MILLISECONDS);

    System.out.println("Press enter to shutdown");
    System.out.println("===========================================================\n\n");
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
        "/src/main/config/emailExample.json";
    JSONParser parser = new JSONParser();
    Object object = parser.parse(new FileReader(path));
    return (JSONObject) object;
  }

  private static void sendTraffic (Map<String, Long> trafficProportion, D2Client d2Client)
      throws Exception
  {
    for (Map.Entry<String, Long> proportion : trafficProportion.entrySet())
    {
      long queryPerSecond = proportion.getValue();
      String serviceName = proportion.getKey();
      Random random = new Random();
      for (int i = 0; i < queryPerSecond; i++)
      {
        final URI uri = new URI("d2://" + serviceName + "?user=" + random.nextInt());
        RestRequestBuilder requestBuilder = new RestRequestBuilder(uri).setMethod("get");
        RestRequest request = requestBuilder.build();
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
