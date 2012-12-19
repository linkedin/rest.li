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

package com.example.fortune;


import com.linkedin.r2.transport.http.server.HttpServer;
import com.linkedin.r2.transport.http.server.HttpServerFactory;
import com.linkedin.restli.server.DelegatingTransportDispatcher;
import com.linkedin.restli.server.RestLiConfig;
import com.linkedin.restli.server.RestLiServer;

/**
 * Very simple HTTP server to demonstrate RestLi.
 *
 * @author Doug Young
 */
public class RestLiFortunesServer
{
  public static void main(String[] args) throws Exception
  {
    // Create a config that supplies a path to our Resources.
    // Resources will be loaded and used automatically
    RestLiConfig config = new RestLiConfig();
    config.addResourcePackageNames("com.example.fortune.impl");

    // Create and run an HTTP server on port 7279 for the RestLiServer
    HttpServer server = new HttpServerFactory()
                             .createServer(7279,
                                       new DelegatingTransportDispatcher(new RestLiServer(config)));
    server.start();
    System.out.println("Fortune server running on port 7279 Press any key to stop.");
    System.in.read();
    server.stop();
  }
}
