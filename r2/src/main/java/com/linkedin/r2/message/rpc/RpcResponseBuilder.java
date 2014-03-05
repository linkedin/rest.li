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

import com.linkedin.r2.message.ResponseBuilder;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public final class RpcResponseBuilder
        extends BaseRpcMessageBuilder<RpcResponseBuilder>
        implements ResponseBuilder<RpcResponseBuilder>, RpcMessageBuilder<RpcResponseBuilder>
{
  /**
   * Constructs a new builder with no initial values.
   */
  @Deprecated
  public RpcResponseBuilder() {}

  /**
   * Copies the values from the supplied response. Changes to this builder will not be reflected
   * in the original message.
   *
   * @param response the response to copy
   */
  @Deprecated
  public RpcResponseBuilder(RpcResponse response)
  {
    super(response);
  }

  @Override
  @Deprecated
  public RpcResponse build()
  {
    return new RpcResponseImpl(getEntity());
  }

  @Override
  @Deprecated
  public RpcResponse buildCanonical()
  {
    return build();
  }
}
