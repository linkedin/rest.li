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

/* $Id$ */
package test.r2.perf;

import java.lang.reflect.Field;
import java.net.URI;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class PerfConfig
{
  // Property keys
  private static final String PERF_HTTP_PORT = "perf.http.port";
  private static final String PERF_CLIENT_NUM_THREADS = "perf.client.num_threads";
  private static final String PERF_CLIENT_NUM_MSGS = "perf.client.num_msgs";
  private static final String PERF_CLIENT_MSG_SIZE = "perf.client.msg_size";
  private static final String PERF_RELATIVE_URI = "perf.relative_uri";
  private static final String PERF_HOST = "perf.host";
  private static final String PERF_SERVER_MSG_SIZE = "perf.server.msg_size";
  private static final String PERF_CLIENT_PURE_STREAMING = "perf.client.pure_streaming";
  private static final String PERF_SERVER_PURE_STREAMING = "perf.server.pure_streaming";
  private static final String PERF_CLIENT_REST_OVER_STREAM = "perf.client.restOverStream";
  private static final String PERF_SERVER_REST_OVER_STREAM = "perf.server.restOverStream";

  // Default property values
  private static final String DEFAULT_HOST = "localhost";

  private static final int DEFAULT_HTTP_PORT = 8082;

  private static final URI DEFAULT_RELATIVE_URI = URI.create("/echo");

  private static final int DEFAULT_CLIENT_NUM_THREADS = 100;
  private static final int DEFAULT_CLIENT_NUM_MSGS = 500 * 1000;
  private static final int DEFAULT_CLIENT_MSG_SIZE = 1000;
  private static final int DEFAULT_SERVER_MSG_SIZE = 1000;

  public static int getHttpPort()
  {
    return getInt(PERF_HTTP_PORT);
  }

  public static int getNumClientThreads()
  {
    return getInt(PERF_CLIENT_NUM_THREADS);
  }

  public static int getNumMessages()
  {
    return getInt(PERF_CLIENT_NUM_MSGS);
  }

  public static int getMessageSize()
  {
    return getInt(PERF_CLIENT_MSG_SIZE);
  }

  public static int getServerMessageSize()
  {
    return getInt(PERF_SERVER_MSG_SIZE);
  }

  public static URI getRelativeUri()
  {
    return getUri(PERF_RELATIVE_URI);
  }

  public static String getHost()
  {
    return getString(PERF_HOST);
  }

  public static URI getHttpUri()
  {
    return URI.create("http://" + getHost() + ":" + getHttpPort() + getRelativeUri());
  }

  public static boolean isClientPureStreaming()
  {
    return getBoolean(PERF_CLIENT_PURE_STREAMING);
  }

  public static boolean isServerPureStreaming()
  {
    return getBoolean(PERF_SERVER_PURE_STREAMING);
  }

  public static boolean clientRestOverStream()
  {
    return getBoolean(PERF_CLIENT_REST_OVER_STREAM);
  }

  public static boolean serverRestOverStream()
  {
    return getBoolean(PERF_SERVER_REST_OVER_STREAM);
  }

  private static URI getUri(String propName)
  {
    final String propVal = System.getProperty(propName);
    return propVal != null ? URI.create(propVal) : PerfConfig.<URI>getDefaultValue(propName);
  }

  private static String getString(String propName)
  {
    final String propVal = System.getProperty(propName);
    return propVal != null ? propVal : PerfConfig.<String>getDefaultValue(propName);
  }

  private static int getInt(String propName)
  {
    final String propVal = System.getProperty(propName);
    return propVal != null ? Integer.parseInt(propVal) : PerfConfig.<Integer>getDefaultValue(propName);
  }

  private static boolean getBoolean(String propName)
  {
    final String propVal = System.getProperty(propName);
    return propVal != null && Boolean.parseBoolean(propVal);
  }

  @SuppressWarnings("unchecked")
  private static <T> T getDefaultValue(String propName)
  {
    propName = propName.substring("perf.".length()).toUpperCase().replace('.', '_');
    try
    {
      final Field field = PerfConfig.class.getDeclaredField("DEFAULT_" + propName);
      return (T)field.get(null);
    }
    catch (Exception e)
    {
      throw new RuntimeException("Couldn't get default value for: " + propName, e);
    }
  }
}
