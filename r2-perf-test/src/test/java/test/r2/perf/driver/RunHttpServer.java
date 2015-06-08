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
package test.r2.perf.driver;

import com.linkedin.r2.transport.common.Server;
import test.r2.perf.PerfConfig;
import test.r2.perf.server.HttpPerfServerFactory;

import java.io.IOException;
import java.net.URI;


/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class RunHttpServer
{
  private static volatile Server SERVER;

  public static void main(String[] args) throws IOException
  {
    final int port = PerfConfig.getHttpPort();
    final URI relativeUri = PerfConfig.getRelativeUri();
    final int msgSize = PerfConfig.getServerMessageSize();
    final boolean pureStreaming = PerfConfig.isServerPureStreaming();

    if (pureStreaming)
    {
      SERVER = new HttpPerfServerFactory().createPureStreamServer(port, relativeUri, msgSize);
    }
    else
    {
      SERVER = new HttpPerfServerFactory().create(port, relativeUri, msgSize);
    }
    SERVER.start();
  }

  public static void stop() throws IOException
  {
    SERVER.stop();
  }
}
