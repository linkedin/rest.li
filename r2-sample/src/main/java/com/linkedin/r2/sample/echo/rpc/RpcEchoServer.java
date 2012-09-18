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
package com.linkedin.r2.sample.echo.rpc;

import com.linkedin.data.ByteString;
import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.CallbackAdapter;
import com.linkedin.r2.message.rpc.RpcRequest;
import com.linkedin.r2.message.rpc.RpcResponse;
import com.linkedin.r2.message.rpc.RpcResponseBuilder;
import com.linkedin.r2.sample.echo.EchoService;
import com.linkedin.r2.transport.common.RpcRequestHandler;

import java.nio.charset.Charset;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class RpcEchoServer implements RpcRequestHandler
{
  /* package private */ static final Charset CHARSET = Charset.forName("UTF-8");

  private final EchoService _service;

  public RpcEchoServer(EchoService service)
  {
    _service = service;
  }

  @Override
  public void handleRequest(RpcRequest request, Callback<RpcResponse> callback)
  {
    final String msg = request.getEntity().asString(CHARSET);

    _service.echo(msg, new CallbackAdapter<RpcResponse,String>(callback) {
      @Override
      protected RpcResponse convertResponse(String response)
      {
        return new RpcResponseBuilder()
                .setEntity(ByteString.copyString(response, CHARSET))
                .build();
      }
    });
  }
}
