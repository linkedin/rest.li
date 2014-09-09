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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In this example we want to mimic a situation where a server normally responds quickly
 * to a request. But when it serves certain types of requests the latency
 * jumps really high. For example maybe we have 2 clients that send request to a
 * compute cluster in AWS. The first client always send small batches but the second
 * client always send big batches. So the response latency for the first client is fine
 * but the latency of response for the second client almost always times out. This is
 * despite the compute cluster owner has defined that the client should time out after
 * 5000 ms.
 *
 * In D2 we provide a way for client to "override" the rules that the service owner
 * have explicitly state in d2Config.json. To override certain configuration parameter,
 * we just need to provide the key and value in clientOverrideConfig when we build
 * the d2 client. However the service owner must specify what values the client are
 * allowed to override.
 *
 * So to demonstrate how this works, we'll create 2 different clients. We'll set a
 * dummy echo server that respond to request in 10000ms. But the timeout is set to 5000ms.
 * The first client doesn't override the timeout param so all the requests will time out.
 * The second client overrides the timeout param so all the request still goes through.
 *
 * @author Oby Sumampouw (osumampo@linkedin.com)
 */
public class BigDataClientExample
{

  private static ConcurrentHashMap<String, AtomicInteger> stats =
      new ConcurrentHashMap<String, AtomicInteger>();

  public static void main (String[] args)
      throws Exception
  {
    stats.put("normalClient", new AtomicInteger());
    stats.put("overrideClient", new AtomicInteger());
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
    Map<String, Map<String, Object>> clientServicesConfig =
        (Map<String, Map<String, Object>>)
            json.get("clientServicesConfig");
    final Long clientShutdownTimeout = (Long) json.get("clientShutdownTimeout");
    final Long clientStartTimeout = (Long) json.get("clientStartTimeout");

    System.out.println("Finished parsing client config");

    D2ClientBuilder builder = new D2ClientBuilder().setZkHosts(zkConnectString)
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
                                                   .setFsBasePath(fsBasePath);

    final D2Client normalD2Client = builder.build();
    final D2Client overrideD2Client =
        builder.setClientServicesConfig(clientServicesConfig)
               .build();

    System.out.println("Finished creating d2 clients, starting d2 clients...");

    ScheduledExecutorService
        executorService = Executors.newSingleThreadScheduledExecutor();
    final CountDownLatch latch = new CountDownLatch(2);

    //start both d2 clients
    startClients(normalD2Client, overrideD2Client, executorService, clientStartTimeout,
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
                 }
    );
    latch.await();
    System.out.println("D2 clients are sending traffic to 'compute' service.");
    System.out.println("NormalClient will fail because the timeout is 5 seconds.");
    System.out.println("The server responds to our request in 10 seconds.");
    System.out.println("OverrideClient will succeed because we override the timeout to 10 seconds");
    ScheduledFuture normalTask = executorService.scheduleAtFixedRate(new Runnable()
    {
      @Override
      public void run ()
      {
        try
        {
          sendTraffic(trafficProportion, normalD2Client, "Normal d2 client");
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    }, 0, 1000, TimeUnit.MILLISECONDS);

    ScheduledFuture overrideTask = executorService.scheduleAtFixedRate(new Runnable()
    {
      @Override
      public void run ()
      {
        try
        {
          sendTraffic(trafficProportion, overrideD2Client, "Override d2 client");
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
    normalTask.cancel(false);
    overrideTask.cancel(false);

    System.out.println("Shutting down... please wait 15 seconds.");
    shutdown(normalD2Client, overrideD2Client, executorService, clientShutdownTimeout);
  }

  private static void startClients(final D2Client d2Client1, final D2Client d2Client2,
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
          d2Client1.start(new Callback<None>()
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
          d2Client2.start(new Callback<None>()
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

  private static void shutdown(final D2Client d2Client1, final D2Client d2Client2,
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
          d2Client1.shutdown(new Callback<None>()
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
          d2Client2.shutdown(new Callback<None>()
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
        "/src/main/config/bigDataExample.json";
    JSONParser parser = new JSONParser();
    Object object = parser.parse(new FileReader(path));
    return (JSONObject) object;
  }

  private static void sendTraffic (Map<String, Long> trafficProportion,
                                   D2Client d2Client, final String name)
      throws Exception
  {
    for (Map.Entry<String, Long> proportion : trafficProportion.entrySet())
    {
      long queryPerSecond = proportion.getValue();
      String serviceName = proportion.getKey();
      for (int i = 0; i < queryPerSecond; i++)
      {
        final URI uri = new URI("d2://" + serviceName);
        final long sent = System.currentTimeMillis();
        RestRequestBuilder requestBuilder = new RestRequestBuilder(uri).setMethod("get");
        RestRequest request = requestBuilder.build();
        //we don't care about the result from the server after all,
        //you can see the traffic hits the echo server from stdout
        d2Client.restRequest(request, new Callback<RestResponse>()
        {
          @Override
          public void onError (Throwable e)
          {
            System.err.println(name + " sending URI = " + uri.toString() +
                                   " didn't get any response after " +
                                   (System.currentTimeMillis() - sent) + " ms");
          }

          @Override
          public void onSuccess (RestResponse result)
          {
            System.out.println(name + " sending URI = " + uri.toString() +
                                   " was served by " +
                                   result.getEntity().asString("UTF-8") +
                                   " after " + (System.currentTimeMillis() - sent) +
                                   " ms");
          }
        });
      }
    }
  }
}
