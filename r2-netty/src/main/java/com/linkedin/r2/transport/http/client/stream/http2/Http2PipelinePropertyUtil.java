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

import com.linkedin.util.ArgumentUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.util.AttributeKey;
import java.util.Optional;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Util for setting, retrieving and removing properties in Http2 streams
 */
final class Http2PipelinePropertyUtil {
  private static final Logger LOG = LoggerFactory.getLogger(Http2PipelinePropertyUtil.class);

  private Http2PipelinePropertyUtil() {
  }

  public static <T> T set(ChannelHandlerContext ctx, Http2Connection http2Connection, int streamId,
      AttributeKey<Http2Connection.PropertyKey> key, T value) {
    return doAction(ctx, http2Connection, streamId, key, (stream, propertyKey) -> stream.setProperty(propertyKey, value));
  }

  public static <T> T remove(ChannelHandlerContext ctx, Http2Connection http2Connection, int streamId,
      AttributeKey<Http2Connection.PropertyKey> key) {
    return doAction(ctx, http2Connection, streamId, key, Http2Stream::removeProperty);
  }

  public static <T> T get(ChannelHandlerContext ctx, Http2Connection http2Connection, int streamId,
      AttributeKey<Http2Connection.PropertyKey> key) {
    return doAction(ctx, http2Connection, streamId, key, Http2Stream::getProperty);
  }

  private static <T> T getKey(ChannelHandlerContext ctx, AttributeKey<T> key) {
    ArgumentUtil.notNull(ctx, "ctx");
    ArgumentUtil.notNull(key, "key");
    return ctx.channel().attr(key).get();
  }

  private static <T> T doAction(ChannelHandlerContext ctx, Http2Connection http2Connection, int streamId,
      AttributeKey<Http2Connection.PropertyKey> key, BiFunction<Http2Stream, Http2Connection.PropertyKey, T> function) {
    ArgumentUtil.notNull(http2Connection, "http2Connection");
    final Http2Stream stream = http2Connection.stream(streamId);
    if (stream == null)
    {
      LOG.debug("Stream {} no longer exists", streamId);
      return null;
    }
    final Http2Connection.PropertyKey propertyKey = getKey(ctx, key);
    if (propertyKey == null)
    {
      LOG.debug("Property key {} is not valid", key);
      return null;
    }
    return function.apply(stream, propertyKey);
  }
}
