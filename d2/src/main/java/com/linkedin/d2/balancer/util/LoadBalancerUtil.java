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

package com.linkedin.d2.balancer.util;


import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.clients.DynamicClient;
import com.linkedin.r2.message.RequestContext;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.slf4j.Logger;

import static com.linkedin.d2.discovery.util.LogUtil.info;
import static com.linkedin.d2.discovery.util.LogUtil.warn;


public class LoadBalancerUtil
{

  public static Throwable findOriginalThrowable(Throwable throwable)
  {
    //just to make sure we don't go to infinite loop. Most exception is less than 100 level deep.
    int depth = 0;
    Throwable original = throwable;
    while (original.getCause() != null && depth < 100)
    {
      original = original.getCause();
      depth++;
    }
    return original;
  }

  public static String join(Collection<?> toJoin, String joinBy)
  {
    if (toJoin == null)
    {
      return null;
    }

    String joined = "";

    for (Object o : toJoin)
    {
      joined += o.toString() + joinBy;
    }

    if (joined.length() > 0)
    {
      joined = joined.substring(0, joined.length() - joinBy.length());
    }

    return joined;
  }

  public static String getServiceNameFromUri(URI uri)
  {
    // Should this be host name or authority?
    return uri.getAuthority();
  }

  public static String getPathFromUri(URI uri)
  {
    return uri.getPath();
  }

  public static String getRawPathFromUri(URI uri)
  {
    return uri.getRawPath();
  }

  private static final Pattern DOT_PATTERN = Pattern.compile(Pattern.quote("."));

  public static Map<String, Map<String, String>> getSubProperties(String prefix,
                                                                  String propertiesString) throws IOException
  {
    Properties fileProperties = new Properties();
    Map<String, Map<String, String>> utilServicePropertyMap =
        new HashMap<String, Map<String, String>>();

    fileProperties.load(new StringReader(propertiesString));

    for (Entry<Object, Object> row : fileProperties.entrySet())
    {
      String key = row.getKey().toString();
      String property = row.getValue().toString();

      if (key.startsWith(prefix))
      {
        String[] keySplit = DOT_PATTERN.split(key, 3);
        String serviceName = keySplit[1];
        String subKey = keySplit[2];

        Map<String, String> serviceProperties = utilServicePropertyMap.get(serviceName);

        if (serviceProperties == null)
        {
          serviceProperties = new HashMap<String, String>();
          utilServicePropertyMap.put(serviceName, serviceProperties);
        }

        serviceProperties.put(subKey, property);
      }
    }

    return utilServicePropertyMap;
  }

  public static <C extends Object> C getOrElse(Map<String, C> map,
                                               String key,
                                               C elseReturn)
  {
    C obj = map.get(key);

    if (obj == null)
    {
      return elseReturn;
    }

    return obj;
  }

  public static <C> List<C> getOrElse(List<C> list)
  {
    if (list == null)
    {
      return new ArrayList<C>();
    }

    return list;
  }

  public static File createTempDirectory(String name) throws IOException
  {
    final File temp;

    temp = File.createTempFile("temp-" + name, Long.toString(System.nanoTime()));

    if (!(temp.delete()))
    {
      throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
    }

    if (!(temp.mkdir()))
    {
      throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
    }

    return (temp);
  }

  public static void syncShutdownClient(DynamicClient client, Logger log)
  {
    info(log, "shutting down");

    final CountDownLatch latch = new CountDownLatch(1);

    client.shutdown(new Callback<None>()
    {
      public void onSuccess(None t)
      {
        latch.countDown();
      }

      public void onError(Throwable e)
      {
        latch.countDown();
      }
    });

    try
    {
      if (!latch.await(5, TimeUnit.SECONDS))
      {
        warn(log, "unable to shutdown properly after 5 seconds");
      }
      else
      {
        info(log, "shutdown complete");
      }
    }
    catch (InterruptedException e)
    {
      warn(log, "shutdown was interrupted");
    }
  }

  public static class TargetHints
  {
    public static final String TARGET_SERVICE_KEY_NAME = "D2-Hint-TargetService";


    /**
     * Inserts a hint in RequestContext instructing D2 to bypass normal hashing behavior
     * and instead route to the specified target service. This is different than
     * setRequestContextTargetHost because it sets the service context path as well as the host.
     * @param context RequestContext for the request which will be made
     * @param targetService target service's URI to be used as a hint in the RequestContext
     */
    public static void setRequestContextTargetService(RequestContext context, URI targetService)
    {
      context.putLocalAttr(TARGET_SERVICE_KEY_NAME, targetService);
    }

    /**
     * Looks for a target service hint in the RequestContext, returning it if found, or null if no
     * hint is present.
     * @param context RequestContext for the request
     * @return URI for target service hint, or null if no hint is present in the RequestContext
     */
    public static URI getRequestContextTargetService(RequestContext context)
    {
      return (URI)context.getLocalAttr(TARGET_SERVICE_KEY_NAME);
    }
  }

}
