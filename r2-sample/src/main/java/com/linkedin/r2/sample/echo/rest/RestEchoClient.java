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
package com.linkedin.r2.sample.echo.rest;

import com.linkedin.data.ByteString;
import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.CallbackAdapter;
import com.linkedin.r2.message.rest.RestMethod;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.sample.echo.EchoService;
import com.linkedin.r2.transport.common.Client;

import java.net.URI;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class RestEchoClient implements EchoService
{
  private final URI _uri;
  private final Client _client;

  public RestEchoClient(URI uri, Client client)
  {
    _uri = uri;
    _client = client;
  }

  public void echo(String msg, Callback<String> callback)
  {
    final RestRequest req = new RestRequestBuilder(_uri)
            .setEntity(ByteString.copyString(msg, RestEchoServer.CHARSET))
            .setMethod(RestMethod.POST)
            .build();
    _client.restRequest(req, new CallbackAdapter<String, RestResponse>(callback) {
      @Override
      protected String convertResponse(RestResponse response) throws Exception
      {
        return response.getEntity().asString(RestEchoServer.CHARSET);
      }
    });
  }
}
