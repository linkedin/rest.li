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
import com.linkedin.r2.message.BaseMessage;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
/* package private */ abstract class BaseRpcMessage extends BaseMessage implements RpcMessage
{
  /* package private */ BaseRpcMessage(ByteString body)
  {
    super(body);
  }

  @Override
  public RpcMessageBuilder<? extends RpcMessageBuilder<?>> rpcBuilder()
  {
    return builder();
  }

  @Override
  public abstract RpcMessageBuilder<? extends RpcMessageBuilder<?>> builder();
}
