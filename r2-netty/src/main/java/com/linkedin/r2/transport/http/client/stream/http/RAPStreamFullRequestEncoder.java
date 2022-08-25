/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.client.stream.http;

import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.netty.common.NettyRequestAdapter;
import com.linkedin.pegasus.io.netty.channel.ChannelHandlerContext;
import com.linkedin.pegasus.io.netty.handler.codec.MessageToMessageEncoder;
import com.linkedin.pegasus.io.netty.handler.codec.http.HttpRequest;

import java.util.List;

/**
 * This encoder encodes RestRequest to Netty's HttpRequest.
 *
 * @author Zhenkai Zhu
 */

class RAPStreamFullRequestEncoder extends MessageToMessageEncoder<RestRequest>
{
  @Override
  protected void encode(ChannelHandlerContext ctx, RestRequest msg, List<Object> out) throws Exception
  {
    HttpRequest nettyRequest = NettyRequestAdapter.toNettyRequest(msg);
    out.add(nettyRequest);
  }
}
