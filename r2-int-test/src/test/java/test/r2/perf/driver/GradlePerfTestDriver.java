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

import java.lang.reflect.Method;

/**
 * Very, very hacky way to run the client and the server - this is a stop gap until Gradle supports
 * backgrounding java processes.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public class GradlePerfTestDriver
{
  public static void main(String[] args) throws Exception
  {
    if (args.length != 2)
    {
      System.err.println("Usage: GradlePerfTestDriver <server_class> <client_class>");
      System.exit(1);
    }

    final Class<?> serverClass = Class.forName(args[0]);
    final Class<?> clientClass = Class.forName(args[1]);

    final Runnable serverThread = createRunnable(serverClass);
    final Runnable clientThread = createRunnable(clientClass);

    serverThread.run();

    clientThread.run();

    System.exit(0);
  }

  private static Runnable createRunnable(Class<?> clazz) throws NoSuchMethodException
  {
    final Method method = clazz.getMethod("main", String[].class);
    return new Runnable() {
      @Override
      public void run()
      {
        try
        {
          method.invoke(null, new Object[] {new String[0]});
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    };
  }
}
