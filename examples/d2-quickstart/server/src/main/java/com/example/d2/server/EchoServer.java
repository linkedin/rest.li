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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.List;

/**
 * EchoServer is a simple http server that deploys into localhost,
 * prints out requests to stdout and always return success
 *
 * @author Oby Sumampouw (osumampo@linkedin.com)
 */
public class EchoServer
{
  private final int        _port;
  private final HttpServer _server;

  public EchoServer (int port, final String name, List<String> contextPaths)
      throws IOException
  {
    _port = port;
    _server = HttpServer.create(new InetSocketAddress(_port), 0);
    for (String contextPath : contextPaths)
    {
      _server.createContext(contextPath, new MyHandler(contextPath, name));
    }
    _server.setExecutor(null);
  }

  static class MyHandler implements HttpHandler
  {
    private final String _name;
    private final String _serverName;

    private MyHandler(String name, String serverName)
    {
      _name = name;
      _serverName = serverName;
    }

    public void handle(HttpExchange t) throws IOException
    {
      System.out.println(new Date().toString() + ": " + _serverName
                             + " received a request for the context handler = " + _name );
      String response = "Successfully contacted server " + _serverName;
      t.sendResponseHeaders(200, response.length());
      OutputStream os = t.getResponseBody();
      os.write(response.getBytes());
      os.close();
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
    _server.stop(0);
  }

}
