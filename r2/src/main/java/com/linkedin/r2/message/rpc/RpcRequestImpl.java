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

import com.linkedin.data.ByteString;

import java.net.URI;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
/* package private */ final class RpcRequestImpl extends BaseRpcMessage implements RpcRequest
{
  private final URI _uri;

  /* package private */ RpcRequestImpl(ByteString body, URI uri)
  {
    super(body);

    assert uri != null;

    _uri = uri;
  }

  @Override
  public URI getURI()
  {
    return _uri;
  }

  @Override
  @Deprecated
  @SuppressWarnings("deprecation")
  public RpcRequestBuilder builder()
  {
    return new RpcRequestBuilder(this);
  }

  @Override
  public RpcRequestBuilder requestBuilder()
  {
    return builder();
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
    {
      return true;
    }
    if (!(o instanceof RpcRequestImpl))
    {
      return false;
    }
    if (!super.equals(o))
    {
      return false;
    }

    RpcRequestImpl that = (RpcRequestImpl) o;
    return _uri.equals(that._uri);
  }

  @Override
  public int hashCode()
  {
    int result = super.hashCode();
    result = 31 * result + _uri.hashCode();
    return result;
  }

  @Override
  public String toString()
  {
    return "RpcRequest[uri=" + getURI() + ",entityLength=" + getEntity().length() + "]";
  }
}
