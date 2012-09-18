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
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerRepository;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import com.linkedin.r2.transport.http.server.HttpServer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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
  private Engine _engine;
  private HttpServer _server;
  private Map<String, Level> _originalLevels = new HashMap<String, Level>();

  @BeforeSuite
  public void init() throws Exception
  {
    _scheduler = Executors.newScheduledThreadPool(numCores + 1);
    _engine = new EngineBuilder()
        .setTaskExecutor(_scheduler)
        .setTimerScheduler(_scheduler)
        .build();

    reduceLogging(NETTY_CLIENT_LOGGER, Level.ERROR);
    reduceLogging(ASYNC_POOL_LOGGER, Level.FATAL);

    _server = RestLiExamplesServer.createServer(_engine);
    _server.start();
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

  @AfterSuite
  public void shutdown() throws Exception
  {
    _server.stop();
    _engine.shutdown();
    _scheduler.shutdownNow();

    restoreLogging(NETTY_CLIENT_LOGGER);
    restoreLogging(ASYNC_POOL_LOGGER);
  }
}
