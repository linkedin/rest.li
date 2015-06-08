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

/**
 * $Id: $
 */

package com.linkedin.restli.client;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.AbstractClient;
import com.linkedin.r2.transport.common.bridge.client.TransportCallbackAdapter;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.r2.transport.http.common.HttpBridge;

import java.util.Map;

/**
 * TODO this should probably live in the r2 module and support more features.
 * @author Steven Ihde
 * @version $Revision: $
 */

public class MockClient extends AbstractClient
{
  private final int _status;
  private final Map<String,String> _headers;
  private final byte[] _body;

  public MockClient(int status, Map<String,String> headers, byte[] body)
  {
    _status = status;
    _headers = headers;
    _body = body;
  }

  @Override
  public void streamRequest(StreamRequest request, RequestContext requestContext,
                          Callback<StreamResponse> callback)
  {
    TransportCallback<StreamResponse> adapter = HttpBridge.streamToHttpCallback(new TransportCallbackAdapter<StreamResponse>(callback), request);

    RestResponse response = new RestResponseBuilder()
            .setStatus(status())
            .setHeaders(headers())
            .setEntity(body())
            .build();

    adapter.onResponse(TransportResponseImpl.success(Messages.toStreamResponse(response)));
  }

  @Override
  public void shutdown(Callback<None> callback)
  {
  }

  protected int status()
  {
    return _status;
  }

  protected Map<String, String> headers()
  {
    return _headers;
  }

  protected byte[] body()
  {
    return _body;
  }
}
