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
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.Client;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import test.r2.perf.Generator;


/**
* @author Chris Pettitt
* @version $Revision$
*/
/* package private */ class RestClientRunnable extends AbstractClientRunnable<RestRequest, RestResponse>
{
  private final Client _client;

  public RestClientRunnable(Client client,
                        AtomicReference<Stats> stats,
                        CountDownLatch startLatch,
                        Generator<RestRequest> reqGen)
  {
    super(stats, startLatch, reqGen);
    _client = client;
  }

  @Override
  protected void sendMessage(RestRequest nextMsg, Callback<RestResponse> callback)
  {
    _client.restRequest(nextMsg, callback);
  }
}
