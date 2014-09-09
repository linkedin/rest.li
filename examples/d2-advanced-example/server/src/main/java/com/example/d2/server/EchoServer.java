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

package com.example.d2.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * EchoServer is a simple http server that deploys into localhost,
 * prints out requests to stdout and always return success after preconfigured delay.
 *
 * @author Oby Sumampouw (osumampo@linkedin.com)
 */
public class EchoServer
{
  private final int        _port;
  private final HttpServer _server;
  private final ScheduledExecutorService _executorService;
  private final long _delay;
  private final ConcurrentHashMap<String, AtomicInteger> _sensors;
  private final String _name;

  public EchoServer (int port, final String name, List<String> contextPaths,
                     int numThreads, long delay)
      throws IOException
  {
    _port = port;
    _server = HttpServer.create(new InetSocketAddress(_port), 0);
    _sensors = new ConcurrentHashMap<String, AtomicInteger>();
    _name = name;
    for (String contextPath : contextPaths)
    {
      _sensors.put(contextPath, new AtomicInteger());
      _server.createContext(contextPath, new MyHandler(contextPath, name));
    }
    _server.setExecutor(null);
    _executorService = Executors.newScheduledThreadPool(numThreads);
    _delay = delay;
  }

  class MyHandler implements HttpHandler
  {
    private final String _name;
    private final String _serverName;

    private MyHandler(String name, String serverName)
    {
      _name = name;
      _serverName = serverName;
    }

    public void handle(final HttpExchange t) throws IOException
    {
      final String date = new Date().toString();
      _sensors.get(_name).incrementAndGet();
      Headers headers = t.getRequestHeaders();
      Long delay = _delay;
      if (headers.containsKey("delay"))
      {
        List<String> headerValues = headers.get("delay");
        delay = Long.parseLong(headerValues.get(0));
      }
      System.out.println( date + ": " + _serverName
                             + " received a request for the context handler = " + _name);

      _executorService.schedule(new Runnable()
      {
        @Override
        public void run ()
        {
          String response = "server " + _serverName;
          try
          {
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
          }
          catch (IOException e)
          {
            e.printStackTrace();
          }

        }
      }, delay, TimeUnit.MILLISECONDS);
    }
  }

  public void start()
      throws IOException
  {
    _server.start();
  }

  public void stop()
      throws IOException
  {
    _executorService.shutdownNow();
    _server.stop(0);
    for (Map.Entry<String, AtomicInteger> entry : _sensors.entrySet())
    {
      System.out.println("Name : " + _name + " path " + entry.getKey() +
                             " received " + entry.getValue().get() + " hits.");
    }
  }

}
