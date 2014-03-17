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

package com.linkedin.restli.examples;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.r2.transport.http.server.HttpServer;
import com.linkedin.restli.server.filter.RequestFilter;
import com.linkedin.restli.server.filter.ResponseFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerRepository;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class RestLiIntegrationTest
{
  public static final String NETTY_CLIENT_LOGGER = "com.linkedin.r2.transport.http.client.HttpNettyClient";
  public static final String ASYNC_POOL_LOGGER = "com.linkedin.r2.transport.http.client.AsyncPoolImpl";
  private final int numCores = Runtime.getRuntime().availableProcessors();

  private ScheduledExecutorService _scheduler;
  private Engine                   _engine;
  private HttpServer               _server;
  private HttpServer               _serverWithoutCompression;
  private HttpServer               _serverWithFilters;
  private final Map<String, Level> _originalLevels     = new HashMap<String, Level>();

  // By default start a single synchronous server with compression.
  public void init() throws Exception
  {
    init(false, false);
  }

  // If not requested, don't start no-compression server
  public void init(boolean async) throws IOException
  {
    init(async, false);
  }

  public void init(boolean async, boolean includeNoCompression) throws IOException
  {
    int asyncTimeout = async ? 5000 : -1;

    _scheduler = Executors.newScheduledThreadPool(numCores + 1);
    _engine =
        new EngineBuilder().setTaskExecutor(_scheduler)
                           .setTimerScheduler(_scheduler)
                           .build();

    reduceLogging(NETTY_CLIENT_LOGGER, Level.ERROR);
    reduceLogging(ASYNC_POOL_LOGGER, Level.FATAL);

    // Always start one server, whether sync or async
    _server =
        RestLiIntTestServer.createServer(_engine,
                                         RestLiIntTestServer.DEFAULT_PORT,
                                         RestLiIntTestServer.supportedCompression,
                                         async,
                                         asyncTimeout);
    _server.start();

    // If requested, also start no compression server
    if (includeNoCompression)
    {
      _serverWithoutCompression =
          RestLiIntTestServer.createServer(_engine,
                                           RestLiIntTestServer.NO_COMPRESSION_PORT,
                                           "",
                                           async,
                                           asyncTimeout);
      _serverWithoutCompression.start();
    }
  }


  public void init(List<? extends RequestFilter> requestFilters, List<? extends ResponseFilter> responseFilters) throws IOException
  {
    int asyncTimeout = 5000;
    _scheduler = Executors.newScheduledThreadPool(numCores + 1);
    _engine = new EngineBuilder().setTaskExecutor(_scheduler).setTimerScheduler(_scheduler).build();
    reduceLogging(NETTY_CLIENT_LOGGER, Level.ERROR);
    reduceLogging(ASYNC_POOL_LOGGER, Level.FATAL);
    _serverWithFilters =
        RestLiIntTestServer.createServer(_engine,
                                         RestLiIntTestServer.DEFAULT_PORT,
                                         RestLiIntTestServer.supportedCompression,
                                         false,
                                         asyncTimeout,
                                         requestFilters,
                                         responseFilters);
    _serverWithFilters.start();
  }

  private void reduceLogging(String logName, Level newLevel)
  {
    LoggerRepository repo = Logger.getLogger(logName).getLoggerRepository();
    _originalLevels.put(logName, repo.getThreshold());
    repo.setThreshold(newLevel);
  }

  private void restoreLogging(String logName)
  {
    LoggerRepository repo = Logger.getLogger(logName).getLoggerRepository();
    repo.setThreshold(_originalLevels.get(logName));

  }

  public void shutdown() throws Exception
  {
    if (_server != null)
    {
      _server.stop();
    }
    if (_serverWithoutCompression != null)
    {
      _serverWithoutCompression.stop();
    }
    if (_serverWithFilters != null)
    {
      _serverWithFilters.stop();
    }
    if (_engine != null)
    {
      _engine.shutdown();
    }
    if (_scheduler != null)
    {
      _scheduler.shutdownNow();
    }

    restoreLogging(NETTY_CLIENT_LOGGER);
    restoreLogging(ASYNC_POOL_LOGGER);
  }
}
