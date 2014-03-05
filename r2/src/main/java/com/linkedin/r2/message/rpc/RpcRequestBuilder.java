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
package com.linkedin.r2.message.rpc;

import com.linkedin.r2.message.RequestBuilder;
import com.linkedin.util.ArgumentUtil;

import java.net.URI;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public final class RpcRequestBuilder
        extends BaseRpcMessageBuilder<RpcRequestBuilder>
        implements RequestBuilder<RpcRequestBuilder>, RpcMessageBuilder<RpcRequestBuilder>
{
  private URI _uri;

  /**
   * Constructs a new builder using the given uri.
   *
   * @param uri the URI for the service involved in the request
   * @deprecated r2 rpc is not supported, use rest instead
   */
  @Deprecated
  public RpcRequestBuilder(URI uri)
  {
    setURI(uri);
  }

  /**
   * Copies the values from the supplied request. Changes to this builder will not be reflected
   * in the original message.
   *
   * @param request the request to copy
   * @deprecated r2 rpc is not supported, use rest instead
   */
  @Deprecated
  public RpcRequestBuilder(RpcRequest request)
  {
    super(request);

    setURI(request.getURI());
  }

  @Override
  @Deprecated
  public URI getURI()
  {
    return _uri;
  }

  @Override
  @Deprecated
  public RpcRequestBuilder setURI(URI uri)
  {
    ArgumentUtil.notNull(uri, "uri");

    _uri = uri;

    return this;
  }

  @Override
  @Deprecated
  public RpcRequest build()
  {
    return new RpcRequestImpl(getEntity(), getURI());
  }

  @Override
  @Deprecated
  public RpcRequest buildCanonical()
  {
    return new RpcRequestImpl(getEntity(), getURI().normalize());
  }
}
