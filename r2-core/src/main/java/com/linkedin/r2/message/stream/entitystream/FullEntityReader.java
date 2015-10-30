package com.linkedin.r2.message.stream.entitystream;

import com.linkedin.common.callback.Callback;
import com.linkedin.data.ByteString;

/**
 * This is a convenience Reader to assemble the full entity of a stream message.
 *
 * @author Zhenkai Zhu
 */
public final class FullEntityReader implements Reader
{
  private final ByteString.Builder _builder;
  private final Callback<ByteString> _callback;

  private ReadHandle _rh;

  /**
   * @param callback the callback to be invoked when the reader finishes assembling the full entity
   */
  public FullEntityReader(Callback<ByteString> callback)
  {
    _callback = callback;
    _builder = new ByteString.Builder();
  }

  public void onInit(ReadHandle rh)
  {
    _rh = rh;
    _rh.request(10);
  }

  public void onDataAvailable(ByteString data)
  {
    _builder.append(data);
    _rh.request(1);
  }

  public void onDone()
  {
    ByteString entity = _builder.build();
    _callback.onSuccess(entity);
  }

  public void onError(Throwable ex)
  {
    _callback.onError(ex);
  }
}
