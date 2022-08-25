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

package com.linkedin.r2.netty.common;

import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.netty.entitystream.StreamWriter;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.http.client.AsyncPool;
import com.linkedin.r2.transport.http.client.common.ssl.SslSessionValidator;
import com.linkedin.r2.util.Timeout;
import com.linkedin.pegasus.io.netty.channel.Channel;
import com.linkedin.pegasus.io.netty.channel.ChannelFuture;
import com.linkedin.pegasus.io.netty.channel.ChannelPipeline;
import com.linkedin.pegasus.io.netty.util.AttributeKey;
import com.linkedin.pegasus.io.netty.util.concurrent.Promise;
import java.util.concurrent.ScheduledFuture;

/**
 * Lists all {@link AttributeKey} used to access the channel attributes.
 *
 * @author Sean Sheng
 * @author Nizar Mankulangara
 */
public interface NettyChannelAttributes
{
  /**
   * Attribute for the {@link Promise} that sets after ALPN is complete.
   *  If the channel is https this will be used to set the ALPN promise
   *  and if the channel is clearText this attribute will be used to set the Http to Http2 upgrade promise
   */
  AttributeKey<ChannelFuture> INITIALIZATION_FUTURE = AttributeKey.newInstance("initializationPromise");

  /**
   * Attribute for the {@link StreamWriter} responsible for writing response
   * data from the {@link ChannelPipeline} to the {@link EntityStream}.
   */
  AttributeKey<StreamWriter> RESPONSE_WRITER = AttributeKey.newInstance("responseWriter");

  /**
   * Attribute for the channel {@link AsyncPool}.
   */
  AttributeKey<AsyncPool<Channel>> CHANNEL_POOL = AttributeKey.newInstance("channelPool");

  /**
   * Attribute for the channel {@link Timeout} that trigger various tasks upon expire.
   */
  AttributeKey<ScheduledFuture<ChannelPipeline>> TIMEOUT_FUTURE = AttributeKey.newInstance("timeout");

  /**
   * Attribute for the channel {@link ScheduledFuture} that trigger stream idle timeout Exception.
   */
  AttributeKey<StreamingTimeout> STREAMING_TIMEOUT_FUTURE = AttributeKey.newInstance("streamingTimeout");

  /**
   * Attribute for the channel response {@link TransportCallback}.
   */
  AttributeKey<TransportCallback<StreamResponse>> RESPONSE_CALLBACK = AttributeKey.newInstance("responseCallback");

  /**
   * Attribute for the {@link SslSessionValidator}.
   */
  AttributeKey<SslSessionValidator> SSL_SESSION_VALIDATOR = AttributeKey.valueOf("sslSessionValidator");
}
