/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.r2.netty.handler.http2;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * On the client side, server initiated streams are not supported at the moment. Therefore,
 * {@link UnsupportedHandler} is not expected to be added to the pipeline. An error
 * is logged if the server initializes a stream and handler is added.
 *
 * @author Sean Sheng
 * @author Nizar Mankulangara
 */
@Sharable
public class UnsupportedHandler extends ChannelHandlerAdapter
{
  private static final Logger LOG = LoggerFactory.getLogger(UnsupportedHandler.class);

  @Override
  public void handlerAdded(ChannelHandlerContext ctx)
  {
    LOG.error("Remotely created streams is not supported for the client implementation.");
    ctx.channel().close();
  }
}
