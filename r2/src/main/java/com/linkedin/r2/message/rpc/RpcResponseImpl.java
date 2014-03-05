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

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
/* package private */ final class RpcResponseImpl extends BaseRpcMessage implements RpcResponse
{
  RpcResponseImpl(ByteString body)
  {
    super(body);
  }

  @Override
  @Deprecated
  @SuppressWarnings("deprecation")
  public RpcResponseBuilder builder()
  {
    return new RpcResponseBuilder(this);
  }

  @Override
  @Deprecated
  @SuppressWarnings("deprecation")
  public RpcResponseBuilder responseBuilder()
  {
    return builder();
  }

  @Override
  @Deprecated
  @SuppressWarnings("deprecation")
  public boolean equals(Object o)
  {
    return super.equals(o) && o instanceof RpcResponseImpl;
  }

  @Override
  public String toString()
  {
    return "RpcResponse[entityLength=" + getEntity().length() + "]";
  }
}
