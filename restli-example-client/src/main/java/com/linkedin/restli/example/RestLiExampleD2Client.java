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

package com.linkedin.restli.example;


import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.D2Client;
import com.linkedin.d2.balancer.D2ClientBuilder;
import com.linkedin.restli.client.RestClient;

import java.io.PrintWriter;


/**
 * @author Keren Jin
 */
public class RestLiExampleD2Client
{
  /**
   * This is a stand-alone app to demo the use of client-side Pegasus API. To run in,
   * com.linkedin.restli.example.RestLiExamplesServer has to be running.
   *
   * The only argument is the path to the resource on the photo server, e.g. /album/1
   */
  public static void main(String[] args) throws Exception
  {
    final D2Client d2Client = new D2ClientBuilder().build();
    d2Client.start(new FutureCallback<>());
    final RestClient restClient = new RestClient(d2Client, "d2://");
    final RestLiExampleBasicClient photoClient = new RestLiExampleBasicClient(restClient);

    photoClient.sendRequest(args[0], new PrintWriter(System.out));
    photoClient.shutdown();
  }
}
