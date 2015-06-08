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
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.sample.echo.EchoService;
import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.r2.util.IOUtil;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class RestEchoServer implements RestRequestHandler
{
  /* package private */ static final Charset CHARSET = Charset.forName("UTF-8");

  private final EchoService _echoService;

  public RestEchoServer(EchoService echoService)
  {
    _echoService = echoService;
  }

  public void handleRequest(RestRequest request, RequestContext requestContext,
                            Callback<RestResponse> callback)
  {
    final String msg;
    try
    {
      msg = IOUtil.toString(request.getEntity().asInputStream(), CHARSET.name()) ;
    }
    catch (IOException ex)
    {
      callback.onError(ex);
      return;
    }
    _echoService.echo(msg, new CallbackAdapter<RestResponse, String>(callback) {
      @Override
      protected RestResponse convertResponse(String responseMsg)
      {
        return new RestResponseBuilder()
                .setEntity(ByteString.copyString(responseMsg, CHARSET))
                .setHeader("Content-Type", "text/plain")
                .build();
      }
    });
  }
}
