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

package test.r2.perf;

import java.net.InetAddress;
import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import java.lang.InterruptedException;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.test.util.GaussianRandom;

public class R2PerfDriver
{
  private R2PerfDriver()
  {
  }

  private static final Logger _log = LoggerFactory.getLogger(R2PerfDriver.class);

  public static void sendRequest(Client client, URI uri, RestRequest request,final int numThreads, final int runs, final double delay,final double range, final String remote_host, final String relative_uri, final double rampup)
  throws Exception
  {
    final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
    final ExecutorService service =
    new ThreadPoolExecutor(numThreads, numThreads, 500, TimeUnit.MILLISECONDS, queue);
    final String _current_host = InetAddress.getLocalHost().getHostName();

    try
    {
      for (int i = 0; i < numThreads; i++)
      {
        service.submit(runnable(client, uri, request, runs/numThreads, delay, range, relative_uri, remote_host, _current_host, rampup));
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      service.shutdown();

      if (!service.awaitTermination(600000000, TimeUnit.SECONDS))
      {
        throw new Exception("Threadpool shutdown: Timed out waiting!");
      }
    }
  }

  private static Runnable runnable(final Client client,
                                 final URI uri,
                                 final RestRequest request,
                                 final int runs,
                                 final double delay,
                                 final double range,
                                 final String relative_uri,
                                 final String remote_host,
                                 final String current_host,
                                 final double rampup)
  throws InterruptedException
  {
    return new Runnable()
    {
      public void run()
      {
        double resptime = -1.0;
        for (int i = 0; i < runs; i++)
        {
          if (i == 0 )
          {
            // Rampup threads
            try
            {
              long sleeptime = GaussianRandom.delay(rampup,rampup);
              Thread.sleep(sleeptime);
            }
            catch (InterruptedException e)
            {
            }
            System.out.println("Started "+" threadid:"+Thread.currentThread().getId()+",threadName:"+Thread.currentThread().getName());
          }
          try
          {
            final FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();

            try
            {
              if (delay > 0)
              {
                Thread.sleep(GaussianRandom.delay(delay,range));
              }
            } catch (Exception e)
            {
            }
            double start = System.nanoTime();
            sendMessage(client, request, callback);
            try
            {
              RestResponse response = callback.get();
              resptime = (System.nanoTime() - start)/1000000;
              int resplength = response.getEntity().length();
              if ( resplength > 0)
              {
                NumberFormat formatter = new DecimalFormat("####.##");
                _log.info(message("OK",String.valueOf(resplength)," in "+formatter.format(resptime)+"ms", relative_uri,remote_host, current_host));
              }
              else
              {
                _log.info(message("FAILED",String.valueOf(resplength)," ,Failed to receive response."+errmsg(i), relative_uri,remote_host, current_host));
              }
            }
            catch (Exception e)
            {
              _log.info(message("FAILED","NA"," ,Failed to get RestResponse."+errmsg(i)+" Exception:"+e.getMessage(), relative_uri,remote_host, current_host));
            }
          }
          catch (Exception e)
          {
            _log.info(message("FAILED","NA"," ,Failed to get RestResponse."+errmsg(i)+" Exception:"+e.getMessage(), relative_uri,remote_host, current_host));
          }
        }
      }
    };
  }

  private static void sendMessage(Client client, RestRequest request, FutureCallback<RestResponse> callback)
  {
    client.restRequest(request, callback);
  }

  private static String errmsg(int run)
  {
    return " run:"+String.valueOf(run)+" threadid:"+Thread.currentThread().getId()+",threadName:"+Thread.currentThread().getName();
  }

  private static String message(String status, String responselength, String resptimemsg, String relative_uri, String remote_host, String current_host)
  {
    try
    {
      return "[("+current_host+",SimpleRestClient,"+relative_uri+")["+remote_host+",r2d2,sendRequest]] [] [R2D2] "+status+"(threadid:"+Thread.currentThread().getId()+",threadName:"+Thread.currentThread().getName()+",responseLength:"+responselength+")"+resptimemsg;
    }
    catch (Exception e)
    {
      return null;
    }
  }
}

