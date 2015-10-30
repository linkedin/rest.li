package com.linkedin.r2.message.stream;

import com.linkedin.r2.message.BaseResponseBuilder;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.stream.entitystream.EntityStream;

/**
 * @author Zhenkai Zhu
 */
public final class StreamResponseBuilder extends BaseResponseBuilder<StreamResponseBuilder>
    implements StreamMessageBuilder<StreamResponseBuilder>
{
  /**
   * Constructs a new builder with no initial values.
   */
  public StreamResponseBuilder() {}

  /**
   * Copies the values from the supplied response. Changes to this builder will not be reflected
   * in the original message.
   *
   * @param response the response to copy
   */
  public StreamResponseBuilder(Response response)
  {
    super(response);
  }

  @Override
  public StreamResponse build(EntityStream entityStream)
  {
    return new StreamResponseImpl(entityStream, getHeaders(), getCookies(), getStatus());
  }

  @Override
  public StreamResponse buildCanonical(EntityStream entityStream)
  {
    return new StreamResponseImpl(entityStream, getCanonicalHeaders(), getCanonicalCookies(), getStatus());
  }
}
