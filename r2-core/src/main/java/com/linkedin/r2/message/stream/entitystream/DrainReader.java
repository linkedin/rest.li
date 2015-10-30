package com.linkedin.r2.message.stream.entitystream;

import com.linkedin.data.ByteString;

/**
 * This is a convenience reader to drain the bytes in the entity stream and simply discard the bytes.
 *
 * @author Zhenkai Zhu
 */
public class DrainReader implements Reader
{
  private ReadHandle _rh;

  public void onInit(ReadHandle rh)
  {
    _rh = rh;
    _rh.request(Integer.MAX_VALUE);
  }

  public void onDataAvailable(ByteString data)
  {
    _rh.request(1);
  }

  public void onDone()
  {
  }

  public void onError(Throwable ex)
  {
  }

}
