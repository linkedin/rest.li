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

package com.linkedin.r2.transport.http.client;

import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.transport.common.WireAttributeHelper;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;

import java.nio.channels.ClosedChannelException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.linkedin.r2.transport.http.client.HttpNettyClient.LOG;


/**
 * Netty pipeline handler which takes a complete received message and invokes the
 * user-specified callback.
 *
 * @author Steven Ihde
 * @version $Revision: $
 */

class RAPResponseHandler extends UpstreamHandlerWithAttachment<TransportCallback<RestResponse>>
{
  // Note that an instance of this class needs to be stateless, since a single instance is used
  // in multiple ChannelPipelines simultaneously.  The per-channel state is stored in the
  // ChannelHandlerContext attachment.

  // Access to the attachment is not synchronized; Netty's threading model for upstream events
  // means that only one thread ever sends upstream events on a particular channel.  Since this
  // is an upstream-only handler, we don't need to worry about downstream events.

  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception
  {
    RestResponse response = (RestResponse)e.getMessage();

    // In general there should always be a callback to handle a received message,
    // but it could have been removed due to a previous exception or closure on the
    // channel
    TransportCallback<RestResponse> callback = removeAttachment(ctx);
    if (callback != null)
    {
      LOG.debug("{}: handling a response", e.getChannel().getRemoteAddress());
      final Map<String, String> headers = new HashMap<String, String>(response.getHeaders());
      final Map<String, String> wireAttrs =
            new HashMap<String, String>(WireAttributeHelper.removeWireAttributes(headers));

      final RestResponse newResponse = new RestResponseBuilder(response)
              .unsafeSetHeaders(headers)
              .build();

      callback.onResponse(TransportResponseImpl.success(newResponse, wireAttrs));
    }
    else
    {
      LOG.debug("{}: dropped a response", e.getChannel().getRemoteAddress());
    }
    super.messageReceived(ctx, e);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception
  {
    TransportCallback<RestResponse> callback = removeAttachment(ctx);
    if (callback != null)
    {
      LOG.debug(e.getChannel().getRemoteAddress() + ": exception on active channel", e.getCause());
      callback.onResponse(TransportResponseImpl.<RestResponse>error(
              HttpNettyClient.toException(e.getCause()), Collections.<String,String>emptyMap()));
    }
    else
    {
      LOG.debug(e.getChannel().getRemoteAddress() + ": exception on idle channel", e.getCause());
    }
    super.exceptionCaught(ctx, e);
  }

  @Override
  public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception
  {
    // XXX this seems a bit odd, but if the channel closed before downstream layers received a response, we
    // have to deal with that ourselves (it does not get turned into an exception by downstream
    // layers, even though some other protocol errors do)

    TransportCallback<RestResponse> callback = removeAttachment(ctx);
    if (callback != null)
    {
      LOG.debug("{}: active channel closed", e.getChannel().getRemoteAddress());
      callback.onResponse(TransportResponseImpl.<RestResponse>error(new ClosedChannelException(),
                                                                    Collections.<String, String>emptyMap()));
    }
    else
    {
      LOG.debug("{}: idle channel closed", e.getChannel().getRemoteAddress());
    }
    super.channelClosed(ctx, e);
  }
}
