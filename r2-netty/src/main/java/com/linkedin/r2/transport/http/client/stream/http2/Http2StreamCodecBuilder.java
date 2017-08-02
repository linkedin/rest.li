/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.client.stream.http2;

import io.netty.handler.codec.http2.AbstractHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.DefaultHttp2ConnectionDecoder;
import io.netty.handler.codec.http2.DefaultHttp2ConnectionEncoder;
import io.netty.handler.codec.http2.DefaultHttp2FrameReader;
import io.netty.handler.codec.http2.DefaultHttp2FrameWriter;
import io.netty.handler.codec.http2.DefaultHttp2HeadersDecoder;
import io.netty.handler.codec.http2.DefaultHttp2LocalFlowController;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2FrameReader;
import io.netty.handler.codec.http2.Http2FrameWriter;
import io.netty.handler.codec.http2.Http2HeadersDecoder;
import io.netty.handler.codec.http2.Http2InboundFrameLogger;
import io.netty.handler.codec.http2.Http2OutboundFrameLogger;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.StreamBufferingEncoder;
import io.netty.util.internal.ObjectUtil;

import static io.netty.handler.codec.http2.DefaultHttp2LocalFlowController.DEFAULT_WINDOW_UPDATE_RATIO;


class Http2StreamCodecBuilder extends AbstractHttp2ConnectionHandlerBuilder<Http2StreamCodec, Http2StreamCodecBuilder>
{
  // TODO: Consider exposing these as configurable values
  private final int MAX_INITIAL_STREAM_WINDOW_SIZE = 8 * 1024 * 1024;
  private final boolean AUTO_REFILL_CONNECTION_WINDOW = true;

  private long _maxContentLength = -1;
  private long _gracefulShutdownTimeoutMillis = -1;
  private Http2Connection _connection = null;

  public Http2StreamCodecBuilder maxContentLength(long maxContentLength)
  {
    ObjectUtil.checkPositive(maxContentLength, "maxContentLength");
    _maxContentLength = maxContentLength;
    return self();
  }

  public Http2StreamCodecBuilder gracefulShutdownTimeoutMillis(long gracefulShutdownTimeoutMillis)
  {
    ObjectUtil.checkPositive(gracefulShutdownTimeoutMillis, "gracefulShutdownTimeoutMillis");
    _gracefulShutdownTimeoutMillis = gracefulShutdownTimeoutMillis;
    return self();
  }

  @Override
  public Http2StreamCodecBuilder connection(Http2Connection connection)
  {
    ObjectUtil.checkNotNull(connection, "connection");
    _connection = connection;
    return self();
  }

  @Override
  public Http2StreamCodec build()
  {
    ObjectUtil.checkNotNull(_connection, "connection");

    Http2HeadersDecoder headerDecoder = new DefaultHttp2HeadersDecoder(isValidateHeaders());
    Http2FrameReader reader = new DefaultHttp2FrameReader(headerDecoder);
    Http2FrameWriter writer = new DefaultHttp2FrameWriter(headerSensitivityDetector());

    if (frameLogger() != null) {
      reader = new Http2InboundFrameLogger(reader, frameLogger());
      writer = new Http2OutboundFrameLogger(writer, frameLogger());
    }

    Http2ConnectionEncoder encoder = new DefaultHttp2ConnectionEncoder(_connection, writer);
    boolean encoderEnforceMaxConcurrentStreams = encoderEnforceMaxConcurrentStreams();

    if (encoderEnforceMaxConcurrentStreams) {
      if (_connection.isServer()) {
        encoder.close();
        reader.close();
        throw new IllegalArgumentException(
            "encoderEnforceMaxConcurrentStreams: " + encoderEnforceMaxConcurrentStreams +
                " not supported for server");
      }
      encoder = new StreamBufferingEncoder(encoder);
    }

    _connection.local().flowController(
        new DefaultHttp2LocalFlowController(_connection, DEFAULT_WINDOW_UPDATE_RATIO, AUTO_REFILL_CONNECTION_WINDOW));
    Http2ConnectionDecoder decoder = new DefaultHttp2ConnectionDecoder(_connection, encoder, reader);

    super.codec(decoder, encoder);

    return super.build();
  }

  @Override
  protected Http2StreamCodec build(
      Http2ConnectionDecoder decoder,
      Http2ConnectionEncoder encoder,
      Http2Settings initialSettings)
      throws Exception
  {
    ObjectUtil.checkPositive(_maxContentLength, "maxContentLength");
    ObjectUtil.checkPositive(_gracefulShutdownTimeoutMillis, "gracefulShutdownTimeoutMillis");
    ObjectUtil.checkNotNull(_connection, "connection");

    // HTTP/2 initial settings
    initialSettings.initialWindowSize(Math.min(MAX_INITIAL_STREAM_WINDOW_SIZE, (int)_maxContentLength));

    Http2StreamCodec codec = new Http2StreamCodec(decoder, encoder, initialSettings);
    super.frameListener(new Http2FrameListener(_connection, codec, _maxContentLength));
    super.gracefulShutdownTimeoutMillis(_gracefulShutdownTimeoutMillis);

    return codec;
  }
}
