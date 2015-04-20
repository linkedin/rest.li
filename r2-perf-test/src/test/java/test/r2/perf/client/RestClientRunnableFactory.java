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
package test.r2.perf.client;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.common.util.None;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import test.r2.perf.Generator;


/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class RestClientRunnableFactory implements ClientRunnableFactory
{
  private final Client _client;
  private final Generator<RestRequest> _reqGen;

  public RestClientRunnableFactory(Client client, Generator<RestRequest> reqGen)
  {
    _client = client;
    _reqGen = reqGen;
  }

  @Override
  public Runnable create(AtomicReference<Stats> stats, CountDownLatch startLatch)
  {
    return new RestClientRunnable(_client, stats, startLatch, _reqGen);
  }

  @Override
  public void shutdown()
  {
    final FutureCallback<None> callback = new FutureCallback<None>();
    _client.shutdown(callback);

    try
    {
      callback.get();
    }
    catch (Exception e)
    {
      // Print out error and continue
      e.printStackTrace();
    }
  }
}
