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

import java.net.URI;
import java.util.Collections;

import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class PerfClients
{
  public static PerfClient httpRpc(URI uri, int numThreads, int numMsgs, int msgSize)
  {
    final TransportClient transportClient = new HttpClientFactory().getClient(Collections.<String, String>emptyMap());
    final Client client = new TransportClientAdapter(transportClient);
    final RequestGenerator<String> reqGen = new StringRequestGenerator(numMsgs, msgSize);
    final ClientRunnableFactory crf = new RpcClientRunnableFactory(client, uri, reqGen);

    return new PerfClient(crf, numThreads);
  }

  public static PerfClient httpRest(URI uri, int numThreads, int numMsgs, int msgSize)
  {
    final TransportClient transportClient = new HttpClientFactory().getClient(Collections.<String, String>emptyMap());
    final Client client = new TransportClientAdapter(transportClient);
    final RequestGenerator<RestRequest> reqGen = new RestRequestGenerator(uri, numMsgs, msgSize);
    final ClientRunnableFactory crf = new RestClientRunnableFactory(client, reqGen);

    return new PerfClient(crf, numThreads);
  }
}
