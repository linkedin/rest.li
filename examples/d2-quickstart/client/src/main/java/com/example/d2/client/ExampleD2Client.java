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


package com.example.d2.client;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.D2Client;
import com.linkedin.d2.balancer.D2ClientBuilder;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Creates a d2Client that tries to contact "articleRecommendation", "newsArticle",
 * "jobRecommendation" several times.
 * The D2Client configuration is located at
 * d2-quickstart/client/src/main/config/client.json
 *
 * @author Oby Sumampouw (osumampo@linkedin.com)
 */
public class ExampleD2Client
{
  public static void main(String[] args)
      throws IOException, ParseException, InterruptedException
  {
    //get client configuration
    JSONObject json = parseConfig();
    String zkConnectString = (String) json.get("zkConnectString");
    Long zkSessionTimeout = (Long) json.get("zkSessionTimeout");
    String zkBasePath = (String) json.get("zkBasePath");
    Long zkStartupTimeout = (Long) json.get("zkStartupTimeout");
    Long zkLoadBalancerNotificationTimeout = (Long) json.get("zkLoadBalancerNotificationTimeout");
    String zkFlagFile = (String) json.get("zkFlagFile");
    String fsBasePath = (String) json.get("fsBasePath");
    final Map<String, Long> trafficProportion = (Map<String, Long>) json.get("trafficProportion");
    final Long clientShutdownTimeout = (Long) json.get("clientShutdownTimeout");
    final Long clientStartTimeout = (Long) json.get("clientStartTimeout");
    Long rate = (Long) json.get("rateMillisecond");
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

    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
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
    System.out.println("D2 client is sending traffic");

    ScheduledFuture task = executorService.scheduleAtFixedRate(new Runnable()
    {
      @Override
      public void run ()
      {
        try
        {
          sendTraffic(trafficProportion, d2Client);
        }
        catch (URISyntaxException e)
        {
          e.printStackTrace();
        }
      }
    }, 0, rate, TimeUnit.MILLISECONDS);

    System.out.println("Press enter to stop D2 client...");
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
        "/src/main/config/client.json";
    JSONParser parser = new JSONParser();
    Object object = parser.parse(new FileReader(path));
    return (JSONObject) object;
  }

  private static void sendTraffic(Map<String, Long> trafficProportion, D2Client d2Client)
      throws URISyntaxException
  {
    for (Map.Entry<String, Long> entry : trafficProportion.entrySet())
    {
      if (entry.getKey().equals("comment")) {
        continue;
      }
      URI uri = new URI("d2://" + entry.getKey());
      RestRequest request = new RestRequestBuilder(uri).setMethod("get").build();
      for (long i = 0; i < entry.getValue(); i++)
      {
        //we don't care about the result from the server after all,
        //you can see the traffic hits the echo server from stdout
        d2Client.restRequest(request);
      }
    }
  }
}
