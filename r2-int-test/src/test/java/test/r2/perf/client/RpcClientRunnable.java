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

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.sample.echo.rpc.RpcEchoClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
* @author Chris Pettitt
* @version $Revision$
*/
/* package private */ class RpcClientRunnable extends AbstractClientRunnable<String, String>
{
  private final RpcEchoClient _echoClient;

  public RpcClientRunnable(RpcEchoClient echoClient,
                        AtomicReference<Stats> stats,
                        CountDownLatch startLatch,
                        RequestGenerator<String> reqGen)
  {
    super(stats, startLatch, reqGen);
    _echoClient = echoClient;
  }

  @Override
  protected void sendMessage(String nextMsg, Callback<String> callback)
  {
    _echoClient.echo(nextMsg, callback);
  }
}
