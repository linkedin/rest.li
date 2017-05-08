/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.client.stream.http2;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.util.AttributeKey;


/**
 * Util for setting, retrieving and removing properties in Http2 streams
 */
final class Http2PipelinePropertyUtil {
  private Http2PipelinePropertyUtil() {
  }

  public static <T> T set(ChannelHandlerContext ctx, Http2Connection http2Connection, int streamId,
      AttributeKey<Http2Connection.PropertyKey> key, T value) {
    return http2Connection.stream(streamId).setProperty(getKey(ctx, key), value);
  }

  public static <T> T remove(ChannelHandlerContext ctx, Http2Connection http2Connection, int streamId,
      AttributeKey<Http2Connection.PropertyKey> key) {
    return http2Connection.stream(streamId).removeProperty(getKey(ctx, key));
  }

  public static <T> T get(ChannelHandlerContext ctx, Http2Connection http2Connection, int streamId,
      AttributeKey<Http2Connection.PropertyKey> key) {
    return http2Connection.stream(streamId).getProperty(getKey(ctx, key));
  }

  private static <T> T getKey(ChannelHandlerContext ctx, AttributeKey<T> key) {
    return ctx.channel().attr(key).get();
  }
}
