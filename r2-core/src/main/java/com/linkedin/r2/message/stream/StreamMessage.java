package com.linkedin.r2.message.stream;

import com.linkedin.r2.message.MessageHeaders;
import com.linkedin.r2.message.stream.entitystream.EntityStream;

/**
 * StreamMessage is a message with MessageHeaders and an EntityStream as its entity.
 * StreamMessage is not immutable and in general cannot be reused.
 *
 * @author Zhenkai Zhu
 */
public interface StreamMessage extends MessageHeaders
{
  /**
   * Returns the EntityStream for this message. The entity stream can only be read once (i.e. Message
   * does not keep a copy of the whole entity).
   *
   * @return the EntityStream of this message.
   */
  EntityStream getEntityStream();

  StreamMessageBuilder<? extends StreamMessageBuilder<?>> builder();
}
